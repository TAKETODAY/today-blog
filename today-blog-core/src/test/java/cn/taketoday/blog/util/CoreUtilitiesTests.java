/*
 * Copyright 2017 - 2026 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 公共工具在独立于服务器的类路径上保持原有行为。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/26
 */
class CoreUtilitiesTests {

  @Test
  void providesStringHelpers() {
    assertThat(StringUtils.delHtml("<p>Hello&nbsp; world</p>")).isEqualTo("Helloworld");
    assertThat(StringUtils.getRandomImageName("avatar")).matches("[0-9a-f]{8}avatar\\.png");
    assertThat(StringUtils.isBlank(" ")).isTrue();
    assertThat(HashUtils.getRandomHashString(32)).matches("[A-Za-z0-9]{32}");
  }

  @Test
  void transfersStreams() throws IOException {
    byte[] contents = "公共流工具".getBytes(StandardCharsets.UTF_8);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    StreamUtils.transferTo(new ByteArrayInputStream(contents), output, 3);

    assertThat(output.toByteArray()).isEqualTo(contents);
  }

  @Test
  void handlesSuccessfulResults() {
    AtomicBoolean called = new AtomicBoolean();
    ReturnValue<String> result = ReturnValue.ok("value");

    result.ifSuccess(() -> called.set(true));

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.get()).isEqualTo("value");
    assertThat(result.map(String::length).get()).isEqualTo(5);
    assertThat(called).isTrue();
  }

  @Test
  void handlesFailedResults() {
    IOException failure = new IOException("failed");
    ReturnValue<String> result = ReturnValue.failed(failure);

    assertThat(result.isSuccess()).isFalse();
    result.ifFailed(error -> assertThat(error).isSameAs(failure));
    assertThatThrownBy(() -> result.onFailedThrow(error -> new IllegalStateException(error)))
            .isInstanceOf(IllegalStateException.class).hasCause(failure);
  }

  @Test
  void handlesOperationStatus() {
    AtomicBoolean called = new AtomicBoolean();

    assertThat(Status.forStatus(true)).isEqualTo(Status.ok());
    assertThat(Status.failed().success).isFalse();
    Status.ok().onSuccess(() -> called.set(true));
    assertThat(called).isTrue();
    assertThatThrownBy(() -> Status.failed().onFailedThrow(() -> new IOException("failed")))
            .isInstanceOf(IOException.class).hasMessage("failed");
  }

}
