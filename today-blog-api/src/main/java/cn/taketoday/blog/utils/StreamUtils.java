package cn.taketoday.blog.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cn.taketoday.blog.BlogConstant;

/**
 * @author TODAY 2020/12/22 17:36
 */
public abstract class StreamUtils extends cn.taketoday.util.StreamUtils {

  public static void transferTo(final InputStream source, final OutputStream out) throws IOException {
    transferTo(source, out, BlogConstant.BUFFER_SIZE);
  }

  public static void transferTo(final InputStream source, final OutputStream out, int bufferSize) throws IOException {
    int bytesRead;
    final byte[] buffer = new byte[bufferSize];
    while ((bytesRead = source.read(buffer)) != -1) {
      out.write(buffer, 0, bytesRead);
    }
    out.flush();
  }

}
