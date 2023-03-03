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

package cn.taketoday.blog.util;

import java.io.IOException;
import java.io.Writer;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.WritableResource;

/**
 * @author TODAY 2021/3/28 19:00
 */
public class RemoveAscii {

  public static void main(String[] args) throws IOException {
    ClassPathResource resource = new ClassPathResource("text");
    String content = StreamUtils.copyToString(resource.getInputStream());
    StringBuilder builder = new StringBuilder(content.length());

    char[] chars = content.toCharArray();

    for (char aChar : chars) {
      if (aChar >= 128 || aChar == ',' || aChar == '\r' || aChar == '\n') {
        builder.append(aChar);
      }
    }

    Resource originalResource = resource.getOriginalResource();

    if (originalResource instanceof WritableResource writableResource) {
      Writer writer = writableResource.getWriter();
      writer.write(builder.toString());

      writer.flush();
      writer.close();
    }
    System.err.println(builder);

  }
}
