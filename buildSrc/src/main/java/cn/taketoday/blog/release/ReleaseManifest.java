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

package cn.taketoday.blog.release;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * 为发行文件生成完整性清单，不验证发布者身份。
 */
public abstract class ReleaseManifest extends DefaultTask {

  @InputDirectory
  @PathSensitive(PathSensitivity.RELATIVE)
  public abstract DirectoryProperty getPayloadDirectory();

  @OutputFile
  public abstract RegularFileProperty getManifestFile();

  @Input
  public abstract Property<String> getProductVersion();

  @Input
  public abstract Property<String> getBuildNumber();

  @TaskAction
  public void generate() throws Exception {
    Path root = getPayloadDirectory().get().getAsFile().toPath();
    StringBuilder content = new StringBuilder("format=1\nproduct=today-blog\nversion=")
        .append(getProductVersion().get()).append("\nbuild=").append(getBuildNumber().get())
        .append("\nminimumJava=17\n");
    try (var paths = Files.walk(root)) {
      for (Path path : paths.sorted().toList()) {
        if (Files.isSymbolicLink(path)) {
          throw new GradleException("发行包不得包含符号链接: " + path);
        }
        if (!Files.isRegularFile(path)) {
          continue;
        }
        String name = root.relativize(path).toString().replace('\\', '/');
        if (!name.matches("[A-Za-z0-9_./@+,-]+") || name.contains("..") || name.contains("//")) {
          throw new GradleException("不支持的发行文件路径: " + name);
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(path)) {
          byte[] buffer = new byte[65536];
          int count;
          while ((count = input.read(buffer)) != -1) {
            digest.update(buffer, 0, count);
          }
        }
        content.append("sha256.").append(name).append('=')
            .append(HexFormat.of().formatHex(digest.digest())).append('\n');
      }
    }
    Path output = getManifestFile().get().getAsFile().toPath();
    Files.createDirectories(output.getParent());
    Files.writeString(output, content, StandardCharsets.US_ASCII);
  }
}
