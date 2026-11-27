package cn.taketoday.blog.web.console;

import cn.taketoday.blog.model.Mail;
import cn.taketoday.blog.model.form.MailConditionForm;
import cn.taketoday.blog.service.MailQueueService;
import cn.taketoday.blog.web.Pageable;
import cn.taketoday.blog.web.Pagination;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import infra.web.annotation.GET;
import infra.web.annotation.POST;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RequiresBlogger
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/console/mails")
class MailConsoleHttpHandler {

  private final MailQueueService mailQueueService;

  @GET
  public Pagination<Mail> list(MailConditionForm form, Pageable pageable) {
    return mailQueueService.pagination(form, pageable);
  }

  @POST("/{id}/retry")
  public void retry(@PathVariable long id) {
    mailQueueService.retry(id);
  }
}
