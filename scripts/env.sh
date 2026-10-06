#!/usr/bin/env bash
# 由其他脚本 source；仅选择本工程的原生工具和运行库。
PROJECT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)"
for tool_dir in "$PROJECT_DIR"/.local/tools/node-*/bin "$PROJECT_DIR"/.local/tools/apache-maven-*/bin "$PROJECT_DIR"/.local/tools/jdk-*/bin "$PROJECT_DIR"/.local/tools/mysql-*/bin "$PROJECT_DIR"/.local/tools/pnpm/bin; do
  if [[ -d "$tool_dir" ]]; then export PATH="$tool_dir:$PATH"; fi
done
for jdk_dir in "$PROJECT_DIR"/.local/tools/jdk-*; do
  if [[ -d "$jdk_dir/bin" ]]; then export JAVA_HOME="$jdk_dir"; fi
done
if [[ -d "$PROJECT_DIR/.local/tools/native-libs/usr/lib" ]]; then
  export LD_LIBRARY_PATH="$PROJECT_DIR/.local/tools/native-libs/usr/lib${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
fi
if [[ -d "$PROJECT_DIR/.local/tools/native-libs/lib/x86_64-linux-gnu" ]]; then
  export LD_LIBRARY_PATH="$PROJECT_DIR/.local/tools/native-libs/lib/x86_64-linux-gnu${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
fi
