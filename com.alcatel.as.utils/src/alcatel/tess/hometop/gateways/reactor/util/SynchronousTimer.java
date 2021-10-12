// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.util;

// Jdk
import java.util.concurrent.Future;

import alcatel.tess.hometop.gateways.reactor.Reactor;

/**
 * Class used to assign a future object into a SynchronousTimerTask 
 * attribute. Only used internally, by reactor impl. (deprecated).
 * @deprecated use {@link Reactor#schedule(Runnable, long, java.util.concurrent.TimeUnit)} 
 * and use the returned Future in order to cancel timer tasks.
 */
@Deprecated
public class SynchronousTimer {
  public static void setFuture(SynchronousTimerTask task, Future<?> future) {
    task.future = future;
  }
  
  public static Future<?> getFuture(SynchronousTimerTask task) {
    return task.future;
  }
  
  public static void setScheduledExecutionTime(SynchronousTimerTask task, long scheduledExecutionTime) {
    task.scheduledExecutionTime = scheduledExecutionTime;
  }
  
  public static long getNextExecutionTime(SynchronousTimerTask task) {
    return task.scheduledExecutionTime;
  }
}
