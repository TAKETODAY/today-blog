package cn.taketoday.blog.web.controller;

import java.io.Serial;

import cn.taketoday.core.NoStackTraceRuntimeException;

/**
 * @author TODAY 2021/1/15 23:20
 */
public class ArticlePasswordException extends NoStackTraceRuntimeException {
  @Serial
  private static final long serialVersionUID = 1L;

  public ArticlePasswordException(String message) {
    super(message);
  }

}
