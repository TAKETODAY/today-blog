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

package cn.taketoday.blog.web.maintenance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import cn.taketoday.blog.updater.MaintenanceState;
import cn.taketoday.blog.updater.UpgradeFiles;

import static org.assertj.core.api.Assertions.assertThat;

class MaintenanceModeTests {

  @TempDir
  Path temp;

  @Test
  void servesNormallyWithoutStateFile() {
    MaintenanceMode mode = new MaintenanceMode(marker());

    MaintenanceMode.Status status = mode.status();

    assertThat(status.enabled()).isFalse();
    assertThat(status.message()).isNull();
    assertThat(status.source()).isNull();
    assertThat(mode.enabled()).isFalse();
    assertThat(mode.exists()).isFalse();
  }

  @Test
  void reportsManuallyEnabledState() throws Exception {
    MaintenanceMode mode = new MaintenanceMode(marker());

    mode.enable("维护中，请稍后访问", 300, MaintenanceState.Source.MANUAL);

    MaintenanceMode.Status status = mode.status();
    assertThat(status.enabled()).isTrue();
    assertThat(status.message()).isEqualTo("维护中，请稍后访问");
    assertThat(status.retryAfterSeconds()).isEqualTo(300);
    assertThat(status.source()).isEqualTo(MaintenanceState.Source.MANUAL);
    assertThat(mode.exists()).isTrue();
    assertThat(mode.file()).isEqualTo(marker());
  }

  @Test
  void restoresNormalServiceAfterDisable() throws Exception {
    Path marker = marker();
    MaintenanceMode mode = new MaintenanceMode(marker);
    mode.enable("维护中", 60, MaintenanceState.Source.UPGRADE);
    assertThat(mode.enabled()).isTrue();

    mode.disable();

    MaintenanceMode.Status status = mode.status();
    assertThat(status.enabled()).isFalse();
    assertThat(status.message()).isNull();
    assertThat(status.source()).isNull();
    assertThat(Files.exists(marker)).isFalse();
  }

  @Test
  void keepsRejectingWhenStateFileIsUnreadable() throws Exception {
    Path marker = marker();
    // 非法 Unicode 转义会让 Properties.load 抛出异常，模拟状态文件损坏。
    Files.write(marker, "broken=\\uZZZZ".getBytes(StandardCharsets.ISO_8859_1));

    MaintenanceMode.Status status = new MaintenanceMode(marker).status();

    assertThat(status.enabled()).isTrue();
    assertThat(status.message()).isEqualTo(MaintenanceState.DEFAULT_MESSAGE);
    assertThat(status.retryAfterSeconds()).isEqualTo(MaintenanceState.DEFAULT_RETRY_AFTER_SECONDS);
    assertThat(status.source()).isEqualTo(MaintenanceState.Source.UPGRADE);
  }

  @Test
  void keepsRejectingWhenSourceIsUnknown() throws Exception {
    Path marker = marker();
    Properties properties = new Properties();
    properties.setProperty("enabled", "true");
    properties.setProperty("message", "外部写入的维护状态");
    properties.setProperty("retryAfter", "30");
    properties.setProperty("source", "unknown");
    UpgradeFiles.write(marker, properties);

    MaintenanceMode.Status status = new MaintenanceMode(marker).status();

    assertThat(status.enabled()).isTrue();
    assertThat(status.message()).isEqualTo("外部写入的维护状态");
    assertThat(status.retryAfterSeconds()).isEqualTo(30);
    assertThat(status.source()).isNull();
  }

  @Test
  void fallsBackForBlankMessageAndInvalidRetryAfter() throws Exception {
    Path marker = marker();
    Properties properties = new Properties();
    properties.setProperty("enabled", "true");
    properties.setProperty("message", "   ");
    properties.setProperty("retryAfter", "0");
    properties.setProperty("source", "manual");
    UpgradeFiles.write(marker, properties);

    MaintenanceMode.Status status = new MaintenanceMode(marker).status();

    assertThat(status.enabled()).isTrue();
    assertThat(status.message()).isEqualTo(MaintenanceState.DEFAULT_MESSAGE);
    assertThat(status.retryAfterSeconds()).isEqualTo(MaintenanceState.DEFAULT_RETRY_AFTER_SECONDS);
    assertThat(status.source()).isEqualTo(MaintenanceState.Source.MANUAL);
  }

  @Test
  void existsIgnoresCachedStatus() throws Exception {
    Path marker = marker();
    MaintenanceState.enable(marker, "外部写入的维护状态", MaintenanceState.Source.UPGRADE);
    MaintenanceMode mode = new MaintenanceMode(marker);
    // 先读一次让短缓存生效，再绕过 MaintenanceMode 直接删除文件。
    assertThat(mode.status().enabled()).isTrue();

    Files.delete(marker);

    assertThat(mode.exists()).isFalse();
  }

  private Path marker() {
    return temp.resolve(MaintenanceState.FILE_NAME);
  }
}
