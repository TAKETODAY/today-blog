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
package cn.taketoday.blog.service;

import com.aliyun.oss.ClientException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

import cn.taketoday.blog.Pageable;
import cn.taketoday.blog.Pagination;
import cn.taketoday.blog.config.AttachmentConfig;
import cn.taketoday.blog.config.BlogConfig;
import cn.taketoday.blog.config.OssConfig;
import cn.taketoday.blog.model.Attachment;
import cn.taketoday.blog.model.enums.AttachmentType;
import cn.taketoday.blog.model.form.AttachmentForm;
import cn.taketoday.blog.repository.AttachmentRepository;
import cn.taketoday.blog.util.FileUtils;
import cn.taketoday.blog.util.RemoteFileOperations;
import cn.taketoday.blog.util.StringUtils;
import cn.taketoday.stereotype.Service;
import cn.taketoday.transaction.annotation.Transactional;
import cn.taketoday.web.InternalServerException;
import cn.taketoday.web.multipart.MultipartFile;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-03-16 14:46
 */
@Service
public class AttachmentService {
  private final AttachmentConfig attachmentConfig;
  private final RemoteFileOperations ossOperations;
  private final AttachmentRepository attachmentRepository;

  public AttachmentService(AttachmentConfig attachmentConfig,
          AttachmentRepository repository, OssConfig ossConfig) {
    this.attachmentConfig = attachmentConfig;
    this.attachmentRepository = repository;
    this.ossOperations = new RemoteFileOperations(ossConfig);
  }

  /**
   * 新增附件信息
   *
   * @param attachment attachment
   * @return Attachment
   */
  public Attachment persist(Attachment attachment) {
    attachmentRepository.save(attachment);
    return attachment;
  }

  public int count() {
    return attachmentRepository.getTotalRecord();
  }

  public Pagination<Attachment> filter(AttachmentForm form, Pageable pageable) {
    int count = attachmentRepository.getRecordFilter(form);
    if (count < 1) {
      return Pagination.empty();
    }

    List<Attachment> rets = attachmentRepository.filter(
            form,
            getPageNow(pageable.getCurrent(), pageable.getSize()),
            pageable.getSize()
    );

    return Pagination.ok(rets, count, pageable);
  }

  protected int getPageNow(int pageNow, int pageSize) {
    return (pageNow - 1) * pageSize;
  }

  public List<Attachment> getAll(int pageNow, int pageSize) {
    return attachmentRepository.find((pageNow - 1) * pageSize, pageSize);
  }

  public Attachment getById(long id) {
    return attachmentRepository.findById(id);
  }

  public void update(Attachment model) {
    attachmentRepository.update(model);
  }

  /**
   * 根据编号移除附件
   *
   * @param attachId attachId
   * @return 旧附件
   */

  @Transactional
  public Attachment removeById(long attachId) {
    Attachment attachment = getById(attachId);
    if (attachment == null) {
      return null;
    }

    // 先删除数据库，可以回滚
    attachmentRepository.deleteById(attachId);

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

  public Attachment upload(MultipartFile file, String suffix) {
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
  public Attachment attachLocalUpload(MultipartFile file, String suffix) {
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
    // fix #3 Upload file not found exception
    File parentFile = dest.getParentFile();
    if (!parentFile.exists()) {
      parentFile.mkdirs();
    }
    /*
     * The uploaded file is being stored on disk
     * in a temporary location so move it to the
     * desired file.
     */
    if (dest.exists()) {
      Files.delete(dest.toPath());
    }
    file.transferTo(dest);
    return dest;
  }

  public Attachment attachOssUpload(MultipartFile file, String suffix) {
    String fileName = getName(file, suffix);
    String uploadUri = FileUtils.getUploadFilePath(fileName); // /upload/image/2019/3/10/1.jpg

    try {
      File destFile = saveFile(file, uploadUri);
      Attachment attachment = createAttachment(fileName, uploadUri, destFile);
      ossOperations.uploadFile(uploadUri, destFile);

      // OSS
      attachment.setSync(true);
      persist(attachment);
      return attachment;
    }
    catch (ClientException e) {
      throw new InternalServerException("附件保存失败", e);
    }
    catch (IOException e) {
      throw new InternalServerException("本地附件保存失败", e);
    }
  }

  private String getName(MultipartFile file, String suffix) {
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

  public List<Attachment> getLatest() {
    return attachmentRepository.findLatest();
  }

  public List<Attachment> getAll(Pageable pageable) {
    return getAll(pageable.getCurrent(), pageable.getSize());
  }

  @Transactional
  public void uploadOSS(Attachment attachment) {
    attachment.setSync(true);
    attachmentRepository.update(attachment);
    File dest = attachmentConfig.getLocalFile(attachment);
    ossOperations.uploadFile(attachment.getLocation(), dest);
  }

  @Transactional
  public void deleteOSS(Attachment attachment) {
    attachment.setSync(false);
    attachmentRepository.update(attachment);

    String location = attachment.getLocation();
    ossOperations.removeFile(location);
  }

}
