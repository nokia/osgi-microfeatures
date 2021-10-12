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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.alcatel.as.service.log.LogService;
import com.nokia.as.autoconfig.AutoConfigurator;
import com.nokia.as.autoconfig.Configuration;
import com.nokia.as.autoconfig.bnd.VersionedConfiguration;
import com.nokia.as.autoconfig.bnd.VersionedFactoryConfiguration;
import com.nokia.as.autoconfig.bnd.VersionedSingletonConfiguration;

import alcatel.tess.hometop.gateways.utils.Log;

public class JsonParser implements Parser {
	
	private LogService logger = Log.getLogger(AutoConfigurator.LOGGER);
	
	public List<VersionedConfiguration> parse(URL url, String version) {
		List<VersionedConfiguration> vConfs = new ArrayList<>();
		try(InputStream is = url.openStream()) {
			JSONTokener tokener = new JSONTokener(is);
			JSONObject root = new JSONObject(tokener);
			
			String[] pids = JSONObject.getNames(root);
			for(String pid : pids) {
				if(root.get(pid) instanceof JSONArray) {
					VersionedFactoryConfiguration config = new VersionedFactoryConfiguration(version, pid);
					JSONArray factoryProps = (JSONArray) root.get(pid);
					
					for(int i = 0; i < factoryProps.length(); i++) {
						JSONObject jsonProps = factoryProps.getJSONObject(i);
						Map<String, Object> props = new HashMap<>();
						String[] keys = JSONObject.getNames(jsonProps);
						for(String key : keys) {
							props.put(key, jsonProps.getString(key));
						}
						props.put(Configuration.AUTOCONF_ID, "true");
						config.props.add(props);
					}
					
					vConfs.add(config);
				} else {
					VersionedSingletonConfiguration config = new VersionedSingletonConfiguration(version, pid);
					JSONObject props = (JSONObject) root.get(pid);
					
					String[] keys = JSONObject.getNames(props);
					for(String key : keys) {
						config.props.put(key, props.getString(key));
					}
					props.put(Configuration.AUTOCONF_ID, "true");
					
					vConfs.add(config);
				}
			}
		} catch (Exception e) {
			logger.warn("Error while parsing configuration from json %s", url);
			logger.debug("Error is ", e);
		}
		return vConfs;
	}
	
	public Map<String, Object> parseFile(URL url) {
		try(InputStream is = url.openStream()) {
			JSONTokener tokener = new JSONTokener(is);
			JSONObject root = new JSONObject(tokener);
			
			Map<String, Object> props = new HashMap<>();
			String[] keys = JSONObject.getNames(root);
			for(String key : keys) {
				props.put(key, root.getString(key));
			}
			props.put(Configuration.AUTOCONF_ID, "true");
			return props;
		} catch (Exception e) {
			logger.warn("Error while parsing configuration from json %s", url);
			logger.debug("Error is ", e);
		}
		return Collections.emptyMap();
	}
}
