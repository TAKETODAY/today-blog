package cn.taketoday.blog.model.form;

import infra.persistence.annotation.Like;
import infra.persistence.annotation.Column;
import infra.persistence.annotation.OrderByClause;
import lombok.Data;

@Data
@OrderByClause("id DESC")
public class MailConditionForm {

  private String status;

  @Column("recipient")
  private String to;

  @Like
  private String subject;
}
