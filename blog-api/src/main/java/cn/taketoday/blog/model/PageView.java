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
package cn.taketoday.blog.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import cn.taketoday.core.style.ToStringBuilder;
import lombok.Getter;
import lombok.Setter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-05-23 09:20
 */
@Setter
@Getter
public class PageView implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private Long id;

  private String ip;

  private String url;

  private String user;

  private String os;
  private String device;

  private String referer;
  private String userAgent;

  private String browser;
  private String browserVersion;

  private LocalDateTime createAt;

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final PageView pageView))
      return false;
    return Objects.equals(id, pageView.id)
            && Objects.equals(ip, pageView.ip)
            && Objects.equals(url, pageView.url)
            && Objects.equals(user, pageView.user)
            && Objects.equals(os, pageView.os)
            && Objects.equals(device, pageView.device)
            && Objects.equals(referer, pageView.referer)
            && Objects.equals(userAgent, pageView.userAgent)
            && Objects.equals(browser, pageView.browser)
            && Objects.equals(browserVersion, pageView.browserVersion)
            && Objects.equals(createAt, pageView.createAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, ip, url, user, os, device, referer, userAgent, browser, browserVersion, createAt);
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("id", id)
            .append("ip", ip)
            .append("url", url)
            .append("user", user)
            .append("os", os)
            .append("device", device)
            .append("referer", referer)
            .append("userAgent", userAgent)
            .append("browser", browser)
            .append("browserVersion", browserVersion)
            .append("createAt", createAt)
            .toString();
  }

}
