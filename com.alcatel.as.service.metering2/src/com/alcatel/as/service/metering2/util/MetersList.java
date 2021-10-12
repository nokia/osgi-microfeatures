// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering2.util;

import java.util.ArrayList;
import java.util.List;

import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.Monitorable;

/**
 * This is a helper class to manipulate Meters lists when a Monitorable is updated.
 */
public class MetersList {

    protected List<Meter> _meters = new ArrayList<> ();
    protected String _pattern;

    public static interface Listener {
	void addedMeter (Monitorable monitorable, Meter meter);
	void removedMeter (Monitorable monitorable, Meter meter);
    }

    /**
     * New MetersList.
     */
    public MetersList (){
	this (null);
    }
    /**
     * New MetersList.
     * A meters pattern is provided.
     * @param pattern the meters pattern.
     */
    public MetersList (String pattern){
	_pattern = pattern;
    }
    /**
     * The list is filled with the monitorable.
     * @param monitorable the monitorable whose meters are loaded.
     * @return this
     */
    public MetersList init (Monitorable monitorable){
	for (Meter meter : monitorable.getMeters ().values ()){
	    if (_pattern != null && !Meters.matches (meter.getName (), _pattern)) continue;
	    _meters.add (meter);
	}
	return this;
    }
    /**
     * The list is filled with the monitorable.
     * @param monitorable the monitorable whose meters are loaded.
     * @param listener an optional listener
     * @return this
     */
    public MetersList init (Monitorable monitorable, Listener listener){
	for (Meter meter : monitorable.getMeters ().values ()){
	    if (_pattern != null && !Meters.matches (meter.getName (), _pattern)) continue;
	    _meters.add (meter);
	    if (listener != null) listener.addedMeter (monitorable, meter);
	}
	return this;
    }
    /**
     * Returns the meters list.
     * @return the list
     */
    public List<Meter> getMeters (){ return _meters;}
     /**
     * Returns the pattern.
     * @return the pattern (or null if not set).
     */
    public String getPattern (){ return _pattern;}

    /**
     * Updates the Meters list (when a monitorable is updated).
     * @param monitorable the monitorable
     * @param added an optional list where to fill added meters
     * @param removed an optional list where to fill removed meters
     * @return the new meters list
     */
    public List<Meter> update (Monitorable monitorable, final List<Meter> added, final List<Meter> removed){
	Listener listener = null;
	if (added != null || removed != null){
	    listener = new Listener (){
		    public void addedMeter (Monitorable monitorable, Meter meter){
			if (added != null) added.add (meter);
		    }
		    public void removedMeter (Monitorable monitorable, Meter meter){
			if (removed != null) removed.add (meter);
		    }
		};
	}
	return update (monitorable, listener);
    }
    /**
     * Updates the Meters list (when a monitorable is updated).
     * @param Monitorable the monitorable
     * @param listener an optional listener
     * @return the new meters list
     */
    public List<Meter> update (Monitorable monitorable, Listener listener){
	boolean noDiff = (listener == null);
	List<Meter> old = noDiff ? null : (List<Meter>) ((ArrayList)_meters).clone ();
	_meters.clear ();
	for (Meter meter : monitorable.getMeters ().values ()){
	    if (_pattern != null && !Meters.matches (meter.getName (), _pattern)) continue;
	    _meters.add (meter);
	    if (noDiff) continue;
	    if (!old.remove (meter)){
		listener.addedMeter (monitorable, meter);
	    }
	}
	if (!noDiff){
	    for (Meter meter : old){
		listener.removedMeter (monitorable, meter);
	    }
	}
	return _meters;
    }
}
