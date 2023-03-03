/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.blog.model.enums;

import java.io.File;

import cn.taketoday.blog.model.Attachment;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.MediaTypeFactory;
import cn.taketoday.lang.Enumerable;
import cn.taketoday.web.multipart.MultipartFile;

/**
 * 附件类型
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2020/12/22 17:33
 */
public enum AttachmentType implements Enumerable<String> {

  IMAGE,
  AUDIO,
  VIDEO,
  TEXT,
  OTHER;

  static final MediaType TEXT_MediaType = new MediaType("text");
  static final MediaType IMAGE_MediaType = new MediaType("image");
  static final MediaType VIDEO_MediaType = new MediaType("video");
  static final MediaType AUDIO_MediaType = new MediaType("audio");

  public static boolean isImage(Attachment attachment) {
    return attachment.getFileType() == AttachmentType.IMAGE;
  }

  public static AttachmentType from(String fileName) {
    return MediaTypeFactory.getMediaType(fileName)
            .map(AttachmentType::from)
            .orElse(AttachmentType.OTHER);
  }

  private static AttachmentType from(MediaType mediaType) {
    if (mediaType.isCompatibleWith(IMAGE_MediaType)) {
      return AttachmentType.IMAGE;
    }
    else if (mediaType.isCompatibleWith(VIDEO_MediaType)) {
      return AttachmentType.VIDEO;
    }
    else if (mediaType.isCompatibleWith(TEXT_MediaType)) {
      return AttachmentType.TEXT;
    }
    else if (mediaType.isCompatibleWith(AUDIO_MediaType)) {
      return AttachmentType.AUDIO;
    }
    return AttachmentType.OTHER;
  }

  public static AttachmentType from(File file) {
    return from(file.getName());
  }

  public static AttachmentType from(MultipartFile file) {
    String contentType = file.getContentType();
    if (contentType != null) {
      AttachmentType fileType = from(MediaType.valueOf(contentType));
      if (fileType == AttachmentType.OTHER) { // 检测文件名
        fileType = from(file.getOriginalFilename());
      }

      if (fileType == AttachmentType.OTHER) { // 上传的不可能为文件夹
        return AttachmentType.TEXT;
      }
      return fileType;
    }

    return AttachmentType.OTHER;
  }

}
