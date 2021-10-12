// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.autoconfig.sys;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import com.nokia.as.autoconfig.AutoConfigurator;

import com.nokia.as.autoconfig.Configuration;
import com.nokia.as.autoconfig.Utils;

public class SystemPropertyResolver {
	
	private Configuration config;
	
	private BiConsumer<String, Object> consumeProperties = (key, value) -> {
	    String[] splitKey = key.split(":");
        if(splitKey.length != 2) return;
        
        String pid = splitKey[0];
        Map<String, Object> m = config.config.get(pid);
        if(m == null) config.config.put(pid, new HashMap<>());
        m = config.config.get(pid);
        m.put(splitKey[1], value);
	};
	        
	
	/*
	 * Resolves the system and environment properties:
	 *   system properties are the ones set by -D
	 *   environment properties are defined in the environment
	 *  The property is defined by pid:key=value
	 *  System properties take precedence over the environment properties
	 */
	public void resolve() {
		config = new Configuration();
		Map<String, Object> systemProps =
				 Utils.getSystemProperties()
					  .entrySet()
					  .stream()
					  .collect(Collectors.toMap(e -> (String) e.getKey(), 
							  					Entry::getValue));
		Map<String, Object> envProps = new HashMap<>();
		envProps.putAll(Utils.getEnvProperties());

		envProps.forEach(consumeProperties);
		systemProps.forEach(consumeProperties);
	}
	
	public Configuration config() {
		config.config.values().forEach(p -> p.put(Configuration.AUTOCONF_ID, "true"));
		return config;
	}
	
}
