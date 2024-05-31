/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

import java.io.File;

import cn.taketoday.blog.model.Attachment;
import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.stereotype.Component;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/12 18:59
 */
@Component
@ConfigurationProperties(prefix = "attachment")
public class AttachmentConfig {

  private String outsideResource;

  /**
   * CDN
   */
  private String cdnHostPrefix;

  public void setCdnHostPrefix(String cdnHostPrefix) {
    this.cdnHostPrefix = cdnHostPrefix;
  }

  public void setOutsideResource(String outsideResource) {
    this.outsideResource = outsideResource;
  }

  public String getCdnHostPrefix() {
    return cdnHostPrefix;
  }

  public String getOutsideResource() {
    return outsideResource;
  }

  public String getRemoteURL(String uri) {
    return cdnHostPrefix + uri;
  }

  public File getLocalFile(Attachment attachment) {
    return new File(outsideResource, attachment.getLocation());
  }

  public File getLocalFile(String location) {
    return new File(outsideResource, location);
  }

  public String getUploadDir() {
    return outsideResource + "/upload/";
  }

}
