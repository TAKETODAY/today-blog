package cn.taketoday.blog.web;

import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.model.User;
import cn.taketoday.web.UnauthorizedException;
import lombok.Data;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/11 08:05
 */
@Data
public class LoginInfo {

  private User loginUser;

  private Blogger blogger;

  public boolean isLoggedIn() {
    return loginUser != null;
  }

  public boolean isBloggerLoggedIn() {
    return blogger != null;
  }

  public long getLoginUserId() {
    if (loginUser == null) {
      throw new UnauthorizedException();
    }
    return loginUser.getId();
  }

}
