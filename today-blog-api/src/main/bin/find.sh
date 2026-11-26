#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "$0")" && pwd)
if [[ -f "$SCRIPT_DIR/app.pid" ]]; then
  pid=$(cat "$SCRIPT_DIR/app.pid")
  if [[ "$pid" =~ ^[1-9][0-9]*$ ]] && kill -0 "$pid" 2>/dev/null; then
    ps -p "$pid" -o pid=,ppid=,etime=,command=
    exit 0
  fi
fi

echo 'today-blog 未运行。'
