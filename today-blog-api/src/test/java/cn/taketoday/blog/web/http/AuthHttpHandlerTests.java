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

package cn.taketoday.blog.web.http;

import org.junit.jupiter.api.Test;

import cn.taketoday.blog.model.User;
import cn.taketoday.blog.web.LoginInfo;
import cn.taketoday.blog.web.WebAPITest;
import infra.beans.factory.annotation.Autowired;
import infra.http.HttpStatus;
import infra.test.web.mock.assertj.MockMvcTester;

@WebAPITest
class AuthHttpHandlerTests {

  @Autowired
  private MockMvcTester mvc;

  @Test
  void anonymousSessionProbeReturnsNoContent() {
    mvc.get().uri("/api/auth")
            .requestAttr(LoginInfo.class.getName(), new LoginInfo())
            .assertThat().hasStatus(HttpStatus.NO_CONTENT).body().isEmpty();
  }

  @Test
  void authenticatedSessionProbeReturnsUser() {
    User user = new User(42L);
    user.setName("tester");
    LoginInfo identity = new LoginInfo();
    identity.setLoginUser(user);

    mvc.get().uri("/api/auth")
            .requestAttr(LoginInfo.class.getName(), identity)
            .assertThat().hasStatusOk().bodyJson()
            .extractingPath("$.name").isEqualTo("tester");
  }

  @Test
  void anonymousProfileUpdateStillRequiresAuthentication() {
    mvc.put().uri("/api/auth")
            .requestAttr(LoginInfo.class.getName(), new LoginInfo())
            .assertThat().hasStatus(HttpStatus.UNAUTHORIZED);
  }
}
