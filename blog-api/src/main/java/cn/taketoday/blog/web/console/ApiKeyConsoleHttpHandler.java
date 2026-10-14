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

import java.util.List;

import cn.taketoday.blog.model.ApiKey;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.service.ApiKeyService;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import infra.http.HttpStatus;
import infra.web.annotation.DELETE;
import infra.web.annotation.GET;
import infra.web.annotation.POST;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.ResponseStatus;
import infra.web.annotation.RestController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

/**
 * 博主 API Key 管理接口。
 */
@RequiresBlogger
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/console/api-keys")
class ApiKeyConsoleHttpHandler {

  private final ApiKeyService apiKeyService;

  @GET
  public List<ApiKey> list(User user) {
    return apiKeyService.findByUserId(user.getId());
  }

  @POST
  @ResponseStatus(HttpStatus.CREATED)
  public CreatedApiKey create(@Valid @RequestBody CreateApiKeyRequest request, User user) {
    ApiKeyService.CreatedApiKey created = apiKeyService.create(request.name, user);
    return new CreatedApiKey(created.apiKey(), created.value());
  }

  @DELETE("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revoke(@PathVariable long id, User user) {
    apiKeyService.revoke(id, user.getId());
  }

  static class CreateApiKeyRequest {
    @NotBlank
    public String name;
  }

  record CreatedApiKey(ApiKey apiKey, String value) {
  }
}
