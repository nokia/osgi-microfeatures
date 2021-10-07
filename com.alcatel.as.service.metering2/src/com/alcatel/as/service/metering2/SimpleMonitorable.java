package com.alcatel.as.service.metering2;

import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.alcatel.as.service.metering2.util.SerialExecutor;


/**
 * Abstract class implementing a {@link Monitorable} interface.
 */
public class SimpleMonitorable implements Monitorable {  
  /**
   * Our Monitorable service name.
   */
  private String _name;
  
  /**
   * The Monitorable {@link Description#}
   */
  private volatile String _description;
  
  /**
   * The concurrent map holding all the meters managed by this Monitorable service.
   */
  protected final ConcurrentMap<String, Meter> _meters = new ConcurrentHashMap<>();
  
  /**
   * The object representing the registration for our Monitorable service.
   */
  private volatile ServiceRegistration<?> _registration;
  
  /**
   * Are we registered
   */
  private volatile boolean _registered;
  
  /**
   * Used to allow safe updates, especially when the monitorable service is being registered. 
   */
  private final SerialExecutor _serial = new SerialExecutor();
  
  /**
   * Creates a new Monitorable service.
   * @param monitorableName the name of the monitorable service that will be registered in the OSGI registry 
   * @param monitorableDesc the monitorable description
   */
  public SimpleMonitorable(String monitorableName, String monitorableDesc) {
    _name = monitorableName;
    _description = monitorableDesc;
  }
  
  /**
   * Returns the monitorable service description.
   * @return The monitorable service description
   */
  @Override
  public String getDescription() {
    return _description;
  }
  /**
   * Sets the monitorable service description.
   * @param description The monitorable service description
   */
  public void setDescription (String description){
      _description = description;
  }
  
  /**
   * Returns the monitorable service meters.
   * @return The monitorable service meters
   */
  @Override
  public ConcurrentMap<String, Meter> getMeters() {
    return _meters;
  }
  
  /**
   * Returns a given meter managed by this monitorable service.
   */
  public Meter getMeter(String name) {
    return _meters.get(name);
  }
  
  /**
   * Returns the Monitorable service name.
   */
  public String getName() {
    return _name;
  }
  
  /**
   * Creates a {@link Meter} for measuring an absolute value. 
   * @param name The meter name
   * @return A new {@link Meter} instance
   */
  public Meter createAbsoluteMeter(MeteringService service, String name) {
    Meter m = service.createAbsoluteMeter(name);
    _meters.put(name, m);
    return m;
  }
  
  /**
   * Creates a {@link Meter} for measuring an incremental value. 
   * @param service the MeteringService used to create the meter
   * @param name The meter name
   * @param parent an incremental parent meter, or null
   * @return A new {@link Meter} instance
   */
  public Meter createIncrementalMeter(MeteringService service, String name, Meter parent) {
    Meter m = service.createIncrementalMeter(name, parent);
    _meters.put(name, m);
    return m;
  }
  
  /**
   * Creates a {@link Meter} for measuring a value supplied dynamcally by a java callback 
   * @param name The meter name
   * @param valueSupplier a java callback used to supply the actual meter value
   * @return A new {@link Meter} instance
   */
  public Meter createValueSuppliedMeter(MeteringService service, String name, ValueSupplier valueSupplier) {
    Meter m = service.createValueSuppliedMeter(name, valueSupplier);
    _meters.put(name, m);
    return m;
  }
  
  /**
   * Adds some meters to the current list of meters managed by this Monitorable service.
   * 
   * @param meters the list of meters to add in this Monitorable
   */
  public SimpleMonitorable addMeter(Meter ... meters) {
    for (Meter meter : meters) {
      _meters.put(meter.getName(), meter);
    }
    return this;
  }
  
  /**
   * Adds some meters to the current list of meters managed by this Monitorable service, and update the monitorable service
   * properties.
   * 
   * @param meters the list of meters to remove from this Monitorable
   */
  public SimpleMonitorable removeMeter(Meter ... meters) {
    for (Meter meter : meters) {
      _meters.remove(meter.getName(), meter);
      meter.stopAllJobs();
    }
    return this;
  }
  
  public void start(final BundleContext bc) {
    // serialize service registration and service update, and order to be able to handle updates that are triggered
    // when the service is registering.
    _serial.execute(new Runnable() {
      public void run() {
        if (!_registered) {
          Hashtable<String, Object> props = new Hashtable<>();
          props.put(Monitorable.NAME, _name);
          _registration = bc.registerService(Monitorable.class.getName(), SimpleMonitorable.this, props);
          _registered = true;
        }
      }
    });
  }
  
  public void stop() {
    _serial.execute(new Runnable() {
      public void run() {
        if (_registered) {
          try {      
            _registration.unregister();
            _registration = null;
          } catch (Throwable t) {
            // our bundle may be stopped.
            _registration = null;
            _registered = false;
          }
        }
        for (Meter m : _meters.values()) {
            m.stopAllJobs();
          }
        _meters.clear();                      
      }
    });
  }
  
  /**
   * Update the service properties associated to this Monitorable service, in order to notify other listeners
   * @param added if some meters have been added, false if removed
   * @param meters the list of added (or removed) meters
   */
  public SimpleMonitorable updated() {
    // serialize service registration and service update ! This allows to schedule an update while the monitorable 
    // is being registered.
    _serial.execute(new Runnable() {
      public void run() {
        if (_registered) {
          final Hashtable<String, Object> props = new Hashtable<>();
          props.put(Monitorable.NAME, _name);
          
          try {
            _registration.setProperties(props);
          } catch (Throwable t) {
            // our bundle seems stopped.
          }
        }
      }
    });
    return this;
  }
  /**
  * change monitorable name 
  */ 
  public void setName(String name) {
	if (_registered) throw new IllegalStateException("cannot change name when already started");
	_name=name;
  }
}
