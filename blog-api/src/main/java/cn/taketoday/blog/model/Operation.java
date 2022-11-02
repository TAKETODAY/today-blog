package cn.taketoday.blog.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import cn.taketoday.core.style.ToStringBuilder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Operation implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private long id;// created time
  /** 产生日志的ip */
  private String ip;
  /** 标题 */
  private String title;
  /** 内容 */
  private String content;
  // operation user
  private String user;
  private String type;
  private String result;

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("id", id)
            .append("ip", ip)
            .append("title", title)
            .append("content", content)
            .append("user", user)
            .append("type", type)
            .append("result", result)
            .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Operation operation))
      return false;
    return id == operation.id
            && Objects.equals(ip, operation.ip)
            && Objects.equals(title, operation.title)
            && Objects.equals(user, operation.user)
            && Objects.equals(type, operation.type)
            && Objects.equals(content, operation.content)
            && Objects.equals(result, operation.result);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, ip, title, content, user, type, result);
  }
}
