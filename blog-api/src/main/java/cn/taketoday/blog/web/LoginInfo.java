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

import org.jspecify.annotations.Nullable;

import cn.taketoday.blog.UnauthorizedException;
import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.model.User;
import lombok.Data;

/**
 * 登录信息封装类，用于存储当前会话中的用户和博主信息。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/11 08:05
 */
@Data
public class LoginInfo {

  private @Nullable User loginUser;

  private @Nullable Blogger blogger;

  public boolean isLoggedIn() {
    return loginUser != null;
  }

  public boolean isBloggerLoggedIn() {
    return blogger != null;
  }

  public long getLoginUserId() {
    if (loginUser == null) {
      throw new UnauthorizedException();
    }
    return loginUser.getId();
  }

}
