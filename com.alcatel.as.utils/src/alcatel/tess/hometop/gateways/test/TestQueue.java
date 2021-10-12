// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.test;

import alcatel.tess.hometop.gateways.concurrent.QueueFactory;
import alcatel.tess.hometop.gateways.concurrent.QueueIF;
import alcatel.tess.hometop.gateways.utils.GetOpt;
import alcatel.tess.hometop.gateways.utils.Utils;

/**
 * The class <code>TestQueue</code> show how to use the Fifo Queues
 * which may be found under the "concurrent" package.
 *
 * This test class lauchs mutliple threads. A set of threads just write
 * messages into the queue, and another one read these messages off the
 * queue. At the end of the test, some statistics are displayed.
 */
public class TestQueue {
  public static void main(String args[]) throws Exception {
    GetOpt opt = new GetOpt(args, "-c: -nprod: -ncons: -t: -q:");
    int capacity = 0, nprod = 0, ncons = 0, timeSec = 0;
    String arg = null, queue = null;
    
    if (args.length == 0) {
      usage("Invalid arguments");
    }
    
    while ((arg = opt.nextArg()) != null) {
      if (arg.equals("c")) {
        capacity = opt.nextInt();
        continue;
      }
      
      if (arg.equals("nprod")) {
        nprod = opt.nextInt();
        continue;
      }
      
      if (arg.equals("ncons")) {
        ncons = opt.nextInt();
        continue;
      }
      
      if (arg.equals("t")) {
        timeSec = opt.nextInt();
        continue;
      }
      
      if (arg.equals("q")) {
        queue = opt.nextString();
        continue;
      }
      
      usage("Unknown arg: " + arg + "=" + opt.nextString());
    }
    
    new TestQueue(capacity, nprod, ncons, timeSec, queue);
  }
  
  private static void usage(String msg) {
    System.err.println(msg);
    
    msg = "java TestQueue -c <int> -nprod <int> -ncons <int> -t <seconds> -q <string>" + LINE_SEPARATOR
        + "-c: Queue capacity (1024 is a good choice)" + LINE_SEPARATOR
        + "-nprod: Number of threads that write messages into the queue" + LINE_SEPARATOR
        + "-ncons: Number of threads that read messages off the queue" + LINE_SEPARATOR
        + "-t: The time in seconds after which the test stops" + LINE_SEPARATOR
        + "-q: the name of the queue that will be used" + LINE_SEPARATOR + "\t(\"buffer\"," + "\"linked\","
        + "\"buffer_delegates\"," + "\"bounded_linked\")" + LINE_SEPARATOR;
    
    System.err.println(msg);
    System.exit(1);
  }
  
  TestQueue(int capacity, int nProd, int nCons, int timeSec, String queue) throws InterruptedException {
    this.queue = QueueFactory.create(queue, capacity);
    
    Producer[] prods = new Producer[nProd];
    Consumer[] cons = new Consumer[nCons];
    
    System.out.println("creating threads ...");
    
    for (int i = 0; i < nProd; i++) {
      prods[i] = new Producer();
    }
    
    for (int i = 0; i < nCons; i++) {
      cons[i] = new Consumer();
    }
    
    System.out.println("starting threads ...");
    
    for (int i = 0; i < nProd; i++) {
      prods[i].launch();
    }
    
    for (int i = 0; i < nCons; i++) {
      cons[i].launch();
    }
    
    System.out.println("threads started ! (all averages will be given in millis)");
    
    timeSec *= 1000;
    long begin = System.currentTimeMillis();
    
    synchronized (this) {
      while (timeSec > 0) {
        wait(1000);
        timeSec -= 1000;
        
        long duration = System.currentTimeMillis() - begin;
        displayStats(prods, cons, duration);
      }
    }
    
    for (int i = 0; i < nProd; i++) {
      prods[i].interrupt();
    }
    
    for (int i = 0; i < nCons; i++) {
      cons[i].interrupt();
    }
    
    for (int i = 0; i < nProd; i++) {
      prods[i].join();
    }
    
    for (int i = 0; i < nCons; i++) {
      cons[i].join();
    }
    
    long duration = System.currentTimeMillis() - begin;
    displayStats(prods, cons, duration);
  }
  
  private void displayStats(Producer prods[], Consumer cons[], long duration) {
    int nProduced = 0;
    for (int i = 0; i < prods.length; i++) {
      nProduced += prods[i].getProduced();
    }
    
    int nConsumed = 0;
    for (int i = 0; i < cons.length; i++) {
      nConsumed += cons[i].getConsumed();
    }
    
    long nProducedAvgMs = nProduced / duration;
    long nConsumedAvgMs = nConsumed / duration;
    
    System.out.println("produced=" + nProduced + ", consumed=" + nConsumed + ", qsize=" + queue.size()
        + ", avg produced/millis=" + nProducedAvgMs + ", avg consumed/millis=" + nConsumedAvgMs);
  }
  
  private class Consumer extends Thread {
    
    Consumer() {
      setPriority(Thread.NORM_PRIORITY - 1);
      start();
    }
    
    synchronized void launch() {
      started = true;
      this.notify();
    }
    
    public void run() {
      synchronized (this) {
        try {
          while (!started)
            this.wait();
        }
        
        catch (InterruptedException e) {
          return;
        }
      }
      
      Object[] in = new Object[10];
      
      for (;;) {
        try {
          queue.take();
          nConsumed++;
        }
        
        catch (InterruptedException t) {
          break;
        }
      }
    }
    
    synchronized int getConsumed() {
      return (this.nConsumed);
    }
    
    private volatile int nConsumed = 0;
    private boolean started = false;
  }
  
  private class Producer extends Thread {
    
    Producer() {
      start();
    }
    
    synchronized void launch() {
      started = true;
      this.notify();
    }
    
    public void run() {
      synchronized (this) {
        try {
          while (!started) {
            this.wait();
          }
        }
        
        catch (InterruptedException e) {
          return;
        }
      }
      
      for (;; nProduced++) {
        try {
          queue.put(new Integer(nProduced));
        }
        
        catch (Exception e) {
          break;
        }
      }
    }
    
    synchronized int getProduced() {
      return (this.nProduced);
    }
    
    private volatile int nProduced = 0;
    private boolean started = false;
  }
  
  private QueueIF queue;
  private final static String LINE_SEPARATOR = Utils.LINE_SEPARATOR;
}
