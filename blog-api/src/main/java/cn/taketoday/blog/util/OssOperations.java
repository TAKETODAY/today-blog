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

package cn.taketoday.blog.util;

import com.aliyun.oss.OSSClient;

import java.io.File;

import cn.taketoday.blog.config.OssConfig;
import cn.taketoday.stereotype.Singleton;

/**
 * @author TODAY 2021/3/31 21:37
 * @since 3.0
 */
@Singleton
public class OssOperations {

  private final OssConfig config;
  private volatile OSSClient ossClient;

  public OssOperations(OssConfig config) {
    this.config = config;
  }

  public boolean isOssEnabled() {
    return config.enabled;
  }

  public void removeFile(String filePath) {
    obtainClient().deleteObject(config.bucket, getLocation(filePath));
  }

  public void uploadFile(String location, File destFile) {
    obtainClient().putObject(config.bucket, getLocation(location), destFile);
  }

  public boolean exists(String location) {
    return obtainClient().doesObjectExist(config.bucket, getLocation(location));
  }

  OSSClient obtainClient() {
    OSSClient ossClient = this.ossClient;
    if (ossClient == null) {
      synchronized(this) {
        ossClient = this.ossClient;
        if (ossClient == null) {
          ossClient = config.newClient();
          this.ossClient = ossClient;
        }
      }
    }
    return ossClient;
  }

  protected String getLocation(String key) {
    if (key.startsWith("/")) {
      key = key.substring(1);
    }
    return key;
  }

}
