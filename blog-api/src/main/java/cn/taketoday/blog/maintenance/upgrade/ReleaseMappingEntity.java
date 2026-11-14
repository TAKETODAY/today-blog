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
 * 前端发行文件映射配置。
 * <p>
 * 数据库保存可编辑的默认配置；启动升级任务时再快照为磁盘文件交给只依赖 JDK 的升级器。
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
@Getter
@Setter
@Table("t_release_mapping")
public class ReleaseMappingEntity {

  @Id
  private String name;

  private String mode;

  private String source;

  private String target;

  private String createdAt;

  private String updatedAt;

}
