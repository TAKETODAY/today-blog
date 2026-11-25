# 打包、部署和后台升级

`today-blog` 位于独立 Gradle 模块 `blog-cli`，是执行完即退出的非 Web infra 应用。
API 默认由 CLI 直接管理进程，不依赖 systemd；也可在部署时选择 systemd，CLI 的启停和升级命令不变。
升级包校验、发行文件映射和磁盘协议位于纯 JDK 模块 `blog-upgrade-protocol`，
由 `blog-api` 和 `blog-cli` 共同依赖；`blog-api` 不依赖 CLI 实现。
CLI 实现位于 `cn.taketoday.blog.cli`。升级命令使用 infra 的 `EntityManager` 读写任务，
不依赖 API 或 Web 模块；帮助和启停命令不会读取数据库配置。CLI 实体的 Native Image 反射元数据由
infra AOT 从 `@ReflectiveScan` 和 `@Table` 生成。
可单独运行 `./gradlew :blog-upgrade-protocol:test :blog-cli:test releaseCli` 构建并测试，产物为
`build/dist/today-blog-cli.jar`。API 以任务 ID 调用独立升级进程；停止旧 API 不会终止升级器。

## 构建

构建机需要 JDK 17+、Node.js 和 pnpm（本次验收使用 JDK 21、Node.js 24、pnpm 12）。
Gradle 在 `common`、`console`、`desktop` 下分别按各自的 `pnpm-lock.yaml` 安装依赖。
服务器建议 JDK 25。当前版本不使用密钥、证书或数字签名。

```bash
./gradlew release
```

自动回滚仅恢复程序，不撤销数据库迁移。发布前应确认迁移向后兼容；不可逆迁移必须人工维护部署。
包内包含版本、构建编号和所有文件的 SHA-256 摘要。`BUILD_NUMBER` 必须单调递增（默认 UTC 时间戳）。

任务定义位于 `gradle/release.gradle`，通过 Gradle 依赖依次完成前端依赖安装、Console/Desktop 构建、
后端发行目录整理、SHA-256 清单和 ZIP 生成；不再依赖 Python 或嵌套调用 Gradle。
`releaseDistZip`、`releaseDistTar` 和 `installReleaseDist` 共用固定的 `today-blog/` 根目录。
文件名带版本号和构建编号，但归档内清单必须位于 `today-blog/META-INF/upgrade.properties`。
在线升级仅接受 ZIP。`release` 会运行 `verifyRelease`，用服务器升级器校验最终 ZIP；
任一文件缺失、路径不合规或摘要不匹配都会中止构建。正式打包请使用干净的工作目录，
避免本地旧编译产物（例如已删除的 Flyway 迁移）进入发行包。
产物为 `build/today-blog-<version>-<build>.zip` 和 `build/dist/today-blog-cli.jar`。
`release` 不自动执行测试，发布前可运行 `./gradlew :blog-upgrade-protocol:test :blog-cli:test :blog-api:test`。

在目标平台上安装 GraalVM JDK 25（含 `native-image`）和 C 工具链，将 `JAVA_HOME` 指向该 JDK 后运行
`./gradlew -PnativeCli :blog-cli:nativeSmokeTest`，构建
`blog-cli/build/native/nativeCompile/today-blog` 并运行 `--help`，验证原生应用能启动。
默认安装仍使用 JAR。原生 CLI 无需 JVM，但须按目标系统和 CPU 架构构建；`blog-api` 仍需要 Java。
绕过安装脚本的 `/usr/local/bin/today-blog` 包装入口直接运行原生二进制时，systemd 部署需显式设置
`BLOG_SERVICE_MANAGER=systemd`；包装入口会自动读取安装时记录的模式。

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

Linux 服务器需要 JDK、sudo >= 1.9.10、Bash 和 rsync。直接进程模式还需要 util-linux（`setsid`、`setpriv`）；
systemd 模式要求服务器运行 systemd（支持 `Type=exec` 和 `ExecCondition`）。
部署用户需要 SSH 和首次安装 sudo 权限。数据库需自行创建并备份。安装向导只连接已经创建的空库，
自动导入发行包内的初始化脚本；已有博客不要导入初始化脚本。
运行时数据统一放在 `BLOG_DATA_ROOT`（默认 `/var/lib/today-blog`），可用 `today-blog data` 管理。

```bash
bash scripts/deploy.sh deploy@server build/today-blog-<version>-<build>.zip
# 或改用 systemd 托管 API
bash scripts/deploy.sh --systemd deploy@server build/today-blog-<version>-<build>.zip
```

安装脚本不会启动服务。安装完成后可以直接创建 `/etc/today-blog/today-blog.env`（建议 `root:today`、`0640`），
也可以执行 `sudo today-blog install --wizard` 打开一次性的本机浏览器向导。向导会让管理员选择
`direct` 或 `systemd`、测试数据库连接、初始化空库、创建博主账号，并生成应用配置和 NGINX 示例；
它不会覆盖非空数据库、自动启动 API、启用 systemd 或修改 NGINX。远程服务器通过 SSH 本地端口转发访问向导。
向导完成后配置文件位于 `/etc/today-blog/`，数据库密码不出现在命令行；已有博客不要使用向导，直接配置并走升级流程。
`today-blog.env` 采用 Bash 赋值语法，由 `today` 用户加载，含空格的值须加引号。默认 prod profile，
API 工作目录为 `$BLOG_DATA_ROOT`，运行时数据不得写入 release。
API 可通过 `JAVA_HOME` 选择 Java；默认部署的 CLI JAR 使用 `/usr/bin/java`。
配置文件不得存储在版本目录；需要时通过 `JAVA_OPTS` 的
`-Dapp.config.additional-location=file:<path>` 追加外部覆盖文件，向导生成的 `today-blog.env` 即采用这种方式。
附件、头像和背景图的本地文件默认保存在 `$BLOG_DATA_ROOT/data/upload/`，新安装无需单独配置附件路径。
开发环境（`dev`）未设置 `BLOG_DATA_ROOT` 时，默认仍使用 `~/today-blog/data` 存储附件。
已有的 `attachment.outside-resource` 仍优先，不会自动迁移文件或改写配置。
安装向导默认不写此配置；仅在向导进程设置 `BLOG_DATA_PATH` 时，才把它写成显式覆盖。
如果自定义附件目录不在 `$BLOG_DATA_ROOT/data` 下，请单独备份和检查；`today-blog data` 不会读取 API 的附件配置来查找这些文件。

反向代理静态目录：

- Console：`/opt/today-blog/current/console`
- Desktop：`/opt/today-blog/current/desktop`
- API：代理到 `127.0.0.1:8080`
- 向导生成的 NGINX 草案：`/etc/today-blog/nginx.conf.example`；审核后复制，先执行 `nginx -t` 再 reload。
- 必须拒绝外部访问 `/api/internal/health`；绑定本机 API 或用防火墙阻止公网直连端口。
- 上传请求大小、超时需与后端保持一致。

```bash
sudo today-blog start
curl --fail http://127.0.0.1:8080/api/internal/health
```

正常返回 `UP:<build>`。确认 API 和两套静态页面正常后再使用在线升级。
停止 API：`sudo today-blog stop`。重启：`sudo today-blog restart`，它会等待当前构建健康（最多 90 秒）。
`start` 不等待 API 就绪，请以健康接口和日志为准。直接进程模式不提供开机自启或崩溃自动重启。
systemd 模式由 `today-blog.service` 以 `today` 身份运行 API，异常退出时自动尝试重启；如需开机自启，
请在配置完成并确认 API 健康后执行 `sudo systemctl enable today-blog.service`。
查看命令：`today-blog --help`。运维可用 `sudo today-blog upgrade --task-id <ID>` 执行已提交且仍待接管的数据库任务；
该命令不创建任务，也不直接接受 ZIP。升级未成功时，即使回滚成功也返回非零退出码。

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
使用自定义映射根目录时，须预先创建目录、设置权限并同步更新白名单。

当前不提供复制模式；目标在同一挂载点且 nginx 可跟随符号链接时使用上述方案即可。
新增映射前可先在测试环境确认 nginx、SELinux/AppArmor 和文件权限允许访问链接目标。

## 后台升级

系统 → 系统升级 → 执行升级 → 上传或选择升级包 → 确认参数 → 开始升级。
任务 ID 保存在浏览器本地，重载页面后继续查询。重启可能导致登录失效，需重新登录。

### 升级期间拒绝请求

管理端提交升级（`POST /api/console/maintenance/upgrade/packages/{id}/start`）后立即进入维护状态，无需等待升级器
执行到切换步骤。API 进程不退出，但对新的业务请求返回 `503 Service Unavailable` 和
`Retry-After`，直至升级器准备完成并停止旧 API。升级器核对新 API 返回的构建号，
健康检查失败时恢复旧版本并再次检查。反向代理在切换期间可能返回短暂的 502/503。

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
也可用 `sudo today-blog restart` 恢复服务。

维护状态保存在 API 进程内存中，不写入磁盘。API 重启后维护状态自动清除，因此升级器切换
版本并重启 API 后，新版本或回滚后的旧版本会按正常状态提供服务。手动维护在进程重启后同样
会清除，需要长期维护时请在服务重启后重新开启。健康检查仍返回 `UP:<build>`，维护状态不会
阻止新版本验收。

### 数据库备份配置

默认启用升级前数据库备份。在线发行 ZIP 不会更新固定的 CLI JAR 和启动脚本。

安装与备份环境检查仅验证配置、工具和空间，数据库连接和权限在实际备份时验证。
未配置备份时默认禁止升级；如明确关闭本次备份，页面会要求二次确认风险。

服务器安装 `mysqldump` 或与生产数据库版本兼容的 `mariadb-dump`。
创建 root 所有、不可被其他用户写入的 `/etc/today-blog/upgrade.properties`（建议 0644；不含密码）：

```properties
dumpExecutable=/usr/bin/mysqldump
defaultsFile=/etc/today-blog/backup.cnf
database=today
backupReadGroup=today
```

备份默认写入 `$BLOG_DATA_ROOT/backups/db`。已有的 `backupRoot` 显式配置仍然生效；
若它指向默认目录之外，`today-blog data list backups`、`data prune` 和 `data export` 不会管理该位置。

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

备份根目录及全部父目录必须由 root 管理，且不可被组或其他用户写入；建议根目录 0755。
配置 `backupReadGroup=today` 后，已完成的 gzip 归档为 `root:today 0640`，任务目录为 `root:today 0710`；
SQL 临时文件仍为 0600。未设置该项时，新任务目录和归档继续使用 0700 和 0600。
博主可在「系统备份」下载已登记的数据库备份；不要将备份目录映射为 NGINX 静态目录。
已有归档如需下载，由 root 对确认过的任务目录和归档分别设置上述属组和权限，无需重新备份。
API 无需读取数据库密码，只检查凭据文件的元信息。
若配置文件设为 0600，API 无法检查环境，会显示未就绪；密码必须单独保存在 0600 的 defaultsFile 中。

使用自定义备份目录时须预先创建目录，不可放在 release 下。即使关闭升级前备份，仍需配置
升级器的任务数据库连接；关闭备份只跳过导出，不跳过数据库状态读写。

备份在解压准备完成后、切换 current 和启动新版本之前执行；失败或超时不会重启旧服务。
导出先写私有临时 SQL，再压缩为 `database.sql.gz`，成功后才发布备份文件与元信息。
超时覆盖导出及压缩，失败删除临时数据，任务仅显示脱敏错误。默认备份超时 600 秒，
可配置 30–3600 秒；健康检查默认 90 秒，可配置 10–600 秒；备注最多 500 字。
安装盘至少预留 3 GiB，备份盘至少 256 MiB；这些只是最低门槛，运维须额外预留
完整 SQL 和压缩文件同时存在的空间。磁盘写满时备份失败并阻止切换。

在线一致性备份仅适用于 InnoDB 等事务表，备份期间禁止并发 DDL；非事务表不能保证快照一致性。
备份不包含附件，也不包含备份时间点之后的新写入，不代表整个网站的全量一致性备份。
升级前请按[整站备份与恢复指南](backup-restore.md)生成包含本地网站文件的恢复点并确认异地副本。数据库导出不能替代整站恢复点；默认归档不包含 OSS 对象，云端对象需独立保护或配置可选导出器。
首次启用前应在隔离数据库导入 SQL 并验证关键数据；定期从异地副本演练恢复。
恢复演练请使用独立的数据库实例：导出的 SQL 含 `USE` 原库名，给客户端指定另一个默认库
并不能隔离导入。不要直接向生产数据库导入；本功能不提供自动数据库恢复。

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

升级器通过 `EntityManager` 读取 `t_upgrade_record` 和 `t_upgrade_path_mapping`，
直接更新任务状态。成功后会按 `delete_package_after_upgrade` 删除升级包文件和记录。
`t_backup_operation` 不再创建；加密备份任务状态由独立执行器写入 root 管理的 `/var/lib/today-blog-backup-jobs/`，不依赖应用数据库。

管理后台「系统管理 → 系统备份」（`/system/backups`）提供备份列表和详情。配置 [后台备份权限](backup-restore.md#管理后台)后，可提交按类型选择的加密备份任务；
接口统一使用 `/api/console/backups`；旧页面 `/system/snapshots` 重定向到新入口。
所有接口要求博主身份；详情只返回允许展示的元信息，不返回配置凭据或备份内容。
未启用后台备份权限时，创建按钮禁用；保护、删除和在线恢复仍不开放。既有数据库备份的下载不受影响。

目录与权限：

```text
/opt/today-blog/                         root 所有，API 只读
  current -> releases/<version>-<build>
  releases/                            新旧版本并存
  highest-build.properties             成功发布的最高构建号
  run/blog-api.pid                      直接进程模式下的 PID 与进程启动时间
/usr/local/bin/today-blog               运维 CLI 入口
/usr/local/lib/today-blog/cli.jar       固定、root 所有，包含 JDBC 驱动
/usr/local/lib/today-blog/{updater,today-updater,launch-api,start-api}
                                         root 管理的升级入口和 API 启动脚本
/etc/today-blog/service-manager          root 管理的服务模式：direct 或 systemd
/etc/today-blog/data-root                root 管理的运行时数据根目录记录
/etc/today-blog/data.properties          保留策略与空间告警阈值，root 所有
/etc/today-blog/upgrade.properties       升级器数据库和备份配置，root 所有
/etc/today-blog/release-mapping-roots    root 所有，前端映射目标白名单
/var/lib/today-blog/                     运行时数据根目录（BLOG_DATA_ROOT）
  data/                                  today 写，业务数据、上传和附件
  backups/db/                            root 写，数据库备份
  upgrade/packages/                      today 写，升级包文件
  upgrade/updater.log                    升级进程日志，root 写
  logs/blog-api.log                      直接进程模式下的 API 日志
  tmp/                                   上传、备份解压和恢复等临时文件
/var/www/today-blog/                     映射目标，root 管理，升级器发布链接
```

维护状态保存在 API 进程内存中，不写入磁盘。API 重启后维护状态自动清除，因此升级器切换
版本并重启 API 后，新版本或回滚后的旧版本会按正常状态提供服务。手动维护在进程重启后同样
会清除，需要长期维护时请在服务重启后重新开启。健康检查仍返回 `UP:<build>`，维护状态不会
阻止新版本验收。

## 运行时数据统一管理

默认情况下，可由运维清理、备份或搬迁的数据集中在 `BLOG_DATA_ROOT`（默认 `/var/lib/today-blog`）下。
直接进程模式的 API 控制台日志由 CLI 写入该根目录的 `logs/blog-api.log`，systemd 模式由 journal 收集。
上传请求超出内存阈值时，临时文件写入 `$BLOG_DATA_ROOT/tmp/multipart`，不会纳入数据导出。
配置、发行版本和受信任的运维入口仍保留在 `/etc/today-blog`、
`/opt/today-blog` 和 `/usr/local`。

升级目录和直接进程模式的工作目录分别使用 `$BLOG_DATA_ROOT/upgrade` 和 `$BLOG_DATA_ROOT`，
不再读取 `BLOG_UPGRADE_STATE`、`BLOG_API_WORK_PATH`。更换数据根目录不会迁移文件，
请先停止服务，备份并搬迁数据，再以 `BLOG_DATA_ROOT` 指定新目录运行 `install-server.sh --runtime-only`，
并检查 `attachment.outside-resource`、`backupRoot` 和反向代理配置。未显式指定新目录时，
`--runtime-only` 会保留 `/etc/today-blog/data-root` 中已有的根目录；部署后的 API 和 CLI 共用这份配置。
数据库只保存相对文件路径：附件相对于附件根目录，升级包相对于 `$BLOG_DATA_ROOT/upgrade/packages`，
备份相对于配置的 `backupRoot`，发行目录相对于安装根目录。映射目标在库中相对于服务器文件系统根，
API 仍用绝对路径显示和编辑，并在发布前校验白名单。附件的公开 `/upload/...` URL 不变。
Flyway v3.8 会转换旧记录中的路径，不会移动文件。只要按原目录结构搬迁升级包，修改数据根目录后无需重新上传。
部署本版本时请先更新 CLI，再更新 API。新版 CLI 可读取迁移前的任务；旧版 CLI 无法读取迁移后的升级包路径。
若回滚到迁移前的 API，旧 API 的升级包管理与已迁移的记录不兼容；不要用旧 API 管理这些升级包。

```bash
sudo today-blog data status                     # 各子目录占用、磁盘余量和空间告警
sudo today-blog data list backups|packages|logs
sudo today-blog data check                      # 所有权、权限和可用空间校验
sudo today-blog data prune                      # 按保留策略预览待删数据
sudo today-blog data prune --apply              # 实际删除
sudo today-blog data backup                     # 立即导出数据库备份
sudo today-blog data restore <database.sql.gz> --yes
sudo today-blog data export --output /tmp/today-blog-data.zip
sudo today-blog data policy                     # 查看保留策略
sudo today-blog data policy --backups-keep 5 --packages-keep 3 --logs-days 14
```

保留策略和空间告警阈值保存在 `/etc/today-blog/data.properties`，默认保留 5 份数据库备份、
3 个升级包、14 天日志，已用空间达到 85% 或可用空间低于 1 GiB 时在 `data status` 中告警。
`prune` 默认只预览，只有加 `--apply` 才删除；删除时跳过符号链接，带 `.protected` 标记的
备份不会被清理。

## 运维与边界

```bash
sudo tail -n 100 /var/lib/today-blog/logs/blog-api.log
sudo tail -n 100 /var/lib/today-blog/upgrade/updater.log
mysql -e "select id,status,message from today.t_upgrade_record order by created_at desc limit 5"
```

systemd 模式用 `sudo journalctl -u today-blog.service` 查看 API 日志，用
`sudo journalctl -u today-blog-updater@<ID>.service` 查看后台升级任务日志（替换 `<ID>`）。
后台升级任务运行在独立的 oneshot unit 中，API 重启不会终止升级器；`today` 仍只获准通过固定入口传入任务 ID。
日常启停应使用 `today-blog`，这样会与升级共用文件锁；手动执行 `systemctl start/stop/restart` 会绕过该锁。

自动回滚仅覆盖可捕获的安装、启动、健康检查失败。
断电、SIGKILL、磁盘写满可能留下中间状态，当前不提供崩溃后自动恢复或后台手动回滚按钮。
此时先确认升级进程已退出，再用 `sudo today-blog stop` 停止 API，将 current 原子切回已知正常的 release，
然后用 `sudo today-blog start` 启动并检查健康状态。
不要将程序回滚误认为数据库恢复。不要删除正在运行或可能用于回滚的版本目录。
上传包、失败版本和历史版本暂不自动清理，由管理员定期按保留策略清理。
在线包不能修改 CLI 或启动脚本；这些受信任的文件需通过 SSH 人工维护。
更新运维入口或切换服务模式都应在没有升级任务运行的维护窗口进行。`--runtime-only` 不替换 `current`
或现有版本；不指定模式时保留已记录的模式，旧安装没有模式文件则默认使用 `direct`。

- 直接进程切到 systemd：先 `sudo today-blog stop`，再运行
  `sudo bash scripts/install-server.sh --systemd --runtime-only build/dist/today-blog-cli.jar`，最后 `sudo today-blog start`。
- systemd 切到直接进程：先 `sudo systemctl disable --now today-blog.service`，再运行
  `sudo bash scripts/install-server.sh --direct --runtime-only build/dist/today-blog-cli.jar`，最后 `sudo today-blog start`。
- 从早期 systemd 安装升级并继续使用 systemd：先停止旧服务，检查旧 unit 的 drop-in，
  再用 `--systemd --runtime-only` 更新。API 环境文件现在由 `today` 按 Bash 语法读取；
  确认 `/etc/today-blog/today-blog.env` 可由 `today` 读取（例如 `root:today`、`0640`）后再启动。

不要让两种启动方式同时管理同一个 API 实例。
新校验器不接受含旧 `META-INF/upgrade.sig` 的包，请使用新脚本重新打包。

发布前必须在独立 Linux 环境验收正常升级、启动失败回滚、权限、代理、附件保留。
本地 JVM 测试不能代替真实服务器验收。
