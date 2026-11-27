/*
 * Copyright 2017 - 2025 the original author or authors.
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

package cn.taketoday.blog.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author TODAY 2020/12/22 17:36
 */
public abstract class StreamUtils extends infra.util.StreamUtils {

  public static void transferTo(final InputStream source, final OutputStream out) throws IOException {
    transferTo(source, out, 8 * 1024);
  }

  public static void transferTo(final InputStream source, final OutputStream out, int bufferSize) throws IOException {
    int bytesRead;
    final byte[] buffer = new byte[bufferSize];
    while ((bytesRead = source.read(buffer)) != -1) {
      out.write(buffer, 0, bytesRead);
    }
    out.flush();
  }

}
