/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2024 All Rights Reserved.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.blog.service;

import com.aliyun.oss.ClientException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import cn.taketoday.blog.config.AttachmentConfig;
import cn.taketoday.blog.model.Attachment;
import cn.taketoday.blog.model.enums.AttachmentType;
import cn.taketoday.blog.model.form.AttachmentForm;
import cn.taketoday.blog.util.FileUtils;
import cn.taketoday.blog.util.OssOperations;
import cn.taketoday.blog.util.StringUtils;
import cn.taketoday.blog.web.ErrorMessageException;
import cn.taketoday.blog.web.Pageable;
import cn.taketoday.blog.web.Pagination;
import cn.taketoday.jdbc.RepositoryManager;
import cn.taketoday.jdbc.persistence.EntityManager;
import cn.taketoday.jdbc.persistence.Page;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Service;
import cn.taketoday.transaction.annotation.Transactional;
import cn.taketoday.web.InternalServerException;
import cn.taketoday.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

/**
 * 附件服务
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-03-16 14:46
 */
@Service
@RequiredArgsConstructor
public class AttachmentService {

  private final EntityManager entityManager;

  private final OssOperations ossOperations;

  private final AttachmentConfig attachmentConfig;

  private final RepositoryManager repositoryManager;

  /**
   * 新增附件信息
   *
   * @param attachment attachment
   */
  public void persist(Attachment attachment) {
    repositoryManager.persist(attachment);
  }

  @Nullable
  public Attachment getById(long id) {
    return entityManager.findById(Attachment.class, id);
  }

  public Optional<Attachment> fetch(long id) {
    return Optional.ofNullable(getById(id));
  }

  /**
   * 获取全部附件数量
   */
  public int count() {
    return entityManager.count(Attachment.class).intValue();
  }

  public Pagination<Attachment> filter(AttachmentForm form, Pageable pageable) {
    return Pagination.from(entityManager.page(Attachment.class, form, pageable));
  }

  public void updateById(Attachment model) {
    entityManager.updateById(model);
  }

  /**
   * 根据编号移除附件
   *
   * @param attachId attachId
   * @return 旧附件
   */
  @Nullable
  @Transactional
  public Attachment removeById(long attachId) {
    Attachment attachment = getById(attachId);
    if (attachment == null) {
      return null;
    }

    // 先删除数据库，可以回滚
    entityManager.delete(Attachment.class, attachId);

    File file = attachmentConfig.getLocalFile(attachment);
    try {
      FileUtils.deleteFolder(file);
    }
    catch (IOException ignored) { }

    // 删除远程OSS

    if (Objects.equals(Boolean.TRUE, attachment.getSync())) {
      try {
        String location = attachment.getLocation();
        ossOperations.removeFile(location);
      }
      catch (RuntimeException e) {
        throw InternalServerException.failed("OSS 删除失败", e);
      }
    }

    return attachment;
  }

  public Attachment upload(MultipartFile file, @Nullable String suffix) {
    if (ossOperations.isOssEnabled()) {
      return attachOssUpload(file, suffix);
    }
    return attachLocalUpload(file, suffix);
  }

  /**
   * 原生服务器上传
   *
   * @param file file
   * @return Map
   */
  public Attachment attachLocalUpload(MultipartFile file, @Nullable String suffix) {
    String fileName = getName(file, suffix);
    String uploadUri = FileUtils.getUploadFilePath(fileName); // /upload/image/2019/3/10/1.jpg

    try {
      File dest = saveFile(file, uploadUri);
      Attachment attachment = createAttachment(fileName, uploadUri, dest);
      attachment.setSync(false);
      persist(attachment);
      return attachment;
    }
    catch (IOException e) {
      throw new InternalServerException("本地附件保存失败", e);
    }
  }

  private File saveFile(MultipartFile file, String uploadUrl) throws IOException {
    File dest = attachmentConfig.getLocalFile(uploadUrl);
    file.transferTo(dest);
    return dest;
  }

  public Attachment attachOssUpload(MultipartFile file, @Nullable String suffix) {
    String fileName = getName(file, suffix);
    String uploadUri = FileUtils.getUploadFilePath(fileName); // /upload/image/2019/3/10/1.jpg

    try {
      File destFile = saveFile(file, uploadUri);
      Attachment attachment = createAttachment(fileName, uploadUri, destFile);
      repositoryManager.executeWithoutResult(status -> {
        attachment.setSync(true);
        persist(attachment);

        ossOperations.uploadFile(uploadUri, destFile);
      });
      return attachment;
    }
    catch (ClientException e) {
      throw new InternalServerException("附件OSS保存失败", e);
    }
    catch (IOException e) {
      throw new InternalServerException("本地附件保存失败", e);
    }
  }

  private String getName(MultipartFile file, @Nullable String suffix) {
    if (StringUtils.isNotEmpty(suffix)) {
      return suffix;
    }
    return file.getOriginalFilename();
  }

  public Attachment createAttachment(String fileName, String uploadUri, File dest) {
    Attachment attachment = new Attachment();

    attachment.setUri(uploadUri);
    attachment.setName(fileName);
    attachment.setLocation(uploadUri);
    attachment.setSize(dest.length());
    attachment.setFileType(AttachmentType.from(fileName));
    return attachment;
  }

  public Page<Attachment> pageable(Pageable pageable) {
    return entityManager.page(Attachment.class, pageable);
  }

  @Transactional
  public void uploadOSS(long id) {
    Attachment attachment = obtainById(id);

    if (!attachment.isSynchronizedOSS()) {
      Attachment update = new Attachment();
      update.setId(id);
      update.setSync(true);
      // 数据库删除之后文件没有删除可以回滚
      entityManager.updateById(update);

      File dest = attachmentConfig.getLocalFile(attachment);
      ossOperations.uploadFile(attachment.getLocation(), dest);
    }
    else {
      throw ErrorMessageException.failed("已经同步过啦");
    }
  }

  @Transactional
  public void deleteOSS(long id) {
    Attachment attachment = obtainById(id);

    if (attachment.isSynchronizedOSS()) {
      Attachment update = new Attachment();
      update.setId(id);
      update.setSync(false);
      // 数据库删除之后文件没有删除可以回滚
      entityManager.updateById(update);

      String location = attachment.getLocation();
      ossOperations.removeFile(location);
    }
  }

  private Attachment obtainById(long id) {
    Attachment attachment = getById(id);
    if (attachment == null) {
      throw ErrorMessageException.failed("附件不存在");
    }
    return attachment;
  }

}
