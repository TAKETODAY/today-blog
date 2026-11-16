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

package cn.taketoday.blog.error;

import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;

import cn.taketoday.blog.util.ObjectUtils;
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

  private final ProblemResponses problems = new ProblemResponses();

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
    Properties chinese = load("errors.properties");
    Properties english = load("errors_en.properties");
    assertThat(english.keySet()).isEqualTo(chinese.keySet());
    HashSet<String> codes = new HashSet<>();
    for (Problem type : ProblemCatalog.all()) {
      assertThat(codes.add(type.code())).isTrue();
      assertThat(type.code()).matches("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+");
      for (String suffix : new String[] { ".title", ".detail" }) {
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

  private static Properties load(String file) throws Exception {
    Properties properties = new Properties();
    try (var reader = new InputStreamReader(ProblemResponsesTests.class.getResourceAsStream("/i18n/" + file),
            StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }

  private static MockHttpContext request(String language) {
    MockHttpContext request = new MockHttpContext();
    request.getRequest().addHeader("Accept-Language", language);
    return request;
  }
}
