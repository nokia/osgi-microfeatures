// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.log.impl.asrlog;

import org.apache.felix.dm.annotation.api.Component;

import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;

import alcatel.tess.hometop.gateways.utils.Log;

@Component
public class LogServiceFactoryImpl implements LogServiceFactory {
	public LogService getLogger(String name) {
		return Log.getLogger(name);
	}

	public LogService getLogger(Class<?> clazz) {
		return Log.getLogger(clazz);
	}
}
