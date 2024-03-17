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

import java.time.LocalDateTime;

import cn.taketoday.jdbc.persistence.Id;
import cn.taketoday.jdbc.persistence.Table;
import lombok.Data;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-05-23 09:20
 */
@Data
@Table("t_page_view")
public class PageView {

  @Id
  private Long id;

  private String url;

  private String user;

  private String os;
  private String device;

  private String referer;
  private String userAgent;

  private String browser;
  private String browserVersion;

  /**
   * IP 地址的位置
   */
  private String ip;

  private String ipCountry;
  private String ipProvince;
  private String ipCity;
  private String ipArea;
  private String ipIsp;

  private LocalDateTime createAt;

}
