package com.alcatel_lucent.as.service.dns.impl;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import com.alcatel.as.service.reporter.api.AlarmService;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel.as.util.serviceloader.ServiceLoader;

public class DNSFactoryImplStandalone extends
		DNSFactoryImpl {
	private final static Map<String, Object> DEFAULT_CONF = new HashMap<String, Object>() {
		private static final long serialVersionUID = 1L;

		{
			put(CACHE_ENABLED, "true");
			put(NORESPONSE_TTL, "0");
			put(DNS_TIMEOUT, "2");
			put(DNS_TIMEOUT, "5");
		}
	};

	private final static Hashtable<String, Object> _systemConfig = new Hashtable<String, Object>() {
		private static final long serialVersionUID = 1L;

		{
			put(ConfigConstants.GROUP_NAME, "dns"); // whatever instancename,
													// not used
			put(ConfigConstants.GROUP_NAME, "group"); // whatever groupname, not
														// used
		}
	};

	public DNSFactoryImplStandalone() {
		this(DEFAULT_CONF);
	}

	public DNSFactoryImplStandalone(Map<String, Object> cnf) {
		super.init(cnf, _systemConfig, ServiceLoader
				.getService(AlarmService.class));
		super.start();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public DNSFactoryImplStandalone(Dictionary cnf) {
		Map<String, Object> config = new HashMap<String, Object>();
		for (Enumeration<String> keys = cnf.keys(); keys
				.hasMoreElements();) {
			String key = keys.nextElement();
			config.put(key, cnf.get(key));
		}
		super.init(config, _systemConfig, ServiceLoader
				.getService(AlarmService.class));
		super.start();
	}
}
