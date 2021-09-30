package com.nextenso.proxylet.engine;

import static com.alcatel.as.util.config.ConfigConstants.COMPONENT_ID;
import static com.alcatel.as.util.config.ConfigConstants.COMPONENT_NAME;
import static com.alcatel.as.util.config.ConfigConstants.GROUP_NAME;
import static com.alcatel.as.util.config.ConfigConstants.HOST_NAME;
import static com.alcatel.as.util.config.ConfigConstants.INSTANCE_ID;
import static com.alcatel.as.util.config.ConfigConstants.INSTANCE_NAME;
import static com.alcatel.as.util.config.ConfigConstants.INSTANCE_PID;
import static com.alcatel.as.util.config.ConfigConstants.PLATFORM_ID;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.utils.Enumerations;

import com.alcatel.as.util.config.ConfigHelper;
import com.nextenso.proxylet.Proxylet;
import com.nextenso.proxylet.ProxyletConfig;
import com.nextenso.proxylet.ProxyletContext;
import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.engine.criterion.Criterion;
import com.nextenso.proxylet.engine.criterion.CriterionWrapper;
import com.nextenso.proxylet.event.ProxyletConfigEvent;
import com.nextenso.proxylet.event.ProxyletConfigListener;

/**
 * This class encapsulates an environment to run a Proxylet, mainly: - the Proxylet Object - the
 * Proxylet name - the Proxylet description - the Proxylet Criterion Object - the Proxylet
 * Config (this implements it) - the Proxylet Context the Proxylet belongs to
 */
public class ProxyletEnv implements ProxyletConfig {
  private final static Logger LOGGER = Logger.getLogger("callout");
  
  // Config parameters
  private Dictionary _properties;
  
  // System Config parameters
  private Dictionary _systemProperties;
  
  // hardcoded params from protocol.xml
  private Dictionary _params = new Hashtable();
  
  // the Config listeners
  private List<ProxyletConfigListener> _listeners = new ArrayList<ProxyletConfigListener>();
  
  // the Proxylet name and description
  private String _proxyletName, _proxyletDesc = "";
  
  // the Context this Proxylet belongs to
  private Context _context;
  
  // the Proxylet Criterion
  private CriterionWrapper _criterion;
  
  // the Proxylet
  private Proxylet _proxylet;
  
  private int _id;
  
  // the proxy application
  private ProxyAppEnv _proxyAppEnv;
  
  public ProxyletEnv() {
  }
  
  public void init() throws ProxyletException {
    _proxylet.init(this);
    String name = _proxyletName + " (" + _proxylet.getProxyletInfo() + ")";
    _context.log("Initialized: " + name + " with criterion: " + getCriterion());
  }
  
  public void destroy() {
    String name = _proxyletName + " (" + _proxylet.getProxyletInfo() + ")";
    _context.log("Destroying: " + name);
    try {
      _proxylet.destroy();
    } catch (Throwable t) {
      _context.log("Exception while destroying " + name, t);
    }
  }
  
  public void setId(int id) {
    _id = id;
  }
  
  public int getId() {
    return _id;
  }
  
  /** deprecated ? */
  @SuppressWarnings("unchecked")
  public void addParameter(String name, String value) {
    if (name == null)
      return;
    if (value != null)
      _params.put(name, value);
    else
      _params.remove(name);
  }
  
  @SuppressWarnings("unchecked")
  public void setProperties(Dictionary properties) {
    _properties = properties;
  }
  
  @SuppressWarnings({ "unchecked", "serial" })
  public void setSystemProperties(final Dictionary systemProperties) {
    _systemProperties = new Hashtable() {
      {
        put(ProxyletConstants.PARAM_NAME_APP_NAME, systemProperties.get(COMPONENT_NAME));
        put(ProxyletConstants.PARAM_NAME_APP_ID, systemProperties.get(COMPONENT_ID));
        put(ProxyletConstants.PARAM_NAME_APP_PID, systemProperties.get(INSTANCE_PID));
        put(ProxyletConstants.PARAM_NAME_APP_INSTANCE, systemProperties.get(INSTANCE_NAME));
        put(ProxyletConstants.PARAM_NAME_APP_HOST, systemProperties.get(HOST_NAME));
        put(ProxyletConstants.PARAM_NAME_APP_GROUP, systemProperties.get(GROUP_NAME));
        put(ProxyletConstants.PARAM_NAME_APP_PLATFORM_ID, systemProperties.get(PLATFORM_ID));
        put(ProxyletConstants.PARAM_NAME_APP_INSTANCE_ID, systemProperties.get(INSTANCE_ID));
      }
    };
  }
  
  @SuppressWarnings("unchecked")
  public void updateProperties(String[] propNames) {
    synchronized (_listeners) {
      int size = _listeners.size();
      if (size == 0)
        return;
      
      ProxyletConfigEvent event = new ProxyletConfigEvent(this, propNames);
      for (ProxyletConfigListener listener : _listeners) {
        try {
          listener.configEvent(event);
        } catch (Throwable t) {
          LOGGER.error("Unexpected exception while reconfiguring proxylet " + _proxyletName, t);
        }
      }
    }
  }
  
  public void setContext(Context context) {
    _context = context;
  }
  
  public Context getContext() {
    return _context;
  }
  
  public String getProxyletDescription() {
    return _proxyletDesc;
  }
  
  public void setProxyletDescription(String desc) {
    _proxyletDesc = desc;
  }
  
  public void setProxyletName(String name) {
    _proxyletName = name;
  }
  
  public CriterionWrapper getCriterionWrapper() {
    return _criterion;
  }
  
  public Criterion getCriterion() {
    return _criterion.getCriterion();
  }
  
  public void setCriterionWrapper(CriterionWrapper criterion) {
    _criterion = criterion;
  }
  
  public Proxylet getProxylet() {
    return _proxylet;
  }
  
  @SuppressWarnings("unchecked")
  public Class getProxyletClass() {
    return ProxyletInvocationHandler.getProxyletClass(_proxylet);
  }
  
  public void setProxylet(Proxylet proxylet) {
    _proxylet = proxylet;
  }
  
  /********************************************
   * Implementation of ProxyletConfig
   ********************************************/
  
  public String getProxyletName() {
    return _proxyletName;
  }
  
  public ProxyletContext getProxyletContext() {
    return _context;
  }
  
  public String getStringParameter(String name) throws ProxyletException {
    
    String o = ConfigHelper.getString(_params, name, null);
    if (o == null)
      o = ConfigHelper.getString(_properties, name, null);
    if (o == null)
      o = ConfigHelper.getString(_systemProperties, name, null);
    if (o == null)
      throw new ProxyletException("Null parameter value for: " + name);
    return o;
  }
  
  public String getStringParameter(String name, String def) {
    String val = ConfigHelper.getString(_params, name, null);
    if (val == null)
      val = ConfigHelper.getString(_properties, name, null);
    if (val == null)
      val = ConfigHelper.getString(_systemProperties, name, def);
    return val;
  }
  
  public int getIntParameter(String name) throws ProxyletException {
    Object o = _params.get(name);
    if (o == null)
      o = _properties.get(name);
    if (o == null)
      o = _systemProperties.get(name);
    if (o == null)
      throw new ProxyletException("Null parameter value for: " + name);
    try {
      return Integer.parseInt(o.toString());
    } catch (NumberFormatException e) {
      throw new ProxyletException("Invalid parameter value for: " + name);
    }
  }
  
  public int getIntParameter(String name, int def) {
    try {
      return getIntParameter(name);
    } catch (ProxyletException e) {
      return def;
    }
  }
  
  public boolean getBooleanParameter(String name) throws ProxyletException {
    Object o = _params.get(name);
    if (o == null)
      o = _properties.get(name);
    if (o == null)
      o = _systemProperties.get(name);
    if (o == null)
      throw new ProxyletException("Null parameter value for: " + name);
    return (o.toString().equalsIgnoreCase("true"));
  }
  
  public boolean getBooleanParameter(String name, boolean def) {
    try {
      return getBooleanParameter(name);
    } catch (ProxyletException e) {
      return def;
    }
  }
  
  public Enumeration getParameterNames() {
    return new Enumerations(_params.keys(), _properties.keys(), _systemProperties.keys());
  }
  
  public void registerProxyletConfigListener(ProxyletConfigListener listener) {
    // Decorate the listener with our dynamic proxy, which sets proper class loader.
    ProxyletConfigListener l = (ProxyletConfigListener) ProxyletInvocationHandler.newInstance(listener
        .getClass().getClassLoader(), listener);
    synchronized (_listeners) {
      _listeners.add(l);
    }
  }
  
  public void deregisterProxyletConfigListener(ProxyletConfigListener listener) {
    synchronized (_listeners) {
      _listeners.remove(listener);
    }
  }
  
  /**
   * Sets the proxy application environment this proxylet belongs to.
   */
  public void setProxyAppEnv(ProxyAppEnv env) {
    _proxyAppEnv = env;
  }
  
  /**
   * Gets the name of the proxy application this proxylet belongs to.
   */
  public ProxyAppEnv getProxyAppEnv() {
    return _proxyAppEnv;
  }
  
}
