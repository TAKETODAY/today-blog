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

package cn.taketoday.blog.snapshot;

import java.util.List;
import java.util.Map;

import cn.taketoday.blog.updater.UpgradeFiles;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import infra.web.annotation.DELETE;
import infra.web.annotation.GET;
import infra.web.annotation.POST;
import infra.web.annotation.PUT;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;

/**
 * 应用快照管理接口，所有操作均要求博主身份。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/12
 */
@RestController
@RequiresBlogger
@RequestMapping("/api/console/snapshots")
class SnapshotHttpHandler {

  private final SnapshotService snapshots;

  public SnapshotHttpHandler(SnapshotService snapshots) {
    this.snapshots = snapshots;
  }

  @GET
  public List<SnapshotService.SnapshotView> list() {
    return snapshots.snapshots();
  }

  @GET("/capabilities")
  public Map<String, Object> capabilities() {
    return snapshots.capabilities();
  }

  @GET("/operations")
  public List<SnapshotService.OperationView> operations() {
    return snapshots.operations();
  }

  @GET("/{id}")
  public SnapshotService.SnapshotDetail detail(@PathVariable String id) {
    return snapshots.snapshot(id);
  }

  @POST
  public void create() {
    snapshots.requireExecutor();
  }

  @POST("/{id}/restore")
  public void restore(@PathVariable String id) {
    UpgradeFiles.checkId(id);
    snapshots.requireExecutor();
  }

  @PUT("/{id}/protection")
  public void protection(@PathVariable String id) {
    UpgradeFiles.checkId(id);
    snapshots.requireExecutor();
  }

  @DELETE("/{id}")
  public void delete(@PathVariable String id) {
    UpgradeFiles.checkId(id);
    snapshots.requireExecutor();
  }
}
