package cn.taketoday.blog.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import cn.taketoday.blog.BlogConstant;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.RequestContext;

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
   * 数据初始化
   *
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
  public static void exportCsv(String fileName, String content, RequestContext context) throws IOException {

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

  /**
   * demo,请勿调用！
   */
  public static void demo(RequestContext context) {
    // csv表头
    String header = "openid,手机号,红包名称,状态,导入时间,领取时间,短信状态,红包金额,兑换结果";
    // 下面 data里的key，可以说是数据库字段了
    String key = "user_openid,user_phone,gift_name,status,create_time,get_time,staff_code,gift_price,responseContent";
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    String fileName = sdf.format(new Date()) + "-慢必赔信息.csv";

    // 从数据库加载 你的数据
    List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
    String content = CSVUtils.formatCsvData(data, header, key).toString();
    try {
      CSVUtils.exportCsv(fileName, content, context);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

}
