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

import cn.taketoday.blog.updater.DatabaseBackup;
import cn.taketoday.blog.updater.DatabaseBackupFactory;
import cn.taketoday.blog.updater.ReleaseMappings;
import cn.taketoday.blog.updater.UpgradeFiles;
import cn.taketoday.blog.updater.UpgradePackage;
import infra.aot.hint.annotation.RegisterReflection;
import infra.app.Application;
import infra.app.ApplicationRunner;
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
    forNormalApplication(BlogCliApplication.class).run(args).close();
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
  ApplicationRunner commandRunner(UpgradeFiles files, UpgradePackage packages, ReleaseMappings mappings,
          ObjectProvider<UpgradeTaskStore> stores, DatabaseBackupFactory backups, ProcessServiceControl service) {
    Path policy = Path.of(System.getenv().getOrDefault("BLOG_MAPPING_ROOTS", ReleaseMappings.POLICY.toString()));
    return new BlogCommands(installRoot(), state(), policy, files, packages, mappings, stores, backups, service);
  }

  private Path installRoot() {
    return Path.of(System.getenv().getOrDefault("BLOG_INSTALL_PATH", "/opt/today-blog"));
  }

  private Path state() {
    return Path.of(System.getenv().getOrDefault("BLOG_UPGRADE_STATE", "/var/lib/today-blog-upgrade"));
  }

}
