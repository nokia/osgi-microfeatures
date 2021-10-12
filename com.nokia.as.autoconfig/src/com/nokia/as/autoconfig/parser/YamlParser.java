// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.autoconfig.parser;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.alcatel.as.service.log.LogService;
import com.nokia.as.autoconfig.AutoConfigurator;
import com.nokia.as.autoconfig.Configuration;
import com.nokia.as.autoconfig.bnd.VersionedConfiguration;
import com.nokia.as.autoconfig.bnd.VersionedFactoryConfiguration;
import com.nokia.as.autoconfig.bnd.VersionedSingletonConfiguration;

import alcatel.tess.hometop.gateways.utils.Log;

public class YamlParser implements Parser {
	
	private LogService logger = Log.getLogger(AutoConfigurator.LOGGER);

	private Yaml yamlParser = new Yaml();
	
	@SuppressWarnings("unchecked")
	public List<VersionedConfiguration> parse(URL url, String version) {
		List<VersionedConfiguration> vConfs = new ArrayList<>();
		try(InputStream is = url.openStream()) {
			
			Map<String, Object> yaml = yamlParser.load(is);
			yaml.keySet().forEach(pid -> {
				if(yaml.get(pid) instanceof List) {
					VersionedFactoryConfiguration config = new VersionedFactoryConfiguration(version, pid);
					List<Map<String, Object>> factoryProps = (List<Map<String, Object>>) yaml.get(pid);
					
					factoryProps.forEach(p -> {
						p.put(Configuration.AUTOCONF_ID, "true");
						config.props.add(p);
					});
					
					vConfs.add(config);
				} else {
					VersionedSingletonConfiguration config = new VersionedSingletonConfiguration(version, pid);
					Map<String, Object> props = (Map<String, Object>) yaml.get(pid);
					props.put(Configuration.AUTOCONF_ID, "true");
					config.props.putAll(props);
					vConfs.add(config);
				}
			});			
		} catch (Exception e) {
			logger.warn("Error while parsing configuration from yaml %s", url);
			logger.debug("Error is ", e);
		}
		return vConfs;
	}
	
	public Map<String, Object> parseFile(URL url) {
		try(InputStream is = url.openStream()) {
			Map<String, Object> parsed = yamlParser.load(is);
			parsed.put(Configuration.AUTOCONF_ID, "true");
			return parsed;
		} catch (Exception e) {
			logger.warn("Error while parsing configuration from yaml %s", url);
			logger.debug("Error is ", e);
		}
		return Collections.emptyMap();
	}

}
