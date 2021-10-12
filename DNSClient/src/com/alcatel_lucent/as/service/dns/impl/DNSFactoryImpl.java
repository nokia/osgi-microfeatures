// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.service.dns.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.alcatel.as.service.reporter.api.AlarmService;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel.as.util.config.ConfigHelper;
import com.alcatel_lucent.as.service.dns.DNSClient;
import com.alcatel_lucent.as.service.dns.DNSFactory;

/**
 * This class is used to create a new instance of a DNSClient or a
 * TelURLResolver.
 */
public class DNSFactoryImpl extends DNSFactory {

	/**
	 * <code>serialVersionUID</code>
	 */

	private final static Logger MY_LOGGER = Logger
			.getLogger("dns.impl.factory");
	protected DNSProperties _properties = new DNSProperties();

	private volatile static AlarmService _alrmSrv;

	// Configurations
	protected final static String CACHE_ENABLED = "dns.cache.enabled";
	protected final static String NORESPONSE_TTL = "dns.cache.noresponse.ttl";
	protected final static String DNS_TIMEOUT = "dns.timeout";
	protected final static String ALARM_WM = "dns.alarmWaterMark";

	/**
	 * Gets a new instance of DNSClient.
	 * 
	 * @return A new instance of DNSClient or <code>null</code> if an
	 *         instanciation problem occurred.
	 */
	@Override
	public DNSClient newDNSClient() {
		if (MY_LOGGER.isDebugEnabled()) {
			MY_LOGGER.debug("Creating a new client ");
		}
		DNSClient client = null;
		try {
			client = new Client(_properties);
		} catch (Exception e) {
			if (MY_LOGGER.isEnabledFor(Level.ERROR)) {
				MY_LOGGER.error(
						"Cannot create a new client", e);
			}

		}

		return client;
	}

	/**
	 * 
	 * @param config
	 * @param _alrmSrv
	 */
	protected void init(Map<String, Object> config,
			Dictionary<String, Object> system,
			AlarmService alrmSrv) {
		_alrmSrv = alrmSrv;

		if (MY_LOGGER.isDebugEnabled()) {
			MY_LOGGER.debug("init conf=" + config);
		}

		String instanceName = ConfigHelper.getString(
				system, ConfigConstants.GROUP_NAME)
				+ "__"
				+ ConfigHelper.getString(system,
						ConfigConstants.INSTANCE_NAME);

		_properties.setInstanceName(instanceName);

		String enabled = (String) config
				.get("dns.cache.enabled");
		boolean cacheEnabled = ("true"
				.equalsIgnoreCase(enabled));
		_properties.setCacheEnabled(cacheEnabled);

		String sTimeout = (String) config
				.get("dns.timeout");
		try {
			int timeout = Integer.parseInt(sTimeout);
			_properties.setTimeout(timeout);
		} catch (Exception e) {
			if (MY_LOGGER.isInfoEnabled()) {
				MY_LOGGER
						.info("dns.timeout property is not a number -> use default value");
			}
		}

		String noResonseTtl = (String) config
				.get("dns.cache.noresponse.ttl");
		long ttl = 0L;
		try {
			ttl = Long.parseLong(noResonseTtl);
			_properties.setNotFoundTTL(ttl);
		} catch (Exception e) {
			if (MY_LOGGER.isInfoEnabled()) {
				MY_LOGGER
						.info("dns.cache.noresponse.ttl property is not a number -> use default value");
			}
		}

		String alarmWaterMark = (String) config
				.get("dns.alarmWaterMark");
		try {
			long seconds = 0;
			// If cache is enabled and TTL > 0, we must force alarm water mark
			// to 0 because
			// when response timeouts are cached, dns requests are sent only one
			// time.
			if (cacheEnabled && ttl > 0L) {
				MY_LOGGER
						.info("forcing alarm water mark to 0 because dns cache is used and TTL is > 0");
			} else {
				seconds = Long.parseLong(alarmWaterMark);
			}
			MY_LOGGER.info("Alarm water mark=" + seconds
					+ " seconds.");
			_properties.setAlarmWaterMark(seconds);
		} catch (Exception e) {
			if (MY_LOGGER.isInfoEnabled()) {
				MY_LOGGER
						.info("dns.alarmWaterMark property is not a number -> use default value");
			}
		}

		String etcHostsFile = DNSProperties.ETC_HOSTS;
		String resolvConfFile = DNSProperties.RESOLV_CONF;
		String nsswitchConfFile = DNSProperties.NS_SWITCH_CONF;
		String additionalServers = null;

		Properties properties = new Properties();
		File file = new File("dnsFactory.properties");
		if (file.exists() && file.isFile()) {

			FileInputStream iStream = null;
			try {
				iStream = new FileInputStream(file);
				properties.load(iStream);
				etcHostsFile = (String) properties
						.get("dns.files.hosts");
				resolvConfFile = (String) properties
						.get("dns.files.resolvconf");
				nsswitchConfFile = (String) properties
						.get("dns.files.nsswitchconf");
				additionalServers = (String) properties
						.get("dns.server.additionals");
			} catch (FileNotFoundException e) {
				if (MY_LOGGER.isDebugEnabled()) {
					MY_LOGGER
							.debug("init: dnsFactory.properties - File not found -> use default values");
				}
			} catch (IOException e) {
				if (MY_LOGGER.isDebugEnabled()) {
					MY_LOGGER
							.debug("init: cannot read the dnsFactory.properties file content -> use default values");
				}
			} finally {
				if (iStream != null) {
					try {
						iStream.close();
					} catch (IOException e) {
						if (MY_LOGGER.isDebugEnabled()) {
							MY_LOGGER
									.debug("init: cannot read the dnsFactory.properties file content -> use default values");
						}
					}
				}
			}

		} else {
			if (MY_LOGGER.isDebugEnabled()) {
				MY_LOGGER
						.debug("init: no properties file (dnsFactory.properties) -> use default values");
			}
		}
		_properties.setHostsFile(etcHostsFile);
		_properties.setResolvConfFile(resolvConfFile);
		_properties.setNsSwitchConfFile(nsswitchConfFile);
		_properties.setAdditionalServers(additionalServers);

		if (MY_LOGGER.isDebugEnabled()) {
			MY_LOGGER.debug("init: etcHostsFile="
					+ etcHostsFile + ", resolvConfFile="
					+ resolvConfFile
					+ ", nsswitchConfFile="
					+ nsswitchConfFile
					+ ", additionalServers="
					+ additionalServers);
		}
		changed(config);

	}

	protected void changed(Map<String, Object> config) {
		String mode = (String) config.get("dns.ipvmode");
		_properties.setIpvMode((mode == null) ? 10
				: Integer.parseInt(mode));
	}

	/**
   * 
   */
	public void start() {
		if (MY_LOGGER.isDebugEnabled()) {
			MY_LOGGER.debug("start");
		}
	}

	public void stop() {
		if (MY_LOGGER.isDebugEnabled()) {
			MY_LOGGER.debug("stop");
		}
	}

	public static AlarmService getAlarmService() {
		return _alrmSrv;
	}
}
