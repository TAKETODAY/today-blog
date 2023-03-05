/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.blog.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import cn.taketoday.blog.BlogConstant;
import cn.taketoday.blog.ConfigBinding;
import cn.taketoday.blog.Pageable;
import cn.taketoday.format.support.ApplicationConversionService;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.ip2region.IpLocation;
import cn.taketoday.lang.Nullable;
import cn.taketoday.ui.Model;
import cn.taketoday.util.DataSize;
import cn.taketoday.util.StringUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.web.RequestContext;

import static java.util.regex.Pattern.compile;

public abstract class BlogUtils {
  private static final List<String> IP_HEADERS = List.of(
          "X-Real-IP",           // X-Real-IP：nginx服务代理
          "Proxy-Client-IP",     // Proxy-Client-IP：apache 服务代理
          "WL-Proxy-Client-IP",  // WL-Proxy-Client-IP：weblogic 服务代理
          "HTTP_CLIENT_IP",      // HTTP_CLIENT_IP：有些代理服务器
          "X-Forwarded-For"      // X-Forwarded-For：Squid 服务代理
  );

  public static String remoteAddress(RequestContext request) {
    String ipAddresses = getIpAddresses(request.requestHeaders());

    //有些网络通过多层代理，那么获取到的ip就会有多个，一般都是通过逗号（,）分割开来，并且第一个ip为客户端的真实IP
    if (StringUtils.isNotEmpty(ipAddresses)) {
      String ip = ipAddresses.split(",")[0];
      if (isIP(ip)) {
        return ip;
      }
    }

    //还是不能获取到，最后再通过 request.getRemoteAddress();获取
    return request.getRemoteAddress();
  }

  @Nullable
  private static String getIpAddresses(HttpHeaders requestHeaders) {
    for (String ipHeader : IP_HEADERS) {
      String ipAddresses = requestHeaders.getFirst(ipHeader);
      if (isIP(ipAddresses)) {
        return ipAddresses;
      }
    }
    return null;
  }

  private static boolean isIP(String ipAddresses) {
    return StringUtils.hasText(ipAddresses) && !IpLocation.UNKNOWN.equalsIgnoreCase(ipAddresses);
  }

  // -------------------------------

  public static void resolveBinding(Object bean, Map<String, String> optionsMap) {
    Class<?> beanClass = bean.getClass();

    ConfigBinding annotation = beanClass.getAnnotation(ConfigBinding.class);
    String prefix;
    if (annotation != null) {
      prefix = annotation.value();
    }
    else {
      prefix = BlogConstant.BLANK;
    }

    for (Field field : ReflectionUtils.getDeclaredFields(beanClass)) {
      ConfigBinding bindingOnField = field.getAnnotation(ConfigBinding.class);
      String key;
      if (bindingOnField != null) {
        if (bindingOnField.splice()) {
          key = prefix + bindingOnField.value();
        }
        else {
          key = bindingOnField.value();
        }
      }
      else {
        key = prefix + field.getName();
      }

      String source = optionsMap.get(key);
      if (source != null) {
        Object converted = ApplicationConversionService.getSharedInstance().convert(source, field.getType());
        if (converted != null) {
          ReflectionUtils.setField(ReflectionUtils.makeAccessible(field), bean, converted);
        }
      }
    }
  }

  // ----------------------------xss

  private static final Set<Pattern> XSS_PATTERNS = new HashSet<Pattern>(7, 1.0f);

  static {
    XSS_PATTERNS.add(compile("<(no)?script[^>]*>.*?</(no)?script>", Pattern.CASE_INSENSITIVE));
    XSS_PATTERNS.add(compile("(javascript:|vbscript:|view-source:)*", Pattern.CASE_INSENSITIVE));
    XSS_PATTERNS.add(compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL));
    XSS_PATTERNS.add(compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL));
    XSS_PATTERNS.add(compile("<(\"[^\"]*\"|\'[^\']*\'|[^\'\">])*>",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL));

    XSS_PATTERNS.add(compile("(window\\.location|window\\.|\\.location|document\\.cookie|document\\.|alert\\(.*?\\)|window\\.open\\()*",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL));

    XSS_PATTERNS.add(compile(
            "<+\\s*\\w*\\s*(oncontrolselect|oncopy|oncut|ondataavailable|ondatasetchanged|ondatasetcomplete|ondblclick|ondeactivate|ondrag|ondragend|ondragenter|ondragleave|ondragover|ondragstart|ondrop|onerror=|onerroupdate|onfilterchange|onfinish|onfocus|onfocusin|onfocusout|onhelp|onkeydown|onkeypress|onkeyup|onlayoutcomplete|onload|onlosecapture|onmousedown|onmouseenter|onmouseleave|onmousemove|onmousout|onmouseover|onmouseup|onmousewheel|onmove|onmoveend|onmovestart|onabort|onactivate|onafterprint|onafterupdate|onbefore|onbeforeactivate|onbeforecopy|onbeforecut|onbeforedeactivate|onbeforeeditocus|onbeforepaste|onbeforeprint|onbeforeunload|onbeforeupdate|onblur|onbounce|oncellchange|onchange|onclick|oncontextmenu|onpaste|onpropertychange|onreadystatechange|onreset|onresize|onresizend|onresizestart|onrowenter|onrowexit|onrowsdelete|onrowsinserted|onscroll|onselect|onselectionchange|onselectstart|onstart|onstop|onsubmit|onunload)+\\s*=+",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL));
  }

  /**
   *
   */
  public static String stripXss(String value) {
    if (StringUtils.isNotEmpty(value)) {
      Matcher matcher;
      for (Pattern pattern : XSS_PATTERNS) {
        matcher = pattern.matcher(value);
        if (matcher.find()) {
          value = matcher.replaceAll(BlogConstant.BLANK);
        }
      }
    }
    return value;
  }

  /**
   *
   */
  public static String stripAllXss(String value) {
    if (StringUtils.isEmpty(value)) {
      return value;
    }
    return stripXss(value).replaceAll("<", "&lt;").replaceAll(">", "&gt;");
  }

  // --------------------------

  /**
   *
   */
  public static String formatTime(long ct) {

    ct = (System.currentTimeMillis() - ct) / 1000;

    if (ct > 31104000) {
      return (ct / 31104000 + "年前");
    }
    else if (ct > 2592000) {
      return (ct / 2592000 + "个月前");
    }
    else if (ct > 172800) {
      return (ct / 86400 + "天前");
    }
    else if (ct > 86400) {
      return ("昨天");
    }
    else if (ct > 3600) {
      return (ct / 3600 + "小时前");
    }
    else if (ct > 60) {
      return (ct / 60 + "分钟前");
    }
    else if (ct > 0) {
      return (ct + "秒前");
    }
    else {
      return ("刚刚");
    }
  }

  public static String getImageWh(File file) {
    try {
      final BufferedImage image = ImageIO.read(new FileInputStream(file));
      return image.getWidth() + "x" + image.getHeight();
    }
    catch (Exception e) {
      return "error";
    }
  }

  public static String formatSize(long bytes) {
    if (bytes < DataSize.BYTES_PER_KB) {
      return bytes + "B";
    }
    if (bytes < DataSize.BYTES_PER_MB) {
      return bytes / DataSize.BYTES_PER_KB + "KB";
    }
    if (bytes < DataSize.BYTES_PER_GB) {
      return String.format("%.1fMB", (float) bytes / DataSize.BYTES_PER_MB);
    }
    if (bytes < DataSize.BYTES_PER_TB) {
      return String.format("%.2fGB", (float) bytes / DataSize.BYTES_PER_GB);
    }
    return String.format("%.2fGB", (float) bytes / DataSize.BYTES_PER_TB);
  }

  /**
   *
   */
  public static boolean base642Image(String base64ImgStr, String path) {
    // decode
    byte[] buf = Base64.getDecoder().decode(base64ImgStr.replace("data:image/png;base64,", "").getBytes());
    // 处理数据
    for (int i = 0; i < buf.length; ++i) {
      if (buf[i] < 0) {
        buf[i] += 256;
      }
    }
    try (OutputStream out = new FileOutputStream(path)) {
      out.write(buf);
      out.flush();
      return true;
    }
    catch (IOException e) {
      return false;
    }
  }

  /**
   *
   */
  public static String decodeBase64(final String str) {
    return new String(Base64.getDecoder().decode(str.getBytes(BlogConstant.DEFAULT_CHARSET)));
  }

  public static void diskInfo() {
    File[] disks = File.listRoots();
    for (File file : disks) {
      System.out.print(file.getPath() + "\t");
      System.out.print("空闲未使用 = " + file.getFreeSpace() / 1024 / 1024 / 1024 + "GB\t");// 空闲空间
      System.out.print("已经使用 = " + file.getUsableSpace() / 1024 / 1024 / 1024 + "GB\t");// 可用空间
      System.out.print("总容量 = " + file.getTotalSpace() / 1024 / 1024 / 1024 + "GB\t");// 总空间
    }
  }

  public static String image2Base64(String imgFilePath) throws IOException {
    try (InputStream inputStream = new FileInputStream(imgFilePath)) {
      byte[] bytes = StreamUtils.copyToByteArray(inputStream);
      return new String(Base64.getEncoder().encode(bytes));
    }
  }

  /**
   *
   */
  public static String getMemInfo() {
    Runtime runtime = Runtime.getRuntime();
    return runtime.freeMemory() / 1024 / 1024 + "MB/" + runtime.maxMemory() / 1024 / 1024 + "MB";
  }

  public static String getNowTime() {
    return new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(new Date());
  }

  /**
   *
   */
  public static String isoToUtf8(String str) {
    return new String(str.getBytes(StandardCharsets.ISO_8859_1), BlogConstant.DEFAULT_CHARSET);
  }

  /**
   * 编码
   */
  public static String toBase64Str(String str) {
    return new String(Base64.getEncoder().encode(str.getBytes(BlogConstant.DEFAULT_CHARSET)));
  }

  /**
   *
   */
  public static String getDiskInfo() {
    File[] disks = File.listRoots();
    long free = 0;
    long all = 0;
    for (File file : disks) {
      free = free + file.getFreeSpace();
      all = all + file.getTotalSpace();
    }
    return free / 1024 / 1024 / 1024 + "GB/" + all / 1024 / 1024 / 1024 + "GB";
  }

  /**
   *
   */
  public Set<String> getImageAddress(String htmlStr) {
    Set<String> pics = new HashSet<>();
    String img = "";
    Pattern p_image;
    Matcher m_image;
    // String regEx_img = "<img.*src=(.*?)[^>]*?>"; //图片链接地址
    String regEx_img = "<img.*src\\s*=\\s*(.*?)[^>]*?>";
    p_image = compile(regEx_img, Pattern.CASE_INSENSITIVE);
    m_image = p_image.matcher(htmlStr);
    while (m_image.find()) {
      // 得到<img />数据
      img = m_image.group();
      // 匹配<img>中的src数据
      Matcher m = compile("src\\s*=\\s*\"?(.*?)(\"|>|\\s+)").matcher(img);
      while (m.find()) {
        pics.add(m.group(1));
      }
    }
    return pics;
  }

  public static String getFirstImagePath(String htmlStr) {
    try {
      // String regEx_img = "<img.*src=(.*?)[^>]*?>"; //图片链接地址
      // String regEx_img = "<img.*src\\s*=\\s*(.*?)[^>]*?>";
      String regEx_img = "<img.*data-original\\s*=\\s*(.*?)[^>]*?>";
      Pattern p_image = compile(regEx_img, Pattern.CASE_INSENSITIVE);
      Matcher m_image = p_image.matcher(htmlStr);
      m_image.find();
      // 得到<img />数据
      // 匹配<img>中的src数据
      Matcher m = compile("data-original\\s*=\\s*\"?(.*?)(\"|>|\\s+)").matcher(m_image.group());
      m.find();
      return m.group(1);
    }
    catch (Exception e) {
      return null;
    }
  }

  public static void prepareModel(Pageable pageable, int count, Model model) {
    model.setAttribute("size", pageable.getSize());
    model.setAttribute("pageNow", pageable.getCurrent());
    model.setAttribute("count", count);
  }

  public static boolean notFound(int page, int pageCount) {
    return page > pageCount || page < 1;
  }

  public static int pageCount(int rowCount, int size) {
    return (rowCount - 1) / size + 1;
  }

  public static void main(String[] args) {

    String value = null;
    value = stripXss("<script language=text/javascript>alert(document.cookie);</script>");
    System.out.println("type-1: '" + value + "'");

    value = stripXss("<script src='' onerror='alert(document.cookie)'></script>");
    System.out.println("type-2: '" + value + "'");

    value = stripXss("</script>");
    System.out.println("type-3: '" + value + "'");

    value = stripXss(" eval(abc);");
    System.out.println("type-4: '" + value + "'");

    value = stripXss(" expression(abc);");
    System.out.println("type-5: '" + value + "'");

    value = stripXss("<img src='' onerror='alert(document.cookie);'></img>");
    System.out.println("type-6: '" + value + "'");

    value = stripXss("<img src='' onerror='alert(document.cookie);'/>");
    System.out.println("type-7: '" + value + "'");

    value = stripXss("<img src='' onerror='alert(document.cookie);'>");
    System.out.println("type-8: '" + value + "'");

    value = stripXss("<script language=text/javascript>alert(document.cookie);");
    System.out.println("type-9: '" + value + "'");

    value = stripXss("<script>window.location='url'");
    System.out.println("type-10: '" + value + "'");

    value = stripXss(" onload='alert(\"abc\");");
    System.out.println("type-11: '" + value + "'");

    value = stripXss("<img src=x<!--'<\"-->>");
    System.out.println("type-12: '" + value + "'");

    value = stripXss("<=img onstop=");
    System.out.println("type-13: '" + value + "'");

  }

}
