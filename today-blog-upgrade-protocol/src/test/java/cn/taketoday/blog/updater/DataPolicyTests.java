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
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataPolicyTests {

  @TempDir
  Path temp;

  @Test
  void missingFileUsesDefaults() throws Exception {
    assertThat(DataPolicy.load(temp.resolve("missing.properties"))).isEqualTo(DataPolicy.defaults());
  }

  @Test
  void parsesConfiguredValues() {
    Properties values = new Properties();
    values.setProperty(DataPolicy.BACKUPS_KEEP, "8");
    values.setProperty(DataPolicy.PACKAGES_KEEP, "2");
    values.setProperty(DataPolicy.LOGS_DAYS, "30");
    values.setProperty(DataPolicy.WARNING_USED_PERCENT, "90");
    values.setProperty(DataPolicy.WARNING_FREE_BYTES, "2048");

    assertThat(DataPolicy.parse(values))
            .isEqualTo(new DataPolicy(8, 2, 30, 90, 2048));
  }

  @Test
  void rejectsInvalidValues() {
    assertThatThrownBy(() -> new DataPolicy(-1, 3, 14, 85, 0))
            .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new DataPolicy(5, 3, 14, 0, 0))
            .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new DataPolicy(5, 3, 14, 101, 0))
            .isInstanceOf(IllegalArgumentException.class);

    Properties values = new Properties();
    values.setProperty(DataPolicy.LOGS_DAYS, "not-a-number");
    assertThatThrownBy(() -> DataPolicy.parse(values))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void storesAndReloadsPolicy() throws Exception {
    Path config = temp.resolve("today-blog/data.properties");
    DataPolicy policy = DataPolicy.defaults()
            .withBackupsKeep(9)
            .withWarningFreeBytes(4096);

    policy.store(config);

    assertThat(Files.isRegularFile(config)).isTrue();
    assertThat(DataPolicy.load(config)).isEqualTo(policy);
  }

}
