#!/usr/bin/env python3
"""检查 Git 暂存快照；仅输出文件名与问题类型，不输出敏感值。"""
from pathlib import Path
import re
import subprocess
import sys

PROJECT = Path(__file__).resolve().parent.parent


def git(*arguments, input_data=None, check=True):
    return subprocess.run(
        ["git", *arguments], cwd=PROJECT, input=input_data,
        stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=check,
    )


def local_secrets():
    secrets = set()
    for config in PROJECT.glob(".env*"):
        if not config.is_file() or config.name.endswith(".example"):
            continue
        for line in config.read_text(errors="replace").splitlines():
            if line.lstrip().startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            if re.search(r"PASSWORD|SECRET|TOKEN|(?:^|_)KEY$", key, re.I):
                value = value.strip().strip("\"'")
                if len(value) >= 8 and not value.startswith("replace_with_"):
                    secrets.add(value.encode())
    return secrets


def main():
    paths = git("ls-files", "--cached", "-z").stdout
    ignored = git("check-ignore", "--no-index", "--stdin", "-z", input_data=paths, check=False)
    if ignored.returncode not in (0, 1):
        raise SystemExit("隐私检查无法读取 Git 忽略规则。")
    issues = [(path.decode(), "本地忽略文件已被跟踪") for path in ignored.stdout.split(b"\0") if path]
    patterns = [
        ("私钥", rb"-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----"),
        ("GitHub 令牌", rb"(?:gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,})"),
        ("云访问密钥", rb"AKIA[0-9A-Z]{16}"),
        ("API 密钥", rb"sk-(?:proj-|svcacct-)?[A-Za-z0-9_-]{30,}"),
        ("URL 中的账号密码", rb"https?://[^\s/@:]+:[^\s/@]+@"),
        ("本机用户目录", rb"/(?:home/[A-Za-z0-9_.-]+|root/[A-Za-z0-9_.-]+)"),
        ("Windows 本机绝对路径", rb"[A-Za-z]:\\[A-Za-z0-9]"),
    ]
    secrets = local_secrets()
    count = 0
    for raw_path in paths.split(b"\0"):
        if not raw_path:
            continue
        path = raw_path.decode()
        blob = git("show", ":" + path, check=False)
        if blob.returncode:
            issues.append((path, "暂存快照不可读；请先解决合并冲突"))
            continue
        count += 1
        if any(secret in blob.stdout for secret in secrets):
            issues.append((path, "包含本地实际凭据"))
        for label, pattern in patterns:
            if re.search(pattern, blob.stdout):
                issues.append((path, label))
    if issues:
        for path, reason in sorted(set(issues)):
            print(f"拒绝提交：{path}（{reason}）", file=sys.stderr)
        return 1
    print(f"提交隐私检查通过：{count} 个暂存文件；未发现已知凭据、机器路径或被忽略的运行文件。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
