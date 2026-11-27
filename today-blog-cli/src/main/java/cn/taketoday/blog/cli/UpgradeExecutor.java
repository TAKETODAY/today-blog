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
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.util.Comparator;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.blog.updater.DatabaseBackupResult;
import cn.taketoday.blog.updater.ReleaseMapping;
import cn.taketoday.blog.updater.ReleaseMappings;
import cn.taketoday.blog.updater.RuntimeLayout;
import cn.taketoday.blog.updater.StoredPath;
import cn.taketoday.blog.updater.UpgradeFiles;
import cn.taketoday.blog.updater.UpgradePackage;

/**
 * 执行一次完整的发行版本升级。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/20
 */
public final class UpgradeExecutor {

  private final Path root;

  private final RuntimeLayout layout;

  private final Path mappingPolicy;

  private final UpgradeFiles files;

  private final UpgradePackage packages;

  private final ReleaseMappings mappings;

  private final ServiceControl service;

  private final UpgradeTaskStore store;

  private final BackupControl backup;

  public UpgradeExecutor(Path root, RuntimeLayout layout, Path mappingPolicy, UpgradeFiles files,
          UpgradePackage packages, ReleaseMappings mappings, ServiceControl service,
          UpgradeTaskStore store, BackupControl backup) {
    this.root = root.toAbsolutePath().normalize();
    this.layout = layout;
    this.mappingPolicy = mappingPolicy.toAbsolutePath().normalize();
    this.files = files;
    this.packages = packages;
    this.mappings = mappings;
    this.service = service;
    this.store = store;
    this.backup = backup;
  }

  public boolean execute(UpgradeTask task) throws Exception {
    long id = task.id();
    Path lockPath = root.resolve("upgrade.lock");
    Files.createDirectories(root.resolve("releases"));
    try (var channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            var lock = channel.tryLock()) {
      if (lock == null) {
        throw new IOException("已有升级任务或 API 管理命令运行");
      }
      if (!store.begin(id)) {
        throw new IOException("升级任务不处于待启动状态");
      }
      Path oldRelease = null;
      long oldBuild = 0;
      Path taskDirectory = null;
      boolean switched = false;
      boolean rollbackFailed = false;
      try {
        oldRelease = root.resolve("current").toRealPath();
        if (!root.resolve("releases").toRealPath().equals(oldRelease.getParent())) {
          throw new IOException("current 未指向受管理的发行目录");
        }
        oldBuild = Long.parseLong(files.read(oldRelease.resolve("release.properties")).getProperty("build"));
        taskDirectory = Files.createTempDirectory(root.resolve("releases"), ".upgrade-");
        Path archive = taskDirectory.resolve("package.zip");
        if (Files.getFileStore(root).getUsableSpace() < 3L * 1024 * 1024 * 1024) {
          throw new IOException("安装磁盘可用空间不足 3 GiB");
        }
        copyPackage(packageDirectory(task), archive);
        UpgradePackage.Manifest manifest = packages.verify(archive);
        if (manifest.build() != task.build() || !manifest.version().equals(task.version())) {
          throw new IOException("升级包版本与任务记录不一致");
        }
        Path highWaterPath = root.resolve("highest-build.properties");
        long highest = Files.exists(highWaterPath)
                ? Long.parseLong(files.read(highWaterPath).getProperty("build")) : oldBuild;
        if (manifest.build() <= Math.max(oldBuild, highest)) {
          throw new IOException("禁止重复安装或降级");
        }
        String oldFrontendMode = files.read(oldRelease.resolve("release.properties"))
                .getProperty("frontendMode", "nginx");
        if (!oldFrontendMode.equals(manifest.frontendMode())) {
          throw new IOException("不允许在普通升级中切换前端发行模式");
        }
        Path release = root.resolve("releases").resolve(manifest.version() + "-" + manifest.build());
        if (Files.exists(release)) {
          throw new IOException("版本目录已存在");
        }
        store.status(id, "INSTALLING", "准备新版本");
        Path unpacked = Files.createDirectory(taskDirectory.resolve("release"));
        packages.extract(archive, unpacked, manifest);
        if (!unpacked.resolve("api/bin/blog-api").toFile().setExecutable(true, false)) {
          throw new IOException("无法设置启动权限");
        }
        Properties metadata = new Properties();
        metadata.setProperty("version", manifest.version());
        metadata.setProperty("build", Long.toString(manifest.build()));
        metadata.setProperty("frontendMode", manifest.frontendMode());
        files.write(unpacked.resolve("release.properties"), metadata);
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
        switchRelease(release);
        switched = true;
        store.started(id, StoredPath.relativize(root, release));
        if (!task.mappings().isEmpty()) {
          mappings.publish(root, layout.upgrade(), task.mappings(), mappings.roots(mappingPolicy));
        }
        service.restart();
        store.status(id, "HEALTH_CHECKING", "等待新版本健康检查");
        if (!service.healthy(manifest.build(), task.healthTimeoutSeconds())) {
          throw new IOException("新版本健康检查失败");
        }
        files.write(highWaterPath, metadata);
        store.finished(id, "SUCCESS", "升级成功: " + manifest.version());
        deleteConsumedPackage(task);
        return true;
      }
      catch (Exception error) {
        if (switched) {
          try {
            try {
              store.status(id, "ROLLING_BACK", "新版本失败，恢复旧版本");
            }
            catch (Exception statusError) {
              error.addSuppressed(statusError);
            }
            switchRelease(oldRelease);
            service.restart();
            if (!service.healthy(oldBuild, task.healthTimeoutSeconds())) {
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
        return false;
      }
      finally {
        if (taskDirectory != null) {
          deleteDirectory(taskDirectory);
        }
      }
    }
  }

  private Path packageDirectory(UpgradeTask task) throws IOException {
    try {
      String location = task.packagePath();
      Path path = Path.of(location);
      if (path.isAbsolute()) {
        // CLI 先于 API 更新时，启动新版本前 Flyway 尚未迁移已有任务。
        location = StoredPath.relativize(layout.packages(), path);
      }
      return layout.resolvePackage(location).getParent().getFileName();
    }
    catch (IllegalArgumentException invalid) {
      throw new IOException("升级包必须位于上传目录内", invalid);
    }
  }

  private SecureDirectoryStream<Path> openPackageRoot() throws IOException {
    var directory = Files.newDirectoryStream(layout.packages());
    if (directory instanceof SecureDirectoryStream<Path> secure) {
      return secure;
    }
    directory.close();
    throw new IOException("升级包目录不支持安全文件操作");
  }

  private void copyPackage(Path name, Path target) throws IOException {
    try (var directory = openPackageRoot();
            var source = directory.newDirectoryStream(name, LinkOption.NOFOLLOW_LINKS)) {
      Path file = Path.of("package.zip");
      var attributes = source.getFileAttributeView(file, BasicFileAttributeView.class,
              LinkOption.NOFOLLOW_LINKS).readAttributes();
      if (!attributes.isRegularFile() || attributes.size() > UpgradePackage.MAX_PACKAGE_BYTES) {
        throw new IOException("升级包不存在或过大");
      }
      try (var input = Channels.newInputStream(source.newByteChannel(file,
              Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)));
              var output = Files.newOutputStream(target)) {
        if (input.transferTo(new LimitedOutputStream(output, UpgradePackage.MAX_PACKAGE_BYTES)) == 0) {
          throw new IOException("升级包为空");
        }
      }
    }
  }

  private void deleteConsumedPackage(UpgradeTask task) {
    if (!task.deletePackageAfterUpgrade()) {
      return;
    }
    try (var directory = openPackageRoot()) {
      Path name = packageDirectory(task);
      store.deletePackage(task.packageId());
      try (var source = directory.newDirectoryStream(name, LinkOption.NOFOLLOW_LINKS)) {
        source.deleteFile(Path.of("package.zip"));
      }
      directory.deleteDirectory(name);
    }
    catch (Exception ignored) {
      // 升级已经成功，清理失败不能把结果改写成失败。
    }
  }

  private void switchRelease(Path target) throws IOException {
    Path link = root.resolve(".current-" + java.util.UUID.randomUUID());
    try {
      Files.createSymbolicLink(link, target.toAbsolutePath());
      Files.move(link, root.resolve("current"), StandardCopyOption.ATOMIC_MOVE,
              StandardCopyOption.REPLACE_EXISTING);
    }
    finally {
      Files.deleteIfExists(link);
    }
  }

  private void deleteDirectory(Path directory) {
    try (var paths = Files.walk(directory)) {
      for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
        Files.deleteIfExists(path);
      }
    }
    catch (Exception ignored) {
    }
  }

  private static final class LimitedOutputStream extends java.io.FilterOutputStream {

    private final long limit;

    private long total;

    private LimitedOutputStream(java.io.OutputStream output, long limit) {
      super(output);
      this.limit = limit;
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
      total += length;
      if (total > limit) {
        throw new IOException("升级包过大");
      }
      out.write(bytes, offset, length);
    }

    @Override
    public void write(int value) throws IOException {
      write(new byte[] { (byte) value }, 0, 1);
    }
  }

  @FunctionalInterface
  public interface BackupControl {

    DatabaseBackupResult create(String id, int timeoutSeconds) throws Exception;
  }

  public interface ServiceControl {

    void restart() throws Exception;

    boolean healthy(long build, int timeoutSeconds) throws Exception;
  }

}
