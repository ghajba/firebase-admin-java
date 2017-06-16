package com.google.firebase.database.tubesock;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.firebase.database.core.ThreadInitializer;

import java.util.concurrent.ThreadFactory;

public final class ThreadConfig {

  private final ThreadInitializer threadInitializer;
  private final ThreadFactory threadFactory;

  public ThreadConfig(ThreadInitializer threadInitializer, ThreadFactory threadFactory) {
    this.threadInitializer = checkNotNull(threadInitializer);
    this.threadFactory = checkNotNull(threadFactory);
  }

  ThreadInitializer getInitializer() {
    return threadInitializer;
  }

  ThreadFactory getThreadFactory() {
    return threadFactory;
  }
}
