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

package cn.taketoday.blog.web.controller;

import cn.taketoday.context.annotation.Profile;
import cn.taketoday.session.WebSession;
import cn.taketoday.web.annotation.CookieValue;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.GetMapping;
import cn.taketoday.web.annotation.RequestHeader;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.annotation.SessionAttribute;
import lombok.Data;

@Profile("dev")
@RestController
@RequestMapping("/testing")
public class TestController {

  @GetMapping("/empty")
  public void empty() {

  }

  @GetMapping("/hello")
  public String hello() {
    return "hello";
  }

  @Data
  static class RequestParams {
    @RequestHeader("X-Header")
    private String header;

    @RequestHeader("Accept-Encoding")
    private String acceptEncoding;

    private String name;

    @SessionAttribute("session")
    private String session;

    @CookieValue
    private String cookie;

    @CookieValue("Authorization")
    private String authorization;

  }

  @GET("/binder")
  public RequestParams binder(RequestParams params, WebSession session) {
    session.setAttribute("session", params.toString());
    return params;
  }

}
