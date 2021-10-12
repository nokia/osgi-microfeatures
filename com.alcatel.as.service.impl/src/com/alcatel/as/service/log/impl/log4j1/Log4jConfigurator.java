// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.log.impl.log4j1;

import static com.alcatel.as.util.config.ConfigConstants.HOST_NAME;
import static com.alcatel.as.util.config.ConfigConstants.INSTALL_DIR;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.xml.DOMConfigurator;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.FileDataProperty;
import com.alcatel_lucent.as.management.annotation.config.SelectProperty;

@Config(name = "log4j", section = "Log4j Configuration")
public class Log4jConfigurator {
	private final static String CONF_TYPE_XML = "xml";
	private final static String CONF_TYPE_PROPERTIES = "properties";

	@FileDataProperty(title = "Log4j configuration", help = "Configuration of Log4j loggers", fileData = "log4j.properties", dynamic = true, required = true)
	public final static String LOG4J_CONFIGURATION = "log4j.configuration";

	@SelectProperty(title = "Configuration type", help = "Defines the type of the log4jConfiguration. Valid values are properties or xml. " +
	                        "When you specify xml, instance/log4j.xml file will be used instead of default instance/log4j.properties", range = {
			CONF_TYPE_XML, CONF_TYPE_PROPERTIES }, defval = "properties", dynamic = true, required = false)
	public final static String CONF_TYPE = "log4j.configurationType";

//	@BooleanProperty(title = "Stdout redirection", help = "Redirect stdout to log4j", defval = true, dynamic = true, required = true)
//	public final static String REDIRECT_STDOUT = "log4j.redirectStdout";

	public Log4jConfigurator(boolean activateOSGiLogger, String log4jProperty) {
		_activateOSGiLogger = activateOSGiLogger;
		_log4jProperty = log4jProperty;
	}

	public void updated(Dictionary conf) {
		if (conf != null && _log4jConfig != null) {
			_logger.warn("Reconfiguring log4j ...");
			loadConfiguration(conf); // reconfigure
		}
		_log4jConfig = conf;
	}

	public void start() {
		_hostName = System.getProperty(HOST_NAME);
		if (_hostName == null) {
			_hostName = System.getProperty("platform.agent.host");// deprecated
		}

		loadConfiguration(_log4jConfig);
		_logger.info("Log4jConfigurator service started");
	}

	public void loadConfiguration(Dictionary conf) {
		String log4jConf = (String) conf.get(_log4jProperty);
		if (log4jConf == null) {
			return;
		}

		String log4jConfigType = (String) conf.get(CONF_TYPE);
		if (CONF_TYPE_XML.equals(log4jConfigType)) {
			try {
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				// ignore DTD ...
				dBuilder.setEntityResolver((publicId, systemId) -> {
					if (systemId.contains("log4j.dtd")) {
						return new InputSource(new StringReader(""));
					} else {
						return null;
					}
				});

				Document doc = null;
				doc = dBuilder.parse(new ByteArrayInputStream(log4jConf.getBytes(StandardCharsets.UTF_8)));
				DOMConfigurator.configure(doc.getDocumentElement());
			} catch (Exception e) {
				_logger.error("Can't load log4j xml configuration", e);
				return;
			}
		} else {
			Properties p = new Properties();
			try {
				p.load(new ByteArrayInputStream(log4jConf.getBytes("ASCII")));
			} catch (IOException ignored) {
			}

			if (_logger.isDebugEnabled()) {
				_logger.debug("Loading log4j configuration: " + p);
			}
			replaceVariables(p);

			resetLog4jConfiguration();
			PropertyConfigurator.configure(p);
		}

		// If we know that the OSGi property "ds.log.level, turn on osgi logger.
		if (_activateOSGiLogger) {
			Logger.getLogger("osgi").setLevel(Level.DEBUG);
		}

		_logger.info("Log4j configured");
	}

	private static void resetLog4jConfiguration() {
		// Reset all existing loggers
		Enumeration cats = LogManager.getCurrentLoggers();
		while (cats.hasMoreElements()) {
			Logger c = (Logger) cats.nextElement();
			c.setLevel(null);
			c.setAdditivity(true);
			c.setResourceBundle(null);
		}
		LoggerRepository rep = LogManager.getLoggerRepository();
		if (rep instanceof Hierarchy) {
			((Hierarchy) rep).getRendererMap().clear();
		}
	}

	private static String parse(Throwable e) {
		StringWriter buffer = new StringWriter();
		PrintWriter pw = new PrintWriter(buffer);
		e.printStackTrace(pw);
		return (buffer.toString());
	}

	private static String replace(String str, String pattern, String replace) {
		int s = 0;
		int e = 0;
		StringBuffer result = new StringBuffer();

		while ((e = str.indexOf(pattern, s)) >= 0) {
			result.append(str.substring(s, e));
			result.append(replace);
			s = e + pattern.length();
		}
		result.append(str.substring(s));
		return result.toString();
	}

	@SuppressWarnings("unchecked")
	private void replaceVariables(Properties p) {
		Enumeration e = p.keys();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			String val = p.getProperty(key);
			if (_hostName != null) {
				val = replace(val, "{" + HOST_NAME + "}", _hostName);
			}
			val = replace(val, "{" + INSTALL_DIR + "}", _instdir);
			p.setProperty(key, val);
		}
	}

	final static Logger _logger = Logger.getLogger("as.service.log4j.Log4jConfigurator");

	private Dictionary _log4jConfig;
	private String _log4jProperty;
	private boolean _activateOSGiLogger;
	private String _hostName;
	private String _instdir;
}
