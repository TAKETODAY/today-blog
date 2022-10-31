package cn.taketoday.blog;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY
 * @since 2020/12/13 21:55
 */
@Getter
@Setter
@AllArgsConstructor
public class ValidationError {

  private Object validation;

  public static ValidationError failed(Object validation) {
    return new ValidationError(validation);
  }

}
