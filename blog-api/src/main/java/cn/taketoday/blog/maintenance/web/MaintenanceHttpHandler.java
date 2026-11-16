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

package cn.taketoday.blog.maintenance.web;

import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import cn.taketoday.blog.log.Logging;
import cn.taketoday.blog.maintenance.MaintenanceStatus;
import cn.taketoday.blog.maintenance.ServerStatus;
import cn.taketoday.blog.maintenance.upgrade.ReleaseMappingService;
import cn.taketoday.blog.maintenance.upgrade.UpgradePackageEntity;
import cn.taketoday.blog.maintenance.upgrade.UpgradePackageService;
import cn.taketoday.blog.maintenance.upgrade.UpgradePackageStatus;
import cn.taketoday.blog.maintenance.upgrade.UpgradePathMappingService;
import cn.taketoday.blog.maintenance.upgrade.UpgradeRecordEntity;
import cn.taketoday.blog.maintenance.upgrade.UpgradeRecordService;
import cn.taketoday.blog.maintenance.upgrade.UpgradeSettingService;
import cn.taketoday.blog.maintenance.upgrade.UpgradeStatus;
import cn.taketoday.blog.updater.DatabaseBackup;
import cn.taketoday.blog.updater.ReleaseMapping;
import cn.taketoday.blog.updater.ReleaseMappings;
import cn.taketoday.blog.updater.UpgradeFiles;
import cn.taketoday.blog.updater.UpgradeOptions;
import cn.taketoday.blog.updater.UpgradePackage;
import cn.taketoday.blog.util.StringUtils;
import cn.taketoday.blog.error.ProblemException;
import cn.taketoday.blog.error.CommonProblems;
import cn.taketoday.blog.maintenance.upgrade.UpgradeProblems;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import infra.web.annotation.DELETE;
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
 * 管理端维护状态与升级接口。
 * <p>
 * 升级包、升级记录、文件映射和默认参数全部保存在数据库；本地文件只保存升级包和备份产物。
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
@RequiresBlogger
@RestController
@RequestMapping("/api/console/maintenance")
class MaintenanceHttpHandler {

  public static final String DEFAULT_MESSAGE = "系统维护中，请稍后重试";

  public static final int DEFAULT_RETRY_AFTER_SECONDS = 120;

  private final Path state = Path.of(System.getenv().getOrDefault("BLOG_UPGRADE_STATE", "/var/lib/today-blog-upgrade"));

  private final Path installRoot = Path.of(System.getenv().getOrDefault("BLOG_INSTALL_PATH", "/opt/today-blog"));

  private final Path mappingPolicy = Path.of(System.getenv().getOrDefault(
          "BLOG_MAPPING_ROOTS", ReleaseMappings.POLICY.toString()));

  private final Path packageRoot;

  private final ServerStatus serverStatus;

  private final UpgradePackageService packages;

  private final UpgradeRecordService records;

  private final UpgradePathMappingService pathMappings;

  private final UpgradeSettingService settings;

  private final ReleaseMappingService releaseMappings;

  MaintenanceHttpHandler(ServerStatus serverStatus, UpgradePackageService packages,
          UpgradeRecordService records, UpgradePathMappingService pathMappings,
          UpgradeSettingService settings, ReleaseMappingService releaseMappings) {
    this.serverStatus = serverStatus;
    this.packages = packages;
    this.records = records;
    this.pathMappings = pathMappings;
    this.settings = settings;
    this.releaseMappings = releaseMappings;
    this.packageRoot = state.resolve("packages").toAbsolutePath().normalize();
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
        message = null;
      }
      MaintenanceStatus status = new MaintenanceStatus(request.enabled,
              message, retryAfter(request.retryAfter), MaintenanceStatus.Source.MANUAL);
      serverStatus.setMaintenanceStatus(status);
      return status;
    }
    serverStatus.setMaintenanceStatus(MaintenanceStatus.DISABLED);
    return MaintenanceStatus.DISABLED;
  }

  @GET("/upgrade/settings")
  public UpgradeOptions settings() {
    return settings.get();
  }

  @PUT("/upgrade/settings")
  @Logging(title = "修改升级设置")
  public synchronized UpgradeOptions settings(@RequestBody UpgradeOptions options) {
    enabled();
    settings.save(options);
    return settings.get();
  }

  @GET("/upgrade/mappings")
  public ReleaseMappingSettings mappings() {
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
  @Logging(title = "修改文件映射")
  public synchronized ReleaseMappingSettings mappings(@RequestBody ReleaseMappingSettingsRequest request) {
    enabled();
    List<ReleaseMapping> mappings = request == null || request.mappings() == null
            ? List.of() : List.copyOf(request.mappings());
    try {
      ReleaseMappings.validate(mappings, ReleaseMappings.roots(mappingPolicy));
      ReleaseMappings.validateTargets(mappings, installRoot, state);
    }
    catch (Exception invalid) {
      upgradeLogger.warn("Invalid release mappings", invalid);
      throw new ProblemException(UpgradeProblems.MAPPING_INVALID);
    }
    releaseMappings.replaceAll(mappings);
    return mappings();
  }

  @GET("/upgrade/packages")
  public List<PackageView> packages() {
    return packages.findAll().stream().map(PackageView::from).toList();
  }

  @POST("/upgrade/packages")
  @Logging(title = "上传升级包", content = "文件: [#{#file.originalFilename}], 文件大小：[#{#file.contentLength}]")
  public PackageView upload(Part file) throws Exception {
    upgradeLogger.info("Upload TODAY BLOG Server package");
    enabled();
    if (file.getContentLength() <= 0 || file.getContentLength() > UpgradePackage.MAX_PACKAGE_BYTES) {
      throw new ProblemException(UpgradeProblems.PACKAGE_SIZE_INVALID);
    }
    String id = UUID.randomUUID().toString();
    Path directory = Files.createDirectories(packageRoot.resolve(id));
    Path temp = directory.resolve("upload.tmp");
    Path location = directory.resolve("package.zip");
    try {
      file.transferTo(temp);
      var manifest = UpgradePackage.verify(temp);
      Files.move(temp, location, StandardCopyOption.ATOMIC_MOVE);
      String originalName = file.getOriginalFilename();
      if (StringUtils.isBlank(originalName)) {
        originalName = "package.zip";
      }
      UpgradePackageEntity entity = new UpgradePackageEntity();
      entity.setOriginalName(originalName);
      entity.setDisplayName(originalName);
      entity.setStatus(UpgradePackageStatus.READY);
      entity.setVersion(manifest.version());
      entity.setBuild(manifest.build());
      entity.setFileSize(Files.size(location));
      entity.setSha256(sha256(location));
      entity.setEntryCount(manifest.hashes().size());
      entity.setLocation(location.toString());
      entity.setCreatedAt(Instant.now());
      packages.create(entity);
      return PackageView.from(entity);
    }
    catch (Exception error) {
      deleteDirectory(directory);
      upgradeLogger.warn("Upgrade package upload failed", error);
      throw new ProblemException(UpgradeProblems.PACKAGE_INVALID);
    }
    finally {
      Files.deleteIfExists(temp);
    }
  }

  @PUT("/upgrade/packages/{id}")
  @Logging(title = "修改升级包")
  public PackageView updatePackage(@PathVariable long id, @RequestBody PackageUpdate request) {
    enabled();
    return PackageView.from(packages.update(id, request.displayName(), request.note()));
  }

  @DELETE("/upgrade/packages/{id}")
  @Logging(title = "删除升级包", content = "升级包: [#{#id}]")
  public void deletePackage(@PathVariable long id) {
    enabled();
    if (records.hasRunning(id)) {
      throw new ProblemException(UpgradeProblems.PACKAGE_IN_USE);
    }
    packages.delete(id);
  }

  @POST("/upgrade/packages/{id}/start")
  @Logging(title = "启动系统升级", content = "升级包: [#{#id}]")
  public synchronized RecordView start(@PathVariable long id, @RequestBody StartUpgradeRequest request) throws Exception {
    enabled();
    UpgradePackageEntity entity = packages.find(id);
    if (entity.getStatus() != UpgradePackageStatus.READY) {
      throw new ProblemException(UpgradeProblems.PACKAGE_CONSUMED);
    }
    UpgradeOptions options = request == null || request.options() == null ? settings.get() : request.options();
    List<ReleaseMapping> mappings = releaseMappings.findAll();
    if (!mappings.isEmpty()) {
      try {
        ReleaseMappings.validate(mappings, ReleaseMappings.roots(mappingPolicy));
        ReleaseMappings.validateTargets(mappings, installRoot, state);
      }
      catch (Exception invalid) {
        upgradeLogger.warn("Invalid release mappings", invalid);
        throw new ProblemException(UpgradeProblems.MAPPING_INVALID);
      }
    }
    Map<String, Object> environment = environment();
    if (!Boolean.TRUE.equals(environment.get("updaterReady"))
            || (options.backupEnabled() && !Boolean.TRUE.equals(environment.get("backupReady")))) {
      throw new ProblemException(UpgradeProblems.ENVIRONMENT_NOT_READY);
    }
    if ((long) environment.get("installFreeBytes") < 3L * 1024 * 1024 * 1024
            || (options.backupEnabled() && (long) environment.get("backupFreeBytes") < 256L * 1024 * 1024)) {
      throw new ProblemException(UpgradeProblems.SPACE_INSUFFICIENT);
    }
    if (records.running() != null) {
      throw new ProblemException(UpgradeProblems.IN_PROGRESS);
    }

    CurrentRelease current = currentRelease();
    Long oldBuild = parseBuild(current.build());
    UpgradeRecordEntity record = new UpgradeRecordEntity();
    record.setPackageId(entity.getId());
    record.setPackageName(entity.getDisplayName());
    record.setPackageVersion(entity.getVersion());
    record.setPackageBuild(entity.getBuild());
    record.setPackageSha256(entity.getSha256());
    record.setPackagePath(entity.getLocation());
    record.setOldVersion(current.version());
    record.setOldBuild(oldBuild);
    record.setStatus(UpgradeStatus.STARTING);
    record.setBackupEnabled(options.backupEnabled());
    record.setBackupTimeoutSeconds(options.backupTimeoutSeconds());
    record.setHealthTimeoutSeconds(options.healthTimeoutSeconds());
    record.setNote(options.note());
    record.setDeletePackageAfterUpgrade(request != null && request.deletePackage());
    record.setMessage("任务已提交，请等待升级器启动");
    record.setCreatedAt(Instant.now());
    records.create(record);
    long recordId = record.getId();
    pathMappings.replace(recordId, mappings);
    packages.markConsumed(entity.getId());
    enableMaintenance();
    try {
      Process process = new ProcessBuilder("/usr/bin/sudo", "-n", "/usr/bin/systemctl", "start", "--no-block",
              "today-blog-updater@" + recordId + ".service").redirectErrorStream(true)
              .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
      if (!process.waitFor(10, TimeUnit.SECONDS) || process.exitValue() != 0) {
        process.destroyForcibly();
        throw new java.io.IOException("无法启动升级服务");
      }
    }
    catch (Exception failure) {
      serverStatus.setMaintenanceStatus(MaintenanceStatus.DISABLED);
      packages.markReady(entity.getId());
      records.failed(recordId, "无法启动升级服务，请检查 sudoers 和 systemd 日志");
      upgradeLogger.error("Cannot start upgrade service", failure);
      throw new ProblemException(UpgradeProblems.START_FAILED);
    }
    return RecordView.from(records.find(recordId));
  }

  @GET("/upgrade/records")
  public List<RecordView> records() {
    return records.findAll().stream().map(RecordView::from).toList();
  }

  @GET("/upgrade/records/{id}")
  public RecordView record(@PathVariable long id) {
    return RecordView.from(records.find(id));
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
  public CurrentRelease current() throws Exception {
    return currentRelease();
  }

  private CurrentRelease currentRelease() throws Exception {
    String release = System.getenv("BLOG_RELEASE_PATH");
    if (release == null) {
      return new CurrentRelease("开发环境（未启用在线升级）", null);
    }
    Properties properties = UpgradeFiles.read(Path.of(release).resolve("release.properties"));
    return new CurrentRelease(properties.getProperty("version", ""), properties.getProperty("build"));
  }

  private static @Nullable Long parseBuild(@Nullable String build) {
    return build == null || build.isBlank() ? null : Long.valueOf(build);
  }

  private static String sha256(Path file) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    try (var input = Files.newInputStream(file)) {
      byte[] buffer = new byte[65536];
      int count;
      while ((count = input.read(buffer)) != -1) {
        digest.update(buffer, 0, count);
      }
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  private static void deleteDirectory(Path directory) {
    try (var paths = Files.walk(directory)) {
      for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
        Files.deleteIfExists(path);
      }
    }
    catch (Exception ignored) {
    }
  }

  private void enableMaintenance() {
    MaintenanceStatus status = new MaintenanceStatus(true, null,
            DEFAULT_RETRY_AFTER_SECONDS, MaintenanceStatus.Source.UPGRADE);
    serverStatus.setMaintenanceStatus(status);
  }

  private void enabled() {
    if (System.getenv("BLOG_RELEASE_PATH") == null) {
      throw new ProblemException(UpgradeProblems.DEPLOYMENT_REQUIRED);
    }
  }

  private static int retryAfter(@Nullable Integer value) {
    if (value == null) {
      return DEFAULT_RETRY_AFTER_SECONDS;
    }
    if (value < 1 || value > 86400) {
      throw new ProblemException(CommonProblems.INVALID_REQUEST);
    }
    return value;
  }

  static class MaintenanceRequest {
    public boolean enabled;
    public @Nullable String message;
    public @Nullable Integer retryAfter;
  }

  public record ReleaseMappingSettings(List<ReleaseMapping> mappings, List<String> roots,
          boolean ready, String message) {
  }

  public record ReleaseMappingSettingsRequest(List<ReleaseMapping> mappings) {
  }

  public record PackageView(Long id, String originalName, String displayName, String note,
          UpgradePackageStatus status, String version, long build, long fileSize, String sha256,
          int entryCount, String location, Instant createdAt, Instant consumedAt) {

    static PackageView from(UpgradePackageEntity entity) {
      return new PackageView(entity.getId(), entity.getOriginalName(), entity.getDisplayName(),
              entity.getNote(), entity.getStatus(), entity.getVersion(), entity.getBuild(),
              entity.getFileSize(), entity.getSha256(), entity.getEntryCount(),
              entity.getLocation(), entity.getCreatedAt(), entity.getConsumedAt());
    }
  }

  public record PackageUpdate(String displayName, String note) {
  }

  public record StartUpgradeRequest(UpgradeOptions options, boolean deletePackage) {
  }

  public record RecordView(Long id, Long packageId, String packageName, String packageVersion,
          long packageBuild, String packageSha256, String packagePath,
          String oldVersion, Long oldBuild, UpgradeStatus status,
          boolean backupEnabled, int backupTimeoutSeconds, int healthTimeoutSeconds,
          String note, boolean deletePackageAfterUpgrade,
          String releasePath, String backupPath, Long backupBytes, String backupSha256,
          Instant backupCompletedAt, String message, Instant createdAt, Instant startedAt,
          Instant finishedAt) {

    static RecordView from(UpgradeRecordEntity entity) {
      return new RecordView(entity.getId(), entity.getPackageId(), entity.getPackageName(),
              entity.getPackageVersion(), entity.getPackageBuild(), entity.getPackageSha256(),
              entity.getPackagePath(), entity.getOldVersion(), entity.getOldBuild(), entity.getStatus(),
              entity.isBackupEnabled(), entity.getBackupTimeoutSeconds(), entity.getHealthTimeoutSeconds(),
              entity.getNote(), entity.isDeletePackageAfterUpgrade(), entity.getReleasePath(),
              entity.getBackupPath(), entity.getBackupBytes(), entity.getBackupSha256(),
              entity.getBackupCompletedAt(), entity.getMessage(), entity.getCreatedAt(),
              entity.getStartedAt(), entity.getFinishedAt());
    }
  }

  public record CurrentRelease(String version, String build) {
  }

}
