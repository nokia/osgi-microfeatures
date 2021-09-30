package alcatel.tess.hometop.gateways.concurrent;

// Jdk
import java.lang.reflect.Method;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import alcatel.tess.hometop.gateways.utils.Config;
import alcatel.tess.hometop.gateways.utils.ConfigException;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.metering.MeteringService;
import com.alcatel.as.util.serviceloader.ServiceLoader;

/**
 * Thread manager. This class internally uses the PlatformExecutors service, and you should not use it anymore.
 * @deprecated Use PlatformExecutors API.
 */
public class ThreadPool extends java.util.concurrent.ThreadPoolExecutor {
  /** Config parameter: thread pool size */
  public final static String CNF_THREAD_POOLSIZE = "system.tpool.size";
  
  /** Config parameter: thread pool queue size */
  public final static String CNF_THREAD_POOLQUEUESIZE = "system.tpool.queueSize";
  
  /** Config parameter: queue implementation name (see QueueFactory) */
  public final static String CNF_THREAD_QUEUE = "system.tpool.queue";
  
  /** Config parameter: max idle threads */
  public final static String CNF_THREAD_MAXIDLETHREADS = "system.tpool.maxIdleThreads";
  
  /** Our ThreadPool singleton. */
  private static volatile ThreadPool _singleton;
  
  /** The Service used to create concrete thread pool */
  private static volatile PlatformExecutors _pfExecs;
  
  /** The actual thread pool impl */
  private volatile PlatformExecutor _tpool;
  
  /** ThreadPool name (null means we are using default platform IO thread pool) */
  private final String _name;
  
  /** Are we using default platform thread pool ? */
  private volatile boolean _usingDefaultPlatformThreadPool;
  
  /**
   * Binds the metering service.
   * @param ms the metering service
   * @deprecated not used anymore.
   */
  public void bind(MeteringService ms) {
  }
  
  /**
   * Binds the platform executors
   */
  public static void bind(PlatformExecutors execs) {
	  _pfExecs = execs;
  }
  
  /**
   * Creates a new thread pool.
   */
  public ThreadPool() {
    super(0, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
    _tpool = getPlatformExecutors().getIOThreadPoolExecutor();
    _name = "Legacy-ThreadPool";
    _usingDefaultPlatformThreadPool = true;
  }
  
  /**
   * Creates a new thread pool
   * @param name the tpool name
   * @param size the max number of worker threads
   */
  public ThreadPool(String name, int size) {
    this(name, "as.stat.tpool." + name, size);
  }
  
  public ThreadPool(String name, String statName, int size) {
    super(0, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
    PlatformExecutors execs = getPlatformExecutors();
    _tpool = execs.createIOThreadPoolExecutor(name, statName, size);
    _name = name;
    _usingDefaultPlatformThreadPool = false;
  }
  
  public ThreadPool(String name, String statName, int size, long maxKeepAlive /* not used for now */) {
    this(name, statName, size);
  }
  
  @Deprecated
  public ThreadPool(int size, int maxIdleThreads) {
    this("Legacy-ThreadPool", size);
  }
  
  @Deprecated
  public ThreadPool(int size, int maxPendingReq, String queueKind) {
    this("Legacy-ThreadPool", size);
  }
  
  @Deprecated
  public ThreadPool(int size, int maxPendingReq, String queueKind, int maxIdleThreads) {
    this("Legacy-ThreadPool", size);
  }
  
  @Deprecated
  public ThreadPool(Config cnf) throws ConfigException {
    this("Legacy-ThreadPool", cnf.getInt(CNF_THREAD_POOLSIZE));
  }
  
  @Deprecated
  public ThreadPool(String name, int size, int maxPendingReq, String queueKind, int maxIdleThreads) {
    this(name, size);
  }
  
  @Deprecated
  public ThreadPool(String name, int size, int maxPendingReq, BlockingQueue<?> queue, int maxIdleThreads) {
    this(name, size);
  }
  
  @Deprecated
  public synchronized void setSize(int size) {
    if (size == getSize()) {
      return;
    }
    
    if (! _usingDefaultPlatformThreadPool) {
      _tpool.shutdown();
    }
    _tpool = getPlatformExecutors().createIOThreadPoolExecutor(_name, "as.stat.tpool." + _name, size);
    _usingDefaultPlatformThreadPool = false;
  }
  
  @Deprecated
  public int getSize() {
    // Since ExecutorService does not provide such method, we call it by instrospection
    Object[] params = {};
    Class<?>[] paramsClasses = {};
    try {
      Method getSizeMethod = _tpool.getClass().getMethod("getSize", paramsClasses);
      getSizeMethod.setAccessible(true);
      return (Integer) getSizeMethod.invoke(_tpool, params);
    } catch (Throwable e) {
      throw new IllegalStateException("Can't get tpool size");
    }
  }
  
  @Override
  public String toString() {
    return _tpool.toString();
  }
  
  @Deprecated
  public String getName() {
    return _name;
  }
  
  @Deprecated
  @Override
  public void execute(final Runnable task) {
    _tpool.execute(task);
  }
  
  @Deprecated
  public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
    return _tpool.schedule(task, delay, unit);
  }
  
  @Deprecated
  public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit, boolean inThreadPool) {
    Runnable r = inThreadPool ? decorate(task) : task;
    return _tpool.schedule(r, delay, unit);
  }
  
  @Deprecated
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initDelay, long period, TimeUnit unit) {
    return scheduleAtFixedRate(task, initDelay, period, unit, true);
  }
  
  @Deprecated
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initDelay, long period, TimeUnit unit,
                                                boolean inThreadPool) {
    Runnable r = inThreadPool ? decorate(task) : task;
    return _tpool.scheduleAtFixedRate(r, initDelay, period, unit);
  }
  
  @Deprecated
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initDelay, long delay, TimeUnit unit) {
    return scheduleWithFixedDelay(task, initDelay, delay, unit, true);
  }
  
  @Deprecated
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initDelay, long delay, TimeUnit unit,
                                                   boolean inThreadPool) {
    Runnable r = inThreadPool ? decorate(task) : task;
    return _tpool.scheduleWithFixedDelay(r, initDelay, delay, unit);
  }
  
  @Deprecated
  public void start(Runnable task) throws InterruptedException {
    execute(task);
  }
  
  @Deprecated
  public boolean tryStart(Runnable task) {
    execute(task);
    return true;
  }
  
  @Deprecated
  public void terminate(boolean gracefulShutdown) {
    if (!_usingDefaultPlatformThreadPool) {
      _tpool.shutdown();
    }
  }
  
  @Deprecated
  public void join(boolean flush) {
    if (!_usingDefaultPlatformThreadPool) {
      try {
        _tpool.awaitTermination(10000L, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
      }
    }
  }
  
  @Deprecated
  public int getCurrentWorkers() {
    return 0;
  }
  
  public boolean isPooled(Thread t) {
    return false;
  }
  
  public static synchronized ThreadPool getInstance() {
    if (_singleton == null) {
      _singleton = new ThreadPool();
    }
    return _singleton;
  }
  
  private static synchronized PlatformExecutors getPlatformExecutors() {
    return _pfExecs != null ? _pfExecs : ServiceLoader.getService(PlatformExecutors.class);
  }
  
  private Runnable decorate(final Runnable task) {
    return new Runnable() {
      @Override
      public void run() {
        execute(task);
      }
    };
  }
}
