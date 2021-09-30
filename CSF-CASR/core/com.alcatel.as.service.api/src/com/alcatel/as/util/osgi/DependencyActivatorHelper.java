package com.alcatel.as.util.osgi;

import static org.osgi.framework.Constants.BUNDLE_NAME;
import static org.osgi.framework.Constants.BUNDLE_VERSION;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.log4j.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * This class is based on the old DependencyManager API. Use DMHelper instead.
 *
 * Helper class for activators based on the DependencyManager framework.
 * Additional features include:
 * <ul>
 * <li> ServiceRegistry abstraction for dynamic registration of services
 * <li> easy accessors for bundle name, symbolicname and version
 * </ul>
 *
 * @deprecated use DMHelper instead of this class.
 */
public abstract class DependencyActivatorHelper extends DependencyActivatorBase implements
                                                                               ServiceRegistry
{
  protected BundleContext _bctx;

  protected DependencyManager _dmgr;

  protected final Logger _logger;
  protected Thread _depChecker;

  protected boolean doRegisterServiceRegistry() {
    return true;
  }
  
  public DependencyActivatorHelper(Logger logger)
  {
    _logger = logger;
  }

  @Override
@SuppressWarnings( { "unchecked", "serial" })
  public void init(final BundleContext ctx, final DependencyManager dmgr) throws Exception
  {
    _bctx = ctx;
    _dmgr = dmgr;

    if (doRegisterServiceRegistry()) {
      // register our activator as a proxy to the ServiceRegistry
      // so that bundle classes can call (un)registerService
      addService(createComponent().setInterface(ServiceRegistry.class.getName(), new Hashtable() {
        {
          put("name", DependencyActivatorHelper.this.getClass().getPackage().getName());
          put("version", getBundleVersion());
        }
      }).setImplementation(this).setCallbacks(null, null, null, null));
    }

    if (_logger.isDebugEnabled())
    {
      _logger.debug(_bctx.getBundle() + ".init(" + this.getClass().getPackage().getName() + ")");
    }
  }

  @Override
  public void destroy(BundleContext ctx, DependencyManager mgr) throws Exception
  {
    if (_logger.isDebugEnabled())
    {
      _logger.debug(_bctx.getBundle() + ".destroy()");
    }
  }

  @SuppressWarnings("unchecked")
  public void addService(Component service)
  {
    _dmgr.add(service);
  }

  /**
   * generic helper to register/update a service, in a way compatible with both SCR and
   * DependencyManager
   * 
   * @param previousReg a previously returned ServiceRegistration instance ('Object' used to avoid
   *          OSGi API dependency). If null, a new registration will be created, otherwise,
   *          updates the previously registered object
   * @param clazz the interface name to register
   * @param o the instance to register
   * @param props the associated white-board properties
   */
  @SuppressWarnings("unchecked")
  public Object registerService(Object previousReg, String clazz, Object o, Dictionary props)
  {
    if (_bctx == null)
      throw new IllegalStateException("Bundle is not active");

    if (previousReg != null && !(previousReg instanceof ServiceRegistration))
    {
      throw new IllegalArgumentException(previousReg + " is not an ServiceRegistration instance");
    }

    if (_logger.isDebugEnabled())
      _logger.debug("Bundle " + getBundleSymbolicName() + " registering new service " + clazz);

    ServiceRegistration newreg = _bctx.registerService(clazz, o, props);
    if (previousReg != null)
    {
      try
      {
        ((ServiceRegistration) previousReg).unregister();
      }
      catch (IllegalStateException e)
      {
        // service already unregistered: ignore
      }
    }

    return newreg;
  }

  public void unregisterService(Object registration)
  {
    if (registration == null)
    {
      return;
    }
    if (!(registration instanceof ServiceRegistration))
    {
      throw new IllegalArgumentException(registration
          + " is not an ServiceRegistration instance");
    }
    ((ServiceRegistration) registration).unregister();
  }

  public <S> Map<S, Map<String, Object>> lookupService(Class<S> clazz, String filter) throws Exception
  {
    Map<S, Map<String, Object>> res = new HashMap<S, Map<String, Object>>();
    for (ServiceReference<S> ref : _bctx.getServiceReferences(clazz, filter))
    {
      S service = _bctx.getService(ref);
      if (service != null)
      {
        Map<String, Object> props = new HashMap<String, Object>();
        for (String k : ref.getPropertyKeys()) props.put(k, ref.getProperty(k));
        res.put(service, props);
      }
    }
    return res;
  }
	public <S> List<ServiceRef<S>> lookupServices(Class<S> clazz, String filter) throws Exception
	{
    List<ServiceRef<S>> res = new ArrayList<ServiceRef<S>>();
    for (ServiceReference<S> ref : _bctx.getServiceReferences(clazz, filter))
    {
      final S service = _bctx.getService(ref);
      if (service != null)
      {
        final Map<String, Object> props = new HashMap<String, Object>();
        for (String k : ref.getPropertyKeys()) props.put(k, ref.getProperty(k));
        res.add(new ServiceRef<S>() {
					public S get() { return service; }
					public Map<String, Object> properties() { return props; }
				});
      }
    }
    return res;
	}

  public String getBundleName()
  {
    String name = (String) _bctx.getBundle().getHeaders().get(BUNDLE_NAME);
    return (name != null ? name : _bctx.getBundle().getSymbolicName());
  }

  public String getBundleSymbolicName()
  {
    return _bctx.getBundle().getSymbolicName();
  }

  public String getBundleVersion()
  {
    String version = (String) _bctx.getBundle().getHeaders().get(BUNDLE_VERSION);
    return (version != null ? version : "0.0.0");
  }

  public Bundle getBundle()
  {
    return _bctx.getBundle();
  }

  public BundleContext getBundleContext()
  {
    return _bctx;
  }

  public DependencyManager getDependencyManager()
  {
    return _dmgr;
  }
  
  public Component createService() {
	  return _dmgr.createComponent();
  }
}
