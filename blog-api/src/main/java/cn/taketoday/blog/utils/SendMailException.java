package cn.taketoday.blog.utils;

public class SendMailException extends Exception {

  private static final long serialVersionUID = -3587825188366930579L;

  public SendMailException() { }

  public SendMailException(String message) {
    super(message);
  }

  public SendMailException(Throwable cause) {
    super(cause);
  }
}
