#!/usr/bin/env bash
# 兼容入口；实际构建及打包由 Gradle release 任务完成。
set -Eeuo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
exec ./gradlew release "$@"
