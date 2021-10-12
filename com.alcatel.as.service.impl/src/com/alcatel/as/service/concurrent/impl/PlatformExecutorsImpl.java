// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.concurrent.impl;

// Jdk
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.Scheduler;
import com.alcatel.as.service.concurrent.ThreadContext;
import com.alcatel.as.service.concurrent.TimerService;
import com.alcatel.as.service.metering2.Monitorable;
import com.alcatel.as.util.config.ConfigHelper;
import com.alcatel.as.util.serviceloader.ServiceLoader;
import com.alcatel_lucent.as.management.annotation.config.BooleanProperty;
import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.IntProperty;
import com.alcatel_lucent.as.management.annotation.config.MonconfVisibility;
import com.alcatel_lucent.as.management.annotation.config.StringProperty;

/**
 * This class provides our PlatformExecutors OSgi service
 */
@Config(section = "General Parameters/Thread Pool", rootSnmpName = "alcatel.srd.a5350.CalloutAgent", rootOid = { 637, 71, 6, 110 }, 
  monconfModule="CalloutAgent")
public class PlatformExecutorsImpl extends PlatformExecutors {
  /** 
   * Our logger 
   */
  private final static Logger _logger = Logger.getLogger("as.service.concurrent.PlatformExecutorsImpl");
  
  /** 
   * Property name: size of the IO thread pool. 
   */
  @IntProperty(min = 0, max = 1024, title = "Size of the blocking thread pool.",
               help = "Specifies the size of the thead pool used to perform blocking (io bound) actions.",
               required = false, dynamic = true, defval = 10,
               oid = 124, snmpName = "NumberOfThreadsUsedByTheThreadPool")
  private final static String CONF_TPOOL_SIZE = "system.tpool.size";
  
  /** 
   * Property name: size of the CPU thread pool. 
   */
  @IntProperty(min = -1, max = 0x7fffffff, title = "Max idle thread timeout in seconds.",
      help = "Specifies the max number of seconds during which an idle worker thread remains alive." + 
    		  " A time value of zero will cause threads to terminate immediately after executing tasks." + 
              " A time value of -1 will cause idle worker threads to never die",
      required = false, dynamic = true, defval = 10)
  private final static String CONF_TPOOL_KEEPALIVE = "system.tpool.keepalive";

  /** 
   * Property name: size of the CPU thread pool. 
   */
  @IntProperty(min = 0, max = 1024, title = "Size of the processing thread pool.",
      help = "Specifies the size of the thead pool used to perform non blocking (cpu-bound) actions.",
      required = false, dynamic = true, defval = 0,
      oid = 136, snmpName = "NumberOfThreadsUsedByTheProcessingThreadPool")
  private final static String CONF_CPU_TPOOL_SIZE = "system.processing-tpool.size";

  /** 
   * Property name: Flag used to transport the current thread context class loader between executors. 
   */
  @BooleanProperty(
    title = "Thread Context Class Loader Management",
    help = "When set to true, this parameter ensures that the context class loader is managed by the Platform Executors Service.",
    oid = 140, snmpName = "SystemUseTCCL", required = false,
    dynamic = false, defval = true)
  private final static String CONF_USE_TCCL = "system.useTCCL";
  
  /** 
   * Property name: process CPU affinity. 
   */
  @StringProperty(title = "CPU affinity",
      help = "This parameter defines the process CPU affinity. Leave it blank to use all available cores, else specify here some numbers or ranges, separated by commas and without any white spaces. For example -> 0,5,7,9-11 (The first processor number is 0, and range limits are inclusive: 0-2 means the first, the second and the third processors will be mapped to the process).",
      defval = "", dynamic=false, required=false, monconfVisibility=MonconfVisibility.BOTH)
  private final static String CONF_CPU_AFFINITY = "system.tpool.affinity";

  @IntProperty(min = -1, max = 10000, title = "Platform Execuotors Monitoring Poll Interval",
	      help = "Configures the platform executors monitoring system. The property specifies " +
	             "the polloing interval (millis) used to periodically monitor the health of the platform thread pool. " +
	    		 "By default, -1 means no monitoring is performed. " +
	             "When enabling the monitoring, it's suggested to use a value of 100 millis.",
	      required = false, dynamic = true, defval = -1)
  public final static String CONF_MONITORING_POLL = "system.tpool.monitoring.poll";

  @IntProperty(min = 1, max = 10000, title = "Platform Execuotors Monitoring Max Schedule Time",
	      help = "Configures the platform executors monitoring system. The property specifies " +
	             "the max duration time a task should take in order to be scheduled in the platform threadpool." +
	    		 " When enabled, a stacktrace is dumped in case tasks schedule delay crosses the specified value (millis).",
	      required = false, dynamic = true, defval = 4000)
	  public final static String CONF_MONITORING_MAXSCHEDULE = "system.tpool.monitoring.maxschedule";

  @IntProperty(min = -1, max = 10000, title = "Platform Execuotors Monitoring Max Stacktraces",
	      help = "Configures the platform executors monitoring system. The property specifies " +
	             "the max number of stacktraces dumped in case the processing thread pool is detected to be blocked " +
	    		  "for a duration greater than the system.tpool.monitoring.maxschedule delay.",
	      required = false, dynamic = true, defval = 10)
  public final static String CONF_MONITORING_MAXSTACKTRACES = "system.tpool.monitoring.maxstacktraces";

  /** 
   * Patterns used to validate the syntax of the cpu affinity parameter. 
   * a cpu affinity parameter is a list of ints or ranges separated by a comma. For example: "0,1,10-12"
   */
  public final static String RANGE_OR_INT = "((\\d+(-\\d+))|(\\d+))";
  public final static Pattern AFFINITY_PATTERN = Pattern.compile("^(" + RANGE_OR_INT + "(," + RANGE_OR_INT + ")*)?$");

  /** 
   * ThreadPool internally used for managing IO-bound tasks.
   */
  private ThreadPoolBase _ioThreadPool;
  
  /** 
   * ThreadPoolExecutor used to execute IO-bound tasks 
   */
  private ThreadPoolExecutor _ioThreadPoolExecutor;
  
  /** 
   * ThreadPool internally used for managing CPU-bound tasks.
   */
  private ThreadPoolBase _cpuThreadPool;
  
  /** 
   * The ThreadPoolExecutor used to execute CPU-bound tasks 
   */
  private ThreadPoolExecutor _cpuThreadPoolExecutor;
  
  /** Our wheel timer service (50 millis accuracy). */
  private TimerService _timerService;
    
  /** Our Executors Meters. */
  private Meters _meters;

  /** 
   * Class used to manage a pool of serial queue executors running in the CPU thread pool. 
   */
  private QueueExecutorPool _cpuQueueExecutorPool;
  
  /** 
   * Class used to manage a pool of serial queue executors running in the IO thread pool. 
   */
  private QueueExecutorPool _ioQueueExecutorPool;
  
  /** 
   * Flag used to check if the current thread context classloader must be transported between executors. 
   */
  private boolean _useTCCL;
  
  /** 
   * Map of thread queue executors 
   */
  private final Map<String, ThreadQueueExecutor> _threadQueueExecutors = new HashMap<>();
  
  /**
   * Thread local used to manage per thread context informations.
   */
  private final static int DEF_PROC_TPOOL_SIZE = Integer.getInteger(CONF_CPU_TPOOL_SIZE, 3);
  
  /**
   * Enabled cores (by affinity configuration, -1 means no affinity configured).
   */
  private int _enabledCoresByAffinity;
  
  /**
   * system property which can be used to configure the processing thread pool impl.
   */
  private final static String PROCESSING_THREAD_POOL_IMPL = "system.processing-tpool.type";
  
  /**
   * Possible implementaitons for processing thread pool.
   */
  enum ProcessingThreadPool {
	  ForkJoin,
	  Simple,
	  Asr
  };
  
  /**
   * Implementation used for the processing thread pool.
   */
  private ProcessingThreadPool _processingThreadPool = ProcessingThreadPool.ForkJoin;

  /** 
   * Thread local used to manage per thread context informations.
   */
  private final ThreadLocal<ThreadContextImpl> _currentThreadContext = new ThreadLocal<ThreadContextImpl>() {
    @Override
    protected ThreadContextImpl initialValue() {
      return new ThreadContextImpl();
    }
  };
  
  public void start(Map<String, Object> conf) {
    // Inject our singleton into the Helper class, so other classes can have access to us
    Helper.bind(this);
    
    // Init IO blocking thread pool (with legacy thread pool from Utils)
    _ioThreadPool = new SimpleThreadPool("IO-ThreadPool", 10, _meters, true);
    _ioThreadPoolExecutor = new ThreadPoolExecutor(_ioThreadPool);
    
    // Get configured cpu affinity (-1 means no cpu affinity configured).
    _enabledCoresByAffinity = getCPUAffinity();
    
    // Init CPU thread pool
    
    int cpuTPoolSize = getProcessingThreadPoolSize(conf);
    cpuTPoolSize = (cpuTPoolSize == 0) ? Runtime.getRuntime().availableProcessors() : cpuTPoolSize;
    
    _processingThreadPool = ProcessingThreadPool.valueOf(System.getProperty(PROCESSING_THREAD_POOL_IMPL, ProcessingThreadPool.ForkJoin.toString()));
    
    _cpuThreadPool = createProcessingThreadPool("Processing-ThreadPool", cpuTPoolSize);
    _cpuThreadPoolExecutor = new ThreadPoolExecutor(_cpuThreadPool);

    // Update internal thread pools size
    modified(conf);
  }
  
  private ThreadPoolBase createProcessingThreadPool(String label, int size) {
	switch (_processingThreadPool) {
	case ForkJoin:
      _logger.info("Using ForkJoin Pool: size=" + size);
      return new FJPool(label, size, _meters);
      
	case Asr:		
		_logger.info("Using ASR Stealing Thread Pool: size=" + size);
		return new StealingThreadPool(label, size, _meters);
		
	case Simple:
		_logger.info("Using Simple Thread Pool: size=" + size);
		return new SimpleThreadPool(label, size, _meters, false);
		
	default:
		throw new IllegalStateException();
	}
  }
  
  /**
   * Handles configuration updates. We synchronize the method in order to just do a memory barrier.
   * @param conf the updated configuration
   */
  public synchronized void modified(Map<String, Object> conf) {    
    // Setup IO thread pool size.
    int ioTPoolSize = ConfigHelper.getInt(conf, CONF_TPOOL_SIZE, 10);
    
    // max idle worker keep alive timeout in seconds for io blocking threadpool 
    long ioTPoolKeepAlive = ConfigHelper.getLong(conf, CONF_TPOOL_KEEPALIVE, 10);

    // Modify processing thread pool size.     
    int cpuTPoolSize = getProcessingThreadPoolSize(conf);
        
    if (cpuTPoolSize < 0) {
      throw new IllegalArgumentException("Invalid size for property " + CONF_CPU_TPOOL_SIZE + ":"
          + cpuTPoolSize);
    }
    
    // Check if we must take into account thread context class loader.
    String useTCCL = (String) conf.get(CONF_USE_TCCL);
    useTCCL = (useTCCL == null) ? "true" : useTCCL;
    _useTCCL = Boolean.valueOf(useTCCL);
    
    _logger
        .info("Will use CPU thread pool size=" + cpuTPoolSize + ", and IO thread pool size=" + ioTPoolSize);
    _cpuThreadPool.setSize(cpuTPoolSize);
    _ioThreadPool.setSize(ioTPoolSize);
    _ioThreadPool.setKeepAlive(ioTPoolKeepAlive);
    
    // Init pool of queue executors
    _cpuQueueExecutorPool = new QueueExecutorPool(_cpuThreadPool, cpuTPoolSize, _meters);
    _ioQueueExecutorPool = new QueueExecutorPool(_ioThreadPool, ioTPoolSize, _meters);
  }
  
  public void bindTimerService(TimerService ts) {
    _timerService = ts;
  }
  
  void bindMeters(Monitorable meters) {
    _meters = (Meters) meters;
  }
  
  Meters getMeters() {
    return _meters;
  }
  
  @Override
  public ThreadContext getCurrentThreadContext() {
    return _currentThreadContext.get();
  }
  
  @SuppressWarnings("deprecation")
  @Override
  public PlatformExecutor getExecutor(String id) {
    if (id.equals(PlatformExecutors.CURRENT_CALLBACK_EXECUTOR)) {
      return getCurrentThreadContext().getCallbackExecutor();
    } else if (id.equals(PlatformExecutors.CURRENT_EXECUTOR)) {
      return getCurrentThreadContext().getCurrentExecutor();
    } else if (id.equals(PlatformExecutors.CURRENT_PREFERRED_CALLBACK_EXECUTOR)) {
      return getCurrentThreadContext().getPreferredCallbackExecutor();
    } else if (id.equals(PlatformExecutors.CURRENT_ROOT_EXECUTOR)) {
      return getCurrentThreadContext().getRootExecutor();
    } else if (id.equals(PlatformExecutors.THREAD_POOL_EXECUTOR)) {
      return _ioThreadPoolExecutor;
    } else if (id.startsWith(PlatformExecutors.THREAD_POOL_EXECUTOR + ".")) {
      int dot = id.indexOf(".");
      if (dot == -1) {
        throw new IllegalArgumentException("thread pool id must ends with a worker id");
      }
      return getThreadPoolExecutor(id.substring(dot + 1));
    } else {
      PlatformExecutor exec = _threadQueueExecutors.get(id.toLowerCase());
      if (exec != null) {
        return exec;
      }
      // lookup from osgi registry ?
      return ServiceLoader.getService(PlatformExecutor.class, "(id~=" + id + ")");
    }
  }
  
  public synchronized PlatformExecutor createThreadQueueExecutor(String id, ThreadFactory factory) {
    ThreadQueueExecutor e = new ThreadQueueExecutor(id, factory);
    _threadQueueExecutors.put(id.toLowerCase(), e);
    return e;
  }
  
  @Override
  public PlatformExecutor getThreadPoolExecutor() {
    return _ioThreadPoolExecutor;
  }
  
  @Override
  public PlatformExecutor getIOThreadPoolExecutor() {
    return _ioThreadPoolExecutor;
  }
  
  @Override
  public PlatformExecutor getProcessingThreadPoolExecutor() {
    return _cpuThreadPoolExecutor;
  }
  
  @Override
  public PlatformExecutor createQueueExecutor(PlatformExecutor exec) {
    return createQueueExecutor(exec, null);
  }
  
  @Override
  public PlatformExecutor createQueueExecutor(PlatformExecutor exec, String id) {
    if (exec instanceof PlatformExecutor) {
      PlatformExecutor pe = (PlatformExecutor) exec;
      if (!pe.isThreadPoolExecutor()) {
        throw new IllegalArgumentException(
            "the executor passed to the createQueueExecutor is not a thread pool executor");
      }
      return new QueueExecutor(((ThreadPoolExecutor) exec).getThreadPool(), id, _meters);
    }
    return new QueueExecutor(exec, id, _meters);
  }
  
  @Override
  public PlatformExecutor getThreadPoolExecutor(Object queue) {
    return getIOThreadPoolExecutor(queue);
  }
  
  @Override
  public PlatformExecutor getIOThreadPoolExecutor(Object queue) {
    return _ioQueueExecutorPool.getQueueExecutorFor(queue);
  }
  
  @Override
  public PlatformExecutor getProcessingThreadPoolExecutor(Object queue) {
    return _cpuQueueExecutorPool.getQueueExecutorFor(queue);
  }
  

  @Override
  public Scheduler createScheduler() {
    return new SchedulerImpl();
  }
  
  @Override
  public PlatformExecutor createIOThreadPoolExecutor(String label, String stat, int size) {
    return new ThreadPoolExecutor(new SimpleThreadPool(label, size, _meters, true));
  }
  
  @Override
  public PlatformExecutor createProcessingThreadPoolExecutor(String label, String stat, int size) {
    return new ThreadPoolExecutor(createProcessingThreadPool(label, size));
  }

  ThreadPoolBase getIOThreadPool() {
    return _ioThreadPool;
  }
  
  ThreadPoolBase getCPUThreadPool() {
    return _cpuThreadPool;
  }
  
  TimerService getTimerService() {
    return _timerService;
  }
    
  boolean useTCCL() {
    return _useTCCL;
  }
  
  synchronized PlatformExecutor getThreadQueueExecutor(String id) {
    return _threadQueueExecutors.get(id.toLowerCase());
  }
  
  synchronized void removeThreadQueueExecutor(String id) {
    _threadQueueExecutors.remove(id.toLowerCase());
  }
  
  private int getProcessingThreadPoolSize(Map<String, Object> conf) {
    int cpuTPoolSize = ConfigHelper.getInt(conf, CONF_CPU_TPOOL_SIZE, -1);
    switch (cpuTPoolSize) {
      case -1:
        // not yet configured.
        if (_enabledCoresByAffinity != -1) {
          cpuTPoolSize = Math.min(DEF_PROC_TPOOL_SIZE, _enabledCoresByAffinity);
        } else {
          cpuTPoolSize = DEF_PROC_TPOOL_SIZE;
        }
        break;
        
      case 0:
        if (_enabledCoresByAffinity != -1) {
          cpuTPoolSize = Math.min(Runtime.getRuntime().availableProcessors(), _enabledCoresByAffinity);
        } else {
          cpuTPoolSize = Runtime.getRuntime().availableProcessors();
        }
        break;
        
      default:
        break;
    }
    return cpuTPoolSize;
  }

  // Return configured CPU affinity (0 means no affinity configured)
  private int getCPUAffinity() {
    // Get cpu affinity from system property, or from configuration.
    String affinity = System.getProperty(CONF_CPU_AFFINITY, "");
    affinity = affinity.trim();
    affinity = affinity.replace(" ", "");
    
    if (affinity.equals("")) {
      return -1;
    }    
    
    // First, verify correct syntax (list of number, separated by comma, or dash).       
    Matcher m = AFFINITY_PATTERN.matcher(affinity);
    if (! m.matches()) {
      _logger.error("Invalid parameter value for property " + CONF_CPU_AFFINITY + ":" + affinity);
      return -1;
    }
    
    // Compute number of enabled cores.
    int enabledCores = parseEnabledCores(affinity);
    if (enabledCores == -1) {
      _logger.error("Invalid parameter value for property " + CONF_CPU_AFFINITY + ":" + affinity);
      return -1;
    }
    
    _logger.warn("CPU affinity configuration: " + affinity + " (enabled cores=" + enabledCores + ")");
    // Return the number of enabled cores.
    return enabledCores;
  }
  
  /**
   * Parse the number of enabled cores
   * @param affinity the list of cores, or range of cores. A core number always starts with 0 (0 is the first core).
   *        Example: "0,2-3" -> the first, third and fourth cores are enabled and the second core is disabled, so the total number
   *        of enabled core is 3 (core 0, 2 and 3). 
   * @return number of enabled cores
   */
  private int parseEnabledCores(String affinity) {
    Set<Integer> enabledProcs = new HashSet<>();
    
    String[] tokens = affinity.split(",");
    for (String token : tokens) {
      token = token.trim();
      if (token.length() == 0) {
        continue;
      }
      if (token.indexOf("-") != -1) {
        // range: something like 1-3
        String[] ranges = token.split("-");
        int first = Integer.parseInt(ranges[0]);
        int last = Integer.parseInt(ranges[1]);
        if (first > last) {
          return -1; // invalid configuration
        }
        for (int i = first; i <= last; i ++) {
          enabledProcs.add(Integer.valueOf(i));
        }
      } else {
        // proc number: add it to list of enabled procs
        enabledProcs.add(Integer.valueOf(token));
      }           
    }
    
    return enabledProcs.size();
  }

  private boolean runCommand(String cmd) {
    try {
      _logger.warn("Setting process CPU affinity with command: " + cmd);
      Process p = Runtime.getRuntime().exec(cmd, null);
      StringBuilder sb = new StringBuilder();
      InputStream in = p.getInputStream();
      int c;
      boolean println = false;
      while ((c = in.read()) != -1) {
        sb.append((char) c);
        println = true;
      }
      if (println) {
        _logger.info(sb.toString());
      }
      in = p.getErrorStream();
      println = false;
      sb = new StringBuilder();

      while ((c = in.read()) != -1) {
        sb.append((char) c);
      }
      if (println) {
        _logger.error(sb.toString());
      }
      int erc = p.waitFor();
      if (erc == 0) {
        _logger.info("Taskset command successfully performed: " + cmd);
        return true;
      } else {
        _logger.error("Taskset command: " + cmd + " failed with status: " + erc);
        return false;
      }
    }
    
    catch (Throwable t) {
      _logger.error("Taskset command failed");
      return false;
    }
  }

  private int getPID() {
    try {
      java.lang.management.RuntimeMXBean runtime = java.lang.management.ManagementFactory.getRuntimeMXBean();
      java.lang.reflect.Field jvm = runtime.getClass().getDeclaredField("jvm");
      jvm.setAccessible(true);
      sun.management.VMManagement mgmt = (sun.management.VMManagement) jvm.get(runtime);
      java.lang.reflect.Method pid_method = mgmt.getClass().getDeclaredMethod("getProcessId");
      pid_method.setAccessible(true);
      int pid = (Integer) pid_method.invoke(mgmt);
      return pid;
    } catch (Throwable t) {
      t.printStackTrace();
      return -1;
    }
  }
}
