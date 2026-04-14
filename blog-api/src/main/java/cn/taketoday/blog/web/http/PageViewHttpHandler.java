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

import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import cn.taketoday.blog.model.IpLocation;
import cn.taketoday.blog.model.PageView;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.service.IpLocationService;
import cn.taketoday.blog.util.BlogUtils;
import cn.taketoday.blog.util.StringUtils;
import cn.taketoday.blog.web.LoginInfo;
import cn.taketoday.blog.web.interceptor.RequestLimit;
import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.UserAgent;
import eu.bitwalker.useragentutils.Version;
import infra.http.HttpHeaders;
import infra.persistence.EntityManager;
import infra.web.RequestContext;
import infra.web.annotation.POST;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;
import infra.web.util.UriUtils;
import lombok.RequiredArgsConstructor;

/**
 * 页面访问统计处理器
 * <p>
 * 用于记录博客页面的访问信息，包括用户代理、IP地址、地理位置等。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-05-23 17:10
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pv")
class PageViewHttpHandler {

  private final EntityManager entityManager;

  private final IpLocationService ipLocationService;

  @POST
  @RequestLimit(count = 5)
  public void create(@Nullable String referer, RequestContext request, LoginInfo loginInfo) {
    if (!loginInfo.isBloggerLoggedIn()) {

      String url = request.getHeader(HttpHeaders.REFERER);
      if (StringUtils.isEmpty(url)) {
        return;
      }
      URI uri = URI.create(url);
      String host = uri.getHost();
      String path = uri.getPath();

      PageView pageView = new PageView();

      pageView.setHost(host);
      pageView.setPath(path);

      String ua = request.getHeader(HttpHeaders.USER_AGENT);
      UserAgent userAgent = UserAgent.parseUserAgentString(ua);

      String ip = BlogUtils.remoteAddress(request);
      pageView.setIp(ip);
      pageView.setUrl(UriUtils.decode(url, StandardCharsets.UTF_8));
      pageView.setOs(userAgent.getOperatingSystem().getName());
      pageView.setDevice(userAgent.getOperatingSystem().getDeviceType().getName());
      pageView.setReferer(StringUtils.hasText(referer) ? UriUtils.decode(referer, StandardCharsets.UTF_8) : null);
      pageView.setUserAgent(ua);

      IpLocation ipLocation = ipLocationService.lookup(ip);
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

}
