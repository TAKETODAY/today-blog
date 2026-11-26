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

/**
 * 数据库文件路径使用显式基准目录下的相对路径，不依赖进程工作目录。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/24
 */
public final class StoredPath {

  private StoredPath() {
  }

  public static Path parse(String value) {
    Path path = Path.of(value);
    if (value.isBlank() || path.isAbsolute() || path.getRoot() != null || path.startsWith("..")
            || value.indexOf('\\') >= 0 || (value.length() > 1 && value.charAt(1) == ':')
            || !value.equals(path.normalize().toString().replace(path.getFileSystem().getSeparator(), "/"))) {
      throw new IllegalArgumentException("文件路径必须是规范化的相对路径");
    }
    return path;
  }

  public static String relativize(Path root, Path file) {
    if (!root.isAbsolute() || !file.isAbsolute() || !file.equals(file.normalize()) || !file.startsWith(root)) {
      throw new IllegalArgumentException("文件路径必须位于指定的根目录内");
    }
    String value = root.relativize(file).toString().replace(file.getFileSystem().getSeparator(), "/");
    parse(value);
    return value;
  }

}
