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

package cn.taketoday.blog.web;

import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.model.User;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.session.InMemorySessionRepository;
import infra.session.Session;
import infra.session.SessionEventDispatcher;
import infra.session.SessionIdGenerator;
import infra.session.SessionIdResolver;
import infra.session.SessionListener;
import infra.session.SessionRepository;
import infra.stereotype.Component;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.2 2026/3/7 22:06
 */
@Configuration
public class SessionConfig {

  @Bean
  static SessionIdResolver sessionIdResolver() {
    return SessionIdResolver.authenticationInfo();
  }

  @Component
  static SessionListener authSessionListener() {
    return new SessionListener() {

      @Override
      public void sessionCreated(Session session) {
        new Blogger().bindTo(session);
        new User().bindTo(session);
      }
    };
  }

  @Bean
  public SessionRepository sessionRepository(SessionEventDispatcher eventDispatcher, SessionIdGenerator idGenerator) {
    return new InMemorySessionRepository(eventDispatcher, idGenerator) {

      @Override
      public Session retrieveSession(String id) {
        Session session = super.retrieveSession(id);
        if (session == null) {
          session = createSession(id);
          session.start();
          session.save();
        }
        return session;
      }
    };
  }

}
