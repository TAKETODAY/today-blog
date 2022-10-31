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

package cn.taketoday.blog.utils;

import org.junit.Test;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
@since 
 * 2020-07-12 10:22
 */
public class MD5Test {

  @Test
  public void testMD5() {

    MD5 md5 = new MD5();
    MD5 md52 = new MD5();

    final String md5Str2 = md5.getMD5Str("666");
    final String md5Str = md5.getMD5Str(md5Str2);
    final String md5Strs = md52.getMD5Str(md5Str2);

    System.err.println(md5Str);
    System.err.println(md5Strs);
  }

}
