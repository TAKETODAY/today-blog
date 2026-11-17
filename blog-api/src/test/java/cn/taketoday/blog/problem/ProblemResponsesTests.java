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

package cn.taketoday.blog.problem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import cn.taketoday.blog.util.ObjectUtils;
import infra.core.io.PathMatchingPatternResourceLoader;
import infra.web.mock.MockHttpContext;
import infra.http.HttpStatus;
import infra.validation.ObjectError;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotBlank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 问题响应与消息资源测试。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/16
 */
class ProblemResponsesTests {

  private final ProblemResponses problems = new ProblemResponses(new PathMatchingPatternResourceLoader());

  @Test
  void createsDefaultProblemThroughStaticFactory() {
    Problem problem = Problem.of("request.parameter_missing", HttpStatus.BAD_REQUEST);
    assertThat(problem).isInstanceOf(DefaultProblem.class)
            .isEqualTo(new DefaultProblem("request.parameter_missing", HttpStatus.BAD_REQUEST));
    assertThat(problem.code()).isEqualTo("request.parameter_missing");
    assertThat(problem.status()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(problem.type().toString()).isEqualTo("urn:today-blog:problem:request.parameter_missing");
    var exception = new ProblemException(problem, "email");
    assertThat(exception.getProblem()).isSameAs(problem);
    var response = problems.response(exception.getProblem(), request("en"), exception.getArguments());
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getDetail()).isEqualTo("A required parameter is missing: email.");
  }

  @Test
  void defaultProblemRejectsMissingDefinition() {
    assertThatThrownBy(() -> Problem.of(null, HttpStatus.BAD_REQUEST)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> Problem.of("request.invalid", null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> Problem.of(" ", HttpStatus.BAD_REQUEST)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void negotiatesLanguageAndFormatsArguments() {
    MockHttpContext request = request("fr;q=1,en-US;q=0.8,zh-CN;q=0.5");
    var problem = problems.create(CommonProblems.PARAMETER_MISSING, request, "email");
    assertThat(problem.getTitle()).isEqualTo("Missing request parameter");
    assertThat(problem.getDetail()).isEqualTo("A required parameter is missing: email.");
    assertThat(problem.getType()).isEqualTo(CommonProblems.PARAMETER_MISSING.type());
    assertThat(problem.getProperties()).containsEntry("code", "request.parameter_missing");
  }

  @Test
  void defaultsToChineseForAbsentUnsupportedAndInvalidHeaders() {
    for (String header : new String[] { "", "fr", "invalid!", "en;q=0,zh;q=0.8" }) {
      assertThat(problems.create(CommonProblems.AUTH_REQUIRED, request(header)).getDetail())
              .isEqualTo("请登录后重试。");
    }
  }

  @Test
  void expandsCodeAsTopLevelJsonProperty() throws Exception {
    var problem = problems.create(CommonProblems.INTERNAL_ERROR, request("en"));
    assertThat(ObjectUtils.toMap(ObjectUtils.toJSON(problem)))
            .containsEntry("code", "system.internal_error")
            .containsEntry("status", 500)
            .doesNotContainKey("properties");
  }

  @Test
  void allTypesHaveCompleteUniqueMessagesAndCompatibleTemplates() throws Exception {
    Properties chinese = loadMessages("");
    Properties english = loadMessages("_en");
    assertThat(english.keySet()).isEqualTo(chinese.keySet());
    HashSet<String> codes = new HashSet<>();
    for (Problem type : ProblemCatalog.all()) {
      assertThat(codes.add(type.code())).isTrue();
      assertThat(type.code()).matches("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+");
      for (String suffix : new String[] { ".title", "" }) {
        String key = type.code() + suffix;
        assertThat(chinese.getProperty(key)).isNotBlank();
        assertThat(english.getProperty(key)).isNotBlank();
        assertThat(new MessageFormat(chinese.getProperty(key), Locale.CHINESE).getFormatsByArgumentIndex().length)
                .isEqualTo(new MessageFormat(english.getProperty(key), Locale.ENGLISH).getFormatsByArgumentIndex().length);
      }
      for (String language : new String[] { "zh-CN", "en" }) {
        var response = problems.response(type, request(language), 42);
        var body = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(type.status());
        assertThat(body.getStatus()).isEqualTo(type.status().value());
        assertThat(body.getProperties()).containsEntry("code", type.code());
        assertThat(body.getDetail()).isNotBlank().doesNotContain("{0}");
      }
    }
  }

  @Test
  void catalogIncludesEveryProblemEnum() throws Exception {
    Path root = Path.of(Problem.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    HashSet<Problem> discovered = new HashSet<>();
    try (var paths = Files.walk(root.resolve("cn/taketoday/blog"))) {
      for (Path path : paths.filter(file -> file.toString().endsWith(".class")).toList()) {
        String name = root.relativize(path).toString().replace('/', '.').replace('\\', '.');
        Class<?> candidate = Class.forName(name.substring(0, name.length() - 6), false, Problem.class.getClassLoader());
        if (candidate.isEnum() && Problem.class.isAssignableFrom(candidate)) {
          Arrays.stream(candidate.getEnumConstants()).map(Problem.class::cast).forEach(discovered::add);
        }
      }
    }
    assertThat(ProblemCatalog.all()).containsExactlyInAnyOrderElementsOf(discovered);
  }

  @Test
  void translatesOriginalConstraintTemplateAndSafelyFallsBack() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var violation = factory.getValidator().validate(new Input()).iterator().next();
      ObjectError error = new ObjectError("input", "already interpolated");
      error.wrap(violation);
      assertThat(problems.validationMessage(error, request("en"))).isEqualTo("Please enter an email address.");
      assertThat(problems.validationMessage(error, request("zh"))).isEqualTo("请输入邮箱。");
    }
    var unknown = new ObjectError("input", new String[] { "unknown" }, null, "secret rejected value");
    assertThat(problems.validationMessage(unknown, request("en"))).isEqualTo("The input value is invalid.");
    var standard = new ObjectError("input", new String[] { "NotBlank" }, null, "secret rejected value");
    assertThat(problems.validationMessage(standard, request("en"))).isEqualTo("Must not be blank.");
  }

  static class Input {

    @NotBlank(message = "{validation.email.required}")
    String email = "";
  }

  @Test
  void discoversEveryDomainResource() throws Exception {
    Path root = Path.of(ProblemResponsesTests.class.getResource("/problems").toURI());
    try (var paths = Files.walk(root)) {
      var basenames = paths.filter(path -> path.getFileName().toString().equals("messages.properties"))
              .map(path -> "problems/" + root.relativize(path).toString().replace('\\', '/'))
              .map(path -> path.substring(0, path.length() - ".properties".length()))
              .toList();
      assertThat(ProblemResponses.discoverMessageBasenames(new PathMatchingPatternResourceLoader()))
              .containsExactlyInAnyOrderElementsOf(basenames);
    }
  }

  @Test
  void maintenanceConstantsHaveLocalizedMessages() throws Exception {
    Properties chinese = loadMessages("");
    Properties english = loadMessages("_en");
    for (var field : cn.taketoday.blog.maintenance.MaintenanceProblems.class.getFields()) {
      if (field.getType() == Problem.class) {
        Problem problem = (Problem) field.get(null);
        for (String suffix : new String[] { ".title", "" }) {
          String key = problem.code() + suffix;
          assertThat(chinese.getProperty(key)).as(key).isNotBlank();
          assertThat(english.getProperty(key)).as(key).isNotBlank();
        }
        assertThat(problems.create(problem, request("en")).getDetail())
                .isEqualTo(english.getProperty(problem.code()));
        assertThat(problems.create(problem, request("zh-CN")).getDetail())
                .isEqualTo(chinese.getProperty(problem.code()));
      }
    }
  }

  @Test
  void discoversNestedDirectoriesAndJarResources(@TempDir Path directory) throws Exception {
    Path classes = directory.resolve("problems/classes");
    Path nested = classes.resolve("problems/custom/problems/nested/messages.properties");
    Files.createDirectories(nested.getParent());
    Files.writeString(nested, "custom.title=Custom");
    Files.writeString(nested.resolveSibling("messages_en.properties"), "custom.title=English");
    Path jar = directory.resolve("problems/messages.jar");
    try (var output = new JarOutputStream(Files.newOutputStream(jar))) {
      for (String entry : new String[] { "problems/", "problems/plugin/", "problems/plugin/messages.properties" }) {
        output.putNextEntry(new JarEntry(entry));
        if (entry.endsWith(".properties")) {
          output.write("plugin.title=Plugin".getBytes(StandardCharsets.UTF_8));
        }
        output.closeEntry();
      }
    }
    try (var loader = new URLClassLoader(new URL[] { classes.toUri().toURL(), jar.toUri().toURL() }, null)) {
      assertThat(ProblemResponses.discoverMessageBasenames(new PathMatchingPatternResourceLoader(loader)))
              .containsExactly("problems/custom/problems/nested/messages", "problems/plugin/messages");
    }
  }

  @Test
  void rejectsMissingMessageResources(@TempDir Path directory) throws Exception {
    try (var loader = new URLClassLoader(new URL[] { directory.toUri().toURL() }, null)) {
      assertThatThrownBy(() -> ProblemResponses.discoverMessageBasenames(new PathMatchingPatternResourceLoader(loader)))
              .isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("No problem messages found");
    }
  }

  @Test
  void rejectsDuplicateBasenamesAcrossClasspathRoots(@TempDir Path directory) throws Exception {
    Path first = directory.resolve("first");
    Path second = directory.resolve("second");
    for (Path root : new Path[] { first, second }) {
      Path message = root.resolve("problems/shared/messages.properties");
      Files.createDirectories(message.getParent());
      Files.writeString(message, "shared.title=Shared");
    }
    try (var loader = new URLClassLoader(new URL[] { first.toUri().toURL(), second.toUri().toURL() }, null)) {
      assertThatThrownBy(() -> ProblemResponses.discoverMessageBasenames(new PathMatchingPatternResourceLoader(loader)))
              .isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("Duplicate problem message basename: problems/shared/messages");
    }
  }

  private static Properties loadMessages(String languageSuffix) throws Exception {
    Properties combined = new Properties();
    for (String basename : ProblemResponses.discoverMessageBasenames(new PathMatchingPatternResourceLoader())) {
      Properties domain = load(basename + languageSuffix + ".properties");
      Properties chinese = load(basename + ".properties");
      assertThat(domain.keySet()).as(basename).isEqualTo(chinese.keySet());
      for (String key : domain.stringPropertyNames()) {
        assertThat(combined.containsKey(key)).as("Duplicate message key: %s", key).isFalse();
        assertThat(domain.getProperty(key)).as(key).isNotBlank();
        assertThat(new MessageFormat(domain.getProperty(key), Locale.ROOT).getFormatsByArgumentIndex().length)
                .as(key)
                .isEqualTo(new MessageFormat(chinese.getProperty(key), Locale.ROOT).getFormatsByArgumentIndex().length);
        combined.setProperty(key, domain.getProperty(key));
      }
    }
    return combined;
  }

  private static Properties load(String file) throws Exception {
    Properties properties = new Properties();
    try (var reader = new InputStreamReader(ProblemResponsesTests.class.getResourceAsStream("/" + file),
            StandardCharsets.UTF_8)) {
      Properties unique = new Properties() {
        @Override
        public synchronized Object put(Object key, Object value) {
          assertThat(containsKey(key)).as("Duplicate message key in %s: %s", file, key).isFalse();
          return super.put(key, value);
        }
      };
      unique.load(reader);
      properties.putAll(unique);
    }
    return properties;
  }

  private static MockHttpContext request(String language) {
    MockHttpContext request = new MockHttpContext();
    request.getRequest().addHeader("Accept-Language", language);
    return request;
  }
}
