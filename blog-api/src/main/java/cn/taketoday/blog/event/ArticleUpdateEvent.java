/*
 * Copyright 2017 - 2026 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.blog.event;

/**
 * 文章更新事件。
 * <p>
 * 当文章内容发生更新时发布此事件，用于通知监听器执行相应的后续操作（如更新索引、清除缓存等）。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.2 2024/11/12 17:55
 */
public class ArticleUpdateEvent extends BlogEvent {

  private final long articleId;

  /**
   * 构造一个新的文章更新事件。
   *
   * @param source 事件源对象
   * @param articleId 被更新的文章ID
   */
  public ArticleUpdateEvent(Object source, long articleId) {
    super(source);
    this.articleId = articleId;
  }

  /**
   * 获取被更新的文章ID。
   *
   * @return 文章ID
   */
  public long getArticleId() {
    return articleId;
  }

}
