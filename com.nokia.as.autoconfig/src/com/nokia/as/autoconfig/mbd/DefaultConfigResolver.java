package com.nokia.as.autoconfig.mbd;

import static com.alcatel.as.service.metatype.PropertyDescriptor.DEFAULT_VALUE;

import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;

import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.metatype.MetaData;
import com.alcatel.as.service.metatype.MetatypeParser;
import com.alcatel.as.service.metatype.PropertiesDescriptor;
import com.alcatel.as.service.metatype.PropertyDescriptor;
import com.nokia.as.autoconfig.AutoConfigurator;
import com.nokia.as.autoconfig.Configuration;

import alcatel.tess.hometop.gateways.utils.Log;

public class DefaultConfigResolver {

	private final LogService logger = Log.getLogger(AutoConfigurator.LOGGER);
	private Configuration defaultConfig = new Configuration();
	
	private MetatypeParser mbdParser;
	
	public DefaultConfigResolver(MetatypeParser mbdParser) {
		this.mbdParser = mbdParser;
	}
	
	public void resolve(Bundle bundle) {
		logger.debug("Resolving the default configuration of bundle %s", bundle.getSymbolicName());
		
		MetaData metadata = null;
		try {
			logger.debug("Loading the metadata");
			metadata = mbdParser.loadMetadata(bundle, false);
		} catch(Exception e) {
			logger.warn("Error while loading metadata for %s", bundle.getSymbolicName(), e);
		}
		
		if(metadata == null) {
			logger.warn("No metadata found for bundle %s", bundle.getSymbolicName());
			return;
		}
		
		populateDefaultConfig(metadata);
	}
	
	private void populateDefaultConfig(MetaData metadata) {
		Map<String, PropertiesDescriptor> props = metadata.getProperties();
	
		if (props != null) {
			props.entrySet()
				 .forEach(e -> defaultConfig.config.put(e.getKey(), 
														getDefaultDictionary(e.getValue())));
		}
	}
	
	private Map<String, Object> getDefaultDictionary(PropertiesDescriptor propDescriptor) {
		Map<String, PropertyDescriptor> properties = propDescriptor.getProperties();
		Map<String, Object> props = 
				properties.entrySet()
			  		.stream()
					.collect(Collectors.toMap(Map.Entry::getKey,
							  			      e -> e.getValue().getAttribute(DEFAULT_VALUE)));
		props.put(Configuration.AUTOCONF_ID, "true");
		return props;
	}
	
	public Configuration config() {
		return defaultConfig;
	}	
}
