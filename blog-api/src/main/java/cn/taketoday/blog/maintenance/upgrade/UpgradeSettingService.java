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

import cn.taketoday.blog.updater.UpgradeOptions;
import infra.persistence.EntityManager;
import infra.stereotype.Service;
import infra.transaction.annotation.Transactional;

/**
 * 升级默认参数。
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
@Service
public class UpgradeSettingService {

  private static final Integer DEFAULT_ID = 1;

  private final EntityManager entityManager;

  public UpgradeSettingService(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public UpgradeOptions get() {
    UpgradeSettingEntity entity = entityManager.findById(UpgradeSettingEntity.class, DEFAULT_ID);
    if (entity == null) {
      return UpgradeOptions.defaults();
    }
    return new UpgradeOptions(entity.isBackupEnabled(), entity.getBackupTimeoutSeconds(),
            entity.getHealthTimeoutSeconds(), entity.getNote() == null ? "" : entity.getNote());
  }

  @Transactional
  public void save(UpgradeOptions options) {
    UpgradeSettingEntity entity = entityManager.findById(UpgradeSettingEntity.class, DEFAULT_ID);
    if (entity == null) {
      entity = new UpgradeSettingEntity();
      entity.setId(DEFAULT_ID);
      entity.setBackupEnabled(options.backupEnabled());
      entity.setBackupTimeoutSeconds(options.backupTimeoutSeconds());
      entity.setHealthTimeoutSeconds(options.healthTimeoutSeconds());
      entity.setNote(options.note());
      entityManager.persist(entity);
      return;
    }
    entity.setBackupEnabled(options.backupEnabled());
    entity.setBackupTimeoutSeconds(options.backupTimeoutSeconds());
    entity.setHealthTimeoutSeconds(options.healthTimeoutSeconds());
    entity.setNote(options.note());
    entityManager.updateById(entity);
  }

}
