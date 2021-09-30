/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */
package com.nokia.licensing.querycache;

import java.io.InputStream;
import java.util.HashMap;

import com.nokia.licensing.interfaces.CLJLPreferences;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.logging.LicenseLogger;
import com.nokia.licensing.plugins.PluginRegistry;


/**
 *
 * @author chhgupta
 *
 */
public class QueryRetrieval {

	private static HashMap<String, String> cache = new HashMap<>();

	/**
	 * This class has the method to retrieve the SQL query based on the keyword (method name)
	 */
	public static String getSQLData(final String id) {
		if (!cache.containsKey(id)) {
			readSQLConfig(getSQLFileName());
		}
		return cache.get(id);
	}

	private static void readSQLConfig(final String fileName) {
		final InputStream inputStream = QueryRetrieval.class.getClassLoader().getResourceAsStream(fileName);
		final String schema = getSchema();
		cache.putAll(new LicenseSQLParser().parseSQLFile(inputStream, schema));
	}

	private static String getSQLPluginConfigurationElement(final String keyName, final String defaultValue) {
		try {
			final CLJLPreferences cljlPreferences = PluginRegistry.getRegistry().getPlugin(CLJLPreferences.class);
			return cljlPreferences.getPreferencesSystemRoot().node("SQLPlugin").get(keyName, defaultValue);
		} catch (final LicenseException e) {
			LicenseLogger.getInstance().error(QueryRetrieval.class.getName(), "get" + keyName, e.getMessage());
			return defaultValue;
		}
	}

	private static String getSQLFileName() {
		return getSQLPluginConfigurationElement("SQLQueryList", "LicenseSQL.xml");
	}

	private static String getSchema() {
		return getSQLPluginConfigurationElement("schema", "LICENSE");
	}
}
