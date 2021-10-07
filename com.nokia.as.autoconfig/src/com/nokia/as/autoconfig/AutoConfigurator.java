package com.nokia.as.autoconfig;

import static com.alcatel.as.util.config.ConfigConstants.SYSTEM_PID;
import static com.nokia.as.autoconfig.Configuration.SPID_ID;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.metatype.MetatypeParser;
import com.nokia.as.autoconfig.bnd.BundleConfigResolver;
import com.nokia.as.autoconfig.file.DirectoryWatcher;
import com.nokia.as.autoconfig.file.FileConfigResolver;
import com.nokia.as.autoconfig.legacy.LegacyConfiguration;
import com.nokia.as.autoconfig.legacy.LegacyConfigurationStandalone;
import com.nokia.as.autoconfig.legacy.Log4jConfigurator;
import com.nokia.as.autoconfig.legacy.SystemPidGenerator;
import com.nokia.as.autoconfig.mbd.DefaultConfigResolver;
import com.nokia.as.autoconfig.sys.SystemPropertyResolver;
import com.nokia.as.autoconfig.transform.EnvironmentTransformer;
import com.nokia.as.autoconfig.transform.FilePropertyTransformer;
import com.nokia.as.autoconfig.transform.InstancePidTransformer;
import com.nokia.as.autoconfig.transform.JavaLangTransformer;
import com.nokia.as.autoconfig.transform.Transformer;
import com.nokia.as.autoconfig.transform.VaultTransformer;

import alcatel.tess.hometop.gateways.utils.Log;

public class AutoConfigurator implements EventHandler {
	
	public static final String LOGGER = "com.nokia.as.autoconfig";
	public static LogService logger = Log.getLogger(AutoConfigurator.LOGGER);
	
	private Configuration previous;
	private Configuration current;
	
	private volatile ConfigurationAdmin configAdmin;
	private MetatypeParser mbdParser;
	private volatile BundleContext bc;
	
	private List<DirectoryWatcher> directoryWatchers = new ArrayList<>();
	private LegacyConfigurationStandalone legacyConfigurationStandalone;
	private final Map<String, LegacyConfiguration> legacyConfig = new HashMap<>();
	private Log4jConfigurator log4j;
	
	private FilePropertyTransformer fptrans;
	private InstancePidTransformer iptrans;
	private JavaLangTransformer jltrans;
	private EnvironmentTransformer envtrans;
	private VaultTransformer vaulttrans;
	private Transformer transformer = new Transformer();
	
	private DefaultConfigResolver defaultResolver;
	private BundleConfigResolver bundleResolver;
	private FileConfigResolver fileResolver;
	private SystemPropertyResolver systemResolver;
	
	private Map<org.osgi.service.cm.Configuration, String> factoryIds = new HashMap<>();
	private static long systemPidSalt = System.nanoTime();
	
	public void start() {
		logger.info("Starting AutoConfigurator");
		
		previous = new Configuration();
		current = new Configuration();

		fptrans = new FilePropertyTransformer();
		iptrans = new InstancePidTransformer(System::nanoTime);
		jltrans = new JavaLangTransformer();
		envtrans = new EnvironmentTransformer();
		vaulttrans = new VaultTransformer(logger);

		defaultResolver = new DefaultConfigResolver(mbdParser);
		bundleResolver = new BundleConfigResolver();
		fileResolver = new FileConfigResolver();
		systemResolver = new SystemPropertyResolver();

		log4j = new Log4jConfigurator();
		legacyConfigurationStandalone = new LegacyConfigurationStandalone(bc);
	
		Activator.getConfDirs().forEach(fileResolver::resolve);	
		//Set up directory watchers
		List<String> confDirs = Activator.getConfDirs();
		for(String dir : confDirs) {
			logger.debug("Setting up directory watcher for directory %s", dir);
			if(new File(dir).exists()) {
				try {
					DirectoryWatcher dw = new DirectoryWatcher(this, dir);
					dw.start();
					directoryWatchers.add(dw);
				} catch (IOException e) {
					logger.warn("Unable to watch %s", dir);
					logger.debug("Reason is %s", e.getMessage());
				}
			} else {
				logger.debug("%s not found", dir);
			}
		}
		
		configure();
		log4j.registerServices(bc, current.config);
	}
	
	public void stop() throws Exception {
		logger.info("Stopping Autoconfigurator");
		logger.debug("Interrupting directory watchers");
		log4j.unregisterServices(bc);
		legacyConfigurationStandalone.delete();
		legacyConfig.values().forEach(LegacyConfiguration::delete);
		
		directoryWatchers.forEach(Thread::interrupt);
		directoryWatchers.forEach(dw -> {
			try {
				dw.join(10000);
			} catch (InterruptedException e) {
				logger.debug("Could not join with DirectoryWatcher");
			}
		});
	}
	
	public void bundleAdded(Bundle bundle) {
		logger.debug("Detected bundle %s, resolving default configuration", bundle.getSymbolicName());
		defaultResolver.resolve(bundle);
		configure();
	}
	
	public void configAdded(Bundle bundle) {
		logger.debug("Detected configuration bundle %s, resolving configuration " + bundle.getSymbolicName());
		bundleResolver.resolve(bundle);
		configure();
	}
	
	public void configRemoved(Bundle bundle) {
		logger.debug("Detected configuration bundle %s, removing configuration " + bundle.getSymbolicName());
		bundleResolver.remove(bundle);
		configure();
	}
	
	public void fileEvent(String directory) {
		logger.debug("Received a file event on directory %s, resolving directory", directory);
		fileResolver.resolve(directory);
		configure();
	}
	
	private void configure() {
		systemResolver.resolve();
		logger.debug("Applying configuration");
		previous = getPreviousFromConfigAdmin();
		current = getCurrentConfiguration();
		generateSystemPid(current);
		
		logger.trace("Previous config: %s", previous);
		logger.trace("Current config: %s", current);

		ConfigurationDelta delta = current.getDelta(previous);
		if(!delta.isEmpty()) {
			logger.debug("Delta: %s", delta);
			logger.debug("Applying configuration delta");
			applyConfiguration(delta);
			logger.debug("Updating legacy configuration standalone");
			legacyConfigurationStandalone.update(current.config);
		} else {
			logger.debug("Delta is empty, doing nothing");
		}
	}
	
	private Configuration getCurrentConfiguration() {
		Function<Map<String, Object>, Map<String, Object>> transformation = fptrans.andThen(vaulttrans).andThen(jltrans).andThen(iptrans).andThen(envtrans);
		
		logger.debug("Applying transformers on default configuration");
		Configuration defaultConfig = transformer.transform(defaultResolver.config(), transformation);
		logger.trace("Default configuration: %s", defaultConfig);

		logger.debug("Applying transformers on bundle configuration");
		Configuration bundleConfig = transformer.transform(bundleResolver.config(), transformation);
		logger.trace("Bundle configuration: %s", bundleConfig);

		logger.debug("Applying transformers on file configuration");
		Configuration fileConfig = transformer.transform(fileResolver.config(), transformation);
		logger.trace("File configuration: %s", fileConfig);

		logger.debug("Applying transformers on system configuration");
		Configuration systemConfig = transformer.transform(systemResolver.config(), transformation);
		logger.trace("System configuration: %s", systemConfig);
		
		Configuration beforePatch = Configuration.merge(Configuration.merge(Configuration.merge(defaultConfig, bundleConfig), fileConfig), systemConfig);
		return transformer.transform(fileResolver.applyPatch(beforePatch), transformation);
	}
	
	private void applyConfiguration(ConfigurationDelta delta) {
	    
		logger.debug("Populating new configuration");
		delta.added.config.forEach(this::populateConfig);
		delta.added.factoryConfig.forEach(this::createFactoryConfig);
		
		logger.debug("Deleting old configuration");
		delta.deleted.config.keySet().forEach(this::deleteConfig);
		delta.deleted.factoryConfig.forEach(this::deleteFactoryConfig);
		
		logger.debug("Updating existing configuration");
		delta.updated.config.forEach(this::populateConfig);
		delta.updated.factoryConfig.forEach(this::updateFactoryConfig);
	}
	
	private void populateConfig(String pid, Map<String, Object> properties) {
		logger.debug("Populating config for %s", pid);
		if(pid.equals(SYSTEM_PID)) {
			logger.debug("pid = system, adding properties to system: %s", properties);
			SystemPidGenerator.addPropsToSystem(properties);
		}
		try {
			org.osgi.service.cm.Configuration conf = configAdmin.getConfiguration(pid, "?");
			logger.debug("Applying configuration in ConfigAdmin");
			Dictionary<String, Object> dicProp = Utils.dictionaryFromMap(properties);
			conf.update(dicProp);
			logger.debug("Updating legacy configuration");
			LegacyConfiguration legacyConfig = this.legacyConfig.computeIfAbsent(pid, lc -> new LegacyConfiguration(pid, bc));	
			legacyConfig.update(dicProp);
		} catch(IOException e) {
			logger.warn("Error while populating config for %s", e, pid);
		}
	}
	
	private void deleteConfig(String pid) {
		logger.debug("Deleting config for %s", pid);
		try {
			logger.debug("Deleting configuration from ConfigAdmin");
			org.osgi.service.cm.Configuration conf = configAdmin.getConfiguration(pid, "?");
			if("true".equals(conf.getProperties().get(Configuration.AUTOCONF_ID))) {
				conf.delete();
				logger.debug("Updating legacy configuration");
				LegacyConfiguration legacyConfig = this.legacyConfig.remove(pid);
				if(legacyConfig != null) legacyConfig.delete();
			}
		} catch(IOException e) {
			logger.warn("Error while deleting config for %s", e, pid);
		}
	}
	
	private void createFactoryConfig(String factoryPid, List<Map<String, Object>> properties) {
		logger.debug("Populating factory config for %s", factoryPid);
		properties.forEach(m -> {
			try {
				String factoryId = m.remove(Configuration.FACTORY_ID).toString();
				logger.debug("Creating factory configuration in ConfigAdmin");
				org.osgi.service.cm.Configuration conf = configAdmin.createFactoryConfiguration(factoryPid, "?");
				conf.update(Utils.dictionaryFromMap(m));
				factoryIds.put(conf, factoryId);
			} catch(IOException e) {
				logger.warn("Error while populating factory config for %s", e, factoryPid);
			}
		});
	}
	
	private void deleteFactoryConfig(String factoryPid, List<Map<String, Object>> properties) {
		logger.debug("Deleting factory config for " + factoryPid);
		properties.forEach(m -> {
			try {
				String pid = m.get(SPID_ID).toString();
				logger.debug("Deleting factory configuration from ConfigAdmin");
				org.osgi.service.cm.Configuration conf = configAdmin.getConfiguration(pid, "?");
				if("true".equals(conf.getProperties().get(Configuration.AUTOCONF_ID))) {
					conf.delete();
				}
			} catch(IOException e) {
				logger.warn("Error while deleting factory config for %s", e, factoryPid);
			}
		});
	}
	
	private void updateFactoryConfig(String factoryPid, List<Map<String, Object>> properties) {
		logger.debug("Updating factory config for %s", factoryPid);
		properties.forEach(m -> {
			try {
				String factoryId = m.remove(Configuration.FACTORY_ID).toString();
				logger.debug("updating factory configuration in ConfigAdmin");
				org.osgi.service.cm.Configuration conf = configAdmin.getConfiguration(m.get("service.pid").toString(), "?");
				conf.update(Utils.dictionaryFromMap(m));
				factoryIds.put(conf, factoryId);
			} catch(IOException e) {
				logger.warn("Error while populating factory config for %s", e, factoryPid);
			}
		});
	}
	
	private Configuration getPreviousFromConfigAdmin() {
        Configuration previousConfig = new Configuration();
        
        try {
            org.osgi.service.cm.Configuration[] configs = configAdmin.listConfigurations(null);
            if(configs == null) return previousConfig;
            for(org.osgi.service.cm.Configuration conf : configs) {
                String factoryPid = conf.getFactoryPid();
                if(factoryPid == null) {
                    previousConfig.config.put(conf.getPid(), Utils.mapFromDictionary(conf.getProperties()));
                } else {
                    List<Map<String, Object>> fConf = previousConfig.factoryConfig.get(factoryPid);
                    if(fConf == null) previousConfig.factoryConfig.put(factoryPid, new ArrayList<>());
                    fConf = previousConfig.factoryConfig.get(factoryPid);
                    Map<String, Object> props = Utils.mapFromDictionary(conf.getProperties());
                    logger.debug("Factory id for %s: %s", factoryPid, factoryIds.get(conf));
                    props.put(Configuration.FACTORY_ID, factoryIds.get(conf));
                    fConf.add(props);
                }
            }
        } catch (Exception e) {
            logger.warn("Could not load previous config from config admin", e);
        }       
        return previousConfig;
    }
	
	public void handleEvent(Event event) {
		if(event.getProperty("config.pid") != null) {
			logger.debug("Handling event: %s", event);
			configure();
		} else {
			logger.warn("Missing config.pid property in com/nokia/casr/ConfigEvent/UPDATED event");
		}
	}

	private void generateSystemPid(Configuration configuration) {
        if(configuration.config.get(SYSTEM_PID) == null) {
            logger.debug("Generating system entry");
            Map<String, Object> systemPidEntry = SystemPidGenerator.generateSystemPidEntry(systemPidSalt);
            logger.debug("System PID entry = %s", systemPidEntry);
            configuration.config.put(SYSTEM_PID, systemPidEntry);
        }
    }
}
