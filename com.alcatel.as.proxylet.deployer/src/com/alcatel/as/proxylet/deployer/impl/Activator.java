package com.alcatel.as.proxylet.deployer.impl;

import java.util.Dictionary;
import java.util.Properties;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.diagnostics.ServiceDiagnostics;
import com.alcatel.as.util.config.ConfigHelper;
import com.nextenso.proxylet.engine.DeployerDescriptor;

/**
 * Activator used for the blueprint mode. 
 * Base Activator for other modes
 */
public class Activator extends DependencyActivatorBase {
	protected final static Logger _logger = Logger.getLogger("as.service.pxletdeployer.Activator");
    DependencyManager _dm;    
	
    public void init(BundleContext ctx, DependencyManager dm) throws Exception {
    	// Obtain agent config in order to see which protocols are enabled
    	_dm = dm;
    	Component agentConfig = dm.createComponent()
    			.setImplementation(this)
    			.add(createConfigurationDependency().setPid("agent").setCallback("setAgentConfig"));
    	dm.add(agentConfig);
    }
    
    protected void setAgentConfig(Dictionary<String, Object> d) {
    	// Only filter DeployerDescritors matching the list of enabled protocols
    	String filter = getDeployerDescriptorFilter(d); 
    	
    	if (filter == null) {
			_logger.debug("no protocol defined in callout agent, won't track any proxylet protocol descriptors.");
    		// no protocol defined in callout agent: do nothing
    		return;
    	}
    	
		if (_logger.isDebugEnabled())
			_logger.debug("Will track deployer descriptors with filter " + filter);

		// The following component properties allows to make sure that this component 
		// is started from the IO-threadpool, and never from the current thread.	     
		Properties properties = new Properties();
		properties.setProperty("asr.component.parallel", "true");
		properties.setProperty("asr.component.cpubound", "false");

		// now create our DeployerDescriptor adapter component.
		// A DeployerDescritor component will be created for each discovered DeployerDescriptor available from the registry
		Component adapter = _dm.createAdapterComponent()
			.setAdaptee(DeployerDescriptor.class, filter)
			.setImplementation(PxletDeployerImpl.class)
			.setServiceProperties(properties)
			.add(createServiceDependency()
				.setRequired(true).setService(ServiceDiagnostics.class).setAutoConfig("_diagnostics"))                  
			.add(createServiceDependency()
				.setRequired(true).setService(Dictionary.class, "(service.pid=globalConfig)").setCallbacks("updateGlobalConfig", null));
		_dm.add(adapter);
    }

	private String getDeployerDescriptorFilter(Dictionary<String, Object> d) {
		String protocols = ConfigHelper.getString(d, "agent.muxhandlers", "").toLowerCase().trim();
		if (_logger.isDebugEnabled())
			_logger.debug("Enabled protocols:" + protocols);
		if (protocols.length() == 0) {
			return null;
		}
		String filter = "(|";
		for (String protocol : protocols.trim().split(" ")) {
			filter += "(protocol~=" + protocol + ")";
		}
		filter += ")";
		return filter;
	}
    
}
