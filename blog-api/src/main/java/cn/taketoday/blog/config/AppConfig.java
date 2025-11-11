/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.blog.config;

import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import cn.taketoday.blog.log.Logging;
import cn.taketoday.blog.log.LoggingInterceptor;
import cn.taketoday.blog.service.ArticleService;
import cn.taketoday.blog.service.BloggerService;
import cn.taketoday.blog.service.LoggingService;
import cn.taketoday.blog.service.OptionService;
import cn.taketoday.blog.util.BCryptPasswordEncoder;
import cn.taketoday.blog.util.BCryptPasswordEncoder.BCryptVersion;
import cn.taketoday.blog.util.PasswordEncoder;
import infra.aop.support.DefaultPointcutAdvisor;
import infra.aop.support.annotation.AnnotationMatchingPointcut;
import infra.beans.factory.ObjectProvider;
import infra.beans.factory.annotation.DisableAllDependencyInjection;
import infra.beans.factory.config.BeanDefinition;
import infra.cache.annotation.EnableCaching;
import infra.cache.support.CaffeineCacheManager;
import infra.context.annotation.Configuration;
import infra.context.annotation.Primary;
import infra.context.annotation.Role;
import infra.core.Ordered;
import infra.core.annotation.Order;
import infra.jdbc.RepositoryManager;
import infra.persistence.EntityManager;
import infra.session.SessionManager;
import infra.session.SessionManagerOperations;
import infra.session.config.EnableSession;
import infra.stereotype.Component;
import infra.web.config.annotation.ViewControllerRegistry;
import infra.web.config.annotation.WebMvcConfigurer;
import infra.web.view.ModelAndView;
import lombok.RequiredArgsConstructor;

/**
 * App 配置
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-05-26 17:28
 */
@EnableCaching
@EnableSession
@RequiredArgsConstructor
@DisableAllDependencyInjection
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@Configuration(proxyBeanMethods = false)
class AppConfig implements WebMvcConfigurer {

  private final OptionService optionService;

  private final BloggerService bloggerService;

  private final ArticleService articleService;

  @Primary
  @Component
  public static SessionManagerOperations sessionManagerOperations(SessionManager sessionManager) {
    return new SessionManagerOperations(sessionManager);
  }

  @Component
  static PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(BCryptVersion.$2A);
  }

  @Component
  static RepositoryManager repositoryManager(DataSource dataSource) {
    return new RepositoryManager(dataSource);
  }

  @Component
  static EntityManager entityManager(RepositoryManager repositoryManager) {
    return repositoryManager.getEntityManager();
  }

  @Component
  static CaffeineCacheManager caffeineCacheManager() {
    return new CaffeineCacheManager(Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .expireAfterAccess(10, TimeUnit.SECONDS)
            .maximumSize(100));
  }

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addViewController("/sitemap.xml")
            .setContentType("application/xml")
            .setReturnValue(() -> new ModelAndView("sitemap", getModel()));

    registry.addViewController("/feed.rss")
            .setContentType("application/xml;charset=utf-8")
            .setReturnValue(() -> new ModelAndView("rss", getModel()));

    registry.addViewController("/feed.atom")
            .setContentType("application/xml;charset=utf-8")
            .setReturnValue(() -> new ModelAndView("atom", getModel()));
  }

  private Map<String, Object> getModel() {
    return Map.of(
            "sitemap", articleService.getSitemap(),
            "rss", articleService.getRss(),
            "atom", articleService.getAtom(),
            "opt", optionService.getOptionsMap(),
            "author", bloggerService.getBlogger()
    );
  }

  // 日志
  @Component
  @Order(Ordered.HIGHEST_PRECEDENCE)
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  static LoggingInterceptor loggingInterceptor(ObjectProvider<Executor> executor,
          ObjectProvider<LoggingService> loggerService, ObjectProvider<UserSessionResolver> sessionResolver) {
    return new LoggingInterceptor(executor, loggerService, sessionResolver);
  }

  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  static DefaultPointcutAdvisor pointcutAdvisor(LoggingInterceptor loggingInterceptor) {
    var pointcut = AnnotationMatchingPointcut.forMethodAnnotation(Logging.class);
    return new DefaultPointcutAdvisor(pointcut, loggingInterceptor);
  }

}

