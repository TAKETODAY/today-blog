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

package cn.taketoday.blog.ext.ip;

import java.io.UnsupportedEncodingException;
import java.util.StringTokenizer;

@Deprecated(forRemoval = true)
public abstract class IPUtils {
  /**
   * 从ip的字符串形式得到字节数组形式
   *
   * @param ip 字符串形式的ip
   * @return 字节数组形式的ip
   */
  public static byte[] getIpByteArrayFromString(String ip) {
    byte[] ret = new byte[4];
    // 传入的ip地址有时候会带有一个单引号，去除这个单引号。发现这种异常：java.lang.NumberFormatException: For input
    // string: "1'"
    ip = ip.replaceAll("'", "");
    StringTokenizer st = new StringTokenizer(ip, ".");
    ret[0] = (byte) (Integer.parseInt(st.nextToken()) & 0xFF);
    ret[1] = (byte) (Integer.parseInt(st.nextToken()) & 0xFF);
    ret[2] = (byte) (Integer.parseInt(st.nextToken()) & 0xFF);
    ret[3] = (byte) (Integer.parseInt(st.nextToken()) & 0xFF);
    return ret;
  }

  /**
   * 对原始字符串进行编码转换，如果失败，返回原始的字符串
   *
   * @param s 原始字符串
   * @param srcEncoding 源编码方式
   * @param destEncoding 目标编码方式
   * @return 转换编码后的字符串，失败返回原始字符串
   */
  public static String getString(String s, String srcEncoding, String destEncoding) {
    try {
      return new String(s.getBytes(srcEncoding), destEncoding);
    }
    catch (UnsupportedEncodingException e) {
      return s;
    }
  }

  /**
   * 根据某种编码方式将字节数组转换成字符串
   *
   * @param b 字节数组
   * @param offset 要转换的起始位置
   * @param len 要转换的长度
   * @param encoding 编码方式
   * @return 如果encoding不支持，返回一个缺省编码的字符串
   */
  public static String getString(byte[] b, int offset, int len, String encoding) {
    try {
      return new String(b, offset, len, encoding);
    }
    catch (UnsupportedEncodingException e) {
      return new String(b, offset, len);
    }
  }

  /**
   * @param ip ip的字节数组形式
   * @return 字符串形式的ip
   */
  public static String getIpStringFromBytes(byte[] ip) {
    StringBuilder sb = new StringBuilder();
    sb.append(ip[0] & 0xFF);
    sb.append('.');
    sb.append(ip[1] & 0xFF);
    sb.append('.');
    sb.append(ip[2] & 0xFF);
    sb.append('.');
    sb.append(ip[3] & 0xFF);
    return sb.toString();
  }
}
