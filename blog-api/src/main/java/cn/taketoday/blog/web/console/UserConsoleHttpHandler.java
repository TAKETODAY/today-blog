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

package cn.taketoday.blog.web.console;

import org.jspecify.annotations.NullUnmarked;

import java.util.Objects;

import cn.taketoday.blog.log.Logging;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.model.enums.UserStatus;
import cn.taketoday.blog.service.AttachmentService;
import cn.taketoday.blog.service.UserService;
import cn.taketoday.blog.web.ErrorMessageException;
import cn.taketoday.blog.web.Pageable;
import cn.taketoday.blog.web.Pagination;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import infra.persistence.EntityManager;
import infra.persistence.Page;
import infra.web.annotation.DELETE;
import infra.web.annotation.GET;
import infra.web.annotation.POST;
import infra.web.annotation.PUT;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RequestParam;
import infra.web.annotation.RestController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-04-09 15:56
 */
@RestController
@RequiresBlogger
@RequiredArgsConstructor
@RequestMapping("/api/console/users")
class UserConsoleHttpHandler {

  private final UserService userService;

  private final EntityManager entityManager;

  private final AttachmentService attachmentService;

  // ------------------------ api

  @GET
  public Pagination<User> get(Pageable pageable) {
    Page<User> page = entityManager.page(User.class, pageable);
    return Pagination.from(page);
  }

  /**
   * save article
   */
  @POST
  @Logging(title = "创建用户", content = "user: [#{#user.name}]")
  public void create(@RequestBody User user) {
    userService.register(user);
  }

  @PUT(path = "/{id}", params = "status")
  @Logging(title = "更新用户状态", content = "更新用户：[#{#id}] 状态为：[#{#status.description}]")
  public void status(@PathVariable long id, @RequestParam UserStatus status) {
    userService.updateStatusById(status, id);
  }

  @DELETE("/{id}")
  @Logging(title = "删除用户", content = "删除用户 : [#{#id}]")
  public void delete(@PathVariable long id) {
    userService.deleteById(id);
  }

  @PUT("/{id}")
  public void update(@PathVariable long id, @Valid @RequestBody UserSettingsForm form) {
    User oldUser = userService.getById(id);
    ErrorMessageException.notNull(oldUser, "用户不存在");

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
      userService.updateById(user);
    }
    else {
      throw ErrorMessageException.failed("资料未更改");
    }
  }

  @NullUnmarked
  public static class UserSettingsForm {

    @NotEmpty(message = "请输入姓名或昵称")
    public String name;

    public String site;

    public String introduce = "暂无介绍";

    public boolean notification = false;
  }

}
