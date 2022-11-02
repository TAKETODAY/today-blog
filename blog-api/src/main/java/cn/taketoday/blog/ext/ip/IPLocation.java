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

package cn.taketoday.blog.ext.ip;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-03-24 19:27
 */
public final class IPLocation {

  /** 本地网络 */
  public static final String LAN = "LAN";
  /** 未知地址 */
  public static final String UNKNOWN = "UNKNOWN";

  private final String area;
  private final String country;

  public IPLocation(String country, String area) {
    this.country = country;
    switch (area.trim()) {
      case "CZ88.NET":
        this.area = UNKNOWN;
        break;
      case "IANA":
      case "保留地址用于本地回送":
        this.area = LAN;
        break;
      default:
        this.area = area;
        break;
    }
  }

  public IPLocation getCopy() {
    return new IPLocation(country, area);
  }

  public String getCountry() {
    return country;
  }

  public String getArea() {
    return area;
  }

}
