/*
 * Copyright (c) 2006 Nokia. All rights reserved.
 */

package com.nokia.licensing.utils;

import java.util.logging.Level;


/**
 * Constants.
 */
public class Consts {

	/** Attribute name */
	public static final String ATTR_AUTHMETHOD = "authenticationMethod";

	/** Attribute name */
	public static final String ATTR_CLASS = "class";

	/** Attribute name */
	public static final String ATTR_DRIVER = "driver";

	/** Attribute name */
	public static final String ATTR_ERRORLOGGERNAME = "errorLoggerName";

	/** Attribute name */
	public static final String ATTR_ISCACHED = "isCached";

	/** Attribute name */
	public static final String ATTR_KEY = "key";

	/** Attribute name */
	public static final String ATTR_NAME = "name";

	/** Attribute name */
	public static final String ATTR_NODE = "node";

	/** Attribute name */
	public static final String ATTR_PASSWORD = "password";

	/** Cache refresh interval in minutes or seconds */
	public static final String ATTR_REFRESHINTERVAL = "refreshInterval";

	/** Tells whether cache refresh interval is minutes or seconds */
	public static final String ATTR_REFRESHINTERVALTYPE = "refreshIntervalType";

	/** Tells whether cache refresh interval is minutes or seconds */
	public static final String ATTR_REFRESHINTERVALTYPE_MINUTES = "Minutes";

	/** Tells whether cache refresh interval is minutes or seconds */
	public static final String ATTR_REFRESHINTERVALTYPE_SECONDS = "Seconds";

	/**
	 * XML tags and attributes for XML preferences file
	 */

	/** Attribute name */
	public static final String ATTR_SYSTEM = "system";

	/** Attribute name */
	public static final String ATTR_SYSTEMROOT = "systemRoot";

	/** Attribute name */
	public static final String ATTR_TRACELOGGERNAME = "traceLoggerName";

	/** Attribute name */
	public static final String ATTR_TYPE = "type";

	/** Attribute name */
	public static final String ATTR_URL = "url";

	/** Attribute name */
	public static final String ATTR_USER = "user";

	/** Attribute name */
	public static final String ATTR_USERNAME = "userName";

	/** Attribute name */
	public static final String ATTR_USERROOT = "userRoot";

	/** Attribute name */
	public static final String ATTR_VALUE = "value";

	/**
	 * The factor by which the interval is multiplied. If factor is 1, then the interval read from properties file is
	 * considered as milliseconds, 1000 means seconds, 60 * 1000 means minutes and 60 * 60 * 1000 means hours.
	 */
	public static final long CACHE_REFRESH_INTERVAL_FACTOR = 60 * 1000;

	/** For MercuryLogger */
	public static final String LOGGER_COMPONENT_NAME = "com.nokia.oss.cseprefs";

	/** Max properties file count */
	public static final int MAX_PROP_FILE_COUNT = 50;

	/** DTD URL for preferences files */
	public static final String PREF_DTD_URL = "http://java.sun.com/dtd/preferences.dtd";

	/** DTD file name for properties files */
	public static final String PROP_DTD_FILE = "preferences-properties.dtd";

	/** Properties file used if the main properties file could not be found */
	public static final String PROP_FILE_NAME_DEFAULT = "preferences-properties-default.xml";

	/** Main properties file (postfix) */
	public static final String PROP_FILE_NAME_POSTFIX = ".xml";

	/** Main properties file (prefix) */
	public static final String PROP_FILE_NAME_PREFIX = "preferences-properties-";

	/** Slash char */
	public static final String SLASH = "/";

	/** Large StringBuffer - 5120 characters */
	public static final int STRING_BUFFER_LARGE = 5120;

	/** Medium StringBuffer - 1024 characters */
	public static final int STRING_BUFFER_MEDIUM = 1024;

	// Different initial sizes for StringBuffers

	/** Small StringBuffer - 256 characters */
	public static final int STRING_BUFFER_SMALL = 256;

	/** The level which errors are logged */
	public static final java.util.logging.Level LOGGING_LEVEL = Level.ALL;

	/** Node name */
	public static final String TAG_CONFIGFILES = "configfiles";

	/** Attribute name */
	public static final String TAG_ENTRY = "entry";

	/** Node name */
	public static final String TAG_EXCLUDEDKEYS = "excludedkeys";

	/** Node name */
	public static final String TAG_FILES = "files";

	/** Node name */
	public static final String TAG_KEY = "key";

	/** Attribute name */
	public static final String TAG_MAP = "map";

	/** Attribute name */
	public static final String TAG_NODE = "node";

	/** Node name */
	public static final String TAG_PLUGIN = "plugin";

	/** Attribute name */
	public static final String TAG_PREFERENCES = "preferences";

	/**
	 * XML tags and attributes for properties file
	 */

	/** Node name */
	public static final String TAG_PROPERTIES = "properties";
}
