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

package cn.taketoday.blog.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import cn.taketoday.blog.UnauthorizedException;
import cn.taketoday.blog.model.enums.UserStatus;
import cn.taketoday.blog.util.HashUtils;
import cn.taketoday.core.AttributeAccessor;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.jdbc.persistence.Transient;
import cn.taketoday.lang.Nullable;
import lombok.Getter;
import lombok.Setter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-12-06 19:56
 */
@Setter
@Getter
public class User implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  public static final String AttributeKey = "userInfo";

  private static final String DEFAULT_PASSWORD = HashUtils.getEncodedPassword("https://taketoday.cn");

  /** id register time */
  private Long id;
  /** state */
  private UserStatus status;
  /** name */
  private String name;
  /** email */
  private String email;
  /** web site */
  private String site;
  /** type */
  private String type;
  /** passwd */
  @JsonIgnore
  private String password;
  /** avatar */
  private String avatar;
  /** description */
  private String introduce;
  /** back ground **/
  private String background;
  /** email notification */
  private Boolean notification;

  @Transient
  private boolean blogger;

  public User() {

  }

  public User(Long id) {
    this.id = id;
  }

  public User(String email) {
    this.email = email;
  }

  public boolean getNotification() {
    return notification == null || notification;
  }

  @Transient
  public boolean isDefaultPassword() {
    return DEFAULT_PASSWORD.equals(password);
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("id", id)
            .append("status", status)
            .append("name", name)
            .append("email", email)
            .append("site", site)
            .append("type", type)
            .append("password", password)
            .append("avatar", avatar)
            .append("introduce", introduce)
            .append("background", background)
            .append("isBlogger", blogger)
            .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final User user))
      return false;
    return blogger == user.blogger
            && status == user.status
            && Objects.equals(id, user.id)
            && Objects.equals(name, user.name)
            && Objects.equals(site, user.site)
            && Objects.equals(type, user.type)
            && Objects.equals(email, user.email)
            && Objects.equals(avatar, user.avatar)
            && Objects.equals(password, user.password)
            && Objects.equals(introduce, user.introduce)
            && Objects.equals(background, user.background)
            && Objects.equals(notification, user.notification);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, status, name, email, site, type, password, avatar, introduce, background, notification, blogger);
  }

  /**
   * Bind this instance to AttributeAccessor
   *
   * @param accessor session or request
   */
  public void bindTo(AttributeAccessor accessor) {
    accessor.setAttribute(AttributeKey, this);
  }

  // Static

  /**
   * Find login user
   */
  @Nullable
  public static User find(AttributeAccessor accessor) {
    Object attribute = accessor.getAttribute(AttributeKey);
    if (attribute instanceof User user) {
      return user;
    }
    return null;
  }

  public static User obtain(AttributeAccessor accessor) {
    User blogger = find(accessor);
    if (blogger == null) {
      throw new UnauthorizedException();
    }
    return blogger;
  }

  public static void unbind(AttributeAccessor accessor) {
    accessor.removeAttribute(AttributeKey);
  }

  /**
   * Is user logged in
   */
  public static boolean isPresent(@Nullable AttributeAccessor accessor) {
    return accessor != null && find(accessor) != null;
  }

}
