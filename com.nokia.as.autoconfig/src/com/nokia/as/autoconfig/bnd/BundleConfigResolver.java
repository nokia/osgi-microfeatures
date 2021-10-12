// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.autoconfig.bnd;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;

import com.alcatel.as.service.log.LogService;
import com.nokia.as.autoconfig.AutoConfigurator;
import com.nokia.as.autoconfig.Configuration;
import com.nokia.as.autoconfig.Utils;
import com.nokia.as.autoconfig.parser.JsonParser;
import com.nokia.as.autoconfig.parser.Parser;
import com.nokia.as.autoconfig.parser.YamlParser;

import alcatel.tess.hometop.gateways.utils.Log;

public class BundleConfigResolver {
	
	private LogService logger = Log.getLogger(AutoConfigurator.LOGGER);
	private Map<String, List<VersionedSingletonConfiguration>> singletonConfigs = new HashMap<>();
	private Map<String, List<VersionedFactoryConfiguration>> factoryConfigs = new HashMap<>();
	
	private Map<String, Parser> parsers = new HashMap<>();
	
	public BundleConfigResolver() {
		parsers.put("yaml", new YamlParser());
		parsers.put("json", new JsonParser());
		logger.debug("Defined parsers for BundleResolver: %s", parsers.keySet().stream().collect(Collectors.joining(", ")));
	}
	
	public void resolve(Bundle bundle) {
		logger.debug("Resolving config bundle %s", bundle.getSymbolicName());
		String version = getVersion(bundle);
		List<URL> cfgFiles = filterConfFiles(bundle);
		
		logger.debug("Version = %s", version);
		logger.debug("Config files detected: %s", cfgFiles);
		
		Map<Boolean, List<VersionedConfiguration>> parsed = 
		cfgFiles.stream()
				.filter(f -> !f.toString().endsWith("/"))
				.filter(f -> Utils.getCorrectParser(f, parsers, logger).isPresent())
				.map(f -> Utils.getCorrectParser(f, parsers, logger).get().parse(f, version))
				.flatMap(f -> f.stream())
				.collect(Collectors.partitioningBy(VersionedFactoryConfiguration.class::isInstance));
			
		parsed.get(true)
			  .stream()
			  .map(VersionedFactoryConfiguration.class::cast)
			  .forEach(c -> {
				  factoryConfigs.putIfAbsent(c.pid, new ArrayList<>());
				  factoryConfigs.get(c.pid).add(c);
				  logger.debug("Adding factory config for pid %s, version %s", c.pid, c.version);
				  logger.trace("with properties %s", c.props);
			  });
		
		parsed.get(false)
			  .stream()
			  .map(VersionedSingletonConfiguration.class::cast)
			  .forEach(c -> {
				  singletonConfigs.putIfAbsent(c.pid, new ArrayList<>());
				  singletonConfigs.get(c.pid).add(c);
				  logger.debug("Adding config for pid %s, version %s", c.pid, c.version);
				  logger.trace("with properties %s", c.props);
			  });
	}
	
	public void remove(Bundle bundle) {
		logger.debug("Removing config bundle %s", bundle.getSymbolicName());
		String version = getVersion(bundle);
		List<URL> cfgFiles = filterConfFiles(bundle);
		
		logger.debug("Version = %s", version);
		logger.debug("Config files detected: %s", cfgFiles);

		Map<Boolean, List<VersionedConfiguration>> parsed = 
		cfgFiles.stream()
				.filter(f -> !f.toString().endsWith("/"))
				.filter(f -> Utils.getCorrectParser(f, parsers, logger).isPresent())
				.map(f -> Utils.getCorrectParser(f, parsers, logger).get().parse(f, version))
				.flatMap(f -> f.stream())
				.collect(Collectors.partitioningBy(VersionedFactoryConfiguration.class::isInstance));
		
		parsed.get(true)
			.stream()
			.map(VersionedFactoryConfiguration.class::cast)
			.forEach(c -> {
				factoryConfigs.get(c.pid).remove(c);
				logger.debug("Removing factory config for pid %s, version %s", c.pid, c.version);
			});
		
		parsed.get(false)
		.stream()
		.map(VersionedSingletonConfiguration.class::cast)
		.forEach(c -> {
			singletonConfigs.get(c.pid).remove(c);
			logger.debug("Removing config for pid %s, version %s", c.pid, c.version);
		});
	}
	
	private String getVersion(Bundle bundle) {
		String version = "0.0.0";
		try {
			BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
			List<BundleCapability> capabilities = bundleWiring.getCapabilities("com.nokia.as.conf");
			version = capabilities.get(0).getAttributes().get("v").toString();
		} catch(Exception e) {
			logger.warn("Assuming version 0.0.0 for %s", bundle.getSymbolicName());
		}

		return version;
	}
	
	private List<URL> filterConfFiles(Bundle bundle) {
		List<URL> files = new ArrayList<>();
		parsers.keySet().forEach(t -> {
			Enumeration<URL> found = bundle.findEntries("/", "*." + t, true);
			if(found != null) files.addAll(Collections.list(found));
		});
		
		return files;
	}
	
	public Configuration config() {
		Configuration config = new Configuration();
		singletonConfigs.forEach((k, v) -> {
			v.sort(Comparator.naturalOrder());
			for(VersionedSingletonConfiguration c : v) {
				config.config.put(k, c.props);
			}
		});
		
		factoryConfigs.forEach((k, v) -> {
			v.sort(Comparator.naturalOrder());
			for(VersionedFactoryConfiguration c : v) {
				config.factoryConfig.put(k, c.props);
			}
		});
		
		return config;
	}

}
