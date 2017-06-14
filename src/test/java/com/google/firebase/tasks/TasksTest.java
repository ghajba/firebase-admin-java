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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.util.concurrent.ListenableFuture;
import java.rmi.RemoteException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Test;

public class TasksTest {

  private static final Object RESULT = new Object();
  private static final RemoteException EXCEPTION = new RemoteException();
  private static final int SCHEDULE_DELAY_MS = 50;
  private static final int TIMEOUT_MS = 200;
  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  @Test
  public void testCall_nonNullResult() throws Exception {
    ListenableFuture<Object> task =
        Tasks.call(
            TaskExecutors.DIRECT,
            new Callable<Object>() {
              @Override
              public Object call() throws Exception {
                return RESULT;
              }
            });
    assertEquals(RESULT, task.get());
  }

  @Test
  public void testCall_nullResult() throws Exception {
    ListenableFuture<Void> task =
        Tasks.call(
            TaskExecutors.DIRECT,
            new Callable<Void>() {
              @Override
              public Void call() throws Exception {
                return null;
              }
            });
    assertNull(task.get());
  }

  @Test
  public void testCall_exception() throws Exception {
    ListenableFuture<Void> task =
        Tasks.call(
            TaskExecutors.DIRECT,
            new Callable<Void>() {
              @Override
              public Void call() throws Exception {
                throw EXCEPTION;
              }
            });
    try {
      task.get();
      fail("No error thrown");
    } catch (ExecutionException e) {
      assertEquals(EXCEPTION, e.getCause());
    }
  }

  @Test(expected = NullPointerException.class)
  public void testCall_nullCallable() {
    Tasks.call(null);
  }

  @Test(expected = NullPointerException.class)
  public void testCall_nullExecutor() {
    Tasks.call(
        null,
        new Callable<Void>() {
          @Override
          public Void call() throws Exception {
            return null;
          }
        });
  }
}
