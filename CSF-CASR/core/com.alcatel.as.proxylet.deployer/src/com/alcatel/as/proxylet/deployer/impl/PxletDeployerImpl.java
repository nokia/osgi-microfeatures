package com.alcatel.as.proxylet.deployer.impl;

// jdk
import static com.nextenso.proxylet.engine.ProxyletApplication.CONTEXT_LISTENER;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.alcatel.as.service.diagnostics.ServiceDiagnostics;
import com.nextenso.proxylet.admin.Bearer;
import com.nextenso.proxylet.admin.Protocol;
import com.nextenso.proxylet.engine.DeployerDescriptor;
import com.nextenso.proxylet.engine.ProxyletApplication;
import com.nextenso.proxylet.engine.ProxyletInvocationHandler;
import com.nextenso.proxylet.event.ProxyletContextListener;
import com.nextenso.proxylet.event.ProxyletEventListener;

/**
 * This is the OSGi version of the PxletDeployer. it listens for bundle start/stop events and
 * scans started bundles for a proxylet application (http/diameter) it is responsible for
 * building a ProxyApp wrapper and handing it to the Pxlet container via OSGi's "white board"
 * 
 * Thread safe: all components are running within their own queue on the IO blocking threadpool.
 * This means that all lifecycle / service dependency events are serially handled in the queue 
 * (on the blocking threadpool).
 * 
 * See the special "asr.component.parallel" and "asr.component.cpubound" properties in the Activator.
 * 
 * Note: fields are not volatile, no need to do that because dependency manager ensures safe object
 * publications when callbacks are invoked.
 */
public class PxletDeployerImpl {
	private final static Logger _logger = Logger.getLogger("as.service.deployer.pxlet");
	private String _protocol;
	private BearerProvider _bearerProvider; //different for each implem
	private BundleContext _bctx; //injected by DM
	private ServiceDiagnostics _diagnostics; 
	private ProxyletApplicationImpl _app;
	private ServiceRegistration _registration;
	private Hashtable _references;
	private Map<String, Class[]> _bindings;
	private DeployerDescriptor _dd; //injected by DM
	Dictionary _globalconf; //injected by DM

	// all required dependencies are injected. Now add more dependencies dynamically.
	public void init(Component comp) {
		String listenerFilter = "(protocol=" + _dd.getProtocol() + ")";
		DependencyManager dm = comp.getDependencyManager();
		
		comp.add(dm.createServiceDependency()
			  .setService(ProxyletContextListener.class, listenerFilter)
			  .setCallbacks("bindProxyletContextListener", "unbindProxyletContextListener")
			  .setAutoConfig(false)
			  .setRequired(false));
		
		comp.add(dm.createServiceDependency()
			  .setService(ProxyletEventListener.class, listenerFilter)
              .setCallbacks("bindProxyletEventListener", "unbindProxyletEventListener")
              .setAutoConfig(false)
              .setRequired(false));
      
		if (_dd.getBindings() != null) {
			for (Class[] intrfcs : _dd.getBindings().values()) {
         	  for (Class intrfc : intrfcs) {
         		  comp.add(dm.createServiceDependency()
         				  .setService(intrfc, (String) null)
         				  .setCallbacks("bindObject", "unbindObject")
         				  .setAutoConfig(false)
         				  .setRequired(false));
         	  }
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public void updateGlobalConfig(Dictionary<String, Object> globalconf) {
		_globalconf = globalconf;
		if (_app != null) {
            String[] properties =  Collections.list(globalconf.keys())
            		.stream()
            		.map(k -> k.toString())
            		.toArray(String[]::new);
			_app.updateConfig(globalconf, properties);
		}
	}
      
	// called by DM when all dependencies are resolved
	void start() {
		try {
			_protocol = _dd.getProtocol().toLowerCase();
			_bindings = _dd.getBindings();
			_app = new ProxyletApplicationImpl(_dd.getParser(), _globalconf);
			_bearerProvider = new ConfigBearerProvider(_dd);
			// retrieve merged contexts from the BearerProvider...
			Protocol p = Protocol.getProtocolInstance(_protocol.toUpperCase());
			Bearer bearer = _bearerProvider.readDeployedBearerContext();
			_app.init(bearer);
			if (_app.isEmpty()) {
				if (_logger.isDebugEnabled())
					_logger.debug(
							"No application deployed for protocol " + _protocol + ". Registering empty application.");
				Hashtable<String, Object> serviceProps = new Hashtable<>();
				serviceProps.put("protocol", _protocol);
				_registration = _bctx.registerService(ProxyletApplication.class.getName(), _app, serviceProps);
				return;
			} else {
				_app.start();
				startCountdown();
			}

			if (_logger.isDebugEnabled())
				_logger.debug("PxletDeployer[" + _protocol + "] started.");
		} catch (Exception e) {
			_logger.error("Failed to start PxletDeployer[" + _protocol + "]", e);
		}
	}
  
	protected void stop() {
		try {
			_registration.unregister();
		} catch (Throwable t) {
		}
		_registration = null;
	}
  
	// bind OSGi listeners. see Activator
	protected void bindProxyletContextListener(final Map props, final ProxyletContextListener o) {
		if (_logger.isDebugEnabled())
			_logger.debug("got OSGi ProxyletContextListener binding: " + o + " with properties " + props);
		bindInstance(CONTEXT_LISTENER, (String) props.get("name"), o);
	}

	protected void unbindProxyletContextListener(final Map props, final ProxyletContextListener o) {
		if (_logger.isDebugEnabled())
			_logger.debug("OSGi ProxyletContextListener removed: " + o + " with properties " + props);
		unbindInstance(CONTEXT_LISTENER, (String) props.get("name"), o);
	}

	protected void bindProxyletEventListener(final Map props, final ProxyletEventListener o) {
		if (_logger.isDebugEnabled())
			_logger.debug("got OSGi ProxyletEventListener binding: " + o + " with properties " + props);
		bindInstance((String) props.get("type"), (String) props.get("name"), o);
	}

	protected void unbindProxyletEventListener(final Map props, final ProxyletEventListener o) {
		if (_logger.isDebugEnabled())
			_logger.debug("OSGi ProxyletEventListener removed: " + o + " with properties " + props);
		unbindInstance((String) props.get("type"), (String) props.get("name"), o);
	}
  
	// note: Map cannot be used here because we're not binding a specific interface
	// see DM supported callback signatures
	protected void bindObject(final ServiceReference sref, final Object o) {
		if (_logger.isDebugEnabled())
			_logger.debug("got OSGi binding: " + o);

		// WARNING! duplicate loop in unbindObject.. please reflect changes!
		boolean bound = false;
		for (String type : _app.getTypes()) {
			Class[] classes = _bindings.get(type);
			if (classes != null)
				for (Class role : classes) {
					if (role.isAssignableFrom(o.getClass())) {
						String name = (String) sref.getProperty("name");
						if (name == null)
							for (String n : _app.getNames(type)) {
								if (o.getClass().getName().equals(_app.getClassName(type, n))) {
									name = n;
									break;
								}
							}
						bindInstance(type, name, o);
						bound = true;
					}
				}
		}
		if (bound)
			registerWhenReady();
		else
			_logger.warn("bound object " + o + " does not match any recognized interface for protocol " + _protocol
					+ ". Ignore.");
	}
  
	protected void unbindObject(final ServiceReference sref, final Object o) {
		if (_logger.isDebugEnabled())
			_logger.debug("OSGi binding removed: " + o);

		// WARNING! duplicate loop in bindObject.. please reflect changes!
		boolean unbound = false;
		for (String type : _app.getTypes()) {
			Class[] classes = _bindings.get(type);
			if (classes != null)
				for (Class role : classes) {
					if (role.isAssignableFrom(o.getClass())) {
						String name = (String) sref.getProperty("name");
						if (name == null)
							for (String n : _app.getNames(type)) {
								if (o.getClass().getName().equals(_app.getClassName(type, n))) {
									name = n;
									break;
								}
							}
						unbindInstance(type, name, o);
						unbound = true;
					}
				}
		}
		if (unbound && _registration != null) {
			try {
				_registration.unregister();
			} catch (IllegalStateException ise) {
				if (_logger.isDebugEnabled())
					_logger.debug("Pxlet application already unregistered. Ignore " + ise);
			}
			_registration = null;
		} else
			_logger.warn("bound object " + o + " does not match any recognized interface for protocol " + _protocol
					+ ". Ignore.");
	}
  
	private void bindInstance(String type, String name, Object o) {
		if (_app == null || _app.isEmpty()) {
			_logger.info("Ignoring unexpected proxylet registration: '" + o + "'; No application deployed for protocol "
					+ _protocol);
			return;
		}

		_app.setInstance(type.trim(), name, ProxyletInvocationHandler.newInstance(o.getClass().getClassLoader(), o));
	}
  
	private void unbindInstance(String type, String name, Object o) {
		if (_app == null || _app.isEmpty()) {
			_logger.info("Ignoring unexpected proxylet removal: '" + o + "'; No application deployed for protocol "
					+ _protocol);
			return;
		}
		_app.setInstance(type.trim(), name, null);
		if (_registration != null) {
			if (_logger.isDebugEnabled())
				_logger.debug("Unregister " + _protocol + " application after OSGi pxlet " + o + " is gone.");
			_registration.unregister();
			_registration = null;
		}
	}

	public String getProtocol() {
		return _protocol;
	}
  
	protected void registerWhenReady() {
		if (_app.isReady()) {
				_logger.info(_protocol + " application is ready; register " + _app + " with " + _bctx);
			_registration = _bctx.registerService(ProxyletApplication.class.getName(), _app, new Hashtable() {
				{
					put("protocol", _protocol);
				}
			});
		}
	}

	private void registerInstance(String name, Object instance) {
		if (_references == null)
			_references = new Hashtable();

		if ((name != null) && (instance != null))
			_references.put(name, instance);
	}

	private Object getRegisteredInstance(String name) {
		if ((_references == null) || (name == null))
			return null;
		else
			return _references.get(name);
	}

	private void startCountdown() {
		final long countdown = 60000;
		new Timer().schedule(new TimerTask() {
			public void run() {
				if (!_app.isReady()) {
					_logger.warn(_protocol + " application is NOT READY to be deployed after " + countdown
							+ "ms. Awaiting OSGi proxylets : " + _app.awaiting() + " .Services diagnostics summary : "
							+ _diagnostics.notAvail());
				} else if (!_app.isInitDone()) {
					_logger.warn(_protocol + " application is ready but NOT DEPLOYED after " + countdown
							+ "ms. Services diagnostics summary : " + _diagnostics.notAvail());
				}
			}
		}, countdown);
	}
}
