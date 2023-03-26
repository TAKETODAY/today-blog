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

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import cn.taketoday.blog.model.enums.LoggingType;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.jdbc.persistence.Id;
import cn.taketoday.jdbc.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Table("logging")
public class Operation implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  @Id
  private Long id;

  /**
   * 标题
   */
  private String title;
  /**
   * 内容
   */
  private String content;

  /**
   * 产生日志的ip
   */
  private String ip;

  /**
   * IP 地址的位置
   */
  private String ipCountry;
  private String ipProvince;
  private String ipCity;
  private String ipArea;
  private String ipIsp;

  // operation user
  private String user;

  private LoggingType type;

  private LocalDateTime invokeAt;
  private LocalDateTime createAt;

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("id", id)
            .append("title", title)
            .append("content", content)
            .append("ip", ip)
            .append("ipCountry", ipCountry)
            .append("ipProvince", ipProvince)
            .append("ipCity", ipCity)
            .append("ipArea", ipArea)
            .append("ipIsp", ipIsp)
            .append("user", user)
            .append("type", type)
            .append("invokeAt", invokeAt)
            .append("createAt", createAt)
            .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Operation operation))
      return false;
    return Objects.equals(id, operation.id)
            && type == operation.type
            && Objects.equals(title, operation.title)
            && Objects.equals(content, operation.content)
            && Objects.equals(ip, operation.ip)
            && Objects.equals(ipCountry, operation.ipCountry)
            && Objects.equals(ipProvince, operation.ipProvince)
            && Objects.equals(ipCity, operation.ipCity)
            && Objects.equals(ipArea, operation.ipArea)
            && Objects.equals(ipIsp, operation.ipIsp)
            && Objects.equals(user, operation.user)
            && Objects.equals(createAt, operation.createAt)
            && Objects.equals(invokeAt, operation.invokeAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, title, content, ip, ipCountry, ipProvince, ipCity,
            ipArea, ipIsp, user, type, invokeAt, createAt);
  }

}
