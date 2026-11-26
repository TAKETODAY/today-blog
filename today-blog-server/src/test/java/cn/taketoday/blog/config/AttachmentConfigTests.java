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

package cn.taketoday.blog.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import cn.taketoday.blog.model.Attachment;
import cn.taketoday.blog.updater.RuntimeLayout;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.properties.ConfigurationPropertiesBindingPostProcessor;
import infra.core.env.MapPropertySource;
import infra.web.multipart.parsing.DefaultMultipartParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * 验证附件目录的运行时默认值和配置覆盖。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/23
 */
class AttachmentConfigTests {

  @TempDir
  Path temp;

  @Test
  void resolvesUploadsAndExistingAttachmentsUnderTheSelectedRuntimeDataDirectory() {
    Path root = temp.resolve("custom-runtime");
    try (var context = attachmentContext(root, Map.of())) {
      AttachmentConfig config = context.getBean(AttachmentConfig.class);
      assertLocalFile(config, root.resolve("data"));
      assertThat(config.getRemoteURL("/upload/image/photo.png")).isEqualTo("/upload/image/photo.png");
    }
  }

  @Test
  void explicitOutsideResourceOverridesTheRuntimeDataDirectory() {
    Path existingDirectory = temp.resolve("existing-attachments");
    try (var context = attachmentContext(temp.resolve("runtime"), Map.of(
            "attachment.outside-resource", existingDirectory.toString(),
            "attachment.cdn-host-prefix", "https://cdn.example.com"))) {
      AttachmentConfig config = context.getBean(AttachmentConfig.class);
      assertLocalFile(config, existingDirectory);
      assertThat(config.getRemoteURL("/upload/image/photo.png"))
              .isEqualTo("https://cdn.example.com/upload/image/photo.png");
    }
  }

  @Test
  void storesMultipartFilesUnderRuntimeTmpWithoutChangingTheUploadThreshold() {
    Path root = temp.resolve("custom-runtime");
    try (var context = new AnnotationConfigApplicationContext()) {
      context.registerBean(RuntimeLayout.class, () -> new RuntimeLayout(root));
      context.registerBean(InitializationBeanPostProcessor.class,
              () -> AppConfig.multipartStorage(context.getBeanProvider(RuntimeLayout.class)));
      context.registerBean(DefaultMultipartParser.class, () -> {
        var parser = new DefaultMultipartParser();
        parser.setThreshold(32 * 1024);
        return parser;
      });
      context.refresh();

      var parser = context.getBean(DefaultMultipartParser.class);
      assertThat(parser.getTempRepository()).isEqualTo(root.resolve("tmp/multipart")).isDirectory();
      assertThat(parser.getThreshold()).isEqualTo(32 * 1024);
    }
  }

  private AnnotationConfigApplicationContext attachmentContext(Path root, Map<String, Object> properties) {
    var context = new AnnotationConfigApplicationContext();
    context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("attachment-test", properties));
    ConfigurationPropertiesBindingPostProcessor.register(context);
    context.registerBean(RuntimeLayout.class, () -> new RuntimeLayout(root));
    context.register(AttachmentConfig.class);
    context.refresh();
    return context;
  }

  private void assertLocalFile(AttachmentConfig config, Path directory) {
    String location = "upload/image/photo.png";
    var attachment = new Attachment();
    attachment.setLocation(location);

    assertThat(config.getOutsideResource()).isEqualTo(directory.toString());
    assertThat(Path.of(config.getUploadDir())).isEqualTo(directory.resolve("upload"));
    assertThat(config.getLocalFile(location).toPath()).isEqualTo(directory.resolve("upload/image/photo.png"));
    assertThat(config.getLocalFile(attachment)).isEqualTo(config.getLocalFile(location));
    assertThat(config.getLocalFile("/" + location)).isEqualTo(config.getLocalFile(location));
    assertThat(config.relativeLocation("/" + location)).isEqualTo(location);
    assertThatIllegalArgumentException().isThrownBy(() -> config.getLocalFile("/var/data/photo.png"));
    assertThatIllegalArgumentException().isThrownBy(() -> config.getLocalFile("upload/../photo.png"));
  }

}
