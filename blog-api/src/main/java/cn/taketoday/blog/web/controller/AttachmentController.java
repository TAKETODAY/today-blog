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

import cn.taketoday.blog.Pageable;
import cn.taketoday.blog.aspect.Logger;
import cn.taketoday.blog.model.Attachment;
import cn.taketoday.blog.model.form.AttachmentForm;
import cn.taketoday.blog.service.AttachmentService;
import cn.taketoday.blog.utils.Pagination;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.web.NotFoundException;
import cn.taketoday.web.annotation.DELETE;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.POST;
import cn.taketoday.web.annotation.PUT;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.PutMapping;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.multipart.MultipartFile;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-03-28 19:58
 */
@CustomLog
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/attachments")
@RequiresBlogger
public class AttachmentController {

  private final AttachmentService attachmentService;

  @GET
  public Pagination<Attachment> filter(AttachmentForm form, Pageable pageable) {
    return attachmentService.filter(form, pageable);
  }

  /**
   * Upload file
   */
  @POST
  @ResponseStatus(HttpStatus.CREATED)
  @Logger(value = "上传附件", content = "文件名: [${file.getFileName()}]")
  public Attachment upload(MultipartFile file) {
    return attachmentService.upload(file, null);
  }

  @GET("/{id}")
  public Attachment obtainById(@PathVariable long id) {
    Attachment attachment = attachmentService.getById(id);
    NotFoundException.notNull(attachment, "附件不存在");
    return attachment;
  }

  @PUT("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Logger(value = "更新附件", content = "更新附件:[${attachment.name}] 地址:[${root.args[0].url}]")
  public void put(@RequestBody Attachment attachment, @PathVariable long id) {
    attachmentService.update(attachment.setId(id));
  }

  @DELETE("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Logger(value = "删除附件", content = "删除id为:[${root.args[0]}]的附件")
  public void delete(@PathVariable long id) {
    Attachment attachment = attachmentService.removeById(id);
    if (attachment != null && log.isInfoEnabled()) {
      log.info("删除附件 [{}] 成功!", attachment.getName());
    }
  }

  /**
   * Upload aliyun
   */
  @PutMapping("/{id}/sync-aliyun")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Logger(value = "同步附件到阿里云", content = "ID为: [${id}]")
  public void syncToAliyun(long id) {
    final Attachment byId = obtainById(id);
    attachmentService.uploadAliyun(byId);
  }

  @PutMapping("/{id}/delete-aliyun")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Logger(value = "删除阿里云附件", content = "ID为: [${id}]")
  public void deleteAliyun(long id) {
    final Attachment byId = obtainById(id);
    attachmentService.deleteAliyun(byId);
  }

}
