package cn.taketoday.blog.model.enums;

import cn.taketoday.lang.Enumerable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-03-17 21:57
 */
public enum PostStatus implements Enumerable<Integer> {

  /** 已发布 */
  PUBLISHED(0, "已发布"),
  /** 草稿 */
  DRAFT(1, "草稿"),
  /** 回收站 */
  RECYCLE(2, "回收站");

  private final int code;
  private final String msg;

  PostStatus(int code, String msg) {
    this.code = code;
    this.msg = msg;
  }

  public static PostStatus valueOf(int code) {
    return switch (code) {
      case 0 -> PUBLISHED;
      case 1 -> DRAFT;
      default -> RECYCLE;
    };
  }

  @Override
  public Integer getValue() {
    return code;
  }

  @Override
  public String getDescription() {
    return msg;
  }

}
