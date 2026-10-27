#!/usr/bin/env bash
set -Eeuo pipefail
[[ $# == 2 ]] || { echo '用法: deploy.sh user@host dist/today-blog-version-build.zip（首次安装）'; exit 1; }
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REMOTE="$(ssh "$1" mktemp -d)"
[[ "$REMOTE" =~ ^/tmp/tmp\.[A-Za-z0-9]+$ ]] || { echo '服务器临时目录格式不受支持'; exit 1; }
rsync -av "$2" "$1:$REMOTE/package.zip"
rsync -av "$ROOT/dist/today-blog-updater.jar" "$1:$REMOTE/updater.jar"
rsync -av "$ROOT/scripts/install-server.sh" "$1:$REMOTE/install-server.sh"
ssh -t "$1" "sudo bash '$REMOTE/install-server.sh' '$REMOTE/package.zip' '$REMOTE/updater.jar'"
echo "上传文件保留在 $REMOTE，确认后可手动删除。后续升级请使用后台管理。"
