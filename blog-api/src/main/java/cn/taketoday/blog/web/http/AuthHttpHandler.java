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

package cn.taketoday.blog.web.http;

import org.hibernate.validator.constraints.Length;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

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
import cn.taketoday.blog.util.PasswordEncoder;
import cn.taketoday.blog.util.StringUtils;
import cn.taketoday.blog.web.ErrorMessageException;
import cn.taketoday.blog.web.interceptor.RequestLimit;
import cn.taketoday.blog.web.interceptor.RequiresUser;
import infra.beans.support.BeanPropertyUtils;
import infra.http.HttpStatus;
import infra.http.ResponseEntity;
import infra.session.Session;
import infra.session.SessionManagerOperations;
import infra.web.HttpContext;
import infra.web.annotation.DELETE;
import infra.web.annotation.GET;
import infra.web.annotation.POST;
import infra.web.annotation.PUT;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.ResponseStatus;
import infra.web.annotation.RestController;
import infra.web.multipart.Part;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.CustomLog;

/**
 * 当前用户认证与账户资料 HTTP 接口。
 *
 * <p>登录状态通过会话维护。{@code GET /api/auth} 是登录状态探测接口：已登录时返回当前用户，
 * 未登录时返回 {@code 204 No Content}；修改账户资料的接口则要求当前会话已认证。</p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-09-15 20:27
 */
@CustomLog
@RestController
@RequestMapping("/api/auth")
class AuthHttpHandler {

  private final UserService userService;

  private final BloggerService bloggerService;

  private final PasswordEncoder passwordEncoder;

  private final AttachmentService attachmentService;

  private final SessionManagerOperations sessionManagerOperations;

  public AuthHttpHandler(UserService userService, BloggerService bloggerService, AttachmentService attachmentService,
          SessionManagerOperations sessionManagerOperations, PasswordEncoder passwordEncoder) {
    this.userService = userService;
    this.bloggerService = bloggerService;
    this.attachmentService = attachmentService;
    this.sessionManagerOperations = sessionManagerOperations;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * 查询当前会话的登录用户。
   *
   * <p>该接口用于前端探测登录状态，匿名访问属于正常流程，不会返回 401。</p>
   *
   * @param loginUser 当前会话用户；未登录时为 {@code null}
   * @return {@code 200 OK} 和当前用户，或未登录时的 {@code 204 No Content}
   */
  @GET
  public ResponseEntity<@Nullable User> getLoginUser(@Nullable User loginUser) {
    return loginUser != null
            ? ResponseEntity.ok(loginUser)
            : ResponseEntity.noContent().build();
  }

  /**
   * 退出当前会话。
   *
   * <p>调用后当前会话立即失效，后续需要认证的请求将被拒绝。</p>
   *
   * @param session 当前 HTTP 会话
   */
  @DELETE
  public void logout(Session session) {
    session.invalidate();
  }

  @NullUnmarked
  static class UserFrom {

    /** 登录邮箱。 */
    @NotEmpty(message = "邮箱不能为空")
    @Email(message = "请您输入正确格式的邮箱")
    public String email;

    /** 登录密码。 */
    @NotEmpty(message = "密码不能为空")
    public String password;
  }

  private void applyBlogger(Session session, User loginUser, Blogger blogger) {
    blogger.bindTo(session);
    loginUser.setBlogger(true);
  }

  /**
   * 使用邮箱和密码登录。
   *
   * <p>登录成功后将用户绑定到当前会话并返回用户信息。与博主账号匹配时，
   * 同时绑定博主身份。登录接口每分钟最多允许尝试 5 次。</p>
   *
   * @param from 邮箱和密码
   * @param request 当前 HTTP 请求，用于获取或创建会话
   * @return 登录成功后的用户信息
   */
  @POST(params = "v2")
  @RequestLimit(unit = TimeUnit.MINUTES, count = 5, errorMessage = "一分钟只能尝试5次登陆,请稍后重试")
  @Logging(title = "登录", content = "邮箱:[#{#from.email}]登录")
  public User loginV2(@Valid @RequestBody UserFrom from, HttpContext request) {
    User loginUser = userService.getByEmail(from.email);
    if (loginUser == null) {
      throw ErrorMessageException.failed(from.email + " 账号不存在!");
    }

    if (!passwordEncoder.matches(from.password, loginUser.getPassword())) {
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

    Session session = sessionManagerOperations.getSession(request);
    // login success
    loginUser.bindTo(session);

    // is blogger ?
    Blogger blogger = bloggerService.getBlogger();
    // 是对应邮箱 判断密码

    if (Objects.equals(loginUser.getEmail(), blogger.getEmail())) {
      if (!passwordEncoder.matches(from.password, blogger.getPasswd())) {
        blogger = bloggerService.fetchBlogger();
        if (passwordEncoder.matches(from.password, blogger.getPasswd())) {
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

  @NullUnmarked
  public static class InfoForm {
    /** 用户姓名或昵称。 */
    @NotBlank(message = "请输入姓名或昵称")
    public String name;

    /** 个人站点地址。 */
    @Nullable
    public String site;

    /** 个人简介，最多 256 个字符。 */
    @Length(max = 256, message = "介绍最多256个字符")
    public String introduce;

    /** 是否接收邮件通知。 */
    public boolean notification = false;
  }

  /**
   * 更新当前登录用户的资料。
   *
   * <p>如果提交内容与当前资料完全一致，将返回业务错误。</p>
   *
   * @param loginUser 当前登录用户
   * @param form 用户资料
   * @return 更新后的用户信息
   */
  @PUT
  @RequestLimit(count = 2, unit = TimeUnit.MINUTES, errorMessage = "一分钟只能最多修改2次用户信息")
  public User updateUserInfo(@RequiresUser User loginUser, @RequestBody @Valid InfoForm form) {
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

    if (!Objects.equals(form.site, loginUser.getSite())) {
      user.setSite(form.site);
    }

    if (form.notification != loginUser.getNotification()) {
      user.setNotification(form.notification);
    }

    userService.updateById(user);

    // update to session
    BeanPropertyUtils.copy(user, loginUser);
    return loginUser;
  }

  @NullUnmarked
  public static class ChangePasswordForm {

    /** 当前密码。 */
    @NotBlank(message = "旧密码不能为空")
    public String oldPassword;

    /** 新密码，长度为 6 至 48 个字符。 */
    @Length(min = 6, max = 48, message = "新密码至少输入6个字符，最多48个字符")
    public String newPassword;

    /** 新密码确认值，必须与 {@link #newPassword} 一致。 */
    public String confirmNewPassword;
  }

  /**
   * 修改当前登录用户的密码。
   *
   * <p>修改成功返回 {@code 204 No Content}。</p>
   *
   * @param loginUser 当前登录用户
   * @param form 旧密码、新密码及确认密码
   */
  @PUT(params = "password")
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
    if (!passwordEncoder.matches(form.oldPassword, byId.getPassword())) {
      throw ErrorMessageException.failed("原密码错误");
    }

    // 重新生成
    String newPassword = passwordEncoder.encode(form.newPassword);

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

  @NullUnmarked
  public static class UserEmailForm {

    /** 新邮箱地址。 */
    @NotEmpty(message = "请输入新邮箱")
    public String email;

    /** 当前登录密码，用于确认身份。 */
    @NotEmpty(message = "请输入密码")
    public String password;
  }

  /**
   * 修改当前登录用户的邮箱。
   *
   * @param loginUser 当前登录用户
   * @param form 新邮箱和当前登录密码
   * @return 更新后的用户信息
   */
  @PUT(params = "email-mobile-phone")
  @RequestLimit(unit = TimeUnit.MINUTES, errorMessage = "一分钟只能最多修改2次邮箱或手机")
  public User changeEmailAndMobilePhone(@RequiresUser User loginUser, @Valid @RequestBody UserEmailForm form) {
    if (Objects.equals(loginUser.getEmail(), form.email)) {
      throw ErrorMessageException.failed("未更改任何信息");
    }

    // Check User's Password
    if (passwordEncoder.matches(form.password, loginUser.getPassword())) {
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
   * 修改当前登录用户的头像。
   *
   * @param loginUser 当前登录用户
   * @param avatar 新头像文件
   * @return 更新后的用户信息
   */
  @PUT(params = "avatar")
  @RequestLimit(unit = TimeUnit.MINUTES, errorMessage = "一分钟只能最多修改1次头像")
  @Logging(title = "用户保存头像", content = "上传:[#{#avatar.getOriginalFilename()}] 邮箱:[#{#loginUser.email}]")
  public User changeAvatar(@RequiresUser User loginUser, Part avatar) {
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

  /**
   * 修改当前用户的背景图片。
   *
   * @param loginUser 当前会话用户
   * @param background 新背景图片文件
   * @return 更新后的用户信息
   */
  @PUT(params = "background")
  @Logging(title = "用户修改背景", content =
          "文件名: [#{#background.getOriginalFilename()}] 邮箱:[#{#loginUser.email}]")
  public User changeBackground(User loginUser, Part background) {
    Attachment attachment = attachmentService.upload(
            background, StringUtils.getRandomImageName(background.getOriginalFilename()));

    String path = attachment.getUri();
    if (!path.equals(loginUser.getBackground())) { // not equals
      User user = new User(loginUser.getId());
      user.setBackground(path);
      userService.updateById(user);
      loginUser.setBackground(path);
    }
    return loginUser;
  }

}
