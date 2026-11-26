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

package cn.taketoday.blog.maintenance;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.blog.config.BlogLocaleResolver;
import cn.taketoday.blog.problem.ProblemResponses;
import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.ApplicationEventPublisher;
import infra.context.availability.ApplicationAvailability;
import infra.core.io.PathMatchingPatternResourceLoader;
import infra.http.ProblemDetail;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.web.mock.MockHttpContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@JUnitConfig(BlogLocaleResolver.class)
class MaintenanceFilterTests {

  @Autowired
  ApplicationContext applicationContext;

  @Test
  void rejectsRequestsDuringMaintenance() throws Exception {
    MaintenanceFilter filter = filter(new MaintenanceStatus(
            true, "系统升级中", 60, MaintenanceStatus.Source.MANUAL));

    AtomicBoolean proceeded = new AtomicBoolean();
    RecordingHttpContext context = request("GET", "/api/articles");

    filter.doFilter(context, ignored -> proceeded.set(true));

    assertThat(proceeded).isFalse();
    assertThat(context.getStatus()).isEqualTo(503);
    assertThat(context.responseHeaders().getFirst("Retry-After")).isEqualTo("60");
    assertThat(context.responseHeaders().getFirst("Content-Type")).contains("application/problem+json");
    assertThat(context.getSentProblem().getDetail()).isEqualTo("系统升级中");
  }

  @Test
  void localizesDefaultMaintenance() throws Exception {
    RecordingHttpContext context = request("GET", "/api/articles");
    context.getRequest().addHeader("Accept-Language", "en-US");
    filter(new MaintenanceStatus(true, null, 30, MaintenanceStatus.Source.MANUAL))
            .doFilter(context, ignored -> {
            });
    assertThat(context.responseHeaders().getFirst("Content-Type")).contains("application/problem+json");
    assertThat(context.responseHeaders().getFirst("Content-Language")).isEqualTo("en");
    assertThat(context.getSentProblem())
            .satisfies(problem -> {
              assertThat(problem.getTitle()).isEqualTo("System under maintenance");
              assertThat(problem.getDetail()).isEqualTo("The system is under maintenance. Please try again later.");
              assertThat(problem.getProperties()).containsOnlyKeys("code");
              assertThat(problem.getProperties().get("code")).isEqualTo("maintenance.under_maintenance");
            });
  }

  @Test
  void allowsHealthAndUpgradeStatus() throws Exception {
    MaintenanceFilter filter = filter(new MaintenanceStatus(
            true, "系统升级中", 60, MaintenanceStatus.Source.UPGRADE));

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
  void allowsMaintenanceToggle() throws Exception {
    MaintenanceFilter filter = filter(new MaintenanceStatus(
            true, "系统维护中", 120, MaintenanceStatus.Source.MANUAL));
    String toggle = MaintenanceFilter.MAINTENANCE_PATH;

    AtomicBoolean queried = new AtomicBoolean();
    filter.doFilter(request("GET", toggle), ignored -> queried.set(true));
    assertThat(queried).isTrue();

    AtomicBoolean updated = new AtomicBoolean();
    filter.doFilter(request("PUT", toggle), ignored -> updated.set(true));
    assertThat(updated).isTrue();

    MockHttpContext rejected = request("GET", "/api/articles");
    filter.doFilter(rejected, ignored -> {
    });
    assertThat(rejected.getStatus()).isEqualTo(503);
    assertThat(rejected.responseHeaders().getFirst("Cache-Control")).isEqualTo("no-store");
  }

  @Test
  void allowsSessionProbeAndLoginDuringManualMaintenance() throws Exception {
    MaintenanceFilter filter = filter(new MaintenanceStatus(
            true, "手动维护中", 120, MaintenanceStatus.Source.MANUAL));

    AtomicBoolean probe = new AtomicBoolean();
    filter.doFilter(request("GET", MaintenanceFilter.AUTH_PATH), ignored -> probe.set(true));
    assertThat(probe).isTrue();

    AtomicBoolean login = new AtomicBoolean();
    filter.doFilter(request("POST", MaintenanceFilter.AUTH_PATH), ignored -> login.set(true));
    assertThat(login).isTrue();

    MockHttpContext update = request("PUT", MaintenanceFilter.AUTH_PATH);
    filter.doFilter(update, ignored -> {
    });
    assertThat(update.getStatus()).isEqualTo(503);
  }

  @Test
  void servesNormallyWhenMaintenanceDisabled() throws Exception {
    MaintenanceFilter filter = filter(MaintenanceStatus.DISABLED);

    AtomicBoolean proceeded = new AtomicBoolean();
    filter.doFilter(request("GET", "/api/articles"), ignored -> proceeded.set(true));

    assertThat(proceeded).isTrue();
  }

  private static MaintenanceFilter filter(MaintenanceStatus status) {
    ServerStatus serverStatus = new ServerStatus(
            mock(ApplicationAvailability.class), mock(ApplicationEventPublisher.class));
    serverStatus.setMaintenanceStatus(status);
    return new MaintenanceFilter(serverStatus, new ProblemResponses(new PathMatchingPatternResourceLoader()));
  }

  private RecordingHttpContext request(String method, String uri) {
    RecordingHttpContext context = new RecordingHttpContext(applicationContext);
    context.getRequest().setMethod(method);
    context.getRequest().setRequestURI(uri);
    return context;
  }

  /**
   * 记录过滤器直接发送的问题响应，避免单元测试依赖完整的 MVC 调度上下文。
   */
  private static final class RecordingHttpContext extends MockHttpContext {

    private @Nullable ProblemDetail sentProblem;

    RecordingHttpContext(ApplicationContext applicationContext) {
      super(applicationContext);
    }

    @Override
    public void send(@Nullable Object result) {
      if (result instanceof ProblemDetail problem) {
        this.sentProblem = problem;
      }
    }

    ProblemDetail getSentProblem() {
      assertThat(sentProblem).isNotNull();
      return sentProblem;
    }
  }
}
