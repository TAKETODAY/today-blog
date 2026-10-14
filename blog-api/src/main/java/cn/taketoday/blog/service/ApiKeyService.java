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

package cn.taketoday.blog.service;

import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import cn.taketoday.blog.model.ApiKey;
import cn.taketoday.blog.model.User;
import infra.persistence.EntityManager;
import infra.stereotype.Service;
import infra.transaction.annotation.Transactional;

/**
 * API Key 创建、验证和撤销服务。
 */
@Service
public class ApiKeyService {

  private static final String PREFIX = "tbk_";

  private static final SecureRandom RANDOM = new SecureRandom();

  private final EntityManager entityManager;

  private final UserService userService;

  public ApiKeyService(EntityManager entityManager, UserService userService) {
    this.entityManager = entityManager;
    this.userService = userService;
  }

  @Transactional
  public CreatedApiKey create(String name, User user) {
    String value = PREFIX + UUID.randomUUID().toString().replace("-", "")
            + Long.toUnsignedString(RANDOM.nextLong(), 36);

    ApiKey apiKey = new ApiKey();
    apiKey.setUserId(user.getId());
    apiKey.setName(name);
    apiKey.setKeyHash(hash(value));
    apiKey.setCreatedAt(Instant.now());
    entityManager.persist(apiKey);
    return new CreatedApiKey(apiKey, value);
  }

  public List<ApiKey> findByUserId(long userId) {
    return entityManager.find(ApiKey.class, Map.of("userId", userId));
  }

  public @Nullable User authenticate(@Nullable String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    ApiKey apiKey = entityManager.findFirst(ApiKey.class, Map.of("keyHash", hash(value)));
    if (apiKey == null || apiKey.revoked()) {
      return null;
    }

    User user = userService.getById(apiKey.getUserId());
    if (user == null || user.getStatus() != cn.taketoday.blog.model.enums.UserStatus.NORMAL) {
      return null;
    }
    apiKey.setLastUsedAt(Instant.now());
    entityManager.updateById(apiKey);
    return user;
  }

  @Transactional
  public void revoke(long id, long userId) {
    ApiKey apiKey = entityManager.findById(ApiKey.class, id);
    if (apiKey != null && Objects.equals(apiKey.getUserId(), userId) && !apiKey.revoked()) {
      apiKey.setRevokedAt(Instant.now());
      entityManager.updateById(apiKey);
    }
  }

  @Transactional
  public void deleteById(long id, long userId) {
    ApiKey apiKey = entityManager.findById(ApiKey.class, id);
    if (apiKey != null && Objects.equals(apiKey.getUserId(), userId)) {
      entityManager.delete(ApiKey.class, id);
    }
  }

  public static @Nullable String extractValue(@Nullable String authorization, @Nullable String apiKey) {
    if (apiKey != null && !apiKey.isBlank()) {
      return apiKey;
    }
    if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
      return authorization.substring(7).trim();
    }
    return null;
  }

  private static String hash(String value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
              .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
    catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is unavailable", ex);
    }
  }

  public record CreatedApiKey(ApiKey apiKey, String value) {
  }
}
