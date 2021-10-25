// // Copyright 2000-2021 Nokia
// //
// // Licensed under the Apache License 2.0
// // SPDX-License-Identifier: Apache-2.0
// //
//
//

package com.nokia.as.diameter.tools.loader;

import static java.lang.System.err;
import static java.lang.System.out;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import org.apache.log4j.Logger;

import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.sun.nio.sctp.AbstractNotificationHandler;
import com.sun.nio.sctp.SctpChannel;

public abstract class DiameterLoader extends AbstractNotificationHandler<SctpChannel> implements TPSListener, Runnable {
  static Logger _logger = Logger.getLogger(DiameterLoader.class);
  static DiameterMessageParser _parser = DiameterMessageParser.getInstance();
  static Selector _selector;
  static String _to;
  static int _port;
  static String _from;
  static ForkJoinPool _tpool;
  static final ScheduledExecutorService _timer = Executors.newScheduledThreadPool(1);
  static int _bodySize;
  final static AtomicInteger _requestCounter = new AtomicInteger();
  static int _fillbackSize;
  static byte[] _acrUserName;
  static AtomicInteger _ok = new AtomicInteger();
  static AtomicInteger _okToTal = new AtomicInteger();
  static AtomicInteger _3004 = new AtomicInteger();
  static AtomicInteger _3004Total = new AtomicInteger();
  static AtomicInteger _sent = new AtomicInteger();
  static AtomicInteger _connected = new AtomicInteger();
  static AtomicLong _requestsLatency = new AtomicLong();
  static AtomicLong _requestsLatencyTotal = new AtomicLong();
  static int _bulkSize;
  static int _peers = -1;
  static boolean _sctp;
  static String _sctpSecondary;
  static boolean _fragment;
  static long _sctpSecondarySchedule;
  static TPSManager _tpsManager;
  static Stat _stat = new Stat();
  static boolean _calculateLatency;
  static volatile boolean _shutdown;
  static int _readTimeoutSec = 5; 
  
  static int _duration;
  static volatile int _tps = -1;
  
  final ConcurrentLinkedQueue<Runnable> _tasks = new ConcurrentLinkedQueue<Runnable>();
  Thread _selectorThread;
  final AtomicInteger _ceaReceived = new AtomicInteger();
  final AtomicInteger _dpaReceived = new AtomicInteger();
  
  long _lastStatTime;
  volatile int _tpsPerConnections = -1;
  ConcurrentHashMap<Integer, Long> _requestsSendDate = new ConcurrentHashMap<Integer, Long>();
  final AtomicInteger _wakeupCounter = new AtomicInteger();
  volatile int _selectCounter;
  volatile boolean _selecting;
  
  static class Stat implements Runnable {
    int _index = 1;
    long _maxOK = Integer.MIN_VALUE;
    int _readTimeoutCheck;
    int _received = 0;
    int _time = 0;
    
    @Override
    public void run() {
      while (true) {
        try {
          Thread.sleep(1000);
          stat();
        } catch (InterruptedException e) {
        }
      }
    }
    
    public void stat() {
      long latency = 0;
      long latencyTotal = 0;
      long ok = 0;
      long ko = 0;
      long received = _ok.get() + _3004.get();
      
      if (_readTimeoutSec != 0) {
        _received += received;
        if ((++_readTimeoutCheck) >= _readTimeoutSec) {
          _logger.info("checking read timeout: _readTimeoutCheck=" + _readTimeoutCheck + ", received=" + _received);
          if (_received == 0) {
            // did not receive anything since last stat: diamlb probably
            // died
            _logger.error("did not receive response since 1 second. exiting ...");
            System.exit(2);
          }
          _readTimeoutCheck = 0;
          _received = 0;
        }
      }
      
      if (_calculateLatency) {
        latency = _requestsLatency.getAndSet(0) / ((received == 0) ? 1 : received);
        long receivedTotal = _okToTal.get() + _3004Total.get();
        latencyTotal = _requestsLatencyTotal.get() / ((receivedTotal == 0) ? 1 : receivedTotal);
        _logger.warn((++_time) + ": Cnx=" + _connected.get() + "; Sent=" + _sent.getAndSet(0) + "; OK="
            + (ok = _ok.getAndSet(0)) + "; 3004=" + (ko = _3004.getAndSet(0)) + "; Latency=" + latency
            + "; Total=" + _okToTal.get() + "; Total-3004=" + _3004Total.get() + "; Total-Latency="
            + latencyTotal);
      } else {
        _logger.warn((++_time) + ": Cnx=" + _connected.get() + "; Sent=" + _sent.getAndSet(0) + "; OK="
            + (ok = _ok.getAndSet(0)) + "; 3004=" + (ko = _3004.getAndSet(0)) + "; Total=" + _okToTal.get()
            + "; Total-3004=" + _3004Total.get());
      }
      ++_index;
      
      if (_duration != -1) {
        if (_tps != -1) {
          _maxOK = Math.max(_maxOK, ok);
        }
        if (_index > _duration) {
          if (_tps != -1) {
            if (_maxOK < _tps) {
              _logger.warn("Test failed: could not reach expected tps: (max=" + _maxOK + ")");
              System.exit(1);
            }
          }
          
          if (!_shutdown) {
            _logger.warn("Test done: About to send DPR on every peer connections.");
            _shutdown = true;
          }
        }
      }
    }
  }
  
  private static void usage(boolean errorCondition, String errMsg) {
    if (errorCondition) {
      err.println(errMsg);
      out.println("Usage: java "
          + DiameterLoader.class.getSimpleName()
          + " -from <ip> -to <ip> -port <number> -peers <num_of_socks> [-duration <number> -tps <range of TPS> -bulksize <size> -tpool <size> -acrUserNameSize <size> -fillbackSize <number> -latency -sctp -fragment]");
      
      out.println();
      out.println("\t-from local address to bind the diameter loader to.");
      out.println("\t-to remote diameter proxy/server address.");
      out.println("\t-port remote diameter proxy/server port number.");
      out.println("\t-peers number of peer connections to open.");
      out.println("\t-tps a tps range. Format is: \"tps1/duration_in_secondsS tps2/duration_in_secondsS\" ...");
      out.println("\t-sctpSecondary a secondary address to use when connecting in sctp.");
      out.println("\t-sctpSecondarySchedule The delay in millis before adding the sctp secondary local addr.");
      out.println("\t-bulksize Size of send buffer used to send a bulk of diameter requests.");
      out.println("\t-tpool tpoolSize (0: use number of processors)");
      out.println("\t-latency calculate response latency average");
      out.println("\t-acrUserNameSize size of request UserName AVP to be included in request.");
      out.println("\t-fillbackSize size of specific fillback AVP to be included in response.");
      out.println("\t-duration test duration in seconds. The loader will exit after the specified duration.");
      out.println("\t-readTimeout read timeot in seconds (1 by default).");
      out.println("\t-fragment send each message with fragmentation (false by default).");

      out.println();
      out.println("Examples:");
      out.println("\trun.sh -from 169.254.198.6 -to 169.254.198.6 -port 3868 -peers 10");
      out.println("\trun.sh -from 169.254.198.6 -to 169.254.198.6 -port 3868 -peers 10 -sctp");
      out.println("\trun.sh -from 169.254.198.6 -to 169.254.198.6 -port 3868 -peers 10 -tpool 3 -latency");
      out.println("\trun.sh -from 169.254.198.6 -to 169.254.198.6 -port 3868 -peers 10 -tps \"10000/5s 50000/20s\"");
      
      System.exit(1);
    }
  }
  
  static void parseArgs(String argv[]) {
    int index = 0;
    _peers = -1;
    int acrUserNameSize = 20;
    _bulkSize = 64 * 1024;
    _fillbackSize = -1;
    int tpoolSize = 0;
    String tps = null;
    _duration = -1;
    _port = -1;
    _readTimeoutSec = 5;
    
    while (index < argv.length) {
      String arg = argv[index++];
      if (arg.equals("-sctp")) {
        _sctp = true;
      } else if (arg.equals("-fragment")) {
        _fragment = true;
      } else if (arg.equals("-sctpSecondary")) {
        _sctpSecondary = argv[index++];
      } else if (arg.equals("-sctpSecondarySchedule")) {
          _sctpSecondarySchedule = Long.parseLong(argv[index++]);
      } else if (arg.equals("-tpool")) {
        tpoolSize = Integer.valueOf(argv[index++]);
      } else if (arg.equals("-from")) {
        _from = argv[index++];
      } else if (arg.equals("-to")) {
        _to = argv[index++];
      } else if (arg.equals("-port")) {
        _port = Integer.parseInt(argv[index++]);
      } else if (arg.equals("-peers")) {
        _peers = Integer.parseInt(argv[index++]);
      } else if (arg.equals("-bulksize")) {
        _bulkSize = Integer.parseInt(argv[index++]);
      } else if (arg.equals("-acrUserNameSize")) {
        acrUserNameSize = Integer.parseInt(argv[index++]);
      } else if (arg.equals("-fillbackSize")) {
        _fillbackSize = Integer.parseInt(argv[index++]);
      } else if (arg.equals("-tps")) {
        tps = argv[index++];
        _tpsManager = new TPSManager(tps, _timer);
      } else if (arg.equals("-latency")) {
        _calculateLatency = true;
      } else if (arg.equals("-duration")) {
        _duration = Integer.parseInt(argv[index++]);
      } else if (arg.equals("-readTimeout")) {
        _readTimeoutSec = Integer.parseInt(argv[index++]);
      } else {
        usage(true, "Wrong argument: " + arg);
      }
    }
    
    usage(_readTimeoutSec < 0, "readTimeout option must not be negative");
    usage(_to == null, "missing -to argument");
    usage(_port == -1, "missing -port argument");
    usage(_peers == -1, "missing -peers argument");
    
    tpoolSize = (tpoolSize == 0) ? Runtime.getRuntime().availableProcessors() : tpoolSize;
    _tpool = new ForkJoinPool(tpoolSize + 1 /* for the selector */);
    
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < acrUserNameSize; i++) {
	//sb.append("X");
    }
    //sb.append ("7Cu6uT8FQuLBlfJ4k8puVca");
    //sb.append ("7+u6uT8FQuLBlfJ4k8puVca");
    sb.append ("2CtSWT1lI0iU+vEGAao7yZD@nokia.com");
    _acrUserName = UTF8StringFormat.toUtf8String(sb.toString());
    
    out.println("Will use the following parameters:\n");
    out.println("\tprotocol=" + ((_sctp) ? "SCTP" : "TCP"));
    out.println("\tfrom=" + _from);
    out.println("\tto=" + _to);
    out.println("\tport=" + _port);
    out.println("\tpeers=" + _peers);
    out.println("\tbulksize=" + _bulkSize);
    out.println("\tacrUserNameSize=" + acrUserNameSize);
    out.println("\tfillbackSize=" + ((_fillbackSize != -1) ? _fillbackSize : "0"));
    out.println("\tthreadPool=" + tpoolSize);
    out.println("\ttps=" + ((tps == null) ? "MAX" : tps));
    out.println("\tduration=" + ((_duration == -1) ? "unlimitted" : _duration));
    out.println("\treadTimeout=" + _readTimeoutSec);
    out.println();
  }
  
  public static void main(String argv[]) throws Exception {
    DiameterLoader.parseArgs(argv);
    DiameterLoader loader = _sctp ? new SctpDiameterLoader() : new TcpDiameterLoader();
    _tpool.execute(loader);
    _stat.run();
  }
  
  public void setTPS(final int tps) {
    _tps = tps;
    schedule(new Runnable() {
      @Override
      public void run() {
        _logger.warn("Scheduling TPS=" + tps);
        _tpsPerConnections = tps < _peers ? 1 : tps / _peers;
        for (SelectionKey key : _selector.keys()) {
          startTpsTimer(key);
        }
      }
    });
  }
  
  void scheduleWriteInterest(final SelectionKey sk) {
    if (Thread.currentThread() == _selectorThread) {
      sk.interestOps(sk.interestOps() | SelectionKey.OP_WRITE);
      return;
    }
    schedule(new Runnable() {
      @Override
      public void run() {
        sk.interestOps(sk.interestOps() | SelectionKey.OP_WRITE);
      }
    });
  }
  
  void scheduleReadInterest(final SelectionKey sk) {
    if (Thread.currentThread() == _selectorThread) {
      sk.interestOps(sk.interestOps() | SelectionKey.OP_READ);
      return;
    }
    
    schedule(new Runnable() {
      @Override
      public void run() {
        sk.interestOps(sk.interestOps() | SelectionKey.OP_READ);
      }
    });
  }
  
  void runScheduledTasks() {
    ConcurrentLinkedQueue<Runnable> tasks = _tasks;
    Runnable task;
    while ((task = tasks.poll()) != null) {
      try {
        task.run();
      }
      
      catch (Throwable t) {
        _logger.error("Caught unexpected exception while running scheduled reactor task", t);
      }
    }
  }
  
  // called by child classes when one peer has received its initial cea.
  void ceaReceived(SelectionKey sk) {
    if (_ceaReceived.incrementAndGet() == _peers) {
      _logger.warn("All CEA received: loading ...");
      schedule(new Runnable() {
        @Override
        public void run() {
          if (_tpsManager != null) {
            setTPS(_tpsManager.start(DiameterLoader.this));
          } else {
            for (SelectionKey key : _selector.keys()) {
              key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            }
          }
        }
      });
    }
  }
  
  void dpaReceived(SelectionKey sk) {
    _logger.warn("Received DPA (" + (_dpaReceived.get() + 1) + ")");
    if (_dpaReceived.incrementAndGet() == _peers) {
      _logger.warn("All DPA received: stopping test ...");
      System.exit(0);
    }
  }
  
  void sending(int hopID) {
    _sent.incrementAndGet();
    if (_calculateLatency) {
      if (_requestsSendDate.put(hopID, new Long(System.nanoTime())) != null) {
        _logger.error("sending message with duplicate hopByHop id.");
        System.exit(1);
      }
    }
  }
  
  void received(ByteBuffer response, boolean OK) {
    if (OK) {
      _ok.incrementAndGet();
      _okToTal.incrementAndGet();
    } else {
      _3004.incrementAndGet();
      _3004Total.incrementAndGet();
    }
    
    if (_calculateLatency) {
      int hopID = DiameterUtils.getHopId(response);
      Long requestSendDate = _requestsSendDate.remove(hopID);
      if (requestSendDate != null) {
        long elapsed = System.nanoTime() - requestSendDate;
        // _logger.warn("elapsed=" + elapsed);
        _requestsLatency.addAndGet(elapsed);
        _requestsLatencyTotal.addAndGet(elapsed);
      } else {
        _logger.error("Got response with unknown hop id: " + Integer.toHexString(hopID));
        System.exit(1);
      }
    }
  }
  
  abstract void connect(InetSocketAddress from, InetSocketAddress to) throws IOException;
  
  abstract void connected(SelectionKey sk) throws IOException;
  
  abstract void readReady(final SelectionKey sk) throws Exception;
  
  abstract void writeReady(final SelectionKey sk) throws Exception;
  
  abstract void startTpsTimer(SelectionKey key);
  
  void schedule(final Runnable task) {
    _tasks.offer(task);
    if (_wakeupCounter.getAndIncrement() == _selectCounter && _selecting) {
      _selector.wakeup();
    }
  }
  
  private int select() throws IOException {
    int selected = 0;
    _selecting = true;
    try {
      if (_selectCounter == _wakeupCounter.get()) {
        selected = _selector.select();
      }
    } finally {
      _selecting = false;
      _selectCounter = _wakeupCounter.get();
    }
    return selected;
  }
  
  public void run() {
    System.out.println("Starting selector thread (" + Thread.currentThread() + ")");
    try {
      _selectorThread = Thread.currentThread();
      _selector = Selector.open();
      for (int i = 0; i < _peers; i++) {
        connect(_from != null ? new InetSocketAddress(_from, 0) : null, new InetSocketAddress(_to, _port));
      }
      
      while (true) {
        runScheduledTasks();
        int selected = select();
        
        if (selected > 0) {
          Set<SelectionKey> readySet = _selector.selectedKeys();
          
          for (Iterator<SelectionKey> it = readySet.iterator(); it.hasNext();) {
            final SelectionKey key = it.next();
            it.remove();
            
            if (key.isConnectable()) {
              connected(key);
            } else {
              if (key.isWritable()) {
                writeReady(key);
              }
              if (key.isReadable()) {
                readReady(key);
              }
            }
          }
        }
      }
    }
    
    catch (Throwable e) {
      _logger.error("unexpected exception", e);
      System.exit(1);
    }
  }
}
