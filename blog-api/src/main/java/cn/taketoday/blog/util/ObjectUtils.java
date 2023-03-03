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

package cn.taketoday.blog.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import cn.taketoday.web.RequestContext;

/**
 * @author TODAY 2021/3/10 18:23
 */
public abstract class ObjectUtils {

  private static ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
  }

  // JSON

  public static ObjectMapper getSharedMapper() {
    return objectMapper;
  }

  public static void setSharedMapper(ObjectMapper objectMapper) {
    ObjectUtils.objectMapper = objectMapper;
  }

  /**
   * javaBean、列表数组转换为json字符串
   */
  public static String toJSON(Object obj) throws IOException {
    return objectMapper.writeValueAsString(obj);
  }

  /**
   * json 转JavaBean
   */
  public static <T> T fromJSON(String jsonString, Class<T> clazz) throws IOException {
    return objectMapper.readValue(jsonString, clazz);
  }

  /**
   * writeValue
   */
  public static void writeValue(OutputStream out, Object object) throws IOException {
    objectMapper.writeValue(out, object);
  }

  public static void writeValue(RequestContext context, Object value) throws IOException {
    objectMapper.writeValue(context.getWriter(), value);
  }

  /**
   * json字符串转换为map
   */
  public static Map<String, Object> toMap(String jsonString) throws IOException {
    return objectMapper.readValue(jsonString, Map.class);
  }

}
