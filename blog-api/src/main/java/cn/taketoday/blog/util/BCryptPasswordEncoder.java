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

package cn.taketoday.blog.util;

import org.jspecify.annotations.Nullable;

import java.security.SecureRandom;
import java.util.regex.Pattern;

import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * Implementation of PasswordEncoder that uses the BCrypt strong hashing function. Clients
 * can optionally supply a "version" ($2a, $2b, $2y) and a "strength" (a.k.a. log rounds
 * in BCrypt) and a SecureRandom instance. The larger the strength parameter the more work
 * will have to be done (exponentially) to hash the passwords. The default value is 10.
 *
 * @author Dave Syer
 */
public class BCryptPasswordEncoder implements PasswordEncoder {

  private static final Pattern BCRYPT_PATTERN = Pattern.compile("\\A\\$2(a|y|b)?\\$(\\d\\d)\\$[./0-9A-Za-z]{53}");

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final int strength;

  private final BCryptVersion version;

  private final @Nullable SecureRandom random;

  public BCryptPasswordEncoder() {
    this(-1);
  }

  /**
   * @param strength the log rounds to use, between 4 and 31
   */
  public BCryptPasswordEncoder(int strength) {
    this(strength, null);
  }

  /**
   * @param version the version of bcrypt, can be 2a,2b,2y
   */
  public BCryptPasswordEncoder(BCryptVersion version) {
    this(version, null);
  }

  /**
   * @param version the version of bcrypt, can be 2a,2b,2y
   * @param random the secure random instance to use
   */
  public BCryptPasswordEncoder(BCryptVersion version, @Nullable SecureRandom random) {
    this(version, -1, random);
  }

  /**
   * @param strength the log rounds to use, between 4 and 31
   * @param random the secure random instance to use
   */
  public BCryptPasswordEncoder(int strength, @Nullable SecureRandom random) {
    this(BCryptVersion.$2A, strength, random);
  }

  /**
   * @param version the version of bcrypt, can be 2a,2b,2y
   * @param strength the log rounds to use, between 4 and 31
   */
  public BCryptPasswordEncoder(BCryptVersion version, int strength) {
    this(version, strength, null);
  }

  /**
   * @param version the version of bcrypt, can be 2a,2b,2y
   * @param strength the log rounds to use, between 4 and 31
   * @param random the secure random instance to use
   */
  public BCryptPasswordEncoder(BCryptVersion version, int strength, @Nullable SecureRandom random) {
    if (strength != -1 && (strength < BCrypt.MIN_LOG_ROUNDS || strength > BCrypt.MAX_LOG_ROUNDS)) {
      throw new IllegalArgumentException("Bad strength");
    }
    this.version = version;
    this.strength = (strength == -1) ? 10 : strength;
    this.random = random;
  }

  @Override
  public String encode(CharSequence rawPassword) {
    Assert.notNull(rawPassword, "rawPassword is required");
    String salt = getSalt();
    return BCrypt.hashpw(rawPassword.toString(), salt);
  }

  private String getSalt() {
    if (this.random != null) {
      return BCrypt.gensalt(this.version.getVersion(), this.strength, this.random);
    }
    return BCrypt.gensalt(this.version.getVersion(), this.strength);
  }

  @Override
  public boolean matches(CharSequence rawPassword, @Nullable String encodedPassword) {
    Assert.notNull(rawPassword, "rawPassword is required");
    if (encodedPassword == null || encodedPassword.isEmpty()) {
      this.logger.warn("Empty encoded password");
      return false;
    }
    if (!BCRYPT_PATTERN.matcher(encodedPassword).matches()) {
      this.logger.warn("Encoded password does not look like BCrypt");
      return false;
    }
    return BCrypt.checkpw(rawPassword.toString(), encodedPassword);
  }

  /**
   * Stores the default bcrypt version for use in configuration.
   *
   * @author Lin Feng
   */
  public enum BCryptVersion {

    $2A("$2a"),

    $2Y("$2y"),

    $2B("$2b");

    private final String version;

    BCryptVersion(String version) {
      this.version = version;
    }

    public String getVersion() {
      return this.version;
    }

  }

}
