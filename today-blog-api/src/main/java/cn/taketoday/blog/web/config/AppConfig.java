/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
package cn.taketoday.blog.web.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.blog.aspect.LoggingInterceptor;
import cn.taketoday.blog.config.UserSessionResolver;
import cn.taketoday.blog.service.LoggingService;
import cn.taketoday.blog.utils.ObjectUtils;
import cn.taketoday.cache.annotation.EnableCaching;
import cn.taketoday.cache.support.CaffeineCacheManager;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Primary;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.core.env.Environment;
import cn.taketoday.jdbc.RepositoryManager;
import cn.taketoday.jdbc.config.JdbcProperties;
import cn.taketoday.jdbc.core.JdbcOperations;
import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.jdbc.datasource.DataSourceTransactionManager;
import cn.taketoday.jdbc.support.JdbcTransactionManager;
import cn.taketoday.stereotype.Component;
import cn.taketoday.stereotype.Singleton;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.support.TransactionTemplate;
import cn.taketoday.web.config.WebMvcConfiguration;
import cn.taketoday.web.registry.ViewControllerHandlerMapping;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-05-26 17:28
 */
@EnableCaching
@EnableConfigurationProperties(JdbcProperties.class)
@Configuration(proxyBeanMethods = false)
public class AppConfig implements WebMvcConfiguration {

  private final ThreadPoolExecutor executor =
          new ThreadPoolExecutor(4, 20,
                  10, TimeUnit.SECONDS,
                  new LinkedBlockingQueue<>(500),
                  new ThreadPoolExecutor.CallerRunsPolicy());

  @Singleton
  public ThreadPoolExecutor executor() {
    return executor;
  }

  @Primary
  @Singleton
  JdbcTemplate jdbcTemplate(DataSource dataSource, JdbcProperties properties) {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    JdbcProperties.Template template = properties.getTemplate();
    jdbcTemplate.setFetchSize(template.getFetchSize());
    jdbcTemplate.setMaxRows(template.getMaxRows());
    if (template.getQueryTimeout() != null) {
      jdbcTemplate.setQueryTimeout((int) template.getQueryTimeout().getSeconds());
    }
    return jdbcTemplate;
  }

  @Bean
  public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
    return new TransactionTemplate(transactionManager);
  }

  @Singleton
  DataSourceTransactionManager transactionManager(@Qualifier("repositoryManager") RepositoryManager s, Environment environment, DataSource dataSource) {
    return createTransactionManager(environment, dataSource);
  }

  private DataSourceTransactionManager createTransactionManager(Environment environment, DataSource dataSource) {
    return environment.getProperty("dao.exceptiontranslation.enabled", Boolean.class, Boolean.TRUE)
           ? new JdbcTransactionManager(dataSource) : new DataSourceTransactionManager(dataSource);
  }

  @Component
  RepositoryManager repositoryManager(JdbcOperations op, DataSource dataSource) {
    return new RepositoryManager(dataSource);
  }

  @Singleton
  public CaffeineCacheManager caffeineCacheManager(RepositoryManager s) {
    final CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    cacheManager.setCaffeine(
            Caffeine.newBuilder()
                    .expireAfterWrite(10, TimeUnit.SECONDS)
                    .maximumSize(100)
    );
    return cacheManager;
  }

//  @Singleton
//  @Profile("prod")
//  public ResourceHandlerRegistry prodResourceMappingRegistry(ApplicationContext context) {
//
//    final ResourceHandlerRegistry registry = new ResourceHandlerRegistry(context);
//
//    registry.addResourceHandler(AdminInterceptor.class)//
//            .setPathPatterns("/assets/admin/**")//
//            .setOrder(Ordered.HIGHEST_PRECEDENCE)//
//            .addLocations("/assets/admin/");
//
//    return registry;
//  }

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

  @Singleton
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
  @Singleton
  @Order(Ordered.HIGHEST_PRECEDENCE)
  LoggingInterceptor loggingInterceptor(
          ThreadPoolExecutor executor,
          LoggingService loggerService,
          UserSessionResolver sessionResolver) {
    return new LoggingInterceptor(executor, loggerService, sessionResolver);
  }

//  @Singleton
//  NettyWebServer nettyWebServer() {
//    final NettyWebServer nettyWebServer = new NettyWebServer();
//    nettyWebServer.setChildThreadCount(5);
//    nettyWebServer.setParentThreadCount(5);
//    return nettyWebServer;
//  }
}

