#!/usr/bin/env bash
set -euo pipefail
source "$(dirname -- "${BASH_SOURCE[0]}")/env.sh"
cd "$PROJECT_DIR"
git -c core.whitespace=blank-at-eol,blank-at-eof,space-before-tab,cr-at-eol diff --check
for script in scripts/*.sh; do bash -n "$script"; done
(cd frontend && pnpm typecheck && pnpm build)
echo "格式、Shell 语法、TypeScript 与前端构建检查通过。"
