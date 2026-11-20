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

package cn.taketoday.blog.updater;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Properties;

import infra.app.Application;
import infra.app.ApplicationRunner;
import infra.app.InfraApplication;
import infra.context.annotation.Bean;

/**
 * 使用普通 infra 应用上下文执行一次升级任务，不启动 Web 服务。
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
@InfraApplication(proxyBeanMethods = false)
public class BlogUpdaterApplication {

  public static void main(String[] args) {
    Application.forNormalApplication(BlogUpdaterApplication.class).run(args).close();
  }

  @Bean
  UpgradeFiles upgradeFiles() {
    return new UpgradeFiles();
  }

  @Bean
  UpgradePackage upgradePackage() {
    return new UpgradePackage();
  }

  @Bean
  ReleaseMappings releaseMappings() {
    return new ReleaseMappings();
  }

  @Bean
  DatabaseBackupFactory databaseBackupFactory(UpgradeFiles files) {
    return new DatabaseBackupFactory(files, Path.of(System.getenv().getOrDefault(
            "BLOG_UPGRADE_CONFIG", DatabaseBackup.CONFIG.toString())));
  }

  @Bean
  ProcessServiceControl serviceControl(UpgradeFiles files) {
    URI health = URI.create(System.getenv().getOrDefault("BLOG_HEALTH_URL",
            "http://127.0.0.1:8080/api/internal/health"));
    Path work = Path.of(System.getenv().getOrDefault("BLOG_API_WORK_PATH", "/var/lib/today-blog"));
    String launcher = System.getenv().getOrDefault("BLOG_API_LAUNCHER", "/usr/local/lib/today-blog/launch-api");
    return new ProcessServiceControl(installRoot(), state(), work, health, List.of(launcher), files);
  }

  @Bean
  ApplicationRunner upgradeRunner(UpgradeFiles files, UpgradePackage packages, ReleaseMappings mappings,
          UpgradeDatabaseFactory databases, DatabaseBackupFactory backups, ProcessServiceControl service) {
    return args -> {
      List<String> commands = args.getNonOptionArgs();
      if (args.getSourceArgs().length == 3 && commands.size() == 3 && commands.get(0).equals("prepare-release")) {
        prepareRelease(Path.of(commands.get(1)), Path.of(commands.get(2)), packages, files);
        return;
      }
      if (args.getSourceArgs().length != 1 || args.getNonOptionArgs().size() != 1) {
        throw new IllegalArgumentException("用法: <任务 ID> | start-api | stop-api | prepare-release <ZIP> <目录>");
      }
      String command = args.getNonOptionArgs().get(0);
      Path root = installRoot();
      if (command.equals("start-api") || command.equals("stop-api")) {
        try (var channel = FileChannel.open(root.resolve("upgrade.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                var lock = channel.tryLock()) {
          if (lock == null) {
            throw new IllegalStateException("已有升级任务或 API 管理命令运行");
          }
          if (command.equals("start-api")) {
            service.start();
          }
          else {
            service.stop();
          }
        }
        return;
      }
      files.checkId(command);
      long id = Long.parseLong(command);
      Path policy = Path.of(System.getenv().getOrDefault("BLOG_MAPPING_ROOTS", ReleaseMappings.POLICY.toString()));
      try (UpgradeTaskStore store = new JdbcUpgradeTaskStore(databases.configured())) {
        UpgradeExecutor executor = new UpgradeExecutor(root, state(), policy, files, packages, mappings,
                service, store, (taskId, timeout) -> backups.configured().create(taskId, timeout));
        executor.execute(store.load(id));
      }
    };
  }

  private void prepareRelease(Path archive, Path target, UpgradePackage packages, UpgradeFiles files) throws Exception {
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

  private Path installRoot() {
    return Path.of(System.getenv().getOrDefault("BLOG_INSTALL_PATH", "/opt/today-blog"));
  }

  private Path state() {
    return Path.of(System.getenv().getOrDefault("BLOG_UPGRADE_STATE", "/var/lib/today-blog-upgrade"));
  }

}
