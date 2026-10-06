#!/usr/bin/env bash
set -euo pipefail
source "$(dirname -- "${BASH_SOURCE[0]}")/env.sh"
case "${1:-all}" in
  all)
    (cd "$PROJECT_DIR/backend" && bash ./mvnw -B -ntp test)
    (cd "$PROJECT_DIR/frontend" && pnpm test && pnpm test:e2e)
    ;;
  backend) shift; (cd "$PROJECT_DIR/backend" && bash ./mvnw -B -ntp test "$@") ;;
  unit) shift; (cd "$PROJECT_DIR/frontend" && pnpm test "$@") ;;
  e2e) shift; (cd "$PROJECT_DIR/frontend" && pnpm test:e2e "$@") ;;
  live)
    shift
    [[ -f "$PROJECT_DIR/.env.local" ]] || { echo "缺少本地环境，请先 setup/start。" >&2; exit 1; }
    # 真实链路测试只允许托管的本地实例，不接受任意外部数据库。
    source "$PROJECT_DIR/.env.local"
    [[ "${CSMS_MANAGE_DB:-1}" == 1 && "$CSMS_DB_HOST" == 127.0.0.1 ]] || { echo "禁止对外部数据库运行真实链路测试。" >&2; exit 1; }
    python3 - "$PROJECT_DIR" "$CSMS_DB_HOST" "$CSMS_DB_PORT" "$CSMS_DB_NAME" <<'PY'
import sys
from pathlib import Path
project = Path(sys.argv[1])
try:
    for service, directory in [("mysql", project / ".local/mysql/data"), ("backend", project / "backend")]:
        pid = (project / ".local/run" / (service + ".pid")).read_text().strip()
        if not pid.isdigit() or Path("/proc", pid, "cwd").resolve() != directory:
            raise ValueError("PID 不属于本地项目")
        if service == "backend":
            env = dict(item.split(b"=", 1) for item in Path("/proc", pid, "environ").read_bytes().split(b"\0") if b"=" in item)
            for key, expected in zip([b"CSMS_DB_HOST", b"CSMS_DB_PORT", b"CSMS_DB_NAME"], sys.argv[2:]):
                if env.get(key) != expected.encode():
                    raise ValueError("后端数据库配置不匹配")
except (OSError, ValueError) as error:
    raise SystemExit("拒绝 live 测试：必须由本项目启动本地 MySQL 与后端，且配置匹配。") from error
PY
    curl -fsS http://127.0.0.1:8081/actuator/health >/dev/null
    (cd "$PROJECT_DIR/frontend" && pnpm test:e2e:live "$@")
    ;;
  *) echo "用法：$0 {all|backend|unit|e2e|live} [测试参数]" >&2; exit 2 ;;
esac
