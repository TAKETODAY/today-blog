/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.blog.service;

import org.junit.jupiter.api.Test;

import cn.taketoday.blog.model.IpLocation;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.2 2024/11/14 23:00
 */
class IpLocationServiceTests {
  final IpLocationService service = new IpLocationService();

  @Test
  void lookup() {
    System.out.println(service.lookup("8.8.8.8"));
    System.out.println(service.lookup("1.1.1.1"));
    System.out.println(service.lookup("2606:4700:3032::6815:480a"));

    IpLocation location = service.lookup("8.8.8.8");
    assertThat(location).isNotNull();

  }

}