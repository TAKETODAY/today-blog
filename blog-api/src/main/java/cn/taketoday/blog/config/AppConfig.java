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

package cn.taketoday.blog.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.aop.support.annotation.AnnotationMatchingPointcut;
import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.blog.aspect.Logging;
import cn.taketoday.blog.aspect.LoggingInterceptor;
import cn.taketoday.blog.service.LoggingService;
import cn.taketoday.blog.util.ObjectUtils;
import cn.taketoday.cache.annotation.EnableCaching;
import cn.taketoday.cache.support.CaffeineCacheManager;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.EnableAspectJAutoProxy;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.jdbc.RepositoryManager;
import cn.taketoday.stereotype.Component;
import cn.taketoday.web.config.WebMvcConfigurer;
import cn.taketoday.web.handler.ViewControllerHandlerMapping;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-05-26 17:28
 */
@EnableCaching
@EnableAspectJAutoProxy
@DisableAllDependencyInjection
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@Configuration(proxyBeanMethods = false)
public class AppConfig implements WebMvcConfigurer {

  @Component
  RepositoryManager repositoryManager(DataSource dataSource) {
    return new RepositoryManager(dataSource);
  }

  @Component
  public CaffeineCacheManager caffeineCacheManager() {
    return new CaffeineCacheManager(Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .maximumSize(100));
  }

  @Override
  public void configureViewController(ViewControllerHandlerMapping registry) {
    registry.addViewController("/sitemap.xml", "/core/sitemap").setContentType("application/xml");
    registry.addViewController("/rss.xml", "/feed/rss").setContentType("application/rss+xml;charset=utf-8");
    registry.addViewController("/atom.xml", "/feed/atom").setContentType("application/atom+xml;charset=utf-8");

    registry.addViewController("/NotFound", "/error/404").setStatus(404);
    registry.addViewController("/Forbidden", "/error/403").setStatus(403);
    registry.addViewController("/BadRequest", "/error/400").setStatus(400);
    registry.addViewController("/ServerIsBusy", "/error/500").setStatus(500);
    registry.addViewController("/MethodNotAllowed", "/error/405").setStatus(405);
  }

  // 异常

  @Component
  public ObjectMapper objectMapper() {
    final ObjectMapper sharedMapper = ObjectUtils.getSharedMapper();
    sharedMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
    sharedMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    // objectMapper.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
    sharedMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    sharedMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

    return sharedMapper;
  }

  // 日志
  @Component
  @Order(Ordered.HIGHEST_PRECEDENCE)
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  LoggingInterceptor loggingInterceptor(ObjectProvider<Executor> executor,
          ObjectProvider<LoggingService> loggerService, ObjectProvider<UserSessionResolver> sessionResolver) {
    return new LoggingInterceptor(executor, loggerService, sessionResolver);
  }

  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  DefaultPointcutAdvisor pointcutAdvisor(LoggingInterceptor loggingInterceptor) {
    var pointcut = AnnotationMatchingPointcut.forMethodAnnotation(Logging.class);
    return new DefaultPointcutAdvisor(pointcut, loggingInterceptor);
  }

}

