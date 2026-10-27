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
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * 独立模块的一次性升级入口，不启动 Web 容器、数据库或应用上下文。
 */
public final class BlogUpdaterApplication {

  private BlogUpdaterApplication() {
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1)
      throw new IllegalArgumentException("需要一个升级任务 ID");
    UpgradeFiles.checkId(args[0]);
    Path root = Path.of(System.getenv().getOrDefault("BLOG_INSTALL_PATH", "/opt/today-blog"));
    Path state = Path.of(System.getenv().getOrDefault("BLOG_UPGRADE_STATE", "/var/lib/today-blog-upgrade"));
    String health = System.getenv().getOrDefault("BLOG_HEALTH_URL", "http://127.0.0.1:8080/api/internal/health");
    try {
      run(root, state, args[0], new SystemdControl(URI.create(health)));
    }
    catch (Exception error) {
      Path result = state.resolve("results").resolve(args[0] + ".properties");
      if (!Files.exists(result)) {
        UpgradeFiles.status(state.resolve("results"), args[0], "FAILED", error.toString());
      }
      throw error;
    }
  }

  public interface ServiceControl {
    void restart() throws Exception;

    boolean healthy(long build) throws Exception;
  }

  public static void run(Path root, Path state, String id, ServiceControl service) throws Exception {
    UpgradeFiles.checkId(id);
    Path results = state.resolve("results");
    Files.createDirectories(root.resolve("releases"));
    Files.createDirectories(results);
    try (var channel = FileChannel.open(root.resolve("upgrade.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            var lock = channel.tryLock()) {
      if (lock == null) {
        UpgradeFiles.status(results, id, "FAILED", "已有升级任务运行");
        return;
      }
      Path current = root.resolve("current");
      Path oldRelease = current.toRealPath();
      Properties oldMetadata = UpgradeFiles.read(oldRelease.resolve("release.properties"));
      long oldBuild = Long.parseLong(oldMetadata.getProperty("build"));
      Path task = Files.createTempDirectory(root.resolve("releases"), ".upgrade-");
      boolean switched = false;
      boolean rollbackFailed = false;
      try {
        UpgradeFiles.status(results, id, "VALIDATING", "校验升级包");
        // 先复制到仅 updater 可写的目录，之后校验和安装均使用同一份文件。
        Path archive = task.resolve("package.zip");
        Path upload = state.resolve("incoming").resolve(id).resolve("package.zip");
        if (!Files.isRegularFile(upload, java.nio.file.LinkOption.NOFOLLOW_LINKS)
                || Files.size(upload) > UpgradePackage.MAX_PACKAGE_BYTES)
          throw new IOException("升级包不存在或过大");
        try (var input = Files.newInputStream(upload); var output = Files.newOutputStream(archive)) {
          long copied = input.transferTo(new java.io.OutputStream() {
            long total;

            @Override
            public void write(int value) throws IOException { write(new byte[] { (byte) value }, 0, 1); }

            @Override
            public void write(byte[] bytes, int off, int len) throws IOException {
              total += len;
              if (total > UpgradePackage.MAX_PACKAGE_BYTES)
                throw new IOException("升级包过大");
              output.write(bytes, off, len);
            }
          });
          if (copied == 0)
            throw new IOException("升级包为空");
        }
        var manifest = UpgradePackage.verify(archive);
        Path highWaterPath = root.resolve("highest-build.properties");
        long highest = Files.exists(highWaterPath)
                ? Long.parseLong(UpgradeFiles.read(highWaterPath).getProperty("build")) : oldBuild;
        if (manifest.build() <= Math.max(oldBuild, highest))
          throw new IOException("禁止重复安装或降级");
        String releaseName = manifest.version() + "-" + manifest.build();
        Path release = root.resolve("releases").resolve(releaseName);
        if (Files.exists(release))
          throw new IOException("版本目录已存在");
        UpgradeFiles.status(results, id, "INSTALLING", "准备新版本");
        Path unpacked = Files.createDirectory(task.resolve("release"));
        UpgradePackage.extract(archive, unpacked, manifest);
        if (!unpacked.resolve("api/bin/blog-api").toFile().setExecutable(true, false))
          throw new IOException("无法设置启动权限");

        Properties metadata = new Properties();
        metadata.setProperty("version", manifest.version());
        metadata.setProperty("build", Long.toString(manifest.build()));
        UpgradeFiles.write(unpacked.resolve("release.properties"), metadata);
        Files.move(unpacked, release, StandardCopyOption.ATOMIC_MOVE);
        UpgradeFiles.status(results, id, "RESTARTING", "切换版本并重启 API");
        switchRelease(root, release);
        switched = true;
        service.restart();
        UpgradeFiles.status(results, id, "HEALTH_CHECKING", "等待新版本健康检查");
        if (!service.healthy(manifest.build()))
          throw new IOException("新版本健康检查失败");
        UpgradeFiles.write(highWaterPath, metadata);
        UpgradeFiles.status(results, id, "SUCCESS", "升级成功: " + manifest.version());
      }
      catch (Exception error) {
        if (switched) {
          try {
            UpgradeFiles.status(results, id, "ROLLING_BACK", "新版本失败，恢复旧版本");
            switchRelease(root, oldRelease);
            service.restart();
            if (!service.healthy(oldBuild))
              throw new IOException("旧版本健康检查失败");
            UpgradeFiles.status(results, id, "ROLLED_BACK", "已恢复旧版本；不撤销数据库迁移。原因: " + error.getMessage());
          }
          catch (Exception rollbackError) {
            rollbackFailed = true;
            error.addSuppressed(rollbackError);
            UpgradeFiles.status(results, id, "FAILED", "回滚失败，需要人工处理: " + rollbackError.getMessage());
          }
        }
        else
          UpgradeFiles.status(results, id, "FAILED", error.toString());
        if (!switched || rollbackFailed)
          throw error;
      }
      finally {
        try (var paths = Files.walk(task)) {
          for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList())
            Files.deleteIfExists(path);
        }
      }
    }
  }

  private static void switchRelease(Path root, Path target) throws IOException {
    Path link = root.resolve(".current-" + java.util.UUID.randomUUID());
    try {
      Files.createSymbolicLink(link, target.toAbsolutePath());
      Files.move(link, root.resolve("current"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
    finally { Files.deleteIfExists(link); }
  }

  private record SystemdControl(URI healthUrl) implements ServiceControl {

    @Override
    public void restart() throws Exception {
      Process process = new ProcessBuilder("/usr/bin/systemctl", "restart", "today-blog.service").inheritIO().start();
      if (!process.waitFor(60, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        throw new IOException("重启服务超时");
      }
      if (process.exitValue() != 0)
        throw new IOException("systemd 重启失败");
    }

    @Override
    public boolean healthy(long build) throws Exception {
      if (!"http".equals(healthUrl.getScheme()) || !LOOPBACK_HOSTS.contains(healthUrl.getHost())) {
        throw new IOException("健康检查必须使用本机 HTTP 地址");
      }
      HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
      long deadline = System.nanoTime() + Duration.ofSeconds(90).toNanos();
      while (System.nanoTime() < deadline) {
        try {
          var response = client.send(HttpRequest.newBuilder(healthUrl).timeout(Duration.ofSeconds(3)).GET().build(),
                  HttpResponse.BodyHandlers.ofString());
          if (response.statusCode() == 200 && response.body().trim().equals("UP:" + build))
            return true;
        }
        catch (IOException ignored) { }
        Thread.sleep(2000);
      }
      return false;
    }

    private static final java.util.Set<String> LOOPBACK_HOSTS = java.util.Set.of("127.0.0.1", "localhost", "[::1]");
  }
}
