package cn.taketoday.blog.event;

import cn.taketoday.blog.model.Comment;

/**
 * 评论状态变更为审核通过时发布。
 */
public class CommentApprovedEvent extends BlogApplicationEvent {

  private final Comment comment;

  public CommentApprovedEvent(Object source, Comment comment) {
    super(source);
    this.comment = comment;
  }

  public Comment getComment() {
    return comment;
  }
}
