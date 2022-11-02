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
package cn.taketoday.blog.service;

import net.coobird.thumbnailator.Thumbnails;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import cn.taketoday.blog.Pageable;
import cn.taketoday.blog.config.BlogConfig;
import cn.taketoday.blog.config.OssConfig;
import cn.taketoday.blog.model.Attachment;
import cn.taketoday.blog.model.enums.FileType;
import cn.taketoday.blog.model.form.AttachmentForm;
import cn.taketoday.blog.repository.AttachmentRepository;
import cn.taketoday.blog.utils.FileUtils;
import cn.taketoday.blog.utils.Pagination;
import cn.taketoday.blog.utils.RemoteFileOperations;
import cn.taketoday.blog.utils.StringUtils;
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
  private final BlogConfig blogConfig;
  private final RemoteFileOperations fileOperations;
  private final AttachmentRepository attachmentRepository;

  public AttachmentService(BlogConfig blogConfig,
          AttachmentRepository repository, OssConfig ossConfig) {
    this.blogConfig = blogConfig;
    this.attachmentRepository = repository;
    this.fileOperations = new RemoteFileOperations(ossConfig);
  }

  /**
   * 新增附件信息
   *
   * @param attachment attachment
   * @return Attachment
   */

  public Attachment save(Attachment attachment) {
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

    // base
    String upload = blogConfig.getUpload();
    if (StringUtils.isNotEmpty(upload)) {
      String location = attachment.getLocation();
      File file = new File(upload, location);
      try {
        FileUtils.deleteFolder(file);
      }
      catch (IOException ignored) { }

      // 删除缩略图
      if (FileUtils.isImage(attachment)) {
        File thumb = new File(upload, computeThumbLocation(location));
        try {
          FileUtils.deleteFolder(thumb);
        }
        catch (IOException ignored) { }
      }
    }

    // 删除远程OSS
    if (Objects.equals(Boolean.TRUE, attachment.getSync())) {
      try {
        String location = attachment.getLocation();
        fileOperations.removeFile(location);
        fileOperations.removeFile(computeThumbLocation(location));
      }
      catch (RuntimeException e) {
        throw InternalServerException.failed("OSS 删除失败", e);
      }
    }
    return attachment;
  }

  protected String computeThumbLocation(String location) {
    int index = location.indexOf('.');
    if (index > -1) {
      StringBuilder builder = new StringBuilder(location);
      builder.insert(index, "-thumb");
      return builder.toString();
    }
    return location.concat("-thumb.png");
  }

  @Transactional
  public void uploadAliyun(Attachment attachment) {
    attachment.setSync(true);
    update(attachment);
    String location = attachment.getLocation();
    File dest = new File(blogConfig.getUpload(), location);

    fileOperations.uploadFile(attachment.getLocation(), dest);
  }

  @Transactional
  public void deleteAliyun(Attachment attachment) {
    attachment.setSync(false);
    update(attachment);

    String location = attachment.getLocation();
    fileOperations.removeFile(location);
  }

  public Attachment upload(MultipartFile file, String suffix) {
    if (fileOperations.isOssEnabled()) {
      return attachAliyunUpload(file, suffix);
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
    String uploadUrl = FileUtils.getUploadFilePath(fileName); // /upload/image/2019/3/10/1.jpg

    try {
      File dest = saveFile(file, uploadUrl);
      Attachment attachment = createAttachment(fileName, uploadUrl, dest);
      // 存缩略图
      if (FileUtils.isImage(attachment)) {
        File thumbFile = new File(blogConfig.getUpload(), computeThumbLocation(uploadUrl));
        // 压缩图片
        scale(dest, thumbFile);
      }
      attachment.setSync(false);
      save(attachment);
      return attachment;
    }
    catch (IOException e) {
      throw new InternalServerException("本地附件保存失败", e);
    }
  }

  private File saveFile(MultipartFile file, String uploadUrl) throws IOException {
    File dest = new File(blogConfig.getUpload(), uploadUrl);
    file.transferTo(dest);
    return dest;
  }

  public Attachment attachAliyunUpload(MultipartFile file, String suffix) {
    String fileName = getName(file, suffix);
    String uploadUrl = FileUtils.getUploadFilePath(fileName); // /upload/image/2019/3/10/1.jpg

    try {
      File destFile = saveFile(file, uploadUrl);
      Attachment attachment = createAttachment(fileName, uploadUrl, destFile);
      fileOperations.uploadFile(uploadUrl, destFile);

      // 存缩略图
      if (FileUtils.isImage(attachment)) {
        String thumbUploadPath = computeThumbLocation(uploadUrl);
        File thumbFile = new File(blogConfig.getUpload(), thumbUploadPath);
        // 压缩图片
        scale(destFile, thumbFile);

        fileOperations.uploadFile(thumbUploadPath, thumbFile);
      }
      // 阿里云
      attachment.setSync(true);
      save(attachment);
      return attachment;
    }
    catch (IOException e) {
      throw new InternalServerException("本地附件保存失败", e);
    }
  }

  private void scale(File srcFile, File thumbFile) throws IOException {
    Thumbnails.of(srcFile)
            .useOriginalFormat()
            .scale(0.5)
            .toFile(thumbFile);
  }

  private String getName(MultipartFile file, String suffix) {
    if (StringUtils.isNotEmpty(suffix)) {
      return suffix;
    }
    return file.getOriginalFilename();
  }

  public Attachment createAttachment(String fileName, String uploadUrl, File dest) {
    return new Attachment()
            .setId(System.currentTimeMillis())
            .setUri(uploadUrl)
            .setName(fileName)
            .setLocation(uploadUrl)
            .setSize(dest.length())
            .setFileType(FileType.from(fileName));
  }

  public List<Attachment> getLatest() {
    return attachmentRepository.findLatest();
  }

  public List<Attachment> getAll(Pageable pageable) {
    return getAll(pageable.getCurrent(), pageable.getSize());
  }

}
