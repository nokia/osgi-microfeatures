package alcatel.tess.hometop.gateways.test;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Random;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.concurrent.ThreadPool;
import alcatel.tess.hometop.gateways.utils.GetOpt;

import com.alcatel.as.service.metering.MeteringService;
import com.alcatel.as.util.serviceloader.ServiceLoader;

/**
 * This class shows how to use a thread pool associated with an object pool.
 */
public class TestThreadPool {
  // private attributes
  
  private final static Logger _logger = Logger.getLogger(TestThreadPool.class);
  private static ThreadPool tp;
  private static int works;
  private static Random rnd = new Random();
  private static boolean gracefulShutdown;
  private int tasks;
  private long _duration;
  
  private static void usage(String msg) {
    System.err.println(msg);
    System.err.println("Usage: " + TestThreadPool.class.getName()
        + "  -size <int> -gracefulShutdown -duration <ms>");
    System.exit(1);
  }
  
  public static void main(String args[]) throws Exception {
    GetOpt opt = new GetOpt(args, "size: tasks: gracefulShutdown");
    String arg = null;
    int size = -1, tasks = 0;
    boolean gracefulShutdown = false;
    long duration = 5000;
    
    if (args.length == 0)
      usage("missing arguments");
    
    while ((arg = opt.nextArg()) != null) {
      if (arg.equals("size")) {
        size = opt.nextInt();
        continue;
      }
      
      if (arg.equals("tasks")) {
        tasks = opt.nextInt();
        continue;
      }
      
      if (arg.equals("duration")) {
        duration = opt.nextLong();
        continue;
      }
      
      if (arg.equals("gracefulShutdown")) {
        gracefulShutdown = true;
        continue;
      }
      
      usage("Unknown argument: " + arg);
    }
    
    ThreadPool tp = new ThreadPool("MyThreadPool", size);
    System.out.println("ThreadPool Size=" + tp.getSize());
    Dictionary system = new Hashtable();
    system.put("metering.delay", 3);
    MeteringService ms = (MeteringService) ServiceLoader.loadClass(MeteringService.class, null,
                                                                   new Object[] { system },
                                                                   new Class[] { Dictionary.class });
    tp.bind(ms);
    TestThreadPool test = new TestThreadPool(tp, tasks, gracefulShutdown, duration);
    test.doTest();
  }
  
  private static int NWORKS = 200000;
  
  public TestThreadPool(ThreadPool tp, int tasks, boolean gracefulShutdown, long duration) throws Exception {
    this.tp = tp;
    this.works = NWORKS;
    this.tasks = tasks;
    this.gracefulShutdown = gracefulShutdown;
    _duration = duration;
  }
  
  public void doTest() throws Exception {
    Thread stat = new Thread(new Runnable() {
      public void run() {
        while (true) {
          System.out.println(tp);
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
          }
        }
      }
    }, "Stat");
    stat.start();
    
    long t1 = System.currentTimeMillis();
    for (int i = 0; i < tasks; i++) {
      // Get a worker from our pool.
      Worker w = new Worker();
      w.setId("" + i);
      tp.start(w);
    }
    long t2 = System.currentTimeMillis();
    System.out.println("ALL TASKS ARE NOW SCHEDULED (" + (t2 - t1) + " millis.)");
    
    Thread.sleep(_duration);
    System.out.println("shuting down: Gracefulshutdown=" + gracefulShutdown);
    tp.terminate(gracefulShutdown);
    System.out.println("stopped");
    Thread.sleep(Integer.MAX_VALUE);
  }
  
  /**
   * Our recyclable worker thread (must be public)
   */
  public static class Worker implements Runnable {
    public Worker() {
    }
    
    /**
     * Various worker methods (just used for demo)
     */
    public void setId(String id) {
      this.id = id;
    }
    
    /**
     * Runnable interface ...
     */
    public void run() {
      try {
        // Thread.sleep (rnd.nextInt(10));
        Thread.sleep(rnd.nextInt(2));
      }
      
      catch (InterruptedException e) {
      }
      
      finally {
        _logger.info("Worker " + id + " (" + Thread.currentThread().getName() + ") DONE");
      }
    }
    
    private String id;
    private String attrs[];
  }
}
