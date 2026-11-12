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

package cn.taketoday.blog.model;

import infra.persistence.annotation.Id;
import infra.persistence.annotation.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 应用快照元信息。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/12
 */
@Getter
@Setter
@Table("t_app_snapshot")
public class AppSnapshot {

  @Id
  private String id;

  private String name;

  private String status;

  private String version;

  private String build;

  private Long operatorId;

  private String createdAt;

  private boolean protectedSnapshot;

  private String manifestSha256;

  private long totalBytes;

  private String sourceOperationId;

  private String metadata;

}
