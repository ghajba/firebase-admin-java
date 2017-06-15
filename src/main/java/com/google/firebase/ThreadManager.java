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

package com.google.firebase;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.firebase.internal.NonNull;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public abstract class ThreadManager {

  /**
   * Returns the thread configuration for an app. Implementations may return the same
   * {@link Config} for multiple apps.
   *
   * @param app A {@link FirebaseApp} instance.
   * @return A non-null {@link Config} instance.
   */
  protected abstract Config getConfig(@NonNull FirebaseApp app);

  protected abstract ThreadFactory getDatabaseThreadFactory();

  /**
   * Cleans up any thread-related resources associated with an app. This method is invoked when an
   * app is deleted.
   *
   * @param app A {@link FirebaseApp} instance.
   */
  protected abstract void cleanup(@NonNull FirebaseApp app);

  public static final class Config {
    final ListeningExecutorService executor;
    final ListeningScheduledExecutorService scheduledExecutor;

    public Config(ExecutorService executor, ScheduledExecutorService scheduledExecutor) {
      this.executor = MoreExecutors.listeningDecorator(checkNotNull(executor));
      this.scheduledExecutor = MoreExecutors.listeningDecorator(checkNotNull(scheduledExecutor));
    }
  }

}
