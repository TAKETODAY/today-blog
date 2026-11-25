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
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.Channels;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.BasicFileAttributeView;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import cn.taketoday.blog.updater.BackupType;
import cn.taketoday.blog.updater.UpgradeFiles;
import cn.taketoday.blog.updater.UpgradePackage;

/**
 * 一次停写窗口内采集网站状态，仅发布完整的加密归档。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/25
 */
final class SiteBackup {

  private static final Path DEV_NULL = Path.of("/dev/null");

  private final UpgradeFiles files;

  private final UpgradePackage packages;

  private final Path installRoot;

  private final Service service;

  private final Database database;

  private final Trust trust;

  SiteBackup(UpgradeFiles files, UpgradePackage packages, Path installRoot, Service service, Database database) {
    this(files, packages, installRoot, service, database, files::verifyTrusted);
  }

  SiteBackup(UpgradeFiles files, UpgradePackage packages, Path installRoot, Service service, Database database, Trust trust) {
    this.files = files;
    this.packages = packages;
    this.installRoot = installRoot;
    this.service = service;
    this.database = database;
    this.trust = trust;
  }

  Path create(Path config) throws Exception {
    return create(config, BackupType.recommended(), null);
  }

  Path create(Path config, Set<BackupType> types, Boolean uploadOffsite) throws Exception {
    BackupType.encode(types);
    trust.verify(config, true);
    Properties settings = files.read(config);
    Path output = trusted(settings, "output", true);
    if (!Files.isDirectory(output) || overlaps(output, installRoot)) {
      throw new IOException("整站备份输出目录无效");
    }
    Path stage = Files.createTempDirectory(output, ".site-work-", PosixFilePermissions.asFileAttribute(
            PosixFilePermissions.fromString("rwx------")));
    Path archive;
    Path offsite;
    try {
      offsite = !Boolean.FALSE.equals(uploadOffsite) && settings.containsKey("offsiteUploader")
              ? executable(settings, "offsiteUploader") : null;
      if (Boolean.TRUE.equals(uploadOffsite) && offsite == null) {
        throw new IOException("未配置异地上传器");
      }
      archive = create(settings, stage, types);
    }
    finally {
      try (var paths = Files.walk(stage)) {
        for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
          Files.delete(path);
        }
      }
    }
    System.out.println("加密备份已在本地生成: " + archive);
    System.out.println("备份范围: " + BackupType.encode(types));
    System.out.println("密文 SHA-256: " + hash(archive));
    if (offsite != null) {
      try {
        run(List.of(offsite.toString(), archive.toString()), "异地备份传输或校验失败", 7200);
      }
      catch (Exception failure) {
        throw new OffsiteUploadFailed(archive, failure);
      }
      System.out.println("异地备份上传器已确认副本；尚未执行恢复验证");
    }
    else {
      System.out.println("尚未确认异地副本");
    }
    return archive;
  }

  private Path create(Properties settings, Path stage, Set<BackupType> types) throws Exception {
    Path payload = Files.createDirectory(stage.resolve("site"));
    if (Files.getFileStore(stage).getUsableSpace() < 1024L * 1024 * 1024) {
      throw new IOException("整站备份暂存盘至少需要 1 GiB 可用空间");
    }
    Path age = executable(settings, "age");
    Path tar = executable(settings, "tar");
    String recipient = settings.getProperty("recipient", "").strip();
    if (!recipient.matches("age1[0-9a-z]{58}")) {
      throw new IOException("recipient 必须是 age X25519 公钥");
    }
    run(List.of(age.toString(), "-r", recipient, "-o", stage.resolve("age-check").toString(), DEV_NULL.toString()),
            "age 加密预检失败", 15);
    boolean stopRequired = !types.equals(Set.of(BackupType.DATABASE));
    if (stopRequired && !"true".equals(settings.getProperty("noExternalWriters"))) {
      throw new IOException("请确认没有数据库、本地网站文件的外部写入，再配置 noExternalWriters=true");
    }
    Properties metadata = new Properties();
    metadata.setProperty("format", "1");
    metadata.setProperty("types", BackupType.encode(types));
    metadata.setProperty("siteScope", types.containsAll(BackupType.recommended()) ? "full-local-site" : "partial");
    metadata.setProperty("oss", types.contains(BackupType.OSS) ? "export" : "none");
    metadata.setProperty("consistency", stopRequired ? "stopped" : "online-database");
    Context context = new Context(settings, stage, payload, metadata);
    var captures = new ArrayList<BackupProvider.Capture>();
    Path lockPath = installRoot.resolve("upgrade.lock");
    trust.verify(installRoot, false);
    try (var channel = FileChannel.open(lockPath, LinkOption.NOFOLLOW_LINKS, StandardOpenOption.CREATE,
            StandardOpenOption.WRITE); var lock = channel.tryLock()) {
      if (lock == null) {
        throw new IOException("已有升级任务、整站备份或 API 管理命令运行");
      }
      var current = files.read(installRoot.resolve("current/release.properties"));
      metadata.setProperty("version", current.getProperty("version"));
      metadata.setProperty("build", current.getProperty("build"));
      for (BackupProvider provider : providers()) {
        if (types.contains(provider.type())) {
          captures.add(provider.prepare(context));
        }
      }
      boolean running = stopRequired && service.running();
      Exception failure = null;
      try {
        if (running) {
          service.stop();
        }
        if (stopRequired && service.running()) {
          throw new IOException("未确认 API 已停止，不生成恢复点");
        }
        metadata.setProperty("capturedAt", Instant.now().toString());
        for (BackupProvider.Capture capture : captures) {
          capture.backup();
        }
      }
      catch (Exception error) {
        failure = error;
        throw error;
      }
      finally {
        if (running) {
          try {
            service.restartAndWait(90);
          }
          catch (Exception restartError) {
            var error = new IOException("备份后 API 未恢复，请立即检查服务和健康接口", restartError);
            if (failure == null) {
              throw error;
            }
            failure.addSuppressed(error);
          }
        }
      }
    }
    catch (OverlappingFileLockException locked) {
      throw new IOException("已有升级任务、整站备份或 API 管理命令运行", locked);
    }

    try (var output = Files.newOutputStream(payload.resolve("manifest.properties"))) {
      metadata.store(output, null);
    }
    writeChecksums(payload);
    Path encrypted = Files.createFile(stage.resolve("backup.age"), PosixFilePermissions.asFileAttribute(
            PosixFilePermissions.fromString("rw-------")));
    List<Process> pipeline = ProcessBuilder.startPipeline(List.of(
            command(List.of(tar.toString(), "-czf", "-", "-C", stage.toString(), "site")),
            command(List.of(age.toString(), "-r", recipient)).redirectInput(ProcessBuilder.Redirect.PIPE)
                    .redirectOutput(encrypted.toFile())));
    try {
      for (Process process : pipeline) {
        if (!process.waitFor(3600, TimeUnit.SECONDS) || process.exitValue() != 0) {
          throw new IOException("整站归档压缩或加密失败");
        }
      }
    }
    finally {
      pipeline.forEach(SiteBackup::terminate);
    }
    if (Files.size(encrypted) == 0) {
      throw new IOException("整站归档为空");
    }
    try (var data = FileChannel.open(encrypted, StandardOpenOption.WRITE)) {
      data.force(true);
    }
    String name = metadata.getProperty("capturedAt").replaceAll("[^0-9TZ]", "") + "-" + UUID.randomUUID();
    return Files.move(encrypted, stage.getParent().resolve("site-" + name + ".tar.gz.age"), StandardCopyOption.ATOMIC_MOVE);
  }

  private List<BackupProvider> providers() {
    return List.of(new DatabaseBackupProvider(), new AttachmentBackupProvider(),
            new WebsiteBackupProvider(), new ConfigurationBackupProvider(),
            new ReleaseBackupProvider(), new OssBackupProvider());
  }

  record Context(Properties settings, Path stage, Path payload, Properties metadata) {
  }

  private final class DatabaseBackupProvider implements BackupProvider {
    @Override
    public BackupType type() {
      return BackupType.DATABASE;
    }

    @Override
    public Capture prepare(Context context) throws Exception {
      Path dbStage = Files.createDirectory(context.stage().resolve("db"), PosixFilePermissions.asFileAttribute(
              PosixFilePermissions.fromString("rwx------")));
      Database.Dump dump = database.prepare(dbStage);
      return () -> {
        Path destination = context.payload().resolve("database.sql.gz");
        dump.write(destination);
        try (var sql = new GZIPInputStream(Files.newInputStream(destination))) {
          if (sql.transferTo(java.io.OutputStream.nullOutputStream()) == 0) {
            throw new IOException("数据库备份为空");
          }
        }
      };
    }
  }

  private abstract class FileBackupProvider implements BackupProvider {
    abstract List<String> sources(Properties settings) throws IOException;

    @Override
    public Capture prepare(Context context) throws IOException {
      var keys = sources(context.settings());
      for (String key : keys) {
        Path source = path(context.settings(), key);
        if (!key.substring(7).matches("[a-z0-9][a-z0-9_-]*") || overlaps(source, context.stage().getParent())
                || overlaps(source, installRoot) || !source.toRealPath().equals(source)) {
          throw new IOException("网站文件来源无效: " + key);
        }
      }
      return () -> {
        Path website = Files.createDirectories(context.payload().resolve("files"));
        for (String key : keys) {
          Path source = path(context.settings(), key);
          copySource(source, website.resolve(key.substring(7)));
          context.metadata().setProperty(key, source.toString());
        }
      };
    }
  }

  private final class AttachmentBackupProvider extends FileBackupProvider {
    @Override
    public BackupType type() {
      return BackupType.ATTACHMENTS;
    }

    @Override
    List<String> sources(Properties settings) throws IOException {
      if (!Files.isDirectory(path(settings, "source.upload"), LinkOption.NOFOLLOW_LINKS)) {
        throw new IOException("请配置实际生效的本地 upload 目录");
      }
      return List.of("source.upload");
    }
  }

  private final class WebsiteBackupProvider extends FileBackupProvider {
    @Override
    public BackupType type() {
      return BackupType.WEBSITE;
    }

    @Override
    List<String> sources(Properties settings) {
      return settings.stringPropertyNames().stream().filter(key -> key.equals("source.custom-static")
              || key.startsWith("source.website-")).sorted().toList();
    }
  }

  private final class ConfigurationBackupProvider extends FileBackupProvider {
    @Override
    public BackupType type() {
      return BackupType.CONFIGURATION;
    }

    @Override
    List<String> sources(Properties settings) throws IOException {
      if (!settings.containsKey("source.app-config") || !settings.containsKey("source.proxy-config")) {
        throw new IOException("必须配置 source.app-config 和 source.proxy-config");
      }
      return settings.stringPropertyNames().stream().filter(key -> key.startsWith("source.")
              && !key.equals("source.upload") && !key.equals("source.custom-static")
              && !key.startsWith("source.website-")).sorted().toList();
    }
  }

  private final class ReleaseBackupProvider implements BackupProvider {
    @Override
    public BackupType type() {
      return BackupType.RELEASE;
    }

    @Override
    public Capture prepare(Context context) throws Exception {
      Path artifacts = Files.createDirectory(context.payload().resolve("artifacts"));
      copy(trusted(context.settings(), "releaseZip", false), artifacts.resolve("release.zip"));
      copy(trusted(context.settings(), "cliJar", false), artifacts.resolve("cli.jar"));
      copy(trusted(context.settings(), "installer", false), artifacts.resolve("install-server.sh"));
      checkRelease(packages.verify(artifacts.resolve("release.zip")));
      return () -> {
      };
    }
  }

  private final class OssBackupProvider implements BackupProvider {
    @Override
    public BackupType type() {
      return BackupType.OSS;
    }

    @Override
    public Capture prepare(Context context) throws IOException {
      Path exporter = executable(context.settings(), "ossExporter");
      return () -> {
        Path objects = Files.createDirectory(context.payload().resolve("oss"));
        run(List.of(exporter.toString(), objects.toString()), "OSS 对象导出失败", 1800);
        Path inventory = objects.resolve("inventory.json");
        if (!Files.isRegularFile(inventory, LinkOption.NOFOLLOW_LINKS) || Files.size(inventory) == 0) {
          throw new IOException("OSS 导出未生成对象清单");
        }
      };
    }
  }

  private void checkRelease(UpgradePackage.Manifest manifest) throws IOException {
    Path release = installRoot.resolve("current").toRealPath();
    if (!installRoot.resolve("releases").toRealPath().equals(release.getParent())) {
      throw new IOException("current 未指向受管理的发行目录");
    }
    var current = files.read(release.resolve("release.properties"));
    if (!manifest.version().equals(current.getProperty("version"))
            || !Long.toString(manifest.build()).equals(current.getProperty("build"))) {
      throw new IOException("备份用发行 ZIP 与当前运行版本不匹配");
    }
    for (var file : manifest.hashes().entrySet()) {
      Path installed = release.resolve(file.getKey());
      if (!Files.isRegularFile(installed, LinkOption.NOFOLLOW_LINKS)
              || !installed.toRealPath().equals(installed) || !hash(installed).equals(file.getValue())) {
        throw new IOException("当前发行文件与备份用发行 ZIP 不匹配: " + file.getKey());
      }
    }
    // 禁止遗漏额外安装文件；自定义内容应独立放置并作为网站文件采集。
    try (var paths = Files.walk(release)) {
      for (Path installed : paths.toList()) {
        if (Files.isDirectory(installed, LinkOption.NOFOLLOW_LINKS)) {
          continue;
        }
        String name = release.relativize(installed).toString();
        if (!name.equals("release.properties") && !manifest.hashes().containsKey(name)) {
          throw new IOException("当前发行目录存在未列入 ZIP 的文件: " + name);
        }
      }
    }
  }

  private Path trusted(Properties props, String key, boolean privatePath) throws IOException {
    Path path = path(props, key);
    trust.verify(path, privatePath);
    return path;
  }

  private Path executable(Properties props, String key) throws IOException {
    Path path = path(props, key).toRealPath();
    trust.verify(path, false);
    if (!Files.isRegularFile(path) || !Files.isExecutable(path)) {
      throw new IOException("备份命令不可执行: " + key);
    }
    return path;
  }

  private static Path path(Properties props, String key) throws IOException {
    Path path = Path.of(props.getProperty(key, ""));
    if (!path.isAbsolute() || !path.normalize().equals(path)) {
      throw new IOException("请配置规范的绝对路径: " + key);
    }
    return path;
  }

  private static boolean overlaps(Path first, Path second) {
    return first.startsWith(second) || second.startsWith(first);
  }

  private static void copy(Path source, Path destination) throws IOException {
    if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("备份工件不是普通文件: " + source);
    }
    try (InputStream input = Files.newInputStream(source, LinkOption.NOFOLLOW_LINKS)) {
      Files.copy(input, destination);
    }
  }

  private static void copySource(Path source, Path destination) throws IOException {
    try (var root = Files.newDirectoryStream(source.getRoot())) {
      if (!(root instanceof SecureDirectoryStream<Path> directory)) {
        throw new IOException("网站文件来源不支持安全目录操作");
      }
      copySource(directory, source, 0, destination);
    }
  }

  private static void copySource(SecureDirectoryStream<Path> directory, Path source, int index, Path destination)
          throws IOException {
    Path name = source.getName(index);
    if (index == source.getNameCount() - 1) {
      copyEntry(directory, name, destination);
    }
    else {
      try (var child = directory.newDirectoryStream(name, LinkOption.NOFOLLOW_LINKS)) {
        copySource(child, source, index + 1, destination);
      }
    }
  }

  private static void copyEntry(SecureDirectoryStream<Path> directory, Path name, Path destination) throws IOException {
    var attributes = directory.getFileAttributeView(name, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS)
            .readAttributes();
    if (attributes.isDirectory()) {
      Files.createDirectory(destination);
      try (var entries = directory.newDirectoryStream(name, LinkOption.NOFOLLOW_LINKS)) {
        for (Path entry : entries) {
          copyEntry(entries, entry.getFileName(), destination.resolve(entry.getFileName()));
        }
      }
    }
    else if (attributes.isRegularFile()) {
      try (InputStream input = Channels.newInputStream(directory.newByteChannel(name,
              Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)))) {
        Files.copy(input, destination);
      }
    }
    else {
      throw new IOException("备份来源包含符号链接或特殊文件: " + name);
    }
  }

  private static String hash(Path path) throws IOException {
    try (var input = new DigestInputStream(Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS),
            MessageDigest.getInstance("SHA-256"))) {
      input.transferTo(java.io.OutputStream.nullOutputStream());
      return HexFormat.of().formatHex(input.getMessageDigest().digest());
    }
    catch (java.security.NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  private static void writeChecksums(Path payload) throws IOException {
    try (var paths = Files.walk(payload); var list = Files.newBufferedWriter(payload.resolve("SHA256SUMS"))) {
      for (Path file : paths.sorted().toList()) {
        if (file.equals(payload.resolve("SHA256SUMS")) || Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS)) {
          continue;
        }
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
          throw new IOException("归档中存在非普通文件");
        }
        String name = payload.relativize(file).toString();
        String escaped = name.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
        list.write((escaped.equals(name) ? "" : "\\") + hash(file) + "  " + escaped + "\n");
      }
    }
  }

  private static ProcessBuilder command(List<String> args) {
    ProcessBuilder builder = new ProcessBuilder(args).redirectInput(DEV_NULL.toFile())
            .redirectError(ProcessBuilder.Redirect.DISCARD);
    builder.environment().remove("TAR_OPTIONS");
    builder.environment().remove("GZIP");
    return builder;
  }

  private static void run(List<String> args, String message, int seconds) throws Exception {
    Process process = command(args).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
    try {
      if (!process.waitFor(seconds, TimeUnit.SECONDS) || process.exitValue() != 0) {
        throw new IOException(message);
      }
    }
    finally {
      terminate(process);
    }
  }

  private static void terminate(Process process) {
    if (process.isAlive()) {
      process.descendants().forEach(ProcessHandle::destroyForcibly);
      process.destroyForcibly();
    }
  }

  interface Service {
    boolean running() throws Exception;

    void stop() throws Exception;

    void restartAndWait(int timeoutSeconds) throws Exception;
  }

  static final class OffsiteUploadFailed extends IOException {
    private final Path archive;

    OffsiteUploadFailed(Path archive, Exception cause) {
      super("异地备份传输或校验失败；本地加密归档已保留", cause);
      this.archive = archive;
    }

    Path archive() {
      return archive;
    }
  }

  interface Database {
    Dump prepare(Path staging) throws Exception;

    interface Dump {
      void write(Path destination) throws Exception;
    }
  }

  interface Trust {
    void verify(Path path, boolean privatePath) throws IOException;
  }
}
