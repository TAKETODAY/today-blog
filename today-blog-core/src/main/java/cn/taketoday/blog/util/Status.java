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

package cn.taketoday.blog.util;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/25
 */
public class Status {

  public final boolean success;

  Status(boolean success) {
    this.success = success;
  }

  public Status onFailed(Runnable failedCmd) {
    if (!success) {
      failedCmd.run();
    }
    return this;
  }

  public Status onSuccess(Runnable okCmd) {
    if (success) {
      okCmd.run();
    }
    return this;
  }

  /**
   * @param <X> the type of the exception to be thrown
   * @param exceptionSupplier the supplier which will return the exception to be thrown
   * @throws X if there is no value present
   */
  public <X extends Throwable> Status onFailedThrow(Supplier<? extends X> exceptionSupplier) throws X {
    if (success) {
      return this;
    }
    throw exceptionSupplier.get();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Status status = (Status) o;
    return success == status.success;
  }

  @Override
  public int hashCode() {
    return Objects.hash(success);
  }

  public static Status ok() {
    return new Status(true);
  }

  public static Status failed() {
    return new Status(false);
  }

  public static Status forStatus(boolean success) {
    return new Status(success);
  }

}
