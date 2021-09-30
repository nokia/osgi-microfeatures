package alcatel.tess.hometop.gateways.reactor.util;

import java.util.concurrent.Future;

import alcatel.tess.hometop.gateways.reactor.Reactor;

/**
 * Tasks that may be run by the SynchronousTimer class. This class is rather similar to
 * java.util.TimerTask.
 * @deprecated use {@link Reactor#schedule(Runnable, long, java.util.concurrent.TimeUnit)} 
 * and use the returned Future in order to cancel timer tasks.
 */
@Deprecated
public abstract class SynchronousTimerTask implements Runnable {
  /** Part only visible by this package, notably by the SynchronousTimer class. */
  transient volatile Future<?> future;
  transient volatile long scheduledExecutionTime;
  
  /** State variable: not yet scheduled. */
  public static final int VIRGIN = 0;
  
  /** State variable: scheduled */
  public static final int SCHEDULED = 1;
  
  /** State variable: has been executed. */
  public static final int EXECUTED = 2;
  
  /** State variable: has been cancelled. */
  public static final int CANCELLED = 3;
  
  /**
   * Default constructor.
   */
  public SynchronousTimerTask() {
  }
  
  /**
   * Cancel this task.
   * 
   * @return true if the task has not yet run, false if not.
   */
  public boolean cancel() {
    Future<?> ft = this.future;
    return ft != null ? ft.cancel(false) : false;
  }
  
  /**
   * Returns the <i>scheduled</i> execution time of the most recent <i>actual</i> execution of
   * this task.
   */
  public long scheduledExecutionTime() {
    return (scheduledExecutionTime);
  }
  
  /**
   * Return the state for this timed task.
   */
  public int getState() {
    Future<?> f = this.future;
    if (f == null) {
      return VIRGIN;
    } else if (future.isCancelled()) {
      return CANCELLED;
    } else if (future.isDone()) {
      return EXECUTED;
    } else {
      return SCHEDULED;
    }
  }
  
  /**
   * The action to be performed by this timer task.
   */
  public abstract void run();
}
