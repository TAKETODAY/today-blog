package cn.taketoday.blog.utils;

import java.io.IOException;
import java.io.Writer;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.WritableResource;

/**
 * @author TODAY 2021/3/28 19:00
 */
public class RemoveAscii {

  public static void main(String[] args) throws IOException {
    ClassPathResource resource = new ClassPathResource("text");
    String content = StreamUtils.copyToString(resource.getInputStream());
    StringBuilder builder = new StringBuilder(content.length());

    char[] chars = content.toCharArray();

    for (char aChar : chars) {
      if (aChar >= 128 || aChar == ',' || aChar == '\r' || aChar == '\n') {
        builder.append(aChar);
      }
    }

    Resource originalResource = resource.getOriginalResource();

    if (originalResource instanceof WritableResource writableResource) {
      Writer writer = writableResource.getWriter();
      writer.write(builder.toString());

      writer.flush();
      writer.close();
    }
    System.err.println(builder);

  }
}
