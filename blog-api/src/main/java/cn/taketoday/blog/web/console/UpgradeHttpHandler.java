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

package cn.taketoday.blog.web.console;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import cn.taketoday.blog.log.Logging;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.updater.UpgradeFiles;
import cn.taketoday.blog.updater.UpgradePackage;
import cn.taketoday.blog.web.ErrorMessageException;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import infra.http.HttpStatus;
import infra.web.annotation.GET;
import infra.web.annotation.POST;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;
import infra.web.multipart.Part;

/**
 * 管理端仅上传与派发任务，安装与重启由独立 systemd unit 执行。
 */
@RequiresBlogger
@RestController
@RequestMapping("/api/console/upgrade")
class UpgradeHttpHandler {

  private final Path state = Path.of(System.getenv().getOrDefault("BLOG_UPGRADE_STATE", "/var/lib/today-blog-upgrade"));


  @GET("/current")
  public Properties current() throws Exception {
    String release = System.getenv("BLOG_RELEASE_PATH");
    if (release == null) {
      Properties result = new Properties();
      result.setProperty("version", "开发环境（未启用在线升级）");
      return result;
    }
    return UpgradeFiles.read(Path.of(release).resolve("release.properties"));
  }

  @POST("/packages")
  @Logging(title = "上传升级包", content = "操作人: [#{#user.id}]")
  public synchronized Properties upload(Part file, User user) throws Exception {
    enabled();
    if (file.getContentLength() <= 0 || file.getContentLength() > UpgradePackage.MAX_PACKAGE_BYTES) {
      throw ErrorMessageException.failed("升级包大小必须在 1 字节至 512 MiB 之间");
    }
    String id = UUID.randomUUID().toString();
    Path task = Files.createDirectories(state.resolve("incoming").resolve(id));
    Path temp = task.resolve("upload.tmp");
    try {
      try (var input = file.getInputStream(); var output = Files.newOutputStream(temp)) {
        byte[] buffer = new byte[65536];
        long total = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
          total += count;
          if (total > UpgradePackage.MAX_PACKAGE_BYTES) {
            throw new java.io.IOException("升级包过大");
          }
          output.write(buffer, 0, count);
        }
      }
      var manifest = UpgradePackage.verify(temp);
      long currentBuild = Long.parseLong(current().getProperty("build"));
      if (manifest.build() <= currentBuild) {
        throw ErrorMessageException.failed("禁止重复安装或降级");
      }
      Files.move(temp, task.resolve("package.zip"), StandardCopyOption.ATOMIC_MOVE);
      Properties metadata = new Properties();
      metadata.setProperty("id", id);
      metadata.setProperty("version", manifest.version());
      metadata.setProperty("build", Long.toString(manifest.build()));
      metadata.setProperty("operatorId", String.valueOf(user.getId()));
      metadata.setProperty("status", "READY");
      metadata.setProperty("message", "文件完整性校验通过（未验证来源），等待确认升级");
      UpgradeFiles.write(task.resolve("metadata.properties"), metadata);
      return metadata;
    }
    catch (Exception error) {
      Files.deleteIfExists(task.resolve("package.zip"));
      throw ErrorMessageException.failed("升级包校验失败: " + error.getMessage());
    }
    finally {
      Files.deleteIfExists(temp);
    }
  }

  @POST("/{id}/start")
  @Logging(title = "启动系统升级", content = "任务: [#{#id}]，操作人: [#{#user.id}]")
  public synchronized Map<String, String> start(@PathVariable String id, User user) throws Exception {
    enabled();
    UpgradeFiles.checkId(id);
    Path metadataPath = state.resolve("incoming").resolve(id).resolve("metadata.properties");
    Properties metadata = UpgradeFiles.read(metadataPath);
    if (!"READY".equals(metadata.getProperty("status"))) {
      throw ErrorMessageException.failed("该任务已提交，不可重复启动", HttpStatus.CONFLICT);
    }
    metadata.setProperty("status", "STARTING");
    metadata.setProperty("message", "任务已提交，请等待升级器启动");
    UpgradeFiles.write(metadataPath, metadata);
    Process process = new ProcessBuilder("/usr/bin/sudo", "-n", "/usr/bin/systemctl", "start", "--no-block",
            "today-blog-updater@" + id + ".service").redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
    if (!process.waitFor(10, TimeUnit.SECONDS) || process.exitValue() != 0) {
      process.destroyForcibly();
      metadata.setProperty("status", "FAILED");
      metadata.setProperty("message", "无法启动升级服务，请检查 sudoers 和 systemd 日志");
      UpgradeFiles.write(metadataPath, metadata);
      throw ErrorMessageException.failed(metadata.getProperty("message"));
    }
    return Map.of("id", id, "status", "STARTING");
  }

  @GET("/{id}")
  public Properties status(@PathVariable String id) throws Exception {
    UpgradeFiles.checkId(id);
    Properties result = UpgradeFiles.read(state.resolve("incoming").resolve(id).resolve("metadata.properties"));
    Path status = state.resolve("results").resolve(id + ".properties");
    if (Files.exists(status)) {
      result.putAll(UpgradeFiles.read(status));
    }
    return result;
  }

  private void enabled() {
    if (System.getenv("BLOG_RELEASE_PATH") == null) {
      throw ErrorMessageException.failed("请先完成 systemd 部署配置");
    }
  }
}
