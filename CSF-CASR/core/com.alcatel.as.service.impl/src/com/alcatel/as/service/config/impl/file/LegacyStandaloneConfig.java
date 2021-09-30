package com.alcatel.as.service.config.impl.file;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;

import alcatel.tess.hometop.gateways.utils.Config;

public class LegacyStandaloneConfig extends StandaloneConfig
{
    // The legacy Config object we'll provide (it will contain all known PID dictionaries).
    private Config _config;

    public LegacyStandaloneConfig(String dir, String period, String log4jPid)
    {
        super(dir, period, log4jPid);
    }

    @Override protected void propertiesUpdated(String pids) {
      super.propertiesUpdated(pids); // otherwise not detected by gogo command parser!
    }
    
    @Override
    protected synchronized void doScan()
    {
        // Delegate the actual scan to our super class
        super.doScan();

        // Provide the legacy config object, as well as the "globalConfig" pid (for pxlet deployer)
        if (_config == null)
        {
            _config = new Config();

            // Copy all known dictionaries into this global config object
            for (ConfigEntry entry : _configs.values())
            {
                Dictionary<String, Object> dict = entry.getProperties();
                for (String key : Collections.list(dict.keys()))
                {
                    _config.put(key, dict.get(key));
                }
            }

            // Provide the legacy Config into the OSGi registry.
            _bc.registerService(Config.class.getName(), _config, null);

            // Provide the legacy "globalConfig" pid
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("service.pid", "globalConfig");
            _bc.registerService(Dictionary.class.getName(), _config, props);
        }
    }
}
