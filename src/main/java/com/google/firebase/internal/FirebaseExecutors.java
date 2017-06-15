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

import com.google.firebase.FirebaseApp;
import com.google.firebase.ThreadManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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

  static final class DefaultThreadManager extends ThreadManager {

    private ExecutorService executorService;
    private ScheduledExecutorService scheduledExecutorService;
    private final Map<String, Config> apps = new HashMap<>();

    private DefaultThreadManager() {
    }

    @Override
    protected synchronized Config init(FirebaseApp app) {
      Config config = apps.get(app.getName());
      if (config == null) {
        if (apps.isEmpty()) {
          executorService = Executors.newCachedThreadPool();
          scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        }
        config = new Config(executorService, scheduledExecutorService);
        apps.put(app.getName(), config);
      }
      return config;
    }

    @Override
    protected synchronized void cleanup(FirebaseApp app) {
      Config config = apps.remove(app.getName());
      if (config != null && apps.isEmpty()) {
        executorService.shutdownNow();
        scheduledExecutorService.shutdownNow();
        executorService = null;
        scheduledExecutorService = null;
      }
    }
  }

  static final class GaeThreadManager extends ThreadManager {

    private ScheduledExecutorService scheduledExecutorService;
    private final Map<String, Config> apps = new HashMap<>();

    private GaeThreadManager() {
    }

    @Override
    protected synchronized Config init(FirebaseApp app) {
      Config config = apps.get(app.getName());
      if (config == null) {
        if (apps.isEmpty()) {
          scheduledExecutorService = new GaeScheduledExecutorService("FirebaseDefault");
        }
        config = new Config(scheduledExecutorService, scheduledExecutorService);
        apps.put(app.getName(), config);
      }
      return config;
    }

    @Override
    protected synchronized void cleanup(FirebaseApp app) {
      Config config = apps.remove(app.getName());
      if (config != null && apps.isEmpty()) {
        scheduledExecutorService.shutdownNow();
        scheduledExecutorService = null;
      }
    }
  }
}
