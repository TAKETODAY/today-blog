# 整站备份与恢复（Linux/systemd）

`sudo today-blog backup-site` 生成一个包含数据库、本地网站文件、配置和发布工件的加密恢复点。原服务器不可用时，使用异地归档、独立保管的 age 私钥、兼容的 Linux/JDK/MySQL，以及创建新服务账号和调整域名的权限重建站点。

**默认备份不包含 OSS 对象。** 数据库中的 OSS 附件记录和引用仍会随数据库保存。`oss=none` 表示对象必须由云存储独立保护；对象丢失时，仅凭默认归档无法恢复对应文件。没有覆盖数据库、本地附件、网站文件、运行配置和发布工件的选择标记为 `siteScope=partial`，不能视作整站恢复点；`full-local-site` 也不等于已恢复验证。

升级页的「数据库备份」不是整站恢复点，**不包含网站文件、OSS 或配置**。程序回滚不会撤销数据库迁移或恢复网站文件。

## 配置

1. 在构建机重新构建 CLI（`./gradlew releaseCli`），按[部署文档](deployment.md)用可信的 `install-server.sh --systemd --runtime-only` 更新服务器运维入口。在线升级 ZIP 不更新 CLI。仅支持 systemd；后台只提交请求，独立的 root unit 执行备份。
2. 安装 `age`、GNU tar 和兼容的 `mysqldump`/`mariadb-dump`。按[数据库备份配置](deployment.md#数据库备份配置)设置 `upgrade.properties` 和仅 root 可读的 MySQL defaults 文件。确认导出连接的是应用真正使用的库；导出应包含表、Flyway 历史、触发器、事件和例程。禁止 DB 事件、外部脚本、其他节点或管理操作在采集时写入/执行 DDL；不要把 API 维护模式当作停写。只有确认前提成立才设 `noExternalWriters=true`；定时执行要求这一前提对每次执行都成立。
3. 将 [`site-backup.properties.example`](../../scripts/backup/site-backup.properties.example) 按实际路径配置为 `/etc/today-blog/site-backup.properties`（root:root、0600）。建立 root:root、0700 的 `output`，父目录也必须由 root 管理且不能被他人写入。暂存含明文，应位于加密卷，不发布为 NGINX 静态目录；磁盘预留原始 SQL、压缩 SQL、本地文件副本及密文同时存在所需空间（1 GiB 只是最低门槛）。在隔离可信机器用 `age-keygen` 生成密钥，仅将 `age1…` 公钥放到网站服务器；私钥至少保存两份独立且受保护的副本。发布签名私钥如有，单独加密离线保管，不放入站点服务器或恢复点。
4. 保留部署时使用的原始发行 ZIP 和匹配当前 CLI 的安装脚本，放到 root 管理的目录。`releaseZip` 必须与 `/opt/today-blog/current` 的版本、构建号和发行文件摘要一致。**每次升级后**更新 `releaseZip` 指向对应的可信工件，再验证定时任务；不要用待升级的 ZIP，也不要依赖升级器留存上传的 ZIP。
5. 清点生效配置。`source.upload` 必须指向实际 `upload/` 目录（默认 `$BLOG_DATA_ROOT/data/upload/`，或 `attachment.outside-resource` 下的 `upload/`；没有本地附件也需创建空目录）。列出应用环境、外部 YAML/密钥引用、NGINX 站点文件及 include、systemd drop-in、自定义静态文件、映射策略等；只保存重建所需数据。排除日志、缓存、临时上传、历史备份、待升级 ZIP。每项使用 `source.<标签>=绝对路径`，仅接受普通文件和目录；有符号链接时列出实际目标，并记录如何重建链接。不要整包归档 `/etc` 或整个运行时数据根目录。文件复制不保留所有者、ACL 或原权限；在恢复前把必要的权限规则、外部依赖版本和配置步骤记录为一个 `source.runbook` 文件。

   `source.upload` 归属本地附件，`source.custom-static` 和 `source.website-*` 归属网站文件，其余 `source.*` 归属运行配置。如果没有外置网站文件，网站文件范围为空，发行包内静态资源由发布工件覆盖。

### 可选 OSS 导出

默认使用云存储独立保护，例如启用版本控制、限制永久删除权限并保留非当前版本。普通镜像同步可能传播误删。需要将对象纳入归档时，配置由 root 管理的 `ossExporter`（项目不内置云厂商导出工具）。执行器只传入一个参数，即暂存的 `oss/` 目录。导出器使用只读凭据，须保存对象内容与恢复所需元数据、生成非空 `inventory.json`，确认导出完整后才返回 0；框架校验清单文件存在及归档内文件摘要，不核验云端清单的完整性。只接受普通文件、目录；导出失败不发布归档。选择 OSS 时也必须确认没有云端外部写入者，并考虑导出对停写窗口的影响。由运维在隔离桶验证配套导入流程后，再对后台开放 OSS。

### 管理后台

先在隔离环境验证备份配置，再将 [`backup-console.properties.example`](../../scripts/backup/backup-console.properties.example) 安装为 `/etc/today-blog/backup-console.properties`（root:root、0644）。它只含允许的类型和异地上传开关，不含凭据。后台「系统管理 → 系统备份」可按类型选择或全选已授权类型；推荐范围不含 OSS。只允许经过验证的类型，异地上传与 OSS 未验证前不要授权。删除此策略文件可关闭后台提交，不影响运维 CLI 与定时任务。

后台同时只执行一项请求，通过 `today-blog-backup-console.service` 执行，采集期间网页可能暂时不可访问，服务恢复后可查看状态、范围、耗时及本地归档名称。任务记录位于 `/var/lib/today-blog-backup-jobs/`，不含凭据，不随数据库回滚；按需定期归档旧记录。故障排查使用 `sudo journalctl -u today-blog-backup-console.service`，并为此 unit 配置告警。后台不提供加密归档下载、在线恢复或删除，不能替代异地读回确认和恢复演练。

### 执行、告警与保留

```bash
sudo today-blog backup-site
sudo today-blog backup-site --types DATABASE
sudo today-blog backup-site --types DATABASE,ATTACHMENTS
# 只有在配置 OSS 导出器后才可使用 all
sudo today-blog backup-site --types all
```

命令以现有升级锁串行化采集。仅数据库范围在线执行，需要 InnoDB 且不得并发执行 DDL；其余范围停止 API 并验证已停止，采集后若原先在运行则恢复并核对构建号。不要并行手动启动 unit、修改应用配置或执行发布。支持加密前预检、归档校验和原子发布。采集/压缩/加密失败返回非零，不发布新归档；服务无法恢复时优先检查 `today-blog.service`。断电、强制杀进程或 systemd 超时并不保证恢复执行：立即检查服务状态，仅在确认没有备份或升级运行后手工恢复服务、清理专用目录下遗留的 `.site-work-*`。不要把残留明文目录及不完整产物当备份。

配置 `offsiteUploader` 后，本地发布后执行该程序，唯一参数是 `.tar.gz.age` 路径；只有上传并读回校验通过后才允许返回 0。可参考 [`site-offsite-upload.example`](../../scripts/backup/site-offsite-upload.example) 的 rclone 实现。异地存储走加密传输，使用独立账号和专用桶，启用不可变保留或仅追加权限，禁止网站运行账号删除备份。不配置上传器时，命令只生成本地归档，须手工上传和读回校验；上传失败非零退出并保留本地归档，可只重试传输，不必再停机。

在隔离环境验证一次后，将 [`today-blog-site-backup.service`](../../scripts/backup/today-blog-site-backup.service) 和 [`today-blog-site-backup.timer`](../../scripts/backup/today-blog-site-backup.timer) 安装到 `/etc/systemd/system/`。按实际时区/停写窗口修改后运行：

```bash
sudo systemctl daemon-reload
sudo systemctl start today-blog-site-backup.service
sudo journalctl -u today-blog-site-backup.service
sudo systemctl enable --now today-blog-site-backup.timer
sudo systemctl list-timers today-blog-site-backup.timer
```

给备份 unit 添加 `OnFailure=` drop-in，指向现有告警 unit；同时从独立监控检查最近一次**异地确认**是否超过 RPO（例如每天低峰备份，超过 26 小时告警）及 API 是否在线。不要把 archive 校验、上传成功、恢复演练通过混为同一状态。先量实际停写时间；窗口放不下时，不启用定时停机方案，评估文件系统快照和按版本异步复制。

推荐保留最近 7 个日备份、4 个周备份、3 个自然月备份，每个周期保留一份已确认的异地副本。升级前另做并异地校验一份，单独标记为保护，至少保留至新版本稳定并验证新的可恢复点。清理先预览清单，排除保护点和最后一份演练通过的副本，再用独立的保留管理账号删除；可以在异地按分类配置生命周期规则。不要在未核实异地副本前清理本地文件；`today-blog data prune` 不管理整站归档。记录备份时间、版本、远端标识和校验结果，不记录密钥或业务明文。

## 隔离恢复演练

只使用新建隔离 Linux 主机、空且独立的 MySQL 实例；如需演练 OSS 文件访问，另用测试 OSS 桶。网络默认阻止访问生产 DB、生产 OSS、邮件和其他写入型依赖；不要在生产环境试跑任何导入或安装命令。

以下步骤针对完整本地站点范围。部分备份同样要先解密、校验并检查清单，再仅在隔离环境恢复归档中确实存在的内容；不要把不同时间采集的数据库和文件拼成一致性恢复点。

1. 从异地取回密文，核对可信渠道记录的密文摘要并在空目录解密。只有在解密和完整性校验均通过后才继续；`age` 公钥加密不提供发送方签名，必须信任归档来源。作为普通用户解包，不授予归档内文件权限或所有者。

   ```bash
   set -euo pipefail
   umask 077
   work=$(mktemp -d)
   age -d -i /secure/offline-recovery.agekey -o "$work/site.tar.gz" /path/to/site.tar.gz.age
   tar --no-same-owner --no-same-permissions -xzf "$work/site.tar.gz" -C "$work"
   cd "$work/site"
   sha256sum --strict -c SHA256SUMS
   cat manifest.properties
   if [[ -f database.sql.gz ]]; then
     gzip -t database.sql.gz
   fi
   ```

2. 阅读 `manifest.properties` 和环境记录。用与源版本兼容的数据库和客户端；确认原库名、字符集、排序规则、例程 `DEFINER` 所需账号，隔离实例关闭事件调度。在隔离实例导入整个库（SQL 含 `CREATE DATABASE`/`USE` 原名，客户端另选默认库不会隔离）。不得用 `--force` 跳过错误，不执行安装向导初始化已有业务数据。恢复账号与应用账号分开，使用指向隔离实例的 0600 defaults 文件。

   ```bash
   gzip -dc database.sql.gz | mysql --defaults-file=/secure/drill-import.cnf
   ```

3. 安装匹配的发布包和 CLI。先在可信渠道核对安装工件，再在**新主机**运行：`sudo bash artifacts/install-server.sh --systemd artifacts/release.zip artifacts/cli.jar`。非默认数据根目录应在安装前通过 `BLOG_DATA_ROOT` 指定。先不启动；按 `files/`、`manifest.properties` 和安全运行手册恢复附件和自定义文件，匹配 `attachment.outside-resource`，为 `today` 配置正确的读写权限。逐项审核部署配置、恢复 NGINX 路径/必要映射链接，检查 `nginx -t`；凭据指向演练环境或替换。不要恢复旧 PID、进程状态或直接执行旧升级任务。数据库内旧升级记录可作为历史参考，但归档不含旧 ZIP 和数据库备份文件，旧下载/包记录可能无法使用。
4. 如果有 OSS 引用，按 `oss=export` 的对象清单用配套导入工具恢复到隔离测试桶，并抽样读回验证对象内容和元数据；`oss=none` 时使用独立准备的对象副本。配置演练账号和测试访问域名。没有可用对象副本时，将对应文件访问标记为未验证或不可恢复，不连接或修改生产桶。
5. 启动前保存 `flyway_schema_history` 的有序查询结果及行数；执行 `sudo today-blog start`，核对 `/api/internal/health` 返回 `UP:<备份构建号>`。当前启动会执行 `repair → baseline → migrate`，使用匹配版本时不应出现意外历史变化。启动后重新查询并比较；异常时保留演练日志并停止，不继续切流量。
6. 验证公开站点、文章、评论、后台登录与页面加载，实际请求本地附件、头像、背景；对照附件记录抽样校验路径和内容。单独记录 OSS 对象来源及访问结果，不把独立副本计为本站归档的恢复成果。记录恢复点 UTC 时间、版本/构建、异地副本标识、导入和迁移结果、检查项、实际 RPO/RTO 与限制。仅解压成功、SHA-256 一致或健康接口返回 200 都不代表恢复通过。定期重演，尤其在配置、数据库版本或存储方式变化后；演练结束安全清除明文与临时实例。

真实故障时也先在新环境验证同一恢复点，再由运维执行独立的流量切换；不要把按快照恢复当作无损回滚（快照之后的写入不在其中）。
