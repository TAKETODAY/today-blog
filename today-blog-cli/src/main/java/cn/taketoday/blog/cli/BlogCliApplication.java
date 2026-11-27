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

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import cn.taketoday.blog.updater.DataPolicy;
import cn.taketoday.blog.updater.DatabaseBackup;
import cn.taketoday.blog.updater.DatabaseBackupFactory;
import cn.taketoday.blog.updater.ReleaseMappings;
import cn.taketoday.blog.updater.RuntimeLayout;
import cn.taketoday.blog.updater.UpgradeFiles;
import cn.taketoday.blog.updater.UpgradePackage;
import infra.aot.hint.annotation.RegisterReflection;
import infra.app.ApplicationRunner;
import infra.app.Banner;
import infra.app.InfraApplication;
import infra.beans.factory.ObjectProvider;
import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.ReflectiveScan;

import static infra.app.Application.forNormalApplication;

/**
 * 使用普通 infra 应用上下文执行一条运维命令，不启动 Web 服务。
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
@ReflectiveScan
@RegisterReflection(classes = Autowired.class)
@InfraApplication(proxyBeanMethods = false)
public class BlogCliApplication {

  public static void main(String[] args) {
    forNormalApplication(BlogCliApplication.class)
            .bannerMode(Banner.Mode.OFF)
            .run(args)
            .close();
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
  InstallWizard installWizard(UpgradeFiles files, RuntimeLayout layout) {
    return new InstallWizard(installRoot(), files, layout);
  }

  @Bean
  RuntimeLayout runtimeLayout() {
    return RuntimeLayout.fromEnvironment();
  }

  @Bean
  DatabaseBackupFactory databaseBackupFactory(UpgradeFiles files, RuntimeLayout layout) {
    Path config = Path.of(System.getenv().getOrDefault("BLOG_UPGRADE_CONFIG", DatabaseBackup.CONFIG.toString()));
    return new DatabaseBackupFactory(files, config, layout);
  }

  @Bean
  ApiServiceControl serviceControl(UpgradeFiles files, RuntimeLayout layout) {
    URI health = URI.create(System.getenv().getOrDefault("BLOG_HEALTH_URL",
            "http://127.0.0.1:8080/api/internal/health"));
    return switch (System.getenv().getOrDefault("BLOG_SERVICE_MANAGER", "direct")) {
      case "direct" -> {
        String launcher = System.getenv().getOrDefault("BLOG_API_LAUNCHER", "/usr/local/lib/today-blog/launch-api");
        yield new ProcessServiceControl(installRoot(), layout, health, List.of(launcher), files);
      }
      case "systemd" -> new SystemdServiceControl(installRoot(), health, files);
      default -> throw new IllegalArgumentException("BLOG_SERVICE_MANAGER 必须为 direct 或 systemd");
    };
  }

  @Bean
  DataCommands dataCommands(UpgradeFiles files, DatabaseBackupFactory backups, RuntimeLayout layout) {
    Path policy = Path.of(System.getenv().getOrDefault("BLOG_DATA_POLICY", DataPolicy.DEFAULT_CONFIG.toString()));
    return new DataCommands(layout, files, backups, policy);
  }

  @Bean
  ApplicationRunner commandRunner(UpgradeFiles files, UpgradePackage packages, ReleaseMappings mappings,
          ObjectProvider<UpgradeTaskStore> stores, DatabaseBackupFactory backups, ApiServiceControl service,
          InstallWizard wizard, DataCommands data, RuntimeLayout layout) {
    Path policy = Path.of(System.getenv().getOrDefault("BLOG_MAPPING_ROOTS", ReleaseMappings.POLICY.toString()));
    return new BlogCommands(installRoot(), layout, policy, files, packages, mappings, stores, backups, service,
            wizard, data);
  }

  private Path installRoot() {
    return Path.of(System.getenv().getOrDefault("BLOG_INSTALL_PATH", "/opt/today-blog"));
  }

}
