/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import java.util.List;

import cn.taketoday.blog.ext.ip.IPSeeker;
import cn.taketoday.blog.model.PageView;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.service.PageViewService;
import cn.taketoday.blog.utils.BlogUtils;
import cn.taketoday.blog.utils.StringUtils;
import cn.taketoday.blog.web.LoginInfo;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.POST;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RestController;
import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.UserAgent;
import eu.bitwalker.useragentutils.Version;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-05-23 17:10
 */
@RestController
@RequestMapping("/api/pv")
public class PageViewController {

  private final PageViewService pageViewService;

  public PageViewController(final PageViewService pageViewService) {
    this.pageViewService = pageViewService;
  }

  @POST
  public void pv(String referer, final RequestContext request, LoginInfo loginInfo) {
    if (!loginInfo.isBloggerLoggedIn()) {
      HttpHeaders requestHeaders = request.requestHeaders();

      String url = requestHeaders.getFirst(HttpHeaders.REFERER);
      if (StringUtils.isEmpty(url)) {
        return;
      }

      PageView pageView = new PageView();

      String ua = requestHeaders.getFirst(HttpHeaders.USER_AGENT);
      UserAgent userAgent = UserAgent.parseUserAgentString(ua);

      String ip = BlogUtils.remoteAddress(request);
      pageView.setIp(ip + ":" + new IPSeeker().getAddress(ip))
              .setUrl(url)
              .setOs(userAgent.getOperatingSystem().getName())
              .setDevice(userAgent.getOperatingSystem().getDeviceType().getName())
              .setReferer(referer)
              .setUserAgent(ua);

      Browser browser = userAgent.getBrowser();
      if (browser != null) {
        pageView.setBrowser(browser.getName());
        Version version = browser.getVersion(ua);
        if (version != null) {
          pageView.setBrowserVersion(version.getVersion());
        }
      }

      User loginUser = loginInfo.getLoginUser();
      if (loginUser != null) {
        pageView.setUser(loginUser.getName() + ":" + loginUser.getEmail());
      }

      pageViewService.save(pageView);
    }
  }

  @GET
  @RequiresBlogger
  public List<PageView> pv() {
    return pageViewService.getAll();
  }

}
