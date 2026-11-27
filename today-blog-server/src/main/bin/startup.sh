#!/usr/bin/env bash
set -euo pipefail

#
# Copyright 2017 - 2024 the original author or authors.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program. If not, see [https://www.gnu.org/licenses/]
#

SCRIPT_DIR=$(cd -- "$(dirname -- "$0")" && pwd)
PID_FILE="$SCRIPT_DIR/app.pid"
DATA_ROOT=${BLOG_DATA_ROOT:-}
if [[ -z "$DATA_ROOT" && -r /etc/today-blog/data-root ]]; then
  DATA_ROOT=$(cat /etc/today-blog/data-root)
fi
DATA_ROOT=${DATA_ROOT:-/var/lib/today-blog}

if [[ -e "$PID_FILE" ]]; then
  pid=$(cat "$PID_FILE")
  if [[ "$pid" =~ ^[1-9][0-9]*$ ]] && kill -0 "$pid" 2>/dev/null; then
    echo "today-blog 已在运行，PID: $pid" >&2
    exit 1
  fi
  rm -f -- "$PID_FILE"
fi

install -d -m 755 "$DATA_ROOT/logs"
export BLOG_DATA_ROOT="$DATA_ROOT"
cd -- "$SCRIPT_DIR"
nohup "$SCRIPT_DIR/blog-api" >>"$DATA_ROOT/logs/blog-api.log" 2>&1 </dev/null &

echo $! > "$PID_FILE"
