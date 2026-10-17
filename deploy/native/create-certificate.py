#!/usr/bin/env python3
"""为已授权的演示服务器生成私有 CA 和包含 IP SAN 的 HTTPS 证书。"""
import argparse
import ipaddress
import os
from pathlib import Path
import secrets
import subprocess
import tempfile


def openssl(*args, check=True):
    result = subprocess.run(['openssl', *map(str, args)], capture_output=True, check=False)
    if check and result.returncode:
        raise SystemExit('OpenSSL 执行失败：' + result.stderr.decode(errors='replace'))
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('ip', type=ipaddress.ip_address)
    parser.add_argument('--directory', type=Path, default=Path('/etc/csms/tls'))
    args = parser.parse_args()
    directory = args.directory.absolute()
    if directory.is_symlink() or directory == Path('/'):
        raise SystemExit('证书目录不得是根目录或符号链接')
    os.umask(0o077)
    directory.mkdir(parents=True, exist_ok=True)
    directory.chmod(0o755)
    files = {name: directory / name for name in
             ['ca.key', 'ca.crt', 'server.key', 'server.crt', 'fullchain.pem']}
    if any(path.is_symlink() for path in files.values()):
        raise SystemExit('证书文件不得是符号链接')
    ca_key, ca = files['ca.key'], files['ca.crt']
    if ca_key.exists() != ca.exists():
        raise SystemExit('已有 CA 文件不完整，禁止覆盖')
    if not ca.exists():
        openssl('req', '-x509', '-newkey', 'rsa:3072', '-nodes', '-sha256',
                '-days', '3650', '-subj', '/CN=CSMS Demo Local CA',
                '-addext', 'basicConstraints=critical,CA:TRUE,pathlen:0',
                '-addext', 'keyUsage=critical,keyCertSign,cRLSign',
                '-keyout', ca_key, '-out', ca)
    ca_key.chmod(0o600)
    ca.chmod(0o644)
    openssl('x509', '-in', ca, '-checkend', '2592000', '-noout')
    key, cert = files['server.key'], files['server.crt']
    if key.exists() != cert.exists():
        raise SystemExit('已有服务器证书不完整，禁止覆盖')
    if not cert.exists():
        with tempfile.TemporaryDirectory(prefix='.issuing-', dir=directory) as staging:
            staging = Path(staging)
            new_key, new_cert = staging / 'server.key', staging / 'server.crt'
            csr, extensions = staging / 'request.csr', staging / 'extensions.cnf'
            extensions.write_text(
                'basicConstraints=critical,CA:FALSE\n'
                'keyUsage=critical,digitalSignature,keyEncipherment\n'
                'extendedKeyUsage=serverAuth\n'
                f'subjectAltName=IP:{args.ip}\n'
                'subjectKeyIdentifier=hash\n'
                'authorityKeyIdentifier=keyid,issuer\n')
            openssl('req', '-new', '-newkey', 'rsa:3072', '-nodes', '-sha256',
                    '-subj', f'/CN={args.ip}', '-keyout', new_key, '-out', csr)
            openssl('x509', '-req', '-in', csr, '-CA', ca, '-CAkey', ca_key,
                    '-set_serial', str(secrets.randbits(159) | 1), '-days', '365',
                    '-sha256', '-extfile', extensions, '-out', new_cert)
            openssl('verify', '-CAfile', ca, '-verify_ip', args.ip, new_cert)
            os.replace(new_key, key)
            os.replace(new_cert, cert)
    openssl('verify', '-CAfile', ca, '-verify_ip', args.ip, cert)
    openssl('x509', '-in', cert, '-checkend', '2592000', '-noout')
    key.chmod(0o600)
    cert.chmod(0o644)
    files['fullchain.pem'].write_bytes(cert.read_bytes() + ca.read_bytes())
    files['fullchain.pem'].chmod(0o644)
    print('私有 CA、IP SAN 和证书有效期验证通过；私钥权限为 600。')
    print(openssl('x509', '-in', ca, '-noout', '-fingerprint', '-sha256').stdout.decode().strip())


if __name__ == '__main__':
    main()
