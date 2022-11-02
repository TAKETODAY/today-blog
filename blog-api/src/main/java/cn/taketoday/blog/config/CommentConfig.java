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

package cn.taketoday.blog.config;

import java.io.Serializable;

import cn.taketoday.blog.ConfigBinding;
import cn.taketoday.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-04-10 18:14
 */
@Setter
@Getter
@Component
@ConfigBinding("comment.")
public class CommentConfig implements Serializable {

  @ConfigBinding("send.mail")
  private boolean sendMail = true;

  @ConfigBinding("list.size")
  private int listSize = 10;

  @ConfigBinding("content.length")
  private int contentLength = 10240;

  private boolean check;

  private String placeholder;

}
