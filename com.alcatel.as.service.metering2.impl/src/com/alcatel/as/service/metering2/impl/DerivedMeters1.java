// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering2.impl;


import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
//import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.SerialExecutor;
import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.Monitorable;
import com.alcatel.as.service.metering2.impl.util.MetersReader;
import com.alcatel.as.service.metering2.util.MeteringRegistry;
import com.alcatel.as.service.metering2.util.Meters;

import aQute.lib.collections.MultiMap;

public class DerivedMeters1 {

	private final static Logger LOGGER = Logger.getLogger(DerivedMeters1.class);
	
	private MeteringService _meteringService;

	private MeteringRegistry _meteringRegistry;

	private boolean _hasStarted = false;
	
	protected final SerialExecutor _serial = new SerialExecutor(LOGGER);

	protected Map<String, Entry> _entries = new HashMap<>();

	protected Dictionary<?, ?> _conf;

	public void updated(Dictionary<?, ?> props) {
		if (_conf == null)
		    _conf = props;
		else{
		    if (_hasStarted){
		    	_conf = props;
		    	startTracking();
		    }
		}
	}

	public void start() {
		_hasStarted = true;
		startTracking();
	}

	private void startTracking(){
		_serial.execute(new Runnable() {
			public void run() {
				String data = (String) _conf.get(Configuration.DERIVED);
				if (LOGGER.isDebugEnabled())
					LOGGER.debug(this + " : parsing : " + data);
				if (data == null) {
					return;
				}
				MetersReader.LineReader<Map<String, Entry>> reader = new MetersReader.LineReader<Map<String, Entry>>() {
					public Map<String, Entry> readLine(String line, Map<String, Entry> entries) {
						Entry newEntry = null;
						Entry o = _entries.remove(line);
						if (o != null) {
							if (LOGGER.isDebugEnabled())
								LOGGER.debug(this + " : keeping entry : " + o);
							entries.put(line, o);
						} else if (line.startsWith("createRateMeter")) {
							if (LOGGER.isDebugEnabled())
								LOGGER.debug(this + " : parsing line : " + line);
							newEntry = new RateValueEntry(
									// apply the rate in a single monitorable
									MetersReader.getParam(line, "-m", "-mon", "-monitorable"),
									MetersReader.getParam(line, "-ms", "-monitorables"),
									MetersReader.getParams(line, "-mt", "-meter"),
									// dont provide many patterns for now for simplicity
									MetersReader.getParam(line, "-mts", "-meters"),
									MetersReader.getParam(line, "-p", "-period"));
							LOGGER.debug("DerivedMeters1 : Meters -> " + newEntry.getClass());
						} else if (line.startsWith("createMaxValueMeter")) {
							if (LOGGER.isDebugEnabled())
								LOGGER.debug(this + " : parsing line : " + line);
							newEntry = new MaxValueEntry(
									// apply the rate in a single monitorable
									MetersReader.getParam(line, "-m", "-mon", "-monitorable"),
									MetersReader.getParam(line, "-ms", "-mons", "-monitorables"),
									MetersReader.getParams(line, "-mt", "-meter"),
									// dont provide many patterns for now for simplicity
									MetersReader.getParam(line, "-mts", "-meters"),
									MetersReader.getParam(line, "-s", "-schedule"));
							LOGGER.debug("DerivedMeters1 : Meters -> " + newEntry.getClass());
						} else if (line.startsWith("createMovingMaxValueMeter")) {
							newEntry = new MovingMaxValueEntry(
									// apply the rate in a single monitorable
									MetersReader.getParam(line, "-m", "-mon", "-monitorable"),
									MetersReader.getParam(line, "-ms", "-mons", "-monitorables"),
									MetersReader.getParams(line, "-mt", "-meter"),
									// dont provide many patterns for now for simplicity
									MetersReader.getParam(line, "-mts", "-meters"),
									MetersReader.getParam(line, "-s","-sm", "-samples"),
									MetersReader.getParam(line, "-sg", "-smg", "-sampling"),
									MetersReader.getParam(line, "-name", "-n"));
							LOGGER.debug("DerivedMeters1 : Meters -> " + newEntry);
						}
						if (newEntry != null) {
							entries.put(line, newEntry);
						}
						return entries;
					}
				};
				Map<String, Entry> entries = MetersReader.parse(data, reader, new HashMap<String, Entry>());
				for (String line : _entries.keySet()) {
					if (entries.get(line) != null)
						continue;
					_entries.get(line).stop();
				}
				_entries = entries;
				for (Entry entry : _entries.values()){
					if (entry != null){
						LOGGER.debug(this + " ENTRY started = " + entry);
						entry.start();
					} else {
						LOGGER.debug(this + " Derived meter IS NULL");
					}
				}
			}
		});
	}
	
	private static interface Entry {
		public void start(); // maybe called many times

		public void stop();
	}

	private class RateValueEntry extends MeteringRegistry.MetersTracker implements Entry {
		String _monName, _monsPattern;
//		String _toMeter;
		boolean _useMonsPattern;
		List<String> _metersNames;
		Object _tracking;
		long _period = 1000L;
		boolean _started;
		Map<Monitorable, List<Meter>> _rateMeters;

		private RateValueEntry(String monName, String monsPattern, List<String> metersNames,
				String metersPattern, String period) {
			super(metersPattern, false, true, false);
			_monName = monName;
			_monsPattern = monsPattern;
			_metersNames = metersNames;
			_rateMeters = new ConcurrentHashMap<>();
			try {
		        if (period != null)
		          _period = Long.parseLong(period);
			} catch (NumberFormatException t){
				throw new IllegalArgumentException("Invalid MultiMonitorableMeters declaration");
			}
			if ((_monName == null && _monsPattern == null) 
					|| (_monName != null && _monsPattern != null)) // dont allow to provide a  mon and a monPattern
				throw new IllegalArgumentException("Invalid MultiMonitorableMeters declaration");
			_useMonsPattern = _monsPattern != null;
		}

		public String toString() {
			return "DerivedMeters1.Meters[" + (_useMonsPattern ? _monsPattern : _monName) + "]";
		}

		public void start() {
			if (_started)
				return;
			if (LOGGER.isDebugEnabled())
				LOGGER.debug(this + " : start");
			_started = true;
			if (_useMonsPattern){
				_tracking = _meteringRegistry.trackMonitorables(_monsPattern, this, _serial);
			} else {
				_tracking = _meteringRegistry.trackMonitorable(_monName, this, _serial);
			}
		}

		public void stop() {
			if (LOGGER.isDebugEnabled())
				LOGGER.debug(this + " : stop");
			_meteringRegistry.stopTracking(_tracking, null);
		}

		@Override
		public void destroyed(Object ctx) {
			super.destroyed(ctx);
			for (Monitorable monitorable : _monitorablesList){
				List<Meter> meterList = _rateMeters.get(monitorable);
				if (meterList != null){
					meterList.forEach(mtr -> {
						monitorable.getMeters ().remove(mtr.getName());
						monitorable.updated ();
					});
				}
				
		    }
		}

		@Override
		public void addedMonitorable(Monitorable monitorable) {
			super.addedMonitorable(monitorable);
			LOGGER.debug(this + "> Added new monitorable: " + monitorable.getName());
		}

		@Override
		public void removedMonitorable(Monitorable monitorable, List<Meter> meters) {
			super.removedMonitorable(monitorable, meters);
			List<Meter> meterList = _rateMeters.get(monitorable);
			if (meterList != null){
				meterList.forEach(mtr -> {
					stopJob( monitorable, mtr);
				});
			}
		}

		@Override
		public void addedMeter(Monitorable monitorable, Meter meter) {
			super.addedMeter(monitorable, meter);
			if (!Meters.matches(meter, "rate") && _metersNames.contains(meter.getName())){
				Meter createdMeter = startJob(_meteringService, monitorable, meter.getName());
				if (createdMeter != null){
					_rateMeters.putIfAbsent(monitorable, new ArrayList<Meter>());
					_rateMeters.get(monitorable).add(createdMeter);
					monitorable.getMeters ().put (createdMeter.getName(), createdMeter);
					monitorable.updated ();
				}
			}
		}

		@Override
		public void removedMeter(Monitorable monitorable, Meter meter) {
			super.removedMeter(monitorable, meter);
			stopJob( monitorable, meter);
		}
		
		Meter startJob(MeteringService metering, Monitorable monitorable, String meterName){
			 Meter target = monitorable.getMeters().get(meterName);
		      if (target == null) {
		        return null;
		      }
		      Meter _meter = Meters.createRateMeter(metering, target, _period);
		      Meter existing = monitorable.getMeters().get(_meter.getName());
		      if (existing != null) {
		        Meters.stopRateMeter(_meter);
		        _meter = existing;
		      } else
		        monitorable.getMeters().put(_meter.getName(), _meter);
		      return _meter;
		}
		
		protected void stopJob(Monitorable monitorable, Meter meter) {
		      Meters.stopRateMeter(meter);
		      monitorable.getMeters().remove(meter.getName());
		    }
	}

	private class MaxValueEntry extends MeteringRegistry.MetersTracker implements Entry{

		String _monName;
		boolean _useMonsPattern;
		String _monsPattern;
		List<String> _metersNames;
		Object _tracking;
		long _scheduled = -1L;
		boolean _started;
		
		public MaxValueEntry(String monName, String monsPattern, List<String> metersNames, 
				String metersPattern, String schedule) {
			super(metersPattern, false, true, false);
			try {
				if (schedule != null)
		        _scheduled = Long.parseLong(schedule);
			} catch (Throwable t) {
				throw new IllegalArgumentException("Invalid scheduled declaration for MaxValueEntry meter");
			}
			_monName = monName;
			_metersNames = metersNames;
//			_metersPattern = metersPattern;
			_useMonsPattern = monsPattern != null;
			if (_useMonsPattern) _monsPattern = monsPattern;
		}

		public void start() {
			if (_started)
				return;
			if (LOGGER.isDebugEnabled())
				LOGGER.debug(this + " : start");
			_started = true;
			if (_useMonsPattern){
				_tracking = _meteringRegistry.trackMonitorables(_monsPattern, this, _serial);
			}
			else {
				_tracking = _meteringRegistry.trackMonitorable(_monName, this, _serial);
			}
		}

		public void stop() {
			if (LOGGER.isDebugEnabled())
				LOGGER.debug(this + " : stop");
			_meteringRegistry.stopTracking(_tracking, null);			
		}

		@Override
		public void addedMeter(Monitorable monitorable, Meter meter) {
			super.addedMeter(monitorable, meter);
			boolean matchPattern = _metersNames.contains(meter.getName());
			if (!Meters.matches(meter, "max") && matchPattern){
				Meter createdMeter = null;
				
				if (_scheduled == -1L) {
					createdMeter = Meters.createMaxValueMeter(_meteringService, meter);
				} else {
					createdMeter = Meters.createScheduledMaxValueMeter(_meteringService, meter, _scheduled, 0);
				}
				Meter existing = monitorable.getMeters().get(createdMeter.getName());
				if (existing != null) {
					// !! already existing
					Meters.stopMaxValueMeter(createdMeter);
					createdMeter = existing;
				} else {
					monitorable.getMeters().put(createdMeter.getName(), createdMeter);
					monitorable.updated ();
				}
			}
		}
		
		@Override
		public void removedMonitorable(Monitorable monitorable, List<Meter> meters) {
			super.removedMonitorable(monitorable, meters);
			if (monitorable == _monitorable) return;
		    if (LOGGER.isDebugEnabled ()) LOGGER.debug (this+" : removedMonitorable : "+monitorable.getName ());
		    for (Meter meter : meters) removedMeter (monitorable, meter);
		}
		
		@Override
		public void removedMeter(Monitorable monitorable, Meter meter) {
			super.removedMeter(monitorable, meter);
			Meters.stopMaxValueMeter(meter);
			monitorable.getMeters().remove(meter.getName());
		}
	}
	
	private class MovingMaxValueEntry extends MeteringRegistry.MetersTracker implements Entry {

		String _monName, _monsPattern, _name;
		boolean _useMonsPattern;
		List<String> _metersNames;
		Object _tracking;
		long _sampling = 1000L;
		int _samples = 5;
		boolean _started;
		
		public MovingMaxValueEntry(String monName, String monsPattern, List<String> metersNames, String metersPattern,
				String samples,  String sampling, String name) {
			super(metersPattern, false, true, false);
			try {
				if (samples != null)
					_samples = Integer.parseInt(samples);
				if (samples != null)
					_sampling = Long.parseLong(sampling);
				if (name != null)
					_name = name;
			} catch (Throwable t) {
				throw new IllegalArgumentException("Invalid scheduled declaration for MaxValueEntry meter");
			}
			_monName = monName;
			_metersNames = metersNames;
			_useMonsPattern = monsPattern != null;
			if (_useMonsPattern) _monsPattern = monsPattern;
		}

		public void start() {
			if (_started)
				return;
			if (LOGGER.isDebugEnabled())
				LOGGER.debug(this + " : start");
			_started = true;
			if (_useMonsPattern){
				_tracking = _meteringRegistry.trackMonitorables(_monsPattern, this, _serial);
			}
			else {
				_tracking = _meteringRegistry.trackMonitorable(_monName, this, _serial);
			}
		}

		public void stop() {
			if (LOGGER.isDebugEnabled())
				LOGGER.debug(this + " : stop");
			_meteringRegistry.stopTracking(_tracking, null);			
		}
		
		@Override
		public void addedMeter(Monitorable monitorable, Meter meter) {
			super.addedMeter(monitorable, meter);
			String givenName = (_name != null)? _name : meter.getName().concat(".mov");
			boolean alreadyExisting = !meter.getName().contains(".mov");
			boolean existingGivenName = (monitorable.getMeters().containsKey(givenName));
			boolean matchPattern = _metersNames.contains(meter.getName());
			if (matchPattern && !existingGivenName && !alreadyExisting){
				Meter createdMeter = Meters.createMovingMaxValueMeter(_meteringService, givenName, meter, _sampling, _samples);
				Meter existing = monitorable.getMeters().get(createdMeter.getName());
				if (existing != null) {
					// !! already existing
					Meters.stopMaxValueMeter(createdMeter);
					createdMeter = existing;
				} else {
					monitorable.getMeters().put(createdMeter.getName(), createdMeter);
				}
			}
		}
		
		@Override
		public void removedMeter(Monitorable monitorable, Meter meter) {
			super.removedMeter(monitorable, meter);
			Meters.stopMaxValueMeter(meter);
			monitorable.getMeters().remove(meter.getName());
		}
		
		@Override
		public void removedMonitorable(Monitorable monitorable, List<Meter> meters) {
			super.removedMonitorable(monitorable, meters);
			if (monitorable == _monitorable) return;
		    if (LOGGER.isDebugEnabled ()) LOGGER.debug (this+" : removedMonitorable : "+monitorable.getName ());
		    for (Meter meter : meters) removedMeter (monitorable, meter);
		}
	}
}
