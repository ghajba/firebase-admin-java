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

package com.google.firebase.internal;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Default executors used for internal Firebase threads. */
public class FirebaseExecutors {

  private static final ListeningExecutorService DEFAULT_EXECUTOR;

  private static final ListeningScheduledExecutorService DEFAULT_SCHEDULED_EXECUTOR;

  static {
    if (GaeThreadFactory.isAvailable()) {
      DEFAULT_SCHEDULED_EXECUTOR = MoreExecutors.listeningDecorator(
          GaeThreadFactory.DEFAULT_EXECUTOR);
      DEFAULT_EXECUTOR = DEFAULT_SCHEDULED_EXECUTOR;
    } else {
      DEFAULT_SCHEDULED_EXECUTOR = MoreExecutors.listeningDecorator(
          Executors.newSingleThreadScheduledExecutor(Executors.defaultThreadFactory()));
      DEFAULT_EXECUTOR = MoreExecutors.listeningDecorator(
          Executors.newCachedThreadPool(Executors.defaultThreadFactory()));
    }
  }

  public static <T> ListenableFuture<T> call(Callable<T> command) {
    checkNotNull(command, "Command must not be null");
    return DEFAULT_EXECUTOR.submit(command);
  }

  public static <T> ListenableScheduledFuture<T> schedule(Callable<T> command, long delayMillis) {
    checkNotNull(command, "Command must not be null");
    return DEFAULT_SCHEDULED_EXECUTOR.schedule(command, delayMillis, TimeUnit.MILLISECONDS);
  }
}
