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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import cn.taketoday.blog.updater.RuntimeLayout;
import cn.taketoday.blog.updater.UpgradeFiles;
import infra.jdbc.Query;
import infra.jdbc.RepositoryManager;
import infra.persistence.annotation.Column;
import infra.persistence.annotation.GeneratedId;
import infra.persistence.annotation.Id;
import infra.persistence.annotation.Table;
import infra.persistence.platform.Platform;
import infra.persistence.support.DefaultEntityManager;

/**
 * 提供一次性的本机浏览器安装向导。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/20
 */
final class InstallWizard {

  private static final Logger log = LoggerFactory.getLogger(InstallWizard.class);

  private static final int MAX_FORM_BYTES = 32 * 1024;

  private static final String CONFIG_DIRECTORY = "/etc/today-blog";

  private static final SecureRandom RANDOM = new SecureRandom();

  private final Path root;

  private final Path configDirectory;

  private final UpgradeFiles files;

  private final RuntimeLayout layout;

  InstallWizard(Path root, UpgradeFiles files, RuntimeLayout layout) {
    this.root = root.toAbsolutePath().normalize();
    this.configDirectory = Path.of(System.getenv().getOrDefault("BLOG_CONFIG_DIRECTORY", CONFIG_DIRECTORY))
            .toAbsolutePath().normalize();
    this.files = files;
    this.layout = layout;
  }

  void run() throws Exception {
    ensureReleaseExists();
    Files.createDirectories(configDirectory);
    Path lockPath = configDirectory.resolve("install.lock");
    boolean locked = false;
    try (var channel = java.nio.channels.FileChannel.open(lockPath, StandardOpenOption.CREATE,
            StandardOpenOption.WRITE); var lock = channel.tryLock()) {
      if (lock == null) {
        throw new IOException("已有安装向导正在运行");
      }
      locked = true;
      runLocked();
    }
    finally {
      if (locked) {
        Files.deleteIfExists(lockPath);
      }
    }
  }

  private void runLocked() throws Exception {
    String token = token();
    CountDownLatch finished = new CountDownLatch(1);
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> handle(exchange, token, finished));
    server.start();
    int port = server.getAddress().getPort();
    String url = "http://127.0.0.1:" + port + "/?token=" + token;
    System.out.println("安装向导已启动，仅监听本机: " + url);
    System.out.println("远程服务器请使用 SSH 转发端口后再打开此地址。");
    try {
      finished.await();
    }
    finally {
      server.stop(0);
    }
  }

  private void handle(HttpExchange exchange, String token, CountDownLatch finished) {
    Map<String, String> submitted = Map.of();
    try {
      if (!constantTimeEquals(token, query(exchange.getRequestURI()).get("token"))) {
        send(exchange, 403, page("安装向导", "访问令牌无效，请使用终端显示的完整地址。", null));
        return;
      }
      if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
        send(exchange, 200, form(token, null, defaults()));
        return;
      }
      if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
        exchange.getResponseHeaders().set("Allow", "GET, POST");
        send(exchange, 405, page("安装向导", "只支持 GET 和 POST。", null));
        return;
      }
      String body = new String(exchange.getRequestBody().readNBytes(MAX_FORM_BYTES + 1), StandardCharsets.UTF_8);
      if (body.getBytes(StandardCharsets.UTF_8).length > MAX_FORM_BYTES) {
        send(exchange, 413, page("安装向导", "提交内容过大。", null));
        return;
      }
      Map<String, String> form = query(body);
      submitted = form;
      if (!constantTimeEquals(token, form.get("token"))) {
        send(exchange, 403, page("安装向导", "访问令牌无效，请重新打开终端显示的地址。", null));
        return;
      }
      InstallationRequest request = InstallationRequest.from(form);
      install(request);
      send(exchange, 200, page("安装完成", "博客初始化已完成。现在可以关闭此页面。", "配置文件已写入 "
              + escape(configDirectory.toString()) + "。请运行 sudo today-blog start 启动 API，再按需启用 NGINX。"));
      finished.countDown();
    }
    catch (Exception failure) {
      if (!(failure instanceof IllegalArgumentException)) {
        log.warn("安装向导执行失败", failure);
      }
      try {
        send(exchange, 400, form(token, failure.getMessage(),
                submitted.isEmpty() ? query(exchange.getRequestURI()) : submitted));
      }
      catch (IOException ignored) {
      }
    }
    finally {
      exchange.close();
    }
  }

  private void install(InstallationRequest request) throws Exception {
    request.validate();
    ensureReleaseExists();
    if (Files.exists(configDirectory.resolve("install.properties"), LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("博客已经完成初始化；已有站点请使用升级流程");
    }
    ensureServiceManager(request.serviceManager());
    DatabaseInstaller database = new DatabaseInstaller(request);
    database.initialize();
    writeConfiguration(request);
  }

  private void ensureReleaseExists() throws IOException {
    Path current = root.resolve("current");
    if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("未找到初始发行版，请先执行 install-server.sh 安装博客文件");
    }
  }

  private void ensureServiceManager(String manager) throws IOException {
    if ("systemd".equals(manager)) {
      if (!Files.isExecutable(Path.of("/usr/bin/systemctl"))
              || !Files.isRegularFile(Path.of("/etc/systemd/system/today-blog.service"))) {
        throw new IOException("systemd unit 尚未安装，请先使用 install-server.sh --systemd");
      }
    }
    if ("direct".equals(manager) && Files.exists(root.resolve("run/blog-api.pid"), LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("检测到直接进程仍在运行，请先停止 API");
    }
    if ("systemd".equals(manager) && Files.isExecutable(Path.of("/usr/bin/systemctl"))) {
      Process process = new ProcessBuilder("/usr/bin/systemctl", "--system", "is-active", "--quiet",
              "today-blog.service").redirectError(ProcessBuilder.Redirect.DISCARD).start();
      try {
        if (process.waitFor() == 0) {
          throw new IOException("检测到 systemd API 服务仍在运行，请先停止 API");
        }
      }
      catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
        throw new IOException("检查 systemd API 服务状态被中断", interrupted);
      }
    }
  }

  private void writeConfiguration(InstallationRequest request) throws IOException {
    Files.createDirectories(configDirectory);
    String override = System.getenv("BLOG_DATA_PATH");
    boolean customDataPath = override != null && !override.isBlank();
    Path data = customDataPath ? Path.of(override) : layout.data();
    Files.createDirectories(data);
    setOwner(data, "today");
    writeSecret(configDirectory.resolve("upgrade-db.password"), request.databasePassword() + "\n");

    // 只覆盖安装向导负责的配置项，保留发行包内 application.yaml 的框架、Flyway 和日志默认值。
    String env = "JAVA_OPTS=" + shellQuote("-Dapp.config.additional-location=file:"
            + configDirectory.resolve("application.yaml") + " -Dinfra.profiles.active=prod") + "\n";
    writeFile(configDirectory.resolve("today-blog.env"), env, 0640);

    String application = "infra:\n"
            + "  profiles:\n"
            + "    active: prod\n"
            + "datasource:\n"
            + "  url: " + yamlQuote(request.jdbcUrl()) + "\n"
            + "  username: " + yamlQuote(request.databaseUser()) + "\n"
            + "  password: " + yamlQuote(request.databasePassword()) + "\n"
            + "  type: com.zaxxer.hikari.HikariDataSource\n"
            + "  driver-class-name: com.mysql.cj.jdbc.Driver\n"
            + "site:\n"
            + "  host: " + yamlQuote(request.siteHost()) + "\n";
    if (customDataPath) {
      application += "attachment:\n"
              + "  outside-resource: " + yamlQuote(data.toString()) + "\n";
    }
    writeFile(configDirectory.resolve("application.yaml"), application, 0640);

    String upgrade = "database=" + request.databaseName() + "\n"
            + "jdbcUrl=" + request.jdbcUrl() + "\n"
            + "jdbcUser=" + request.databaseUser() + "\n"
            + "jdbcPasswordFile=" + configDirectory.resolve("upgrade-db.password") + "\n"
            + "backupReadGroup=today\n";
    writeFile(configDirectory.resolve("upgrade.properties"), upgrade, 0640);
    writeFile(configDirectory.resolve("service-manager"), request.serviceManager() + "\n", 0644);

    String installed = "installedAt=" + Instant.now() + "\n"
            + "siteHost=" + request.siteHost() + "\n"
            + "siteName=" + request.siteName() + "\n";
    writeFile(configDirectory.resolve("install.properties"), installed, 0640);
    writeFile(configDirectory.resolve("nginx.conf.example"), nginx(request), 0644);
  }

  private void writeSecret(Path path, String value) throws IOException {
    writeFile(path, value, 0600);
  }

  private void writeFile(Path path, String value, int mode) throws IOException {
    Files.createDirectories(path.getParent());
    Path temporary = Files.createTempFile(path.getParent(), ".install-", ".tmp");
    try {
      Files.writeString(temporary, value, StandardCharsets.UTF_8);
      setMode(temporary, mode);
      setGroup(temporary, "today");
      Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      setMode(path, mode);
      setGroup(path, "today");
    }
    finally {
      Files.deleteIfExists(temporary);
    }
  }

  private String nginx(InstallationRequest request) {
    return "# 审核后复制到 NGINX 站点配置目录，并先执行 nginx -t。\n"
            + "server {\n"
            + "    listen 80;\n"
            + "    server_name " + request.serverName() + ";\n"
            + "    root " + root.resolve("current/desktop") + ";\n"
            + "\n"
            + "    location = /api/internal/health { deny all; return 404; }\n"
            + "    location /api/ {\n"
            + "        proxy_pass http://127.0.0.1:8080;\n"
            + "        proxy_set_header Host $host;\n"
            + "        proxy_set_header X-Real-IP $remote_addr;\n"
            + "        client_max_body_size 512m;\n"
            + "    }\n"
            + "    location /console/ {\n"
            + "        alias " + root.resolve("current/console") + "/;\n"
            + "        try_files $uri $uri/ /console/index.html;\n"
            + "    }\n"
            + "    location / { try_files $uri $uri/ /index.html; }\n"
            + "}\n";
  }

  private static void setMode(Path path, int mode) throws IOException {
    if (Files.getFileAttributeView(path, java.nio.file.attribute.PosixFileAttributeView.class) != null) {
      String value = String.format("%04o", mode);
      Files.setPosixFilePermissions(path, PosixFilePermissionParser.parse(value));
    }
  }

  private static void setOwner(Path path, String user) throws IOException {
    try {
      var lookup = path.getFileSystem().getUserPrincipalLookupService();
      Files.setOwner(path, lookup.lookupPrincipalByName(user));
    }
    catch (UserPrincipalNotFoundException ignored) {
      // 开发机上可能没有 today 用户；生产安装脚本会预先创建该用户。
    }
  }

  private static void setGroup(Path path, String group) throws IOException {
    try {
      var lookup = path.getFileSystem().getUserPrincipalLookupService();
      var view = Files.getFileAttributeView(path, java.nio.file.attribute.PosixFileAttributeView.class);
      if (view != null) {
        view.setGroup(lookup.lookupPrincipalByGroupName(group));
      }
    }
    catch (UserPrincipalNotFoundException ignored) {
      // 开发机上可能没有 today 组；生产安装脚本会预先创建该组。
    }
  }

  private static String yamlQuote(String value) {
    return "'" + value.replace("'", "''") + "'";
  }

  private static String shellQuote(String value) {
    return "'" + value.replace("'", "'\\''") + "'";
  }

  private static String token() {
    byte[] bytes = new byte[24];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static boolean constantTimeEquals(String expected, String actual) {
    if (actual == null) {
      return false;
    }
    return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII), actual.getBytes(StandardCharsets.US_ASCII));
  }

  private Map<String, String> defaults() {
    Map<String, String> values = new HashMap<>();
    values.put("serviceManager", "direct");
    Path mode = configDirectory.resolve("service-manager");
    try {
      if (Files.isRegularFile(mode, LinkOption.NOFOLLOW_LINKS)) {
        String configured = Files.readString(mode, StandardCharsets.UTF_8).trim();
        if (configured.equals("direct") || configured.equals("systemd")) {
          values.put("serviceManager", configured);
        }
      }
    }
    catch (IOException ignored) {
    }
    return values;
  }

  private static Map<String, String> query(URI uri) {
    return query(uri.getRawQuery());
  }

  private static Map<String, String> query(String value) {
    Map<String, String> result = new HashMap<>();
    if (value == null || value.isEmpty()) {
      return result;
    }
    for (String item : value.split("&")) {
      int equals = item.indexOf('=');
      String key = equals < 0 ? item : item.substring(0, equals);
      String encoded = equals < 0 ? "" : item.substring(equals + 1);
      result.put(URLDecoder.decode(key, StandardCharsets.UTF_8), URLDecoder.decode(encoded, StandardCharsets.UTF_8));
    }
    return result;
  }

  private String form(String token, String error, Map<String, String> values) {
    String selected = values.getOrDefault("serviceManager", "direct");
    String body = """
            <h1>TODAY BLOG 安装向导</h1>
            <p>仅用于新安装。数据库必须是空库；已有博客请取消并走升级流程。</p>
            %s
            <form method="post">
              <input type="hidden" name="token" value="%s">
              <h2>服务</h2>
              <label>管理方式 <select name="serviceManager">
                <option value="direct" %s>直接管理 blog-api</option>
                <option value="systemd" %s>systemd</option>
              </select></label>
              <h2>数据库</h2>
              <label>主机 <input name="databaseHost" value="%s" required></label>
              <label>端口 <input name="databasePort" value="%s" required></label>
              <label>数据库名 <input name="databaseName" value="%s" required></label>
              <label>用户名 <input name="databaseUser" value="%s" required></label>
              <label>密码 <input type="password" name="databasePassword" required></label>
              <h2>站点</h2>
              <label>站点名称 <input name="siteName" value="%s" required></label>
              <label>站点地址 <input name="siteHost" value="%s" required></label>
              <h2>博主账号</h2>
              <label>昵称 <input name="bloggerName" value="%s" required></label>
              <label>邮箱 <input type="email" name="bloggerEmail" value="%s" required></label>
              <label>密码 <input type="password" name="bloggerPassword" minlength="8" required></label>
              <label>确认密码 <input type="password" name="bloggerPasswordAgain" minlength="8" required></label>
              <p><button type="submit">开始安装</button></p>
            </form>
            <style>body{font:16px system-ui;max-width:760px;margin:40px auto;padding:0 20px}label{display:block;margin:10px 0}input,select{display:block;width:100%%;max-width:520px;padding:8px}button{padding:10px 24px}.error{background:#fee;padding:12px}</style>
            """.formatted(
            error == null ? "" : "<p class=\"error\"><strong>未完成：</strong>" + escape(error) + "</p>",
            escape(token),
            "direct".equals(selected) ? "selected" : "",
            "systemd".equals(selected) ? "selected" : "",
            escape(values.getOrDefault("databaseHost", "127.0.0.1")),
            escape(values.getOrDefault("databasePort", "3306")),
            escape(values.getOrDefault("databaseName", "today")),
            escape(values.getOrDefault("databaseUser", "")),
            escape(values.getOrDefault("siteName", "TODAY BLOG")),
            escape(values.getOrDefault("siteHost", "http://localhost")),
            escape(values.getOrDefault("bloggerName", "")),
            escape(values.getOrDefault("bloggerEmail", "")));
    return page("TODAY Blog 安装向导", null, body);
  }

  private static String page(String title, String message, String detail) {
    String content = message == null ? "" : "<p>" + escape(message) + "</p>";
    if (detail != null) {
      content += "<p>" + detail + "</p>";
    }
    return "<!doctype html><html lang=\"zh-CN\"><meta charset=\"UTF-8\"><title>" + escape(title)
            + "</title><body>" + content + "</body></html>";
  }

  private static void send(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
    exchange.getResponseHeaders().set("Cache-Control", "no-store");
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream output = exchange.getResponseBody()) {
      output.write(bytes);
    }
  }

  private static String rootMessage(Throwable failure) {
    Throwable root = failure;
    while (root.getCause() != null && root.getCause() != root) {
      root = root.getCause();
    }
    String message = root.getMessage();
    return message == null || message.isBlank()
            ? "请检查地址、端口、数据库名和凭据"
            : message;
  }

  private static String escape(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
  }

  record InstallationRequest(String serviceManager, String databaseHost, int databasePort, String databaseName,
          String databaseUser, String databasePassword, String siteName, String siteHost, String bloggerName,
          String bloggerEmail, String bloggerPassword, String bloggerPasswordAgain) {

    static InstallationRequest from(Map<String, String> form) {
      String port = form.getOrDefault("databasePort", "");
      int databasePort;
      try {
        databasePort = Integer.parseInt(port);
      }
      catch (NumberFormatException ex) {
        databasePort = -1;
      }
      return new InstallationRequest(form.get("serviceManager"), form.get("databaseHost"), databasePort,
              form.get("databaseName"), form.get("databaseUser"), form.get("databasePassword"),
              form.get("siteName"), form.get("siteHost"), form.get("bloggerName"), form.get("bloggerEmail"),
              form.get("bloggerPassword"), form.get("bloggerPasswordAgain"));
    }

    void validate() {
      require(serviceManager, "服务管理方式");
      if (!serviceManager.equals("direct") && !serviceManager.equals("systemd")) {
        throw new IllegalArgumentException("服务管理方式必须为 direct 或 systemd");
      }
      if (databaseHost == null || !databaseHost.matches("[A-Za-z0-9._:-]{1,253}")) {
        throw new IllegalArgumentException("数据库主机名无效");
      }
      if (databasePort < 1 || databasePort > 65535) {
        throw new IllegalArgumentException("数据库端口无效");
      }
      if (databaseName == null || !databaseName.matches("[A-Za-z0-9_]{1,64}")) {
        throw new IllegalArgumentException("数据库名只能包含字母、数字和下划线");
      }
      require(databaseUser, "数据库用户名");
      require(databasePassword, "数据库密码");
      require(siteName, "站点名称");
      validateSiteHost(siteHost);
      require(bloggerName, "博主昵称");
      if (bloggerEmail == null || !bloggerEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
        throw new IllegalArgumentException("博主邮箱格式无效");
      }
      if (bloggerPassword == null || bloggerPassword.length() < 8 || bloggerPassword.length() > 72) {
        throw new IllegalArgumentException("博主密码长度必须为 8 至 72 个字符");
      }
      if (!Objects.equals(bloggerPassword, bloggerPasswordAgain)) {
        throw new IllegalArgumentException("两次输入的博主密码不一致");
      }
    }

    String jdbcUrl() {
      return "jdbc:mysql://" + databaseHost + ":" + databasePort + "/" + databaseName
              + "?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=UTC";
    }

    String serverName() {
      return URI.create(siteHost).getHost();
    }

    private static void require(String value, String name) {
      if (value == null || value.isBlank() || value.chars().anyMatch(Character::isISOControl)) {
        throw new IllegalArgumentException(name + "不能为空");
      }
    }

    private static void validateSiteHost(String value) {
      if (value == null || value.chars().anyMatch(Character::isISOControl)) {
        throw new IllegalArgumentException("站点地址必须是 http 或 https 地址");
      }
      URI uri;
      try {
        uri = URI.create(value);
      }
      catch (IllegalArgumentException invalid) {
        throw new IllegalArgumentException("站点地址必须是 http 或 https 地址", invalid);
      }
      String host = uri.getHost();
      String path = uri.getRawPath();
      if ((!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))
              || host == null || !host.matches("[A-Za-z0-9](?:[A-Za-z0-9.-]{0,251}[A-Za-z0-9])?")
              || uri.getRawQuery() != null || uri.getRawFragment() != null
              || (path != null && !path.isEmpty() && !path.equals("/")) || uri.getPort() > 65535) {
        throw new IllegalArgumentException("站点地址必须是 http 或 https 地址");
      }
    }
  }

  private static final class DatabaseInstaller {

    private final InstallationRequest request;

    private DatabaseInstaller(InstallationRequest request) {
      this.request = request;
    }

    private void initialize() throws Exception {
      // HttpServer 的工作线程看不到 APP-INF/lib，需要显式注册随包提供的 JDBC 驱动。
      Class.forName("com.mysql.cj.jdbc.Driver");
      RepositoryManager repository = new RepositoryManager(request.jdbcUrl(), request.databaseUser(), request.databasePassword());
      ensureEmpty(repository);
      executeSchema(repository);
      var entities = new DefaultEntityManager(repository, Platform.mysql());
      seed(entities);
    }

    private void ensureEmpty(RepositoryManager repository) {
      try (Query query = repository.createQuery(
              "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ?")) {
        query.addParameter(request.databaseName());
        Number tables = query.scalar(Number.class);
        if (tables != null && tables.longValue() != 0) {
          throw new IllegalStateException("数据库不是空库，安装向导不会覆盖已有数据");
        }
      }
      catch (IllegalStateException failure) {
        throw failure;
      }
      catch (RuntimeException failure) {
        throw new IllegalStateException("无法连接数据库：" + rootMessage(failure), failure);
      }
    }

    private void executeSchema(RepositoryManager repository) throws IOException {
      String sql;
      try (var input = InstallWizard.class.getResourceAsStream("/db/migration/v1.0__init.sql")) {
        if (input == null) {
          throw new IOException("安装包缺少数据库初始化脚本");
        }
        sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
      }
      for (String statement : statements(stripComments(sql))) {
        try (Query query = repository.createQuery(statement)) {
          query.executeUpdate();
        }
      }
    }

    private void seed(DefaultEntityManager entities) {
      String password = BCrypt.hashpw(request.bloggerPassword(), BCrypt.gensalt(12));
      BloggerRow blogger = new BloggerRow();
      blogger.name = request.bloggerName();
      blogger.email = request.bloggerEmail();
      blogger.passwd = password;
      blogger.image = "/upload/avatar/default.png";
      entities.persist(blogger);

      UserRow user = new UserRow();
      user.name = request.bloggerName();
      user.email = request.bloggerEmail();
      user.password = password;
      user.status = 0;
      user.type = "master";
      user.notification = true;
      entities.persist(user);

      option(entities, "site.name", request.siteName());
      option(entities, "site.host", request.siteHost());
    }

    private void option(DefaultEntityManager entities, String name, String value) {
      OptionRow current = entities.findById(OptionRow.class, name);
      if (current == null) {
        OptionRow option = new OptionRow();
        option.name = name;
        option.value = value;
        option.open = true;
        entities.persist(option);
      }
      else {
        current.value = value;
        entities.updateById(current);
      }
    }

    private static List<String> statements(String sql) {
      List<String> result = new ArrayList<>();
      StringBuilder statement = new StringBuilder();
      boolean quoted = false;
      char quote = 0;
      for (int i = 0; i < sql.length(); i++) {
        char current = sql.charAt(i);
        if ((current == '\'' || current == '"') && (i == 0 || sql.charAt(i - 1) != '\\')) {
          if (!quoted) {
            quoted = true;
            quote = current;
          }
          else if (quote == current) {
            quoted = false;
          }
        }
        if (current == ';' && !quoted) {
          addStatement(result, statement);
          statement.setLength(0);
        }
        else {
          statement.append(current);
        }
      }
      addStatement(result, statement);
      return result;
    }

    private static String stripComments(String sql) {
      return sql.lines()
              .filter(line -> {
                String value = line.stripLeading();
                return !value.startsWith("#") && !value.startsWith("--");
              })
              .collect(java.util.stream.Collectors.joining("\n"));
    }

    private static void addStatement(List<String> result, StringBuilder value) {
      String statement = value.toString().strip();
      if (!statement.isEmpty() && !statement.startsWith("#") && !statement.startsWith("--")) {
        result.add(statement);
      }
    }
  }

  @Table("blogger")
  static class BloggerRow {
    @GeneratedId
    public Integer id;
    public String name;
    public String email;
    public String passwd;
    public String image;
  }

  @Table("user")
  static class UserRow {
    @GeneratedId
    public Long id;
    public String name;
    public String email;
    public String password;
    public Integer status;
    public String type;
    public Boolean notification;
  }

  @Table("t_option")
  static class OptionRow {
    @Id
    public String name;
    public String value;
    @Column("public")
    public Boolean open;
  }

  private static final class PosixFilePermissionParser {

    private static java.util.Set<PosixFilePermission> parse(String mode) {
      int value = Integer.parseInt(mode, 8);
      java.util.Set<PosixFilePermission> permissions = new java.util.HashSet<>();
      add(permissions, value, 0400, PosixFilePermission.OWNER_READ);
      add(permissions, value, 0200, PosixFilePermission.OWNER_WRITE);
      add(permissions, value, 0100, PosixFilePermission.OWNER_EXECUTE);
      add(permissions, value, 0040, PosixFilePermission.GROUP_READ);
      add(permissions, value, 0020, PosixFilePermission.GROUP_WRITE);
      add(permissions, value, 0010, PosixFilePermission.GROUP_EXECUTE);
      add(permissions, value, 0004, PosixFilePermission.OTHERS_READ);
      add(permissions, value, 0002, PosixFilePermission.OTHERS_WRITE);
      add(permissions, value, 0001, PosixFilePermission.OTHERS_EXECUTE);
      return permissions;
    }

    private static void add(java.util.Set<PosixFilePermission> permissions, int value, int bit,
            PosixFilePermission permission) {
      if ((value & bit) != 0) {
        permissions.add(permission);
      }
    }
  }

}
