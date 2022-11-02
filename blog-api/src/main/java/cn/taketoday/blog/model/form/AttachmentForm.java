package cn.taketoday.blog.model.form;

import java.util.Objects;

import cn.taketoday.blog.model.enums.FileType;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY 2020/12/22 22:35
 */
@Getter
@Setter
public class AttachmentForm {

  private String name;
  private FileType fileType;

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final AttachmentForm that))
      return false;
    return Objects.equals(name, that.name) && fileType == that.fileType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, fileType);
  }
}
