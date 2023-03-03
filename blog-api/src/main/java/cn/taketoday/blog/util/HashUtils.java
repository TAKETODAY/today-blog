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

import java.util.Random;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-08-05 12:32
 */
public abstract class HashUtils {
  private static final Random random = new Random();

  public static String getEncodedPassword(String password) {
    if (StringUtils.isEmpty(password)) {
      return password;
    }
    return getHash(password);
  }

  public static String getMD5Str(String str) {
    return new MD5().getMD5Str(str);
  }

  public static String getHash(String str) {
    final MD5 md5 = new MD5();
    return md5.getMD5Str(md5.getMD5Str(str));
  }

  public static String getInitialPassword() {
    return getRandomHashString(8);
  }

  public static String getRandomHashString(int length) {
    final char[] password = new char[length];
    final Random random = HashUtils.random;
    for (int i = 0; i < length; i++) {
      password[i] = generateRandomCharacter(random.nextInt(3));
    }
    return String.valueOf(password);
  }

  private static char generateRandomCharacter(int type) {
    int rand;
    switch (type) {
      case 0://随机小写字母
        rand = random.nextInt(26);
        rand += 97;
        return (char) rand;
      case 1://随机大写字母
        rand = random.nextInt(26);
        rand += 65;
        return (char) rand;
      case 2://随机数字
      default:
        rand = random.nextInt(10);
        rand += 48;
        return (char) rand;
    }
  }
}
