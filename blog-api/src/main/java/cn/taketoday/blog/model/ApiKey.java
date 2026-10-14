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

package cn.taketoday.blog.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

import infra.persistence.annotation.GeneratedId;
import infra.persistence.annotation.Column;
import infra.persistence.annotation.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * API Key 持久化实体。密钥原文只在创建响应中返回，数据库仅保存摘要。
 */
@Getter
@Setter
@Table("t_api_key")
public class ApiKey {

  @GeneratedId
  private Long id;

  @Column("user_id")
  private Long userId;

  private String name;

  @JsonIgnore
  @Column("key_hash")
  private String keyHash;

  @Column("created_at")
  private Instant createdAt;

  @Column("last_used_at")
  private Instant lastUsedAt;

  @Column("revoked_at")
  private Instant revokedAt;

  public boolean revoked() {
    return revokedAt != null;
  }
}
