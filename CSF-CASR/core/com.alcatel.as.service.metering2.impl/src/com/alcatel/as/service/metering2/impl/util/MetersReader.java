package com.alcatel.as.service.metering2.impl.util;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.Monitorable;
import com.alcatel.as.service.metering2.util.Meters;

public class MetersReader {
  
  public static class MonitorableEntry {
	  public Object _attachmnent;
	  public Monitorable _monitorable;
	  public final List<MeterEntry> _meters = new ArrayList<>();
    
	  public void attach(Object o) {
      _attachmnent = o;
    }
    
    public <T> T attachment() {
      return (T) _attachmnent;
    }
  }
  
  public static class MeterEntry {
	  public String _monitorableName, _meterName;
	  public Meter _meter;
	  public Object _attachmnent;
    
	  public boolean check() {
      return _monitorableName != null && _meterName != null;
    }
    
	  public Meter start(MeteringService metering, Monitorable monitorable) {
      return null;
    }
    
	  public void stop(Monitorable monitorable) {
    }
    
    @Override
    public boolean equals(Object o) {
      if (o == this)
        return true;
      // only used for lookup in lists
      MeterEntry other = (MeterEntry) o;
      return other._monitorableName == _monitorableName && other._meterName.equals(_meterName)
          && o.getClass().equals(getClass());
    }
    
    protected void attach(Object o) {
      _attachmnent = o;
    }
    
    protected <T> T attachment() {
      return (T) _attachmnent;
    }
  }
  
  public class RateMeterEntry extends MeterEntry {
    protected String _periodS;
    protected long _period = 1000L;
    
    public boolean check() {
      try {
        if (_periodS != null)
          _period = Long.parseLong(_periodS);
      } catch (Throwable t) {
        return false;
      }
      return super.check();
    }
    
    public long getPeriod() {
      return _period;
    }
    
    public Meter start(MeteringService metering, Monitorable monitorable) {
      Meter target = monitorable.getMeters().get(_meterName);
      if (target == null) {
        return null;
      }
      _meter = Meters.createRateMeter(metering, target, _period);
      Meter existing = monitorable.getMeters().get(_meter.getName());
      if (existing != null) {
        // !! already existing
        Meters.stopRateMeter(_meter);
        _meter = existing;
      } else
        monitorable.getMeters().put(_meter.getName(), _meter);
      return _meter;
    }
    
    public void stop(Monitorable monitorable) {
      Meters.stopRateMeter(_meter);
      monitorable.getMeters().remove(_meter.getName());
    }
    
    public String toString() {
      return "RateMeter[" + _monitorableName + "/" + _meterName + "]";
    }
  }
  
  public class MaxValueMeterEntry extends MeterEntry {
    protected String _scheduledS;
    protected long _scheduled = -1L;
    
    public boolean check() {
      try {
        if (_scheduledS != null)
          _scheduled = Long.parseLong(_scheduledS);
      } catch (Throwable t) {
        return false;
      }
      return super.check() && (_scheduled == -1L || _scheduled > 0L);
    }
    
    public long getScheduled() {
      return _scheduled;
    }
    
    public Meter start(MeteringService metering, Monitorable monitorable) {
      Meter target = monitorable.getMeters().get(_meterName);
      if (target == null)
        return null;
      if (_scheduled == -1L)
        _meter = Meters.createMaxValueMeter(metering, target);
      else
        _meter = Meters.createScheduledMaxValueMeter(metering, target, _scheduled, 0);
      Meter existing = monitorable.getMeters().get(_meter.getName());
      if (existing != null) {
        // !! already existing
        Meters.stopMaxValueMeter(_meter);
        _meter = existing;
      } else
        monitorable.getMeters().put(_meter.getName(), _meter);
      return _meter;
    }
    
    public void stop(Monitorable monitorable) {
      Meters.stopMaxValueMeter(_meter);
      monitorable.getMeters().remove(_meter.getName());
    }
    
    public String toString() {
      return "MaxValueMeterEntry[" + _monitorableName + "/" + _meterName + "/" + _scheduled + "]";
    }
  }
  
  public class MovingMaxValueMeterEntry extends MeterEntry {
    protected String _samplingS, _samplesS, _name;
    protected long _sampling = 1000L;
    protected int _samples = 5;
    
    public boolean check() {
      try {
        if (_samplingS != null)
          _sampling = Long.parseLong(_samplingS);
        if (_samplesS != null)
          _samples = Integer.parseInt(_samplesS);
      } catch (Throwable t) {
        return false;
      }
      return super.check() && _samples > 0 && _sampling > 0L && _name != null && _name.length() > 0;
    }
    
    public long getSampling() {
      return _sampling;
    }
    
    public int getSamples() {
      return _samples;
    }
    
    public Meter start(MeteringService metering, Monitorable monitorable) {
      Meter target = monitorable.getMeters().get(_meterName);
      if (target == null)
        return null;
      _meter = Meters.createMovingMaxValueMeter(metering, _name, target, _sampling, _samples);
      Meter existing = monitorable.getMeters().get(_meter.getName());
      if (existing != null) {
        // !! already existing
        Meters.stopMaxValueMeter(_meter);
        _meter = existing;
      } else
        monitorable.getMeters().put(_meter.getName(), _meter);
      return _meter;
    }
    
    public void stop(Monitorable monitorable) {
      Meters.stopMaxValueMeter(_meter);
      monitorable.getMeters().remove(_meter.getName());
    }
    
    public String toString() {
      return "MovingMaxValueMeterEntry[" + _monitorableName + "/" + _meterName + "/" + _sampling + "/" + _samples + "]";
    }
  }

  public static interface LineReader<T> {
    public T readLine (String line, T ctx);
  }
  public static <T> T parse(String data, LineReader<T> lineReader, T ctx) {
    BufferedReader reader = new BufferedReader(new StringReader(data));
    try {
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.startsWith("#") || line.length() == 0)
          continue;
	ctx = lineReader.readLine (line, ctx);
      }
      reader.close ();
    } catch (java.io.IOException ioe) {
      // no io possible
    }
    return ctx;
  }
  
  public List<MeterEntry> parseToList(String data, final Logger logger) {
    LineReader<List<MeterEntry>> reader = new LineReader<List<MeterEntry>> (){
	public List<MeterEntry> readLine (String line, List<MeterEntry> list){
	  MeterEntry meterContext = null;
	  if (line.startsWith("createRateMeter")) {
	    RateMeterEntry context = newRateMeterEntry();
	    context._monitorableName = getParam(line, "-m", "-monitorable");
	    context._meterName = getParam(line, "-mt", "-meter");
	    context._periodS = getParam(line, "-p", "-period");
	    meterContext = context;
	  } else if (line.startsWith("createMaxValueMeter")) {
	    MaxValueMeterEntry context = newMaxValueMeterEntry();
	    context._monitorableName = getParam(line, "-m", "-monitorable");
	    context._meterName = getParam(line, "-mt", "-meter");
	    context._scheduledS = getParam(line, "-scheduled");
	    meterContext = context;
	  } else if (line.startsWith("createMovingMaxValueMeter")) {
	    MovingMaxValueMeterEntry context = newMovingMaxValueMeterEntry();
	    context._monitorableName = getParam(line, "-m", "-monitorable");
	    context._meterName = getParam(line, "-mt", "-meter");
	    context._samplingS = getParam(line, "-sampling");
	    context._samplesS = getParam(line, "-samples");
	    meterContext = context;
	  } else if ((meterContext = parseLine(line, logger)) == null) {
	    logger.warn(this + " : skipping line : " + line);
	    return list;
	  }
	  if (meterContext.check()) {
	    list.add(meterContext);
	    logger.info(this + " : parsed : " + meterContext);
	  } else {
	    logger.error(this + " : invalid line : " + line);
	  }
	  return list;
	}};
    return parse (data, reader, new ArrayList<MeterEntry> ());
  }
  
  public Map<String, MonitorableEntry> parseToMap(String data, Logger logger) {
    Map<String, MonitorableEntry> monitorables = new HashMap<>();
    List<MeterEntry> meters = parseToList(data, logger);
    for (MeterEntry meter : meters) {
      MonitorableEntry monitorable = monitorables.get(meter._monitorableName);
      if (monitorable == null)
        monitorables.put(meter._monitorableName, monitorable = newMonitorableEntry());
      monitorable._meters.add(meter);
    }
    return monitorables;
  }
  
  // the following can be used for overriding
  protected MonitorableEntry newMonitorableEntry() {
    return new MonitorableEntry();
  }
  
  protected RateMeterEntry newRateMeterEntry() {
    return new RateMeterEntry();
  }
  
  protected MaxValueMeterEntry newMaxValueMeterEntry() {
    return new MaxValueMeterEntry();
  }
  
  protected MovingMaxValueMeterEntry newMovingMaxValueMeterEntry() {
    return new MovingMaxValueMeterEntry();
  }
  
  protected MeterEntry parseLine(String line, Logger logger) {
    return null;
  }
  
  public static final String getParam(String line, String ... pnames) {
    String regex = "\'([^\']*)\'|\"([^\"]*)\"|(\\S+)";
    Matcher m = Pattern.compile(regex).matcher(line);

    while (m.find()) {
      for (String pname : pnames) {
        if(m.group().equals(pname))
        {
          // If this is not a flag
          if(m.find())
          {
            return m.group().replace("\"","").replace("'","");
          }
          return null;
        }
      }
    }
    return null;
  }
  public static final boolean getFlag(String line, boolean def, String ... fnames) {
    for (String fname : fnames) {
      String f = " " + fname;
      int index = line.indexOf(f);
      if (index == -1)
        continue;
      int end = index + f.length ();
      if (line.length () == end) return true;
      if (line.charAt (end) == ' ' || line.charAt (end) == '\t') return true;
    }
    return def;
  }
  public static final List<String> getParams(String line, String ... pnames) {
    return getParams (line, new ArrayList<String> (), pnames);
  }
  private static final List<String> getParams(String line, List<String> dest, String ... pnames) {
    for (String pname : pnames){
      String p = " " + pname + " ";
      int index = line.indexOf(p);
      if (index == -1)
        continue;
      index += p.length();
      if (index == line.length())
        continue; // this should be a flag !
      int end = line.indexOf(' ', index);
      if (end == -1)
        end = line.indexOf('\t', index);
      if (end == -1)
        end = line.length();
      dest.add (line.substring(index, end));
      if (end < line.length ())
	getParams (line.substring (end), dest, pname);
    }
    return dest;
  }
  public static void main(String[] s){
    System.out.println (getFlag ("command -f", false, "-flag", "-f"));
    System.out.println (getFlag ("command -f -k", false, "-flag", "-f"));
    System.out.println (!getFlag ("command -fl", false, "-flag", "-f"));
    System.out.println (getParams ("command -p P1 -p P2 -p P3 -pp P4 -pp P5 -xx -p P6 -ppp X -p", "-p", "-pp"));
  }
}
