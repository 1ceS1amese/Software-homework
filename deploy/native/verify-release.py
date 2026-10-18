#!/usr/bin/env python3
"""校验发布目录清单；兼容已有演示包清单。"""
import hashlib
import json
from pathlib import Path
import sys

def verify(path):
    root=Path(path).resolve(strict=True)
    manifest=json.loads((root/'manifest.json').read_text())
    expected=manifest['files']
    if not isinstance(expected,dict) or 'app.jar' not in expected or 'frontend/index.html' not in expected:
        raise ValueError('发布清单缺少应用入口')
    actual=set()
    for file in root.rglob('*'):
        if file.is_symlink(): raise ValueError('发布目录不得含符号链接')
        if file.is_file(): actual.add(file.relative_to(root).as_posix())
    if actual != set(expected)|{'manifest.json'}: raise ValueError('发布文件缺失或存在清单外文件')
    for name,digest in expected.items():
        file=root/name
        if not file.resolve().is_relative_to(root) or hashlib.sha256(file.read_bytes()).hexdigest()!=digest:
            raise ValueError('发布内容校验失败：'+name)
    return manifest

if __name__=='__main__':
    manifest=verify(sys.argv[1])
    print(json.dumps({'source':manifest['application_source_commit'],'files':len(manifest['files']),'verified':True}))
