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

package cn.taketoday.blog.model.oauth;

import java.util.Objects;

import cn.taketoday.core.style.ToStringBuilder;
import lombok.Getter;
import lombok.Setter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-10-26 21:49
 */
@Getter
@Setter
public class Oauth {

  private String appId;
  private String appKey;
  private String redirect;
  private String callback;

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("appId", appId)
            .append("appKey", appKey)
            .append("redirect", redirect)
            .append("callback", callback)
            .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final Oauth oauth))
      return false;
    return Objects.equals(appId, oauth.appId)
            && Objects.equals(appKey, oauth.appKey)
            && Objects.equals(redirect, oauth.redirect)
            && Objects.equals(callback, oauth.callback);
  }

  @Override
  public int hashCode() {
    return Objects.hash(appId, appKey, redirect, callback);
  }
}
