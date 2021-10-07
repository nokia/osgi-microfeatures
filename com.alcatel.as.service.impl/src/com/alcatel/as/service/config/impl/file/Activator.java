package com.alcatel.as.service.config.impl.file;

import java.util.Collection;
import java.util.Hashtable;

import org.apache.felix.cm.PersistenceManager;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationPlugin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.alcatel.as.service.config.impl.FelixPersistenceManager;

public class Activator implements BundleActivator, ServiceTrackerCustomizer<ConfigurationAdmin, ConfigurationAdmin>  
{
    private final static Logger _logger = Logger.getLogger("as.service.config.file.Activator");
    private BundleContext _bctx;
    private ConfigurationPlugin _confPlugin;
    private LegacyStandaloneConfig _confManager;
	private ServiceRegistration<EventHandler> _registration;

	@Override
	public void start(BundleContext ctx) throws Exception {
        try {
			_bctx = ctx;

	        _confManager = new LegacyStandaloneConfig(
	        		(String) _bctx.getProperty("as.config.file.confdir"),
	                (String) _bctx.getProperty("as.config.file.period"),
	                (String) _bctx.getProperty("log4j.pid"));
	        _confManager.setBundleContext(ctx);
	        
			ConfigEntry.propagateSystemProperties(
					"true".equals(ctx.getProperty("as.config.file.propagateSystemProperties")));

			// Provide a backend Felix CM persistence manager in the registry for avoiding
			// CM to store our config in the file system. We must do that before CM actually
			// starts.
			Hashtable<String, Object> props = new Hashtable<>();
			props.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MAX_VALUE));
			props.put("name", "casr");
			ctx.registerService(PersistenceManager.class.getName(), new FelixPersistenceManager(), props);

			// Obtain the ConfigurationPlugin, if one is available
			Collection<ServiceReference<ConfigurationPlugin>> refs = ctx.getServiceReferences(ConfigurationPlugin.class, "(type=asr.connect.launcher)");
			if (refs.size() != 0) {
				_confPlugin = ctx.getService(refs.iterator().next());
			}

			// Track config admin
			Filter filter = ctx.createFilter("(objectClass=" + ConfigurationAdmin.class.getName() + ")");
			ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> cmTracker = new ServiceTracker<>(ctx, filter, this);
			cmTracker.open();
        }
        catch (Throwable t)
        {
            _logger.warn("Failed to start StandaloneConfig", t);
        }
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		_confManager.stop();
	}

	@Override
	public ConfigurationAdmin addingService(ServiceReference<ConfigurationAdmin> reference) {
		ConfigurationAdmin cm = (ConfigurationAdmin) _bctx.getService(reference);
        _confManager.setConfigAdmin(cm);
        _confManager.setConfigPlugin(_confPlugin);
        _confManager.start();
        Hashtable<String, Object> props = new Hashtable<>();
        String[] topics = new String[] {
        		"com/nokia/casr/ConfigEvent/UPDATED"
        };
        props.put(EventConstants.EVENT_TOPIC, topics);
        _registration = _bctx.registerService(EventHandler.class, _confManager, props);
		return cm;
	}

	@Override
	public void modifiedService(ServiceReference<ConfigurationAdmin> reference, ConfigurationAdmin service) {
	}

	@Override
	public void removedService(ServiceReference<ConfigurationAdmin> reference, ConfigurationAdmin service) {
		try {
			_registration.unregister();
		} catch (Exception e) {}
		_confManager.stop();
	}
}
