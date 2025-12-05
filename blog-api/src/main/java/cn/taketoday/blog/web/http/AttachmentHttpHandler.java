/*
 * Copyright 2017 - 2025 the original author or authors.
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

import cn.taketoday.blog.config.AttachmentConfig;
import cn.taketoday.blog.log.Logging;
import cn.taketoday.blog.model.Attachment;
import cn.taketoday.blog.model.form.AttachmentForm;
import cn.taketoday.blog.service.AttachmentService;
import cn.taketoday.blog.web.ErrorMessageException;
import cn.taketoday.blog.web.Pageable;
import cn.taketoday.blog.web.Pagination;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import infra.http.HttpStatus;
import infra.web.annotation.DELETE;
import infra.web.annotation.GET;
import infra.web.annotation.POST;
import infra.web.annotation.PUT;
import infra.web.annotation.PathVariable;
import infra.web.annotation.PutMapping;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.ResponseStatus;
import infra.web.annotation.RestController;
import infra.web.multipart.Part;
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
class AttachmentHttpHandler {

  private final AttachmentConfig attachmentConfig;

  private final AttachmentService attachmentService;

  @GET
  public Pagination<Attachment> filter(AttachmentForm form, Pageable pageable) {
    return attachmentService.filter(form, pageable);
  }

  /**
   * 上传文件
   */
  @POST
  @ResponseStatus(HttpStatus.CREATED)
  @Logging(title = "上传附件", content = "文件名: [#{#file.originalFilename}]")
  public UploadReturnValue upload(Part file) {
    Attachment attachment = attachmentService.upload(file, null);
    String uri = attachment.getUri();
    return new UploadReturnValue(uri, attachmentConfig.getRemoteURL(uri));
  }

  public record UploadReturnValue(String uri, String cdnURL) {

  }

  @GET("/{id}")
  public Attachment obtainById(@PathVariable long id) {
    Attachment attachment = attachmentService.getById(id);
    ErrorMessageException.notNull(attachment, "附件不存在");
    return attachment;
  }

  @PUT("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Logging(title = "更新附件", content = "更新附件:[#{#attachment.name}] 地址:[#{#attachment.uri}]")
  public void put(@RequestBody Attachment attachment, @PathVariable long id) {
    attachment.setId(id);
    attachmentService.updateById(attachment);
  }

  @DELETE("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Logging(title = "删除附件", content = "删除ID为: [#{#id}]的附件")
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
  @Logging(title = "同步附件到阿里云", content = "ID为: [#{#id}]")
  public void syncToAliyun(long id) {
    attachmentService.uploadOSS(id);
  }

  @PutMapping("/{id}/delete-aliyun")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Logging(title = "删除阿里云附件", content = "ID为: [#{#id}]")
  public void deleteAliyun(long id) {
    attachmentService.deleteOSS(id);
  }

}
