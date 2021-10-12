// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.test;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Random;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.concurrent.ThreadPool;

import com.alcatel.as.service.metering.MeteringService;
import com.alcatel.as.util.serviceloader.ServiceLoader;

/**
 * This class shows how to use a thread pool associated with an object pool.
 */
public class TestThreadPool2 {
  // private attributes
  
  private static ThreadPool tp;
  private static int works;
  private static Random rnd = new Random();
  private static boolean gracefulShutdown;
  private int tasks;
  private long _duration;
  static Logger _logger = Logger.getLogger(TestThreadPool2.class);
  
  public static void main(String args[]) throws Exception {
    final ThreadPool tp = new ThreadPool("ThreadPool", 2);
    Dictionary system = new Hashtable();
    system.put("metering.delay", 3);
    MeteringService ms = (MeteringService) ServiceLoader.loadClass(MeteringService.class, null,
                                                                   new Object[] { system },
                                                                   new Class[] { Dictionary.class });
    tp.bind(ms);
    
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
    
    System.out.println("submiting");
    tp.execute(new Task(1));
    tp.execute(new Task(2));
    Thread.sleep(Integer.MAX_VALUE);
  }
  
  static class Task implements Runnable {
    int _id;
    
    Task(int id) {
      _id = id;
    }
    
    @Override
    public void run() {
      try {
        _logger.warn("Task " + _id + " running");
        Thread.sleep(5000);
        _logger.warn("Task " + _id + " done");
      } catch (Throwable t) {
      }
    }
  }
}
