/*
 * Copyright 2017 - 2026 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

import infra.util.StringUtils;
import infra.http.HttpHeaders;
import infra.stereotype.Component;
import infra.web.HttpContext;
import infra.web.LocaleResolver;

/**
 * 按请求的语言偏好选择博客支持的语言，无匹配或请求头无效时使用简体中文。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/18
 */
@Component(LocaleResolver.BEAN_NAME)
public final class BlogLocaleResolver implements LocaleResolver {

  private static final Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;

  private static final List<Locale> SUPPORTED_LOCALES = List.of(DEFAULT_LOCALE, Locale.ENGLISH);

  @Override
  public Locale resolveLocale(HttpContext request) {
    String header = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
    if (StringUtils.isBlank(header)) {
      return DEFAULT_LOCALE;
    }

    List<Locale.LanguageRange> ranges;
    try {
      ranges = Locale.LanguageRange.parse(header);
    }
    catch (IllegalArgumentException ex) {
      return DEFAULT_LOCALE;
    }
    Locale match = Locale.lookup(ranges, SUPPORTED_LOCALES);
    return match != null ? match : DEFAULT_LOCALE;
  }

  @Override
  public void setLocale(HttpContext request, @Nullable Locale locale) {
    throw new UnsupportedOperationException("Locale is resolved from Accept-Language");
  }
}
