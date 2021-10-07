package com.alcatel.as.service.metering2.impl.util;

import java.util.List;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.SerialExecutor;
import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.Monitorable;
import com.alcatel.as.service.metering2.impl.util.Entry;
import com.alcatel.as.service.metering2.impl.util.Util.MergedMeter;
import com.alcatel.as.service.metering2.impl.util.Util.Operation;
import com.alcatel.as.service.metering2.util.MeteringRegistry;



public class MergedMeters extends MeteringRegistry.MetersTracker implements Entry {
	String _monName, _monsPattern, _toMeter;
	boolean _useMonsPattern;
	List<String> _metersNames;
	Object _tracking;
	Operation _op;
	boolean _started;
	MeteringRegistry _meteringRegistry;
	MeteringService _meteringService;
	SerialExecutor _serial;
	private final static Logger LOGGER = Logger.getLogger(MergedMeters.class);
	
	MergedMeters (String monName, String monsPattern, String toMeter, List<String> metersNames, String metersPattern){
	    super (metersPattern, false, true, false);
	    _monName = monName;
	    _monsPattern = monsPattern;
	    _toMeter = toMeter;
	    _metersNames = metersNames;
	    if ((_monName == null && _monsPattern == null)||
		(_monName != null && _monsPattern != null) || // dont allow to provide a mon and a monPattern
		_toMeter == null) throw new IllegalArgumentException ("Invalid MergedMeters declaration");
	    _useMonsPattern = _monsPattern != null;
	}
	public void setOp (Operation op){ _op = op;}
	public String toString (){ return "DerivedMeters2.Meters["+(_useMonsPattern ? _monsPattern : _monName)+"/"+_toMeter+"]";}
	public void start (MeteringRegistry reg, MeteringService srv, SerialExecutor serial){
	    if (_started || reg == null || srv == null || serial == null) return;
	    _serial = serial;
	    _meteringRegistry = reg;
	    _meteringService = srv;
	    if (LOGGER.isDebugEnabled ()) LOGGER.debug (this+" : start");
	    _started = true;
	    if (_useMonsPattern) _tracking = _meteringRegistry.trackMonitorables (_monsPattern, this, _serial);
	    else _tracking = _meteringRegistry.trackMonitorable (_monName, this, _serial);
	}
	public void stop (){
	    if (LOGGER.isDebugEnabled ()) LOGGER.debug (this+" : stop");
	    _meteringRegistry.stopTracking (_tracking, null);
	}
	@Override
	public void destroyed (Object ctx){
	    super.destroyed (ctx);
	    for (Monitorable monitorable : _monitorablesList){
		monitorable.getMeters ().remove (_toMeter);
		monitorable.updated ();
	    }
	}
	@Override
	public void addedMonitorable (Monitorable monitorable){
	    if (LOGGER.isDebugEnabled ()) LOGGER.debug (this+" : addedMonitorable : "+monitorable.getName ());
	    MergedMeter merged = new MergedMeter (_op);
	    Meter meter = _meteringService.createValueSuppliedMeter (_toMeter, merged);
	    meter.attach (merged);
	    monitorable.getMeters ().put (_toMeter, meter);
	    monitorable.updated ();
	}
	@Override
	public void addedMeter (Monitorable monitorable, Meter meter){
	    if (meter.getName ().equals (_toMeter)) return;
	    if (_metersNames.size () == 0 || _metersNames.contains (meter.getName ())){
		if (LOGGER.isDebugEnabled ()) LOGGER.debug (this+" : addedMeter : "+meter.getName ());
		Meter ref = monitorable.getMeters ().get (_toMeter);
		MergedMeter merged = ref.attachment ();
		merged._meters.add (meter);
	    }
	}
	@Override
	public void removedMeter (Monitorable monitorable, Meter meter){
	    Meter ref = monitorable.getMeters ().get (_toMeter);
	    MergedMeter merged = ref.attachment ();
	    if (merged._meters.remove (meter))
		if (LOGGER.isDebugEnabled ()) LOGGER.debug (this+" : removedMeter : "+meter.getName ());
	}
	@Override
	public void removedMonitorable (Monitorable monitorable, List<Meter> meters){
	    if (LOGGER.isDebugEnabled ()) LOGGER.debug (this+" : removedMonitorable : "+monitorable.getName ());
	    monitorable.getMeters ().remove (_toMeter); // safety for GC
	}
    }