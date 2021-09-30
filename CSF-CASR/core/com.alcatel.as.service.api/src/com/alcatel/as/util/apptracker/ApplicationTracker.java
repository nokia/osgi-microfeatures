package com.alcatel.as.util.apptracker;

import static org.osgi.framework.Constants.BUNDLE_VERSION;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.Component;
import org.apache.log4j.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;

import com.alcatel.as.util.cl.BundleClassLoader;

/**
 * The <code>ApplicationTracker</code> class simplifies the deployment of platform application
 * into callout's containers, using services from the OSGi Framework's service registry.
 * Note: this tracker only considers bundles with a file extension of .par, .sar or .war.
 * <p>
 * @author Arjun Panday
 */
@SuppressWarnings("unchecked")
public class ApplicationTracker implements SynchronousBundleListener, AppService {
  /**
   * the ApplicationListener is notified of application bundles started and is reponsible for
   * parsing them.
   */
  public interface Listener {
    /**
     * Parses an application bundle. 
     * If a valid application is found, it can be returned to be automatically registered to the OSGi 
     * service registry or manually by using the registerService method which provides more flexibility
     * (in the latter case the method should return null).
     * @param b the application's bundle
     * @param bcl the application's bundle classloader, useable to load the application code
     * @return in simple use cases, this method returns an object built using the informations 
     *         contained in the application's descriptor file(s) (typically a <protocol>.xml). 
     *         It should at least give access to the application's servlet classes to be 
     *         instanciated by the container and the classloader passed in argument. <br>
     *         In case multiple instances should be returned or more control is needed on the 
     *         registered service properties, registerService should be called for each discovered 
     *         application and this method should return null.
     */
    Object applicationDiscovered(Bundle b, ClassLoader bcl);
  }

  /**
   * Constructs a new ApplicationTracker
   * @param ctx the BundleContext that will be used for registering the AppService and the
   *          application objects
   * @param appServiceProperties a list of properties to characterize the AppService register in
   *          the white-board
   * @param lstn the Listener responsible for parsing the contents of an application bundle
   */
  public ApplicationTracker(BundleContext ctx, Dictionary appServiceProperties, Listener lstn) {
    this(ctx, null, appServiceProperties, lstn);
  }

  /**
   * Constructs a new ApplicationTracker
   * @param ctx the BundleContext that will be used for registering the AppService and the
   *          application objects
   * @param dm the DependencyManager used to register services (if null, we'll use the bundle
   *          context).
   * @param appServiceProperties a list of properties to characterize the AppService register in
   *          the white-board
   * @param lstn the Listener responsible for parsing the contents of an application bundle
   */
  public ApplicationTracker(BundleContext ctx,
			    DependencyManager dm,
			    Dictionary appServiceProperties,
			    Listener lstn)
  {

    _logger =
	Logger.getLogger("as.util.ApplicationTracker." + appServiceProperties);
    if (_logger.isDebugEnabled()) {
      _logger.debug("new ApplicationTracker[" + ctx.getBundle() + "] " + appServiceProperties);
    }
    _context = ctx;
    _dm = dm;
    _appSrvProps = appServiceProperties;
    _apps = new Hashtable<String, List>();
    _listener = lstn;
  }

  /**
   * start listening for applications
   */
  public void open() {
    if (_logger.isDebugEnabled())
      _logger.debug("ApplicationTracker opened");
    // lookup Apps already in the system
    for (Bundle b : _context.getBundles()) {
      if (b.getState() == Bundle.ACTIVE)
	loadApp(b, true);
    }
    // listens for new App events
    _context.addBundleListener(this);
    registerAppService();
  }

  /**
   * stop listening for applications
   */
  public void close() {
    _context.removeBundleListener(this);

    for (List l : _apps.values()) {
      for (Object o : l) {
	if (o instanceof ServiceRegistration) {
	  try {
	    ((ServiceRegistration) o).unregister();
	  } catch (IllegalStateException e) {
	  } // Already unregistered
	} else if (o instanceof Component) {
	  try {
	    _dm.remove((Component) o);
	  } catch (IllegalStateException e) {
	  } // Already unregistered
	}
      }
    }
    _apps.clear();

    if (_logger.isDebugEnabled()) {
      _logger.debug("ApplicationTracker closed");
    }
  }

  /**
   * Register the parsed application to the OSGi service registry.
   * This method should be called from the applicationDiscovered() implementation when a valid application is found.
   * This method assumes default method names for configuration callbacks ("bindConfig" and "updateConfig")
   * @param b the bundle containing the application
   * @param iname the interface of the service to be registered
   * @param ap the service implementation (the application object)
   * @param props properties to be registered along with the service
   */
  public void registerService(Bundle b, String iname, Object ap, Dictionary props) {
    registerService(b, iname, ap, props, null, null);
  }

  /**
   * Register the parsed application to the OSGi service registry.
   * This method should be called from the applicationDiscovered() implementation when a valid application is found.
   * @param b the bundle containing the application
   * @param iname the interface of the service to be registered
   * @param ap the service implementation (the application object)
   * @param props properties to be registered along with the service
   * @param bindConfig the name of the method in ap used to pass the configuration. 
   * @param updateConfig the name of the method in ap used to update the configuration 
   */
  public void registerService(Bundle b,
			      String iname,
			      Object ap,
			      Dictionary props,
			      String bindConfig,
			      String updateConfig)
  {
    if (ap == null)
      return;
    if (iname == null) {
      iname = ap.getClass().getName();
    }
    if (_logger.isDebugEnabled()) {
	_logger.debug("Registering Application for bundle " + b.getSymbolicName() + ": register new " + iname);
    }
    
    // Register and keep a pointer on the ServiceRegistration object for later unregister
    if (_dm == null) {
      addApps(getBundleKey(b), _context.registerService(iname, ap, props));
    } else {
      Component srv = _dm.createComponent()
      	.setInterface(iname, props)
      	.setImplementation(ap);

      boolean hasConfig =
		(b.getEntry("META-INF/mbeans-descriptors.xml") != null) ? true : false;

      if (hasConfig) {
	// Ensure that Jndi is correctly initialized, so we are sure that application will be able to get its configuration properly
	srv.add(_dm.createServiceDependency()
	        .setService(javax.naming.InitialContext.class)
	        //this dependency is not used, we just want to make 
	        //sure it is here
	        .setCallbacks(null, null, null)
	        .setAutoConfig(false)
	        .setRequired(true));
	
	/* This code is lascar only ... 
	String version = (String) b.getHeaders().get(BUNDLE_VERSION);
	if (version == null) {
	  version = "1.0.0";
	}
	/*
	srv.add(_dm.createServiceDependency()
	        .setService(java.util.Dictionary.class,
	                    "(& ("+BUNDLE_SYMBOLICNAME+"="
	                    + b.getSymbolicName() + ")"
	                    + "("+BUNDLE_VERSION+"=" + version + "))")
	         .setCallbacks(bindConfig, updateConfig, null)
	         .setAutoConfig(false)
	         .setRequired(true));
	
	srv.add(_dm.createServiceDependency()
	        .setService(javax.naming.InitialContext.class,
	                    "(& ("+BUNDLE_SYMBOLICNAME+"=" + b.getSymbolicName() + ")"	
	                    + " ("+BUNDLE_VERSION+"=" + version + "))")
	                    //this dependency is not used, we just want to make 
			    //sure it is here
			    .setCallbacks(null, null, null)
	                    .setAutoConfig(false)
	                    .setRequired(true));
        */
      }
      _dm.add(srv);
      addApps(getBundleKey(b), srv);
    }
  }

  /**
   * Tells if a given bundle imports the AppService interface.
   * @param b the bundle to be checked
   * @return true if the given bundle imports the AppService interface, otherwise false.
   */
  public static boolean useAppService(Bundle b) {
    Object ImportPackage = b.getHeaders().get("Import-Package");
    if (ImportPackage == null) {
      return false;
    }
    String imports = ImportPackage.toString();
    return imports.indexOf(AppService.class.getPackage().getName()) != -1;
  }

  /**
   * Sets an interface name to be used when registering the application
   * objects returned by the ApplicationListener.<br>
   * If used this method must be used <b>before</b> the open() method.<br>
   * If not used, the name of the object returned by the ApplicationListener
   * (Object.getClass().getName()) will be used.
   * @param name an interface name implemented by the application objects returned 
   * by the ApplicationListener
   */
  public void setApplicationWrapperInterfaceName(String name) {
      _interfaceName = name;
  }

  /**************** Bundle Listener ***********************/
  /**
   * this method is called by the OSGi framework whenever a bundle's state changes (installed,
   * started, updated, stopped....)
   */
  public void bundleChanged(BundleEvent event) {
    switch (event.getType()) {
    case BundleEvent.STARTED:
      bundleStarted(event.getBundle());
      break;
    case BundleEvent.STOPPED:
      bundleStopped(event.getBundle());
      break;
    default:
    }
  }

  private void bundleStarted(Bundle b) {
    loadApp(b, true);
  }

  private void bundleStopped(Bundle b) {
    if (!useAppTracker(b)) {
      unloadApp(b);
    }
  }

  /**************** AppService ***********************/

  public void registerApplication(Bundle b) {
    if (_logger.isDebugEnabled()) {
      _logger.debug(b.getSymbolicName() + " registered itself via the AppService.. inspect it");
    }
    // sipapp specifically registered by its activator, don't check activator again!
    loadApp(b, false);
  }

  public void unregisterApplication(Bundle b) {
    unloadApp(b);
  }

  /**************** Helpers ***********************/

  private void registerAppService() {
    // register AppService that applications with an activator will use 
    // to register themselves when ready
    String key = getBundleKey(_context.getBundle()) + (_appSrvProps != null ? _appSrvProps.toString() : "");
    if(_dm == null) {
      addApps(key,
      	_context.registerService(AppService.class.getName(), this, _appSrvProps));
    }
    else {
      Component srv = _dm.createComponent()
	  .setInterface(AppService.class.getName(), _appSrvProps)
	  .setImplementation(this);
      _dm.add(srv);
      addApps(key, srv);
    }
    if (_logger.isDebugEnabled())
      _logger.debug(AppService.class.getName() + " registered with " + _appSrvProps);
  }

  private String getBundleKey(Bundle b) {
    StringBuilder sb = new StringBuilder(b.getSymbolicName());
    sb.append("/");
    String version = (String) b.getHeaders().get(BUNDLE_VERSION);
    if (version == null) {
      version = "1.0.0";
    }
    sb.append(version);
    return sb.toString();
  }

  private void addApps(String key, Object registration) {
    List registrations = _apps.get(key);
    if (registrations == null) {
      registrations = new ArrayList();
    }
    registrations.add(registration);
  }

  /**
   * register an application object into the OSGi framework if any found
   */
  private void loadApp(Bundle b, boolean checkImports) {
    Object ap = inspect(b, checkImports);
    if(ap != null) {
      String iname = _interfaceName;
      if(iname == null) {
	iname = ap.getClass().getName();
      }

      if(_logger.isDebugEnabled()) 
	_logger.debug("Application found in " + b.getSymbolicName() + ": register new " + iname);

      registerService(b, iname, ap, null);
    }
  }

  private void unloadApp(Bundle b) {
    // If the bundle that registered the Application is not ACTIVE: don't unregister the
    // service, or we'll
    // catch an IllegalStateException.

    try {
      if (b.getState() != Bundle.ACTIVE) {
	return;
      }
      List registrations = (List) _apps.remove(getBundleKey(b));
      for (Object registration : registrations) {
	try {
	  if (registration instanceof ServiceRegistration) {
	    ServiceRegistration sr = (ServiceRegistration) registration;
	    ServiceReference ref = sr.getReference(); // Can't be null at this point
	    if (ref != null && ref.getBundle().getState() == Bundle.ACTIVE) {
	        sr.unregister();
	    }
	  } else if (registration instanceof Component) {
		  Component s = (Component) registration;
		  _dm.remove(s);
	  }
	} catch (IllegalArgumentException e) {
	  // Service already unregistered (probably because the bundle that registered the app
	  // is already stopped).
	  if (_logger.isDebugEnabled()) {
	    _logger.debug("Application from bundle " + b.getSymbolicName()
		+ " is arealdy unregistered.");
	  }
	}
      }
    } catch (Throwable t) {
      _logger.debug("Could not unregister Application from bundle " + b.getSymbolicName(), t);
    }
  }

  /**
   * inspect a bundle to see whether it contains a SIP application (or something else)
   */
  private Object inspect(Bundle b, boolean checkImports) {
    String name = b.getSymbolicName();
    // OSGi doesn't like getResource on the system bundle..
    // prevent this and in the same time prevent parsing core and framework's bundles...
    String loc = b.getLocation();
    if (b.getBundleId() == 0
	|| !(loc.endsWith(".par") || loc.endsWith(".sar") || loc.endsWith(".war")))
    {
      return null;
    }

    if (_logger.isDebugEnabled())
      _logger.debug("inspecting bundle " + name + " @ " + loc);
    if (checkImports && useAppTracker(b)) {
      return null;
    }
    return _listener.applicationDiscovered(b, new BundleClassLoader(b));
  }

  private boolean useAppTracker(Bundle b) {
    // Check whether the bundle imports the AppTracker. if AppTracker is found: the applications
    // must
    // register/unregister themselves, using the AppTracker.
    String imports = (String) b.getHeaders().get("Import-Package");
    if (imports != null && imports.indexOf(getClass().getPackage().getName()) != -1) {
      String name = b.getSymbolicName();
      if (_logger.isDebugEnabled()) {
                _logger.debug("bundle "
                        + name
                        + " imports "
                        + getClass().getPackage()
                        + ": It must register/unregister itself via the AppService.registerApplication() method."
                        + " This application won't be deployed/undeployed for now.");
      }
      return true;
    }
    return false;
  }

  private Logger _logger;
  BundleContext _context;
  DependencyManager _dm;
  Dictionary _appSrvProps;
  Listener _listener;
  String _interfaceName = null;

  /**
   * Map of registered applications. key = String, val = list of ServiceRegistration or DM
   * service
   */
  Hashtable<String, List> _apps;
}
