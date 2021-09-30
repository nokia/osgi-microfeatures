package com.alcatel.as.service.metering2.impl;

import java.util.*;
import java.util.concurrent.*;
import org.osgi.framework.BundleContext;
import org.apache.log4j.Logger;

import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.impl.util.MetersReader;
import com.alcatel.as.service.metering2.util.*;
import com.alcatel.as.service.concurrent.SerialExecutor;


public class DerivedMeters2 {

	private final static Logger LOGGER = Logger.getLogger(DerivedMeters2.class);
	
    private MeteringService _meteringService;
    private MeteringRegistry _meteringRegistry;
    private BundleContext _osgi;
    
    protected final SerialExecutor _serial = new SerialExecutor(LOGGER);

    protected Map<String, Entry> _entries = new HashMap<> ();
    protected Dictionary _conf;

    public void updated(Dictionary props) {
	if (_conf == null)
	    _conf = props;
	else{
	    _conf = props;
	    start ();
	}
    }
    
    // TODO to remove
    public void setDep(MeteringService ser, MeteringRegistry reg) {
    	_meteringService= ser;
    	_meteringRegistry = reg;
    }
    
    
    public void start (){
    _serial.execute(new Runnable() {
      public void run() {
    	  String data = (String) _conf.get(Configuration.DERIVED);
	if (LOGGER.isDebugEnabled ()) LOGGER.debug ("DerivedMeters2 : parsing : "+data);
        if (data == null)
          return;
	MetersReader.LineReader<Map<String, Entry>> reader = new MetersReader.LineReader<Map<String, Entry>> (){
		public Map<String, Entry> readLine (String line, Map<String, Entry> entries){
		    Entry newEntry = null;
		    Entry o = _entries.remove (line);
		    if (o != null){
			if (LOGGER.isDebugEnabled ()) LOGGER.debug ("DerivedMeters2 : keeping entry : "+o);
			entries.put (line, o);
		    }
		    else if (line.startsWith ("mergeMeters")){
			if (LOGGER.isDebugEnabled ()) LOGGER.debug ("DerivedMeters2 : parsing line : "+line);
			newEntry = new MergedMeters (MetersReader.getParam (line, "-m", "-mon", "-monitorable"),// apply the merge in a single monitorable
						     MetersReader.getParam (line, "-ms", "-monitorables"), // apply the merge in many monitorables : ex : to all diameter clients
						     MetersReader.getParam (line, "-to", "-toMeter"),
						     MetersReader.getParams (line, "-mt", "-meter"),
						     MetersReader.getParam (line, "-mts", "-meters")); // dont provide many patterns for now for simplicity
		    } else if (line.startsWith ("mergeMonitorables")){
			if (LOGGER.isDebugEnabled ()) LOGGER.debug ("DerivedMeters2 : parsing line : "+line);
			newEntry = new MergedMonitorables (MetersReader.getParam (line, "-to", "-toMon", "-toMonitorable"),
							   MetersReader.getParams (line, "-m", "-mon", "-monitorable"),
							   MetersReader.getParam (line, "-ms", "-monitorables"),
							   MetersReader.getParams (line, "-mt", "-meter"),
							   MetersReader.getParams (line, "-mts", "-meters"),
							   MetersReader.getParam (line, "-desc", "-description"));
		    }
		    if (newEntry != null){
			Operation op = _addOp;
			if (MetersReader.getFlag (line, false, "-avg", "-average"))
			    op = _avgOp;
			else if (MetersReader.getFlag (line, false, "-max", "-maximum"))
			    op = _maxOp;
			else if (MetersReader.getFlag (line, false, "-min", "-minimum"))
			    op = _minOp;
			else if (MetersReader.getFlag (line, false, "-or"))
			    op = _orOp;
			else if (MetersReader.getFlag (line, false, "-nor"))
			    op = _norOp;
			else if (MetersReader.getFlag (line, false, "-and"))
			    op = _andOp;
			else if (MetersReader.getFlag (line, false, "-nand"))
			    op = _nandOp;
			else LOGGER.debug("Operation = addOp");
			newEntry.setOp (op);
			entries.put (line, newEntry);
		    }
		    return entries;
		}
	    };
	Map<String, Entry> entries = MetersReader.parse (data, reader, new HashMap<String, Entry> ());
	for (String line : _entries.keySet ()){
	    if (entries.get (line) != null) continue;
	    _entries.get (line).stop ();
	}
	_entries = entries;
	for (Entry entry : _entries.values ())
	    entry.start ();
      }});
    }

    static interface Entry {
	public void start (); //maybe called many times
	public void stop ();
	public void setOp (Operation op);
    }
    
    class MergedMeters extends MeteringRegistry.MetersTracker implements Entry {
		String _monName, _monsPattern, _toMeter;
		boolean _useMonsPattern;
		List<String> _metersNames;
		Object _tracking;
		Operation _op;
		boolean _started;
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
		public void start (){
		    if (_started) return;
		    if (LOGGER.isDebugEnabled ()) LOGGER.debug (this+" : start"+_meteringRegistry);
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
			Meter ref = monitorable.getMeters ().get (_toMeter);
			MergedMeter merged = ref.attachment();
			merged.add(meter);
//			if (LOGGER.isDebugEnabled ()) LOGGER.debug (this+" : addedMeter : "+meter.getName()+":"+meter.getValue()+" to "+ ref.getName()+ "(current value: "+ref.getValue()+")");
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
    private class MergedMonitorables extends MeteringRegistry.MetersTracker implements Entry {
	List<String> _metersNames;
	List<String> _metersPatterns;
	SimpleMonitorable _monitorable;
	String _monsPattern;
	List<String> _monNames;
	List<Object> _trackings = new ArrayList<Object> ();
	Operation _op;
	String _toMon;
	String _desc;
	boolean _started;
	private MergedMonitorables (String toMon, List<String> monsNames, String monsPattern, List<String> metersNames, List<String> metersPatterns, String desc){
	    super (null);
	    _toMon = toMon;
	    _monNames = monsNames;
	    _monsPattern = monsPattern;
	    _metersNames = metersNames;
	    _metersPatterns = metersPatterns;
	    _desc = desc != null ? desc : "DerivedMeters.MergedMonitorables";
	}
	public void setOp (Operation op){ _op = op;}
	@Override
	public String toString (){ return "DerivedMeters.MergedMonitorables["+_toMon+"]";}
	public void start (){
	    if (_started) return;
	    if (LOGGER.isDebugEnabled ()) LOGGER.debug (this+" : start"+_meteringRegistry);
	    _started = true;
	    _monitorable = new SimpleMonitorable (_toMon, _desc);
	    _monitorable.start (_osgi);
	    if (_monsPattern != null)
		_trackings.add (_meteringRegistry.trackMonitorables (_monsPattern, this, _serial));
	    for (String mon : _monNames)
		_trackings.add (_meteringRegistry.trackMonitorable (mon, this, _serial));
	}
	public void stop (){
	    if (LOGGER.isDebugEnabled ()) LOGGER.debug (this+" : stop");
	    for (Object tracking : _trackings) _meteringRegistry.stopTracking (tracking, null);
	    _monitorable.stop ();
	    _monitorable = null;
	    _trackings.clear ();
	}
	@Override
	public void addedMonitorable (Monitorable monitorable){
	    if (monitorable == _monitorable) return; // may happen when using a pattern for monitorables
	    if (LOGGER.isDebugEnabled ()) LOGGER.debug (this+" : addedMonitorable : "+monitorable.getName ());
	}
	@Override
	public void addedMeter (Monitorable monitorable, Meter meter){
	    if (monitorable == _monitorable) return;
	    boolean inNames = _metersNames.contains (meter.getName ());
	    boolean inPatterns = false;
	    if (!inNames){
		for (String pattern : _metersPatterns){
		    if (Meters.matches (meter, pattern)){
			inPatterns = true;
			break;
		    }
		}
	    }
	    if (inNames || inPatterns){
		if (LOGGER.isDebugEnabled ()) LOGGER.debug (this+" : addedMeter : "+meter.getName ());
		Meter existing = _monitorable.getMeters ().get (meter.getName ());
		if (existing != null){
		    MergedMeter merged = existing.attachment ();
		    merged._meters.add (meter);
		} else {
		    MergedMeter merged = new MergedMeter (_op);
		    merged._meters.add (meter);
		    Meter newMeter = _meteringService.createValueSuppliedMeter (meter.getName (), merged);
		    newMeter.attach (merged);
		    _monitorable.getMeters ().put (newMeter.getName (), newMeter);
		    _monitorable.updated ();
		}
	    }
	}
	@Override
	public void removedMeter (Monitorable monitorable, Meter meter){
	    if (monitorable == _monitorable) return;
	    Meter existing = _monitorable.getMeters ().get (meter.getName ());
	    if (existing != null){
		MergedMeter merged = existing.attachment ();
		if (merged._meters.remove (meter) == false) return;
		if (LOGGER.isDebugEnabled ()) LOGGER.debug (this+" : removedMeter : "+meter.getName ());
		if (merged._meters.size () == 0){
		    _monitorable.getMeters ().remove (meter.getName ());
		    _monitorable.updated ();
		}
	    }
	}
	@Override
	public void removedMonitorable (Monitorable monitorable, List<Meter> meters){
	    if (monitorable == _monitorable) return;
	    if (LOGGER.isDebugEnabled ()) LOGGER.debug (this+" : removedMonitorable : "+monitorable.getName ());
	    for (Meter meter : meters) removedMeter (monitorable, meter);
	}
    }
    private static class MergedMeter implements ValueSupplier {
		private Operation _op;
		List<Meter> _meters = new CopyOnWriteArrayList<> ();

		private MergedMeter (Operation op){ _op = op;}
		public long getValue (){
//			LOGGER.debug("[getValue MergedMeter] /"+_op+"/ meters -> "+_meters);
			return _op.getValue (_meters);
		}
		public void add(Meter meter) {
			_meters.add(meter);
//			LOGGER.debug("[Added MergedMeter] meters -> "+_meters);
		}
    }

    private static interface Operation {
	public long getValue (List<Meter> meters);
    }
    static Operation _addOp = new Operation (){
	    public long getValue (List<Meter> meters){
		long l = 0L;
		for (Meter meter : meters){
			l += meter.getValue();}
		return l;
	    }};
    private static Operation _avgOp = new Operation (){
	    public long getValue (List<Meter> meters){
		int nb = meters.size ();
		if (nb == 0) return 0L;
		return _addOp.getValue (meters) / nb;
	    }};
    private static Operation _maxOp = new Operation (){
	    public long getValue (List<Meter> meters){
		if (meters.size () == 0) return -1L;
		long max = 0L;
		for (Meter meter : meters){
		    long l = meter.getValue ();
		    if (l > max) max = l;
		}
		return max;
	    }};
    private static Operation _minOp = new Operation (){
	    public long getValue (List<Meter> meters){
		if (meters.size () == 0) return -1L;
		long min = Long.MAX_VALUE;
		for (Meter meter : meters){
		    long l = meter.getValue ();
		    if (l < min) min = l;
		}
		return min;
	    }};
    private static Operation _orOp = new Operation (){
	    public long getValue (List<Meter> meters){
		for (Meter meter : meters){
		    if (meter.getValue () == 1L) return 1L;
		}
		return 0L;
	    }};
    private static Operation _norOp = new Operation (){
	    public long getValue (List<Meter> meters){
		return _orOp.getValue (meters) == 0L ? 1L : 0L;
	    }};
    private static Operation _andOp = new Operation (){
	    public long getValue (List<Meter> meters){
		if (meters.size () == 0) return 0L;
		for (Meter meter : meters){
		    if (meter.getValue () == 0L) return 0L;
		}
		return 1L;
	    }};
    private static Operation _nandOp = new Operation (){
	    public long getValue (List<Meter> meters){
		return _andOp.getValue (meters) == 0L ? 1L : 0L;
	    }};
}
