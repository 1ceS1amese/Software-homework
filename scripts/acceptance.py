#!/usr/bin/env python3
"""在独立原生 MySQL 实例验收三端写链路；禁止复用任何现有数据库。"""
import argparse
import json
import os
from pathlib import Path
import secrets
import shutil
import signal
import socket
import subprocess
import tempfile
import time
import urllib.error
import urllib.request


PROJECT = Path(__file__).resolve().parent.parent


def free_port():
    with socket.socket() as connection:
        connection.bind(('127.0.0.1', 0))
        return connection.getsockname()[1]


def wait_until(check, children, timeout=90):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if any(child.poll() is not None for child in children):
            raise RuntimeError('验收进程提前退出，请查看忽略目录内的日志')
        try:
            if check():
                return
        except (OSError, urllib.error.URLError):
            pass
        time.sleep(0.25)
    raise RuntimeError('验收服务启动超时')


def ready(url):
    with urllib.request.urlopen(url, timeout=2) as response:
        return response.status == 200


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serve', action='store_true', help='保留隔离服务，供逐功能复验；Ctrl+C 停止')
    parser.add_argument('--skip-build', action='store_true', help='使用已经打包的后端')
    parser.add_argument('--current', action='store_true', help='在本工具已启动的隔离会话中执行指定测试')
    parser.add_argument('playwright_args', nargs=argparse.REMAINDER)
    args = parser.parse_args()
    for tool in ('mysql', 'mysqld', 'java', 'pnpm'):
        if not shutil.which(tool):
            raise SystemExit('请先 source scripts/env.sh 并准备原生工具')
    if args.current:
        state = json.loads((PROJECT / '.local/acceptance/session.json').read_text())
        owner = Path('/proc', str(state['owner_pid']))
        if not owner.exists() or (owner / 'cwd').resolve() != PROJECT:
            raise SystemExit('没有本项目拥有的活动验收会话')
        env = os.environ.copy()
        env.update(state['env'])
        extra = args.playwright_args[1:] if args.playwright_args[:1] == ['--'] else args.playwright_args
        return subprocess.run(['pnpm', 'exec', 'playwright', 'test', '--config',
                               'playwright.acceptance.config.ts', *extra],
                              cwd=PROJECT / 'frontend', env=env).returncode
    directory = PROJECT / '.local/acceptance'
    directory.mkdir(parents=True, exist_ok=True)
    run = Path(tempfile.mkdtemp(prefix='run-', dir=directory))
    run.chmod(0o700)
    db_port, backend_port, frontend_port = free_port(), free_port(), free_port()
    password = secrets.token_hex(24)
    # 保留工具链环境，移除可把 Spring 重定向到既有库/外部配置的继承参数。
    env = {key: value for key, value in os.environ.items()
           if not key.startswith(('SPRING_', 'CSMS_', 'SERVER_', 'VITE_', 'ACCEPTANCE_'))
           and key not in ('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS')}
    # 不读取使用者配置；全部数据库/JWT/监听参数仅指向本次新实例。
    env.update(CSMS_DB_HOST='127.0.0.1', CSMS_DB_PORT=str(db_port),
               CSMS_DB_NAME='csms_acceptance', CSMS_DB_USERNAME='acceptance',
               CSMS_DB_PASSWORD=password, CSMS_JWT_SECRET=secrets.token_hex(48),
               SERVER_ADDRESS='127.0.0.1', SERVER_PORT=str(backend_port),
               CSMS_CORS_ALLOWED_ORIGINS=f'http://127.0.0.1:{frontend_port}',
               VITE_API_TARGET=f'http://127.0.0.1:{backend_port}',
               ACCEPTANCE_BASE_URL=f'http://127.0.0.1:{frontend_port}',
               ACCEPTANCE_BACKEND_URL=f'http://127.0.0.1:{backend_port}')
    children, logs = [], []

    def start(command, name, cwd=PROJECT):
        log = (run / f'{name}.log').open('w')
        logs.append(log)
        child = subprocess.Popen(command, cwd=cwd, env=env, stdout=log,
                                 stderr=subprocess.STDOUT, start_new_session=True)
        children.append(child)
        return child

    def sql(text):
        return subprocess.run(['mysql', '--no-defaults', f'--socket={run / "mysql.sock"}',
                               '-uroot', '--batch', '--skip-column-names'], input=text,
                              text=True, capture_output=True, check=True).stdout

    def stop_signal(_signum, _frame):
        raise KeyboardInterrupt

    signal.signal(signal.SIGTERM, stop_signal)
    try:
        if not args.skip_build:
            with (run / 'build.log').open('w') as log:
                subprocess.run(['bash', './mvnw', '-B', '-ntp', 'package'], cwd=PROJECT / 'backend',
                               env=env, stdout=log, stderr=subprocess.STDOUT, check=True)
        data = run / 'data'
        data.mkdir()
        basedir = Path(shutil.which('mysqld')).resolve().parent.parent
        user = __import__('pwd').getpwuid(os.getuid()).pw_name
        with (run / 'mysql-init.log').open('w') as log:
            subprocess.run(['mysqld', '--no-defaults', '--initialize-insecure', f'--user={user}',
                            f'--basedir={basedir}', f'--datadir={data}'],
                           env=env, stdout=log, stderr=subprocess.STDOUT, check=True)
        start(['mysqld', '--no-defaults', f'--user={user}', f'--basedir={basedir}',
               f'--datadir={data}', '--bind-address=127.0.0.1', f'--port={db_port}',
               '--mysqlx=OFF', '--skip-name-resolve', '--skip-log-bin', f'--socket={run / "mysql.sock"}',
               f'--pid-file={run / "mysql.pid"}', '--character-set-server=utf8mb4'], 'mysql')
        wait_until(lambda: (run / 'mysql.sock').exists() and sql('SELECT 1;').strip() == '1', children)
        sql(f"CREATE DATABASE csms_acceptance CHARACTER SET utf8mb4;\n"
            f"CREATE USER 'acceptance'@'127.0.0.1' IDENTIFIED BY '{password}';\n"
            "GRANT ALL ON csms_acceptance.* TO 'acceptance'@'127.0.0.1';")
        client = run / 'client.cnf'
        client.write_text(f'[client]\nhost=127.0.0.1\nport={db_port}\nuser=acceptance\n'
                          f'password={password}\ndatabase=csms_acceptance\n')
        client.chmod(0o600)
        env['ACCEPTANCE_MYSQL_CONFIG'] = str(client)
        # 每次运行持有独立 JAR 快照，后续打包不能改变正在验收的代码。
        jar = run / 'backend.jar'
        shutil.copy2(PROJECT / 'backend/target/csms-backend-1.0.0-SNAPSHOT.jar', jar)
        start(['java', '-jar', str(jar)], 'backend')
        wait_until(lambda: ready(env['ACCEPTANCE_BACKEND_URL'] + '/actuator/health'), children)
        fixture = (PROJECT / 'frontend/tests/acceptance/fixtures.sql').read_text()
        sql('USE csms_acceptance;\n' + fixture)
        start(['pnpm', 'dev', '--host', '127.0.0.1', '--port', str(frontend_port), '--strictPort'],
              'frontend', PROJECT / 'frontend')
        wait_until(lambda: ready(env['ACCEPTANCE_BASE_URL'] + '/login'), children)
        # 私有会话文件只供本地测试读取，不得提交或输出其中的配置。
        state = {'env': {key: env[key] for key in ('ACCEPTANCE_BASE_URL', 'ACCEPTANCE_BACKEND_URL',
                                                  'ACCEPTANCE_MYSQL_CONFIG')},
                 'run': str(run), 'owner_pid': os.getpid(), 'pids': [child.pid for child in children]}
        session = directory / 'session.json'
        session.write_text(json.dumps(state, indent=2) + '\n')
        session.chmod(0o600)
        print('独立原生 MySQL、后端与前端已就绪；未使用开发库或 Docker。', flush=True)
        if args.serve:
            print('逐功能验收会话已启动；Ctrl+C 结束并停止本次服务。', flush=True)
            while True:
                wait_until(lambda: True, children)
                time.sleep(1)
        else:
            extra = args.playwright_args
            if extra[:1] == ['--']:
                extra = extra[1:]
            result = subprocess.run(['pnpm', 'exec', 'playwright', 'test', '--config',
                                     'playwright.acceptance.config.ts', *extra],
                                    cwd=PROJECT / 'frontend', env=env)
            return result.returncode
    except KeyboardInterrupt:
        return 0
    finally:
        for child in reversed(children):
            if child.poll() is None:
                os.killpg(child.pid, signal.SIGTERM)
                try:
                    child.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    os.killpg(child.pid, signal.SIGKILL)
                    child.wait(timeout=5)
        for log in logs:
            log.close()
        print('本次隔离服务已停止；验收数据库与日志保留在忽略目录。', flush=True)


if __name__ == '__main__':
    raise SystemExit(main())
