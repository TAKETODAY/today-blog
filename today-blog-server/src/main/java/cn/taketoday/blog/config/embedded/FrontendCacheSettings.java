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

package cn.taketoday.blog.config.embedded;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.blog.problem.CommonProblems;
import cn.taketoday.blog.problem.ProblemException;
import cn.taketoday.blog.util.ObjectUtils;
import infra.core.env.Environment;
import infra.core.env.PropertySource;
import infra.jdbc.core.JdbcOperations;
import tools.jackson.core.type.TypeReference;

/**
 * 保存内嵌源站的缓存预设，并向静态资源路由提供已提交的策略快照。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/26
 */
public final class FrontendCacheSettings extends PropertySource<JdbcOperations> {

  private static final String PREFIX = "frontend.cache.";

  private static final String NO_CACHE = "no-cache";

  private static final String ONE_DAY = "public, max-age=86400";

  private static final String ONE_YEAR_IMMUTABLE = "public, max-age=31536000, immutable";

  private static final TypeReference<Map<String, String>> STORED_PRESETS_TYPE = new TypeReference<>() {
  };

  private static final Map<String, String> DEFAULT_HEADERS = Map.of(
          "console.index", NO_CACHE, "console.assets", ONE_DAY, "console.hashed-assets", ONE_YEAR_IMMUTABLE,
          "desktop.index", NO_CACHE, "desktop.assets", ONE_DAY, "desktop.hashed-assets", ONE_YEAR_IMMUTABLE,
          "upload", NO_CACHE);

  private static final Map<String, String> PRESET_HEADERS = Map.of(
          "no-store", "no-store", "no-cache", NO_CACHE, "5m", "public, max-age=300",
          "1h", "public, max-age=3600", "1d", ONE_DAY, "1y", "public, max-age=31536000");

  private final Environment environment;

  private volatile Snapshot snapshot;

  FrontendCacheSettings(Environment environment, JdbcOperations jdbc) {
    super("frontendCacheSettings", jdbc);
    this.environment = environment;
    String stored = jdbc.queryForObject("select policies from t_frontend_cache where id = 1", String.class);
    this.snapshot = resolve(ObjectUtils.getSharedMapper().readValue(stored, STORED_PRESETS_TYPE));
  }

  @Override
  public @Nullable String getProperty(String name) {
    return snapshot.headers().get(name);
  }

  public synchronized View settings() {
    var effective = new HashMap<String, String>();
    DEFAULT_HEADERS.forEach((key, fallback) -> effective.put(key, environment.getProperty(PREFIX + key, fallback)));
    return new View(true, snapshot.presets(), effective);
  }

  public synchronized View update(Map<String, String> presets) {
    Snapshot next = resolve(presets);
    String serialized = ObjectUtils.getSharedMapper().writeValueAsString(next.presets());
    source.update("update t_frontend_cache set policies = ? where id = 1", serialized);
    snapshot = next;
    return settings();
  }

  private static Snapshot resolve(Map<String, String> presets) {
    var headers = new HashMap<String, String>();
    var overrides = new HashMap<String, String>();
    presets.forEach((key, preset) -> {
      if (!DEFAULT_HEADERS.containsKey(key) || preset == null) {
        throw invalidPreset();
      }
      if ("inherit".equals(preset)) {
        return;
      }
      String header = PRESET_HEADERS.get(preset);
      if (header == null) {
        throw invalidPreset();
      }
      if ("1y".equals(preset) && key.endsWith(".hashed-assets")) {
        header = ONE_YEAR_IMMUTABLE;
      }
      headers.put(PREFIX + key, header);
      overrides.put(key, preset);
    });
    return new Snapshot(Map.copyOf(overrides), Map.copyOf(headers));
  }

  private static ProblemException invalidPreset() {
    return ProblemException.of(CommonProblems.INVALID_REQUEST, "frontend.cache.preset.invalid");
  }

  private record Snapshot(Map<String, String> presets, Map<String, String> headers) {
  }

  public record View(boolean embedded, Map<String, String> presets, Map<String, String> effective) {
  }

}
