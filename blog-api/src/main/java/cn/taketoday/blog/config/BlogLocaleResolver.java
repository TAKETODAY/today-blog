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

import cn.taketoday.blog.service.OptionService;
import infra.beans.factory.annotation.Autowired;
import infra.http.HttpHeaders;
import infra.stereotype.Component;
import infra.util.StringUtils;
import infra.web.HttpContext;
import infra.web.LocaleResolver;

/**
 * 优先使用访客明确选择的语言，其次按 Accept-Language 协商，最后回退到站点默认语言。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/18
 */
@Component(LocaleResolver.BEAN_NAME)
public final class BlogLocaleResolver implements LocaleResolver {

  public static final String LOCALE_HEADER = "X-Blog-Locale";

  private static final Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;

  private @Nullable OptionService optionService;

  @Autowired(required = false)
  public void setOptionService(OptionService optionService) {
    this.optionService = optionService;
  }

  @Override
  public Locale resolveLocale(HttpContext request) {
    String selected = request.getHeader(LOCALE_HEADER);
    if ("site".equalsIgnoreCase(selected)) {
      return defaultLocale();
    }
    Locale selectedLocale = supportedLocale(selected);
    if (selectedLocale != null) {
      return selectedLocale;
    }

    String header = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
    if (StringUtils.isBlank(header)) {
      return defaultLocale();
    }

    List<Locale.LanguageRange> ranges;
    try {
      ranges = Locale.LanguageRange.parse(header);
    }
    catch (IllegalArgumentException ex) {
      return defaultLocale();
    }
    for (Locale.LanguageRange range : ranges) {
      if (range.getWeight() > 0) {
        Locale match = switch (Locale.forLanguageTag(range.getRange()).getLanguage()) {
          case "en" -> Locale.ENGLISH;
          case "zh" -> DEFAULT_LOCALE;
          default -> null;
        };
        if (match != null) {
          return match;
        }
      }
    }
    return defaultLocale();
  }

  @Override
  public void setLocale(HttpContext request, @Nullable Locale locale) {
    throw new UnsupportedOperationException("Locale is resolved from request headers and site options");
  }

  private Locale defaultLocale() {
    Locale locale = optionService == null ? null
            : supportedLocale(optionService.getOptionValue(OptionService.SITE_LOCALE));
    return locale != null ? locale : DEFAULT_LOCALE;
  }

  private static @Nullable Locale supportedLocale(@Nullable String value) {
    if ("en-US".equalsIgnoreCase(value) || "en".equalsIgnoreCase(value)) {
      return Locale.ENGLISH;
    }
    if ("zh-CN".equalsIgnoreCase(value) || "zh".equalsIgnoreCase(value)) {
      return DEFAULT_LOCALE;
    }
    return null;
  }
}
