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
package cn.taketoday.blog.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import cn.taketoday.blog.model.enums.PostStatus;
import cn.taketoday.core.style.ToStringBuilder;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY
 * @since 2019-03-27 10:15
 */
@Setter
@Getter
public class Post implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  /** release time stamp */
  protected long id;

  protected String cover;
  protected String title;

  protected int pv;

  // private boolean keepNavigation;
  protected PostStatus status;

  protected String summary;
  /** html content **/
  protected String content;

  /** markdown Content */
  protected String markdown;

  protected long lastModify;

  /** 需要输入密码才能访问该页面 */
  private String password;

  //    public boolean isKeepNavigation() {
  //        return keepNavigation;
  //    }

  public boolean needPassword() {
    return password != null;
  }

  @JsonIgnore
  public String getUrl() {
    return null;
  }

  public void resetPassword() {
    this.password = null;
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("id", id)
            .append("cover", cover)
            .append("title", title)
            .append("pv", pv)
            .append("status", status)
            .append("summary", summary)
            .append("content", content)
            .append("markdown", markdown)
            .append("lastModify", lastModify)
            .append("password", password)
            .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final Post post))
      return false;
    return id == post.id
            && pv == post.pv
            && status == post.status
            && lastModify == post.lastModify
            && Objects.equals(cover, post.cover)
            && Objects.equals(title, post.title)
            && Objects.equals(summary, post.summary)
            && Objects.equals(content, post.content)
            && Objects.equals(markdown, post.markdown)
            && Objects.equals(password, post.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, cover, title, pv, status, summary, content, markdown, lastModify, password);
  }

}
