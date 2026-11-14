# 打包、部署和后台升级

API 使用 systemd，升级器位于独立 Gradle 模块 `blog-updater`，生产代码仅依赖 JDK。
`blog-api` 依赖该模块复用升级包校验；升级器通过 JDBC 直接读取任务和写回状态，不依赖 API 或 Web 框架。
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

## 前端文件映射（可选）

反向代理可以不直接引用版本目录，而是把 `current/console`、`current/desktop` 映射到固定的
Web 根目录。映射由管理端配置，由 root 升级器在切换版本后发布为符号链接：

```text
current/console  ->  /var/www/today-blog/console
current/desktop  ->  /var/www/today-blog/desktop
```

符号链接始终经过 `current`，因此升级只需切换 `current`，nginx 自动读取新版本文件；
升级失败回滚时，链接也随之指回旧版本，无需二次操作。nginx 默认跟随符号链接，
若启用了 `disable_symlinks`，需要关闭或改用直接引用版本目录的方式。

映射参数是具体结构而不是 `Properties`，字段为名称、方式、源和目标：

| 字段   | 约束                                                                     |
|--------|--------------------------------------------------------------------------|
| name   | 小写字母、数字、短横线，最长 32 字符，同一配置内唯一                     |
| mode   | 当前仅支持 `SYMLINK`                                                     |
| source | 只能是 `console`、`desktop` 或其子路径                                   |
| target | 规范化的绝对路径，必须位于服务器白名单目录内，不能与安装目录或状态目录重叠 |

管理后台「系统升级 → 升级配置 → 文件映射」提供增删改界面，也可以直接调用
`GET /api/console/maintenance/upgrade/mappings` 查询、`PUT` 同一路径保存。
配置保存在数据库表 `t_release_mapping`（由 Flyway v3.6 维护表迁移创建），是该配置的唯一来源；
启动升级任务时把当前映射快照到 `t_upgrade_path_mapping`，之后修改默认配置不影响已提交任务。
恢复业务数据库会把默认映射一并恢复到备份时间点，已提交任务仍使用各自快照。
升级器通过数据库读取任务快照，发布前会重新校验白名单、父目录所有权与权限。

白名单由 root 管理，默认文件为 `/etc/today-blog/release-mapping-roots`，每行一个绝对目录。
首次安装会创建 `/var/www/today-blog` 并写入该文件：

```bash
sudo install -d -m 755 /var/www/today-blog
printf '/var/www/today-blog\n' | sudo tee /etc/today-blog/release-mapping-roots
sudo chmod 644 /etc/today-blog/release-mapping-roots
```

目标父目录必须由 root 管理且不可被组或其他用户写入；目标已存在时只接受空目录或指向
同一位置的符号链接，其他内容会拒绝发布并阻止本次升级。新建链接使用临时名加原子重命名。
升级 unit 默认只读文件系统，需要把映射根目录加入 `ReadWritePaths` 后 `daemon-reload`：

```ini
[Service]
ReadWritePaths=/opt/today-blog /var/lib/today-blog /var/lib/today-blog-upgrade /var/backups/today-blog /var/www/today-blog
```

当前不提供复制模式；目标在同一挂载点且 nginx 可跟随符号链接时使用上述方案即可。
新增映射前可先在测试环境确认 nginx、SELinux/AppArmor 和文件权限允许访问链接目标。

## 后台升级

系统 → 系统升级 → 执行升级 → 上传或选择升级包 → 确认参数 → 开始升级。
任务 ID 保存在浏览器本地，重载页面后继续查询。重启可能导致登录失效，需重新登录。

### 升级期间拒绝请求

管理端提交升级（`POST /api/console/maintenance/upgrade/packages/{id}/start`）后立即进入维护状态，无需等待升级器
执行到切换步骤。API 进程不退出，但对新的业务请求返回 `503 Service Unavailable` 和
`Retry-After`；等待约 2 秒让已进入处理的请求完成后再重启 API。新版本通过健康检查
（或回滚版本恢复健康）后重新对外提供服务。

维护期间仍可访问：

- `GET /api/internal/health`：升级器健康检查使用。
- `GET /api/auth`、`POST /api/auth`：探测登录状态和重新登录，保证手动维护期间页面刷新或会话过期后仍能进入管理端。
- `GET /api/console/maintenance/upgrade/**`：管理端继续轮询升级进度。
- `GET /api/console/maintenance`：查询当前维护状态。
- `PUT /api/console/maintenance`：管理员手动开启或关闭维护，请求体为
  `{"enabled":true,"message":"维护中","retryAfter":120}`。

管理端「系统升级 → 执行升级」会显示维护状态并提供「开启维护 / 关闭维护」按钮；
升级任务运行期间按钮禁用，避免中途关闭保护。手动维护不会随升级任务结束自动关闭，
需要管理员主动恢复；维护期间登录状态探测和登录接口保持可用，页面刷新或会话过期后
仍可重新登录并关闭维护。API 进程重启会清除内存中的手动维护状态，无法通过管理端登录时
也可重启 `today-blog.service` 恢复服务。

维护状态保存在 API 进程内存中，不写入磁盘。API 重启后维护状态自动清除，因此升级器切换
版本并重启 API 后，新版本或回滚后的旧版本会按正常状态提供服务。手动维护在进程重启后同样
会清除，需要长期维护时请在服务重启后重新开启。健康检查仍返回 `UP:<build>`，维护状态不会
阻止新版本验收。

### 数据库备份配置

默认启用升级前数据库备份。旧安装必须先通过 SSH 更新固定的 updater JAR、systemd unit，
再使用新页面；在线发行 ZIP 不会更新这些受信任组件。

安装与备份环境检查仅验证配置、工具和空间，数据库连接和权限在实际备份时验证。
未配置备份时默认禁止升级；如明确关闭本次备份，页面会要求二次确认风险。

服务器安装 `mysqldump` 或与生产数据库版本兼容的 `mariadb-dump`。
创建 root 所有、不可被其他用户写入的 `/etc/today-blog/upgrade.properties`（建议 0644；不含密码）：

```properties
dumpExecutable=/usr/bin/mysqldump
defaultsFile=/etc/today-blog/backup.cnf
backupRoot=/var/backups/today-blog
database=today
```

创建 root:root、0600 的 `/etc/today-blog/backup.cnf`，不要将实际密码放入仓库：

```ini
[client]
host=127.0.0.1
port=3306
user=blog_backup
password=由运维安全配置
```

备份账号仅授予目标数据库导出所需权限。普通表和视图通常需要 SELECT、SHOW VIEW，
触发器、事件、存储过程另需对应权限；具体要求依 MySQL/MariaDB 版本而异，须用实际版本演练。
当前导出包含 routines、events、triggers，不应通过授予数据库管理权限来绕过配置问题。

升级器默认复用 `defaultsFile` 中的 `[client]` 账号读写任务状态，需要额外授权：

```sql
GRANT SELECT, UPDATE ON today.t_upgrade_record TO 'blog_backup'@'localhost';
GRANT SELECT ON today.t_upgrade_package TO 'blog_backup'@'localhost';
GRANT SELECT ON today.t_upgrade_path_mapping TO 'blog_backup'@'localhost';
GRANT INSERT, SELECT ON today.t_backup TO 'blog_backup'@'localhost';
GRANT DELETE ON today.t_upgrade_package TO 'blog_backup'@'localhost';
```

也可以使用独立账号，在 `/etc/today-blog/upgrade.properties` 中配置
`jdbcUrl`、`jdbcUser` 和 `jdbcPasswordFile`；密码文件必须由 root 管理且为 0600。
升级器直连数据库，数据库不可用时升级无法启动或回写状态。

备份根目录及全部父目录必须由 root 管理，且不可被组或其他用户写入；建议根目录 0755，
升级器为每个任务建立 0700 子目录，SQL 临时文件及 gzip 文件使用 0600。
API 只读取非敏感配置和凭据文件元信息，不读取数据库密码或备份内容。
若配置文件设为 0600，API 无法检查环境，会显示未就绪；密码必须单独保存在 0600 的 defaultsFile 中。

现有服务器更新升级 unit 时，应同步设置：

```ini
[Service]
TimeoutStartSec=5400
ReadWritePaths=/opt/today-blog /var/lib/today-blog /var/lib/today-blog-upgrade /var/backups/today-blog /var/www/today-blog
```

修改 unit 后执行 `systemctl daemon-reload`。使用自定义备份目录或映射根目录时必须预先创建目录，
并同步修改 `ReadWritePaths`；目录不可放在 release 下。

备份在解压准备完成后、切换 current 和启动新版本之前执行；失败或超时不会重启旧服务。
导出先写私有临时 SQL，再压缩为 `database.sql.gz`，成功后才发布备份文件与元信息。
超时覆盖导出及压缩，失败删除临时数据，任务仅显示脱敏错误。默认备份超时 600 秒，
可配置 30–3600 秒；健康检查默认 90 秒，可配置 10–600 秒；备注最多 500 字。
安装盘至少预留 3 GiB，备份盘至少 256 MiB；这些只是最低门槛，运维须额外预留
完整 SQL 和压缩文件同时存在的空间。磁盘写满时备份失败并阻止切换。

在线一致性备份仅适用于 InnoDB 等事务表，备份期间禁止并发 DDL；非事务表不能保证快照一致性。
备份不包含附件，也不包含备份时间点之后的新写入，不代表整个网站的全量一致性备份。
首次启用前应在隔离数据库导入 SQL 并验证关键数据，定期执行恢复演练及异地备份，参见议题 #22。
不要直接向生产数据库导入；本功能不提供自动数据库恢复。

### 设置与记录

“升级设置”保存默认参数；“执行升级”可覆盖本次参数。启动时把参数和升级包快照写入
`t_upgrade_record`，后续修改默认值不影响已提交任务。开启自动程序回滚仅适用于向后兼容的迁移。
`ROLLED_BACK` 表示本次升级失败、已恢复旧程序，而不是新版本发布成功。

升级相关数据全部保存在数据库：

- `t_upgrade_setting`：默认升级参数
- `t_upgrade_package`：上传的升级包，`location` 指向本地 ZIP
- `t_upgrade_record`：升级任务和历史记录，保存包信息与参数快照
- `t_upgrade_path_mapping`：任务启动时的文件目录映射快照
- `t_release_mapping`：可编辑的默认文件映射
- `t_backup`：备份元信息，`location` 指向本地备份文件

升级包接口：

```text
GET    /api/console/maintenance/upgrade/packages           列表
POST   /api/console/maintenance/upgrade/packages           上传
PUT    /api/console/maintenance/upgrade/packages/{id}      修改名称和备注
DELETE /api/console/maintenance/upgrade/packages/{id}      删除记录和文件
POST   /api/console/maintenance/upgrade/packages/{id}/start 从该包创建升级任务
GET    /api/console/maintenance/upgrade/records            升级记录
GET    /api/console/maintenance/upgrade/records/{id}       任务详情
```

升级器通过 JDBC 读取 `t_upgrade_record`、`t_upgrade_package` 和 `t_upgrade_path_mapping`，
直接更新任务状态。成功后会按 `delete_package_after_upgrade` 删除升级包文件和记录。
`t_backup_operation` 不再创建，备份管理页不展示操作记录；备份执行器接通时需要审计再单独建表。

管理后台「系统管理 → 备份管理」（`/system/backups`）提供备份列表和详情；
接口统一使用 `/api/console/backups`；旧页面 `/system/snapshots` 重定向到新入口。
所有接口要求博主身份；详情只返回允许展示的元信息，不返回配置凭据或备份内容。
当前管理入口为只读：完整执行器接通前，创建、保护、删除和恢复按钮均禁用。

目录与权限：

```text
/opt/today-blog/                         root 所有，API 只读
  current -> releases/<version>-<build>
  releases/                            新旧版本并存
  highest-build.properties             成功发布的最高构建号
/usr/local/lib/today-blog/updater.jar    固定、root 所有，包含 JDBC 驱动
/etc/today-blog/upgrade.properties       升级器数据库和备份配置，root 所有
/etc/today-blog/release-mapping-roots    root 所有，前端映射目标白名单
/var/lib/today-blog-upgrade/
  packages/<UUID>/package.zip           today 写，升级包文件
/var/lib/today-blog/                     today 写，业务运行数据
/var/backups/today-blog/                 root 写，数据库备份
/var/www/today-blog/                    映射目标，root 管理，升级器发布链接
```

维护状态保存在 API 进程内存中，不写入磁盘。API 重启后维护状态自动清除，因此升级器切换
版本并重启 API 后，新版本或回滚后的旧版本会按正常状态提供服务。手动维护在进程重启后同样
会清除，需要长期维护时请在服务重启后重新开启。健康检查仍返回 `UP:<build>`，维护状态不会
阻止新版本验收。

## 运维与边界

```bash
journalctl -u today-blog.service
journalctl -u today-blog-updater@<UUID>.service
mysql -e "select id,status,message from today.t_upgrade_record order by created_at desc limit 5"
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
