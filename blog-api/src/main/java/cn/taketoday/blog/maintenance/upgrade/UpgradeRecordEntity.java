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

package cn.taketoday.blog.maintenance.upgrade;

import java.time.Instant;

import infra.persistence.annotation.GeneratedId;
import infra.persistence.annotation.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 升级记录。包信息保存快照，升级包删除后历史仍可展示。
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
@Getter
@Setter
@Table("t_upgrade_record")
public class UpgradeRecordEntity {

  @GeneratedId
  private Long id;

  private Long packageId;

  private String packageName;

  private String packageVersion;

  private long packageBuild;

  private String packageSha256;

  private String packagePath;

  private String oldVersion;

  private Long oldBuild;

  private UpgradeStatus status;

  private boolean backupEnabled;

  private int backupTimeoutSeconds;

  private int healthTimeoutSeconds;

  private String note;

  private boolean deletePackageAfterUpgrade;

  private String releasePath;

  private String backupPath;

  private Long backupBytes;

  private String backupSha256;

  private Instant backupCompletedAt;

  private String message;

  private Instant createdAt;

  private Instant startedAt;

  private Instant finishedAt;

}
