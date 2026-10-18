#!/usr/bin/env python3
"""只切换本项目已有发布；健康检查失败自动恢复旧链接。须先完成数据库备份。"""
import argparse
import importlib.util
import json
import os
import sys
sys.dont_write_bytecode=True
from pathlib import Path
import subprocess
import time
import urllib.request
import urllib.parse

BASE=Path('/opt/csms')

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('release');parser.add_argument('--backup-file',required=True);parser.add_argument('--health-url',default='http://127.0.0.1:8081/actuator/health')
    args=parser.parse_args()
    if os.geteuid()!=0: raise SystemExit('必须由项目服务器管理员运行')
    if urllib.parse.urlparse(args.health_url).hostname not in ('127.0.0.1','localhost'):
        raise SystemExit('健康检查仅允许本机回环地址')
    backup=Path(args.backup_file).resolve(strict=True)
    if not backup.is_relative_to(Path('/var/backups/csms')) or not backup.is_file() or backup.stat().st_size==0 or backup.stat().st_mode & 0o077:
        raise SystemExit('须提供本项目权限600的有效备份文件')
    root=(BASE/'releases').resolve(strict=True);new=Path(args.release).resolve(strict=True);old=(BASE/'current').resolve(strict=True)
    if not new.is_relative_to(root) or not old.is_relative_to(root): raise SystemExit('只允许本项目发布目录')
    spec=importlib.util.spec_from_file_location('verify_release',Path(__file__).with_name('verify-release.py'));module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    module.verify(new);module.verify(old)
    def switch(target):
        temporary=BASE/'current.next'
        if temporary.exists() or temporary.is_symlink(): raise RuntimeError('存在未处理的切换临时链接')
        temporary.symlink_to(target);os.replace(temporary,BASE/'current')
        subprocess.run(['systemctl','restart','csms-backend'],check=True)
        for _ in range(90):
            try:
                with urllib.request.urlopen(args.health_url,timeout=2) as response:
                    if response.status==200: return
            except OSError: pass
            time.sleep(1)
        raise RuntimeError('后端健康检查超时')
    try: switch(new)
    except Exception:
        switch(old)
        raise SystemExit('新版本启动失败，已恢复上一发布')
    print(json.dumps({'previous':old.name,'current':new.name,'healthy':True}))

if __name__=='__main__': main()
