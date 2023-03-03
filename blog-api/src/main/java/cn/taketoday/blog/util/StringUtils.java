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

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;

import cn.taketoday.blog.BlogConstant;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-04-08 16:43
 */
public abstract class StringUtils extends cn.taketoday.util.StringUtils {

  /**
   * Delete the input string text html
   *
   * @param htmlStr input text
   * @return none html text
   */
  public static String delHtml(String htmlStr) {
    String blank = BlogConstant.BLANK;
    if (htmlStr == null) {
      return blank;
    }
    return htmlStr
            .replaceAll(" ", blank)
            .replaceAll(BlogConstant.HTML_TAG, blank)
            .replaceAll("&nbsp;", blank);
  }

  public static String getRandomImageName(String name) {
    if (name == null) {
      name = BlogConstant.BLANK;
    }
    if (name.lastIndexOf('.') <= -1) {
      name = name.concat(".png");
    }
    return StringUtils.getUUIDString().split("-")[0].concat(name);
  }

  private static Map<Integer, String> keyMap = new HashMap<Integer, String>(); // 用于封装随机产生的公钥与私钥

  public static void main(String[] args) throws Exception {
    // 生成公钥和私钥
    genKeyPair();
    // 加密字符串
    String message = "df723820";
    System.out.println("随机生成的公钥为:" + keyMap.get(0));
    System.out.println("随机生成的私钥为:" + keyMap.get(1));
    String messageEn = encrypt(message, keyMap.get(0));
    System.out.println(message + "\t加密后的字符串为:" + messageEn);
    String messageDe = decrypt(messageEn, keyMap.get(1));
    System.out.println("还原后的字符串为:" + messageDe);
  }

  /**
   * 随机生成密钥对
   *
   * @throws NoSuchAlgorithmException
   */
  public static void genKeyPair() throws NoSuchAlgorithmException {

    // KeyPairGenerator类用于生成公钥和私钥对，基于RSA算法生成对象
    KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
    // 初始化密钥对生成器，密钥大小为96-1024位
    keyPairGen.initialize(1024, new SecureRandom());
    // 生成一个密钥对，保存在keyPair中
    KeyPair keyPair = keyPairGen.generateKeyPair();
    RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate(); // 得到私钥
    RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic(); // 得到公钥
    String publicKeyString = new String(Base64.getEncoder().encode(publicKey.getEncoded()));
    // 得到私钥字符串
    String privateKeyString = new String(Base64.getEncoder().encode(privateKey.getEncoded()));
    // 将公钥和私钥保存到Map
    keyMap.put(0, publicKeyString); // 0表示公钥
    keyMap.put(1, privateKeyString); // 1表示私钥
  }

  /**
   * RSA公钥加密
   *
   * @param str 加密字符串
   * @param publicKey 公钥
   * @return 密文
   * @throws Exception 加密过程中的异常信息
   */
  public static String encrypt(String str, String publicKey) throws Exception {
    // base64编码的公钥
    byte[] decoded = Base64.getDecoder().decode(publicKey);
    PublicKey pubKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
    // RSA加密
    Cipher cipher = Cipher.getInstance("RSA");
    cipher.init(Cipher.ENCRYPT_MODE, pubKey);

    return Base64.getEncoder().encodeToString(cipher.doFinal(str.getBytes(StandardCharsets.UTF_8)));
  }

  /**
   * RSA私钥解密
   *
   * @param str 加密字符串
   * @param privateKey 私钥
   * @return 铭文
   * @throws Exception 解密过程中的异常信息
   */
  public static String decrypt(String str, String privateKey) throws Exception {
    // 64位解码加密后的字符串
    byte[] inputByte = Base64.getDecoder().decode(str.getBytes(StandardCharsets.UTF_8));
    // base64编码的私钥
    byte[] decoded = Base64.getDecoder().decode(privateKey);
    PrivateKey priKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
    // RSA解密
    Cipher cipher = Cipher.getInstance("RSA");
    cipher.init(Cipher.DECRYPT_MODE, priKey);
    return new String(cipher.doFinal(inputByte));
  }
}
