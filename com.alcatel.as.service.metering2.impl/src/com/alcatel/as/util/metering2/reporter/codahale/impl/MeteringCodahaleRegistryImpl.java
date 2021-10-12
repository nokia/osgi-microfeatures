// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.util.metering2.reporter.codahale.impl;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.ServiceRegistration;

import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.Monitorable;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel.as.util.config.ConfigHelper;
import com.alcatel.as.util.metering2.reporter.codahale.MeteringCodahaleRegistry;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;

import alcatel.tess.hometop.gateways.utils.Log;

public class MeteringCodahaleRegistryImpl implements MeteringCodahaleRegistry {
  private final static Log _log = Log.getLogger(MeteringCodahaleRegistryImpl.class);

  private Map<String, MetricRegistry> _registries = new ConcurrentHashMap<>();
  
  /**
   * Our instance name.
   */
  private String _instance;
  
  /**
   * Our OSGi service registration (injected by DM)
   */
  private volatile ServiceRegistration<MeteringCodahaleRegistry> _registration;
  
  @Override
  public Map<String, MetricRegistry> getMeteringRegistries() {
	  return _registries;
  }
  
  public void updated(Dictionary<String, String> system) {
      _instance = ConfigHelper.getString(system, ConfigConstants.INSTANCE_NAME) + ".";
      _log.info("Configured group/Instance name=%s", _instance);
  }
  
  protected void start() {
  }
  
  protected void stop() {
    _log.info("Clearing all Codahale meters");
    _registries.values().forEach(reg -> {
    	for (Map.Entry<String, Metric> e : reg.getMetrics().entrySet()) {
          reg.remove(e.getKey());
        }
    });
  }
  
  // Called by DM (thread safe).
  final void added(Monitorable mon, Map<String, String> props) {
    String tempName = (String) props.get(Monitorable.NAME);
	String monitorableName = getJmxMonitorableName(tempName);
    _log.info("bound monitorable: %s", monitorableName);
    
    // Map the monitorable meters to our codahale meters registry.
    for (Meter meter : mon.getMeters().values()) {
      _log.debug("found meter %s from monitorable service %s", meter.getName(), monitorableName);
      registerMonitorableMeters(mon, monitorableName, meter);
    }
    
    // update our service, so other listeners will be able to see the change
    Hashtable<String, Object> properties = new Hashtable<String, Object>();
    properties.put(MeteringCodahaleRegistry.MONITORABLE_NAME, monitorableName);
    _registration.setProperties(properties);
  }
  
  // Called by DM (thread safe).
  final void changed(Monitorable mon, Map<String, ?> props) {
	String tempName = (String) props.get(Monitorable.NAME);
	String monitorableName = getJmxMonitorableName(tempName);
    _log.info("updating monitorable %s", monitorableName);
    
    MetricRegistry reg = _registries.get(monitorableName);
    if (reg != null) {
    	reg.getMetrics().keySet().forEach(metric -> {
    		 _log.debug("Removed meter %s from updated monitorable service %s", metric, monitorableName);
    		 reg.remove(metric);
    	});
    	_registries.remove(monitorableName);
    }
    
    // Listen to new meters
    mon.getMeters().forEach((meterName, addedMeter) -> {
    	if (reg != null) {
    		if (reg.getMetrics().get(getMeterName(meterName, monitorableName)) == null) {
    	        _log.debug("Added meter %s from updated monitorable service %s", addedMeter.getName(), monitorableName);
    	        registerMonitorableMeters(mon, monitorableName, addedMeter);
    		}
    	} else {
    		registerMonitorableMeters(mon, monitorableName, addedMeter);
    	}
    });
    
    // update our service, so other listeners will be able to see the change
    Hashtable<String, Object> properties = new Hashtable<String, Object>();
    properties.put(MeteringCodahaleRegistry.MONITORABLE_NAME, monitorableName);
    _registration.setProperties(properties);
  }
  
  // Called by DM (thread safe).
  final void removed(Monitorable mon, Map<String, ?> props) {
	String tempName = (String) props.get(Monitorable.NAME);
	String monitorableName = getJmxMonitorableName(tempName);
    _log.info("unbindMonitorable monitorable %s", monitorableName);
    
    MetricRegistry reg = _registries.get(monitorableName);
    if (reg != null) {
    	reg.getMetrics().keySet().forEach(metric -> reg.remove(metric));
    	_registries.remove(monitorableName);
    }
    	
    // update our service, so other listeners will be able to see the change
    Hashtable<String, Object> properties = new Hashtable<String, Object>();
    properties.put(MeteringCodahaleRegistry.MONITORABLE_NAME, monitorableName);
    _registration.setProperties(properties);
  }
  
  private String getMeterName(String meterName, String monitorableName) {
	  return new StringBuilder(_instance).append(monitorableName).append(".").append(meterName).toString();    
  }
  
  private String getJmxMonitorableName(String monitorable) {
	  return monitorable.replaceAll("_", ".")
			  			.replaceAll("\\:", ".")
					    .replaceAll("\\*", "star")
					    .replaceAll("\\.{2,}", ".");
  }
  
  private void registerMonitorableMeters(Monitorable mon, String monitorableName, final Meter m) {
    MetricRegistry reg = null;
	if (_registries.containsKey(monitorableName)) {
    	reg = _registries.get(monitorableName);
    } else {
    	reg = new MetricRegistry();
    	_registries.put(monitorableName, reg);
    }
	switch (m.getType()) {
    case ABSOLUTE:
    case SUPPLIED:
    case INCREMENTAL:
      reg.register(getMeterName(m.getName(), monitorableName), new Gauge<Long>() {
        public Long getValue() {
          try {
            return m.getValue();
          } catch (Throwable err) {
            _log.error("failed to get value from meter %s", err, m.getName());
            return 0L;
          }
        }
      });
      break;    
    }
  }
  
}
