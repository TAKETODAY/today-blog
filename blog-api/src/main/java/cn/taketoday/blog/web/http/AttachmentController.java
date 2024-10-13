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

import cn.taketoday.blog.config.AttachmentConfig;
import cn.taketoday.blog.log.Logging;
import cn.taketoday.blog.model.Attachment;
import cn.taketoday.blog.model.form.AttachmentForm;
import cn.taketoday.blog.service.AttachmentService;
import cn.taketoday.blog.web.ErrorMessageException;
import cn.taketoday.blog.web.Pageable;
import cn.taketoday.blog.web.Pagination;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import cn.taketoday.http.HttpStatus;
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
class AttachmentController {

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
  public UploadReturnValue upload(MultipartFile file) {
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
