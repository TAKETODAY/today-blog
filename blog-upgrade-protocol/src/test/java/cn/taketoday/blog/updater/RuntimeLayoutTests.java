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

package cn.taketoday.blog.updater;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuntimeLayoutTests {

  @Test
  void usesTheSuppliedDefaultWhenTheEnvironmentRootIsMissingOrBlank() {
    Path defaultRoot = Path.of("/srv/development-blog");

    assertThat(RuntimeLayout.fromEnvironment(Map.of(), defaultRoot).data())
            .isEqualTo(defaultRoot.resolve("data"));
    assertThat(RuntimeLayout.fromEnvironment(Map.of(RuntimeLayout.ROOT_ENVIRONMENT, "  "), defaultRoot).data())
            .isEqualTo(defaultRoot.resolve("data"));
  }

  @Test
  void normalizesTheEnvironmentRootInsteadOfUsingTheSuppliedDefault() {
    RuntimeLayout layout = RuntimeLayout.fromEnvironment(
            Map.of(RuntimeLayout.ROOT_ENVIRONMENT, "  /srv/old/../blog  "), Path.of("/srv/development-blog"));

    assertThat(layout.data()).isEqualTo(Path.of("/srv/blog/data"));
  }

  @Test
  void defaultsToSharedDataRoot() {
    RuntimeLayout layout = RuntimeLayout.fromEnvironment(Map.of());
    assertThat(layout.root()).isEqualTo(Path.of("/var/lib/today-blog"));
    assertThat(layout.data()).isEqualTo(Path.of("/var/lib/today-blog/data"));
    assertThat(layout.backups()).isEqualTo(Path.of("/var/lib/today-blog/backups/db"));
    assertThat(layout.upgrade()).isEqualTo(Path.of("/var/lib/today-blog/upgrade"));
    assertThat(layout.packages()).isEqualTo(Path.of("/var/lib/today-blog/upgrade/packages"));
    assertThat(layout.logs()).isEqualTo(Path.of("/var/lib/today-blog/logs"));
    assertThat(layout.tmp()).isEqualTo(Path.of("/var/lib/today-blog/tmp"));
  }

  @Test
  void readsEnvironmentOverride() {
    RuntimeLayout layout = RuntimeLayout.fromEnvironment(Map.of(RuntimeLayout.ROOT_ENVIRONMENT, "/srv/blog/"));
    assertThat(layout.root()).isEqualTo(Path.of("/srv/blog"));
    assertThat(layout.backups()).isEqualTo(Path.of("/srv/blog/backups/db"));
  }

  @Test
  void rejectsRelativeRoot() {
    assertThatThrownBy(() -> new RuntimeLayout(Path.of("relative")))
            .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> RuntimeLayout.fromEnvironment(Map.of(RuntimeLayout.ROOT_ENVIRONMENT, "relative")))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void exposesRelativePaths() {
    RuntimeLayout layout = new RuntimeLayout(Path.of("/srv/blog"));
    assertThat(layout.relative(Path.of("/srv/blog/backups/db/1"))).isEqualTo("backups/db/1");
    assertThat(layout.relative(Path.of("/srv/blog"))).isEqualTo(".");
  }

}
