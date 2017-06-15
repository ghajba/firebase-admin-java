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
import static com.google.common.base.Preconditions.checkState;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import com.google.firebase.FirebaseApp;
import com.google.firebase.ThreadManager;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/** Default executors used for internal Firebase threads. */
public class FirebaseExecutors {

  private static final ListeningExecutorService DEFAULT_EXECUTOR;
  private static final ListeningScheduledExecutorService DEFAULT_SCHEDULED_EXECUTOR;

  public static final ThreadManager DEFAULT_THREAD_MANAGER = new DefaultThreadManager();

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

  private static <T> ListenableFuture<T> call(Callable<T> command) {
    checkNotNull(command, "Command must not be null");
    return DEFAULT_EXECUTOR.submit(command);
  }

  private static <T> ListenableScheduledFuture<T> schedule(Callable<T> command, long delayMillis) {
    checkNotNull(command, "Command must not be null");
    return DEFAULT_SCHEDULED_EXECUTOR.schedule(command, delayMillis, TimeUnit.MILLISECONDS);
  }

  private abstract static class GlobalThreadManager extends ThreadManager {

    private final Map<String, Config> apps = new HashMap<>();

    @Override
    protected final synchronized Config getConfig(FirebaseApp app) {
      Config config = apps.get(app.getName());
      if (config == null) {
        if (apps.isEmpty()) {
          doInit();
        }
        config = newConfig();
        apps.put(app.getName(), config);
      }
      return config;
    }

    @Override
    protected final synchronized void cleanup(FirebaseApp app) {
      Config config = apps.remove(app.getName());
      if (config != null && apps.isEmpty()) {
        doCleanup();
      }
    }

    protected abstract void doInit();

    protected abstract Config newConfig();

    protected abstract void doCleanup();
  }

  private static class DefaultThreadManager extends GlobalThreadManager {
    private ExecutorService executor;
    private ScheduledExecutorService scheduledExecutor;

    @Override
    protected void doInit() {
      executor = Executors.newCachedThreadPool();
      scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    protected Config newConfig() {
      return new Config(executor, scheduledExecutor);
    }

    @Override
    protected void doCleanup() {
      executor.shutdownNow();
      scheduledExecutor.shutdownNow();
      executor = null;
      scheduledExecutor = null;
    }

    @Override
    protected ThreadFactory getDatabaseThreadFactory() {
      return Executors.defaultThreadFactory();
    }
  }

  private static class GaeThreadManager extends GlobalThreadManager {
    private ScheduledExecutorService scheduledExecutor;

    @Override
    protected void doInit() {
      scheduledExecutor = new GaeScheduledExecutorService("FirebaseDefault");
    }

    @Override
    protected Config newConfig() {
      return new Config(scheduledExecutor, scheduledExecutor);
    }

    @Override
    protected void doCleanup() {
      scheduledExecutor.shutdownNow();
      scheduledExecutor = null;
    }

    @Override
    protected ThreadFactory getDatabaseThreadFactory() {
      GaeThreadFactory threadFactory = GaeThreadFactory.getInstance();
      checkState(threadFactory.isUsingBackgroundThreads(),
          "Failed to initialize a GAE background thread factory");
      return threadFactory;
    }
  }
}
