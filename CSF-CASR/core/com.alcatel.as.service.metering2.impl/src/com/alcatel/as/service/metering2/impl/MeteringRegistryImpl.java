package com.alcatel.as.service.metering2.impl;

import java.util.*;
import java.util.concurrent.*;


import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Filter;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.Monitorable;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.util.*;
import com.alcatel.as.service.concurrent.SerialExecutor;

public class MeteringRegistryImpl implements MeteringRegistry {

    private static Object VOID = new Object ();
    
    private Map<String, Monitorable> _monitorables = new ConcurrentHashMap<> ();
    private Map<Object, TrackerWrapper> _trackers = new ConcurrentHashMap<> ();

    private volatile BundleContext _osgi; // injected before init() method by DM
    private Filter _filter;

    public MeteringRegistryImpl (){
    }

    public void init (){
	String filter = "(objectClass="+Monitorable.class.getName ()+")";
	try {
	    _filter = _osgi.createFilter(filter);
	} catch (Exception e) {
	    throw new IllegalArgumentException ("Illegal Filter : "+filter);
	}
    }

    public void destroy (){
	for (TrackerWrapper tracker : _trackers.values ())
	    tracker.stop (null);
	_trackers.clear ();
    }
    
    public void bindMonitorable (Monitorable monitorable){
	_monitorables.put (monitorable.getName (), monitorable);
    }
    
    public void unbindMonitorable (Monitorable monitorable){
	_monitorables.remove (monitorable.getName ());
    }
    
    public Monitorable getMonitorable (String monitorable){
	return _monitorables.get (monitorable);
    }
    public Meter getMeter (String monitorable, String meter){
	Monitorable m = _monitorables.get (monitorable);
	return m != null ? m.getMeters ().get (meter) : null;
    }
    public List<Monitorable> getMonitorables (String monitorablePattern){
	List<Monitorable> ret = new ArrayList<> ();
	for (Monitorable monitorable : _monitorables.values ()){
	    if (Meters.matches (monitorable, monitorablePattern)) ret.add (monitorable);
	}
	return ret;
    }
    public <T> T iterateMonitorables (String monitorablePattern, MonitorableIterator<T> it, T ctx){
	for (Monitorable monitorable : _monitorables.values ()){
	    if (Meters.matches (monitorable, monitorablePattern)) ctx = it.next (monitorable, ctx);
	}
	return ctx;
    }
    public <T> T iterateMonitorables (MonitorableIterator<T> it, T ctx){
	for (Monitorable monitorable : _monitorables.values ()){
	    ctx = it.next (monitorable, ctx);
	}
	return ctx;
    }
    
    public Object trackMonitorable (String monitorable, Tracker tracker, Executor exec){
	return trackMonitorables ("="+monitorable, tracker, exec);
    }
    public Object trackMonitorables (String monitorablePattern, Tracker tracker, Executor exec){
	TrackerWrapper wrapper = new TrackerWrapper (monitorablePattern, tracker, exec);
	Object reg = new Object ();
	_trackers.put (reg, wrapper);
	wrapper.start (reg);
	return reg;
    }
    public boolean stopTracking (Object trackingRegistration, Object ctx){
	TrackerWrapper wrapper = _trackers.remove (trackingRegistration);
	if (wrapper == null) return false;
	wrapper.stop (ctx);
	return true;
    }

    private class TrackerWrapper implements ServiceTrackerCustomizer {

	private String _monitorable;
	private Tracker _tracker;
	private Executor _exec;
	private ServiceTracker _serviceTracker;
	private boolean _stopped;
	private Map<Monitorable, Object> _ctxs = new HashMap<> ();
	
	private TrackerWrapper (String monitorable, Tracker tracker, Executor exec){
	    _monitorable = monitorable;
	    _tracker = tracker;
	    _exec = exec != null ? exec : new SerialExecutor ();
	}
	private void start (final Object trackingRegistration){
	    Runnable r = new Runnable (){
		    public void run (){
			_tracker.init (trackingRegistration);
		    }
		};
	    _exec.execute (r);
	    _serviceTracker = new ServiceTracker(_osgi, _filter, TrackerWrapper.this);
	    _serviceTracker.open ();
	}
	private void stop (final Object ctx){
	    Runnable r = new Runnable (){
		    public void run (){
			_stopped = true;
			_serviceTracker.close ();
			_tracker.destroyed (ctx);
		    }
		};
	    _exec.execute (r);
	}
	@Override
	public Object addingService(ServiceReference ref) {
	    final Monitorable monitorable = (Monitorable) _osgi.getService (ref);
	    if (!Meters.matches (monitorable, _monitorable)) return VOID;
	    Runnable r = new Runnable (){
		    public void run (){
			if (_stopped) return;
			_ctxs.put (monitorable, _tracker.onAdd (monitorable));
		    }
		};
	    _exec.execute (r);
	    return monitorable;
	}
   
	@Override
	public void removedService(ServiceReference ref, Object attachment) {
	    if (attachment == VOID) return;
	    final Monitorable monitorable = (Monitorable) attachment;
	    Runnable r = new Runnable (){
		    public void run (){
			if (_stopped) return;
			_tracker.onRemove (monitorable, _ctxs.remove (monitorable));
		    }
		};
	    _exec.execute (r);
	}
   
	@Override
	public void modifiedService(ServiceReference ref, Object attachment) {
	    if (attachment == VOID) return;
	    final Monitorable monitorable = (Monitorable) attachment;
	    Runnable r = new Runnable (){
		    public void run (){
			if (_stopped) return;
			_ctxs.put (monitorable, _tracker.onUpdate (monitorable, _ctxs.get (monitorable)));
		    }
		};
	    _exec.execute (r);
	}
    }
    
}
