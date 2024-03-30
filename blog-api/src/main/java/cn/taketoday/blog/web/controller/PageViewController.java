/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2024 All Rights Reserved.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.blog.web.controller;

import java.util.List;

import cn.taketoday.blog.model.PageView;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.util.BlogUtils;
import cn.taketoday.blog.util.IpSearchers;
import cn.taketoday.blog.util.StringUtils;
import cn.taketoday.blog.web.LoginInfo;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.ip2region.IpLocation;
import cn.taketoday.jdbc.persistence.EntityManager;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.POST;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RestController;
import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.UserAgent;
import eu.bitwalker.useragentutils.Version;
import lombok.RequiredArgsConstructor;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-05-23 17:10
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pv")
public class PageViewController {

  private final EntityManager entityManager;

  @POST
  public void create(String referer, final RequestContext request, LoginInfo loginInfo) {
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
      pageView.setIp(ip)
              .setUrl(url)
              .setOs(userAgent.getOperatingSystem().getName())
              .setDevice(userAgent.getOperatingSystem().getDeviceType().getName())
              .setReferer(referer)
              .setUserAgent(ua);

      IpLocation ipLocation = IpSearchers.find(ip);
      if (ipLocation != null) {
        pageView.setIpCountry(ipLocation.getCountry());
        pageView.setIpProvince(ipLocation.getProvince());
        pageView.setIpCity(ipLocation.getCity());
        pageView.setIpArea(ipLocation.getArea());
        pageView.setIpIsp(ipLocation.getIsp());
      }

      // TODO ip 字段

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

      entityManager.persist(pageView);
    }
  }

  @GET
  @RequiresBlogger
  public List<PageView> listPageViews() {
    return entityManager.find(PageView.class);
  }

}
