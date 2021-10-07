package com.nokia.as.autoconfig.parser;

import java.net.URL;
import java.util.List;
import java.util.Map;

import com.nokia.as.autoconfig.bnd.VersionedConfiguration;

public interface Parser {

	public List<VersionedConfiguration> parse(URL url, String version);
	public Map<String, Object> parseFile(URL url);
}
