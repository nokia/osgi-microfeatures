package com.nextenso.mux.impl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.alcatel.as.service.metering.Counter;
import com.alcatel.as.service.metering.MeteringService;

/**
 * Utility class that calculate the system load It works be reading the
 * /proc/stat file and calculates the system load in percentile form.
 */
class ProcStatWatcher {
  
	private final static Logger logger = Logger
			.getLogger("as.service.controlflow.cpu");
  
	volatile private int _currentCpuLoad = 0;
	// we will schedule computation with a fixed delay,
	// there is no multiple write possible, Atomic seems useless here
	// private final AtomicInteger _currentCpuLoad = new AtomicInteger(0);
  // private   long cpuA;
  private long _userModeA;
  // private   long userModeNiceA;
  private long _systemModeA;
  private long _idleA;
  private long _elapsedA;
  private long _cpucA;
  
  private String _cpu = "";
  private long _userMode = 0;
  private long _userModeNice = 0;
  private long _systemMode = 0;
  private long _idle = 0;
  private long _elapsed = 0;
  private long _cpuc = 0;
  private int _load = 0;
  private int _statCount = 0;
  private int _statCountError = 0;
  
  private Counter _meterCpuSystem;
  private Counter _meterCpuUser;
  private Counter _meterCpuIdle;
  
  protected ProcStatWatcher(MeteringService ms) {
    _meterCpuSystem = ms.getCounter("as.stat.cpu.system");
    _meterCpuUser = ms.getCounter("as.stat.cpu.user");
    _meterCpuIdle = ms.getCounter("as.stat.cpu.idle");
    if (readStat() == 1) {
      toPrevious();
    }
  }
  
  public int getCurrentCpuLoad() {
		// return _currentCpuLoad.get();
		return _currentCpuLoad;
  }
  
  private void toPrevious() {
    _userModeA = _userMode;
    //	userModeNiceA = userModeNice;
    _systemModeA = _systemMode;
    _idleA = _idle;
    _elapsedA = _elapsed;
    _cpucA = _cpuc;
  }
  
  public void calcStat() {
    long elapsedD;
    long cpuD;
    
    if (readStat() == 0) {
			// _currentCpuLoad.set(0);
			_currentCpuLoad = 0;
    }
    
    _statCount++;
    elapsedD = _elapsed - _elapsedA;
    cpuD = _cpuc - _cpucA;
    if (elapsedD > 0) {
      _load = (int) ((cpuD * 100) / elapsedD);
    } else {
      _statCountError++;
      _load = 0;
			logger.info("FlowControl surveyProcStats: statCount=" + _statCount
					+ " statCountError=" + _statCountError + " elapsedD="
					+ elapsedD + " cpuD=" + cpuD + " load=" + _load);
    }
    if (logger.isDebugEnabled()) {
			logger.debug("elapsedD=" + elapsedD + " cpuD=" + cpuD + " load="
					+ _load);
    }
    
    toPrevious();
		_currentCpuLoad = _load;
		// _currentCpuLoad.set(_load);
  }
  
  private int readStat() {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader("/proc/stat"));
      
      String line;
      int count = 0;
      while ((line = reader.readLine()) != null) {
        StringTokenizer parse = new StringTokenizer(line);
        try {
          while (parse.hasMoreTokens()) {
            count++;
            if (count == 1) {
              _cpu = parse.nextToken();
              // System.out.println("cpu=" + cpu);
            } else if (count == 2) { // userMode
							_userMode = Long.valueOf(parse.nextToken())
									.longValue();
            } else if (count == 3) { // userMode Nice
							_userModeNice = Long.valueOf(parse.nextToken())
									.longValue();
            } else if (count == 4) { // System
							_systemMode = Long.valueOf(parse.nextToken())
									.longValue();
            } else if (count == 5) { // Idle
              _idle = Long.valueOf(parse.nextToken()).longValue();
            }
            
            if (count == 5) {
							_elapsed = _userMode + _userModeNice + _systemMode
									+ _idle;
              _cpuc = _userMode + _userModeNice + _systemMode;
              if (logger.isDebugEnabled()) {
								logger.debug("line=<" + line + "> cpu=" + _cpu
										+ "userMode=" + _userMode
										+ " userModeNice=" + _userModeNice
										+ " systemMode=" + _systemMode
										+ " idle=" + _idle + " elapsed="
                    + _elapsed + " cpuc=" + _cpuc);
              }
              
              _meterCpuSystem.add(_systemMode - _systemModeA);
              _meterCpuUser.add(_userMode - _userModeA);
              _meterCpuIdle.add(_idle - _idleA);
              
              return 1;
            }
          }
        } catch (NoSuchElementException e) {
          logger.error("Exception NoSuchElementException", e);
        }
        return 0;
      }
    } catch (IOException e) {
      logger.error("IoException", e);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
        }
      }
    }
    
    return 0;
  }
}
