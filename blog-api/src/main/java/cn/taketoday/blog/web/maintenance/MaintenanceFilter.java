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

import java.io.IOException;

import cn.taketoday.blog.MaintenanceStatus;
import cn.taketoday.blog.ServerStatus;
import cn.taketoday.blog.util.ObjectUtils;
import cn.taketoday.blog.web.ErrorMessage;
import infra.core.Ordered;
import infra.core.annotation.Order;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.server.PathContainer;
import infra.stereotype.Component;
import infra.web.Filter;
import infra.web.FilterChain;
import infra.web.HttpContext;
import infra.web.util.pattern.PathPattern;
import infra.web.util.pattern.PathPatternParser;

/**
 * 维护状态下拒绝新的业务请求，但保持 API 进程运行。
 * <p>
 * 管理端开启维护或提交升级后，API 立即返回 {@code 503 Service Unavailable}，
 * 让客户端稍后重试，而不是直接断开连接。健康检查和升级进度查询保持可用，
 * 以便升级器验证新版本并让管理端继续轮询任务状态。
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
final class MaintenanceFilter implements Filter {

  static final String HEALTH_PATH = "/api/internal/health";

  static final String MAINTENANCE_PATH = "/api/console/maintenance";

  static final String UPGRADE_PATH = MAINTENANCE_PATH + "/upgrade";

  private static final PathPattern HEALTH_PATTERN = PathPatternParser.defaultInstance.parse(HEALTH_PATH);

  private static final PathPattern MAINTENANCE_PATTERN = PathPatternParser.defaultInstance.parse(MAINTENANCE_PATH);

  private static final PathPattern UPGRADE_PATTERN = PathPatternParser.defaultInstance.parse(UPGRADE_PATH + "/**");

  private final ServerStatus serverStatus;

  MaintenanceFilter(ServerStatus serverStatus) {
    this.serverStatus = serverStatus;
  }

  @Override
  public void doFilter(HttpContext request, FilterChain chain) throws Exception {
    MaintenanceStatus maintenanceStatus = serverStatus.getMaintenanceStatus();
    if (!maintenanceStatus.enabled() || available(request)) {
      chain.doFilter(request);
      return;
    }
    writeUnavailable(request, maintenanceStatus);
  }

  private static boolean available(HttpContext request) {
    PathContainer path = request.getRequestPath().pathWithinApplication();
    if (HEALTH_PATTERN.matches(path)) {
      return true;
    }
    // 维护开关必须始终可用，否则手动开启后无法通过 HTTP 关闭。
    if (MAINTENANCE_PATTERN.matches(path)) {
      return true;
    }
    // 管理端需要继续轮询升级结果，只放行只读查询。
    return HttpMethod.GET.equals(request.getMethod()) && UPGRADE_PATTERN.matches(path);
  }

  private static void writeUnavailable(HttpContext request, MaintenanceStatus status) throws IOException {
    request.setStatus(HttpStatus.SERVICE_UNAVAILABLE);
    request.setContentType(MediaType.APPLICATION_JSON);
    request.setHeader(HttpHeaders.RETRY_AFTER, Integer.toString(status.retryAfterSeconds()));
    request.setHeader(HttpHeaders.CACHE_CONTROL, HttpHeaders.NO_STORE);
    ObjectUtils.writeValue(request, ErrorMessage.failed(status.message()));
  }
}
