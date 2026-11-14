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

import infra.persistence.annotation.Id;
import infra.persistence.annotation.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 上传的升级包。数据库保存元信息，{@code location} 指向实际 ZIP 文件。
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
@Getter
@Setter
@Table("t_upgrade_package")
public class UpgradePackageEntity {

  @Id
  private String id;

  private String originalName;

  private String displayName;

  private String note;

  private UpgradePackageStatus status;

  private String version;

  private long build;

  private long fileSize;

  private String sha256;

  private int entryCount;

  private String location;

  private String createdAt;

  private String consumedAt;

}
