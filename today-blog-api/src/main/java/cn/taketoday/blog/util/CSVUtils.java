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

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import cn.taketoday.blog.BlogConstant;
import infra.http.HttpHeaders;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.HttpContext;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-10-03 20:40
 */
public class CSVUtils {
  private final static Logger logger = LoggerFactory.getLogger(CSVUtils.class);

  /** CSV文件列分隔符 */
  private static final String CSV_COLUMN_SEPARATOR = ",";

  /** CSV文件列分隔符 */
  private static final String CSV_RN = "\r\n";

  /**
   * @param datas 数据库查出来的数据
   * @param displayColNames csv表头
   * @param matchColNames data中的key ，可以说是数据库字段了,原本为”0001”类型的数据在excel中打开会被默认改变为”1”的数据。 解决方法
   * :key前加"'"用于特殊处理；
   * 例如
   * 输入列名为"num"数字为 001，则传入的key值为"-num",保证输出为字符串
   */
  public static StringBuilder formatCsvData(
          List<Map<String, Object>> datas, String displayColNames, String matchColNames) {

    StringBuilder buf = new StringBuilder();

    String[] matchColNamesMapArr = matchColNames.split(",");

    buf.append(displayColNames)//
            .append(CSV_RN);

    if (null != datas) {
      for (Map<String, Object> data : datas) {
        for (String matchColName : matchColNamesMapArr) {
          Object object = data.get(matchColName);
          buf.append(object).append(CSV_COLUMN_SEPARATOR);
        }
        buf.append(CSV_RN);
      }
    }
    logger.info("csv file Initialize successfully");
    return buf;
  }

  /**
   * 导出
   *
   * @param fileName 文件名
   * @param content 内容
   */
  public static void exportCsv(String fileName, String content, HttpContext context) throws IOException {

    // 读取字符编码
    HttpHeaders responseHeaders = context.responseHeaders();
    responseHeaders.set("Pragma", "public");
    responseHeaders.set("Cache-Control", "max-age=30");
    // 设置响应
    context.setContentType("text/csv; charset=UTF-8");

    HttpHeaders requestHeaders = context.requestHeaders();
    String userAgent = requestHeaders.getFirst("User-Agent");
    if (userAgent != null && userAgent.contains("MSIE")) {// IE浏览器
      fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
    }
    else if (userAgent != null && userAgent.contains("Mozilla")) {// google,火狐浏览器
      fileName = new String(fileName.getBytes(), StandardCharsets.ISO_8859_1);
    }
    else {
      fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);// 其他浏览器
    }
    responseHeaders.set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

    // 写出响应
    OutputStream os = context.getOutputStream();
    os.write(content.getBytes(BlogConstant.DEFAULT_CHARSET));
    os.flush();
    os.close();
    logger.info("csv file download completed");
  }

}
