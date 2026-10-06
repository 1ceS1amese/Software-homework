#!/usr/bin/env bash
set -euo pipefail

source "$(dirname -- "${BASH_SOURCE[0]}")/env.sh"
STATE_DIR="$PROJECT_DIR/.local"
ENV_FILE="$PROJECT_DIR/.env.local"
BACKEND_JAR="$PROJECT_DIR/backend/target/csms-backend-1.0.0-SNAPSHOT.jar"
mkdir -p "$STATE_DIR/logs" "$STATE_DIR/run"

require() {
  command -v "$1" >/dev/null || { echo "缺少 $1，请按 README 安装 Linux 工具链。" >&2; exit 1; }
}

load_env() {
  [[ -f "$ENV_FILE" ]] || { echo "请先运行 ./scripts/dev.sh setup。" >&2; exit 1; }
  set -a
  source "$ENV_FILE"
  set +a
}

check_tools() {
  for tool in node pnpm java curl setsid; do require "$tool"; done
  node -e 'const [major, minor] = process.versions.node.split(".").map(Number); if (major < 22 || (major === 22 && minor < 12)) process.exit(1)' || {
    echo "需要 Node.js 22.12 或更高版本。" >&2; exit 1;
  }
}

init_mysql() {
  require mysqld
  require mysql
  local data_dir="$STATE_DIR/mysql/data" basedir
  basedir="$(dirname -- "$(dirname -- "$(readlink -f "$(command -v mysqld)")")")"
  mkdir -p "$data_dir"
  if [[ ! -f "$data_dir/auto.cnf" ]]; then
    [[ -z "$(ls -A "$data_dir")" ]] || { echo "数据库目录不为空，禁止自动覆盖。" >&2; exit 1; }
    mysqld --no-defaults --initialize-insecure --user="$(id -un)" --basedir="$basedir" --datadir="$data_dir" --log-error="$STATE_DIR/logs/mysql-init.log"
  fi
}

start_mysql() {
  [[ "${CSMS_MANAGE_DB:-1}" == 1 ]] || return 0
  [[ "$CSMS_DB_HOST" == 127.0.0.1 ]] || { echo "托管数据库只允许连接 127.0.0.1。" >&2; exit 1; }
  [[ "$CSMS_DB_NAME" =~ ^[a-zA-Z0-9_]+$ && "$CSMS_DB_USERNAME" =~ ^[a-zA-Z0-9_]+$ ]] || { echo "托管库名与用户名仅允许字母、数字及下划线。" >&2; exit 1; }
  [[ "$CSMS_DB_USERNAME" != root ]] || { echo "请使用独立应用账号，禁止使用 root。" >&2; exit 1; }
  init_mysql
  if ! owned_pid mysql >/dev/null; then
    if (echo > "/dev/tcp/127.0.0.1/$CSMS_DB_PORT") 2>/dev/null; then echo "$CSMS_DB_PORT 已被其他数据库占用。" >&2; exit 1; fi
    local basedir
    basedir="$(dirname -- "$(dirname -- "$(readlink -f "$(command -v mysqld)")")")"
    (
      cd "$STATE_DIR/mysql"
      nohup setsid mysqld --no-defaults --user="$(id -un)" --basedir="$basedir" --datadir="$STATE_DIR/mysql/data" \
        --bind-address=127.0.0.1 --port="$CSMS_DB_PORT" --mysqlx=OFF --skip-name-resolve \
        --socket="$STATE_DIR/mysql/mysql.sock" --pid-file="$STATE_DIR/run/mysql.pid" \
        --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci \
        > "$STATE_DIR/logs/mysql.log" 2>&1 < /dev/null &
      echo $! > "$STATE_DIR/run/mysql.pid"
    )
  fi
  local ready=0
  for ((i=0; i<60; i++)); do
    if [[ -S "$STATE_DIR/mysql/mysql.sock" ]] && mysqladmin --no-defaults --socket="$STATE_DIR/mysql/mysql.sock" ping >/dev/null 2>&1; then ready=1; break; fi
    sleep 1
  done
  [[ "$ready" == 1 ]] || { echo "MySQL 未就绪，查看 .local/logs/mysql.log。" >&2; exit 1; }
  if [[ ! -f "$STATE_DIR/mysql/credentials-initialized" ]]; then
    # 初次初始化仅走本地 socket；密码由 stdin 传入，禁止写进命令参数或日志。
    local app_password root_password
    app_password="${CSMS_DB_PASSWORD//\\/\\\\}"; app_password="${app_password//\'/\'\'}"
    root_password="${CSMS_MYSQL_ROOT_PASSWORD//\\/\\\\}"; root_password="${root_password//\'/\'\'}"
    mysql --no-defaults --socket="$STATE_DIR/mysql/mysql.sock" -uroot <<SQL
CREATE DATABASE IF NOT EXISTS \`$CSMS_DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '$CSMS_DB_USERNAME'@'127.0.0.1' IDENTIFIED BY '$app_password';
GRANT ALL PRIVILEGES ON \`$CSMS_DB_NAME\`.* TO '$CSMS_DB_USERNAME'@'127.0.0.1';
ALTER USER 'root'@'localhost' IDENTIFIED BY '$root_password';
SQL
    touch "$STATE_DIR/mysql/credentials-initialized"
  fi
}

build_backend() {
  if command -v mvn >/dev/null; then
    (cd "$PROJECT_DIR/backend" && mvn -B -ntp clean package)
  else
    (cd "$PROJECT_DIR/backend" && bash ./mvnw -B -ntp clean package)
  fi
}

# PID 必须仍属于对应工程，避免停止已被操作系统复用的 PID。
owned_pid() {
  local service="$1" pid cwd
  [[ -f "$STATE_DIR/run/$service.pid" ]] || return 1
  read -r pid < "$STATE_DIR/run/$service.pid"
  [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null || return 1
  cwd="$(readlink "/proc/$pid/cwd" 2>/dev/null || true)"
  local expected_dir="$PROJECT_DIR/$service"
  if [[ "$service" == mysql ]]; then expected_dir="$STATE_DIR/mysql/data"; fi
  [[ "$cwd" == "$expected_dir" ]] || return 1
  printf '%s' "$pid"
}

wait_http() {
  local name="$1" url="$2" attempts="$3" pid
  for ((i=0; i<attempts; i++)); do
    if curl -fsS --max-time 2 "$url" >/dev/null 2>&1; then return; fi
    if ! pid="$(owned_pid "$name")"; then
      echo "$name 启动失败，请查看 $STATE_DIR/logs/$name.log。" >&2; return 1
    fi
    sleep 1
  done
  echo "$name 未能及时就绪，请查看 $STATE_DIR/logs/$name.log。" >&2
  return 1
}

start() {
  check_tools
  load_env
  [[ -f "$BACKEND_JAR" && -d "$PROJECT_DIR/frontend/node_modules" ]] || {
    echo "请先运行 ./scripts/dev.sh setup 安装依赖并打包后端。" >&2; exit 1;
  }
  start_mysql
  if ! owned_pid backend >/dev/null; then
    if curl -s --max-time 2 http://127.0.0.1:8081/ >/dev/null; then
      echo "8081 已被其他进程占用。" >&2; exit 1;
    fi
    (
      cd "$PROJECT_DIR/backend"
      nohup setsid java -jar "$BACKEND_JAR" > "$STATE_DIR/logs/backend.log" 2>&1 < /dev/null &
      echo $! > "$STATE_DIR/run/backend.pid"
    )
  fi
  wait_http backend http://127.0.0.1:8081/actuator/health 90
  if ! owned_pid frontend >/dev/null; then
    if curl -s --max-time 2 http://127.0.0.1:5173/ >/dev/null; then
      echo "5173 已被其他进程占用。" >&2; exit 1;
    fi
    (
      cd "$PROJECT_DIR/frontend"
      nohup setsid pnpm dev --host 127.0.0.1 --strictPort > "$STATE_DIR/logs/frontend.log" 2>&1 < /dev/null &
      echo $! > "$STATE_DIR/run/frontend.pid"
    )
  fi
  wait_http frontend http://127.0.0.1:5173/login 30
  echo "本地项目已启动：http://localhost:5173（后端 8081，MySQL $CSMS_DB_PORT）。"
}

stop() {
  local service pid
  for service in frontend backend mysql; do
    if pid="$(owned_pid "$service")"; then
      kill -- "-$pid"
      for ((i=0; i<20; i++)); do
        kill -0 "$pid" 2>/dev/null || break
        sleep 0.25
      done
      if owned_pid "$service" >/dev/null; then echo "$service 尚未停止，请检查日志。" >&2; return 1; fi
    fi
  done
  echo "项目已停止，原生数据库数据和本地配置已保留。"
}

case "${1:-status}" in
  setup)
    check_tools
    if [[ ! -f "$ENV_FILE" ]]; then
      require openssl
      umask 077
      {
        printf 'CSMS_DB_HOST=127.0.0.1\nCSMS_DB_PORT=3307\nCSMS_DB_NAME=csms\nCSMS_DB_USERNAME=csms\nCSMS_MANAGE_DB=1\n'
        printf 'CSMS_DB_PASSWORD=%s\n' "$(openssl rand -hex 24)"
        printf 'CSMS_MYSQL_ROOT_PASSWORD=%s\n' "$(openssl rand -hex 24)"
        printf 'CSMS_JWT_SECRET=%s\nSERVER_ADDRESS=127.0.0.1\n' "$(openssl rand -hex 48)"
      } > "$ENV_FILE"
      umask 022
    fi
    load_env
    if [[ "${CSMS_MANAGE_DB:-1}" == 1 ]]; then init_mysql; fi
    (cd "$PROJECT_DIR/frontend" && pnpm install --frozen-lockfile)
    build_backend
    echo "环境准备完成，运行 ./scripts/dev.sh start 启动项目。"
    ;;
  start) start ;;
  stop) stop ;;
  restart) stop; start ;;
  status)
    for service in mysql backend frontend; do
      if pid="$(owned_pid "$service")"; then echo "$service: running (PID $pid)"; else echo "$service: stopped"; fi
    done
    curl -fsS --max-time 3 http://127.0.0.1:8081/actuator/health || true
    ;;
  logs) tail -n 60 "$STATE_DIR/logs/backend.log" "$STATE_DIR/logs/frontend.log" ;;
  *) echo "用法：$0 {setup|start|stop|restart|status|logs}" >&2; exit 2 ;;
esac
