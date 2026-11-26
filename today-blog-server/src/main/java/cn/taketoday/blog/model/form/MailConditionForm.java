package cn.taketoday.blog.model.form;

import cn.taketoday.blog.model.enums.MailStatus;
import infra.persistence.annotation.Column;
import infra.persistence.annotation.Like;
import infra.persistence.annotation.OrderByClause;
import lombok.Data;

@Data
@OrderByClause("id DESC")
public class MailConditionForm {

  private MailStatus status;

  @Column("recipient")
  private String to;

  @Like
  private String subject;
}
