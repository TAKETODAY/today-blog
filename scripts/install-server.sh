#!/usr/bin/env bash
# 首次安装，在 Linux 服务器以 root 执行。不保存数据库密码，不覆盖现有配置。
set -Eeuo pipefail
[[ $EUID == 0 ]] || { echo '请以 root 执行'; exit 1; }
[[ $# == 2 ]] || { echo '用法: install-server.sh package.zip cli.jar 或 install-server.sh --runtime-only cli.jar'; exit 1; }
ROOT=/opt/today-blog
STATE=/var/lib/today-blog-upgrade
if [[ $1 != --runtime-only ]]; then
  [[ ! -e "$ROOT/current" ]] || { echo '已有安装，请使用后台升级'; exit 1; }
fi
id today >/dev/null 2>&1 || useradd --system --home /var/lib/today-blog --shell /usr/sbin/nologin today
install -d -m 755 "$ROOT/releases" "$STATE" /etc/today-blog /usr/local/lib/today-blog /var/lib/today-blog
install -d -m 755 /var/backups/today-blog /var/www/today-blog
# 前端映射目标白名单由 root 管理；管理端只能在此目录内选择目标。
if [[ ! -e /etc/today-blog/release-mapping-roots ]]; then
  printf '/var/www/today-blog\n' > /etc/today-blog/release-mapping-roots
  chmod 644 /etc/today-blog/release-mapping-roots
fi
install -d -m 700 -o today -g today "$STATE/packages"
touch "$STATE/updater.log"
chmod 600 "$STATE/updater.log"
chown today:today /var/lib/today-blog
install -m 644 "$2" /usr/local/lib/today-blog/cli.jar
# 首次包校验文件完整性后安装；包必须来自可信渠道，不运行包内的任何 root 脚本。
WORK=''
SUDOERS=''
trap '[[ -z "$WORK" ]] || rm -rf -- "$WORK"; [[ -z "$SUDOERS" ]] || rm -f -- "$SUDOERS"' EXIT
if [[ $1 != --runtime-only ]]; then
  WORK="$(mktemp -d "$ROOT/releases/.initial-XXXXXX")"
  cp "$1" "$WORK/package.zip"
  /usr/bin/java -jar /usr/local/lib/today-blog/cli.jar prepare-release "$WORK/package.zip" "$WORK/release"
  [[ -f "$WORK/release/release.properties" ]] || exit 1
  chmod -R a+rX "$WORK/release"
  INITIAL="initial-$(date -u +%Y%m%d%H%M%S)"
  mv "$WORK/release" "$ROOT/releases/$INITIAL"
  ln -s "$ROOT/releases/$INITIAL" "$ROOT/current"
fi
cat > /usr/local/lib/today-blog/launch-api <<'EOF'
#!/bin/bash
set -euo pipefail
exec /usr/bin/setsid /usr/bin/setpriv --reuid=today --regid=today --init-groups /bin/bash -c '
  export HOME=/var/lib/today-blog USER=today LOGNAME=today
  export JAVA_OPTS=-Dinfra.profiles.active=prod
  if [[ -r /etc/today-blog/today-blog.env ]]; then
    set -a
    source /etc/today-blog/today-blog.env
    set +a
  fi
  exec "$1"
' launch-api "$1"
EOF
cat > /usr/local/bin/today-blog <<'EOF'
#!/bin/bash
set -euo pipefail
if [[ $# != 0 && $1 != --help && $EUID != 0 ]]; then
  echo '请以 root 执行运维命令' >&2
  exit 2
fi
umask 022
exec /usr/bin/java -jar /usr/local/lib/today-blog/cli.jar "$@"
EOF
cat > /usr/local/lib/today-blog/updater <<'EOF'
#!/bin/bash
set -euo pipefail
[[ $EUID == 0 && $# == 1 && $1 =~ ^[1-9][0-9]{0,18}$ ]] || exit 2
cd /usr/local/lib/today-blog
exec /usr/local/bin/today-blog upgrade --task-id "$1" >>/var/lib/today-blog-upgrade/updater.log 2>&1
EOF
cat > /usr/local/lib/today-blog/today-updater <<'EOF'
#!/bin/bash
set -euo pipefail
[[ $# == 1 && $1 =~ ^[1-9][0-9]{0,18}$ ]] || exit 2
exec /usr/bin/sudo -n -- /usr/local/lib/today-blog/updater "$1"
EOF
chmod 755 /usr/local/lib/today-blog/{launch-api,updater,today-updater} /usr/local/bin/today-blog
# sudo >= 1.9.10 支持参数正则；固定命令与任务 ID，不允许任意路径或 shell。
SUDOERS="$(mktemp)"
cat > "$SUDOERS" <<'EOF'
today ALL=(root) NOPASSWD: NOSETENV: /usr/local/lib/today-blog/updater ^[1-9][0-9]{0,18}$
EOF
visudo -cf "$SUDOERS"
install -m 440 "$SUDOERS" /etc/sudoers.d/today-blog-upgrade
echo '安装完成。配置 /etc/today-blog/today-blog.env、数据库和反向代理后，执行 sudo today-blog start'
