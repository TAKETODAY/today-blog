#!/usr/bin/env bash
set -Eeuo pipefail
MODE=--direct
case ${1:-} in
  --direct|--systemd) MODE=$1; shift ;;
esac
[[ $# == 2 ]] || { echo '用法: deploy.sh [--direct|--systemd] user@host build/today-blog-version-build.zip（首次安装）'; exit 1; }
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REMOTE="$(ssh "$1" mktemp -d)"
[[ "$REMOTE" =~ ^/tmp/tmp\.[A-Za-z0-9]+$ ]] || { echo '服务器临时目录格式不受支持'; exit 1; }
rsync -av "$2" "$1:$REMOTE/package.zip"
rsync -av "$ROOT/build/dist/today-blog-cli.jar" "$1:$REMOTE/cli.jar"
rsync -av "$ROOT/scripts/install-server.sh" "$1:$REMOTE/install-server.sh"
ssh -t "$1" "sudo bash '$REMOTE/install-server.sh' '$MODE' '$REMOTE/package.zip' '$REMOTE/cli.jar'"
echo "上传文件保留在 $REMOTE，确认后可手动删除。后续升级请使用后台管理。"
