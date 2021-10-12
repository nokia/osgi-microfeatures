// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.autoconfig.parser;

import java.net.URL;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import com.alcatel.as.service.log.LogService;
import com.nokia.as.autoconfig.AutoConfigurator;
import com.nokia.as.autoconfig.Configuration;
import com.nokia.as.autoconfig.bnd.VersionedConfiguration;

import alcatel.tess.hometop.gateways.utils.Log;

public class CfgParser implements Parser {
	
	private LogService logger = Log.getLogger(AutoConfigurator.LOGGER);

	public List<VersionedConfiguration> parse(URL url, String version) {
		throw new UnsupportedOperationException("This operation is not available for this parser [CfgParser]");
	}

	public Map<String, Object> parseFile(URL url) {
		Map<String, Object> configuration = new HashMap<>();
		String filename = url.getFile();

		try (InputStream is = Files.newInputStream(Paths.get(filename))) {
			configuration = read(is);
			configuration.put(Configuration.AUTOCONF_ID, "true");
			return configuration;
		} catch(Exception e) {
			logger.warn("Error while parsing cfg %s", url);
			logger.debug("Error is ", e);
		}
		
		return Collections.emptyMap();
	}
	
	private Map<String, Object> read(InputStream is) throws IOException {
        Properties props = new Properties();
		props.load(is);
		return props.entrySet().stream()
							   .collect(Collectors.toMap(
							        e -> String.valueOf(e.getKey()),
									e -> String.valueOf(e.getValue())
							   ));
    }
}