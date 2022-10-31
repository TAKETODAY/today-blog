package cn.taketoday.blog.model.form;

import java.util.Objects;

import cn.taketoday.blog.model.enums.PostStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY 2020/12/20 22:42
 */
@Getter
@Setter
public class SearchForm {

  private String title;
  private String content;
  private String category;
  private PostStatus status;

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final SearchForm that))
      return false;
    return status == that.status
            && Objects.equals(title, that.title)
            && Objects.equals(content, that.content)
            && Objects.equals(category, that.category);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, content, category, status);
  }
}
