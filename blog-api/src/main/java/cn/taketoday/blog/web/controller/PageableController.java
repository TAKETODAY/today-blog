/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.blog.web.controller;

import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.blog.BlogConstant;
import cn.taketoday.blog.Pageable;
import cn.taketoday.blog.aspect.Logger;
import cn.taketoday.blog.model.Article;
import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.model.Category;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.model.enums.PostStatus;
import cn.taketoday.blog.model.enums.UserStatus;
import cn.taketoday.blog.service.ArticleService;
import cn.taketoday.blog.service.BloggerService;
import cn.taketoday.blog.service.CategoryService;
import cn.taketoday.blog.service.CommentService;
import cn.taketoday.blog.service.LabelService;
import cn.taketoday.blog.service.UserService;
import cn.taketoday.blog.utils.BlogUtils;
import cn.taketoday.blog.utils.MD5;
import cn.taketoday.blog.utils.Pagination;
import cn.taketoday.blog.utils.StringUtils;
import cn.taketoday.blog.web.interceptor.RequiresUser;
import cn.taketoday.context.annotation.Profile;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.session.WebSession;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.ui.Model;
import cn.taketoday.web.NotFoundException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.annotation.CookieValue;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.POST;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.annotation.SessionAttribute;
import cn.taketoday.web.config.WebMvcConfigurer;
import cn.taketoday.web.view.RedirectModel;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2020-04-04 11:20
 */
@Controller
@Profile("dev")
public class PageableController implements WebMvcConfigurer, BlogConstant {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final String URL_FORBIDDEN = "/Forbidden";
  private static final String URL_NOT_FOUND = "/NotFound";
  //    private static final String URL_SERVER_IS_BUSY = "/ServerIsBusy";

  private final ArticleService articleService;
  private final CategoryService categoryService;

  public PageableController(ArticleService articleService,
          CategoryService categoryService) {
    this.articleService = articleService;
    this.categoryService = categoryService;
  }

  public static String redirectForbidden() {
    return redirect(URL_FORBIDDEN);
  }

  public static String redirectNotFound() {
    return redirect(URL_NOT_FOUND);
  }

  public static String redirect(String path) {
    return "redirect:".concat(path);
  }

  //-----------------------------------------

  /**
   * For index page
   *
   * @param model for model
   * @param pageable
   */
  @GET({ "/index", "/", "", "/index.html" })
  public String index(Model model, Pageable pageable) {

    int pageCount = BlogUtils.pageCount(articleService.countByStatus(PostStatus.PUBLISHED),
            pageable.getSize());

    if (BlogUtils.notFound(pageable.getCurrent(), pageCount)) {
      return redirectNotFound();
    }

    BlogUtils.prepareModel(pageable, pageCount, model);

    model.setAttribute(BlogConstant.KEY_ARTICLE_LIST, articleService.getHomeArticles(pageable));
    return "/index";
  }

  /**
   * Write article
   *
   * @param markdown markdown support?
   * @return a writing article page
   */
  @GET("/articles/write")
  public String write(@RequestParam(defaultValue = "true") boolean markdown) {
    return markdown ? "/admin/article/markdown/write" : "/admin/article/write";
  }

  /**
   * Modify the article
   *
   * @param markdown markdown support?
   */
  @GET("/articles/*/modify")
  public String modify(@RequestParam(defaultValue = "true") boolean markdown) {
    return markdown ? "/admin/article/markdown/modify" : "/admin/article/modify";
  }

  @GET({ "/articles/{id}", "/articles/{id}.html" })
  public String getArticle(String key,
          @PathVariable("id") Long id,
          RequestContext request,
          @SessionAttribute(BlogConstant.BLOGGER_INFO) Blogger blogger) {

    Article article = articleService.getById(id);
    if (article == null) {
      throw new NotFoundException("文章不存在");
    }
    if (article.getStatus() != PostStatus.PUBLISHED) { // 放在控制器层控制
      return redirectNotFound();
    }
    request.setAttribute("article", article);

    // HashUtils.getEncodedPassword(password) //TODO
    if (blogger == null && article.needPassword()) {
      if (key != null) {
        if (!key.equals(article.getPassword())) {
          request.setAttribute("error", true);
          return "/article/password";
        }
      }
      else {
        return "/article/password";
      }
    }

    return "/article/index";
  }

  // ---------------------------

  @GET("/categories/{name}")
  public String categories(Model model, Pageable pageable, @PathVariable String name) {
    Category category = categoryService.getCategory(name);
    if (category == null) {
      return redirectNotFound();
    }
    int pageCount = BlogUtils.pageCount(articleService.countByCategory(name), pageable.getSize());
    if (BlogUtils.notFound(pageable.getCurrent(), pageCount)) {
      return redirectNotFound();
    }
    model.setAttribute(BlogConstant.KEY_TITLE, name);
    model.setAttribute(BlogConstant.KEY_ARTICLE_LIST, articleService.getByCategory(name, pageable));

    BlogUtils.prepareModel(pageable, pageCount, model);
    return "/categories/list";
  }

  // ----------------------------------------

  @Autowired
  private LabelService labelService;

  /**
   * For tags page
   */
  @GET("tags/{tag}")
  public String tagAndPageAndSize(Model model, Pageable pageable, @PathVariable String tag) {

    if (labelService.getByName(tag) == null) {
      return redirectNotFound();
    }

    int pageCount = BlogUtils.pageCount(articleService.countByLabel(tag), pageable.getSize());
    if (BlogUtils.notFound(pageable.getCurrent(), pageCount)) {
      return redirectNotFound();
    }

    model.setAttribute(BlogConstant.KEY_TITLE, tag);
    model.setAttribute(BlogConstant.KEY_ARTICLE_LIST, articleService.getByLabel(tag, pageable));

    BlogUtils.prepareModel(pageable, pageCount, model);

    return "/tags/list";
  }

  /**
   * Search for page
   *
   * current page
   *
   * @param q query string
   */
  @GET("search")
  public String search(String q, Pageable pageable, Model model) {

    if (StringUtils.isEmpty(q)) { // search index page
      return "/search/index";
    }
    q = BlogUtils.stripAllXss(q);
    Pagination<Article> searchArticle = articleService.search(q, pageable);
    if (searchArticle != null) {

      int pageCount = BlogUtils.pageCount((int) searchArticle.getNum(), pageable.getSize());
      if (BlogUtils.notFound(pageable.getCurrent(), pageCount)) {
        return redirectNotFound();
      }
      model.setAttribute(BlogConstant.KEY_TITLE, q);
      model.setAttribute(BlogConstant.KEY_ARTICLE_LIST, searchArticle.getData());
      BlogUtils.prepareModel(pageable, pageCount, model);
    }
    return "/search/list";
  }

  @GET("/login")
  public String login(@CookieValue String email, String forward, Model model) {

    model.setAttribute("forward", forward);
    model.setAttribute(BlogConstant.KEY_EMAIL, email);

    return "/login/index";
  }

  @GET("/otherLogin")
  public String otherLogin(WebSession session, String forward) {
    session.invalidate();
    return redirectLogin(forward);
  }

  /**
   * Redirect to login
   *
   * @param forward login page
   * @return
   */
  static String redirectLogin(String forward) {
    if (StringUtils.isEmpty(forward)) {
      return redirect("/login");
    }
    return redirect("/login?forward=" + StringUtils.uriDecode(forward, StandardCharsets.UTF_8));
  }

  @Autowired
  private BloggerService bloggerService;

  @POST("/login")
  @Logger(value = "登录", //
          content = "email:[${email}] " //
                  + "input code:[${randCode}] "//
                  + "in session:[${randCodeInSession}] "//
                  + "forward to:[${forward}] "//
                  + "msg:[${redirectModel.getAttribute('msg')}]"//
          )
  public String login(WebSession session,
          @RequestParam(required = true) String email,
          @RequestParam(required = true) String passwd,
          @RequestParam(required = true) String randCode,
          @RequestParam(required = false) String forward,
          @SessionAttribute(RAND_CODE) String randCodeInSession,
          RedirectModel redirectModel) //
  {

    session.removeAttribute(RAND_CODE);

    if (!randCode.equalsIgnoreCase(randCodeInSession)) {
      redirectModel.setAttribute(KEY_MSG, "验证码错误!");
      redirectModel.setAttribute(KEY_EMAIL, email);
      redirectModel.setAttribute(KEY_FORWARD, forward);
      return redirectLogin(forward);
    }

    User loginUser = userService.getByEmail(email);
    if (loginUser == null) {
      //log.info("Email: [{}] does not exist.", email);
      redirectModel.setAttribute(KEY_EMAIL, email);
      redirectModel.setAttribute(KEY_FORWARD, forward);
      redirectModel.setAttribute(KEY_MSG, email + " 账号不存在!");
      return redirectLogin(forward);
    }

    MD5 md5 = new MD5();
    passwd = md5.getMD5Str(md5.getMD5Str(passwd));

    if (!loginUser.getPassword().equals(passwd)) {
      //          log.info("Passwd: [{}] does not correct passwd is: [{}].", passwd, loginUser.getPassword());
      redirectModel.setAttribute(KEY_MSG, "密码错误!");
      redirectModel.setAttribute(KEY_EMAIL, email);
      redirectModel.setAttribute(KEY_FORWARD, forward);
      return redirectLogin(forward);
    }

    // check user state
    UserStatus status = loginUser.getStatus();
    //      log.info("Check state: [{}]", status);
    switch (status) {
      case NORMAL:
        break;
      case LOCKED:
      case RECYCLE:
      case INACTIVE:
        redirectModel.setAttribute(KEY_MSG, status.getDescription());
        return redirectLogin(forward);
      default: {
        redirectModel.setAttribute(KEY_MSG, "系统错误");
        return redirectLogin(forward);
      }
    }

    //      log.info("Login success with email: [{}], and passwd: [{}]", email, passwd);
    session.setAttribute(USER_INFO, loginUser);

    // apply login success cookie
    applyCookie(loginUser);

    // is blogger ?
    Blogger blogger = bloggerService.getBlogger();
    if (loginUser.getEmail().equals(blogger.getEmail()) && loginUser.getPassword().equals(blogger.getPasswd())) {
      // log.info("Blogger Login success.");
//      redirectModel.setAttribute(KEY_MSG, "Blogger Login success");
      session.setAttribute(BLOGGER_INFO, blogger);
    }
    else {
      redirectModel.setAttribute(KEY_MSG, "登录成功");
    }
    return redirect(forward);
  }

  protected void applyCookie(User loginUser) {
    RequestContext requestContext = RequestContextHolder.getRequired();
    String contextPath = requestContext.getContextPath();

    HttpCookie email = ResponseCookie.from(KEY_EMAIL, loginUser.getEmail())
            .maxAge(Duration.ofDays(1))
            .path(contextPath + LOGIN_URL)
            .httpOnly(true)
            .build();

    requestContext.addCookie(email);
  }

  // ---------------------------

  @Autowired
  private UserService userService;

  @Autowired
  private CommentService commentService;

  @RequiresUser
  @GET("/user/settings")
  public String settings() {
    return "/user/setting/index";
  }

  /**
   * If user not login, will not a permission to visit page
   */
  @GET("/user/info")
  public String info(User userInfo, Model model, Pageable pageable) {

    int pageCount = BlogUtils.pageCount(commentService.countByUser(userInfo), pageable.getSize());
    if (BlogUtils.notFound(pageable.getCurrent(), pageCount)) {
      return redirectNotFound();
    }

    BlogUtils.prepareModel(pageable, pageCount, model);

    model.setAttribute("comments", commentService.getByUser(userInfo, pageable));

    return "/user/index";
  }

}
