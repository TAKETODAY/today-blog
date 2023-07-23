/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.hibernate.validator.constraints.Length;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import cn.taketoday.blog.BlogConstant;
import cn.taketoday.blog.ErrorMessageException;
import cn.taketoday.blog.Json;
import cn.taketoday.blog.config.BlogConfig;
import cn.taketoday.blog.log.Logging;
import cn.taketoday.blog.model.Attachment;
import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.model.enums.UserStatus;
import cn.taketoday.blog.model.oauth.Oauth;
import cn.taketoday.blog.service.AttachmentService;
import cn.taketoday.blog.service.BloggerService;
import cn.taketoday.blog.service.UserService;
import cn.taketoday.blog.util.HashUtils;
import cn.taketoday.blog.util.HttpUtils;
import cn.taketoday.blog.util.ObjectUtils;
import cn.taketoday.blog.util.StringUtils;
import cn.taketoday.blog.web.interceptor.RequestLimit;
import cn.taketoday.blog.web.interceptor.RequiresUser;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.core.env.Environment;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Nullable;
import cn.taketoday.session.SessionManager;
import cn.taketoday.session.SessionManagerOperations;
import cn.taketoday.session.WebSession;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.InternalServerException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.DELETE;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.POST;
import cn.taketoday.web.annotation.PatchMapping;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.annotation.ResponseBody;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.annotation.SessionAttribute;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.util.UriComponentsBuilder;
import cn.taketoday.web.util.UriUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.CustomLog;
import lombok.Setter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-09-15 20:27
 */
@CustomLog
@RestController
@RequestMapping("/api/auth")
public class AuthorizeController extends SessionManagerOperations {
  private final Oauth giteeOauth;
  private final Oauth gitHubOauth;

  private final BlogConfig blogConfig;
  private final UserService userService;
  private final BloggerService bloggerService;
  private final AttachmentService attachmentService;

  private Map<String, OauthMetadata> oauthMetadata;

  public AuthorizeController(SessionManager sessionManager,
          Environment environment, BlogConfig blogConfig, UserService userService,
          BloggerService bloggerService, AttachmentService attachmentService) {
    super(sessionManager);
    this.blogConfig = blogConfig;
    this.userService = userService;
    this.bloggerService = bloggerService;
    this.giteeOauth = Binder.get(environment).bind("gitee", Oauth.class).get();
    this.gitHubOauth = Binder.get(environment).bind("github", Oauth.class).get();
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

  @Setter
  public static class UserFrom {

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
  @Logging(title = "登录", content = "email:[${#user.email}]")
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
      case NORMAL:
        break;
      case LOCKED:
      case RECYCLE:
      case INACTIVE:
        return Json.failed(status.getDescription(), user.email);
      default: {
        return Json.failed("系统错误", user.email);
      }
    }

    WebSession session = getSession(request);
    // login success
    session.setAttribute(BlogConstant.USER_INFO, loginUser);

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
    session.setAttribute(BlogConstant.BLOGGER_INFO, blogger);
    loginUser.setBlogger(true);
  }

  //---------------------------------------------------------------------
  // 修改当前用户的信息
  //---------------------------------------------------------------------

  // 第三方

  @GET("/{app}")
  @ResponseBody(false)
  public String loginRedirect(@PathVariable String app, String forward, WebSession session) {
    OauthMetadata metadata = getOauthMetadata(app);

    String state = HashUtils.getRandomHashString(16);
    session.setAttribute(BlogConstant.KEY_STATE, state);

    return metadata.getRedirect(state, forward);
  }

  OauthMetadata getOauthMetadata(String app) {
    if (oauthMetadata == null) {
      oauthMetadata = new HashMap<>(4);
      OauthMetadata github = new OauthMetadata();
      github.oauth = gitHubOauth;
      github.callback = "/api/auth/github/callback";
      github.accessTokenUrl = "https://github.com/login/oauth/access_token?client_id=";
      github.oauthUserFunction = accessTokenInfo -> {
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromHttpUrl("https://github.com")
                .query(accessTokenInfo)
                .build()
                .getQueryParams();

        String accessToken = queryParams.getFirst("access_token");
        HttpURLConnection connection = HttpUtils.getConnection("GET", "https://api.github.com/user");
        connection.setRequestProperty("Authorization", "token " + accessToken);
        log.debug("accessToken: [{}]", accessToken);
        return connection;
      };

      OauthMetadata gitee = new OauthMetadata();
      gitee.oauth = giteeOauth;
      gitee.callback = "/api/auth/gitee/callback";
      gitee.accessTokenUrl = "https://gitee.com/oauth/token?grant_type=authorization_code&client_id=";
      gitee.oauthUserFunction = accessTokenInfo -> {
        ObjectMapper sharedMapper = ObjectUtils.getSharedMapper();
        JsonNode jsonNode = sharedMapper.readTree(accessTokenInfo);
        String accessToken = jsonNode.get("access_token").asText();
        return HttpUtils.getConnection("GET", "https://gitee.com/api/v5/user?access_token=" + accessToken);
      };
      oauthMetadata.put("gitee", gitee);
      oauthMetadata.put("github", github);
    }

    OauthMetadata metadata = this.oauthMetadata.get(app);
    if (metadata == null) {
      throw ErrorMessageException.failed("不支持的登录方式");
    }
    return metadata;
  }

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

  @ResponseBody(false)
  @GET("/{app}/callback")
  @Logging(title = "第三方登录回调", content = "app:[${#app}]")
  public String loginCallback(WebSession session,
          @PathVariable String app, @Nullable String forward,
          @RequestParam String code, @RequestParam String state,
          @SessionAttribute(BlogConstant.KEY_STATE) String stateInSession) //
  {
    session.removeAttribute(BlogConstant.KEY_STATE);

    if (!state.equals(stateInSession)) {
      log.warn("第三方登录非法操作");
      return redirectLoginError(forward, "非法操作");
    }

    OauthUser oauthUser;
    try {
      oauthUser = getOauthMetadata(app).getOauthObjectDirectly(forward, code);
    }
    catch (Exception e) {
      return redirectLoginError(forward, e.getMessage());
    }

    String email = oauthUser.email;

    if (StringUtils.isEmpty(email)) {
      return redirectLoginError(forward, "Email 获取失败,请检 '" + app + "' 查邮箱设置,稍后再重试");
    }

    User user = userService.getByEmail(email);
    if (user == null) {
      user = new User()
              .setEmail(email)
              .setStatus(UserStatus.NORMAL)
              .setId(System.currentTimeMillis())
              .setSite(oauthUser.blog)
              .setName(oauthUser.name)
              .setType(BlogConstant.LOGIN_TYPE_GITHUB)
              .setIntroduce(oauthUser.bio)
              .setAvatar(oauthUser.avatar_url)
              .setBackground("/assets/images/bg/info_back.jpg");
      try {
        userService.register(user);
      }
      catch (Exception e) {
        log.error("[{}] 第三方注册失败", email, e);
        return redirectLoginError(forward, "登录出错，请稍后重试");
      }
    }

    UserStatus status = user.getStatus();
    switch (status) {
      case NORMAL -> {
        session.removeAttribute(BlogConstant.BLOGGER_INFO);
        session.setAttribute(BlogConstant.USER_INFO, user);
        return redirectLogin(forward);
      }
      case LOCKED, RECYCLE, INACTIVE -> {
        return redirectLoginError(forward, status.getDescription());
      }
      default -> {
        return redirectLoginError(forward, "登录出错，请稍后重试");
      }
    }
  }

  @FunctionalInterface
  interface OauthUserConnectionFunction {

    HttpURLConnection apply(String accessToken) throws IOException;
  }

  static String decode(String source) {
    return UriUtils.decode(source, StandardCharsets.UTF_8);
  }

  class OauthMetadata {
    Oauth oauth;
    String callback;
    String accessTokenUrl;

    OauthUserConnectionFunction oauthUserFunction;

    void appendRedirectUri(@Nullable String forward, StringBuilder url) {
      url.append("&redirect_uri=")
              .append(decode(blogConfig.getHost()))
              .append(decode(StringUtils.prependLeadingSlash(callback)));

      if (StringUtils.isNotEmpty(forward)) {
        url.append(decode("?forward="))
                .append(decode(forward));
      }
    }

    String getRedirect(String state, @Nullable String forward) {

      StringBuilder redirect = new StringBuilder(128)
              .append("redirect:")
              .append(oauth.getRedirect())
              .append(state);

      appendRedirectUri(forward, redirect);
      return redirect.toString();
    }

    String getAccessToken(@Nullable String forward, String code) {
      StringBuilder url = new StringBuilder(accessTokenUrl);
      url.append(oauth.getAppId())
              .append("&client_secret=")
              .append(oauth.getAppKey())
              .append("&code=")
              .append(code);

      appendRedirectUri(forward, url);
      try {
        return HttpUtils.getResponse("POST", url.toString());
      }
      catch (IOException e) {
        log.error("第三方接口调用失败", e);
        throw InternalServerException.failed("第三方接口调用失败", e);
      }
    }

    OauthUser getOauthObject(String accessToken) {
      try {
        HttpURLConnection userInfoConnection = oauthUserFunction.apply(accessToken);
        String userInfo = HttpUtils.getResponse(userInfoConnection);
        log.debug("userInfo: [{}]", userInfo);
        return ObjectUtils.fromJSON(userInfo, OauthUser.class);
      }
      catch (IOException e) {
        log.error("第三方接口调用失败", e);
        throw InternalServerException.failed("第三方接口调用失败");
      }
    }

    OauthUser getOauthObjectDirectly(@Nullable String forward, String code) {
      // access_token
      String accessToken = getAccessToken(forward, code);
      return getOauthObject(accessToken);
    }
  }

  static class OauthUser {
    public String name;
    public String blog;
    public String email;
    public String bio;
    public String avatar_url;
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

    userService.update(user);

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
  public void changePassword(@RequiresUser User loginUser,
          @RequestBody @Valid ChangePasswordForm form) {

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
    userService.update(user);
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
  public User changeEmailAndMobilePhone(
          @RequiresUser User loginUser, @Valid @RequestBody UserEmailForm form) {
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

      userService.update(user);

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

    userService.update(user);
    loginUser.setAvatar(uri);

    return loginUser;
  }

}
