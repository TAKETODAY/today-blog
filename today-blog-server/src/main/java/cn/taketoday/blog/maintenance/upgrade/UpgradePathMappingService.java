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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.taketoday.blog.updater.ReleaseMapping;
import infra.persistence.EntityManager;
import infra.stereotype.Service;
import infra.transaction.annotation.Transactional;

/**
 * 升级记录的文件目录映射快照。
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
@Service
public class UpgradePathMappingService {

  private final EntityManager entityManager;

  public UpgradePathMappingService(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public List<ReleaseMapping> find(long recordId) {
    List<UpgradePathMappingEntity> entities = entityManager.find(
            UpgradePathMappingEntity.class, Map.of("recordId", recordId));
    List<ReleaseMapping> mappings = new ArrayList<>(entities.size());
    for (UpgradePathMappingEntity entity : entities) {
      mappings.add(ReleaseMapping.fromStorage(entity.getName(),
              ReleaseMapping.Mode.valueOf(entity.getMode()), entity.getSource(), entity.getTarget()));
    }
    return List.copyOf(mappings);
  }

  @Transactional
  public void replace(long recordId, List<ReleaseMapping> mappings) {
    for (UpgradePathMappingEntity entity : entityManager.find(
            UpgradePathMappingEntity.class, Map.of("recordId", recordId))) {
      entityManager.delete(UpgradePathMappingEntity.class, entity.getId());
    }
    for (ReleaseMapping mapping : mappings) {
      UpgradePathMappingEntity entity = new UpgradePathMappingEntity();
      entity.setRecordId(recordId);
      entity.setName(mapping.name());
      entity.setMode(mapping.mode().name());
      entity.setSource(mapping.source());
      entity.setTarget(mapping.storedTarget());
      entityManager.persist(entity);
    }
  }

}
