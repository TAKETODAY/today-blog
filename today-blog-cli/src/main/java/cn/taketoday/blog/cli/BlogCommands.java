/*
 * Copyright 2017 - 2026 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.blog.cli;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.blog.updater.BackupJob;
import cn.taketoday.blog.updater.BackupType;
import cn.taketoday.blog.updater.DatabaseBackupFactory;
import cn.taketoday.blog.updater.ReleaseMappings;
import cn.taketoday.blog.updater.RuntimeLayout;
import cn.taketoday.blog.updater.UpgradeFiles;
import cn.taketoday.blog.updater.UpgradeOptions;
import cn.taketoday.blog.updater.UpgradePackage;
import infra.app.ApplicationArguments;
import infra.app.ApplicationRunner;
import infra.beans.factory.ObjectProvider;

/**
 * CLI 的命令分发及运维操作。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/20
 */
final class BlogCommands implements ApplicationRunner {

  private static final String USAGE = """
          用法: today-blog <命令>
            start                       启动 API
            stop                        停止 API
            restart                     重启 API 并等待当前构建健康
            install --wizard            启动首次安装浏览器向导
            upgrade --task-id <ID>      执行已提交的升级任务
            data <命令>                 统一管理备份、升级包和日志（data --help）
            backup-site                 停写并生成加密的整站恢复点（systemd）
            backup-site --types <类型>   选择逗号分隔的类型，或 all
            --help                      显示帮助
          """;

  private final Path root;
  private final RuntimeLayout layout;
  private final Path policy;
  private final UpgradeFiles files;
  private final UpgradePackage packages;
  private final ReleaseMappings mappings;
  private final ObjectProvider<UpgradeTaskStore> stores;
  private final DatabaseBackupFactory backups;
  private final ApiServiceControl service;

  private final InstallWizard wizard;

  private final DataCommands data;

  BlogCommands(Path root, RuntimeLayout layout, Path policy, UpgradeFiles files, UpgradePackage packages,
          ReleaseMappings mappings, ObjectProvider<UpgradeTaskStore> stores, DatabaseBackupFactory backups,
          ApiServiceControl service, InstallWizard wizard, DataCommands data) {
    this.root = root;
    this.layout = layout;
    this.policy = policy;
    this.files = files;
    this.packages = packages;
    this.mappings = mappings;
    this.stores = stores;
    this.backups = backups;
    this.service = service;
    this.wizard = wizard;
    this.data = data;
  }

  @Override
  public void run(ApplicationArguments arguments) throws Exception {
    String[] args = arguments.getSourceArgs();
    if (args.length == 0 || (args.length == 1 && args[0].equals("--help"))) {
      System.out.print(USAGE);
    }
    else if (args[0].equals("data")) {
      data.run(java.util.Arrays.copyOfRange(args, 1, args.length));
    }
    else if (args.length == 1) {
      switch (args[0]) {
        case "start", "stop", "restart" -> controlApi(args[0]);
        case "backup-site" -> backupSite(BackupType.recommended(), null);
        default -> throw new IllegalArgumentException(USAGE);
      }
    }
    else if (args.length == 3 && args[0].equals("backup-site") && args[1].equals("--types")) {
      backupSite(BackupType.parse(args[2]), null);
    }
    else if (args.length == 3 && args[0].equals("backup-site") && args[1].equals("--job")) {
      backupJob(BackupJob.parse(args[2]));
    }
    else if (args.length == 3 && args[0].equals("upgrade") && args[1].equals("--task-id")) {
      upgrade(args[2]);
    }
    else if (args.length == 3 && args[0].equals("prepare-release")) {
      prepareRelease(Path.of(args[1]), Path.of(args[2]));
    }
    else if (args.length == 2 && args[0].equals("install") && args[1].equals("--wizard")) {
      wizard.run();
    }
    else {
      throw new IllegalArgumentException(USAGE);
    }
  }

  private Path backupSite(Set<BackupType> types, Boolean uploadOffsite) throws Exception {
    if (!(service instanceof SystemdServiceControl systemd)) {
      throw new IOException("整站备份目前仅支持 systemd 部署");
    }
    SiteBackup backup = new SiteBackup(files, packages, root, systemd,
            staging -> {
              var dump = backups.privateBackup(staging);
              return destination -> {
                var result = dump.create("1", 600);
                Files.move(result.location(), destination);
              };
            });
    Path config = Path.of(System.getenv().getOrDefault("BLOG_SITE_BACKUP_CONFIG", "/etc/today-blog/site-backup.properties"));
    return backup.create(config, types, uploadOffsite);
  }

  private void backupJob(BackupJob job) throws Exception {
    files.verifyTrusted(BackupJob.STATUS, false);
    Path status = BackupJob.STATUS.resolve(job.id() + ".properties");
    files.verifyTrusted(status, false);
    Properties record = files.read(status);
    if (!"STARTING".equals(record.getProperty("status")) || !job.id().equals(record.getProperty("id"))
            || !BackupType.encode(job.types()).equals(record.getProperty("types"))
            || !Boolean.toString(job.offsite()).equals(record.getProperty("offsite"))) {
      throw new IOException("备份任务与提交记录不匹配");
    }
    record.setProperty("status", "CREATING");
    files.write(status, record);
    try {
      files.verifyTrusted(BackupJob.POLICY, false);
      Properties allowed = files.read(BackupJob.POLICY);
      if (!BackupType.parse(allowed.getProperty("types", "")).containsAll(job.types())
              || (job.offsite() && !"true".equals(allowed.getProperty("offsite")))) {
        throw new IOException("备份范围未获得运维授权");
      }
      Path archive = backupSite(job.types(), job.offsite());
      recordLocalArchive(record, archive);
      record.setProperty("status", "AVAILABLE");
    }
    catch (SiteBackup.OffsiteUploadFailed failure) {
      record.setProperty("status", "OFFSITE_FAILED");
      recordLocalArchive(record, failure.archive());
      throw failure;
    }
    catch (Exception failure) {
      record.setProperty("status", "FAILED");
      throw failure;
    }
    finally {
      record.setProperty("completedAt", Instant.now().toString());
      files.write(status, record);
    }
  }

  private static void recordLocalArchive(Properties record, Path archive) throws IOException {
    record.setProperty("archive", archive.getFileName().toString());
    record.setProperty("bytes", Long.toString(Files.size(archive)));
  }

  private void controlApi(String command) throws Exception {
    try (var channel = FileChannel.open(root.resolve("upgrade.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            var lock = channel.tryLock()) {
      if (lock == null) {
        throw new IllegalStateException("已有升级任务或 API 管理命令运行");
      }
      switch (command) {
        case "start" -> service.start();
        case "stop" -> service.stop();
        case "restart" -> service.restartAndWait(UpgradeOptions.defaults().healthTimeoutSeconds());
      }
    }
  }

  private void upgrade(String taskId) throws Exception {
    files.checkId(taskId);
    long id = Long.parseLong(taskId);
    UpgradeTaskStore store = stores.get();
    UpgradeExecutor executor = new UpgradeExecutor(root, layout, policy, files, packages, mappings,
            service, store, (backupId, timeout) -> backups.configured().create(backupId, timeout));
    if (!executor.execute(store.load(id))) {
      throw new IOException("升级未成功，已回滚到旧版本；请查看任务 " + id + " 的记录");
    }
  }

  private void prepareRelease(Path archive, Path target) throws Exception {
    var manifest = packages.verify(archive);
    packages.extract(archive, Files.createDirectory(target), manifest);
    if (!target.resolve("api/bin/blog-api").toFile().setExecutable(true, false)) {
      throw new IOException("无法设置启动权限");
    }
    Properties metadata = new Properties();
    metadata.setProperty("version", manifest.version());
    metadata.setProperty("build", Long.toString(manifest.build()));
    files.write(target.resolve("release.properties"), metadata);
  }

}
