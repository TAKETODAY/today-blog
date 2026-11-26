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
import java.util.Set;
import java.util.UUID;

/**
 * 独立备份任务协议，只传递类型及上传意图，不接受文件路径或命令。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/25
 */
public record BackupJob(String id, Set<BackupType> types, boolean offsite) {
  public static final Path STATUS = Path.of("/var/lib/today-blog-backup-jobs");

  public static final Path POLICY = Path.of("/etc/today-blog/backup-console.properties");

  public BackupJob {
    if (!id.matches("[0-9a-f]{32}")) {
      throw new IllegalArgumentException("无效备份任务 ID");
    }
    types = Set.copyOf(types);
    BackupType.encode(types);
  }

  public static BackupJob create(Set<BackupType> types, boolean offsite) {
    return new BackupJob(UUID.randomUUID().toString().replace("-", ""), types, offsite);
  }

  public String encode() {
    return id + "_" + BackupType.encode(types).replace(',', '-') + "_" + (offsite ? "1" : "0");
  }

  public static BackupJob parse(String value) {
    String[] parts = value.split("_", -1);
    if (parts.length != 3 || !parts[2].matches("[01]")) {
      throw new IllegalArgumentException("无效备份任务");
    }
    var job = new BackupJob(parts[0], BackupType.parse(parts[1].replace('-', ',')), parts[2].equals("1"));
    if (!job.encode().equals(value)) {
      throw new IllegalArgumentException("备份任务编码无效");
    }
    return job;
  }
}
