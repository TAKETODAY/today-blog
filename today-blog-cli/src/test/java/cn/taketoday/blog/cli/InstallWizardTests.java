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

package cn.taketoday.blog.cli;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 安装向导输入边界测试。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/20
 */
class InstallWizardTests {

  @Test
  void acceptsAValidInstallationRequest() {
    InstallWizard.InstallationRequest request = InstallWizard.InstallationRequest.from(validForm());

    request.validate();

    assertThat(request.jdbcUrl()).isEqualTo(
            "jdbc:mysql://127.0.0.1:3306/today?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=UTC");
    assertThat(request.serverName()).isEqualTo("blog.example.com");
  }

  @Test
  void acceptsSiteHostsWithoutTrailingSlashOrWithPort() {
    Map<String, String> form = validForm();
    form.put("siteHost", "https://blog.example.com");
    InstallWizard.InstallationRequest request = InstallWizard.InstallationRequest.from(form);

    request.validate();

    assertThat(request.serverName()).isEqualTo("blog.example.com");

    form.put("siteHost", "http://192.168.139.74:8080");
    InstallWizard.InstallationRequest withPort = InstallWizard.InstallationRequest.from(form);
    withPort.validate();

    assertThat(withPort.serverName()).isEqualTo("192.168.139.74");
  }

  @Test
  void rejectsNonEmptyValuesThatCouldEscapeGeneratedConfiguration() {
    Map<String, String> form = validForm();
    form.put("siteHost", "https://blog.example.com/\nproxy_pass http://evil");

    assertThatThrownBy(() -> InstallWizard.InstallationRequest.from(form).validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("站点地址必须是 http 或 https 地址");
  }

  @Test
  void rejectsWeakOrMismatchedAdministratorPasswords() {
    Map<String, String> form = validForm();
    form.put("bloggerPassword", "short");
    form.put("bloggerPasswordAgain", "different");

    assertThatThrownBy(() -> InstallWizard.InstallationRequest.from(form).validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("博主密码长度必须为 8 至 72 个字符");

    form.put("bloggerPassword", "long-enough-password");
    assertThatThrownBy(() -> InstallWizard.InstallationRequest.from(form).validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("两次输入的博主密码不一致");
  }

  private Map<String, String> validForm() {
    return new java.util.HashMap<>(Map.ofEntries(
            Map.entry("serviceManager", "direct"),
            Map.entry("databaseHost", "127.0.0.1"),
            Map.entry("databasePort", "3306"),
            Map.entry("databaseName", "today"),
            Map.entry("databaseUser", "today"),
            Map.entry("databasePassword", "database-password"),
            Map.entry("siteName", "TODAY BLOG"),
            Map.entry("siteHost", "https://blog.example.com/"),
            Map.entry("bloggerName", "管理员"),
            Map.entry("bloggerEmail", "admin@example.com"),
            Map.entry("bloggerPassword", "strong-password"),
            Map.entry("bloggerPasswordAgain", "strong-password")));
  }

}
