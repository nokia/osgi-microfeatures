package testing;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import alcatel.tess.hometop.gateways.concurrent.ThreadPool;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.impl.SerialQueue;

/**
 * Class used to see how fast we can run actors with
 * the ActorDispatcher class, with a thread pool sized to the
 * number of host processors.
 */
public class EventDispatcherBench {
  // A monothreaded actor
  static class Actor implements Runnable {
    private String _name;
    private int _counter = 0;
    private int _limit;
    private int _previousCounter;
    private CountDownLatch _latch;
    private final Executor _queue;
    
    Actor(Executor queue, String name, int limit, CountDownLatch latch) {
      _name = name;
      _limit = limit;
      _latch = latch;
      _queue = queue;
    }
    
    public Executor getQueue() {
      return _queue;
    }
    
    public String getName() {
      return _name;
    }
    
    @Override
    public void run() {
      _previousCounter = _counter;
      if (++_counter == _limit) {
        // We have reached our limit execution count.
        _latch.countDown();
      }
      if (_counter != _previousCounter + 1) {
        throw new IllegalStateException("detected concurrent access on actor " + getName() + ": counter=" + _counter
            + ", previous=" + _previousCounter);
      }
    }
  }
  
  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.out
          .println("Usage: java ActorDispatcherBench <nActors> <loop per actors> <dispatcher queue size> <thread pool size> <tpool (jdk|tpool|pfexec)>");
      System.exit(1);
    }
    int nActors = Integer.parseInt(args[0]);
    int loopPerActors = Integer.parseInt(args[1]);
    int queues = Integer.parseInt(args[2]);
    int tpoolSize = Integer.parseInt(args[3]);
    String tpool = args[4];
    
    System.out.println("actors=" + nActors + ", loop per actors=" + loopPerActors + ", queus=" + queues
        + ", tpoolsize=" + tpoolSize + ", tpool=" + tpool);
    
    if (tpoolSize == 0) {
      OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
      tpoolSize = osBean.getAvailableProcessors();
      System.out.println("tpool size used=" + tpoolSize);
    }
    Executor tp = null;
    
    if (tpool.equals("tpool")) {
      tp = new ThreadPool("MyThreadPool", "as.stat.tpool", tpoolSize);
    } else if (tpool.equals("jdk")) {
      tp = Executors.newFixedThreadPool(tpoolSize);
      ((ThreadPoolExecutor) tp).prestartCoreThread();
    } else if (tpool.equals("pfexec")) {
      ThreadPool.getInstance().setSize(tpoolSize);
      tp = PlatformExecutors.getInstance().getThreadPoolExecutor();
    } else {
      System.out.println("invalid tpool: " + tpool);
      System.exit(1);
    }
    
    long avg = 0;
    long count = 0;
    
    while (true) {
      CountDownLatch latch = new CountDownLatch(nActors);
      Actor[] actors = new Actor[nActors];
      for (int i = 0; i < nActors; i++) {
        actors[i] = new Actor(new SerialQueue(tp), "Actor-" + (i + 1), loopPerActors, latch);
      }
      
      long t1 = System.currentTimeMillis();
      for (int i = 1; i <= loopPerActors; i++) {
        for (int j = 0; j < actors.length; j++) {
          actors[j].getQueue().execute(actors[j]);
        }
      }
      latch.await();
      long t2 = System.currentTimeMillis();
      
      count++;
      avg += (t2 - t1);
      
      System.out.println("DONE: executed " + loopPerActors + " times " + nActors + " actors in " + (avg / count)
          + " millis.");
      
      if (count == 10) {
        count = 0;
        avg = 0;
        System.out.println("Resetting average");
      }
    }
  }
}
