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

package cn.taketoday.blog.maintenance.backup.console;

import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.blog.maintenance.backup.BackupService;
import cn.taketoday.blog.updater.BackupType;
import cn.taketoday.blog.updater.BackupJob;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import infra.core.io.FileSystemResource;
import infra.http.ContentDisposition;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.ResponseEntity;
import infra.web.HttpContext;
import infra.web.annotation.DELETE;
import infra.web.annotation.GET;
import infra.web.annotation.POST;
import infra.web.annotation.PUT;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RestController;

/**
 * 系统备份接口，所有操作均要求博主身份。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/12
 */
@RestController
@RequiresBlogger
@RequestMapping("/api/console/backups")
class BackupHttpHandler {

  private final BackupService backups;

  public BackupHttpHandler(BackupService backups) {
    this.backups = backups;
  }

  @GET
  public List<BackupService.BackupView> list() {
    return backups.backups();
  }

  @GET("/capabilities")
  public Map<String, Object> capabilities() {
    return backups.capabilities();
  }

  @GET("/{id}")
  public BackupService.BackupDetail detail(@PathVariable long id) {
    return backups.backup(id);
  }

  @GET("/{id}/download")
  public ResponseEntity<?> download(@PathVariable long id, HttpContext request) {
    BackupService.Download download = backups.download(id);
    String filename = "database-backup-" + id + ".sql.gz";
    var response = ResponseEntity.ok()
            .contentType("application/gzip")
            .contentLength(download.bytes())
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString());
    if (request.getMethod() == HttpMethod.HEAD) {
      return response.build();
    }
    return response.body(new FileSystemResource(download.archive()));
  }

  @POST
  public BackupJob create(@RequestBody(required = false) CreateRequest request) {
    return backups.submit(request == null ? null : request.types(), request != null && request.offsite());
  }

  @GET("/jobs")
  public List<BackupService.JobView> jobs() {
    return backups.jobs();
  }

  public record CreateRequest(Set<BackupType> types, boolean offsite) {
  }

  @POST("/{id}/restore")
  public void restore(@PathVariable long id) {
    backups.requireExecutor();
  }

  @PUT("/{id}/protection")
  public void protection(@PathVariable long id) {
    backups.requireExecutor();
  }

  @DELETE("/{id}")
  public void delete(@PathVariable long id) {
    backups.requireExecutor();
  }
}
