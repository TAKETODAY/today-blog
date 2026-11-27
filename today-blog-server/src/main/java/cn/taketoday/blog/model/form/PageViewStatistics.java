package cn.taketoday.blog.model.form;

import java.util.Objects;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2021/3/5 16:18
 */
public class PageViewStatistics {

  public int ip;
  public int uv;
  public int pv;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o instanceof PageViewStatistics that) {
      return ip == that.ip && uv == that.uv && pv == that.pv;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(ip, uv, pv);
  }

}
