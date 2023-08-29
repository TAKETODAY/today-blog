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
package cn.taketoday.blog.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.blog.BlogConstant;
import cn.taketoday.blog.config.BlogConfig;
import cn.taketoday.blog.config.CommentConfig;
import cn.taketoday.blog.model.Option;
import cn.taketoday.blog.repository.OptionRepository;
import cn.taketoday.blog.util.BlogUtils;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.stereotype.Service;
import cn.taketoday.transaction.annotation.Transactional;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.InternalServerException;
import freemarker.template.Configuration;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-03-16 21:13
 */
@Service
public class OptionService {

  private final BlogConfig blogConfig;
  private final BeanFactory beanFactory;
  private final CommentConfig commentConfig;
  private final CommentService commentService;
  private final OptionRepository optionRepository;

  private final Map<String, String> optionsMap = new HashMap<>();

  public OptionService(BlogConfig blogConfig, OptionRepository repository, CommentService commentService,
          ConfigurableEnvironment environment, BeanFactory beanFactory, CommentConfig commentConfig) {
    this.blogConfig = blogConfig;
    this.beanFactory = beanFactory;
    this.optionRepository = repository;
    this.commentService = commentService;
    this.commentConfig = commentConfig;

    for (Option option : repository.findAll()) {
      optionsMap.put(option.getName(), option.getValue());
    }

    resolveBinding(optionsMap);

    PropertySources propertySources = environment.getPropertySources();
    propertySources.addLast(new MapPropertySource("optionsMap", (Map) optionsMap));
  }

  public void saveOptions(Map<String, String> optionsMap) {
    if (CollectionUtils.isNotEmpty(optionsMap)) {
      optionsMap.forEach(this::saveOption);
    }
  }

  public void saveOption(String key, String value) {
    optionRepository.save(new Option(key, value));
    optionsMap.put(key, value);
  }

  public Map<String, String> getOptionsMap() {
    return this.optionsMap;
  }

  public void update(String key, String value) {
    String oldValue = optionsMap.get(key);
    if (oldValue == null && value != null) {
      saveOption(key, value);
    }
    else if (!Objects.equals(oldValue, value)) {
      optionRepository.update(new Option(key, value));

      optionsMap.put(key, value);
    }
  }

  @Transactional
  public void update(Map<String, String> optionsMap) {
    if (CollectionUtils.isNotEmpty(optionsMap)) {

      optionsMap.forEach(this::update);
      resolveBinding(optionsMap);

      try {
        beanFactory.getBean(Configuration.class)
                .setSharedVariable(BlogConstant.CDN, blogConfig.getCdn());
      }
      catch (TemplateModelException e) {
        throw InternalServerException.failed("cdn 更新失败", e);
      }
    }
  }

  void resolveBinding(Map<String, String> optionsMap) {
    try {
      BlogUtils.resolveBinding(blogConfig, optionsMap);
      BlogUtils.resolveBinding(commentConfig, optionsMap);

      if (!commentConfig.isSendMail()) {
        commentService.closeEmailNotification(); // TODO 将邮件分开
      }
    }
    catch (RuntimeException e) {
      LoggerFactory.getLogger(OptionService.class).error("When resolve binding options", e);
      throw InternalServerException.failed(e);
    }
  }

}
