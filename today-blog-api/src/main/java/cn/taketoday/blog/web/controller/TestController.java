package cn.taketoday.blog.web.controller;

import cn.taketoday.session.WebSession;
import cn.taketoday.web.annotation.CookieValue;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.GetMapping;
import cn.taketoday.web.annotation.RequestHeader;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.annotation.SessionAttribute;
import lombok.Data;

@RestController
@RequestMapping("/testing")
public class TestController {

  @GetMapping("/empty")
  public void empty() {

  }

  @GetMapping("/hello")
  public String hello() {
    return "hello";
  }

  @Data
  static class RequestParams {
    @RequestHeader("X-Header")
    private String header;

    @RequestHeader("Accept-Encoding")
    private String acceptEncoding;

    private String name;

    @SessionAttribute("session")
    private String session;

    @CookieValue
    private String cookie;

    @CookieValue("Authorization")
    private String authorization;

  }

  @GET("/binder")
  public RequestParams binder(RequestParams params, WebSession session) {
    session.setAttribute("session", params.toString());
    return params;
  }

}
