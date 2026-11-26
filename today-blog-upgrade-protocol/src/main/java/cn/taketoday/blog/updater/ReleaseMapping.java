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

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * 一条发行文件映射：把发行目录中的前端静态文件暴露到反向代理使用的固定路径。
 * <p>
 * 映射只描述路径，不包含命令、凭据或任意可执行内容。实际写入由持有 root 权限的升级器
 * 根据 root 管理的白名单校验后完成。目标路径最终成为指向 {@code current/<source>} 的
 * 符号链接，因此切换和回滚版本时不需要重新发布映射。
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public record ReleaseMapping(String name, Mode mode, String source, String target) {

  /**
   * 映射发布方式。
   */
  public enum Mode {

    /**
     * 目标路径成为指向 {@code <install>/current/<source>} 的符号链接。
     */
    SYMLINK

  }

  private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9-]{0,31}");

  private static final Pattern SOURCE = Pattern.compile("[A-Za-z0-9_@+.,-]+(?:/[A-Za-z0-9_@+.,-]+)*");

  public ReleaseMapping {
    if (name == null || !NAME.matcher(name).matches()) {
      throw new IllegalArgumentException("映射名称只能由小写字母、数字和短横线组成");
    }
    if (mode == null) {
      throw new IllegalArgumentException("映射方式不能为空");
    }
    if (source == null || !SOURCE.matcher(source).matches() || source.contains("..")
            || !(source.equals("console") || source.equals("desktop")
            || source.startsWith("console/") || source.startsWith("desktop/"))) {
      throw new IllegalArgumentException("映射源必须是发行目录中的 console 或 desktop 前端文件");
    }
    if (target == null || target.isBlank()) {
      throw new IllegalArgumentException("映射目标不能为空");
    }
    Path path;
    try {
      path = Path.of(target);
    }
    catch (InvalidPathException invalid) {
      throw new IllegalArgumentException("映射目标不是合法路径");
    }
    if (!path.isAbsolute() || !path.normalize().toString().equals(target)
            || target.indexOf('\t') >= 0 || target.indexOf('\n') >= 0 || target.indexOf('\r') >= 0) {
      throw new IllegalArgumentException("映射目标必须是规范化的绝对路径");
    }
    if (path.getNameCount() < 2) {
      throw new IllegalArgumentException("映射目标不能是根目录");
    }
  }

  public Path targetPath() {
    return Path.of(target);
  }

  /**
   * 发布目标位于安装目录之外，以服务器文件系统根为基准存储，发布时仍须校验白名单。
   */
  public String storedTarget() {
    Path path = targetPath();
    return StoredPath.relativize(path.getRoot(), path);
  }

  public static ReleaseMapping fromStorage(String name, Mode mode, String source, String target) {
    // CLI 可能在 API 执行数据库迁移前读取任务快照。
    if (target.startsWith("/")) {
      return new ReleaseMapping(name, mode, source, target);
    }
    StoredPath.parse(target);
    return new ReleaseMapping(name, mode, source, "/" + target);
  }

  /**
   * 符号链接的最终指向。始终经过 {@code current}，版本切换和回滚会自动生效。
   */
  public Path linkTarget(Path installRoot) {
    return installRoot.toAbsolutePath().normalize().resolve("current").resolve(source);
  }

}
