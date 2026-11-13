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

package cn.taketoday.blog.web.console;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import cn.taketoday.blog.log.Logging;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.backup.AppOperationStatus;
import cn.taketoday.blog.backup.BackupOperationService;
import cn.taketoday.blog.updater.DatabaseBackup;
import cn.taketoday.blog.updater.UpgradeFiles;
import cn.taketoday.blog.updater.UpgradeOptions;
import cn.taketoday.blog.updater.UpgradePackage;
import cn.taketoday.blog.updater.UpgradeStore;
import cn.taketoday.blog.web.ErrorMessageException;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import infra.http.HttpStatus;
import infra.web.annotation.GET;
import infra.web.annotation.POST;
import infra.web.annotation.PUT;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;
import infra.web.multipart.Part;

/**
 * 管理端仅上传与派发任务，安装与重启由独立 systemd unit 执行。
 */
@RequiresBlogger
@RestController
@RequestMapping("/api/console/upgrade")
class UpgradeHttpHandler {

  /**
   * 尚未结束的更新任务状态，存在这些状态时禁止再次启动更新。
   */
  private static final Set<AppOperationStatus> RUNNING_STATUSES = EnumSet.of(
          AppOperationStatus.STARTING,
          AppOperationStatus.VALIDATING,
          AppOperationStatus.INSTALLING,
          AppOperationStatus.BACKING_UP,
          AppOperationStatus.RESTARTING,
          AppOperationStatus.HEALTH_CHECKING,
          AppOperationStatus.ROLLING_BACK);

  private final Path state = Path.of(System.getenv().getOrDefault("BLOG_UPGRADE_STATE", "/var/lib/today-blog-upgrade"));

  private final UpgradeStore store = new UpgradeStore(state);

  private final BackupOperationService operations;

  UpgradeHttpHandler(BackupOperationService operations) {
    this.operations = operations;
  }

  private UpgradeOptions options(Properties values) {
    try {
      return UpgradeOptions.from(values);
    }
    catch (IllegalArgumentException invalid) {
      throw ErrorMessageException.failed("更新参数无效：备份超时 30–3600 秒，健康检查 10–600 秒，备注最多 500 字");
    }
  }

  @GET("/settings")
  public Properties settings() throws Exception {
    return store.settings().properties();
  }

  @PUT("/settings")
  @Logging(title = "修改更新设置", content = "操作人: [#{#user.id}]")
  public synchronized Properties settings(@RequestBody Properties values, User user) throws Exception {
    enabled();
    UpgradeOptions options = options(values);
    store.saveSettings(options);
    return options.properties();
  }

  @GET("/history")
  public java.util.List<Properties> history() throws Exception {
    return operations.upgradeHistory();
  }

  @GET("/environment")
  public Map<String, Object> environment() throws Exception {
    boolean backupReady;
    long backupFreeBytes = 0;
    try {
      backupFreeBytes = DatabaseBackup.configured().usableSpace();
      backupReady = true;
    }
    catch (Exception unavailable) {
      backupReady = false;
    }
    Path root = Path.of(System.getenv().getOrDefault("BLOG_INSTALL_PATH", "/opt/today-blog"));
    boolean updaterReady = System.getenv("BLOG_RELEASE_PATH") != null
            && Files.isRegularFile(Path.of("/usr/local/lib/today-blog/updater.jar"))
            && Files.isRegularFile(Path.of("/etc/systemd/system/today-blog-updater@.service"));
    return Map.of("updaterReady", updaterReady,
            "backupReady", backupReady,
            "backupFreeBytes", backupFreeBytes,
            "installFreeBytes", Files.exists(root) ? Files.getFileStore(root).getUsableSpace() : 0);
  }

  @GET("/current")
  public Properties current() throws Exception {
    String release = System.getenv("BLOG_RELEASE_PATH");
    if (release == null) {
      Properties result = new Properties();
      result.setProperty("version", "开发环境（未启用在线升级）");
      return result;
    }
    return UpgradeFiles.read(Path.of(release).resolve("release.properties"));
  }

  @POST("/packages")
  @Logging(title = "上传升级包", content = "操作人: [#{#user.id}]")
  public synchronized Properties upload(Part file, User user) throws Exception {
    enabled();
    if (file.getContentLength() <= 0 || file.getContentLength() > UpgradePackage.MAX_PACKAGE_BYTES) {
      throw ErrorMessageException.failed("升级包大小必须在 1 字节至 512 MiB 之间");
    }
    String id = UUID.randomUUID().toString();
    Path task = Files.createDirectories(state.resolve("incoming").resolve(id));
    Path temp = task.resolve("upload.tmp");
    try {
      try (var input = file.getInputStream(); var output = Files.newOutputStream(temp)) {
        byte[] buffer = new byte[65536];
        long total = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
          total += count;
          if (total > UpgradePackage.MAX_PACKAGE_BYTES) {
            throw new java.io.IOException("升级包过大");
          }
          output.write(buffer, 0, count);
        }
      }
      var manifest = UpgradePackage.verify(temp);
      long currentBuild = Long.parseLong(current().getProperty("build"));
      if (manifest.build() <= currentBuild) {
        throw ErrorMessageException.failed("禁止重复安装或降级");
      }
      Files.move(temp, task.resolve("package.zip"), StandardCopyOption.ATOMIC_MOVE);
      Properties metadata = new Properties();
      metadata.setProperty("id", id);
      metadata.setProperty("createdAt", java.time.Instant.now().toString());
      metadata.setProperty("packageBytes", Long.toString(Files.size(task.resolve("package.zip"))));
      metadata.setProperty("version", manifest.version());
      metadata.setProperty("build", Long.toString(manifest.build()));
      metadata.setProperty("operatorId", String.valueOf(user.getId()));
      metadata.setProperty("status", AppOperationStatus.READY.name());
      metadata.setProperty("message", "文件完整性校验通过（未验证来源），等待确认升级");
      UpgradeFiles.write(task.resolve("metadata.properties"), metadata);
      return metadata;
    }
    catch (Exception error) {
      Files.deleteIfExists(task.resolve("package.zip"));
      throw ErrorMessageException.failed("升级包校验失败: " + error.getMessage());
    }
    finally {
      Files.deleteIfExists(temp);
    }
  }

  @POST("/{id}/start")
  @Logging(title = "启动系统升级", content = "任务: [#{#id}]，操作人: [#{#user.id}]")
  public synchronized Map<String, String> start(@PathVariable String id, @RequestBody Properties values, User user) throws Exception {
    enabled();
    UpgradeFiles.checkId(id);
    UpgradeOptions options = options(values);
    Map<String, Object> environment = environment();
    if (!Boolean.TRUE.equals(environment.get("updaterReady"))
            || (options.backupEnabled() && !Boolean.TRUE.equals(environment.get("backupReady")))) {
      throw ErrorMessageException.failed("升级器或数据库备份配置未就绪");
    }
    if ((long) environment.get("installFreeBytes") < 3L * 1024 * 1024 * 1024
            || (options.backupEnabled() && (long) environment.get("backupFreeBytes") < 256L * 1024 * 1024)) {
      throw ErrorMessageException.failed("安装盘或备份盘可用空间不足");
    }
    for (Properties task : store.history()) {
      AppOperationStatus status = AppOperationStatus.of(task.getProperty("status"));
      if (status != null && RUNNING_STATUSES.contains(status)) {
        throw ErrorMessageException.failed("已有更新任务运行，请先确认其结果", HttpStatus.CONFLICT);
      }
    }
    if (AppOperationStatus.of(store.task(id).getProperty("status")) != AppOperationStatus.READY) {
      throw ErrorMessageException.failed("该任务已提交，不可重复启动", HttpStatus.CONFLICT);
    }
    store.snapshot(id, options, user.getId(), current());
    try {
      // 数据库记录失败时不派发高权限任务，磁盘记录仍保留供核对。
      operations.recordUpgrade(id);
      Process process = new ProcessBuilder("/usr/bin/sudo", "-n", "/usr/bin/systemctl", "start", "--no-block",
              "today-blog-updater@" + id + ".service").redirectErrorStream(true)
              .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
      if (!process.waitFor(10, TimeUnit.SECONDS) || process.exitValue() != 0) {
        process.destroyForcibly();
        throw new java.io.IOException("无法启动升级服务");
      }
    }
    catch (Exception failure) {
      Path metadataPath = state.resolve("incoming").resolve(id).resolve("metadata.properties");
      Properties metadata = UpgradeFiles.read(metadataPath);
      metadata.setProperty("status", AppOperationStatus.FAILED.name());
      metadata.setProperty("finishedAt", java.time.Instant.now().toString());
      metadata.setProperty("message", "无法启动升级服务，请检查 sudoers 和 systemd 日志");
      UpgradeFiles.write(metadataPath, metadata);
      throw ErrorMessageException.failed(metadata.getProperty("message"));
    }
    return Map.of("id", id, "status", AppOperationStatus.STARTING.name());
  }

  @GET("/{id}")
  public Properties status(@PathVariable String id) throws Exception {
    return operations.upgrade(id);
  }

  private void enabled() {
    if (System.getenv("BLOG_RELEASE_PATH") == null) {
      throw ErrorMessageException.failed("请先完成 systemd 部署配置");
    }
  }
}
