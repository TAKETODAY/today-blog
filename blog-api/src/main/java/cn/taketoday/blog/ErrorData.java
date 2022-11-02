package cn.taketoday.blog;

import cn.taketoday.blog.utils.Result;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author TODAY
 * @since 2020/12/13 21:54
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorData implements Result {

  private Object data;

  public static ErrorData failed(Object data) {
    return new ErrorData(data);
  }

  public static ErrorData failed() {
    return new ErrorData("未知错误");
  }
}
