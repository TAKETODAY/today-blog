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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import cn.taketoday.blog.model.ApiKey;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.model.enums.UserStatus;
import infra.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * API Key 创建、认证和撤销的单元测试。
 */
class ApiKeyServiceTests {

  private final EntityManager entities = mock(EntityManager.class);
  private final UserService users = mock(UserService.class);
  private final ApiKeyService service = new ApiKeyService(entities, users);

  @Test
  void createStoresDigestAndReturnsDistinctSecrets() throws Exception {
    User user = new User(42L);
    ApiKeyService.CreatedApiKey first = service.create("automation", user);
    ApiKeyService.CreatedApiKey second = service.create("automation", user);

    assertThat(first.value()).startsWith("tbk_").isNotEqualTo(second.value());
    assertThat(first.apiKey().getUserId()).isEqualTo(42L);
    assertThat(first.apiKey().getName()).isEqualTo("automation");
    assertThat(first.apiKey().getCreatedAt()).isNotNull();
    assertThat(first.apiKey().getKeyHash()).isEqualTo(digest(first.value()))
            .isNotEqualTo(first.value());
    assertThat(first.apiKey().getLastUsedAt()).isNull();
    assertThat(first.apiKey().revoked()).isFalse();
    verify(entities).persist(first.apiKey());
    verify(entities).persist(second.apiKey());
  }

  @Test
  void listIsRestrictedToOwner() {
    List<ApiKey> keys = List.of(key());
    when(entities.find(ApiKey.class, Map.of("userId", 42L))).thenReturn(keys);

    assertThat(service.findByUserId(42L)).isSameAs(keys);
    verify(entities).find(ApiKey.class, Map.of("userId", 42L));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = { " ", "\t\n" })
  void emptyCredentialsDoNotQueryStorage(String value) {
    assertThat(service.authenticate(value)).isNull();
    verifyNoInteractions(entities, users);
  }

  @Test
  void unknownKeyIsRejected() {
    assertThat(service.authenticate("tbk_unknown")).isNull();
    verifyNoInteractions(users);
    verify(entities, never()).updateById(any(ApiKey.class));
  }

  @Test
  void revokedKeyDoesNotLoadUser() throws Exception {
    ApiKey key = key();
    key.setRevokedAt(Instant.now());
    stubKey(key);

    assertThat(service.authenticate("tbk_secret")).isNull();
    verifyNoInteractions(users);
    verify(entities, never()).updateById(any(ApiKey.class));
  }

  @Test
  void missingUserIsRejected() throws Exception {
    stubKey(key());

    assertThat(service.authenticate("tbk_secret")).isNull();
    verify(entities, never()).updateById(any(ApiKey.class));
  }

  @ParameterizedTest
  @EnumSource(value = UserStatus.class, names = "NORMAL", mode = EnumSource.Mode.EXCLUDE)
  void disabledUserIsRejected(UserStatus status) throws Exception {
    stubKey(key());
    User user = new User(42L);
    user.setStatus(status);
    when(users.getById(42L)).thenReturn(user);

    assertThat(service.authenticate("tbk_secret")).isNull();
    verify(entities, never()).updateById(any(ApiKey.class));
  }

  @Test
  void normalUserAuthenticatesAndUsageIsRecorded() throws Exception {
    ApiKey key = key();
    stubKey(key);
    User user = new User(42L);
    user.setStatus(UserStatus.NORMAL);
    when(users.getById(42L)).thenReturn(user);

    assertThat(service.authenticate("tbk_secret")).isSameAs(user);
    assertThat(key.getLastUsedAt()).isNotNull();
    verify(entities).updateById(key);
  }

  @Test
  void revokedKeyFailsOnNextAuthentication() throws Exception {
    ApiKey key = key();
    stubKey(key);
    when(entities.findById(ApiKey.class, 7L)).thenReturn(key);
    User user = new User(42L);
    user.setStatus(UserStatus.NORMAL);
    when(users.getById(42L)).thenReturn(user);

    assertThat(service.authenticate("tbk_secret")).isSameAs(user);
    service.revoke(7L, 42L);
    assertThat(key.getRevokedAt()).isNotNull();
    assertThat(service.authenticate("tbk_secret")).isNull();
    verify(users, times(1)).getById(42L);
  }

  @Test
  void otherUserCannotRevokeKey() {
    ApiKey key = key();
    when(entities.findById(ApiKey.class, 7L)).thenReturn(key);

    service.revoke(7L, 99L);

    assertThat(key.revoked()).isFalse();
    verify(entities, never()).updateById(any(ApiKey.class));
  }

  @Test
  void repeatedRevocationPreservesTimestamp() {
    ApiKey key = key();
    Instant revokedAt = Instant.parse("2026-01-01T00:00:00Z");
    key.setRevokedAt(revokedAt);
    when(entities.findById(ApiKey.class, 7L)).thenReturn(key);

    service.revoke(7L, 42L);

    assertThat(key.getRevokedAt()).isEqualTo(revokedAt);
    verify(entities, never()).updateById(any(ApiKey.class));
  }

  @Test
  void missingKeyRevocationIsNoOp() {
    service.revoke(7L, 42L);
    verify(entities, never()).updateById(any(ApiKey.class));
  }

  @Test
  void deleteRemovesOwnedKey() {
    ApiKey key = key();
    when(entities.findById(ApiKey.class, 7L)).thenReturn(key);

    service.deleteById(7L, 42L);

    verify(entities).delete(ApiKey.class, 7L);
  }

  @Test
  void otherUserCannotDeleteKey() {
    ApiKey key = key();
    when(entities.findById(ApiKey.class, 7L)).thenReturn(key);

    service.deleteById(7L, 99L);

    verify(entities, never()).delete(ApiKey.class, 7L);
  }

  @Test
  void headersSupportBearerAndExplicitKeyPrecedence() {
    assertThat(ApiKeyService.extractValue("bEaReR  tbk_secret  ", null)).isEqualTo("tbk_secret");
    assertThat(ApiKeyService.extractValue("Bearer tbk_bearer", "tbk_header")).isEqualTo("tbk_header");
    assertThat(ApiKeyService.extractValue("Bearer tbk_bearer", " ")).isEqualTo("tbk_bearer");
    assertThat(ApiKeyService.extractValue("Basic credentials", null)).isNull();
    assertThat(ApiKeyService.extractValue(null, null)).isNull();
  }

  private void stubKey(ApiKey key) throws Exception {
    when(entities.findFirst(ApiKey.class, Map.of("keyHash", digest("tbk_secret")))).thenReturn(key);
  }

  private static ApiKey key() {
    ApiKey key = new ApiKey();
    key.setId(7L);
    key.setUserId(42L);
    return key;
  }

  private static String digest(String value) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(value.getBytes(StandardCharsets.UTF_8)));
  }
}
