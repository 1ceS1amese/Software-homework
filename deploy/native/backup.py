#!/usr/bin/env python3
"""备份本项目独立 MySQL；可恢复到全新临时库并逐表校验，禁止覆盖现有库。"""
import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import shlex
import subprocess

BASE=Path('/var/backups/csms')
CONFIG=Path('/etc/csms/mysql-root.cnf')
MYSQL=Path('/opt/csms/mysql/bin/mysql')
DUMP=Path('/opt/csms/mysql/bin/mysqldump')
ENV={k:v for k,v in os.environ.items() if not k.startswith('MYSQL_')}
ENV['MYSQL_TEST_LOGIN_FILE']='/etc/csms/disabled-login.cnf'

def sql(statement):
    return subprocess.run([str(MYSQL),f'--defaults-file={CONFIG}','--batch','--raw','--skip-column-names'],
        input=statement,text=True,env=ENV,capture_output=True,check=True).stdout.strip()

def digest(schema):
    tables=sql(f"SELECT table_name FROM information_schema.tables WHERE table_schema='{schema}' AND table_type='BASE TABLE' ORDER BY table_name").splitlines()
    result={}
    for table in tables:
        if not re.fullmatch(r'[a-zA-Z0-9_]+',table):raise ValueError('表名不符合项目约定')
        columns=sql(f"SELECT column_name FROM information_schema.columns WHERE table_schema='{schema}' AND table_name='{table}' ORDER BY ordinal_position").splitlines()
        if any(not re.fullmatch(r'[a-zA-Z0-9_]+',c) for c in columns):raise ValueError('字段名不符合项目约定')
        values=sql('SELECT JSON_ARRAY('+','.join('`'+c+'`' for c in columns)+f') FROM `{schema}`.`{table}`').splitlines()
        raw='\n'.join(sorted(values)).encode()
        result[table]={'rows':len(values),'sha256':hashlib.sha256(raw).hexdigest()}
    return result

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--verify-restore',action='store_true')
    args=parser.parse_args()
    if os.geteuid()!=0:raise SystemExit('必须由项目服务器管理员运行')
    if not CONFIG.is_file() or CONFIG.stat().st_mode & 0o077:raise SystemExit('专用客户端配置缺失或权限不安全')
    if Path(sql('SELECT @@datadir')).resolve()!=Path('/var/lib/csms-mysql').resolve():raise SystemExit('拒绝连接非本项目数据库实例')
    parameters={}
    for line in Path('/etc/csms/backend.env').read_text().splitlines():
        if line.strip() and not line.lstrip().startswith('#') and '=' in line:
            key,value=line.split('=',1);parts=shlex.split(value);parameters[key.strip()]=parts[0] if parts else ''
    schema=parameters.get('CSMS_DB_NAME','')
    if not re.fullmatch(r'csms_[a-zA-Z0-9_]+',schema):raise SystemExit('数据库名称不符合项目命名约定')
    BASE.mkdir(mode=0o700,parents=True,exist_ok=True);BASE.chmod(0o700)
    stamp=datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    output=BASE/f'backup-{stamp}-{secrets.token_hex(3)}.sql'
    # 全表一致性摘要需短暂暂停自己的后端；定时普通备份使用单事务，无需停服务。
    was_active=subprocess.run(['systemctl','is-active','--quiet','csms-backend']).returncode==0
    stopped=False
    clone=None
    try:
        if args.verify_restore and was_active:
            subprocess.run(['systemctl','stop','csms-backend'],check=True);stopped=True
        expected=digest(schema) if args.verify_restore else None
        with output.open('xb') as stream:
            output.chmod(0o600)
            subprocess.run([str(DUMP),f'--defaults-file={CONFIG}','--single-transaction','--routines','--events','--triggers',
                '--hex-blob','--no-tablespaces','--set-gtid-purged=OFF',schema],env=ENV,stdout=stream,stderr=subprocess.PIPE,check=True)
        if stopped:
            subprocess.run(['systemctl','start','csms-backend'],check=True);stopped=False
        verified=False
        if args.verify_restore:
            clone='csms_restore_'+secrets.token_hex(8)
            sql(f'CREATE DATABASE `{clone}` CHARACTER SET utf8mb4')
            with output.open('rb') as stream:
                subprocess.run([str(MYSQL),f'--defaults-file={CONFIG}',clone],stdin=stream,stdout=subprocess.DEVNULL,stderr=subprocess.PIPE,env=ENV,check=True)
            if digest(clone)!=expected:raise RuntimeError('恢复副本与备份时全部表摘要不同')
            verified=True
        receipt={'backup':output.name,'sha256':hashlib.sha256(output.read_bytes()).hexdigest(),'restoreVerified':verified,'tables':expected}
        record=output.with_suffix('.json');record.write_text(json.dumps(receipt,indent=2)+'\n');record.chmod(0o600)
        print(json.dumps({'backup':output.name,'sha256':receipt['sha256'],'restoreVerified':verified,'tableCount':len(expected) if expected else None}))
    finally:
        if clone and re.fullmatch(r'csms_restore_[0-9a-f]{16}',clone):sql(f'DROP DATABASE `{clone}`')
        if stopped:subprocess.run(['systemctl','start','csms-backend'],check=True)

if __name__=='__main__':main()
