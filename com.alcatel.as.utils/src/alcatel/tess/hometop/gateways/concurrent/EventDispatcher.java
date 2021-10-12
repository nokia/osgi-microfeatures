// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.concurrent;

// Utils
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class EventDispatcher {
  private final Executor _executor;
  private Map<String, Queue> queues = new HashMap<String, EventDispatcher.Queue>();
  
  public EventDispatcher() {
    _executor = null; // we'll use ThreadPool.getInstance() systematically
  }
  
  public EventDispatcher(Executor executor) {
    _executor = executor;
  }
  
  /**
   * Notifies of a new queue user.
   * @return the number of users for that queue, Integer.MAX_VALUE if queue is null.
   */
  public int addUser(String queue) {
    if (queue == null)
      return Integer.MAX_VALUE;
    synchronized (queues) {
      Queue q = queues.get(queue);
      if (q == null) {
        queues.put(queue, new Queue(queue));
        return 1;
      }
      return ++q.count;
    }
  }
  
  /**
   * Notifies that a queue user has gone.
   * <br/>All the pending tasks are run.
   * <br/>The underlying queue is removed if no more user are using the queue.
   * @return the number of users left for that queue, or -1 if no user was using the queue.
   */
  public int removeUser(String queue) {
    if (queue == null)
      return Integer.MAX_VALUE;
    synchronized (queues) {
      Queue q = queues.get(queue);
      if (q == null)
        return -1;
      if ((--q.count) > 0) {
        return q.count;
      } else {
        queues.remove(queue);
        return 0;
      }
    }
  }
  
  /**
   * Notifies that all the users of a queue are gone.
   * <br/>All the pending tasks are run.
   * @return the number of users that were removed, 0 if no user was using the queue.
   */
  public int removeUsers(String queue) {
    if (queue == null)
      return Integer.MAX_VALUE;
    synchronized (queues) {
      Queue q = queues.remove(queue);
      if (q == null)
        return 0;
      return q.count;
    }
  }
  
  /**
   * Returns the number of users for a queue (possibly 0).
   */
  public int getUsers(String queue) {
    if (queue == null)
      return Integer.MAX_VALUE;
    synchronized (queues) {
      Queue q = queues.get(queue);
      if (q == null)
        return 0;
      return q.count;
    }
  }
  
  /**
   * Runs a task on a specific queue.
   * The queue may be null (means no specific queue needed).
   * There must be at least 1 user for the queue (if not null), otherwise an IllegalStateException is thrown.
   */
  public void dispatch(String queue, Runnable task) throws InterruptedException {
    if (queue == null) {
      // no specific queue
      getExecutor().execute(task);
    } else {
      Queue q = null;
      synchronized (queues) {
        q = queues.get(queue);
      }
      if (q == null)
        throw new IllegalStateException("No user for queue : " + queue);
      q.newTask(task);
    }
  }
  
  /**
   * Called from an executing task to retrieve the current Thread queue.
   */
  public static String getQueue() {
    Queue q = queueThreadLocal.get();
    if (q == null)
      // no queue specified
      return null;
    return q.name;
  }
  
  private Executor getExecutor() {
    return _executor == null ? ThreadPool.getInstance() : _executor;
  }
  
  private class Queue implements Runnable {
    
    private String name;
    private int count = 1;
    private List<Runnable> tasks = new LinkedList<Runnable>();
    private boolean running;
    private Runnable firstTask;
    
    private Queue(String name) {
      this.name = name;
    }
    
    private void newTask(Runnable task) throws InterruptedException {
      synchronized (tasks) {
        if (running) {
          tasks.add(task);
          return;
        } else {
          firstTask = task;
          running = true;
        }
      }
      try {
        getExecutor().execute(this);
      } catch (RejectedExecutionException e) {
        // should never happen
        e.printStackTrace();
        synchronized (tasks) {
          // clean
          running = false;
          firstTask = null;
          tasks.clear();
        }
      }
    }
    
    private Runnable nextTask() {
      synchronized (tasks) {
        if (tasks.size() == 0) {
          running = false;
          return null;
        } else
          return tasks.remove(0);
      }
    }
    
    public void run() {
      queueThreadLocal.set(this);
      
      try {
        Runnable task = firstTask;
        boolean firstRun = true;
        do {
          try {
            task.run();
          } catch (Throwable t) {
            t.printStackTrace();
          }
          if (firstRun) {
            firstTask = null;
            firstRun = false;
          }
        } while ((task = nextTask()) != null);
      } finally {
        queueThreadLocal.set(null);
      }
    }
  }
  
  private static ThreadLocal<Queue> queueThreadLocal = new ThreadLocal<Queue>() {
    protected Queue initialValue() {
      return null;
    }
  };
  
  /**
   * Test
   * java -classpath lib/utils.jar  alcatel/tess/hometop/gateways/concurrent/EventDispatcher <numberofqueues> <numberofevents>
   */
  
  public static void main(String[] args) throws Exception {
    final java.util.concurrent.ThreadPoolExecutor tp = new ThreadPoolExecutor(100, 100, 10, TimeUnit.SECONDS,
        new LinkedBlockingQueue<Runnable>());
    
    final EventDispatcher dispatcher = new EventDispatcher(tp);
    
    dispatcher.addUser("User");
    for (int i = 0; i < 1000; i++) {
      final int I = i;
      dispatcher.dispatch("User", new Runnable() {
        public void run() {
          try {
            System.out.println("Thread[" + Thread.currentThread().getName() + "]: task=" + I);
          }
          
          catch (RejectedExecutionException e) {
            e.printStackTrace();
          }
        }
      });
    }
    dispatcher.removeUser("User");
  }
}
