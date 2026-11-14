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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.blog.updater.ReleaseMapping;
import cn.taketoday.blog.updater.ReleaseMappings;
import infra.persistence.EntityManager;
import infra.persistence.PropertyUpdateStrategy;
import infra.stereotype.Service;
import infra.transaction.annotation.Transactional;

/**
 * 前端文件映射配置的数据库读写。
 * <p>
 * 数据库是可编辑配置的唯一来源；升级器通过任务快照表读取本次配置。
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
@Service
public class ReleaseMappingService {

  private final EntityManager entityManager;

  public ReleaseMappingService(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public List<ReleaseMapping> findAll() {
    List<ReleaseMappingEntity> entities = entityManager.find(ReleaseMappingEntity.class);
    List<ReleaseMapping> mappings = new ArrayList<>(entities.size());
    for (ReleaseMappingEntity entity : entities) {
      mappings.add(toMapping(entity));
    }
    mappings.sort(Comparator.comparing(ReleaseMapping::name));
    return List.copyOf(mappings);
  }

  @Transactional
  public void replaceAll(List<ReleaseMapping> mappings) {
    // 先完成结构和实体校验，任何一条配置非法都不修改数据库。
    ReleaseMappings.validateStructure(mappings);
    List<ReleaseMappingEntity> entities = new ArrayList<>(mappings.size());
    for (ReleaseMapping mapping : mappings) {
      entities.add(toEntity(mapping));
    }

    String now = Instant.now().toString();
    Map<String, ReleaseMappingEntity> existing = new HashMap<>();
    for (ReleaseMappingEntity entity : entityManager.find(ReleaseMappingEntity.class)) {
      existing.put(entity.getName(), entity);
    }
    Set<String> retained = new HashSet<>();
    for (ReleaseMappingEntity entity : entities) {
      ReleaseMappingEntity previous = existing.get(entity.getName());
      if (previous == null) {
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entityManager.persist(entity);
      }
      else {
        entity.setCreatedAt(previous.getCreatedAt());
        entity.setUpdatedAt(now);
        entityManager.updateById(entity, PropertyUpdateStrategy.always());
      }
      retained.add(entity.getName());
    }
    for (String name : existing.keySet()) {
      if (!retained.contains(name)) {
        entityManager.delete(ReleaseMappingEntity.class, name);
      }
    }
  }

  private static ReleaseMapping toMapping(ReleaseMappingEntity entity) {
    try {
      return new ReleaseMapping(entity.getName(),
              ReleaseMapping.Mode.valueOf(entity.getMode()), entity.getSource(), entity.getTarget());
    }
    catch (RuntimeException corrupted) {
      throw new IllegalStateException("文件映射配置损坏: " + entity.getName(), corrupted);
    }
  }

  private static ReleaseMappingEntity toEntity(ReleaseMapping mapping) {
    ReleaseMappingEntity entity = new ReleaseMappingEntity();
    entity.setName(mapping.name());
    entity.setMode(mapping.mode().name());
    entity.setSource(mapping.source());
    entity.setTarget(mapping.target());
    return entity;
  }

}
