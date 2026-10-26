# 打包、部署和后台升级

API 使用 systemd，升级器位于独立 Gradle 模块 `blog-updater`，生产代码仅依赖 JDK。
`blog-api` 依赖该模块复用升级包校验和磁盘协议，升级器不依赖 API 或其 Web、数据库框架。
可单独运行 `./gradlew :blog-updater:build` 构建并测试，产物为
`blog-updater/build/libs/today-blog-updater.jar`，通过 `java -jar` 启动。
通过独立的 `today-blog-updater@<UUID>.service` 执行，不随 API 重启终止。

## 构建

构建机需要 JDK 17+、pnpm 10、Python 3、OpenSSL。服务器建议 JDK 25。

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out upgrade-private.pem
openssl pkey -in upgrade-private.pem -pubout -out upgrade-public.pem
chmod 600 upgrade-private.pem

BLOG_UPGRADE_PRIVATE_KEY=$PWD/upgrade-private.pem \
ROLLBACK_SAFE=true bash scripts/package.sh
```

私钥保存到仓库外，不提交、不部署。`ROLLBACK_SAFE=true` 是发布者对数据库迁移
向后兼容的声明，不是自动检测；不可逆迁移必须人工维护部署。签名同时覆盖版本、
构建编号和所有文件摘要。`BUILD_NUMBER` 必须单调递增（默认 UTC 时间戳）。

## 首次部署

Linux 服务器需要 systemd、JDK（含 jshell）、sudo >= 1.9.10、Bash、rsync。
部署用户需要 SSH 和首次安装 sudo 权限。数据库需自行创建并备份。

```bash
BLOG_UPGRADE_PUBLIC_KEY=$PWD/upgrade-public.pem \
bash scripts/deploy.sh deploy@server dist/today-blog-<version>-<build>.zip
```

安装脚本不会启动服务。先创建 `/etc/today-blog/today-blog.env`（建议 root:root、0600），
根据项目配置设置数据库、附件绝对路径和 multipart 限制（包最大 512 MiB，请求预留余量）。
默认 prod profile，API 工作目录 `/var/lib/today-blog`，运行时数据不得写入 release。
Java 可通过 JAVA_HOME 配置 API；updater 默认使用 `/usr/bin/java`。
配置文件不得存储在版本目录；需要时通过 `JAVA_OPTS` 指定外部配置。

反向代理静态目录：

- Console：`/opt/today-blog/current/console`
- Desktop：`/opt/today-blog/current/desktop`
- API：代理到 `127.0.0.1:8080`
- 必须拒绝外部访问 `/api/internal/health`；绑定本机 API 或用防火墙阻止公网直连端口。
- 上传请求大小、超时需与后端保持一致。

```bash
sudo systemctl start today-blog.service
curl --fail http://127.0.0.1:8080/api/internal/health
```

正常返回 `UP:<build>`。确认 API 和两套静态页面正常后再使用在线升级。

## 后台升级

系统 → 系统升级 → 上传 ZIP → 校验通过 → 开始升级。
任务 ID 保存在浏览器本地，重载页面后继续查询。重启可能导致登录失效，需重新登录。

目录与权限：

```text
/opt/today-blog/                         root 所有，API 只读
  current -> releases/<version>-<build>
  releases/                            新旧版本并存
  highest-build.properties             成功发布的最高构建号
/usr/local/lib/today-blog/updater.jar    固定、root 所有，不随在线包更新
/var/lib/today-blog-upgrade/
  incoming/<UUID>/                      today 写，上传包与元信息
  results/<UUID>.properties             root 写，API 读
/var/lib/today-blog/                     today 写，业务运行数据
```

API 只能 sudo 启动 UUID 命名的升级 unit，没有 shell/java/任意 systemctl 权限。
**API unit 不能设置 `NoNewPrivileges=true`，否则 sudo 无法工作。**
升级器持有全局文件锁，将包复制到 root 私有临时目录后再次验签，再安装到新目录，
原子切换 current、重启 API，最长等待 90 秒验证数据库及构建号。
失败时切回旧版本并再次检查；数据库不回滚。

## 运维与边界

```bash
journalctl -u today-blog.service
journalctl -u today-blog-updater@<UUID>.service
cat /var/lib/today-blog-upgrade/results/<UUID>.properties
```

自动回滚仅覆盖可捕获的安装、启动、健康检查失败。
断电、SIGKILL、磁盘写满可能留下中间状态，当前不提供崩溃后自动恢复或后台手动回滚按钮。
此时先确认升级 unit 已停止，再人工将 current 原子切回已知正常的 release，重启 API 并检查健康状态。
不要将程序回滚误认为数据库恢复。不要删除正在运行或可能用于回滚的版本目录。
上传包、失败版本和历史版本暂不自动清理，由管理员定期按保留策略清理。
在线包不能修改 updater、公钥或 systemd unit；这些受信任组件升级需 SSH 人工维护。

发布前必须在独立 Linux/systemd 环境验收正常升级、启动失败回滚、权限、代理、附件保留。
本地 JVM 测试不能代替真实服务器验收。
