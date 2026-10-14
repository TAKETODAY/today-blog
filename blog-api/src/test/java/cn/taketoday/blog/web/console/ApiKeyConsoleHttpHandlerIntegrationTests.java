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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import cn.taketoday.blog.model.ApiKey;
import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.model.enums.UserStatus;
import cn.taketoday.blog.service.ApiKeyService;
import cn.taketoday.blog.service.BloggerService;
import cn.taketoday.blog.web.WebAPITest;
import infra.beans.factory.annotation.Autowired;
import infra.core.ParameterizedTypeReference;
import infra.http.MediaType;
import infra.persistence.EntityManager;
import infra.session.SessionRepository;
import infra.test.web.mock.assertj.MockMvcTester;
import infra.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API Key 管理接口的 HTTP 与数据库集成测试。
 */
@WebAPITest
@Transactional
class ApiKeyConsoleHttpHandlerIntegrationTests {

  @Autowired
  private MockMvcTester mvc;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private SessionRepository sessions;

  @Autowired
  private ApiKeyService service;

  @Autowired
  private BloggerService bloggers;

  private User owner;

  @Test
  void bloggerKeyCanAccessConsoleWithoutSessionPrivileges() {
    Blogger blogger = new Blogger();
    blogger.setEmail(owner.getEmail());
    bloggers.setBlogger(blogger);
    try {
      String value = service.create("blogger", owner).value();
      mvc.get().uri("/api/console/api-keys").header("X-API-Key", value)
              .assertThat().hasStatusOk().bodyJson()
              .convertTo(new ParameterizedTypeReference<List<ApiKey>>() { })
              .satisfies(keys -> assertThat(keys).hasSize(1));
    }
    finally {
      bloggers.setBlogger(null);
    }
  }

  @Test
  void bearerAuthenticatesWithoutChangingExistingSessionAndRevocationTakesEffect() {
    String value = service.create("request identity", owner).value();
    User sessionUser = User.find(sessions.retrieveSession("key"));

    mvc.get().uri("/api/auth").header("Authorization", "Bearer " + value)
            .assertThat().hasStatusOk().bodyJson()
            .extractingPath("$.id").isEqualTo(owner.getId().intValue());
    assertThat(User.find(sessions.retrieveSession("key"))).isSameAs(sessionUser);

    ApiKey key = service.findByUserId(owner.getId()).get(0);
    service.revoke(key.getId(), owner.getId());
    mvc.get().uri("/api/auth").header("Authorization", "Bearer " + value)
            .assertThat().hasStatus(401);
  }

  @Test
  void explicitInvalidKeyCannotFallBackToLoggedInBlogger() {
    mvc.get().uri("/api/console/api-keys").header("X-API-Key", "tbk_invalid")
            .assertThat().hasStatus(401);
    mvc.get().uri("/api/console/api-keys").header("X-API-Key", "")
            .assertThat().hasStatus(401);
  }

  @Test
  void ordinaryUserKeyCannotUseBloggerSessionPrivileges() {
    String value = service.create("ordinary user", owner).value();
    mvc.get().uri("/api/console/api-keys").header("X-API-Key", value)
            .assertThat().hasStatus(404);
    mvc.get().uri("/api/auth").header("X-API-Key", value)
            .assertThat().hasStatusOk().bodyJson()
            .extractingPath("$.email").isEqualTo(owner.getEmail());
  }

  @BeforeEach
  void bindPersistedOwner() {
    User user = new User("api-key-test@example.com");
    user.setName("API Key test");
    user.setStatus(UserStatus.NORMAL);
    entityManager.persist(user);
    owner = entityManager.findUnique(User.class, Map.of("email", user.getEmail()));
    assertThat(owner).isNotNull();
    assertThat(owner.getId()).isNotNull();
    owner.bindTo(sessions.retrieveSession("key"));
  }

  @AfterEach
  void restoreAutoLoginUser() {
    new User().bindTo(sessions.retrieveSession("key"));
  }

  @Test
  void createListAndRevoke() {
    mvc.post().uri("/api/console/api-keys")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"automation\"}")
            .assertThat().hasStatus(201)
            .bodyJson().convertTo(ApiKeyConsoleHttpHandler.CreatedApiKey.class)
            .satisfies(created -> {
              assertThat(created.value()).startsWith("tbk_");
              assertThat(created.apiKey().getId()).isNotNull();
              assertThat(created.apiKey().getUserId()).isEqualTo(owner.getId());
              assertThat(created.apiKey().getKeyHash()).isNull();

              ApiKey stored = entityManager.findById(ApiKey.class, created.apiKey().getId());
              assertThat(stored).isNotNull();
              assertThat(stored.getKeyHash()).hasSize(64).isNotEqualTo(created.value());
              assertThat(service.authenticate(created.value()).getId()).isEqualTo(owner.getId());

              mvc.get().uri("/api/console/api-keys")
                      .assertThat().hasStatusOk()
                      .bodyJson().convertTo(new ParameterizedTypeReference<List<ApiKey>>() { })
                      .satisfies(keys -> {
                        assertThat(keys).hasSize(1);
                        assertThat(keys.get(0).getName()).isEqualTo("automation");
                        assertThat(keys.get(0).getKeyHash()).isNull();
                        assertThat(keys.get(0).getLastUsedAt()).isNotNull();
                      });

              mvc.post().uri("/api/console/api-keys/{id}/revoke", stored.getId())
                      .assertThat().hasStatus(204);
              assertThat(service.authenticate(created.value())).isNull();
              assertThat(entityManager.findById(ApiKey.class, stored.getId()).getRevokedAt()).isNotNull();
            });
  }

  @Test
  void deleteRemovesOwnedKey() {
    ApiKeyService.CreatedApiKey created = service.create("to delete", owner);

    mvc.delete().uri("/api/console/api-keys/{id}", created.apiKey().getId())
            .assertThat().hasStatus(204);
    assertThat(entityManager.findById(ApiKey.class, created.apiKey().getId())).isNull();
  }

  @Test
  void blankNameIsRejected() {
    mvc.post().uri("/api/console/api-keys")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\" \"}")
            .assertThat().hasStatus(400);
    assertThat(service.findByUserId(owner.getId())).isEmpty();
  }

  @Test
  void otherOwnersKeysAreNeitherListedNorRevoked() {
    User other = new User("other-api-key-test@example.com");
    other.setStatus(UserStatus.NORMAL);
    entityManager.persist(other);
    other = entityManager.findUnique(User.class, Map.of("email", other.getEmail()));
    ApiKeyService.CreatedApiKey created = service.create("other owner", other);

    mvc.get().uri("/api/console/api-keys")
            .assertThat().hasStatusOk()
            .bodyJson().convertTo(new ParameterizedTypeReference<List<ApiKey>>() { })
            .satisfies(keys -> assertThat(keys).isEmpty());
    mvc.delete().uri("/api/console/api-keys/{id}", created.apiKey().getId())
            .assertThat().hasStatus(204);
    assertThat(entityManager.findById(ApiKey.class, created.apiKey().getId())).isNotNull();
  }
}
