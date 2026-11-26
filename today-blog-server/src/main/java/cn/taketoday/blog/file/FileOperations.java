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

package cn.taketoday.blog.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import cn.taketoday.blog.util.ReturnValue;
import cn.taketoday.blog.util.Status;
import infra.core.io.FileSystemResource;
import infra.core.io.Resource;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.Assert;
import infra.util.FileSystemUtils;
import infra.util.StringUtils;
import infra.util.SystemPropertyUtils;
import infra.util.function.IOConsumer;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/25
 */
public class FileOperations {

  private static final Logger log = LoggerFactory.getLogger(FileOperations.class);

  private static final String BLOG_DATA_ENV = "TODAY_BLOG_DATA";

  private static final File baseDataPath = basePath();

  public static final FileOperations attachments = FileOperations.forDirectory("upload");

  public static final FileOperations tempFile = forDirectory("temp", true);

  private final File directory;

  private final String name;

  FileOperations(File directory, String name) {
    this.directory = directory;
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public Path getPath() {
    return directory.toPath();
  }

  public Path getPath(Locatable locatable) {
    return getPath(locatable.getLocation());
  }

  public Path getPath(String path) {
    return Path.of(directory.getPath(), path);
  }

  public File getFile() {
    return directory;
  }

  public File getLocalFile(Locatable locatable) {
    return getLocalFile(locatable.getLocation());
  }

  public File getLocalFile(String path) {
    Assert.notNull(path, "Path is required");
    return new File(directory, path);
  }

  public Resource getResource(Locatable locatable) {
    return getResource(locatable.getLocation());
  }

  public Resource getResource(String location) {
    return new FileSystemResource(getLocalFile(location));
  }

  public ReturnValue<File> persistOutputStream(Locatable locatable, IOConsumer<OutputStream> consumer) {
    return persistOutputStream(locatable.getLocation(), consumer);
  }

  public ReturnValue<File> persistOutputStream(String path, IOConsumer<OutputStream> consumer) {
    return persist(path, (File file) -> {
      try (OutputStream outputStream = new FileOutputStream(file)) {
        consumer.acceptWithException(outputStream);
      }
    });
  }

  public ReturnValue<File> persist(Locatable locatable, IOConsumer<File> consumer) {
    return persist(locatable.getLocation(), consumer);
  }

  public ReturnValue<File> persist(String path, IOConsumer<File> consumer) {
    File file = new File(directory, path);
    File parentFile = file.getParentFile();
    if (!parentFile.exists()) {
      if (!parentFile.mkdirs()) {
        return ReturnValue.failed(new IOException("Failed to create directory " + parentFile.getAbsolutePath()));
      }
    }

    try {
      consumer.acceptWithException(file);
    }
    catch (Throwable e) {
      log.error("file {} cannot be persist", path, e);
      return ReturnValue.failed(e);
    }
    return ReturnValue.ok(file);
  }

  public Status transferFrom(FileOperations fromDir, Locatable from, Locatable to) {
    return transferTo(fromDir.getPath(from), to);
  }

  public ReturnValue<Void> transferFrom(File from, File to) {
    try (FileChannel channel = FileChannel.open(from.toPath(), StandardOpenOption.READ);
            FileChannel toChannel = FileChannel.open(to.toPath(), StandardOpenOption.WRITE)) {

      toChannel.truncate(from.length());
      toChannel.transferFrom(channel, 0, channel.size());
      return ReturnValue.ok(null);
    }
    catch (IOException e) {
      return ReturnValue.failed(e);
    }
  }

  public Status transferTo(Locatable from, Locatable to) {
    return transferTo(getPath(from), to);
  }

  public Status transferTo(Locatable from, FileOperations toDir, Locatable to) {
    return toDir.transferFrom(this, from, to);
  }

  public Status transferTo(File from, Locatable to) {
    return transferTo(from.toPath(), to);
  }

  public Status transferTo(Path from, Locatable to) {
    return Status.forStatus(persistOutputStream(to, (OutputStream stream) -> {
      try (InputStream inputStream = Files.newInputStream(from)) {
        inputStream.transferTo(stream);
      }
    }).isSuccess());
  }

  public Status delete(Locatable locatable) {
    return delete(locatable.getLocation());
  }

  public Status delete(String path) {
    Assert.notNull(path, "Path is required");
    File localFile = getLocalFile(path);
    if (localFile.exists()) {
      return Status.forStatus(localFile.delete());
    }
    return Status.ok();
  }

  // Static Factory Methods

  /**
   * for sub directory
   *
   * @param sub Directory
   * @return Directory operations
   */
  public static FileOperations forDirectory(String sub) {
    return forDirectory(sub, false);
  }

  public static FileOperations forDirectory(String sub, boolean cleanOnStartup) {
    Assert.notNull(sub, "sub dir is required");
    File directory = new File(baseDataPath, sub);
    if (cleanOnStartup && directory.exists()) {
      Assert.state(FileSystemUtils.deleteRecursively(directory), () -> "Directory: '" + directory + "' delete failed");
    }
    if (!directory.exists()) {
      Assert.state(directory.mkdirs(), () -> "Directory: '" + directory + "' create failed");
    }
    return new FileOperations(directory, sub);
  }

  public static FileOperations forTempFiles() {
    return tempFile;
  }

  private static File basePath() {
    String resPath = System.getenv(BLOG_DATA_ENV);
    if (StringUtils.hasText(resPath)) {
      return new File(resPath);
    }

    resPath = System.getProperty("today-blog.data.path");
    if (StringUtils.hasText(resPath)) {
      resPath = SystemPropertyUtils.resolvePlaceholders(resPath, true);
      resPath = SystemPropertyUtils.resolvePlaceholders(resPath, true);
      return new File(resPath);
    }
    return new File(System.getProperty("user.dir") + "/today-blog/data/");
  }

}
