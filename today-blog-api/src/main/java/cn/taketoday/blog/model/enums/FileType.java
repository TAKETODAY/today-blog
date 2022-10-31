package cn.taketoday.blog.model.enums;

import java.io.File;

import cn.taketoday.blog.model.Attachment;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.MediaTypeFactory;
import cn.taketoday.web.multipart.MultipartFile;

/**
 * @author TODAY
 * @since 2020/12/22 17:33
 */
public enum FileType {
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
    return attachment.getFileType() == FileType.IMAGE;
  }

  public static FileType from(String fileName) {
    return MediaTypeFactory.getMediaType(fileName)
            .map(FileType::from)
            .orElse(FileType.OTHER);
  }

  private static FileType from(MediaType mediaType) {
    if (mediaType.isCompatibleWith(IMAGE_MediaType)) {
      return FileType.IMAGE;
    }
    else if (mediaType.isCompatibleWith(VIDEO_MediaType)) {
      return FileType.VIDEO;
    }
    else if (mediaType.isCompatibleWith(TEXT_MediaType)) {
      return FileType.TEXT;
    }
    else if (mediaType.isCompatibleWith(AUDIO_MediaType)) {
      return FileType.AUDIO;
    }
    return FileType.OTHER;
  }

  public static FileType from(File file) {
    return from(file.getName());
  }

  public static FileType from(MultipartFile file) {
    String contentType = file.getContentType();
    if (contentType != null) {
      FileType fileType = from(MediaType.valueOf(contentType));
      if (fileType == FileType.OTHER) { // 检测文件名
        fileType = from(file.getOriginalFilename());
      }

      if (fileType == FileType.OTHER) { // 上传的不可能为文件夹
        return FileType.TEXT;
      }
      return fileType;
    }

    return FileType.OTHER;
  }

}
