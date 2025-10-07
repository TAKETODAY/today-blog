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

package cn.taketoday.blog.service;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.blog.BlogConstant;
import cn.taketoday.blog.ConfigBinding;
import cn.taketoday.blog.event.OptionsUpdateEvent;
import cn.taketoday.blog.model.Option;
import cn.taketoday.blog.util.StringUtils;
import infra.beans.BeanMetadata;
import infra.beans.BeanProperty;
import infra.beans.factory.BeanFactory;
import infra.context.ApplicationEventPublisher;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.EnumerablePropertySource;
import infra.lang.Unmodifiable;
import infra.persistence.EntityManager;
import infra.stereotype.Service;
import infra.transaction.annotation.Transactional;
import infra.util.CollectionUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-03-16 21:13
 */
@Service
public class OptionService {

  private final BeanFactory beanFactory;

  private final EntityManager entityManager;

  private final ConfigurableEnvironment environment;

  private final ApplicationEventPublisher eventPublisher;

  private final OptionsPropertySource optionsPropertySource = new OptionsPropertySource("optionsMap", this);

  public OptionService(BeanFactory beanFactory, EntityManager entityManager,
          ConfigurableEnvironment environment, ApplicationEventPublisher eventPublisher) {
    this.beanFactory = beanFactory;
    this.entityManager = entityManager;
    this.environment = environment;
    this.eventPublisher = eventPublisher;
    environment.getPropertySources().addLast(optionsPropertySource);
    onUpdate();
  }

  /**
   * @since 3.2
   */
  public void persist(Option value) {
    entityManager.persist(value);
  }

  /**
   * @since 3.2
   */
  public void persist(List<Option> value) {
    entityManager.persist(value);
  }

  @Unmodifiable
  public Map<String, String> getOptionsMap() {
    return Collections.unmodifiableMap(optionsPropertySource.optionsMap);
  }

  @Unmodifiable
  public Map<String, String> publicOptions() {
    return Collections.unmodifiableMap(optionsPropertySource.publicOptionsMap);
  }

  /**
   * @since 3.2
   */
  public void update(Option option) {
    entityManager.updateById(option);
  }

  @Transactional
  public void update(Map<String, String> optionsMap) {
    if (CollectionUtils.isNotEmpty(optionsMap)) {
      for (Map.Entry<String, String> entry : optionsMap.entrySet()) {
        update(new Option(entry.getKey(), entry.getValue()));
      }
      onUpdate();
    }
  }

  /**
   * @since 3.2
   */
  @Transactional
  public void update(List<Option> options) {
    options.forEach(entityManager::updateById);
    onUpdate();
  }

  private void onUpdate() {
    optionsPropertySource.updateCache();
    eventPublisher.publishEvent(new OptionsUpdateEvent(this));
  }

  // ------------------------------------------------------------------------
  // Implementation EnumerablePropertySource
  // ------------------------------------------------------------------------

  private final static class OptionsPropertySource extends EnumerablePropertySource<OptionService> {

    private final HashMap<String, String> optionsMap = new HashMap<>();

    private final HashMap<String, String> publicOptionsMap = new HashMap<>();

    public OptionsPropertySource(String name, OptionService source) {
      super(name, source);
    }

    public synchronized void updateCache() {
      optionsMap.clear();
      publicOptionsMap.clear();
      List<Option> options = source.entityManager.find(Option.class);
      for (Option option : options) {
        optionsMap.put(option.getName(), option.getValue());
        if (Boolean.TRUE.equals(option.getOpen())) {
          publicOptionsMap.put(option.getName(), option.getValue());
        }
      }

      List<Object> configBindingBeans = source.beanFactory.getAnnotatedBeans(ConfigBinding.class);
      for (Object configBindingBean : configBindingBeans) {
        resolveBinding(configBindingBean);
      }
    }

    @Nullable
    @Override
    public synchronized String getProperty(String name) {
      return optionsMap.get(name);
    }

    @Override
    public synchronized boolean containsProperty(String name) {
      return optionsMap.containsKey(name);
    }

    @Override
    public synchronized String[] getPropertyNames() {
      return StringUtils.toStringArray(optionsMap.keySet());
    }

    private void resolveBinding(Object bean) {
      Class<?> beanClass = bean.getClass();
      String prefix = getPrefix(beanClass);
      var conversionService = source.environment.getConversionService();
      for (BeanProperty property : BeanMetadata.forClass(beanClass)) {
        String key = getKey(prefix, property);
        String value = getProperty(key);
        if (value != null) {
          property.setValue(bean, value, conversionService);
        }
      }
    }

    private static String getPrefix(Class<?> beanClass) {
      ConfigBinding annotation = beanClass.getAnnotation(ConfigBinding.class);
      if (annotation != null) {
        return annotation.value();
      }
      return BlogConstant.BLANK;
    }

    private static String getKey(String prefix, BeanProperty field) {
      ConfigBinding bindingOnField = field.getAnnotation(ConfigBinding.class);
      if (bindingOnField != null) {
        if (bindingOnField.combine()) {
          return prefix + bindingOnField.value();
        }
        else {
          return bindingOnField.value();
        }
      }
      else {
        return prefix + field.getName();
      }
    }
  }

}
