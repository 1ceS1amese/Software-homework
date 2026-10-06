#!/usr/bin/env python3
"""为 Arch WSL 安装经过哈希锁定的原生开发工具；不使用 Docker。"""
import concurrent.futures
import hashlib
import json
import os
import platform
from pathlib import Path
import shutil
import subprocess
import sys
import tarfile
import tempfile
import urllib.request

PROJECT = Path(__file__).resolve().parent.parent
TOOLS = PROJECT / ".local/tools"
MANIFEST = json.loads((PROJECT / "scripts/linux-tools.json").read_text())
CACHE = Path(os.environ.get("CSMS_DOWNLOAD_CACHE", str(TOOLS / "downloads")))


def download(item):
    destination = CACHE / item["file"]
    if not destination.exists():
        partial = destination.with_suffix(destination.suffix + ".part")
        request = urllib.request.Request(item["url"], headers={"User-Agent": "csms-bootstrap"})
        with urllib.request.urlopen(request, timeout=120) as response, partial.open("wb") as output:
            shutil.copyfileobj(response, output)
        partial.replace(destination)
    with destination.open("rb") as source:
        actual = hashlib.file_digest(source, "sha256").hexdigest()
    if actual != item["sha256"]:
        raise RuntimeError(f"SHA-256 校验失败：{destination.name}")
    print(f"校验通过：{destination.name}", flush=True)
    return item, destination


def main():
    if sys.version_info < (3, 12):
        raise SystemExit("引导器需要 Python 3.12 或更高版本。")
    if platform.system() != "Linux" or platform.machine() != "x86_64":
        raise SystemExit("此引导器仅支持 Arch Linux x86_64 / Arch WSL。其他系统请按开发指南安装工具。")
    version = tuple(map(int, platform.libc_ver()[1].split(".")[:2]))
    if version < (2, 42):
        raise SystemExit("本引导器的原生运行库要求 glibc >= 2.42；禁止自动升级系统库。")
    for command in ["tar", "ar", "fc-cache"]:
        if not shutil.which(command):
            raise SystemExit(f"缺少 {command}，请按 docs/12 的系统依赖说明准备。")
    TOOLS.mkdir(parents=True, exist_ok=True)
    CACHE.mkdir(parents=True, exist_ok=True)
    libraries = TOOLS / "native-libs"
    libraries.mkdir(exist_ok=True)
    # 下载并行，写入工具目录顺序执行，避免覆盖共享运行库时产生竞争。
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        downloaded = list(pool.map(download, MANIFEST["artifacts"]))
    for item, archive in downloaded:
        if item["kind"] == "tool":
            with tarfile.open(archive) as contents:
                contents.extractall(TOOLS, filter="data")
        elif item["kind"] == "library":
            subprocess.run(["tar", "-xf", str(archive), "-C", str(libraries), "--wildcards", "usr/lib/*"], check=True)
        elif item["kind"] == "deb-library":
            with tempfile.TemporaryDirectory(prefix="csms-library-") as directory:
                subprocess.run(["ar", "x", str(archive)], cwd=directory, check=True)
                data = next(Path(directory).glob("data.tar.*"))
                subprocess.run(["tar", "-xf", str(data), "-C", str(libraries)], check=True)
        elif item["kind"] == "font":
            directory = Path.home() / ".local/share/fonts/csms"
            directory.mkdir(parents=True, exist_ok=True)
            shutil.copy2(archive, directory / archive.name)
            subprocess.run(["fc-cache", "-f", str(directory)], check=True)
    node = next(TOOLS.glob("node-*/bin"))
    environment = os.environ.copy()
    environment["PATH"] = str(node) + os.pathsep + environment["PATH"]
    subprocess.run([str(node / "npm"), "install", "--global", "--prefix", str(TOOLS / "pnpm"),
                    "--ignore-scripts", "pnpm@" + MANIFEST["pnpm"]], env=environment, check=True)
    print("原生工具已安装到 .local/tools。下一步：./scripts/dev.sh setup && ./scripts/dev.sh start")


if __name__ == "__main__":
    main()
