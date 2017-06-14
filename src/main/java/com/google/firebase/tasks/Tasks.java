/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.firebase.tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.firebase.internal.GuardedBy;
import com.google.firebase.internal.NonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class Tasks {

  private Tasks() {}

  /**
   * Returns a Task that will be completed with the result of the specified Callable.
   *
   * <p>The Callable will be called on a shared thread pool.
   */
  public static <T> ListenableFuture<T> call(@NonNull Callable<T> callable) {
    return call(TaskExecutors.DEFAULT_THREAD_POOL, callable);
  }

  /**
   * Returns a Task that will be completed with the result of the specified Callable.
   *
   * @param executor the Executor to use to call the Callable
   */
  public static <T> ListenableFuture<T> call(@NonNull ListeningExecutorService executor,
      @NonNull final Callable<T> callable) {
    checkNotNull(executor, "Executor must not be null");
    checkNotNull(callable, "Callback must not be null");
    return executor.submit(callable);
  }
}
