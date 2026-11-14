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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import cn.taketoday.blog.web.ErrorMessageException;
import infra.beans.factory.annotation.Autowired;
import infra.http.HttpStatus;
import infra.persistence.EntityManager;
import infra.stereotype.Service;
import infra.transaction.annotation.Transactional;

/**
 * 升级包的数据库记录与本地文件。
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
@Service
public class UpgradePackageService {

  private final EntityManager entityManager;

  private final Path state;

  @Autowired
  public UpgradePackageService(EntityManager entityManager) {
    this(entityManager, Path.of(System.getenv().getOrDefault(
            "BLOG_UPGRADE_STATE", "/var/lib/today-blog-upgrade")));
  }

  UpgradePackageService(EntityManager entityManager, Path state) {
    this.entityManager = entityManager;
    this.state = state.toAbsolutePath().normalize();
  }

  public List<UpgradePackageEntity> findAll() {
    return entityManager.find(UpgradePackageEntity.class).stream()
            .sorted(Comparator.comparing(UpgradePackageEntity::getCreatedAt).reversed())
            .toList();
  }

  public UpgradePackageEntity find(long id) {
    UpgradePackageEntity entity = entityManager.findById(UpgradePackageEntity.class, id);
    if (entity == null) {
      throw ErrorMessageException.failed("升级包不存在", HttpStatus.NOT_FOUND);
    }
    return entity;
  }

  @Transactional
  public void create(UpgradePackageEntity entity) {
    entityManager.persist(entity);
  }

  @Transactional
  public UpgradePackageEntity update(long id, String displayName, String note) {
    UpgradePackageEntity entity = find(id);
    if (displayName != null) {
      if (displayName.isBlank() || displayName.length() > 255) {
        throw ErrorMessageException.failed("升级包名称必须在 1 至 255 字之间");
      }
      entity.setDisplayName(displayName);
    }
    if (note != null) {
      if (note.length() > 500) {
        throw ErrorMessageException.failed("升级包备注不能超过 500 字");
      }
      entity.setNote(note);
    }
    entityManager.updateById(entity);
    return entity;
  }

  @Transactional
  public void markConsumed(long id) {
    UpgradePackageEntity entity = find(id);
    entity.setStatus(UpgradePackageStatus.CONSUMED);
    entity.setConsumedAt(Instant.now());
    entityManager.updateById(entity);
  }

  @Transactional
  public void markReady(long id) {
    UpgradePackageEntity entity = find(id);
    entity.setStatus(UpgradePackageStatus.READY);
    entity.setConsumedAt(null);
    entityManager.updateById(entity, infra.persistence.PropertyUpdateStrategy.always());
  }

  @Transactional
  public void delete(long id) {
    UpgradePackageEntity entity = find(id);
    Path location = Path.of(entity.getLocation()).toAbsolutePath().normalize();
    entityManager.delete(UpgradePackageEntity.class, id);
    deleteFile(location);
  }

  private void deleteFile(Path location) {
    if (!location.startsWith(state)) {
      throw new IllegalStateException("升级包文件路径不在升级状态目录内");
    }
    try {
      Files.deleteIfExists(location);
      Path parent = location.getParent();
      if (parent != null && !parent.equals(state)) {
        Files.deleteIfExists(parent);
      }
    }
    catch (IOException failure) {
      throw new IllegalStateException("升级包文件删除失败", failure);
    }
  }

}
