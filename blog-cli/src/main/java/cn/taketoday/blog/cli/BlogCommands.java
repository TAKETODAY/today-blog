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
import java.util.Properties;

import cn.taketoday.blog.updater.DatabaseBackupFactory;
import cn.taketoday.blog.updater.ReleaseMappings;
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
            upgrade --task-id <ID>      执行已提交的升级任务
            --help                      显示帮助
          """;

  private final Path root;
  private final Path state;
  private final Path policy;
  private final UpgradeFiles files;
  private final UpgradePackage packages;
  private final ReleaseMappings mappings;
  private final ObjectProvider<UpgradeTaskStore> stores;
  private final DatabaseBackupFactory backups;
  private final ProcessServiceControl service;

  BlogCommands(Path root, Path state, Path policy, UpgradeFiles files, UpgradePackage packages,
          ReleaseMappings mappings, ObjectProvider<UpgradeTaskStore> stores, DatabaseBackupFactory backups,
          ProcessServiceControl service) {
    this.root = root;
    this.state = state;
    this.policy = policy;
    this.files = files;
    this.packages = packages;
    this.mappings = mappings;
    this.stores = stores;
    this.backups = backups;
    this.service = service;
  }

  @Override
  public void run(ApplicationArguments arguments) throws Exception {
    String[] args = arguments.getSourceArgs();
    if (args.length == 0 || (args.length == 1 && args[0].equals("--help"))) {
      System.out.print(USAGE);
    }
    else if (args.length == 1) {
      switch (args[0]) {
        case "start", "stop", "restart" -> controlApi(args[0]);
        default -> throw new IllegalArgumentException(USAGE);
      }
    }
    else if (args.length == 3 && args[0].equals("upgrade") && args[1].equals("--task-id")) {
      upgrade(args[2]);
    }
    else if (args.length == 3 && args[0].equals("prepare-release")) {
      prepareRelease(Path.of(args[1]), Path.of(args[2]));
    }
    else {
      throw new IllegalArgumentException(USAGE);
    }
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
    UpgradeExecutor executor = new UpgradeExecutor(root, state, policy, files, packages, mappings,
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
