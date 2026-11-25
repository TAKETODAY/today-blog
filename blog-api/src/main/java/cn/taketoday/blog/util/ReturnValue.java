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

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import infra.util.function.ThrowingConsumer;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/25
 */
public class ReturnValue<T extends @Nullable Object> implements Supplier<T> {

  public final T value;

  final @Nullable Throwable error;

  ReturnValue(T value, @Nullable Throwable error) {
    this.value = value;
    this.error = error;
  }

  @Override
  public T get() {
    return value;
  }

  public boolean isSuccess() {
    return error == null;
  }

  public ReturnValue<T> ifFailed(Runnable failedCmd) {
    if (error != null) {
      failedCmd.run();
    }
    return this;
  }

  public ReturnValue<T> ifFailed(Consumer<Throwable> failedCmd) {
    if (error != null) {
      failedCmd.accept(error);
    }
    return this;
  }

  /**
   * @param <X> the type of the exception to be thrown
   * @param exceptionSupplier the supplier which will return the exception to be thrown
   * @throws X if there is no value present
   */
  public <X extends Throwable> ReturnValue<T> onFailedThrow(Supplier<? extends X> exceptionSupplier) throws X {
    if (error != null) {
      throw exceptionSupplier.get();
    }
    return this;
  }

  /**
   * @param <X> the type of the exception to be thrown
   * @param exceptionSupplier the supplier which will return the exception to be thrown
   * @throws X if there is no value present
   */
  public <X extends Throwable> ReturnValue<T> onFailedThrow(Function<Throwable, X> exceptionSupplier) throws X {
    if (error != null) {
      throw exceptionSupplier.apply(error);
    }
    return this;
  }

  public ReturnValue<T> ifSuccess(Runnable okConsumer) {
    if (error == null) {
      okConsumer.run();
    }
    return this;
  }

  public ReturnValue<T> ifSuccess(ThrowingConsumer<T> okConsumer) {
    if (error == null) {
      okConsumer.accept(value);
    }
    return this;
  }

  public <U extends @Nullable Object> ReturnValue<U> map(Function<? super T, ? extends U> mapper) {
    return error != null ? ok(null) : ok(mapper.apply(value));
  }

  public static <T> ReturnValue<T> failed(Throwable error) {
    return new ReturnValue<>(null, error);
  }

  public static <T extends @Nullable Object> ReturnValue<T> ok(T value) {
    return new ReturnValue<>(value, null);
  }

}
