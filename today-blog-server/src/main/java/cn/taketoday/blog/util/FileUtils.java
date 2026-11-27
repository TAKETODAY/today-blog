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

package cn.taketoday.blog.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import cn.taketoday.blog.model.Attachment;
import cn.taketoday.blog.model.enums.AttachmentType;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ObjectUtils;
import infra.util.StringUtils;
import infra.web.server.InternalServerException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
public abstract class FileUtils {

  private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

  public static String getUploadFilePath(String name) {
    LocalDateTime now = LocalDateTime.now();
    return new StringBuilder("/upload/")
            .append(now.getYear())
            .append('/')
            .append(now.getMonthValue())
            .append('/')
            .append(now.getDayOfMonth())
            .append('/')
            .append(name)
            .toString();
  }

  public static String getUploadFilePath(String prefix, String name) {
    final LocalDate now = LocalDate.now();
    return new StringBuilder(prefix)
            .append(now.getYear())
            .append('/')
            .append(now.getMonthValue())
            .append('/')
            .append(now.getDayOfMonth())
            .append('/')
            .append(name)
            .toString();
  }

  public static String getRandomUploadFilePath(String prefix, String name) {
    final LocalDate now = LocalDate.now();
    return new StringBuilder(prefix)
            .append(now.getYear())
            .append('/')
            .append(now.getMonthValue())
            .append('/')
            .append(now.getDayOfMonth())
            .append('/')
            .append(HashUtils.getRandomHashString(8))
            .append(name)
            .toString();
  }

  public static String getRandomUploadFilePath(String name) {
    final LocalDate now = LocalDate.now();
    return new StringBuilder()
            .append(now.getYear())
            .append('/')
            .append(now.getMonthValue())
            .append('/')
            .append(now.getDayOfMonth())
            .append('/')
            .append(HashUtils.getRandomHashString(8))
            .append(name)
            .toString();
  }

  public static String getRandomUploadFilePath() {
    final LocalDate now = LocalDate.now();
    return new StringBuilder()
            .append(now.getYear())
            .append('/')
            .append(now.getMonthValue())
            .append('/')
            .append(now.getDayOfMonth())
            .append('/')
            .append(HashUtils.getRandomHashString(16))
            .toString();
  }

  //

  public static void deleteFolder(File folder) throws IOException {
    if (folder.isDirectory()) {
      String[] files = folder.list();
      if (ObjectUtils.isNotEmpty(files)) {
        for (String file : files) {
          deleteFolder(new File(folder, file));
        }
      }
    }
    Files.deleteIfExists(folder.toPath());
  }

  public static void copyFolder(File src, File dest) throws IOException {
    if (src.isDirectory()) {
      mkdirIfNecessary(dest);
      String[] files = src.list();
      if (ObjectUtils.isNotEmpty(files)) {
        for (String file : files) {
          File srcFile = new File(src, file);
          File destFile = new File(dest, file);
          // 递归复制
          copyFolder(srcFile, destFile);
        }
      }
    }
    else {
      transferFile(src, dest);
    }
  }

  public static void mkdirIfNecessary(final File dest) {
    if (!dest.exists() && !dest.mkdir()) {
      throw failed("服务器出错，文件夹创建失败");
    }
  }

  public static void createFileIfNecessary(final File file) {
    try {
      if (!file.exists() && !file.createNewFile()) {
        throw failed("服务器出错，文件创建失败");
      }
    }
    catch (IOException e) {
      throw failed("服务器出错，文件创建失败");
    }
  }

  static InternalServerException failed(String message) {
    return new InternalServerException(message);
  }

  /**
   * @param srcDir 压缩文件夹路径
   * @param outDir 压缩文件输出流
   */
  public static void toZip(String[] srcDir, String outDir) throws IOException {
    try (OutputStream out = new FileOutputStream(new File(outDir))) {
      toZip(out, getFiles(srcDir));
    }
  }

  /**
   * @param srcDir 压缩文件夹路径
   * @param out 压缩文件输出流
   * @throws RuntimeException 压缩失败会抛出运行时异常
   */
  public static void toZip(final String srcDir, final OutputStream out) throws IOException {
    toZip(new File(srcDir), out);
  }

  public static void toZip(final File sourceFile, final OutputStream out) throws IOException {
    try (ZipOutputStream zos = new ZipOutputStream(out)) {
      doCompress(zos, sourceFile, sourceFile.getName());
    }
  }

  public static void toZip(final OutputStream out, final File... srcDir) throws IOException {
    try (ZipOutputStream zos = new ZipOutputStream(out)) {
      compress(zos, srcDir);
    }
  }

  public static void toZip(final OutputStream out, final String... srcDir) throws IOException {
    toZip(out, getFiles(srcDir));
  }

  private static File[] getFiles(final String[] srcDir) {
    final int length = srcDir.length;
    final File[] src = new File[length];
    for (int i = 0; i < length; i++) {
      src[i] = new File(srcDir[i]);
    }
    return src;
  }

  public static void compress(ZipOutputStream zos, File... sourceFileList) throws IOException {
    for (File sourceFile : sourceFileList) {
      doCompress(zos, sourceFile, sourceFile.getName());
    }
  }

  public static void compress(File sourceFile, ZipOutputStream zos) throws IOException {
    doCompress(zos, sourceFile, sourceFile.getName());
  }

  private static void doCompress(final ZipOutputStream zos, final File sourceFile, final String name) throws IOException {
    if (sourceFile.isFile()) { // 文件压缩
      zos.putNextEntry(new ZipEntry(name));
      transferFile(sourceFile, zos);
      zos.closeEntry();
    }
    else { // 文件夹
      File[] listFiles = sourceFile.listFiles();
      if (ObjectUtils.isEmpty(listFiles)) {
        zos.putNextEntry(new ZipEntry(name + '/'));
        zos.closeEntry();
      }
      else {
        for (File file : listFiles) {
          doCompress(zos, file, name + '/' + file.getName());
        }
      }
    }
  }

  /**
   * 传输文件到指定流
   */
  public static void transferFile(final File sourceFile, final OutputStream out) throws IOException {
    try (FileInputStream in = new FileInputStream(sourceFile)) {
      StreamUtils.transferTo(in, out);
    }
  }

  public static void transferFile(final File sourceFile, final File destFile) throws IOException {
    if (!destFile.exists() && !destFile.createNewFile()) {
      throw failed("目标文件创建失败");
    }
    try (FileOutputStream out = new FileOutputStream(destFile)) {
      transferFile(sourceFile, out);
    }
  }

  public static void transferFile(final String sourceFile, final String destFile) throws IOException {
    transferFile(new File(sourceFile), new File(destFile));
  }

  //

  public static boolean isImage(final Attachment attachment) {
    return attachment.getFileType() == AttachmentType.IMAGE;
  }

  public static void zip(Path file, Path destination) throws IOException {
    try (InputStream input = Files.newInputStream(file);
            ZipOutputStream archive = new ZipOutputStream(Files.newOutputStream(destination))) {
      archive.setMethod(ZipEntry.DEFLATED);
      archive.setLevel(Deflater.BEST_COMPRESSION);

      archive.putNextEntry(new ZipEntry(file.getFileName().toString()));
      infra.util.StreamUtils.copy(input, archive);

      archive.closeEntry();
    }
  }

  public static void unzip(Path srcPath, Path saveDir) throws IOException {
    try (InputStream in = Files.newInputStream(srcPath)) {
      unzip(in, saveDir);
    }
  }

  public static void unzip(InputStream in, Path saveDir) throws IOException {
    try (ZipInputStream zais = new ZipInputStream(in)) {
      ZipEntry zipEntry;
      while ((zipEntry = zais.getNextEntry()) != null) {
        String entryFileName = zipEntry.getName();
        var entryFilePath = saveDir.resolve(entryFileName).normalize();
        if (zipEntry.isDirectory()) {
          Files.createDirectories(entryFilePath);
        }
        else {
          Path parent = entryFilePath.getParent();
          if (Files.notExists(parent)) {
            Files.createDirectories(parent);
          }
          try (OutputStream os = Files.newOutputStream(entryFilePath)) {
            infra.util.StreamUtils.copy(zais, os);
          }
        }
      }
    }
  }

  public static void copyDirectory(Path sourceDir, Path targetDir) throws IOException {
    // 确保目标文件夹存在
    Files.createDirectories(targetDir);
    // 遍历源文件夹中的文件和目录
    try (var stream = Files.list(sourceDir)) {
      for (Path path : stream.toList()) {
        Path newPath = targetDir.resolve(path.getFileName());
        if (Files.isDirectory(path)) {
          // 如果是目录，则递归调用
          copyDirectory(path, newPath);
        }
        else {
          // 如果是文件，则复制文件
          Files.copy(path, newPath, StandardCopyOption.REPLACE_EXISTING);
        }
      }
    }
  }

  public static void moveDirectory(Path sourceDir, Path targetDir) throws IOException {
    Files.createDirectories(targetDir);
    Files.move(sourceDir, targetDir, StandardCopyOption.REPLACE_EXISTING);
  }

  public static String getFilename(String location) {
    location = location.replace('\\', '/');
    return StringUtils.getFilename(location);
  }

}
