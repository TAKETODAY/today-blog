#!/usr/bin/env bash
# 首次安装或更新运维入口，在 Linux 服务器以 root 执行。
set -Eeuo pipefail
[[ $EUID == 0 ]] || { echo '请以 root 执行'; exit 1; }
MODE_FILE=/etc/today-blog/service-manager
MODE=''
case ${1:-} in
  --direct|--systemd) MODE=${1#--}; shift ;;
esac
if [[ $# != 2 ]]; then
  echo '用法: install-server.sh [--direct|--systemd] {package.zip|--runtime-only} cli.jar' >&2
  exit 2
fi
if [[ -z "$MODE" && $1 == --runtime-only && -f "$MODE_FILE" ]]; then
  MODE=$(cat "$MODE_FILE")
fi
MODE=${MODE:-direct}
if [[ "$MODE" != direct && "$MODE" != systemd ]]; then
  echo '服务管理方式必须为 direct 或 systemd' >&2
  exit 2
fi
ROOT=/opt/today-blog
STATE=/var/lib/today-blog-upgrade
if [[ $1 != --runtime-only ]]; then
  [[ ! -e "$ROOT/current" ]] || { echo '已有安装，请使用后台升级'; exit 1; }
fi
if [[ "$MODE" == systemd ]]; then
  if ! /usr/bin/systemctl --system show --property=Version --value >/dev/null; then
    echo '当前系统没有可用的 systemd 服务管理器' >&2
    exit 1
  fi
  if [[ -e "$ROOT/run/blog-api.pid" || -L "$ROOT/run/blog-api.pid" ]]; then
    echo '请先用原来的直接进程模式执行 today-blog stop，再切换到 systemd' >&2
    exit 1
  fi
elif [[ -x /usr/bin/systemctl ]]; then
  SERVICE_STATE=$(/usr/bin/systemctl --system show -p ActiveState --value today-blog.service 2>/dev/null || true)
  case "$SERVICE_STATE" in
    active|activating|reloading|deactivating|refreshing)
      echo '请先停止 today-blog.service，再切换到直接进程模式' >&2
      exit 1
      ;;
  esac
  if /usr/bin/systemctl --system is-enabled --quiet today-blog.service 2>/dev/null; then
    echo '请先禁用 today-blog.service 的开机自启，再切换到直接进程模式' >&2
    exit 1
  fi
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
exec /usr/bin/setsid /usr/bin/setpriv --reuid=today --regid=today --init-groups \
  /usr/local/lib/today-blog/start-api "$1"
EOF
cat > /usr/local/lib/today-blog/start-api <<'EOF'
#!/bin/bash
set -euo pipefail
export HOME=/var/lib/today-blog USER=today LOGNAME=today
export JAVA_OPTS=-Dinfra.profiles.active=prod
if [[ -r /etc/today-blog/today-blog.env ]]; then
  set -a
  source /etc/today-blog/today-blog.env
  set +a
fi
if [[ $# == 1 ]]; then
  exec "$1"
fi
export BLOG_INSTALL_PATH=/opt/today-blog BLOG_UPGRADE_STATE=/var/lib/today-blog-upgrade
BLOG_RELEASE_PATH=$(/usr/bin/readlink -e "$BLOG_INSTALL_PATH/current")
[[ ${BLOG_RELEASE_PATH%/*} == "$BLOG_INSTALL_PATH/releases" ]]
export BLOG_RELEASE_PATH
exec "$BLOG_RELEASE_PATH/api/bin/blog-api"
EOF
cat > /usr/local/bin/today-blog <<'EOF'
#!/bin/bash
set -euo pipefail
if [[ $# != 0 && $1 != --help && $EUID != 0 ]]; then
  echo '请以 root 执行运维命令' >&2
  exit 2
fi
umask 022
export BLOG_SERVICE_MANAGER=direct
if [[ -f /etc/today-blog/service-manager ]]; then
  BLOG_SERVICE_MANAGER=$(/usr/bin/cat /etc/today-blog/service-manager)
fi
exec /usr/bin/java -jar /usr/local/lib/today-blog/cli.jar "$@"
EOF
cat > /usr/local/lib/today-blog/updater <<'EOF'
#!/bin/bash
set -euo pipefail
[[ $EUID == 0 && $# == 1 && $1 =~ ^[1-9][0-9]{0,18}$ ]] || exit 2
cd /usr/local/lib/today-blog
if [[ -f /etc/today-blog/service-manager && $(/usr/bin/cat /etc/today-blog/service-manager) == systemd ]]; then
  # 等待 oneshot 退出，API 才能判断升级器是否在接管任务前失败。
  exec /usr/bin/systemctl --system --no-pager start "today-blog-updater@$1.service" >>/var/lib/today-blog-upgrade/updater.log 2>&1
fi
exec /usr/local/bin/today-blog upgrade --task-id "$1" >>/var/lib/today-blog-upgrade/updater.log 2>&1
EOF
cat > /usr/local/lib/today-blog/today-updater <<'EOF'
#!/bin/bash
set -euo pipefail
[[ $# == 1 && $1 =~ ^[1-9][0-9]{0,18}$ ]] || exit 2
exec /usr/bin/sudo -n -- /usr/local/lib/today-blog/updater "$1"
EOF
chmod 755 /usr/local/lib/today-blog/{launch-api,start-api,updater,today-updater} /usr/local/bin/today-blog
SYSTEMD_AVAILABLE=0
if /usr/bin/systemctl --system show --property=Version --value >/dev/null 2>&1; then
  SYSTEMD_AVAILABLE=1
fi
if [[ "$MODE" == systemd || ( "$MODE" == direct && "$SYSTEMD_AVAILABLE" == 1 ) ]]; then
  cat > /etc/systemd/system/today-blog.service <<'EOF'
[Unit]
Description=TODAY Blog API
Wants=network-online.target
After=network-online.target

[Service]
Type=exec
User=today
Group=today
WorkingDirectory=/var/lib/today-blog
ExecCondition=/usr/bin/grep -qx systemd /etc/today-blog/service-manager
ExecStart=/usr/local/lib/today-blog/start-api
Restart=on-failure
RestartSec=5s
TimeoutStopSec=30s
PrivateTmp=true

[Install]
WantedBy=multi-user.target
EOF
  cat > /etc/systemd/system/today-blog-updater@.service <<'EOF'
[Unit]
Description=TODAY Blog upgrade task %i

[Service]
Type=oneshot
WorkingDirectory=/usr/local/lib/today-blog
ExecCondition=/usr/bin/grep -qx systemd /etc/today-blog/service-manager
ExecStart=/usr/local/bin/today-blog upgrade --task-id %i
TimeoutStartSec=2h
PrivateTmp=true
EOF
  /usr/bin/systemctl daemon-reload
fi
# 同一个 root 配置供终端 CLI 和后台任务选择服务管理方式，不从 API 继承。
printf '%s\n' "$MODE" | install -m 644 /dev/stdin "$MODE_FILE"
# sudo >= 1.9.10 支持参数正则；固定命令与任务 ID，不允许任意路径或 shell。
SUDOERS="$(mktemp)"
cat > "$SUDOERS" <<'EOF'
today ALL=(root) NOPASSWD: NOSETENV: /usr/local/lib/today-blog/updater ^[1-9][0-9]{0,18}$
EOF
visudo -cf "$SUDOERS"
install -m 440 "$SUDOERS" /etc/sudoers.d/today-blog-upgrade
if [[ "$MODE" == systemd ]]; then
  echo 'systemd 模式：API 日志使用 journalctl -u today-blog.service；如需开机自启，配置完成后执行 systemctl enable today-blog.service'
fi
echo '安装完成。配置 /etc/today-blog/today-blog.env、数据库和反向代理后，执行 sudo today-blog install --wizard'
echo '向导完成后执行 sudo today-blog start；也可以直接编辑配置文件后跳过向导。'
