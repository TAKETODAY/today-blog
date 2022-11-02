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

import com.aliyun.oss.ClientException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;

import cn.taketoday.blog.config.AttachmentConfig;
import cn.taketoday.blog.model.Attachment;
import cn.taketoday.blog.model.enums.FileType;
import cn.taketoday.blog.repository.AttachmentRepository;
import cn.taketoday.blog.utils.FileUtils;
import cn.taketoday.blog.utils.RemoteFileOperations;
import cn.taketoday.blog.utils.StringUtils;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Singleton;
import cn.taketoday.transaction.annotation.Transactional;
import cn.taketoday.web.InternalServerException;
import cn.taketoday.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/12 19:03
 */
@Singleton
@RequiredArgsConstructor
public class AttachmentOperations {
  private final AttachmentConfig attachmentConfig;
  private final AttachmentRepository repository;
  private final RemoteFileOperations ossOperations;

  public AttachmentConfig getConfig() {
    return attachmentConfig;
  }

  @Nullable
  public Attachment getById(long id) {
    return repository.findById(id);
  }

  public Optional<Attachment> fetch(long id) {
    return Optional.ofNullable(getById(id));
  }

  public void persist(Attachment attachment) {
    repository.save(attachment);
  }

  @Nullable
  @Transactional
  public Attachment removeById(long attachId) {
    Attachment attachment = getById(attachId);
    if (attachment == null) {
      return null;
    }
    // 先删除数据库，可以回滚
    repository.deleteById(attachId);

    // base
    String outsideResource = attachmentConfig.getOutsideResource();
    if (StringUtils.isNotEmpty(outsideResource)) {
      File file = attachmentConfig.getLocalFile(attachment);
      try {
        FileUtils.deleteFolder(file);
      }
      catch (IOException ignored) { }
    }

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

  public Attachment upload(MultipartFile file) {
    return upload(file, null);
  }

  public Attachment upload(MultipartFile file, @Nullable String suffix) {
    if (ossOperations.isOssEnabled()) {
      return attachOssUpload(file, suffix);
    }
    return attachLocalUpload(file, suffix);
  }

  @Transactional
  public void uploadOSS(Attachment attachment) {
    attachment.setSync(true);
    repository.update(attachment);
    File dest = attachmentConfig.getLocalFile(attachment);
    ossOperations.uploadFile(attachment.getLocation(), dest);
  }

  @Transactional
  public void deleteOSS(Attachment attachment) {
    attachment.setSync(false);
    repository.update(attachment);

    String location = attachment.getLocation();
    ossOperations.removeFile(location);
  }

  public Attachment attachOssUpload(MultipartFile file, @Nullable String suffix) {
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
    attachment.setFileType(FileType.from(fileName));
    return attachment;
  }

}
