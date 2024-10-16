/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.blog.web.http;

import org.hibernate.validator.constraints.Length;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import cn.taketoday.blog.log.Logging;
import cn.taketoday.blog.model.Attachment;
import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.model.enums.UserStatus;
import cn.taketoday.blog.service.AttachmentService;
import cn.taketoday.blog.service.BloggerService;
import cn.taketoday.blog.service.UserService;
import cn.taketoday.blog.util.HashUtils;
import cn.taketoday.blog.util.StringUtils;
import cn.taketoday.blog.web.ErrorMessageException;
import cn.taketoday.blog.web.Json;
import cn.taketoday.blog.web.interceptor.RequestLimit;
import cn.taketoday.blog.web.interceptor.RequiresUser;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.session.SessionManager;
import cn.taketoday.session.SessionManagerOperations;
import cn.taketoday.session.WebSession;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.DELETE;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.POST;
import cn.taketoday.web.annotation.PatchMapping;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.util.UriUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.CustomLog;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-09-15 20:27
 */
@CustomLog
@RestController
@RequestMapping("/api/auth")
class AuthorizeController extends SessionManagerOperations {

  private final UserService userService;

  private final BloggerService bloggerService;

  private final AttachmentService attachmentService;

  public AuthorizeController(SessionManager sessionManager, ConfigurableEnvironment environment,
          UserService userService, BloggerService bloggerService, AttachmentService attachmentService) {
    super(sessionManager);
    this.userService = userService;
    this.bloggerService = bloggerService;
    this.attachmentService = attachmentService;
  }

  @GET
  public User getLoginUser(User loginUser) {
    return loginUser;
  }

  @DELETE
  public void logout(WebSession session) {
    session.invalidate();
  }

  static class UserFrom {

    @NotEmpty(message = "邮箱不能为空")
    @Email(message = "请您输入正确格式的邮箱")
    public String email;

    @NotEmpty(message = "密码不能为空")
    public String password;
  }

  /**
   * <pre> {@code
   * {
   *   "success": false,
   *   "message": "登录失败",
   *   "data": {
   *
   *   }
   * }
   * } </pre>
   */
  @POST
  @RequestLimit(unit = TimeUnit.MINUTES, count = 5, errorMessage = "一分钟只能尝试5次登陆,请稍后重试")
  @Logging(title = "登录", content = "邮箱:[#{#user.email}]登录")
  public Json login(@Valid @RequestBody UserFrom user, RequestContext request) {
    User loginUser = userService.getByEmail(user.email);
    if (loginUser == null) {
      return Json.failed(user.email + " 账号不存在!", user.email);
    }

    String passwd = HashUtils.getEncodedPassword(user.password);
    if (!Objects.equals(loginUser.getPassword(), passwd)) {
      return Json.failed("密码错误!", user.email);
    }

    // check user state
    UserStatus status = loginUser.getStatus();
    // log.info("Check state: [{}]", status);
    switch (status) {
      case NORMAL -> { }
      case LOCKED, RECYCLE, INACTIVE -> {
        return Json.failed(status.getDescription(), user.email);
      }
      default -> {
        return Json.failed("系统错误", user.email);
      }
    }

    WebSession session = getSession(request);
    // login success
    loginUser.bindTo(session);

    // is blogger ?
    Blogger blogger = bloggerService.getBlogger();
    // 是对应邮箱 判断密码

    if (Objects.equals(loginUser.getEmail(), blogger.getEmail())) {
      if (!Objects.equals(loginUser.getPassword(), blogger.getPasswd())) {
        blogger = bloggerService.fetchBlogger();
        if (Objects.equals(loginUser.getPassword(), blogger.getPasswd())) {
          applyBlogger(session, loginUser, blogger);
        }
      }
      else {
        applyBlogger(session, loginUser, blogger);
      }
    }

    return Json.ok("登录成功", loginUser);
  }

  private void applyBlogger(WebSession session, User loginUser, Blogger blogger) {
    blogger.bindTo(session);
    loginUser.setBlogger(true);
  }

  /**
   * 登录 v2
   * <pre> {@code
   * {
   *   "id": 1544107262149,
   *   "status": "NORMAL",
   *   "name": "TAKETODAY",
   *   "email": "taketoday@foxmail.com",
   *   "site": "https://taketoday.cn",
   *   "type": "master",
   *   "avatar": "/upload/2019/4/9/6cd520c4-509a-4d35-9028-2485a87bd5c6.png",
   *   "introduce": "代码是我心中的一首诗",
   *   "background": "/upload/2019/4/9/eb785277-0aef-4e42-85f3-2f9280fc6c58.png",
   * }
   * } </pre>
   */
  @POST(params = "v2")
  @RequestLimit(unit = TimeUnit.MINUTES, count = 5, errorMessage = "一分钟只能尝试5次登陆,请稍后重试")
  @Logging(title = "登录", content = "邮箱:[#{#user.email}]登录")
  public User loginV2(@Valid @RequestBody UserFrom user, RequestContext request) {
    User loginUser = userService.getByEmail(user.email);
    if (loginUser == null) {
      throw ErrorMessageException.failed(user.email + " 账号不存在!");
    }

    String passwd = HashUtils.getEncodedPassword(user.password);
    if (!Objects.equals(loginUser.getPassword(), passwd)) {
      throw ErrorMessageException.failed("密码错误!");
    }

    // check user state
    UserStatus status = loginUser.getStatus();
    // log.info("Check state: [{}]", status);
    switch (status) {
      case NORMAL -> { }
      case LOCKED, RECYCLE, INACTIVE -> throw ErrorMessageException.failed(status.getDescription());
      default -> throw ErrorMessageException.failed("系统错误");
    }

    WebSession session = getSession(request);
    // login success
    loginUser.bindTo(session);

    // is blogger ?
    Blogger blogger = bloggerService.getBlogger();
    // 是对应邮箱 判断密码

    if (Objects.equals(loginUser.getEmail(), blogger.getEmail())) {
      if (!Objects.equals(loginUser.getPassword(), blogger.getPasswd())) {
        blogger = bloggerService.fetchBlogger();
        if (Objects.equals(loginUser.getPassword(), blogger.getPasswd())) {
          applyBlogger(session, loginUser, blogger);
        }
      }
      else {
        applyBlogger(session, loginUser, blogger);
      }
    }

    return loginUser;
  }

  //---------------------------------------------------------------------
  // 修改当前用户的信息
  //---------------------------------------------------------------------

  // 第三方

  public static String redirect(String path) {
    return "redirect:".concat(path);
  }

  static String redirectLogin(String forward) {
    if (StringUtils.isEmpty(forward)) {
      return redirect("/login");
    }
    return redirect(forward);
  }

  static String redirectLoginError(String forward, String message) {
    if (StringUtils.isNotEmpty(message)) {
      message = UriUtils.decode(message, StandardCharsets.UTF_8);
    }
    if (StringUtils.isEmpty(forward)) {
      return redirect("/login?message=" + message);
    }
    return redirect("/login?forward=" + UriUtils.decode(forward, StandardCharsets.UTF_8) + "&message=" + message);
  }

  @FunctionalInterface
  interface OauthUserConnectionFunction {

    HttpURLConnection apply(String accessToken) throws IOException;
  }

  static String decode(String source) {
    return UriUtils.decode(source, StandardCharsets.UTF_8);
  }

  //---------------------------------------------------------------------
  // 修改当前用户的信息
  //---------------------------------------------------------------------

  public static class InfoForm {
    @NotBlank(message = "用户名不能为空")
    public String name;

    @Length(max = 1000, message = "介绍最多1000个字符")
    public String introduce;
  }

  /**
   * 当前登录用户信息
   *
   * @param loginUser 登录用户
   * @param form 表单
   */
  @PatchMapping
  @RequestLimit(count = 2, unit = TimeUnit.MINUTES, errorMessage = "一分钟只能最多修改2次用户信息")
  public User userInfo(@RequiresUser User loginUser, @RequestBody @Valid InfoForm form) {
    // 要判断不一致才更新
    if (Objects.equals(form.name, loginUser.getName())
            && Objects.equals(form.introduce, loginUser.getIntroduce())) {
      throw ErrorMessageException.failed("未更改任何信息");
    }

    Long id = loginUser.getId();
    // TODO 验证用户有效性
    // 设置新值
    User user = new User();
    user.setId(id);
    user.setName(form.name);
    user.setIntroduce(form.introduce);

    userService.updateById(user);

    // update to session
    loginUser.setName(form.name);
    loginUser.setIntroduce(form.introduce);
    return loginUser;
  }

  public static class ChangePasswordForm {

    @NotBlank(message = "旧密码不能为空")
    public String oldPassword;

    @Length(min = 6, max = 48, message = "新密码至少输入6个字符，最多48个字符")
    public String newPassword;

    public String confirmNewPassword;
  }

  /**
   * 修改用户密码
   *
   * @param loginUser 登录用户
   * @param form 表单
   */
  @PatchMapping(params = "password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RequestLimit(unit = TimeUnit.MINUTES, errorMessage = "一分钟只能最多修改2次密码")
  public void changePassword(@RequiresUser User loginUser, @RequestBody @Valid ChangePasswordForm form) {
    // 校验密码是否有效
    if (!Objects.equals(form.confirmNewPassword, form.newPassword)) {
      throw ErrorMessageException.failed("两次输入的新密码不一致");
    }

    // 校验数据是否存在该用户
    User byId = userService.getById(loginUser.getId());
    ErrorMessageException.notNull(byId, "要修改密码的用户不存在");

    // 校验旧密码
    String oldPassword = HashUtils.getEncodedPassword(form.oldPassword);
    if (!Objects.equals(oldPassword, byId.getPassword())) {
      throw ErrorMessageException.failed("原密码错误");
    }

    // 重新生成
    String newPassword = HashUtils.getEncodedPassword(form.newPassword);

    // 更新数据库
    User user = new User();
    user.setId(loginUser.getId());
    user.setPassword(newPassword);

    Blogger blogger = bloggerService.getBlogger();
    if (byId.isBlogger() && Objects.equals(blogger.getEmail(), byId.getEmail())) {
      bloggerService.updatePassword(newPassword);
    }

    userService.updateById(user);
  }

  public static class UserEmailForm {

    @NotEmpty(message = "请输入新邮箱")
    public String email;

    @NotEmpty(message = "请输入密码")
    public String password;

//    @NotEmpty(message = "请输入手机号")
//    public String mobilePhone;
  }

  /**
   * Change User's Email
   */
  @PatchMapping(params = "email-mobile-phone")
  @RequestLimit(unit = TimeUnit.MINUTES, errorMessage = "一分钟只能最多修改2次邮箱或手机")
  public User changeEmailAndMobilePhone(@RequiresUser User loginUser, @Valid @RequestBody UserEmailForm form) {
    if (Objects.equals(loginUser.getEmail(), form.email)) {
      throw ErrorMessageException.failed("未更改任何信息");
    }

    // Check User's Password
    String encodedPassword = HashUtils.getEncodedPassword(form.password);
    if (encodedPassword.equals(loginUser.getPassword())) {
      // 更新数据库
      User user = new User();
      user.setEmail(form.email);
      user.setId(loginUser.getId());
//      user.setMobilePhone(form.mobilePhone);

      userService.updateById(user);

      loginUser.setEmail(form.email);
//      loginUser.setMobilePhone(form.mobilePhone);
    }
    else {
      throw ErrorMessageException.failed("密码不正确");
    }
    return loginUser;
  }

  /**
   * change image
   */
  @PatchMapping(params = "avatar")
  @RequestLimit(unit = TimeUnit.MINUTES, errorMessage = "一分钟只能最多修改1次头像")
  public User changeAvatar(@RequiresUser User loginUser, MultipartFile avatar) {
    String originalFilename = avatar.getOriginalFilename();
    String randomHashString = HashUtils.getRandomHashString(16);

    Attachment attachment = attachmentService.upload(avatar, randomHashString + originalFilename);
    String uri = attachment.getUri();

    User user = new User();
    user.setId(loginUser.getId());
    user.setAvatar(uri);

    userService.updateById(user);
    loginUser.setAvatar(uri);

    return loginUser;
  }

}
