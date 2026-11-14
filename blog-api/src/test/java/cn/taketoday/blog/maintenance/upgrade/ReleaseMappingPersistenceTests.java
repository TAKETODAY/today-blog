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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import cn.taketoday.blog.updater.ReleaseMapping;
import cn.taketoday.blog.web.WebAPITest;
import infra.beans.factory.annotation.Autowired;
import infra.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 文件映射配置在数据库中的持久化行为。
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
@WebAPITest
class ReleaseMappingPersistenceTests {

  @Autowired
  ReleaseMappingService service;

  @Autowired
  EntityManager entityManager;

  @TempDir
  Path temp;

  @Test
  void persistsReplacesAndRemovesMappings() {
    ReleaseMapping console = mapping("console", "console", temp.resolve("console"));
    service.replaceAll(List.of(console));

    assertThat(service.findAll()).containsExactly(console);
    ReleaseMappingEntity saved = entityManager.findById(ReleaseMappingEntity.class, "console");
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();

    ReleaseMapping desktop = mapping("desktop", "desktop", temp.resolve("desktop"));
    service.replaceAll(List.of(desktop));

    assertThat(service.findAll()).containsExactly(desktop);
    assertThat(entityManager.findById(ReleaseMappingEntity.class, "console")).isNull();
  }

  @Test
  void invalidMappingNeverChangesDatabase() {
    ReleaseMapping desktop = mapping("desktop", "desktop", temp.resolve("desktop"));
    service.replaceAll(List.of(desktop));

    ReleaseMapping duplicate = mapping("desktop", "console", temp.resolve("duplicate"));
    assertThatThrownBy(() -> service.replaceAll(List.of(desktop, duplicate)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("名称重复");

    assertThat(service.findAll()).containsExactly(desktop);
  }

  private ReleaseMapping mapping(String name, String source, Path target) {
    return new ReleaseMapping(name, ReleaseMapping.Mode.SYMLINK, source, target.toString());
  }

}
