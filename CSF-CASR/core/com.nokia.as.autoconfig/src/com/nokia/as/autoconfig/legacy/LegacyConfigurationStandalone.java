package com.nokia.as.autoconfig.legacy;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.alcatel.as.service.log.LogService;
import com.nokia.as.autoconfig.AutoConfigurator;

import alcatel.tess.hometop.gateways.utils.Config;
import alcatel.tess.hometop.gateways.utils.Log;

public class LegacyConfigurationStandalone {
	private LogService logger = Log.getLogger(AutoConfigurator.LOGGER);
	private volatile Optional<ServiceRegistration<?>> reg = Optional.empty();
	private BundleContext bc;
	
	public LegacyConfigurationStandalone(BundleContext bc) {
		this.bc = bc;
	}
	
	public void update(Map<String, Map<String, Object>> conf) {
		Config config = new Config();
		
		conf.entrySet().stream().forEach(e -> {
			Map<String, Object> vals = e.getValue();
			vals.entrySet().forEach(ee -> config.put(ee.getKey(), ee.getValue()));	
		});
		
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("service.pid", "globalConfig");
		logger.debug("Applying legacy config standalone");
		ServiceRegistration<?> reg = 
				bc.registerService(new String[] { Config.class.getName(), Dictionary.class.getName() }, config, props);
		
		this.reg.ifPresent(previous -> previous.unregister());			
		this.reg = Optional.of(reg);
	}
	
	public void delete() {
		logger.debug("Deleteting legacy config standalone");
		this.reg.ifPresent(previous -> previous.unregister());
	}
}
