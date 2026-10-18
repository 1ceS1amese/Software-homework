#!/usr/bin/env python3
"""从干净 Git 提交构建可核验发布包；不读取或打包本地环境。"""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import tarfile
import tempfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent

def run(args, cwd=ROOT, **kwargs):
    return subprocess.run(args, cwd=cwd, check=True, **kwargs)

def main():
    os.chdir(ROOT)
    if subprocess.check_output(['git', 'status', '--porcelain'], text=True).strip():
        raise SystemExit('发布必须来自干净提交，请先完成检查并提交项目变更。')
    source = subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip()
    tree = subprocess.check_output(['git', 'rev-parse', 'HEAD^{tree}'], text=True).strip()
    version = ET.parse(ROOT / 'backend/pom.xml').getroot().find('{*}version').text
    if version != json.loads((ROOT / 'frontend/package.json').read_text())['version']:
        raise SystemExit('前后端版本不一致')
    env = {k: v for k, v in os.environ.items() if not k.startswith(('CSMS_', 'SPRING_', 'VITE_'))}
    run(['bash', './mvnw', '-B', '-ntp', f'-Dcsms.source={source}', 'package'], ROOT / 'backend', env=env)
    run(['pnpm', 'install', '--frozen-lockfile', '--lockfile-only', '--offline', '--ignore-scripts'], ROOT / 'frontend', env=env)
    run(['pnpm', 'build'], ROOT / 'frontend', env=env)
    output = ROOT / '.local/releases'
    output.mkdir(parents=True, exist_ok=True); output.chmod(0o700)
    release_name = f'{version}-{source[:12]}'
    archive = output / f'{release_name}.tar.gz'
    if archive.exists(): raise SystemExit('同一源码发布包已存在；必须先核对旧产物，禁止静默覆盖。')
    with tempfile.TemporaryDirectory(prefix='package-', dir=output) as temp:
        package = Path(temp)
        shutil.copy2(ROOT / 'backend/target/csms-backend.jar', package / 'app.jar')
        shutil.copytree(ROOT / 'frontend/dist', package / 'frontend')
        (package / 'frontend/release.json').write_text(json.dumps({'version': version, 'source': source})+'\n')
        tracked=subprocess.check_output(['git','ls-files','-z','--','deploy/native']).decode().split('\0')
        for name in filter(None,tracked):
            original=ROOT/name
            if original.is_symlink():raise SystemExit('配置模板禁止符号链接')
            target=package/'native'/original.relative_to(ROOT/'deploy/native')
            target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(original,target)
        files = {}
        for file in sorted(package.rglob('*')):
            if file.is_symlink(): raise SystemExit('发布包禁止符号链接')
            if file.is_file(): files[file.relative_to(package).as_posix()] = hashlib.sha256(file.read_bytes()).hexdigest()
        manifest = {'version': version, 'application_source_commit': source, 'source_tree': tree, 'files': files}
        (package / 'manifest.json').write_text(json.dumps(manifest, indent=2)+'\n')
        with tarfile.open(archive, 'w:gz') as tar:
            for file in sorted(package.rglob('*')):
                if file.is_file():
                    info = tar.gettarinfo(str(file), file.relative_to(package).as_posix())
                    info.uid=info.gid=0; info.uname=info.gname=''; info.mode=0o644
                    with file.open('rb') as stream: tar.addfile(info, stream)
    archive.chmod(0o600)
    digest=hashlib.sha256(archive.read_bytes()).hexdigest()
    (output / f'{release_name}.sha256').write_text(f'{digest}  {archive.name}\n')
    print(f'发布包：{archive.name}，{len(files)} 个受清单保护文件，SHA-256 {digest}')

if __name__ == '__main__': main()
