package cn.taketoday.blog.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import cn.taketoday.blog.model.enums.FileType;
import cn.taketoday.core.style.ToStringBuilder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Attachment implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private Long id;

  /** 附件名 */
  private String name;

  /** CDN地址 */
  private String uri;

  /** 附件本地地址 */
  private String location;

  /** 附件类型 */
  private FileType fileType;

  /** 附件大小 */
  private Long size;

  /** 是否同步 OSS */
  private Boolean sync;

  private LocalDateTime createAt;

  private LocalDateTime updateAt;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof Attachment that) {
      return Objects.equals(id, that.id)
              && fileType == that.fileType
              && Objects.equals(uri, that.uri)
              && Objects.equals(name, that.name)
              && Objects.equals(size, that.size)
              && Objects.equals(sync, that.sync)
              && Objects.equals(location, that.location);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, uri, location, fileType, size, sync);
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("id", id)
            .append("name", name)
            .append("uri", uri)
            .append("location", location)
            .append("fileType", fileType)
            .append("size", size)
            .append("sync", sync)
            .append("createAt", createAt)
            .append("updateAt", updateAt)
            .toString();
  }

}
