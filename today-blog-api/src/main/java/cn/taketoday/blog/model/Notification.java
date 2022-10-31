package cn.taketoday.blog.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import cn.taketoday.blog.model.enums.NotificationType;
import lombok.Getter;
import lombok.Setter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-04-20 20:34
 */
@Setter
@Getter
public class Notification implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private String content;

  private NotificationType type;

  private long userId;

  @JsonIgnore
  private User user;

  private long receiveUserId;

  @JsonIgnore
  private User receiveUser;
  private String id;

  private LocalDateTime createTime;
  private LocalDateTime lastModify;

  /**
   * apply receive {@link User}
   *
   * @param receiveUser receive msg {@link User}
   */
  public Notification setReceiveUser(User receiveUser) {
    this.receiveUser = receiveUser;
    if (receiveUser == null) {
      this.receiveUserId = 0;
      return this;
    }
    return setReceiveUserId(receiveUser.getId());
  }

  public Notification setUser(User user) {
    this.user = user;
    if (user == null) {
      this.userId = 0;
      return this;
    }
    return setUserId(user.getId());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final Notification that))
      return false;
    return userId == that.userId
            && type == that.type
            && receiveUserId == that.receiveUserId
            && Objects.equals(id, that.id)
            && Objects.equals(user, that.user)
            && Objects.equals(content, that.content)
            && Objects.equals(receiveUser, that.receiveUser)
            && Objects.equals(createTime, that.createTime)
            && Objects.equals(lastModify, that.lastModify);
  }

  @Override
  public int hashCode() {
    return Objects.hash(content, type, userId, user, receiveUserId, receiveUser, id, createTime, lastModify);
  }

}
