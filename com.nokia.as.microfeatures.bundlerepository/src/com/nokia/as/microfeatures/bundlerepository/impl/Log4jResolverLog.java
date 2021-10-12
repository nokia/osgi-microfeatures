// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.microfeatures.bundlerepository.impl;

import org.apache.log4j.Logger;

/**
 */
public class Log4jResolverLog extends org.apache.felix.resolver.Logger {

	private final Logger logger;

	public Log4jResolverLog(Logger logger) {
		super(getResolverLogLevel(logger));
		this.logger = logger;
	}

	private static int getResolverLogLevel(Logger logger) {
		if (logger.isDebugEnabled()) {
			return LOG_DEBUG;
		} else if (logger.isInfoEnabled()) {
			return (LOG_INFO);
		} else {
			return (LOG_WARNING);
		}
	}

	@Override
	protected void doLog(int level, String msg, Throwable throwable) {
		switch (level) {
		case LOG_ERROR:
			logger.error(msg, throwable);
			break;
		case LOG_WARNING:
			logger.warn(msg, throwable);
			break;
		case LOG_INFO:
			logger.info(msg, throwable);
			break;
		default:
			logger.debug(msg, throwable);
			break;
		}
	}
}
