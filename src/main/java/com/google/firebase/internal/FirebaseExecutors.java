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

import static com.google.common.base.Preconditions.checkState;

import com.google.firebase.FirebaseApp;
import com.google.firebase.ThreadManager;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/** Default executors used for internal Firebase threads. */
public class FirebaseExecutors {

  public static final ThreadManager DEFAULT_THREAD_MANAGER;

  static {
    if (GaeThreadFactory.isAvailable()) {
      DEFAULT_THREAD_MANAGER = new GaeThreadManager();
    } else {
      DEFAULT_THREAD_MANAGER = new DefaultThreadManager();
    }
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

    /**
     * Initializes the threading resources. Called when the first application is initialized.
     */
    protected abstract void doInit();

    /**
     * Create a new {@link ThreadManager.Config} for the current environment.
     */
    protected abstract Config newConfig();

    /**
     * Cleans up the threading resources. Called when the last application is deleted.
     */
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
