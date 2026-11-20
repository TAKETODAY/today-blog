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

package cn.taketoday.blog.maintenance.backup;

import java.time.Instant;

import infra.persistence.annotation.GeneratedId;
import infra.persistence.annotation.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 备份元信息，{@code location} 指向本地备份文件。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/12
 */
@Getter
@Setter
@Table("t_backup")
public class Backup {

  @GeneratedId
  private Long id;

  private String name;

  private BackupStatus status;

  private String version;

  private String build;

  private Instant createdAt;

  private boolean protectedBackup;

  private String manifestSha256;

  private long totalBytes;

  private Long sourceOperationId;

  private String location;

  private String scope;

  private Instant restoreVerifiedAt;

}
