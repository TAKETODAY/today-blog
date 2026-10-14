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

package cn.taketoday.blog.web.console;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import cn.taketoday.blog.model.ApiKey;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.service.ApiKeyService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * API Key 管理接口测试。
 */
class ApiKeyConsoleHttpHandlerTests {

  private final ApiKeyService service = mock(ApiKeyService.class);

  private final ApiKeyConsoleHttpHandler handler = new ApiKeyConsoleHttpHandler(service);

  private final User owner = new User(42L);

  @Test
  void createReturnsSecretFromService() {
    ApiKey key = new ApiKey();
    ApiKeyConsoleHttpHandler.CreateApiKeyRequest request = new ApiKeyConsoleHttpHandler.CreateApiKeyRequest();
    request.name = "automation";
    when(service.create("automation", owner)).thenReturn(new ApiKeyService.CreatedApiKey(key, "tbk_secret"));

    ApiKeyConsoleHttpHandler.CreatedApiKey response = handler.create(request, owner);

    assertThat(response.apiKey()).isSameAs(key);
    assertThat(response.value()).isEqualTo("tbk_secret");
    verify(service).create("automation", owner);
  }

  @Test
  void listUsesCurrentUserId() {
    List<ApiKey> keys = List.of(new ApiKey());
    when(service.findByUserId(42L)).thenReturn(keys);
    assertThat(handler.list(owner)).isSameAs(keys);
    verify(service).findByUserId(42L);
  }

  @Test
  void deleteUsesCurrentUserId() {
    handler.delete(7L, owner);
    verify(service).deleteById(7L, 42L);
  }

  @Test
  void revokeUsesCurrentUserId() {
    handler.revoke(7L, owner);
    verify(service).revoke(7L, 42L);
  }

  @Test
  void responsesDoNotExposeStoredDigest() throws Exception {
    ApiKey key = new ApiKey();
    key.setName("automation");
    key.setKeyHash("private-digest");
    JsonMapper mapper = JsonMapper.builder().build();

    assertThat(mapper.writeValueAsString(List.of(key)))
            .contains("automation").doesNotContain("keyHash", "private-digest", "tbk_secret");
    assertThat(mapper.writeValueAsString(new ApiKeyConsoleHttpHandler.CreatedApiKey(key, "tbk_secret")))
            .contains("tbk_secret").doesNotContain("keyHash", "private-digest");
  }
}
