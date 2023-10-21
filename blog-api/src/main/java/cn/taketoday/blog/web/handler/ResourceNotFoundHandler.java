/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.blog.web.handler;

import cn.taketoday.blog.ErrorMessage;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Component;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.NotFoundHandler;
import io.prometheus.client.Counter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2020-01-01 22:29
 */
@Component
public class ResourceNotFoundHandler extends NotFoundHandler {

  static final Counter requests = Counter.build()
          .name("requests_not_found")
          .labelNames("uri")
          .help("Total Not Found requests.").register();

  @Nullable
  @Override
  public Object handleRequest(RequestContext request) {
    requests.labels(request.getRequestURI())
            .inc();

    request.setStatus(HttpStatus.NOT_FOUND);

    logNotFound(request);
    return ErrorMessage.failed("资源找不到");
  }

}
