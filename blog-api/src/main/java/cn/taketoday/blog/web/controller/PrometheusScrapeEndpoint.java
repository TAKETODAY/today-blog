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

package cn.taketoday.blog.web.controller;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.GetMapping;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RestController;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/5/29 15:03
 */
//@RestController
//@RequestMapping("/api/prometheus")
public class PrometheusScrapeEndpoint {
  private static final MediaType contentType = MediaType.parseMediaType(
          "text/plain;version=0.0.4;charset=utf-8");

  private static final int METRICS_SCRAPE_CHARS_EXTRA = 1024;

  private final CollectorRegistry collectorRegistry;

  private volatile int nextMetricsScrapeSize = 16;

  public PrometheusScrapeEndpoint(CollectorRegistry collectorRegistry) {
    this.collectorRegistry = collectorRegistry;
  }

  @GetMapping("/scrape")
  public ResponseEntity<String> scrape(
          RequestContext request, @RequestBody(required = false) Map<String, String> body) {
    Map<String, Object> arguments = getArguments(request, body);
    Set<String> includedNames = (Set<String>) arguments.get("includedNames");
    try {
      Writer writer = new StringWriter(nextMetricsScrapeSize);
      Enumeration<Collector.MetricFamilySamples> samples
              = includedNames != null
                ? collectorRegistry.filteredMetricFamilySamples(includedNames)
                : collectorRegistry.metricFamilySamples();
      TextFormat.write004(writer, samples);

      String scrapePage = writer.toString();
      this.nextMetricsScrapeSize = scrapePage.length() + METRICS_SCRAPE_CHARS_EXTRA;
      return ResponseEntity.ok()
              .contentType(contentType)
              .body(scrapePage);
    }
    catch (IOException ex) {
      // This actually never happens since StringWriter doesn't throw an IOException
      throw new IllegalStateException("Writing metrics failed", ex);
    }
  }

  private Map<String, Object> getArguments(RequestContext request, Map<String, String> body) {
    Map<String, Object> arguments = new LinkedHashMap<>(getTemplateVariables(request));

    if (body != null && HttpMethod.POST.equals(request.getMethod())) {
      arguments.putAll(body);
    }
    request.getParameters().forEach(
            (name, values) -> arguments.put(name, (values.length != 1) ? Arrays.asList(values) : values[0]));
    return arguments;
  }

  @Nullable
  private Map<String, String> getTemplateVariables(RequestContext request) {
    HandlerMatchingMetadata matchingMetadata = request.getMatchingMetadata();
    if (matchingMetadata != null) {
      return matchingMetadata.getUriVariables();
    }

    return null;
  }

}
