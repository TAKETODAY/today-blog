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

import cn.taketoday.blog.util.ObjectUtils;
import cn.taketoday.blog.web.ErrorMessage;
import infra.core.Ordered;
import infra.core.annotation.Order;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.stereotype.Component;
import infra.web.Filter;
import infra.web.FilterChain;
import infra.web.HttpContext;

/**
 * 维护状态下拒绝新的业务请求，但保持 API 进程运行。
 * <p>
 * 升级器在切换版本前写入维护状态文件，API 立即返回 {@code 503 Service Unavailable}，
 * 让客户端稍后重试，而不是直接断开连接。健康检查和升级进度查询保持可用，
 * 以便升级器验证新版本并让管理端继续轮询任务状态。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
final class MaintenanceFilter implements Filter {

  static final String HEALTH_PATH = "/api/internal/health";

  static final String UPGRADE_PATH = "/api/console/upgrade";

  private final MaintenanceMode maintenance;

  MaintenanceFilter(MaintenanceMode maintenance) {
    this.maintenance = maintenance;
  }

  @Override
  public void doFilter(HttpContext request, FilterChain chain) throws Exception {
    MaintenanceMode.Status status = maintenance.status();
    if (!status.enabled() || available(request)) {
      chain.doFilter(request);
      return;
    }
    writeUnavailable(request, status);
  }

  private static boolean available(HttpContext request) {
    String uri = request.getRequestURI();
    if (HEALTH_PATH.equals(uri)) {
      return true;
    }
    // 维护开关必须始终可用，否则手动开启后无法通过 HTTP 关闭。
    if ((UPGRADE_PATH + "/maintenance").equals(uri)) {
      return true;
    }
    // 管理端需要继续轮询升级结果，只放行只读查询。
    return HttpMethod.GET.equals(request.getMethod())
            && (UPGRADE_PATH.equals(uri) || uri.startsWith(UPGRADE_PATH + "/"));
  }

  private static void writeUnavailable(HttpContext request, MaintenanceMode.Status status) throws IOException {
    request.setStatus(HttpStatus.SERVICE_UNAVAILABLE);
    request.setContentType(MediaType.APPLICATION_JSON);
    request.setHeader("Retry-After", Integer.toString(status.retryAfterSeconds()));
    request.setHeader("Cache-Control", "no-store");
    ObjectUtils.writeValue(request, ErrorMessage.failed(status.message()));
  }
}
