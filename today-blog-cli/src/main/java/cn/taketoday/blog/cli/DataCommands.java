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
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import cn.taketoday.blog.updater.DataPolicy;
import cn.taketoday.blog.updater.DatabaseBackup;
import cn.taketoday.blog.updater.DatabaseBackupFactory;
import cn.taketoday.blog.updater.DatabaseBackupResult;
import cn.taketoday.blog.updater.RuntimeLayout;
import cn.taketoday.blog.updater.UpgradeFiles;

/**
 * 统一管理运行时数据：查看、清理、备份、恢复、导出和迁移。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/22
 */
final class DataCommands {

  private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
          .withZone(ZoneOffset.UTC);

  private static final String USAGE = """
          用法: today-blog data <命令>
            status                                    显示数据根目录、占用和空间告警
            list backups|packages|logs                列出备份、升级包或日志
            check                                     校验目录所有权、权限和空间
            prune [--apply]                           按保留策略清理（默认只预览）
            backup [--timeout 600]                    立即导出数据库备份
            restore <database.sql.gz> --yes           从备份恢复数据库
            export [--output 文件] [--include-logs]   打包 data、备份和升级包
            policy [--backups-keep N] [--packages-keep N]
                   [--logs-days N] [--warn-used-percent N]
                   [--warn-free-bytes N]                查看或保存保留策略
          数据根目录由 BLOG_DATA_ROOT 指定，默认 /var/lib/today-blog。
          """;

  private final RuntimeLayout layout;

  private final UpgradeFiles files;

  private final DatabaseBackupFactory backups;

  private final Path policyPath;

  private final PrintStream out = System.out;

  DataCommands(RuntimeLayout layout, UpgradeFiles files, DatabaseBackupFactory backups, Path policyPath) {
    this.layout = layout;
    this.files = files;
    this.backups = backups;
    this.policyPath = policyPath;
  }

  void run(String[] args) throws Exception {
    if (args.length == 0 || (args.length == 1 && args[0].equals("--help"))) {
      out.print(USAGE);
      return;
    }
    Options options = Options.parse(args);
    switch (args[0]) {
      case "status" -> status();
      case "list" -> list(requireTarget(options));
      case "check" -> check();
      case "prune" -> prune(options);
      case "backup" -> backup(options);
      case "restore" -> restore(requireTarget(options), options);
      case "export" -> export(options);
      case "policy" -> policy(options);
      default -> throw new IllegalArgumentException(USAGE);
    }
  }

  private void status() throws IOException {
    out.println("数据根目录: " + layout.root());
    if (!Files.isDirectory(layout.root(), LinkOption.NOFOLLOW_LINKS)) {
      out.println("  目录尚未创建，请执行 install-server.sh。");
      return;
    }
    for (var entry : layout.directories().entrySet()) {
      Path directory = entry.getKey();
      if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
        out.println("  %-12s %s（未创建）".formatted(entry.getValue(), display(directory)));
        continue;
      }
      Usage usage = usage(directory);
      out.println("  %-12s %s  文件 %d 个，占用 %s".formatted(
              entry.getValue(), display(directory), usage.files(), human(usage.bytes())));
    }
    FileStore store = Files.getFileStore(layout.root());
    long total = store.getTotalSpace();
    long usable = store.getUsableSpace();
    long used = total - usable;
    long usedPercent = total <= 0 ? 0 : used * 100 / total;
    out.println("磁盘: 可用 %s / 总计 %s（已用 %d%%）"
            .formatted(human(usable), human(total), usedPercent));
    DataPolicy policy = loadPolicy();
    List<String> warnings = new ArrayList<>();
    if (usedPercent >= policy.warningUsedPercent()) {
      warnings.add("已用空间达到 %d%%，阈值 %d%%".formatted(usedPercent, policy.warningUsedPercent()));
    }
    if (usable < policy.warningFreeBytes()) {
      warnings.add("可用空间低于阈值 %s".formatted(human(policy.warningFreeBytes())));
    }
    if (warnings.isEmpty()) {
      out.println("告警: 无");
    }
    else {
      warnings.forEach(warning -> out.println("告警: " + warning));
    }
  }

  private void list(String target) throws IOException {
    switch (target) {
      case "backups" -> listBackups();
      case "packages" -> listPackages();
      case "logs" -> listLogs();
      default -> throw new IllegalArgumentException("未知的数据类型: " + target + "，可选 backups、packages、logs");
    }
  }

  private void listBackups() throws IOException {
    List<Path> entries = listDirectories(layout.backups());
    if (entries.isEmpty()) {
      out.println("没有数据库备份: " + layout.backups());
      return;
    }
    out.println("数据库备份: " + layout.backups());
    for (Path entry : entries) {
      Path archive = entry.resolve("database.sql.gz");
      out.println("  %s  %s  %s".formatted(entry.getFileName(),
              Files.isRegularFile(archive) ? human(Files.size(archive)) : "-", modified(entry)));
    }
  }

  private void listPackages() throws IOException {
    List<Path> entries = listDirectories(layout.packages());
    if (entries.isEmpty()) {
      out.println("没有升级包: " + layout.packages());
      return;
    }
    out.println("升级包: " + layout.packages());
    for (Path entry : entries) {
      Path archive = entry.resolve("package.zip");
      out.println("  %s  %s  %s".formatted(entry.getFileName(),
              Files.isRegularFile(archive) ? human(Files.size(archive)) : "-", modified(entry)));
    }
  }

  private void listLogs() throws IOException {
    if (!Files.isDirectory(layout.logs(), LinkOption.NOFOLLOW_LINKS)) {
      out.println("没有日志目录: " + layout.logs());
      return;
    }
    out.println("API 日志: " + layout.logs());
    try (var paths = Files.list(layout.logs())) {
      List<Path> entries = paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
              .sorted(Comparator.comparing(DataCommands::modified).reversed())
              .toList();
      if (entries.isEmpty()) {
        out.println("  （空）");
      }
      for (Path entry : entries) {
        out.println("  %s  %s  %s".formatted(entry.getFileName(), human(Files.size(entry)), modified(entry)));
      }
    }
  }

  private void check() throws IOException {
    List<String> failures = new ArrayList<>();
    if (!Files.isDirectory(layout.root(), LinkOption.NOFOLLOW_LINKS)) {
      failures.add("数据根目录不存在: " + layout.root());
      throw new IOException(String.join("；", failures));
    }
    requireOwned(layout.root(), "数据根目录", "root", failures);
    for (var entry : layout.directories().entrySet()) {
      Path directory = entry.getKey();
      if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
        out.println("[缺失] %s %s".formatted(entry.getValue(), display(directory)));
        continue;
      }
      String owner = directory.equals(layout.data()) || directory.equals(layout.packages())
              || directory.equals(layout.tmp()) ? "today" : "root";
      boolean safe = requireOwned(directory, entry.getValue(), owner, failures);
      out.println("[%s] %s %s".formatted(safe ? "正常" : "风险", entry.getValue(), display(directory)));
    }
    FileStore store = Files.getFileStore(layout.root());
    DataPolicy policy = loadPolicy();
    out.println("可用空间: %s，阈值 %s".formatted(human(store.getUsableSpace()),
            human(policy.warningFreeBytes())));
    if (!failures.isEmpty()) {
      throw new IOException(String.join("；", failures));
    }
    out.println("检查通过。");
  }

  private void prune(Options options) throws IOException {
    DataPolicy defaults = loadPolicy();
    DataPolicy policy = defaults
            .withBackupsKeep(options.intValue("backups-keep", defaults.backupsKeep()))
            .withPackagesKeep(options.intValue("packages-keep", defaults.packagesKeep()))
            .withLogsDays(options.intValue("logs-days", defaults.logsDays()));
    boolean apply = options.has("apply");
    out.println(apply ? "执行清理:" : "预览清理（加 --apply 才会删除）:");
    prunedCount = 0;
    pruneBackups(policy.backupsKeep(), apply);
    prunePackages(policy.packagesKeep(), apply);
    pruneLogs(policy.logsDays(), apply);
    if (prunedCount == 0) {
      out.println("  没有需要清理的数据。");
    }
  }

  private int prunedCount;

  private void pruneBackups(int keep, boolean apply) throws IOException {
    List<Path> deletions = obsolete(listDirectories(layout.backups()), keep, path ->
            Files.exists(path.resolve(".protected"), LinkOption.NOFOLLOW_LINKS));
    prune("数据库备份", deletions, apply);
  }

  private void prunePackages(int keep, boolean apply) throws IOException {
    List<Path> deletions = obsolete(listDirectories(layout.packages()), keep, path -> false);
    prune("升级包", deletions, apply);
  }

  private void pruneLogs(int days, boolean apply) throws IOException {
    if (!Files.isDirectory(layout.logs(), LinkOption.NOFOLLOW_LINKS)) {
      return;
    }
    Instant cutoff = Instant.now().minusSeconds(days * 86400L);
    try (var paths = Files.list(layout.logs())) {
      List<Path> deletions = paths
              .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
              .filter(path -> modified(path).isBefore(cutoff))
              .toList();
      prune("日志", deletions, apply);
    }
  }

  private void prune(String label, List<Path> deletions, boolean apply) throws IOException {
    for (Path path : deletions) {
      out.println("  %s 删除 %s".formatted(apply ? "已" : "将", layout.relative(path)));
      if (apply) {
        delete(path);
      }
      prunedCount++;
    }
  }

  private void backup(Options options) throws Exception {
    int timeout = options.intValue("timeout", 600);
    Files.createDirectories(layout.backups());
    String id = STAMP.format(Instant.now());
    DatabaseBackupResult result = backups.configured().create(id, timeout);
    out.println("备份完成: " + result.location());
    out.println("大小: " + human(result.bytes()) + "，SHA-256: " + result.sha256());
  }

  private void restore(String target, Options options) throws Exception {
    if (!options.has("yes")) {
      throw new IOException("恢复会覆盖数据库，请先停止 API，并加 --yes 确认");
    }
    Path archive = Path.of(target).toAbsolutePath().normalize();
    if (!Files.isRegularFile(archive, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(archive)) {
      throw new IOException("备份文件不存在或不是普通文件: " + archive);
    }
    Path config = Path.of(System.getenv().getOrDefault("BLOG_UPGRADE_CONFIG",
            DatabaseBackup.CONFIG.toString()));
    files.verifyTrusted(config, false);
    Properties values = files.read(config);
    String database = require(values, "database");
    Path client = Path.of(values.getProperty("clientExecutable", "/usr/bin/mysql"));
    Path credentials = Path.of(values.getProperty("defaultsFile", "/etc/today-blog/backup.cnf"));
    if (!Files.isExecutable(client)) {
      throw new IOException("数据库客户端不可执行: " + client);
    }
    if (!Files.isRegularFile(credentials, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("数据库凭据文件不存在: " + credentials);
    }
    Files.createDirectories(layout.tmp());
    long needed = Math.max(Files.size(archive) * 5, 64L * 1024 * 1024);
    if (Files.getFileStore(layout.tmp()).getUsableSpace() < needed) {
      throw new IOException("临时目录可用空间不足，恢复至少需要 " + human(needed));
    }
    Path sql = new java.io.File(layout.tmp().toFile(), "restore-" + UUID.randomUUID() + ".sql").toPath();
    try {
      out.println("解压备份到临时文件: " + sql);
      try (var input = new GZIPInputStream(Files.newInputStream(archive));
              OutputStream output = Files.newOutputStream(sql)) {
        input.transferTo(output);
      }
      setPrivate(sql);
      out.println("导入数据库 " + database + "，请勿中断。");
      Process process = new ProcessBuilder(client.toString(),
              "--defaults-file=" + credentials, "--database=" + database)
              .redirectInput(sql.toFile())
              .redirectOutput(ProcessBuilder.Redirect.INHERIT)
              .redirectError(ProcessBuilder.Redirect.INHERIT)
              .start();
      int exit = process.waitFor();
      if (exit != 0) {
        throw new IOException("数据库恢复失败，客户端退出码 " + exit);
      }
      out.println("数据库恢复完成: " + database);
    }
    finally {
      Files.deleteIfExists(sql);
    }
  }

  private void export(Options options) throws Exception {
    Path output = options.value("output") == null
            ? Path.of("today-blog-data-" + STAMP.format(Instant.now()) + ".zip").toAbsolutePath().normalize()
            : Path.of(options.value("output")).toAbsolutePath().normalize();
    if (output.startsWith(layout.root())) {
      throw new IOException("导出文件不能位于数据根目录内: " + output);
    }
    if (Files.exists(output, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("导出文件已存在: " + output);
    }
    List<Path> sources = new ArrayList<>();
    sources.add(layout.data());
    sources.add(layout.backups());
    sources.add(layout.packages());
    if (options.has("include-logs")) {
      sources.add(layout.logs());
    }
    long entries = 0;
    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(output))) {
      for (Path source : sources) {
        entries += zip(source, layout.root().relativize(source), zip);
      }
      Properties manifest = new Properties();
      manifest.setProperty("created", Instant.now().toString());
      manifest.setProperty("root", layout.root().toString());
      manifest.setProperty("entries", Long.toString(entries));
      ZipEntry entry = new ZipEntry("today-blog-data/META-INF/export.properties");
      zip.putNextEntry(entry);
      manifest.store(zip, "today-blog data export");
      zip.closeEntry();
    }
    out.println("导出完成: " + output);
    out.println("大小: " + human(Files.size(output)) + "，条目: " + entries);
  }

  private long zip(Path source, Path relative, ZipOutputStream zip) throws IOException {
    if (!Files.exists(source, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(source)) {
      return 0;
    }
    if (Files.isDirectory(source)) {
      long count = 0;
      ZipEntry entry = new ZipEntry("today-blog-data/" + relative + "/");
      zip.putNextEntry(entry);
      zip.closeEntry();
      try (var children = Files.list(source)) {
        List<Path> sorted = children.sorted().toList();
        for (Path child : sorted) {
          count += zip(child, relative.resolve(child.getFileName()), zip);
        }
      }
      return count;
    }
    if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
      return 0;
    }
    ZipEntry entry = new ZipEntry("today-blog-data/" + relative);
    entry.setLastModifiedTime(Files.getLastModifiedTime(source));
    zip.putNextEntry(entry);
    try (var input = Files.newInputStream(source)) {
      input.transferTo(zip);
    }
    zip.closeEntry();
    return 1;
  }

  private void policy(Options options) throws IOException {
    boolean changed = options.has("backups-keep")
            || options.has("packages-keep")
            || options.has("logs-days")
            || options.has("warn-used-percent")
            || options.has("warn-free-bytes");
    DataPolicy policy = loadPolicy();
    if (changed) {
      policy = policy
              .withBackupsKeep(options.intValue("backups-keep", policy.backupsKeep()))
              .withPackagesKeep(options.intValue("packages-keep", policy.packagesKeep()))
              .withLogsDays(options.intValue("logs-days", policy.logsDays()))
              .withWarningUsedPercent(options.intValue("warn-used-percent", policy.warningUsedPercent()))
              .withWarningFreeBytes(options.longValue("warn-free-bytes", policy.warningFreeBytes()));
      policy.store(policyPath);
      out.println("已保存保留策略: " + policyPath);
    }
    else {
      out.println("保留策略: " + policyPath
              + (Files.isRegularFile(policyPath) ? "" : "（文件不存在，显示默认值）"));
    }
    out.println("  备份保留数量: " + policy.backupsKeep());
    out.println("  升级包保留数量: " + policy.packagesKeep());
    out.println("  日志保留天数: " + policy.logsDays());
    out.println("  空间告警已用百分比: " + policy.warningUsedPercent() + "%");
    out.println("  空间告警可用字节: " + policy.warningFreeBytes() + " (" + human(policy.warningFreeBytes()) + ")");
  }

  private DataPolicy loadPolicy() throws IOException {
    return DataPolicy.load(policyPath);
  }

  private String requireTarget(Options options) {
    if (options.positional().isEmpty()) {
      throw new IllegalArgumentException(USAGE);
    }
    return options.positional().get(0);
  }

  private static String require(Properties values, String key) throws IOException {
    String value = values.getProperty(key);
    if (value == null || value.isBlank()) {
      throw new IOException("缺少备份配置项: " + key);
    }
    return value;
  }

  private List<Path> listDirectories(Path directory) throws IOException {
    if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
      return List.of();
    }
    try (var paths = Files.list(directory)) {
      return paths
              .filter(path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
              .filter(path -> !Files.isSymbolicLink(path))
              .sorted(Comparator.comparing(DataCommands::modified).reversed())
              .toList();
    }
  }

  private static List<Path> obsolete(List<Path> entries, int keep, java.util.function.Predicate<Path> protectedEntry) {
    List<Path> deletions = new ArrayList<>();
    int retained = 0;
    for (Path entry : entries) {
      if (protectedEntry.test(entry)) {
        retained++;
        continue;
      }
      if (retained++ >= keep) {
        deletions.add(entry);
      }
    }
    return deletions;
  }

  private static Instant modified(Path path) {
    try {
      return Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toInstant();
    }
    catch (IOException ignored) {
      return Instant.EPOCH;
    }
  }

  private Usage usage(Path directory) throws IOException {
    long files = 0;
    long bytes = 0;
    try (var paths = Files.walk(directory)) {
      for (Path path : paths.toList()) {
        if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
          files++;
          bytes += Files.size(path);
        }
      }
    }
    return new Usage(files, bytes);
  }

  private record Usage(long files, long bytes) {

  }

  private static String display(Path path) {
    return path.toString();
  }

  private static String human(long bytes) {
    if (bytes < 1024) {
      return bytes + " B";
    }
    double value = bytes;
    String[] units = { "KiB", "MiB", "GiB", "TiB" };
    int unit = -1;
    while (value >= 1024 && unit < units.length - 1) {
      value /= 1024;
      unit++;
    }
    return "%.1f %s".formatted(value, units[unit]);
  }

  private static void delete(Path path) throws IOException {
    if (Files.isSymbolicLink(path)) {
      Files.deleteIfExists(path);
      return;
    }
    if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
      try (var children = Files.list(path)) {
        for (Path child : children.toList()) {
          delete(child);
        }
      }
    }
    Files.deleteIfExists(path);
  }

  private static void setPrivate(Path path) {
    try {
      Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"));
    }
    catch (UnsupportedOperationException | IOException ignored) {
      // 非 POSIX 文件系统沿用默认权限。
    }
  }

  private static boolean requireOwned(Path path, String label, String owner, List<String> failures)
          throws IOException {
    boolean safe = true;
    try {
      if (!Files.getOwner(path).getName().equals(owner)) {
        failures.add(label + "必须由 " + owner + " 管理: " + path);
        safe = false;
      }
    }
    catch (UnsupportedOperationException ignored) {
      return safe;
    }
    try {
      Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS);
      if (permissions.contains(PosixFilePermission.OTHERS_WRITE)
              || ("root".equals(owner) && permissions.contains(PosixFilePermission.GROUP_WRITE))) {
        failures.add(label + "允许组或其他用户写入: " + path);
        safe = false;
      }
    }
    catch (UnsupportedOperationException ignored) {
      // 非 POSIX 文件系统不校验权限位。
    }
    return safe;
  }

  private static final class Options {

    private final List<String> arguments;

    private final Set<String> flags;

    private final java.util.Map<String, String> values;

    private Options(List<String> arguments, Set<String> flags, java.util.Map<String, String> values) {
      this.arguments = arguments;
      this.flags = flags;
      this.values = values;
    }

    static Options parse(String[] args) {
      List<String> positional = new ArrayList<>();
      Set<String> flags = new java.util.LinkedHashSet<>();
      java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
      for (int i = 1; i < args.length; i++) {
        String argument = args[i];
        if (argument.startsWith("--")) {
          String name = argument.substring(2);
          int equals = name.indexOf('=');
          if (equals >= 0) {
            values.put(name.substring(0, equals), name.substring(equals + 1));
          }
          else if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
            String next = args[++i];
            if (isKnownValue(name)) {
              values.put(name, next);
            }
            else {
              flags.add(name);
              positional.add(next);
            }
          }
          else if (isKnownValue(name)) {
            throw new IllegalArgumentException("参数 --" + name + " 缺少值");
          }
          else {
            flags.add(name);
          }
        }
        else {
          positional.add(argument);
        }
      }
      return new Options(List.copyOf(positional), Set.copyOf(flags), java.util.Map.copyOf(values));
    }

    private static boolean isKnownValue(String name) {
      return switch (name) {
        case "backups-keep", "packages-keep", "logs-days", "warn-used-percent",
             "warn-free-bytes", "timeout", "output" -> true;
        default -> false;
      };
    }

    boolean has(String name) {
      return flags.contains(name) || values.containsKey(name);
    }

    String value(String name) {
      return values.get(name);
    }

    int intValue(String name, int defaultValue) {
      String value = values.get(name);
      if (value == null) {
        return defaultValue;
      }
      try {
        return Integer.parseInt(value);
      }
      catch (NumberFormatException invalid) {
        throw new IllegalArgumentException("参数 --" + name + " 必须是整数");
      }
    }

    long longValue(String name, long defaultValue) {
      String value = values.get(name);
      if (value == null) {
        return defaultValue;
      }
      try {
        return Long.parseLong(value);
      }
      catch (NumberFormatException invalid) {
        throw new IllegalArgumentException("参数 --" + name + " 必须是整数");
      }
    }

    List<String> positional() {
      return arguments;
    }

  }

}
