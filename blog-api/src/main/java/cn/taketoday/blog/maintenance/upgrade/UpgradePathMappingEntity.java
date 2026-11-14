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

import infra.persistence.annotation.GeneratedId;
import infra.persistence.annotation.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 升级任务启动时的文件目录映射快照。
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
@Getter
@Setter
@Table("t_upgrade_path_mapping")
public class UpgradePathMappingEntity {

  @GeneratedId
  private Long id;

  private String recordId;

  private String name;

  private String mode;

  private String source;

  private String target;

}
