package cn.taketoday.blog.web.interceptor;

import java.util.List;

import cn.taketoday.blog.BlogConstant;
import cn.taketoday.blog.model.Article;
import cn.taketoday.blog.utils.Pagination;
import cn.taketoday.session.SessionHandlerInterceptor;
import cn.taketoday.session.SessionManager;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.RequestContext;

/**
 * @author TODAY 2021/1/10 22:45
 */
public class ArticleFilterInterceptor extends SessionHandlerInterceptor {

  public ArticleFilterInterceptor(SessionManager sessionManager) {
    super(sessionManager);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void afterProcess(RequestContext context, Object handler, Object result) {
    if (result instanceof Pagination && getAttribute(context, BlogConstant.BLOGGER_INFO) == null) {
      // 过滤
      final List<Article> articles = (List<Article>) ((Pagination<?>) result).getData();
      if (!CollectionUtils.isEmpty(articles)) {
        for (final Article article : articles) {
          // 过滤有密码的
          if (article.needPassword()) {
            article.setImage(null);
            article.setStatus(null);
            article.setLabels(null);

            article.setSummary("需要密码查看");
            article.setContent("需要密码查看");
            article.setMarkdown("需要密码查看");
            article.setCategory("需要密码查看");
            article.resetPassword();
          }
        }
      }
    }
  }

}
