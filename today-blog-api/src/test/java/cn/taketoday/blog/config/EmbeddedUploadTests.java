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

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import cn.taketoday.blog.service.AttachmentService;
import cn.taketoday.blog.updater.RuntimeLayout;
import cn.taketoday.blog.util.OssOperations;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.properties.ConfigurationPropertiesBindingPostProcessor;
import infra.core.env.MapPropertySource;
import infra.http.HttpHeaders;
import infra.persistence.EntityManager;
import infra.test.classpath.ClassPathExclusions;
import infra.test.web.mock.assertj.MockMvcTester;
import infra.transaction.support.TransactionOperations;
import infra.web.config.annotation.EnableWebMvc;
import infra.web.multipart.Part;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证本地附件保存和内嵌源站的公开读取使用同一目录。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/26
 */
class EmbeddedUploadTests {

  @TempDir
  Path temp;

  @Test
  void servesFilesInTheDefaultRuntimeDirectory() throws Exception {
    writeDefaultUpload();
    try (var context = context(Map.of())) {
      MockMvcTester.from(context).get().uri("/upload/images/photo.png").assertThat()
              .hasStatusOk().hasBodyTextEqualTo("photo").hasHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
    }
  }

  @Test
  @ClassPathExclusions("today-blog-frontend-*.jar")
  void doesNotServeExistingUploadsWithoutTheEmbeddedFrontendJar() throws Exception {
    writeDefaultUpload();
    try (var context = context(Map.of())) {
      MockMvcTester.from(context).get().uri("/upload/images/photo.png").assertThat().hasStatus(404);
    }
  }

  @Test
  void servesNewUploadsFromTheConfiguredDirectoryWithIndependentCache() throws Exception {
    Path outside = temp.resolve("attachments");
    try (var context = context(Map.of("attachment.outside-resource", outside.toString(),
            "frontend.cache.upload", "public, max-age=120"))) {
      var part = mock(Part.class);
      when(part.getOriginalFilename()).thenReturn("photo space.png");
      doAnswer(call -> {
        Files.writeString(call.<File>getArgument(0).toPath(), "new photo");
        return null;
      }).when(part).transferTo(any(File.class));
      var attachment = context.getBean(AttachmentService.class).upload(part, null);

      assertThat(Files.readString(outside.resolve(attachment.getUri().substring(1)))).isEqualTo("new photo");
      var mvc = MockMvcTester.from(context);
      var uri = new URI(null, null, attachment.getUri(), null);
      mvc.get().uri(uri).assertThat()
              .hasStatusOk().hasBodyTextEqualTo("new photo")
              .hasHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=120");
      mvc.head().uri(uri).assertThat().hasStatusOk().hasBodyTextEqualTo("")
              .hasHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=120");
    }
  }

  @Test
  void doesNotServeDirectoriesMissingAttachmentsOrLinksOutsideTheUploadRoot() throws Exception {
    Path root = Files.createDirectories(temp.resolve("data/upload"));
    Path other = Files.writeString(root.resolveSibling("private.txt"), "private");
    Files.createDirectory(root.resolve("images"));
    Files.createSymbolicLink(root.resolve("link.txt"), other);
    try (var context = context(Map.of())) {
      var mvc = MockMvcTester.from(context);
      for (String path : new String[] { "/upload", "/upload/", "/upload/images", "/upload/images/",
              "/upload/missing", "/upload/link.txt", "/upload/private.txt" }) {
        mvc.get().uri(path).assertThat().hasStatus(404);
      }
      mvc.get().uri(URI.create("/upload/%2e%2e/private.txt")).assertThat().hasStatus(404);
    }
  }

  private void writeDefaultUpload() throws Exception {
    Path directory = Files.createDirectories(temp.resolve("data/upload/images"));
    Files.writeString(directory.resolve("photo.png"), "photo");
  }

  private AnnotationConfigApplicationContext context(Map<String, Object> properties) {
    var context = new AnnotationConfigApplicationContext();
    context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("upload-test", properties));
    ConfigurationPropertiesBindingPostProcessor.register(context);
    context.registerBean(RuntimeLayout.class, () -> new RuntimeLayout(temp));
    context.registerBean(EntityManager.class, () -> mock(EntityManager.class));
    context.registerBean(OssOperations.class, () -> mock(OssOperations.class));
    context.registerBean(TransactionOperations.class, () -> mock(TransactionOperations.class));
    context.register(WebConfiguration.class, AttachmentConfig.class, AttachmentService.class);
    context.scan("cn.taketoday.blog.config.embedded");
    context.refresh();
    return context;
  }

  @Configuration(proxyBeanMethods = false)
  @EnableWebMvc
  static class WebConfiguration {
  }

}
