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

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import cn.taketoday.beans.support.BeanProperties;
import cn.taketoday.blog.ErrorMessageException;
import cn.taketoday.blog.Json;
import cn.taketoday.blog.Pageable;
import cn.taketoday.blog.Pagination;
import cn.taketoday.blog.log.Logging;
import cn.taketoday.blog.model.Attachment;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.model.enums.UserStatus;
import cn.taketoday.blog.service.AttachmentService;
import cn.taketoday.blog.service.UserService;
import cn.taketoday.blog.util.BlogUtils;
import cn.taketoday.blog.util.HashUtils;
import cn.taketoday.blog.util.MD5;
import cn.taketoday.blog.util.StringUtils;
import cn.taketoday.blog.web.interceptor.NoRequestLimit;
import cn.taketoday.blog.web.interceptor.RequestLimit;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.web.annotation.DELETE;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.POST;
import cn.taketoday.web.annotation.PUT;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-04-09 15:56
 */
@RestController
@RequestLimit(count = 1, timeUnit = TimeUnit.MINUTES)
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;
  private final AttachmentService attachmentService;

  // ------------------------ api

  @GET
  @NoRequestLimit
  @RequiresBlogger
  public Pagination<User> get(Pageable pageable) {
    int rowCount = userService.count();
    assertFound(pageable, rowCount);
    return Pagination.ok(userService.get(pageable), rowCount, pageable);
  }

  protected void assertFound(Pageable pageable, int rowCount) {

    if (BlogUtils.notFound(pageable.getCurrent(), BlogUtils.pageCount(rowCount, pageable.getSize()))) {
      throw ErrorMessageException.failed("分页不存在");
    }
  }

  @PUT("/{id}")
  @NoRequestLimit
  @RequiresBlogger
  public Json update(@PathVariable long id, @Valid @RequestBody UserSettingsForm form) {

    User oldUser = userService.getById(id);
    User user = new User();
    boolean change = false;
    if (!Objects.equals(form.name, oldUser.getName())) {
      user.setName(form.name);
      change = true;
    }
    if (!Objects.equals(form.site, oldUser.getSite())) {
      user.setSite(form.site);
      change = true;
    }
    if (!Objects.equals(form.introduce, oldUser.getIntroduce())) {
      user.setIntroduce(form.introduce);
      change = true;
    }
    if (form.notification != oldUser.getNotification()) {
      user.setNotification(form.notification);
      change = true;
    }

    if (change) {
      user.setId(oldUser.getId());
      userService.update(user);
      return Json.ok("资料修改成功");
    }
    return Json.failed("资料未更改");
  }

  /**
   * save article
   */
  @POST
  @NoRequestLimit
  @RequiresBlogger
  @Logging(title = "创建用户", content = "user: [${#user.name}]")
  public Json create(@RequestBody User user) {

    userService.register(user);

    return Json.ok("创建成功");
  }

  @PUT("/{id}/status/{status}")
  @NoRequestLimit
  @RequiresBlogger
  @Logging(title = "更新用户状态", content = "更新用户：[${#id}] 状态为：[${#status.description}]")
  public Json status(@PathVariable long id, @PathVariable UserStatus status) {
    userService.updateStatusById(status, id);

    return Json.ok("状态更新成功");
  }

  @DELETE("/{id}")
  @NoRequestLimit
  @RequiresBlogger
  @Logging(title = "删除用户", content = "删除用户 : [${#id}]")
  public Json delete(@PathVariable long id) {

    userService.deleteById(id);
    return Json.ok("删除成功");
  }

  // --------------------------------- settings

  /**
   * change image
   */
  @POST("/settings/avatar")
  @Logging(title = "用户保存头像", content =
          "上传:[${#avatar.getOriginalFilename()}] email:[${#loginUser.email}]")
  public Json avatar(User loginUser, MultipartFile avatar) {

    Attachment attachment = attachmentService.upload(
            avatar, StringUtils.getRandomImageName(avatar.getOriginalFilename()));

    String path = attachment.getUri();

    if (!path.equals(loginUser.getAvatar())) { // not equals
      userService.update(new User(loginUser.getId()).setAvatar(path));
      loginUser.setAvatar(path);
    }
    return Json.ok("修改成功", path);
  }

  /**
   * Change background image
   *
   * @param loginUser login user
   */
  @POST("/settings/background")
  @Logging(title = "用户修改背景", content =
          "文件名: [${#background.getOriginalFilename()}] 邮箱:[${#loginUser.email}]")
  public Json background(User loginUser, MultipartFile background) {
    Attachment attachment = attachmentService.upload(
            background, StringUtils.getRandomImageName(background.getOriginalFilename()));

    String path = attachment.getUri();
    if (!path.equals(loginUser.getBackground())) { // not equals
      userService.update(new User(loginUser.getId()).setBackground(path));
      loginUser.setBackground(path);
    }
    return Json.ok("修改成功", path);
  }

  @Setter
  public static class UserSettingsForm {
    @NotEmpty(message = "请输入姓名或昵称")
    private String name;
    private String site;
    private String introduce = "暂无介绍";
    private boolean notification = false;
  }

  @PUT("/settings")
  @Logging(title = "用户更新资料", content = "邮箱: [${#loginUser.email}]")
  public Json settings(User loginUser, @Valid @RequestBody UserSettingsForm form) {
    User user = new User();
    boolean change = false;
    if (!Objects.equals(form.name, loginUser.getName())) {
      user.setName(form.name);
      change = true;
    }
    if (!Objects.equals(form.site, loginUser.getSite())) {
      user.setSite(form.site);
      change = true;
    }
    if (!Objects.equals(form.introduce, loginUser.getIntroduce())) {
      user.setIntroduce(form.introduce);
      change = true;
    }
    if (form.notification != loginUser.getNotification()) {
      user.setNotification(form.notification);
      change = true;
    }

    if (change) {
      user.setId(loginUser.getId());
      userService.update(user);
      user.setBlogger(loginUser.isBlogger()); // @since 3.0 修复 更新后权限改变

      BeanProperties.copy(user, loginUser);
      return Json.ok("资料修改成功", loginUser);
    }
    return Json.failed("资料未更改");
  }

  @Setter
  public static class UserPasswordForm {
    @NotEmpty(message = "请输入旧密码")
    String password;
    @NotEmpty(message = "请输入新密码")
    String newPassword;
    @NotEmpty(message = "请重复新密码")
    String rePassword;
  }

  /**
   * Change User's Password
   */

  @PUT("/settings/password")
  @Logging(title = "用户修改密码", content = "邮箱 :[${#userInfo.email}]")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void changePasswd(User userInfo, @Valid @RequestBody UserPasswordForm form) {

    if (!Objects.equals(form.newPassword, form.rePassword)) {
      // repasswd != passwd
      throw ErrorMessageException.failed("两次密码不一致");
    }

    MD5 md5 = new MD5();
    String prePassword = md5.getMD5Str(md5.getMD5Str(form.password));
    if (!Objects.equals(prePassword, userInfo.getPassword())) {
      // Previous Password Incorrect.
      throw ErrorMessageException.failed("先前的密码不正确");
    }

    // Previous Password Correct. Continue To Change Password.
    String newPassword = md5.getMD5Str(md5.getMD5Str(form.newPassword));
    if (Objects.equals(newPassword, userInfo.getPassword())) {
      throw ErrorMessageException.failed("密码未更改");
    }

    userService.update(new User(userInfo.getId()).setPassword(newPassword));

    userInfo.setPassword(newPassword);
  }

  @Setter
  public static class UserEmailForm {
    @NotEmpty(message = "请输入新邮箱")
    String email;
    @NotEmpty(message = "请输入密码")
    String password;
  }

  /**
   * Change User's Email
   */
  @PUT("/settings/email")
  @Logging(title = "用户修改邮箱", content = "邮箱 :[${#userInfo.email}] 新邮箱 :[${#form.email}]")
  public Json changeEmail(User userInfo, @Valid @RequestBody UserEmailForm form) {

    String email = form.email;
    if (Objects.equals(userInfo.getEmail(), email)) {
      return Json.failed("邮箱未更改");
    }

    // Check User's Password
    if (HashUtils.getEncodedPassword(form.password).equals(userInfo.getPassword())) {

      userService.update(new User(userInfo.getId()).setEmail(email));
      userInfo.setEmail(email);
      return Json.ok("邮箱修改成功", userInfo);
    } // Password Incorrect.
    return Json.failed("密码不正确");
  }

}
