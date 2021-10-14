package com.nokia.as.diameter.tools.loader;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

public class TPSManager implements Runnable {
  static Logger _logger = Logger.getLogger(TPSManager.class);
  final String _tpsranges;
  final ArrayList<TPS> _tpsList = new ArrayList<TPS>();
  volatile int _tpsIndex = 0;
  final ScheduledExecutorService _timer;
  private TPSListener _tpsListener;
  
  static class TPS {
    int tps;
    long elapsedNano;
    
    public TPS(int tpsPerConnection, long elapsedNano) {
      this.tps = tpsPerConnection;
      this.elapsedNano = elapsedNano;
    }
  }
  
  TPSManager(String tpsranges, ScheduledExecutorService timer) {
    _tpsranges = tpsranges;
    _timer = timer;
  }
  
  public int start(TPSListener listener) {
    _tpsListener = listener;
    
    if (_tpsranges.indexOf("/") == -1) {
      _tpsList.add(new TPS(Integer.parseInt(_tpsranges), TimeUnit.NANOSECONDS.convert(Integer.MAX_VALUE,
          TimeUnit.SECONDS)));
    } else {      
      for (String tpsrange : _tpsranges.split(",")) {
        String[] info = tpsrange.split("/");
        info[0] = info[0].trim();
        if (info.length > 1) {
          info[1] = info[1].trim();
        }
        
        int tps = Integer.parseInt(info[0]);
        int s = info.length > 1 ? info[1].indexOf("s") : -1;
        if (s != -1) {
          info[1] = info[1].substring(0, s);
        }
        long elapsedSec = info.length > 1 ? Long.parseLong(info[1]): Long.MAX_VALUE;
        long elapsedNano = TimeUnit.NANOSECONDS.convert(elapsedSec, TimeUnit.SECONDS);
        _tpsList.add(new TPS(tps, elapsedNano));
      }
    }
    TPS first = _tpsList.get(0);
    if (_tpsList.size() > 1) {
      _timer.schedule(this, first.elapsedNano, TimeUnit.NANOSECONDS);
    }
    return first.tps;
  }
  
  @Override
  public void run() {
    TPS next = _tpsList.get(++_tpsIndex);
    _logger.warn("rescheduling TPS: " + next.tps + ", elapsed="
        + TimeUnit.MILLISECONDS.convert(next.elapsedNano, TimeUnit.NANOSECONDS));
    _tpsListener.setTPS(next.tps);
    if (_tpsIndex < (_tpsList.size() - 1)) {
      _timer.schedule(this, next.elapsedNano, TimeUnit.NANOSECONDS);
    }
  }
}
