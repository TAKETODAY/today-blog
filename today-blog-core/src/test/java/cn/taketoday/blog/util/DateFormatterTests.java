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

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.2 2025/1/1 23:12
 */
class DateFormatterTests {

  @Test
  void parse() {
    Instant expected = LocalDateTime.of(2025, 1, 1, 22, 41, 7).atZone(ZoneId.systemDefault()).toInstant();

    assertThat(DateFormatter.parse("2025-01-01 22:41:07")).isEqualTo(expected);
    assertThat(DateFormatter.parse("2025-01-01T22:41:07")).isEqualTo(expected);
  }

  @Test
  void parsesExplicitTimeZone() {
    assertThat(DateFormatter.parse("2025-01-01T22:41:07+08:00"))
            .isEqualTo(Instant.parse("2025-01-01T14:41:07Z"));
  }

  @Test
  void rejectsInvalidDates() {
    assertThatThrownBy(() -> DateFormatter.parse("not-a-date"))
            .isInstanceOf(java.time.format.DateTimeParseException.class);
  }

}
