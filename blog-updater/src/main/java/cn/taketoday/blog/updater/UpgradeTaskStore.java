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

package cn.taketoday.blog.updater;

/**
 * 升级器与数据库的任务状态协议。
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public interface UpgradeTaskStore extends AutoCloseable {

  UpgradeTask load(long id) throws Exception;

  void status(long id, String status, String message) throws Exception;

  void started(long id, String releasePath) throws Exception;

  void backup(long id, DatabaseBackupResult backup) throws Exception;

  void finished(long id, String status, String message) throws Exception;

  void deletePackage(long packageId) throws Exception;

  @Override
  void close() throws Exception;

}
