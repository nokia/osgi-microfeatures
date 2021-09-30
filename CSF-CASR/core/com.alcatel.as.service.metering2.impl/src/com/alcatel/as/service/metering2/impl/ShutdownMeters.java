package com.alcatel.as.service.metering2.impl;

import java.util.*;
import java.io.*;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.apache.felix.dm.*;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.service.shutdown.*;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.impl.util.MetersReader;
import com.alcatel.as.service.metering2.util.*;
import com.alcatel.as.util.config.ConfigHelper;

public class ShutdownMeters extends MetersReader implements Shutdownable {
  private final static String CONF = Configuration.SHUTDOWN;
  public static final Logger LOGGER = Logger.getLogger("as.service.metering2.shutdown");
  private String _data;
  private PlatformExecutors _execs;
  private PlatformExecutor _exec;
  private Shutdown _shutdown;
  private MeteringService _metering;
  private List<CheckMeterEntry> _pendings;
  
  protected class CheckMeterEntry extends MeterEntry implements MeterListener<Boolean> {
    protected long _threshold;
    
    public Boolean updated(Meter meter, Boolean running) {
      if (running) {
        if (check(meter.getValue()) == false)
          return true;
        meterDone(this);
      }
      return false;
    }
    
    protected boolean check(long value) {
      return value == _threshold;
    }
    
    @Override
	public Meter start(MeteringService metering, Monitorable monitorable) {
      _meter = monitorable.getMeters().get(_meterName);
      if (_meter != null) {
        _meter.startScheduledJob(this, true, _exec, 100L, 0);
        return _meter;
      }
      return null;
    }
    
    @Override
    public boolean equals(Object o) {
      return o == this;
    }
    
    public String toString() {
      return "CheckMeterEntry[" + _monitorableName + "/" + _meterName + "=" + _threshold + "]";
    }
  }
  
  protected class CheckGTMeterEntry extends CheckMeterEntry {
    protected boolean check(long value) {
      return value >= _threshold;
    }
    
    public String toString() {
      return "CheckGTMeterEntry[" + _monitorableName + "/" + _meterName + ">=" + _threshold + "]";
    }
  }
  
  protected class CheckLTMeterEntry extends CheckMeterEntry {
    protected boolean check(long value) {
      return value <= _threshold;
    }
    
    public String toString() {
      return "CheckLTMeterEntry[" + _monitorableName + "/" + _meterName + "<=" + _threshold + "]";
    }
  }
  
  public String toString() {
    return "ShutdownMeters[" + _pendings + "]";
  }
  
  public synchronized void updated(Dictionary props) {
    LOGGER.info(this + " : updated");
    _data = (String) props.get(CONF);
  }
  
  public void shutdown(Shutdown shutdown) {
    _shutdown = shutdown;
    if (_data == null || _metering == null || _execs == null) {
      done();
      return;
    }
    _exec = _execs.createQueueExecutor(_execs.getThreadPoolExecutor());
    _exec.execute(new Runnable() {
      public void run() {
        shutdownInExec();
      }
    });
  }
  
  private void shutdownInExec() {
    _pendings = new Vector<>(); // sync for toString()
    for (MeterEntry meterEntry : parseToList(_data, LOGGER)) {
      Monitorable monitorable = _metering.getMonitorable(meterEntry._monitorableName);
      if (monitorable == null) {
        LOGGER.warn(this + " : skipping : " + meterEntry + " : cannot find monitorable");
        continue;
      }
      Meter meter = meterEntry.start(_metering, monitorable);
      if (meter == null)
        LOGGER.info(this + " : failed to start : " + meterEntry);
      else {
        LOGGER.info(this + " : started : " + meterEntry);
        if (meterEntry instanceof CheckMeterEntry) {
          _pendings.add((CheckMeterEntry) meterEntry);
        }
      }
    }
    if (_pendings.size() == 0)
      done();
  }
  
  @Override
  protected MeterEntry parseLine(String line, Logger logger) {
    if (line.startsWith("checkMeter")) {
      String lt = getParam(line, "-lt");
      String gt = getParam(line, "-gt");
      String eq = getParam(line, "-eq");
      String rule = lt != null ? lt : (gt != null ? gt : eq);
      boolean doEq = rule == eq;
      boolean doGt = rule == gt;
      boolean doLt = rule == lt;
      long value = 0L;
      try {
        value = Long.parseLong(rule);
      } catch (Exception e) {
        return null;
      }
      CheckMeterEntry checkMeter = doEq ? new CheckMeterEntry() : (doGt ? new CheckGTMeterEntry()
          : new CheckLTMeterEntry());
      checkMeter._monitorableName = getParam(line, "-m", "-monitorable");
      checkMeter._meterName = getParam(line, "-mt", "-meter");
      checkMeter._threshold = value;
      return checkMeter;
    }
    return null;
  }
  
  protected void meterDone(CheckMeterEntry meter) {
    LOGGER.info(this + " : " + meter + " : done");
    _pendings.remove(meter);
    if (_pendings.size() == 0)
      done();
  }
  
  protected void done() {
    LOGGER.info(this + " : done");
    _shutdown.done(this);
  }
  
}
