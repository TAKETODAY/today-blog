/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.blog.config;

import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.auth.DefaultCredentials;

import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.context.properties.NestedConfigurationProperty;
import cn.taketoday.stereotype.Component;

/**
 * Config for OSS
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-03-16 17:59
 */
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssConfig {

  /**
   * 启用 OSS
   */
  public boolean enabled;

  public String bucket;

  public String endpoint;

  public String accessKeyId;

  public String securityToken;

  public String secretAccessKey;

  @NestedConfigurationProperty
  public ClientConfiguration client;

  public OSSClient newClient() {
    var credentials = new DefaultCredentials(accessKeyId, secretAccessKey, securityToken);
    var provider = new DefaultCredentialProvider(credentials);
    return new OSSClient(endpoint, provider, client);
  }

}
