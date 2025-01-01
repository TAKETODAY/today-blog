/*
 * Copyright 2017 - 2024 the original author or authors.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import cn.taketoday.blog.model.IpLocation;
import infra.lang.Nullable;
import infra.stereotype.Component;
import infra.web.annotation.RequestParam;
import infra.web.client.ClientResponse;
import infra.web.client.RestClient;
import infra.web.client.RestClientException;
import infra.web.client.support.RestClientAdapter;
import infra.web.service.annotation.GetExchange;
import infra.web.service.annotation.HttpExchange;
import infra.web.service.invoker.HttpServiceProxyFactory;
import lombok.Data;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.2 2024/11/14 22:51
 */
@Component
public class IpLocationService {

  private static final Logger log = LoggerFactory.getLogger(IpLocationService.class);

  // https://webapi-pc.meitu.com/common/ip_location?ip=
  private final RestClient restClient = RestClient.builder().ignoreStatus().build();

  private final Client client = HttpServiceProxyFactory.forAdapter(RestClientAdapter.create(restClient))
          .createClient(Client.class);

  @Nullable
  public IpLocation lookup(String ip) {
    try {
      var response = client.ipLocation(ip);
      if (response.getStatusCode().is2xxSuccessful()) {
        Body body = response.bodyTo(Body.class);
        if (body != null) {
          BodyData bodyData = body.data.get(ip);
          if (bodyData != null) {
            // todo area
            return new IpLocation(bodyData.nation, bodyData.province, bodyData.city, null, bodyData.isp);
          }
        }
      }
      else {
        log.warn("IP lookup failed: [{}]", response.getStatusCode());
      }
    }
    catch (RestClientException e) {
      log.error("IP lookup failed", e);
    }
    return null;
  }

  @HttpExchange(url = "https://webapi-pc.meitu.com")
  interface Client {

    @GetExchange("/common/ip_location")
    ClientResponse ipLocation(@RequestParam String ip);

    @Nullable
    @GetExchange("/common/ip_location")
    Body ipLocation();

  }

  @Data
  static class Body {
    public int code;

    public Map<String, BodyData> data;
  }

  @Data
  static class BodyData {

    public String nation;

    public String province;

    public String city;

    public String isp;

  }

}
