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

package cn.taketoday.blog.util;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/2/14 17:32
 */
public abstract class DateFormatter {

  static final List<DateTimeFormatter> formatters = List.of(
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
          DateTimeFormatter.ISO_LOCAL_DATE_TIME,
          DateTimeFormatter.ISO_DATE_TIME
  );

  public static Instant parse(CharSequence text) {
    DateTimeParseException exception = null;
    for (DateTimeFormatter formatter : formatters) {
      try {
        return formatter.parse(text, Instant::from);
      }
      catch (DateTimeParseException e) {
        exception = e;
      }
    }
    if (exception != null) {
      throw exception;
    }
    String abbr;
    if (text.length() > 64) {
      abbr = text.subSequence(0, 64) + "...";
    }
    else {
      abbr = text.toString();
    }
    throw new DateTimeParseException("Text '%s' could not be parsed ".formatted(abbr), text, 0, null);
  }

}
