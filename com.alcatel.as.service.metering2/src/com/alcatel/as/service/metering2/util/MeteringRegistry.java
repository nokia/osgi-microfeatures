// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering2.util;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.Monitorable;

public interface MeteringRegistry {

    public static interface Tracker<T> {
	void init (Object trackingRegistration);
	T onAdd (Monitorable monitorable);
	T onUpdate (Monitorable monitorable, T monitorableCtx);
	void onRemove (Monitorable monitorable, T monitorableCtx);
	void destroyed (Object ctx);
    }
    
    public static class AbstractTracker<T> implements Tracker<T> {
	protected List<Monitorable> _monitorablesList;
	protected Map<String, Monitorable> _monitorablesMap;
	protected Object _attachment, _registration;
	protected boolean _destroyed, _keepSingleton;
	protected Monitorable _monitorable;
	public AbstractTracker (boolean keepSingleton, boolean keepList, boolean keepMap){
	    if (keepList) _monitorablesList = new ArrayList<> ();
	    if (keepMap) _monitorablesMap = new HashMap<> ();
	    _keepSingleton = keepSingleton;
	}
	public AbstractTracker attach (Object o){ _attachment = o; return this;}
	public <A> A attachment (){ return (A) _attachment;}
	public Monitorable getSingleton (){ return _monitorable;}
	public List<Monitorable> getList (){ return _monitorablesList;}
	public Map<String, Monitorable> getMap (){ return _monitorablesMap;}
	public Object registration (){ return _registration;}
	public void init (Object registration){ _registration = registration;}
	public T onAdd (Monitorable monitorable){
	    if (_keepSingleton) _monitorable = monitorable;
	    if (_monitorablesList != null) _monitorablesList.add (monitorable);
	    if (_monitorablesMap != null) _monitorablesMap.put (monitorable.getName (), monitorable);
	    return null;
	}
	public T onUpdate (Monitorable monitorable, T monitorableCtx){return monitorableCtx;}
	public void onRemove (Monitorable monitorable, T monitorableCtx){
	    if (_keepSingleton) _monitorable = null;
	    if (_monitorablesList != null) _monitorablesList.remove (monitorable);
	    if (_monitorablesMap != null) _monitorablesMap.remove (monitorable.getName ());
	}
	public void destroyed (Object ctx){_destroyed = true;}
	public boolean isDestroyed (){ return _destroyed;}
    }
    public static class MetersTracker extends AbstractTracker<MetersList> implements MetersList.Listener {
	protected String _metersPattern;
	public MetersTracker (String metersPattern){
	    super (false, false, false);
	    _metersPattern = metersPattern;
	}
	public MetersTracker (String metersPattern, boolean keepMonitorableSingleton, boolean keepMonitorableList, boolean keepMonitorableMap){
	    super (keepMonitorableSingleton, keepMonitorableList, keepMonitorableMap);
	    _metersPattern = metersPattern;
	}
	@Override
	public MetersList onAdd (Monitorable monitorable){
	    super.onAdd (monitorable);
	    addedMonitorable (monitorable);
	    MetersList list = new MetersList (_metersPattern);
	    list.init (monitorable, this);
	    return list;
	}
	public void addedMonitorable (Monitorable monitorable){} // for override
	public void addedMeter (Monitorable monitorable, Meter meter){} // for override
	public void removedMeter (Monitorable monitorable, Meter meter){} // for override
	public void removedMonitorable (Monitorable monitorable, List<Meter> meters){} // for override
	@Override
	public MetersList onUpdate (Monitorable monitorable, MetersList list){
	    super.onUpdate (monitorable, list);
	    list.update (monitorable, this);
	    return list;
	}
	@Override
	public void onRemove (Monitorable monitorable, MetersList list){
	    removedMonitorable (monitorable, list.getMeters ());
	    super.onRemove (monitorable, list);
	}
    }

    public Monitorable getMonitorable (String monitorable);
    public List<Monitorable> getMonitorables (String monitorablePattern);
    public Meter getMeter (String monitorable, String name);
    public <T> T iterateMonitorables (MonitorableIterator<T> iterator, T ctx);
    public <T> T iterateMonitorables (String monitorablePattern, MonitorableIterator<T> iterator, T ctx);
    public Object trackMonitorable (String monitorable, Tracker tracker, Executor exec);
    public Object trackMonitorables (String monitorablePattern, Tracker tracker, Executor exec);
    public boolean stopTracking (Object trackingRegistration, Object ctx);

}
