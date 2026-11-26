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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import cn.taketoday.blog.util.BCryptPasswordEncoder.BCryptVersion;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.2 2025/11/11 11:30
 */
class BCryptPasswordEncoderTests {

  @Test
  void test() {
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(BCryptVersion.$2A);
    String encode = encoder.encode("taketoday.cn");
    assertThat(encoder.matches("taketoday.cn", encode)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = { "strong-password", "中文密码-123456", "" })
  void matchesExistingInstallerHashes(String password) {
    String salt = "$2a$04$......................";
    String existing = org.mindrot.jbcrypt.BCrypt.hashpw(password, salt);

    assertThat(BCrypt.hashpw(password, salt)).isEqualTo(existing);
    assertThat(new BCryptPasswordEncoder().matches(password, existing)).isTrue();
  }

  @Test
  void preservesInstallerVersionAndStrength() {
    String password = "strong-password";
    String encoded = BCrypt.hashpw(password, BCrypt.gensalt(12));

    assertThat(encoded).startsWith("$2a$12$");
    assertThat(new BCryptPasswordEncoder().matches(password, encoded)).isTrue();
    assertThat(org.mindrot.jbcrypt.BCrypt.checkpw(password, encoded)).isTrue();
  }

  @Test
  void rejectsMismatchedAndInvalidHashes() {
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

    assertThat(encoder.matches("wrong-password", encoder.encode("strong-password"))).isFalse();
    assertThat(encoder.matches("strong-password", null)).isFalse();
    assertThat(encoder.matches("strong-password", "invalid")).isFalse();
  }

}
