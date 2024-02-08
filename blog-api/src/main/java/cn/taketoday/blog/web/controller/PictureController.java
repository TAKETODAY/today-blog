/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2024 All Rights Reserved.
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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import cn.taketoday.blog.BlogConstant;
import cn.taketoday.blog.config.AttachmentConfig;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.web.interceptor.RequestLimit;
import cn.taketoday.context.annotation.Profile;
import cn.taketoday.session.WebSession;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RestController;
import lombok.RequiredArgsConstructor;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-09-16 13:30
 */
@RestController
@Profile("dev")
@RequestMapping("/api")
@RequiredArgsConstructor
@RequestLimit(count = 5, unit = TimeUnit.MINUTES, errorMessage = "一分钟最多请求5次")
public class PictureController {

  private static final int IMG_WIDTH = 70;
  private static final int IMG_HEIGHT = 28;

  private static final char[] captchaCodes =
          "abcdefghijklmnopqrstuvwxyzABCD0123456789EFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

  private final AttachmentConfig attachmentConfig;

  private final Random randCodeSource = new Random();

  private final Font font = new Font(BlogConstant.DEFAULT_FONT, Font.PLAIN, 25);

  /**
   * Display login user avatar
   */
  @GET("/avatar")
  public BufferedImage display(User user) throws IOException {
    File localAvatarFile = attachmentConfig.getLocalFile(user.getAvatar());
    return ImageIO.read(localAvatarFile);
  }

  @GET("/captcha")
  public BufferedImage captcha(WebSession session) {
    BufferedImage image = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = (Graphics2D) image.getGraphics();
    // 1.设置背景色
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT);

    StringBuilder randCode = new StringBuilder(4);

    graphics.setColor(Color.RED);
    graphics.setFont(font);

    byte x = 2;
    Random rand = randCodeSource;
    int degree;
    char c;
    for (byte i = 0; i < 4; i++) {
      degree = rand.nextInt() % 15;
      graphics.rotate(degree * Math.PI / 180, x, 20);// 设置旋转角度
      c = captchaCodes[rand.nextInt(72)];

      randCode.append(c);
      graphics.drawString(String.valueOf(c), x, 23);
      graphics.rotate(-degree * Math.PI / 180, x, 20);// 归位
      x += 16;
    }

    session.setAttribute(BlogConstant.RAND_CODE, randCode.toString());
    return image;
  }

}
