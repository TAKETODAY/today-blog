# 打包、部署和后台升级

API 使用 systemd，升级器位于独立 Gradle 模块 `blog-updater`，生产代码仅依赖 JDK。
`blog-api` 依赖该模块复用升级包校验和磁盘协议，升级器不依赖 API 或其 Web、数据库框架。
可单独运行 `./gradlew :blog-updater:build` 构建并测试，产物为
`blog-updater/build/libs/today-blog-updater.jar`，通过 `java -jar` 启动。
通过独立的 `today-blog-updater@<UUID>.service` 执行，不随 API 重启终止。

## 构建

构建机需要 JDK 17+、pnpm 10。服务器建议 JDK 25。当前版本不使用密钥、证书或数字签名。

```bash
./gradlew release
```

自动回滚仅恢复程序，不撤销数据库迁移。发布前应确认迁移向后兼容；不可逆迁移必须人工维护部署。
包内包含版本、构建编号和所有文件的 SHA-256 摘要。`BUILD_NUMBER` 必须单调递增（默认 UTC 时间戳）。

任务定义位于 `gradle/release.gradle`，通过 Gradle 依赖依次完成前端依赖安装、Console/Desktop 构建、
后端发行目录整理、SHA-256 清单和 ZIP 生成；不再依赖 Python 或嵌套调用 Gradle。
发行内容使用 `distributions.create('release')` 定义，由 Distribution 插件生成
`releaseDistZip`、`releaseDistTar` 和 `installReleaseDist` 任务。在线升级仅接受 ZIP。
产物为 `build/today-blog-<version>-<build>.zip` 和 `dist/today-blog-updater.jar`。
`release` 不自动执行测试，发布前可运行 `./gradlew :blog-updater:test :blog-api:test`。

可指定构建编号：

```bash
./gradlew release -PbuildNumber=202610270001
```

发行版本统一使用项目 `version`（在 `gradle.properties` 中配置），不提供单独的发行版本覆盖参数。
构建编号也可通过环境变量 `BUILD_NUMBER` 指定，`-PbuildNumber` 优先；
`scripts/package.sh` 仅转发到 `./gradlew release`。

发布任务会从当前 `PATH` 解析 pnpm 的绝对路径。若 IDEA 等 GUI 启动的 Gradle 未继承
终端的 Node/pnpm 环境，可通过 `-PpnpmExecutable=/绝对路径/pnpm` 或
`PNPM_EXECUTABLE` 指定启动器。启动器依赖的 `node` 也必须在 Gradle 的 `PATH` 中可用；
使用 nvm 时可从已执行 `nvm use` 的终端运行 Gradle，或在 IDE 中配置对应环境。

**安全边界：SHA-256 清单只检查文件与清单一致，不能验证发布者身份，也不能防止攻击者同时修改文件和清单。**
仅上传自己构建并通过 SSH/HTTPS 等可信渠道传输的包；严格保护博主账号、会话和上传目录。
后台升级等同于授权部署可执行代码，不能对外开放上传权限。

## 首次部署

Linux 服务器需要 systemd、JDK（含 jshell）、sudo >= 1.9.10、Bash、rsync。
部署用户需要 SSH 和首次安装 sudo 权限。数据库需自行创建并备份。

```bash
bash scripts/deploy.sh deploy@server build/today-blog-<version>-<build>.zip
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
升级器持有全局文件锁，将包复制到 root 私有临时目录后再次校验文件清单及摘要，再安装到新目录，
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
在线包不能修改 updater 或 systemd unit；这些受信任组件升级需 SSH 人工维护。
从旧签名版本切换时，须先通过 SSH 更新固定的 `/usr/local/lib/today-blog/updater.jar`；
新校验器不接受含旧 `META-INF/upgrade.sig` 的包，请使用新脚本重新打包。

发布前必须在独立 Linux/systemd 环境验收正常升级、启动失败回滚、权限、代理、附件保留。
本地 JVM 测试不能代替真实服务器验收。
