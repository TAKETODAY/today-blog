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

package cn.taketoday.blog;

import cn.taketoday.context.event.EventListener;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.InfraApplication;
import cn.taketoday.framework.context.event.ApplicationFailedEvent;
import cn.taketoday.orm.mybatis.annotation.MapperScan;
import cn.taketoday.session.config.EnableWebSession;
import lombok.CustomLog;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-01-12 21:01
 */
@CustomLog
@EnableWebSession
@InfraApplication
@MapperScan("cn.taketoday.blog.repository")
public class BlogApplication {

  public static void main(String[] args) {
    Application.run(BlogApplication.class, args);
    log.info("----------------Application Started------------------");
  }

  @EventListener
  public void appFailed(ApplicationFailedEvent event) {
    log.info("----------------Application Started Failed ------------------", event.getException());
  }

}
