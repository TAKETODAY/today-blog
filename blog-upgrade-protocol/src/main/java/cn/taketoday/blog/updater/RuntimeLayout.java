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

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运行时数据的默认目录布局。配置、发行版本和受信任的运维入口仍保留在各自的安全位置。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/22
 */
public final class RuntimeLayout {

  /**
   * 覆盖统一数据根目录的环境变量。
   */
  public static final String ROOT_ENVIRONMENT = "BLOG_DATA_ROOT";

  public static final Path DEFAULT_ROOT = Path.of("/var/lib/today-blog");

  private final Path root;

  public RuntimeLayout(Path root) {
    if (root == null || !root.isAbsolute()) {
      throw new IllegalArgumentException("运行时数据根目录必须是绝对路径");
    }
    this.root = root.normalize();
  }

  public static RuntimeLayout fromEnvironment() {
    return fromEnvironment(System.getenv());
  }

  public static RuntimeLayout fromEnvironment(Map<String, String> environment) {
    return fromEnvironment(environment, DEFAULT_ROOT);
  }

  /**
   * 读取环境变量中的数据根目录，未设置或为空白时使用调用方提供的默认目录。
   */
  public static RuntimeLayout fromEnvironment(Map<String, String> environment, Path defaultRoot) {
    String configured = environment.get(ROOT_ENVIRONMENT);
    return new RuntimeLayout(configured == null || configured.isBlank()
            ? defaultRoot : Path.of(configured.trim()));
  }

  public Path root() {
    return root;
  }

  /**
   * 业务数据、上传和附件。
   */
  public Path data() {
    return root.resolve("data");
  }

  /**
   * 数据库备份根目录，每个任务一个子目录。
   */
  public Path backups() {
    return root.resolve("backups").resolve("db");
  }

  /**
   * 升级状态根目录。
   */
  public Path upgrade() {
    return root.resolve("upgrade");
  }

  /**
   * 已上传的升级包。
   */
  public Path packages() {
    return upgrade().resolve("packages");
  }

  /**
   * API 日志目录。
   */
  public Path logs() {
    return root.resolve("logs");
  }

  /**
   * 上传和数据恢复等临时文件。
   */
  public Path tmp() {
    return root.resolve("tmp");
  }

  /**
   * multipart 上传超过内存阈值时使用的临时目录。
   */
  public Path multipart() {
    return tmp().resolve("multipart");
  }

  /**
   * 按显示顺序返回需要创建的目录。
   */
  public Map<Path, String> directories() {
    Map<Path, String> directories = new LinkedHashMap<>();
    directories.put(data(), "业务数据");
    directories.put(backups(), "数据库备份");
    directories.put(upgrade(), "升级状态");
    directories.put(packages(), "升级包");
    directories.put(logs(), "API 日志");
    directories.put(tmp(), "临时文件");
    directories.put(multipart(), "上传临时文件");
    return directories;
  }

  /**
   * 创建缺失的目录，已存在的目录保持不变。
   */
  public void ensureDirectories() throws java.io.IOException {
    for (Path directory : directories().keySet()) {
      java.nio.file.Files.createDirectories(directory);
    }
  }

  /**
   * 数据根目录下的相对路径，用于展示和导出归档。
   */
  public String relative(Path path) {
    Path normalized = path.toAbsolutePath().normalize();
    if (!normalized.startsWith(root)) {
      return normalized.toString();
    }
    return normalized.equals(root) ? "." : root.relativize(normalized).toString();
  }

}
