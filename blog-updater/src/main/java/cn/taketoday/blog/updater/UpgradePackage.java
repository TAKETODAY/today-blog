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

package cn.taketoday.blog.updater;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** API 与升级器共用的完整发行包校验。所有可安装文件必须出现在已签名清单中。 */
public final class UpgradePackage {

  public static final long MAX_PACKAGE_BYTES = 512L * 1024 * 1024;
  private static final long MAX_EXPANDED_BYTES = 2L * 1024 * 1024 * 1024;
  private static final int MAX_ENTRIES = 20000;
  private static final String PREFIX = "today-blog/";
  private static final String MANIFEST = PREFIX + "META-INF/upgrade.properties";
  private static final String SIGNATURE = PREFIX + "META-INF/upgrade.sig";

  private UpgradePackage() { }

  public record Manifest(String version, long build, Map<String, String> hashes) { }

  public static Manifest verify(Path archive, Path publicKey) throws Exception {
    if (Files.size(archive) > MAX_PACKAGE_BYTES) {
      throw new IOException("升级包不得超过 512 MiB");
    }
    try (ZipFile zip = new ZipFile(archive.toFile())) {
      byte[] metadata = read(zip, MANIFEST, 4 * 1024 * 1024);
      byte[] signature = read(zip, SIGNATURE, 16 * 1024);
      String pem = Files.readString(publicKey).replace("-----BEGIN PUBLIC KEY-----", "")
              .replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");
      var key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(pem)));
      Signature verifier = Signature.getInstance("SHA256withRSA");
      verifier.initVerify(key);
      verifier.update(metadata);
      if (!verifier.verify(Base64.getMimeDecoder().decode(signature))) {
        throw new IOException("升级包签名无效");
      }
      Properties properties = new Properties();
      properties.load(new java.io.ByteArrayInputStream(metadata));
      if (!"today-blog".equals(properties.getProperty("product"))
              || !"1".equals(properties.getProperty("format"))) {
        throw new IOException("不支持的升级包产品或格式");
      }
      String version = properties.getProperty("version", "");
      if (!version.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,99}")) {
        throw new IOException("版本号格式错误");
      }
      long build = Long.parseLong(properties.getProperty("build", "0"));
      if (build <= 0 || Runtime.version().feature() < Integer.parseInt(properties.getProperty("minimumJava", "17"))) {
        throw new IOException("构建编号或 Java 版本不符合要求");
      }
      if (!"true".equals(properties.getProperty("rollbackSafe"))) {
        throw new IOException("仅支持声明数据库向后兼容的在线升级包，请人工部署不可逆迁移");
      }
      Map<String, String> hashes = new HashMap<>();
      for (String name : properties.stringPropertyNames()) {
        if (name.startsWith("sha256.")) {
          String path = name.substring(7);
          checkPath(path);
          String hash = properties.getProperty(name);
          if (!hash.matches("[0-9a-f]{64}")) throw new IOException("文件摘要格式错误");
          hashes.put(path, hash);
        }
      }
      if (!hashes.keySet().containsAll(Set.of("api/bin/blog-api", "console/index.html", "desktop/index.html"))
              || hashes.keySet().stream().noneMatch(name -> name.startsWith("api/lib/") && name.endsWith(".jar"))) {
        throw new IOException("升级包缺少后端或前端必要文件");
      }
      Set<String> entries = new HashSet<>();
      Set<String> checked = new HashSet<>();
      long total = 0;
      var enumeration = zip.entries();
      while (enumeration.hasMoreElements()) {
        ZipEntry entry = enumeration.nextElement();
        String name = entry.getName();
        if (!entries.add(name) || entries.size() > MAX_ENTRIES) throw new IOException("重复文件或文件数量超限");
        if (!name.startsWith(PREFIX) || name.contains("\\") || name.contains("..") || name.startsWith("/")) {
          throw new IOException("升级包包含非法路径");
        }
        if (entry.isDirectory()) continue;
        if (name.equals(MANIFEST) || name.equals(SIGNATURE)) continue;
        String path = name.substring(PREFIX.length());
        checkPath(path);
        if (!hashes.containsKey(path)) throw new IOException("存在未签名文件: " + path);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        long size = 0;
        try (InputStream input = zip.getInputStream(entry)) {
          byte[] buffer = new byte[65536];
          int count;
          while ((count = input.read(buffer)) != -1) {
            size += count;
            total += count;
            if (size > MAX_PACKAGE_BYTES || total > MAX_EXPANDED_BYTES) throw new IOException("解压大小超限");
            digest.update(buffer, 0, count);
          }
        }
        if (!hashes.get(path).equals(HexFormat.of().formatHex(digest.digest()))) {
          throw new IOException("文件摘要不匹配: " + path);
        }
        checked.add(path);
      }
      if (!checked.equals(hashes.keySet())) throw new IOException("清单中存在缺失文件");
      return new Manifest(version, build, Map.copyOf(hashes));
    }
  }

  /** 只创建普通文件，不恢复 ZIP 权限、符号链接或特殊文件。调用方必须使用独占的空目录。 */
  public static void extract(Path archive, Path destination, Manifest manifest) throws IOException {
    try (ZipFile zip = new ZipFile(archive.toFile())) {
      for (String path : manifest.hashes().keySet()) {
        Path target = destination.resolve(path);
        Files.createDirectories(target.getParent());
        try (InputStream input = zip.getInputStream(zip.getEntry(PREFIX + path))) {
          Files.copy(input, target);
        }
      }
    }
  }

  private static void checkPath(String path) throws IOException {
    if (!path.matches("[A-Za-z0-9_./@+,-]+") || path.contains("..") || path.contains("//")
            || path.startsWith("/") || !(path.startsWith("api/bin/") || path.startsWith("api/lib/")
            || path.startsWith("console/") || path.startsWith("desktop/"))) {
      throw new IOException("不允许的发行文件路径: " + path);
    }
  }

  private static byte[] read(ZipFile zip, String name, int max) throws IOException {
    ZipEntry entry = zip.getEntry(name);
    if (entry == null || entry.isDirectory()) throw new IOException("缺少签名或清单文件");
    try (InputStream input = zip.getInputStream(entry)) {
      byte[] result = input.readNBytes(max + 1);
      if (result.length > max) throw new IOException("清单或签名过大");
      return result;
    }
  }
}
