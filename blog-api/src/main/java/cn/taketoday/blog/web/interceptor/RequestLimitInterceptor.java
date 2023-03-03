/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.blog.web.interceptor;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import cn.taketoday.blog.BlogConstant;
import cn.taketoday.blog.ErrorMessage;
import cn.taketoday.blog.util.BlogUtils;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.session.SessionHandlerInterceptor;
import cn.taketoday.session.SessionManager;
import cn.taketoday.web.HandlerInterceptor;
import cn.taketoday.web.InterceptorChain;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.HandlerMethod;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/11 10:22
 */
final class RequestLimitInterceptor extends SessionHandlerInterceptor implements HandlerInterceptor {

  private int maxCacheSize = 1024;

  private String defaultErrorMessage = "操作频繁";
  private Clock clock = Clock.system(ZoneId.of("GMT"));

  private final ExpiredChecker expiredChecker = new ExpiredChecker();
  private final ConcurrentHashMap<RequestKey, RequestLimitEntry> requestLimitCache = new ConcurrentHashMap<>();

  public RequestLimitInterceptor(SessionManager sessionManager) {
    super(sessionManager);
  }

  public void setDefaultErrorMessage(String defaultErrorMessage) {
    Assert.notNull(defaultErrorMessage, "默认的错误消息不能为空");
    this.defaultErrorMessage = defaultErrorMessage;
  }

  public void setClock(Clock clock) {
    Assert.notNull(clock, "Clock is required");
    this.clock = clock;
    removeExpiredEntries();
  }

  public void setMaxCacheSize(int maxCacheSize) {
    Assert.isTrue(maxCacheSize > 0, "最大缓存数不能小于0");
    this.maxCacheSize = maxCacheSize;
  }

  @Override
  public Object intercept(RequestContext request, InterceptorChain chain) throws Throwable {
    if (getAttribute(request, BlogConstant.BLOGGER_INFO) == null) {
      // 非博主，进行限流
      HandlerMethod handlerMethod = HandlerMethod.unwrap(chain.getHandler());
      if (handlerMethod != null) {
        RequestLimit requestLimit = handlerMethod.getMethodAnnotation(RequestLimit.class);
        if (requestLimit == null) {
          requestLimit = handlerMethod.getBeanType().getAnnotation(RequestLimit.class);
        }

        if (requestLimit != null && hasTooManyRequests(request, handlerMethod, requestLimit)) {
          return writeTooManyRequests(requestLimit);
        }
        //不需要限流
      }
    }

    // next in the chain
    return chain.proceed(request);
  }

  private ResponseEntity<ErrorMessage> writeTooManyRequests(RequestLimit requestLimit) {
    String errorMessage = requestLimit.errorMessage();
    if (Constant.DEFAULT_NONE.equals(errorMessage)) {
      errorMessage = defaultErrorMessage;
    }

    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ErrorMessage.failed(errorMessage));
  }

  private void checkMaxCacheLimit() {
    if (requestLimitCache.size() >= maxCacheSize) {
      expiredChecker.removeExpired(clock.instant());
    }
  }

  /**
   * 接口的访问频次限制
   */
  private boolean hasTooManyRequests(RequestContext request, HandlerMethod handler, RequestLimit requestLimit) {
    Instant now = clock.instant();
    expiredChecker.checkIfNecessary(now);

    Method method = handler.getMethod();
    String ip = BlogUtils.remoteAddress(request);
    RequestKey key = new RequestKey(ip, method);

    RequestLimitEntry entry = requestLimitCache.computeIfAbsent(
            key, requestKey -> new RequestLimitEntry(requestLimit));

    return entry.isExceeded(now);
  }

  /**
   * Check for expired entry and remove them.
   *
   * @since 4.0
   */
  private void removeExpiredEntries() {
    expiredChecker.removeExpired(clock.instant());
  }

  record RequestKey(String ip, Method action) {

  }

  class RequestLimitEntry {
    public final int maxCount;
    public volatile int requestCount;

    public Instant lastAccessTime = Instant.now(clock);

    public final Duration timeout;

    RequestLimitEntry(RequestLimit requestLimit) {
      long timeout = requestLimit.timeout();
      TimeUnit timeUnit = requestLimit.timeUnit();
      this.maxCount = requestLimit.count();
      this.timeout = Duration.of(timeout, timeUnit.toChronoUnit());
    }

    public boolean isNew() {
      return requestCount == 0;
    }

    // test Exceeded maximum number of requests
    public synchronized boolean isExceeded(Instant now) {
      // 判断是不是已经在规定时间之外了
      if (checkExpired(now)) {
        // 在规定时间之外直接返回不限流
        requestCount = 0;
        lastAccessTime = Instant.now(clock);
        return false;
      }
      int requestCount = this.requestCount;
      this.requestCount++;
      // 在超时时间范围内，请求次数大于最大次数就需要限制
      return requestCount >= maxCount;
    }

    public boolean isExpired() {
      return isExpired(clock.instant());
    }

    private boolean isExpired(Instant now) {
      return checkExpired(now);
    }

    private boolean checkExpired(Instant currentTime) {
      return currentTime.minus(timeout).isAfter(lastAccessTime);
    }

  }

  private final class ExpiredChecker {

    /** Max time between expiration checks. */
    private static final int CHECK_PERIOD = 10;

    private final ReentrantLock lock = new ReentrantLock();

    private Instant checkTime = clock.instant().plus(CHECK_PERIOD, ChronoUnit.SECONDS);

    public void checkIfNecessary() {
      checkIfNecessary(clock.instant());
    }

    public void checkIfNecessary(Instant now) {
      if (checkTime.isBefore(now)) {
        removeExpired(now);
      }
    }

    public void removeExpired(Instant now) {
      if (!requestLimitCache.isEmpty()) {
        if (lock.tryLock()) {
          try {
            Iterator<RequestLimitEntry> iterator = requestLimitCache.values().iterator();
            while (iterator.hasNext()) {
              RequestLimitEntry limitEntry = iterator.next();
              if (limitEntry.isExpired(now) && limitEntry.isNew()) {
                iterator.remove();
              }
            }
          }
          finally {
            this.checkTime = now.plus(CHECK_PERIOD, ChronoUnit.MILLIS);
            lock.unlock();
          }
        }
      }
    }
  }

}
