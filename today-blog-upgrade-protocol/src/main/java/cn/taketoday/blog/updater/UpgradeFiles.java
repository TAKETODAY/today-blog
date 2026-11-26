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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Properties;

/**
 * API 与独立升级模块之间的磁盘协议。
 */
public final class UpgradeFiles {

  public UpgradeFiles() {
  }

  public void checkId(String id) {
    if (!id.matches("[1-9][0-9]{0,18}")) {
      throw new IllegalArgumentException("升级任务 ID 无效");
    }
  }

  public Properties read(Path path) throws IOException {
    Properties properties = new Properties();
    // 按 UTF-8 解码：store() 生成的 Unicode 转义仍是 ASCII，不影响读取；
    // 手工写入的中文提示则不会变成乱码。
    try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }

  public void write(Path path, Properties properties) throws IOException {
    Files.createDirectories(path.getParent());
    Path temp = Files.createTempFile(path.getParent(), ".status-", ".tmp");
    try {
      try (var output = Files.newOutputStream(temp)) {
        properties.store(output, null);
      }
      temp.toFile().setReadable(true, false);
      Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
    finally {
      Files.deleteIfExists(temp);
    }
  }

  public void verifyTrusted(Path path, boolean secret) throws IOException {
    if (!path.isAbsolute() || !path.normalize().equals(path)
            || !Files.exists(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)
            || !Files.getOwner(path).getName().equals("root")) {
      throw new IOException("备份配置、工具及目录必须由 root 管理");
    }
    var permissions = Files.getPosixFilePermissions(path);
    var forbidden = PosixFilePermissions.fromString(secret ? "---rwxrwx" : "----w--w-");
    if (permissions.stream().anyMatch(forbidden::contains)) {
      throw new IOException("备份配置、工具或目录权限不安全");
    }
    Path parent = path.getParent();
    while (parent != null) {
      if (Files.isSymbolicLink(parent) || !Files.getOwner(parent).getName().equals("root")
              || Files.getPosixFilePermissions(parent).stream().anyMatch(
                      PosixFilePermissions.fromString("----w--w-")::contains)) {
        throw new IOException("备份路径的父目录必须由 root 管理且不可由其他用户写入");
      }
      parent = parent.getParent();
    }
  }

}
