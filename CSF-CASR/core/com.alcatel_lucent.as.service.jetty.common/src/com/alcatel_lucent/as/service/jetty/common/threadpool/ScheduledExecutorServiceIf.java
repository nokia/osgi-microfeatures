package com.alcatel_lucent.as.service.jetty.common.threadpool;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public interface ScheduledExecutorServiceIf {

  @SuppressWarnings("rawtypes")
  public ScheduledFuture scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit);
  @SuppressWarnings("rawtypes")
  public ScheduledFuture scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit);

}
