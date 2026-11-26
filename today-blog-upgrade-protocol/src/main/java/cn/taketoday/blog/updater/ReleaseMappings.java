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
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 发行文件映射的类型化读写、白名单校验和符号链接发布。
 * <p>
 * 映射文件是制表符分隔的纯文本，不使用 {@code Properties}：字段固定、
 * 遇到未知字段直接失败，避免管理端和升级器对同一份配置产生不同解释。
 * <p>
 * 安全边界：管理端只能写入映射参数，真正修改固定目录的是以 root 运行的升级器。
 * 升级器要求目标位于 root 管理的白名单目录内，并逐级检查父目录所有权和权限，
 * 防止把目标链接到系统目录或借符号链接越界。
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public final class ReleaseMappings {

  /**
   * root 管理的映射目标白名单，每行一个绝对目录。
   */
  public static final Path POLICY = Path.of("/etc/today-blog/release-mapping-roots");

  public static final int MAX_MAPPINGS = 32;

  private static final String HEADER = "# today-blog release mappings v1";

  private static final String COLUMNS = "# name\tmode\tsource\ttarget";

  private final boolean rootUser;

  public ReleaseMappings() {
    this("root".equals(System.getProperty("user.name")));
  }

  ReleaseMappings(boolean rootUser) {
    this.rootUser = rootUser;
  }

  public List<ReleaseMapping> readIfExists(Path file) throws IOException {
    return Files.exists(file, LinkOption.NOFOLLOW_LINKS) ? read(file) : List.of();
  }

  public List<ReleaseMapping> read(Path file) throws IOException {
    if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("映射配置不是普通文件");
    }
    List<ReleaseMapping> mappings = new ArrayList<>();
    for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
      String line = raw.strip();
      if (line.isEmpty() || line.startsWith("#")) {
        continue;
      }
      if (mappings.size() >= MAX_MAPPINGS) {
        throw new IOException("映射数量超过上限 " + MAX_MAPPINGS);
      }
      String[] columns = line.split("\t", -1);
      if (columns.length != 4) {
        throw new IOException("映射配置格式错误：需要名称、方式、源、目标四个制表符分隔字段");
      }
      ReleaseMapping.Mode mode;
      try {
        mode = ReleaseMapping.Mode.valueOf(columns[1].toUpperCase(Locale.ROOT));
      }
      catch (IllegalArgumentException unsupported) {
        throw new IOException("不支持的映射方式: " + columns[1]);
      }
      try {
        mappings.add(new ReleaseMapping(columns[0], mode, columns[2], columns[3]));
      }
      catch (IllegalArgumentException invalid) {
        throw new IOException("映射配置无效: " + invalid.getMessage());
      }
    }
    validateStructure(mappings);
    return List.copyOf(mappings);
  }

  public void write(Path file, List<ReleaseMapping> mappings) throws IOException {
    validateStructure(mappings);
    StringBuilder content = new StringBuilder(HEADER).append('\n').append(COLUMNS).append('\n');
    for (ReleaseMapping mapping : mappings) {
      content.append(mapping.name()).append('\t')
              .append(mapping.mode().name().toLowerCase(Locale.ROOT)).append('\t')
              .append(mapping.source()).append('\t')
              .append(mapping.target()).append('\n');
    }
    Files.createDirectories(file.getParent());
    Path temp = Files.createTempFile(file.getParent(), ".mappings-", ".tmp");
    try {
      Files.writeString(temp, content, StandardCharsets.UTF_8);
      Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
    finally {
      Files.deleteIfExists(temp);
    }
  }

  /**
   * 读取 root 管理的目标目录白名单。管理端只读，升级器用于再次校验。
   */
  public List<Path> roots(Path policy) throws IOException {
    if (!Files.isRegularFile(policy, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(policy)) {
      throw new IOException("映射目标根目录配置未就绪");
    }
    Set<Path> roots = new HashSet<>();
    for (String raw : Files.readAllLines(policy, StandardCharsets.UTF_8)) {
      String line = raw.strip();
      if (line.isEmpty() || line.startsWith("#")) {
        continue;
      }
      Path root;
      try {
        root = Path.of(line);
      }
      catch (RuntimeException invalid) {
        throw new IOException("映射目标根目录不是合法路径");
      }
      if (!root.isAbsolute() || !root.normalize().toString().equals(line) || root.getNameCount() < 2) {
        throw new IOException("映射目标根目录必须是规范化的绝对目录");
      }
      roots.add(root);
    }
    if (roots.isEmpty()) {
      throw new IOException("映射目标根目录配置为空");
    }
    return List.copyOf(roots);
  }

  /**
   * 校验结构和管理端可见约束。升级器在发布前还会再次校验真实目录。
   */
  public void validate(List<ReleaseMapping> mappings, List<Path> roots) {
    validateStructure(mappings);
    for (ReleaseMapping mapping : mappings) {
      Path target = mapping.targetPath();
      boolean allowed = false;
      for (Path root : roots) {
        if (target.startsWith(root) && !target.equals(root)) {
          allowed = true;
          break;
        }
      }
      if (!allowed) {
        throw new IllegalArgumentException("映射目标不在允许的根目录下: " + target);
      }
    }
  }

  /**
   * 校验目标不与安装目录、升级状态目录重叠，避免向系统运行目录写入链接。
   */
  public void validateTargets(List<ReleaseMapping> mappings, Path installRoot, Path state) {
    Path root = installRoot.toAbsolutePath().normalize();
    Path statePath = state.toAbsolutePath().normalize();
    for (ReleaseMapping mapping : mappings) {
      Path target = mapping.targetPath();
      if (root.startsWith(target) || target.startsWith(root) || target.startsWith(statePath)) {
        throw new IllegalArgumentException("映射目标不能与安装目录或升级状态目录重叠: " + target);
      }
    }
  }

  /**
   * 发布映射：把目标路径替换为指向 {@code current/<source>} 的符号链接。
   * <p>
   * 已存在且指向同一位置的链接视为成功；已存在的空目录会先删除再建立链接；
   * 其他已存在内容一律拒绝，避免覆盖人工维护的文件。失败时删除本次新建的链接。
   */
  public void publish(Path installRoot, Path state,
          List<ReleaseMapping> mappings, List<Path> roots) throws IOException {
    if (mappings.isEmpty()) {
      return;
    }
    validate(mappings, roots);
    validateTargets(mappings, installRoot, state);
    Path root = installRoot.toAbsolutePath().normalize();
    List<Path> created = new ArrayList<>();
    List<Path> removed = new ArrayList<>();
    try {
      for (ReleaseMapping mapping : mappings) {
        Path target = mapping.targetPath();
        Path parent = target.getParent();
        Path realParent = parent.toRealPath();
        Path allowedRoot = matchingRoot(realParent, roots);
        if (allowedRoot == null) {
          throw new IOException("映射目标父目录不在允许的根目录下: " + target);
        }
        verifyTrustedDirectory(realParent, allowedRoot);

        Path desired = mapping.linkTarget(root);
        boolean existed = Files.exists(target, LinkOption.NOFOLLOW_LINKS);
        if (existed && Files.isSymbolicLink(target)) {
          if (Files.readSymbolicLink(target).equals(desired)) {
            continue;
          }
        }
        else if (existed) {
          if (!Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS) || !isEmpty(target)) {
            throw new IOException("映射目标已存在且不是本程序管理的链接: " + target);
          }
          Files.delete(target);
          removed.add(target);
          existed = false;
        }
        replaceLink(target, desired, mapping.name());
        if (!existed) {
          created.add(target);
        }
      }
    }
    catch (IOException | RuntimeException failure) {
      for (Path link : created) {
        deleteQuietly(link);
      }
      for (Path directory : removed) {
        try {
          Files.createDirectories(directory);
        }
        catch (IOException ignored) {
        }
      }
      throw failure;
    }
  }

  private void replaceLink(Path target, Path desired, String name) throws IOException {
    Path parent = target.getParent();
    Path temporary = parent.resolve(".today-blog-" + name + "-" + UUID.randomUUID());
    try {
      Files.createSymbolicLink(temporary, desired);
      Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
    finally {
      deleteQuietly(temporary);
    }
  }

  private Path matchingRoot(Path directory, List<Path> roots) throws IOException {
    for (Path root : roots) {
      if (Files.isDirectory(root) && directory.startsWith(root.toRealPath())) {
        return root.toRealPath();
      }
    }
    return null;
  }

  private void verifyTrustedDirectory(Path directory, Path allowedRoot) throws IOException {
    Path current = directory;
    while (current != null) {
      if (Files.isSymbolicLink(current) || !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
        throw new IOException("映射目标父目录必须是普通目录: " + current);
      }
      if (rootUser && !Files.getOwner(current).getName().equals("root")) {
        throw new IOException("映射目标父目录必须由 root 管理: " + current);
      }
      if (Files.getPosixFilePermissions(current).stream()
              .anyMatch(PosixFilePermissions.fromString("----w--w-")::contains)) {
        throw new IOException("映射目标父目录不能由组或其他用户写入: " + current);
      }
      if (current.equals(allowedRoot)) {
        return;
      }
      current = current.getParent();
    }
  }

  private boolean isEmpty(Path directory) throws IOException {
    try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
      return !entries.iterator().hasNext();
    }
  }

  private void deleteQuietly(Path path) {
    try {
      Files.deleteIfExists(path);
    }
    catch (IOException ignored) {
    }
  }

  public void validateStructure(List<ReleaseMapping> mappings) {
    if (mappings.size() > MAX_MAPPINGS) {
      throw new IllegalArgumentException("映射数量超过上限 " + MAX_MAPPINGS);
    }
    Set<String> names = new HashSet<>();
    Set<String> sources = new HashSet<>();
    Set<Path> targets = new HashSet<>();
    for (ReleaseMapping mapping : mappings) {
      if (!names.add(mapping.name())) {
        throw new IllegalArgumentException("映射名称重复: " + mapping.name());
      }
      if (!sources.add(mapping.source())) {
        throw new IllegalArgumentException("映射源重复: " + mapping.source());
      }
      Path target = mapping.targetPath();
      for (Path other : targets) {
        if (target.startsWith(other) || other.startsWith(target)) {
          throw new IllegalArgumentException("映射目标不能互相嵌套: " + target);
        }
      }
      targets.add(target);
    }
  }

}
