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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * 独立模块的一次性升级入口，不启动 Web 容器、数据库连接池或应用上下文。
 * <p>
 * 任务参数、状态和结果全部通过 JDBC 直接读写数据库；文件只保存升级包、备份和发行产物。
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public final class BlogUpdaterApplication {

  private BlogUpdaterApplication() {
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      throw new IllegalArgumentException("需要一个升级任务 ID");
    }
    UpgradeFiles.checkId(args[0]);
    long id = Long.parseLong(args[0]);
    Path root = Path.of(System.getenv().getOrDefault("BLOG_INSTALL_PATH", "/opt/today-blog"));
    String health = System.getenv().getOrDefault("BLOG_HEALTH_URL", "http://127.0.0.1:8080/api/internal/health");

    try (UpgradeTaskStore store = JdbcUpgradeTaskStore.open(UpgradeDatabase.configured())) {
      try {
        UpgradeTask task = store.load(id);
        run(root, task, new SystemdControl(URI.create(health), task.healthTimeoutSeconds()),
                store, (taskId, timeout) -> DatabaseBackup.configured().create(taskId, timeout));
      }
      catch (Exception error) {
        try {
          store.finished(id, "FAILED", error.toString());
        }
        catch (Exception ignored) {
        }
        throw error;
      }
    }
  }

  public interface ServiceControl {
    void restart() throws Exception;

    boolean healthy(long build) throws Exception;
  }

  @FunctionalInterface
  public interface BackupControl {
    DatabaseBackupResult create(String id, int timeoutSeconds) throws Exception;
  }

  public static void run(Path root, UpgradeTask task, ServiceControl service,
          UpgradeTaskStore store, BackupControl backup) throws Exception {
    run(root, task, service, store, backup, Path.of(System.getenv().getOrDefault(
            "BLOG_MAPPING_ROOTS", ReleaseMappings.POLICY.toString())));
  }

  public static void run(Path root, UpgradeTask task, ServiceControl service,
          UpgradeTaskStore store, BackupControl backup, Path mappingPolicy) throws Exception {
    long id = task.id();
    Path lockPath = root.resolve("upgrade.lock");
    Files.createDirectories(root.resolve("releases"));
    try (var channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            var lock = channel.tryLock()) {
      if (lock == null) {
        store.finished(id, "FAILED", "已有升级任务运行");
        return;
      }
      Path current = root.resolve("current");
      Path oldRelease = current.toRealPath();
      Properties oldMetadata = UpgradeFiles.read(oldRelease.resolve("release.properties"));
      long oldBuild = Long.parseLong(oldMetadata.getProperty("build"));
      Path taskDirectory = Files.createTempDirectory(root.resolve("releases"), ".upgrade-");
      boolean switched = false;
      boolean rollbackFailed = false;
      try {
        store.status(id, "VALIDATING", "校验升级包");
        // 先复制到仅 updater 可写的目录，之后校验和安装均使用同一份文件。
        Path archive = taskDirectory.resolve("package.zip");
        Path packageFile = Path.of(task.packagePath());
        if (!Files.isRegularFile(packageFile, LinkOption.NOFOLLOW_LINKS)
                || Files.size(packageFile) > UpgradePackage.MAX_PACKAGE_BYTES) {
          throw new IOException("升级包不存在或过大");
        }
        if (Files.getFileStore(root).getUsableSpace() < 3L * 1024 * 1024 * 1024) {
          throw new IOException("安装磁盘可用空间不足 3 GiB");
        }
        try (var input = Files.newInputStream(packageFile); var output = Files.newOutputStream(archive)) {
          long copied = input.transferTo(new java.io.OutputStream() {
            long total;

            @Override
            public void write(int value) throws IOException {
              write(new byte[] { (byte) value }, 0, 1);
            }

            @Override
            public void write(byte[] bytes, int off, int len) throws IOException {
              total += len;
              if (total > UpgradePackage.MAX_PACKAGE_BYTES) {
                throw new IOException("升级包过大");
              }
              output.write(bytes, off, len);
            }
          });
          if (copied == 0) {
            throw new IOException("升级包为空");
          }
        }
        var manifest = UpgradePackage.verify(archive);
        Path highWaterPath = root.resolve("highest-build.properties");
        long highest = Files.exists(highWaterPath)
                ? Long.parseLong(UpgradeFiles.read(highWaterPath).getProperty("build")) : oldBuild;
        if (manifest.build() <= Math.max(oldBuild, highest)) {
          throw new IOException("禁止重复安装或降级");
        }
        String releaseName = manifest.version() + "-" + manifest.build();
        Path release = root.resolve("releases").resolve(releaseName);
        if (Files.exists(release)) {
          throw new IOException("版本目录已存在");
        }
        store.status(id, "INSTALLING", "准备新版本");
        Path unpacked = Files.createDirectory(taskDirectory.resolve("release"));
        UpgradePackage.extract(archive, unpacked, manifest);
        if (!unpacked.resolve("api/bin/blog-api").toFile().setExecutable(true, false)) {
          throw new IOException("无法设置启动权限");
        }

        Properties metadata = new Properties();
        metadata.setProperty("version", manifest.version());
        metadata.setProperty("build", Long.toString(manifest.build()));
        UpgradeFiles.write(unpacked.resolve("release.properties"), metadata);
        if (task.backupEnabled()) {
          store.status(id, "BACKING_UP", "备份数据库，尚未切换版本");
          try {
            store.backup(id, backup.create(Long.toString(id), task.backupTimeoutSeconds()));
          }
          catch (Exception failure) {
            throw new IOException("数据库备份失败或超时，未切换版本；请检查服务器备份配置", failure);
          }
        }
        Files.move(unpacked, release, StandardCopyOption.ATOMIC_MOVE);
        for (ReleaseMapping mapping : task.mappings()) {
          if (!Files.exists(release.resolve(mapping.source()), LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("升级包缺少映射源: " + mapping.source());
          }
        }
        store.status(id, "RESTARTING", "切换版本并重启 API");
        switchRelease(root, release);
        switched = true;
        store.started(id, release.toString());
        if (!task.mappings().isEmpty()) {
          ReleaseMappings.publish(root, Path.of(System.getenv().getOrDefault(
                  "BLOG_UPGRADE_STATE", "/var/lib/today-blog-upgrade")), task.mappings(),
                  ReleaseMappings.roots(mappingPolicy));
        }
        service.restart();
        store.status(id, "HEALTH_CHECKING", "等待新版本健康检查");
        if (!service.healthy(manifest.build())) {
          throw new IOException("新版本健康检查失败");
        }
        UpgradeFiles.write(highWaterPath, metadata);
        store.finished(id, "SUCCESS", "升级成功: " + manifest.version());
        deleteConsumedPackage(task, store);
      }
      catch (Exception error) {
        if (switched) {
          try {
            store.status(id, "ROLLING_BACK", "新版本失败，恢复旧版本");
            switchRelease(root, oldRelease);
            service.restart();
            if (!service.healthy(oldBuild)) {
              throw new IOException("旧版本健康检查失败");
            }
            store.finished(id, "ROLLED_BACK", "已恢复旧版本；不撤销数据库迁移。原因: " + error.getMessage());
          }
          catch (Exception rollbackError) {
            rollbackFailed = true;
            error.addSuppressed(rollbackError);
            store.finished(id, "FAILED", "回滚失败，需要人工处理: " + rollbackError.getMessage());
          }
        }
        else {
          store.finished(id, "FAILED", error.toString());
        }
        if (!switched || rollbackFailed) {
          throw error;
        }
      }
      finally {
        try (var paths = Files.walk(taskDirectory)) {
          for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
            Files.deleteIfExists(path);
          }
        }
      }
    }
  }

  private static void deleteConsumedPackage(UpgradeTask task, UpgradeTaskStore store) {
    if (!task.deletePackageAfterUpgrade()) {
      return;
    }
    try {
      store.deletePackage(task.packageId());
      Path location = Path.of(task.packagePath());
      Files.deleteIfExists(location);
      Path parent = location.getParent();
      if (parent != null) {
        Files.deleteIfExists(parent);
      }
    }
    catch (Exception ignored) {
      // 升级已经成功，清理失败不能把结果改写成失败。
    }
  }

  private static void switchRelease(Path root, Path target) throws IOException {
    Path link = root.resolve(".current-" + java.util.UUID.randomUUID());
    try {
      Files.createSymbolicLink(link, target.toAbsolutePath());
      Files.move(link, root.resolve("current"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
    finally {
      Files.deleteIfExists(link);
    }
  }

  private record SystemdControl(URI healthUrl, int timeoutSeconds) implements ServiceControl {

    @Override
    public void restart() throws Exception {
      // 维护状态在 API 侧最多 1 秒生效，等待已进入处理的请求完成后再重启。
      Thread.sleep(2000);
      Process process = new ProcessBuilder("/usr/bin/systemctl", "restart", "today-blog.service").inheritIO().start();
      if (!process.waitFor(60, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        throw new IOException("重启服务超时");
      }
      if (process.exitValue() != 0) {
        throw new IOException("systemd 重启失败");
      }
    }

    @Override
    public boolean healthy(long build) throws Exception {
      if (!"http".equals(healthUrl.getScheme()) || !LOOPBACK_HOSTS.contains(healthUrl.getHost())) {
        throw new IOException("健康检查必须使用本机 HTTP 地址");
      }
      HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
      long deadline = System.nanoTime() + Duration.ofSeconds(timeoutSeconds).toNanos();
      while (System.nanoTime() < deadline) {
        try {
          var response = client.send(HttpRequest.newBuilder(healthUrl).timeout(Duration.ofSeconds(3)).GET().build(),
                  HttpResponse.BodyHandlers.ofString());
          if (response.statusCode() == 200 && response.body().trim().equals("UP:" + build)) {
            return true;
          }
        }
        catch (IOException ignored) {
        }
        Thread.sleep(2000);
      }
      return false;
    }

    private static final java.util.Set<String> LOOPBACK_HOSTS = java.util.Set.of("127.0.0.1", "localhost", "[::1]");
  }

}
