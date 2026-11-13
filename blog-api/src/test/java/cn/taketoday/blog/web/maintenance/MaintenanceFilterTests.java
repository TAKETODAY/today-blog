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

import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.blog.updater.UpgradeFiles;
import infra.web.mock.MockHttpContext;

import static org.assertj.core.api.Assertions.assertThat;

class MaintenanceFilterTests {

  @TempDir
  Path temp;

  @Test
  void rejectsRequestsWhileMaintenanceMarkerExists() throws Exception {
    Path marker = temp.resolve("maintenance.properties");
    Properties properties = new Properties();
    properties.setProperty("message", "系统升级中");
    properties.setProperty("retryAfter", "60");
    UpgradeFiles.write(marker, properties);
    MaintenanceFilter filter = new MaintenanceFilter(new MaintenanceMode(marker));

    AtomicBoolean proceeded = new AtomicBoolean();
    MockHttpContext context = request("GET", "/api/articles");

    filter.doFilter(context, ignored -> proceeded.set(true));

    assertThat(proceeded).isFalse();
    assertThat(context.getStatus()).isEqualTo(503);
    assertThat(context.responseHeaders().getFirst("Retry-After")).isEqualTo("60");
    assertThat(context.getResponse().getContentAsString()).contains("系统升级中");
  }

  @Test
  void allowsHealthAndUpgradeStatus() throws Exception {
    Path marker = temp.resolve("maintenance.properties");
    UpgradeFiles.write(marker, new Properties());
    MaintenanceFilter filter = new MaintenanceFilter(new MaintenanceMode(marker));

    MockHttpContext health = request("GET", MaintenanceFilter.HEALTH_PATH);
    AtomicBoolean healthProceeded = new AtomicBoolean();
    filter.doFilter(health, ignored -> healthProceeded.set(true));
    assertThat(healthProceeded).isTrue();

    MockHttpContext status = request("GET", MaintenanceFilter.UPGRADE_PATH + "/1c1f4b1e-0000-4000-8000-000000000000");
    AtomicBoolean statusProceeded = new AtomicBoolean();
    filter.doFilter(status, ignored -> statusProceeded.set(true));
    assertThat(statusProceeded).isTrue();

    MockHttpContext start = request("POST", MaintenanceFilter.UPGRADE_PATH + "/1c1f4b1e-0000-4000-8000-000000000000/start");
    AtomicBoolean startProceeded = new AtomicBoolean();
    filter.doFilter(start, ignored -> startProceeded.set(true));
    assertThat(startProceeded).isFalse();
  }

  @Test
  void servesNormallyWithoutMarker() throws Exception {
    Path marker = temp.resolve("maintenance.properties");
    MaintenanceFilter filter = new MaintenanceFilter(new MaintenanceMode(marker));

    AtomicBoolean proceeded = new AtomicBoolean();
    filter.doFilter(request("GET", "/api/articles"), ignored -> proceeded.set(true));

    assertThat(proceeded).isTrue();
  }

  private static MockHttpContext request(String method, String uri) {
    MockHttpContext context = new MockHttpContext();
    context.getRequest().setMethod(method);
    context.getRequest().setRequestURI(uri);
    return context;
  }
}
