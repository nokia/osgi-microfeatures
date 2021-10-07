package com.alcatel.as.service.discovery.impl.standalone;

import static com.alcatel.as.util.config.ConfigConstants.COMPONENT_ID;
import static com.alcatel.as.util.config.ConfigConstants.COMPONENT_NAME;
import static com.alcatel.as.util.config.ConfigConstants.GROUP_ID;
import static com.alcatel.as.util.config.ConfigConstants.GROUP_NAME;
import static com.alcatel.as.util.config.ConfigConstants.INSTANCE_ID;
import static com.alcatel.as.util.config.ConfigConstants.INSTANCE_NAME;
import static com.alcatel.as.util.config.ConfigConstants.PLATFORM_ID;
import static com.alcatel.as.util.config.ConfigConstants.PLATFORM_NAME;
import static com.alcatel.as.util.config.ConfigConstants.HOST_NAME;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.discovery.Advertisement;
import com.alcatel.as.util.config.ConfigHelper;

@Component
public class StandaloneAdvertisementTracker {
  private final static Logger _logger = Logger.getLogger(StandaloneAdvertisementTracker.class);
  private static String _hostName;
  
  static {
      try {
    	  InetAddress addr = InetAddress.getLocalHost();
    	  _hostName = addr.getHostName();
      } catch (UnknownHostException e) {
    	  _logger.error("Could not obtain local hostname, using localhost as default hostname.");
    	  _hostName = "localhost";
      }
  }
  
  private final DefProperty DEFAULT_CONSTANT[] = { 
      new DefProperty("provider", String.class, "standalone"),
      new DefProperty(PLATFORM_NAME, String.class, "asr"),
      new DefProperty(PLATFORM_ID, String.class, 1),
      new DefProperty(GROUP_NAME, String.class, null), 
      new DefProperty(GROUP_ID, String.class, 1),
      new DefProperty(COMPONENT_NAME, String.class, null), 
      new DefProperty(COMPONENT_ID, String.class, 1),
      new DefProperty(INSTANCE_NAME, String.class, null), 
      new DefProperty(INSTANCE_ID, Integer.class, 1),
      new DefProperty(HOST_NAME, String.class, _hostName), 
  };
  
  private class DefProperty {
    final String _name;
    final Class _type;
    final Object _default;
    
    DefProperty(String name, Class type, Object def) {
      _name = name;
      _type = type;
      _default = def;
    }
    
    public String toString() {
      return _name;
    }
    
    public Object getDefaultValue() {
      return _default;
    }
    
    public Object getValue() {
      if (_type == Integer.class) {
        return _default != null ? ConfigHelper.getInt(_system, _name, (Integer) _default) : ConfigHelper
            .getInt(_system, _name);
      } else if (_type == String.class) {
        return _default != null ? ConfigHelper.getString(_system, _name, _default.toString()) : ConfigHelper
            .getString(_system, _name);
      } else {
        throw new IllegalStateException("type unsupported: " + _type);
      }
    }
  }
  
  @Inject
  volatile BundleContext _bctx; // injected before optional dependency callbacks
  
  @ServiceDependency(filter = "(service.pid=system)")
  volatile Dictionary<String, String> _system;
  
  @Start
  void start() {
    // check if default properties are present in system properties
    for (DefProperty defProp : DEFAULT_CONSTANT) {
      if (defProp.getDefaultValue() == null &&_system.get(defProp._name) == null) {
        throw new IllegalArgumentException("Missing property " + defProp + " in system properties");
      }
    }
    _logger.info("started StandaloneAdvertisementTracker with system config: " + _system);
  }
  
  @ServiceDependency(required = false, filter = "(!(provider=*))")
  void bindLocalAdvert(Map<String, String> props, Advertisement advert) {
    Hashtable loopbackAdvert = new Hashtable(props);
    for (DefProperty defProp : DEFAULT_CONSTANT) {
      setIfNotExists(loopbackAdvert, defProp);
    }
    if (_logger.isInfoEnabled()) {
      _logger.info("bound local advert " + advert + " with props " + props + ". Registering loopback advert: " + loopbackAdvert);
    }
    _bctx.registerService(Advertisement.class.getName(), new Advertisement(advert.getIp(), advert.getPort()),
                          loopbackAdvert);
  }
  
  private void setIfNotExists(Dictionary props, DefProperty defProp) {
    if (props.get(defProp._name) == null) {
      Object value = defProp.getValue();
      props.put(defProp._name, value);
    }
  }
}
