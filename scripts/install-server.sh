#!/usr/bin/env bash
# 首次安装，在 Linux 服务器以 root 执行。不保存数据库密码，不覆盖现有配置。
set -Eeuo pipefail
[[ $EUID == 0 ]] || { echo '请以 root 执行'; exit 1; }
[[ $# == 2 ]] || { echo '用法: install-server.sh package.zip updater.jar'; exit 1; }
ROOT=/opt/today-blog
STATE=/var/lib/today-blog-upgrade
[[ ! -e "$ROOT/current" ]] || { echo '已有安装，请使用后台升级'; exit 1; }
id today >/dev/null 2>&1 || useradd --system --home /var/lib/today-blog --shell /usr/sbin/nologin today
install -d -m 755 "$ROOT/releases" "$STATE/results" /etc/today-blog /usr/local/lib/today-blog /var/lib/today-blog
install -d -m 700 -o today -g today "$STATE/incoming"
chown today:today /var/lib/today-blog
install -m 644 "$2" /usr/local/lib/today-blog/updater.jar
# 首次包校验文件完整性后安装；包必须来自可信渠道，不运行包内的任何 root 脚本。
WORK="$(mktemp -d "$ROOT/releases/.initial-XXXXXX")"
trap 'rm -rf "$WORK"' EXIT
cp "$1" "$WORK/package.zip"
# 使用与在线升级完全相同的 Java 校验器（JShell 为 JDK 标准工具）。
jshell --class-path /usr/local/lib/today-blog/updater.jar - <<EOF
import java.nio.file.*;
import cn.taketoday.blog.updater.*;
try {
 var manifest = UpgradePackage.verify(Path.of("$WORK/package.zip"));
 var target = Files.createDirectory(Path.of("$WORK/release"));
 UpgradePackage.extract(Path.of("$WORK/package.zip"), target, manifest);
 var properties = new java.util.Properties();
 properties.setProperty("version", manifest.version());
 properties.setProperty("build", Long.toString(manifest.build()));
 UpgradeFiles.write(target.resolve("release.properties"), properties);
} catch (Exception error) { error.printStackTrace(); System.exit(1); }
/exit
EOF
[[ -f "$WORK/release/release.properties" ]] || exit 1
chmod 755 "$WORK/release/api/bin/blog-api"
chmod -R a+rX "$WORK/release"
INITIAL="initial-$(date -u +%Y%m%d%H%M%S)"
mv "$WORK/release" "$ROOT/releases/$INITIAL"
ln -s "$ROOT/releases/$INITIAL" "$ROOT/current"
cat > /usr/local/lib/today-blog/start-api <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
export BLOG_RELEASE_PATH="$(readlink -f /opt/today-blog/current)"
cd /var/lib/today-blog
exec "$BLOG_RELEASE_PATH/api/bin/blog-api" "$@"
EOF
chmod 755 /usr/local/lib/today-blog/start-api
cat > /etc/systemd/system/today-blog.service <<'EOF'
[Unit]
Description=TODAY Blog API
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=today
Group=today
WorkingDirectory=/var/lib/today-blog
Environment=JAVA_OPTS=-Dinfra.profiles.active=prod
EnvironmentFile=-/etc/today-blog/today-blog.env
ExecStart=/usr/local/lib/today-blog/start-api
Restart=on-failure
RestartSec=5
TimeoutStopSec=30
PrivateTmp=true

[Install]
WantedBy=multi-user.target
EOF
cat > /etc/systemd/system/today-blog-updater@.service <<'EOF'
[Unit]
Description=TODAY Blog upgrade %i

[Service]
Type=oneshot
UMask=0022
ExecStart=/usr/bin/java -jar /usr/local/lib/today-blog/updater.jar %i
TimeoutStartSec=600
PrivateTmp=true
ProtectHome=true
ProtectSystem=strict
ReadWritePaths=/opt/today-blog /var/lib/today-blog-upgrade/results
EOF
# sudo >= 1.9.10 支持参数正则；固定命令与 UUID，不允许任意路径或 shell。
SUDOERS="$(mktemp)"
trap 'rm -rf "$WORK"; rm -f "$SUDOERS"' EXIT
cat > "$SUDOERS" <<'EOF'
today ALL=(root) NOPASSWD: /usr/bin/systemctl ^start --no-block today-blog-updater@[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\.service$
EOF
visudo -cf "$SUDOERS"
install -m 440 "$SUDOERS" /etc/sudoers.d/today-blog-upgrade
systemctl daemon-reload
systemctl enable today-blog.service
echo '首次安装完成。配置 /etc/today-blog/today-blog.env、数据库和反向代理后，执行 systemctl start today-blog.service'
