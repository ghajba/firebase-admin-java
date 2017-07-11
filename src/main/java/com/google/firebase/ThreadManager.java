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

/**
 * An interface that controls the thread pools and factories used by the Admin SDK. Each
 * instance of {@link FirebaseApp} uses an implementation of this interface to create and manage
 * threads. Multiple app instances may use a single, shared <code>ThreadManager</code> instance.
 * Methods in this interface (except for cleanup), may get invoked multiple times by the same
 * app, during its lifetime.
 */
public abstract class ThreadManager {

  /**
   * Returns the thread pools for an app. Implementations may return the same instance of
   * {@link ThreadPools} for multiple apps. The returned pools are used by all components
   * of an app except for the realtime database. Database has far stricter and complicated
   * threading requirements, and thus initializes its own thread pools using the
   * factory returned by {@link ThreadManager#getDatabaseThreadFactory(FirebaseApp)}.
   *
   * @param app A {@link FirebaseApp} instance.
   * @return A non-null {@link ThreadPools} instance.
   */
  protected abstract ThreadPools getThreadPools(@NonNull FirebaseApp app);

  /**
   * Returns the <code>ThreadFactory</code> to be used for creating threads in the Realtime
   * database component. This is used to create the run loop, event target and web socket
   * reader/writer threads.
   *
   * @return A non-null <code>ThreadFactory</code>.
   */
  protected abstract ThreadFactory getDatabaseThreadFactory(@NonNull FirebaseApp app);

  /**
   * Cleans up any thread-related resources associated with an app. This method is invoked when an
   * app is deleted.
   *
   * @param app A {@link FirebaseApp} instance.
   */
  protected abstract void cleanup(@NonNull FirebaseApp app);

  /**
   * A collection of thread pools for running background tasks in the Admin SDK. Primarily
   * consists of an <code>ExecutorService</code> and a <code>ScheduledExecutorService</code>.
   * The former is used to run most of the async tasks initiated by the SDK (except the tasks
   * started by the database code). The latter is used for periodic scheduled tasks started by
   * the SDK such as proactive token refresh.
   */
  public static final class ThreadPools {
    final ListeningExecutorService executor;
    final ListeningScheduledExecutorService scheduledExecutor;

    public ThreadPools(ExecutorService executor, ScheduledExecutorService scheduledExecutor) {
      this.executor = MoreExecutors.listeningDecorator(checkNotNull(executor));
      this.scheduledExecutor = MoreExecutors.listeningDecorator(checkNotNull(scheduledExecutor));
    }
  }

}
