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

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import cn.taketoday.blog.web.ErrorMessageException;
import infra.http.HttpStatus;
import infra.persistence.EntityManager;
import infra.persistence.annotation.EntityRef;
import infra.persistence.annotation.In;
import infra.stereotype.Service;
import infra.transaction.annotation.Transactional;

/**
 * 升级记录数据库访问。
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
@Service
public class UpgradeRecordService {

  private static final List<UpgradeStatus> RUNNING_STATUSES = List.of(
          UpgradeStatus.STARTING,
          UpgradeStatus.VALIDATING,
          UpgradeStatus.INSTALLING,
          UpgradeStatus.BACKING_UP,
          UpgradeStatus.RESTARTING,
          UpgradeStatus.HEALTH_CHECKING,
          UpgradeStatus.ROLLING_BACK);

  private final EntityManager entityManager;

  public UpgradeRecordService(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public List<UpgradeRecordEntity> findAll() {
    return entityManager.find(UpgradeRecordEntity.class).stream()
            .sorted(Comparator.comparing(UpgradeRecordEntity::getCreatedAt).reversed())
            .toList();
  }

  public UpgradeRecordEntity find(long id) {
    UpgradeRecordEntity entity = entityManager.findById(UpgradeRecordEntity.class, id);
    if (entity == null) {
      throw ErrorMessageException.failed("升级记录不存在", HttpStatus.NOT_FOUND);
    }
    return entity;
  }

  @Transactional
  public void create(UpgradeRecordEntity entity) {
    entityManager.persist(entity);
  }

  @Transactional
  public void status(long id, UpgradeStatus status, String message) {
    UpgradeRecordEntity entity = find(id);
    entity.setStatus(status);
    entity.setMessage(message);
    entityManager.updateById(entity);
  }

  @Transactional
  public void failed(long id, String message) {
    UpgradeRecordEntity entity = find(id);
    entity.setStatus(UpgradeStatus.FAILED);
    entity.setMessage(message);
    entity.setFinishedAt(Instant.now());
    entityManager.updateById(entity);
  }

  public @Nullable UpgradeRecordEntity running() {
    List<UpgradeRecordEntity> records = entityManager.find(UpgradeRecordEntity.class, new RunningRecords());
    return records.isEmpty() ? null : records.getFirst();
  }

  public List<UpgradeRecordEntity> byPackage(long packageId) {
    return entityManager.find(UpgradeRecordEntity.class, new PackageRecords(packageId));
  }

  public boolean hasRunning(long packageId) {
    return byPackage(packageId).stream()
            .anyMatch(record -> RUNNING_STATUSES.contains(record.getStatus()));
  }

  @EntityRef(UpgradeRecordEntity.class)
  static class RunningRecords {

    @In
    public final List<UpgradeStatus> status = RUNNING_STATUSES;

  }

  @EntityRef(UpgradeRecordEntity.class)
  static class PackageRecords {

    public final long packageId;

    PackageRecords(long packageId) {
      this.packageId = packageId;
    }

  }

}
