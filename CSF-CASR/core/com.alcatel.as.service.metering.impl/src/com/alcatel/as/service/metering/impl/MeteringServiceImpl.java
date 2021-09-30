package com.alcatel.as.service.metering.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.alcatel.as.service.bundleinstaller.BundleInstaller;
import com.alcatel.as.service.metering.Counter;
import com.alcatel.as.service.metering.Gauge;
import com.alcatel.as.service.metering.Meter;
import com.alcatel.as.service.metering.MeterListener;
import com.alcatel.as.service.metering.MeteringConstants;
import com.alcatel.as.service.metering.MeteringService;
import com.alcatel.as.service.metering.Rate;

/**
 * This is the OSGi Metering Service. We provide two interfaces into the OSGi registry: 
 * 1) the MeteringService interface: this is the entry point in the Metering Service API 
 * 2) the EventHandler interface: we provide this interface in order to be notified when log4j updates
 *    the loggers.
 */
@Component(name = "metering", configurationPolicy = ConfigurationPolicy.OPTIONAL, immediate = true,
           service = { EventHandler.class },
           property = { "event.topics=||com/alcatel_lucent/as/service/agent/logging/LOGCONFIG_EVENT" })
public class MeteringServiceImpl implements MeteringService, EventHandler {
  /** Our Logger */
  private final static Logger _logger = Logger.getLogger("as.service.metering.MeteringServiceImpl");
  
  /** The bundle context used when registering Meters into the OSGi registry (null when running outside OSGi) */
  private volatile BundleContext _bctx;
  
  /** Our meters */
  private Map<String, Meter> _meters = new HashMap<String, Meter>();
  
  /** Our default stat sampler */
  private DefaultStatSampler _sampler;
  
  /** The thread pool used when registering meters into the OSGi registry (needed to avoid dead locks) */
  private ThreadPoolExecutor _tpool;
  
  /** Object used to retrieve the current time in millis. */
  private final Time _time;
  
  /** Flag telling if all meters have to be registered in the OSGi registry */
  private final static boolean _registerMeters = Boolean.getBoolean("metering.registerMeters");
  
  /** Flag telling if our service has been started or not. */
  protected boolean _started;
  
  /** Number of expected meter listeners before our service can start */
  protected volatile int _expectedListeners;
  
  /** Is our MeteringService registered in the OSGi service registry ? */
  private final AtomicBoolean _isRegistered = new AtomicBoolean();
  
  /** Registered listeners */
  private Map<String, List<MeterListener>> _listeners = new HashMap();
  
  /** List of listeners bound before our start method */
  private final List<Listener> _pendingListeners = new ArrayList();

  /** List of stateful meters configured using {@link MeteringConstants#STATEFUL_METER} */
  private Set<String> _statefulMeters = new HashSet();
  
  /** Factory internally used by this class when creating meters. */
  static interface MeterFactory {
    Meter create(String name);
  }
  
  /** Rate Factory. */
  final MeterFactory _rateFactory = new MeterFactory() {
    public Meter create(String name) {
      boolean stateful = _statefulMeters.contains(name);
      return stateful ? new StatefulRateImpl(name) : new RateImpl(name);
    }
  };
  
  /** Counter Factory. */
  final MeterFactory _counterFactory = new MeterFactory() {
    public Meter create(String name) {
      boolean stateful = _statefulMeters.contains(name);
      return stateful ? new StatefulCounterImpl(name) : new CounterImpl(name);
    }
  };
  
  /** Gauge Factory. */
  final MeterFactory _gaugeFactory = new MeterFactory() {
    public Meter create(String name) {
      boolean stateful = _statefulMeters.contains(name);
      return stateful ? new StatefulGaugeImpl(name) : new GaugeImpl(name);
    }
  };
  
  /** A meter listener */
  static class Listener {
    final String meterName;
    final MeterListener meterListener;
    
    Listener(MeterListener listener, String meterName) {
      this.meterName = meterName;
      this.meterListener = listener;
    }
    
    @Override
    public String toString() {
      return meterListener + "(" + meterName + ")";
    }
  }
  
  /**
   * Creates a MeteringService implementation (called from OSGi).
   */
  public MeteringServiceImpl() {
    this(new TimeImpl());
  }
  
  /**
   * Creates a MeteringService implementation (called from Junit).
   * @param time the object used to retrieve the current time in millis.
   */
  public MeteringServiceImpl(Time time) {
    _time = time;
  }
  
  /**
   * Because we need to inspect all started bundles, we wait for the bundle installer, which is registered
   * after all bundles are started.
   */
  @Reference
  public void bindBundleInstaller(BundleInstaller bundleInstaller) {
  }
  
  /**
   * Register a meter listener.
   */
  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  public void bindMeterListener(final MeterListener listener, final Map<String, String> props) {
    final String name = props.get(MeteringConstants.METER_NAME);
    if (name == null) {
      _logger.warn("Ignoring bound meter listener without a meter name: " + listener);
      return;
    }
    
    // Create a holder object for this new listener.
    Listener l = new Listener(listener, name);
    
    // Check if we are started.
    synchronized (this) {
      if (!_started) {
        // Store the listener in a temporary list: we'll handle it when started.
        _pendingListeners.add(l);
        return;
      }
    }
    
    // Store the new meter listener, and register it in an existin meter, if any.
    _bindMeterListener(l);
    
    // Register our metering service if all expected listeners are bound.
    tryRegisterMeteringService();
  }

  public void unbindMeterListener(final MeterListener listener, final Map<String, String> props) {
  }
    
  /**
   * Starts the Metering Service.
   * @param config the metering service configuration
   * @param ctx the component context used to retrieve the bundle context (null when running outside OSGi).
   */
  @Activate
  public void start(final Map<?, ?> conf, final ComponentContext cctx) {
    if (_logger.isDebugEnabled()) {
      _logger.debug("Starting metering service with configuration: " + conf);
    }
    
    Dictionary<?, ?> dict = new Map2Dictionary(conf);
    _tpool = new ThreadPoolExecutor(0, 1, 1000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
    CounterImpl.setTime(_time);
    _sampler = new DefaultStatSampler(_time);
    if (conf != null) {
      _sampler.updated(dict);
    }
    //_sampler.start();
    
    if (cctx != null) {
      _bctx = cctx.getBundleContext();
      // See of some started bundles contains some specific MeteringService headers.
      scanBundlesManifest(_bctx);
    }
    
    synchronized (this) {
      // Register listeners bound before our start method.
      for (Listener l : _pendingListeners) {
        _bindMeterListener(l);
      }
      _pendingListeners.clear();
      
      // Mark the started flag.
      _started = true;
    }
    
    // Register our metering service if all expected listeners are bound.
    tryRegisterMeteringService();
  }
  
  private void tryRegisterMeteringService() {
    boolean registerMeteringService = false;
    
    synchronized (this) {
      if (_bctx != null && _listeners.size() >= _expectedListeners) {
        registerMeteringService = true;
      }
    }
    
    if (registerMeteringService && _isRegistered.compareAndSet(false, true)) {
      if (_logger.isInfoEnabled()) {
        _logger.info("All expected listener bound (" + _expectedListeners + "): register Metering Service");
      }
      _bctx.registerService(MeteringService.class.getName(), this, null);
    }
  }
  
  /**
   * Updates the configuration (invoked by Declarative Service, under OSGi)
   * @param config
   */
  @Modified
  public void updated(Map<String, String> config) {
    if (_logger.isDebugEnabled()) {
      _logger.debug("Updating configuration ... " + config);
    }
    _sampler.updated(new Map2Dictionary(config));
  }
  
  /**
   * Stops the metering service (invoked by Declarative Service, under OSGi)
   */
  @Deactivate
  public void stop() {
    if (_logger.isDebugEnabled()) {
      _logger.debug("Stopping ...");
    }
    _sampler.stop();
    _tpool.shutdown();
  }
  
  public Gauge getGauge(String name) {
    return getMeter(name, "gauge.name", Gauge.class, _gaugeFactory);
  }
  
  public Counter getCounter(String name) {
    return getMeter(name, "counter.name", Counter.class, _counterFactory);
  }
  
  public Rate getRate(String name) {
    return getMeter(name, "rate.name", Rate.class, _rateFactory);
  }
  
  /**
   * Handles an OSGi event. We expect here an event from the log4j config admin, which
   * notifies us when log4j configuration has been reinitialized.
   */
  @Override
  public void handleEvent(Event event) {
    _logger.info("Got OSGi event: " + event);
    reload();
  }
  
  /**
   * Reload meters into our log4j meter listener.
   */
  private synchronized void reload() {
    _sampler.clearMeters();
    for (Meter meter : _meters.values()) {
      if (_logger.isDebugEnabled()) {
        _logger.debug("Reloading meter: " + meter.getName());
      }
      _sampler.addMeter(meter);
    }
    _logger.info("Metering Service reloaded");
  }
  
  /**
   * Scan all started bundles and check if they contain some specific
   * metering OSGi manifest headers. At the point this method is called, we know that
   * all bundles have been started because we have been injected with the BundleInstaller,
   * which is registered only once all bundles have been started.
   * 
   * @param ctx the metering service bundle context used to get the list of active bundles.
   * @see MeteringConstants#METER_LISTENERS
   * @see MeteringConstants#STATEFUL_METER
   */
  private void scanBundlesManifest(BundleContext ctx) {
    _expectedListeners = 0;
    for (Bundle b : ctx.getBundles()) {
      if (b.getState() == Bundle.ACTIVE) {
        Dictionary<String, String> headers = b.getHeaders();
        
        // Check If this bundle indicates the number of meter listeners it will register.
        String listeners = headers.get(MeteringConstants.METER_LISTENERS);
        if (listeners != null) {
          _expectedListeners += Integer.valueOf(listeners);
        }
        
        // Check if this bundle needs some meters to be stateful.
        String statefulMeters = headers.get(MeteringConstants.STATEFUL_METER);
        if (statefulMeters != null) {
          String[] meterNames = statefulMeters.trim().split(",");
          for (String meterName : meterNames) {
            _statefulMeters.add(meterName);
          }          
        }
      }
    }
    if (_logger.isInfoEnabled()) {
      _logger.info("Found expected meter listeners: " + _expectedListeners);
      _logger.info("Found stateful meters: " + _statefulMeters);
    }
  }
  
  /**
   * Register a meter listener. When this method is called, our start method has already been called.
   * @param l the new listener.
   */
  private void _bindMeterListener(Listener l) {    
    synchronized (this) {
      if (_logger.isDebugEnabled()) _logger.debug("Bound meter listener " + l.meterListener);
      
      // Register this new listener.
      List<MeterListener> list = _listeners.get(l.meterName);
      if (list == null) {
        list = new ArrayList<MeterListener>();
        _listeners.put(l.meterName, list);
      }
      list.add(l.meterListener);
      
      // Check if an existing meter already exists for this new listener.
      Meter m = _meters.get(l.meterName);
      
      // If we found an existing meter for the listener, register the listener in the meter.
      if (m != null) {
        if (_logger.isDebugEnabled()) _logger.debug("Registering meter listener " + l.meterListener + " in meter " + l.meterName);
        m.addMeterListener(l.meterListener);
      }
    }
  }
  
  @SuppressWarnings("rawtypes")
  private <T> T getMeter(String name, String servicePropName, Class clazz, MeterFactory factory) {
    Meter m = null;
    
    synchronized (this) {
      m = _meters.get(name);
      if (m == null) {
        m = factory.create(name);
        _meters.put(name, m);
        _sampler.addMeter(m);
        
        // Check if there are some listeners available for this new meter.
        List<MeterListener> listeners = _listeners.get(name);
        if (listeners != null) {
          for (MeterListener listener : listeners) {
            if (_logger.isInfoEnabled()) {
              _logger.info("Binding listener " + listener + " to meter " + m);
            }
            m.addMeterListener(listener);
          }
        }
        
        if (_bctx != null && _registerMeters) {
          // Register this counter using a thread poolin order to avoid deadlocks.
          Dictionary<String, String> props = new Hashtable<String, String>();
          props.put(servicePropName, name);
          registerMeter(clazz.getName(), m, props);
        }
      }
    }
    
    return (T) m;
  }
  
  /**
   * Schedules a Meter registration into the OSGi registry. We start a thread in order to
   * avoid any potential dead locks.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void registerMeter(final String serviceName, final Object meter, final Dictionary props) {
    try {
      _tpool.execute(new Runnable() {
        public void run() {
          try {
            _bctx.registerService(serviceName, meter, props);
          } catch (Throwable t) {
            _logger.error("Could not register meter " + meter + " into the OSGi registry", t);
          }
        }
      });
    } catch (Throwable t) {
      _logger.error("Could not register meter " + meter + " into the OSGi registry", t);
    }
  }
}
