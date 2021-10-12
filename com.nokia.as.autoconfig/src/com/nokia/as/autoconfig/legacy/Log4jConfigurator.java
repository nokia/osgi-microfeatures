// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.autoconfig.legacy;

import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.spi.LoggerRepository;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.nokia.as.autoconfig.AutoConfigurator;

import alcatel.tess.hometop.gateways.utils.Log;

public class Log4jConfigurator {

	private LogService logger = Log.getLogger(AutoConfigurator.LOGGER);
	private ServiceRegistration<LoggerRepository> loggerRepository = null;
	private ServiceRegistration<LogServiceFactory> logServiceFactory = null;
	
	private class LogServiceFactoryImpl implements LogServiceFactory {
		public LogService getLogger(String name) {
			return Log.getLogger(name);
		}

		public LogService getLogger(Class<?> clazz) {
			return Log.getLogger(clazz);
		}
	}
	
	public void registerServices(BundleContext bc, Map<String, Map<String, Object>> props) {
		logger.debug("Registering log4j services");
		if(!props.containsKey("log4j") && bc != null) {
			loggerRepository = bc.registerService(LoggerRepository.class, LogManager.getLoggerRepository(), null);
			logServiceFactory = bc.registerService(LogServiceFactory.class, new LogServiceFactoryImpl(), null);
		}
	}
	
	public void unregisterServices(BundleContext bc) {
		logger.debug("Unregistering log4j services");
		if(loggerRepository != null) loggerRepository.unregister();
		if(logServiceFactory != null) logServiceFactory.unregister();
	}
}
