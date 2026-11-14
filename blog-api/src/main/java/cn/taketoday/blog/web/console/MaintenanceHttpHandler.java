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

import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import cn.taketoday.blog.MaintenanceStatus;
import cn.taketoday.blog.ServerStatus;
import cn.taketoday.blog.backup.BackupOperationService;
import cn.taketoday.blog.backup.BackupOperationStatus;
import cn.taketoday.blog.log.Logging;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.updater.DatabaseBackup;
import cn.taketoday.blog.updater.ReleaseMapping;
import cn.taketoday.blog.updater.ReleaseMappings;
import cn.taketoday.blog.updater.UpgradeFiles;
import cn.taketoday.blog.updater.UpgradeOptions;
import cn.taketoday.blog.updater.UpgradePackage;
import cn.taketoday.blog.updater.UpgradeStore;
import cn.taketoday.blog.upgrade.ReleaseMappingService;
import cn.taketoday.blog.util.StringUtils;
import cn.taketoday.blog.web.ErrorMessageException;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import infra.context.ApplicationEventPublisher;
import infra.core.ApplicationTemp;
import infra.http.HttpStatus;
import infra.web.annotation.GET;
import infra.web.annotation.POST;
import infra.web.annotation.PUT;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;
import infra.web.multipart.Part;

import static cn.taketoday.blog.BlogConstant.upgradeLogger;

/**
 * 管理端维护状态接口。
 * <p>
 * 维护状态由 {@link MaintenanceStatus} 统一管理，升级流程开启的状态会在升级结束后自动清除，
 * 管理员手动开启的状态需要主动关闭。
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
@RequiresBlogger
@RestController
@RequestMapping("/api/console/maintenance")
class MaintenanceHttpHandler {

  // 系统维护中，请稍后重试
  public static final String DEFAULT_MESSAGE = "系统维护中，请稍后重试";

  public static final int DEFAULT_RETRY_AFTER_SECONDS = 120;

  /**
   * 尚未结束的更新任务状态，存在这些状态时禁止再次启动更新。
   */
  private static final Set<BackupOperationStatus> RUNNING_STATUSES = EnumSet.of(
          BackupOperationStatus.STARTING,
          BackupOperationStatus.VALIDATING,
          BackupOperationStatus.INSTALLING,
          BackupOperationStatus.BACKING_UP,
          BackupOperationStatus.RESTARTING,
          BackupOperationStatus.HEALTH_CHECKING,
          BackupOperationStatus.ROLLING_BACK);

  private final Path state = Path.of(System.getenv().getOrDefault("BLOG_UPGRADE_STATE", "/var/lib/today-blog-upgrade"));

  private final Path installRoot = Path.of(System.getenv().getOrDefault("BLOG_INSTALL_PATH", "/opt/today-blog"));

  private final Path mappingPolicy = Path.of(System.getenv().getOrDefault(
          "BLOG_MAPPING_ROOTS", ReleaseMappings.POLICY.toString()));

  private final ServerStatus serverStatus;

  private final ApplicationTemp applicationTemp;

  private final ApplicationEventPublisher eventPublisher;

  private final UpgradeStore store = new UpgradeStore(state);

  private final BackupOperationService operations;

  private final ReleaseMappingService releaseMappings;

  MaintenanceHttpHandler(ServerStatus serverStatus, ApplicationTemp applicationTemp,
          ApplicationEventPublisher eventPublisher, BackupOperationService operations,
          ReleaseMappingService releaseMappings) {
    this.serverStatus = serverStatus;
    this.applicationTemp = applicationTemp;
    this.eventPublisher = eventPublisher;
    this.operations = operations;
    this.releaseMappings = releaseMappings;
  }

  @GET
  public MaintenanceStatus maintenance() {
    return serverStatus.getMaintenanceStatus();
  }

  @PUT
  @Logging(title = "切换维护状态", content = "维护: [#{#request.enabled}]")
  public synchronized MaintenanceStatus maintenance(@RequestBody MaintenanceRequest request) throws Exception {
    if (request.enabled) {
      String message = request.message;
      if (StringUtils.isBlank(message)) {
        message = DEFAULT_MESSAGE;
      }
      MaintenanceStatus status = new MaintenanceStatus(request.enabled,
              message, retryAfter(request.retryAfter), MaintenanceStatus.Source.MANUAL);
      serverStatus.setMaintenanceStatus(status);
      return status;
    }
    serverStatus.setMaintenanceStatus(MaintenanceStatus.DISABLED);
    return MaintenanceStatus.DISABLED;
  }

  private UpgradeOptions options(Properties values) {
    try {
      return UpgradeOptions.from(values);
    }
    catch (IllegalArgumentException invalid) {
      throw ErrorMessageException.failed("更新参数无效：备份超时 30–3600 秒，健康检查 10–600 秒，备注最多 500 字");
    }
  }

  @GET("/upgrade/settings")
  public Properties settings() throws Exception {
    return store.settings().properties();
  }

  @PUT("/upgrade/settings")
  @Logging(title = "修改更新设置", content = "操作人: [#{#user.id}]")
  public synchronized Properties settings(@RequestBody Properties values, User user) throws Exception {
    enabled();
    UpgradeOptions options = options(values);
    store.saveSettings(options);
    return options.properties();
  }

  @GET("/upgrade/mappings")
  public ReleaseMappingSettings mappings() throws Exception {
    List<ReleaseMapping> mappings = releaseMappings.findAll();
    try {
      List<Path> roots = ReleaseMappings.roots(mappingPolicy);
      ReleaseMappings.validate(mappings, roots);
      ReleaseMappings.validateTargets(mappings, installRoot, state);
      return new ReleaseMappingSettings(mappings, roots.stream().map(Path::toString).toList(),
              true, "映射目标目录由服务器白名单限定");
    }
    catch (Exception unavailable) {
      return new ReleaseMappingSettings(mappings, List.of(), false, "映射目标根目录配置未就绪");
    }
  }

  @PUT("/upgrade/mappings")
  @Logging(title = "修改文件映射", content = "操作人: [#{#user.id}]")
  public synchronized ReleaseMappingSettings mappings(@RequestBody ReleaseMappingSettingsRequest request, User user)
          throws Exception {
    enabled();
    List<ReleaseMapping> mappings = request == null || request.mappings() == null
            ? List.of() : List.copyOf(request.mappings());
    try {
      ReleaseMappings.validate(mappings, ReleaseMappings.roots(mappingPolicy));
      ReleaseMappings.validateTargets(mappings, installRoot, state);
    }
    catch (Exception invalid) {
      throw ErrorMessageException.failed(invalid.getMessage());
    }
    releaseMappings.replaceAll(mappings, user.getId());
    return mappings();
  }

  @GET("/upgrade/history")
  public List<Properties> history() throws Exception {
    return operations.upgradeHistory();
  }

  @GET("/upgrade/environment")
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
    boolean updaterReady = System.getenv("BLOG_RELEASE_PATH") != null
            && Files.isRegularFile(Path.of("/usr/local/lib/today-blog/updater.jar"))
            && Files.isRegularFile(Path.of("/etc/systemd/system/today-blog-updater@.service"));
    return Map.of("updaterReady", updaterReady,
            "backupReady", backupReady,
            "backupFreeBytes", backupFreeBytes,
            "installFreeBytes", Files.exists(installRoot) ? Files.getFileStore(installRoot).getUsableSpace() : 0);
  }

  @GET("/upgrade/current")
  public Properties current() throws Exception {
    String release = System.getenv("BLOG_RELEASE_PATH");
    if (release == null) {
      Properties result = new Properties();
      result.setProperty("version", "开发环境（未启用在线升级）");
      return result;
    }
    return UpgradeFiles.read(Path.of(release).resolve("release.properties"));
  }

  @POST("/upgrade/packages")
  @Logging(title = "上传升级包", content = "文件: [#{#file.originalFilename}], 文件大小：[#{#file.contentLength}]")
  public Properties upload(Part file, User user) throws Exception {
    upgradeLogger.info("Upload TODAY BLOG Server package");
    enabled();
    if (file.getContentLength() <= 0 || file.getContentLength() > UpgradePackage.MAX_PACKAGE_BYTES) {
      throw ErrorMessageException.failed("升级包大小必须在 1 字节至 512 MiB 之间");
    }
    String id = UUID.randomUUID().toString();
    Path task = Files.createDirectories(state.resolve("incoming").resolve(id));
    Path temp = task.resolve("upload.tmp");
    try {
      file.transferTo(temp);

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
      metadata.setProperty("status", BackupOperationStatus.READY.name());
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

  @POST("/upgrade/{id}/start")
  @Logging(title = "启动系统升级", content = "任务: [#{#id}]，操作人: [#{#user.id}]")
  public synchronized Map<String, String> start(@PathVariable String id, @RequestBody Properties values, User user) throws Exception {
    Path unpackPath = applicationTemp.getDir("upload");

    upgradeLogger.debug("Unzipped file {}", unpackPath);
//    Path backupPath = backupBasePath.resolve(BuildProperties.version);
//    File[] files = unpackPath.resolve("ironman-server").toFile().listFiles();
//    Assert.state(files != null, "The directory structure of the files you provided is incorrect.");

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
      BackupOperationStatus status = BackupOperationStatus.of(task.getProperty("status"));
      if (status != null && RUNNING_STATUSES.contains(status)) {
        throw ErrorMessageException.failed("已有更新任务运行，请先确认其结果", HttpStatus.CONFLICT);
      }
    }
    if (BackupOperationStatus.of(store.task(id).getProperty("status")) != BackupOperationStatus.READY) {
      throw ErrorMessageException.failed("该任务已提交，不可重复启动", HttpStatus.CONFLICT);
    }
    List<ReleaseMapping> mappings = releaseMappings.findAll();
    if (!mappings.isEmpty()) {
      try {
        ReleaseMappings.validate(mappings, ReleaseMappings.roots(mappingPolicy));
        ReleaseMappings.validateTargets(mappings, installRoot, state);
      }
      catch (Exception invalid) {
        throw ErrorMessageException.failed("文件映射配置无效: " + invalid.getMessage());
      }
    }
    store.snapshot(id, options, mappings, user.getId(), current());
    enableMaintenance();
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
      serverStatus.setMaintenanceStatus(MaintenanceStatus.DISABLED);

      Path metadataPath = state.resolve("incoming").resolve(id).resolve("metadata.properties");
      Properties metadata = UpgradeFiles.read(metadataPath);
      metadata.setProperty("status", BackupOperationStatus.FAILED.name());
      metadata.setProperty("finishedAt", java.time.Instant.now().toString());
      metadata.setProperty("message", "无法启动升级服务，请检查 sudoers 和 systemd 日志");
      UpgradeFiles.write(metadataPath, metadata);
      throw ErrorMessageException.failed(metadata.getProperty("message"));
    }
    return Map.of("id", id, "status", BackupOperationStatus.STARTING.name());
  }

  @GET("/upgrade/{id}")
  public Properties status(@PathVariable String id) throws Exception {
    return operations.upgrade(id);
  }

  private void enableMaintenance() {
    MaintenanceStatus status = new MaintenanceStatus(true, DEFAULT_MESSAGE,
            DEFAULT_RETRY_AFTER_SECONDS, MaintenanceStatus.Source.UPGRADE);
    serverStatus.setMaintenanceStatus(status);
  }

  private void enabled() {
    if (System.getenv("BLOG_RELEASE_PATH") == null) {
      throw ErrorMessageException.failed("请先完成 systemd 部署配置");
    }
  }

  private static int retryAfter(@Nullable Integer value) {
    if (value == null) {
      return DEFAULT_RETRY_AFTER_SECONDS;
    }
    if (value < 1 || value > 86400) {
      throw ErrorMessageException.failed("重试等待时间必须在 1 至 86400 秒之间");
    }
    return value;
  }

  static class MaintenanceRequest {
    public boolean enabled;
    public @Nullable String message;
    public @Nullable Integer retryAfter;
  }

  /**
   * 前端静态文件的映射设置。目标必须位于服务器白名单目录内。
   */
  public record ReleaseMappingSettings(List<ReleaseMapping> mappings, List<String> roots,
          boolean ready, String message) {
  }

  public record ReleaseMappingSettingsRequest(List<ReleaseMapping> mappings) {
  }

}
