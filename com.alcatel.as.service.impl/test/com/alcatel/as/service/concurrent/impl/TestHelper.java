// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.concurrent.impl;

import java.util.Hashtable;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.TimerService;

public class TestHelper {

	private static volatile PlatformExecutors _execs;
	private static TimerService _strictTimer;
	private static TimerService _wheelTimer;

	public static void init(int ioTpoolSize, int processingTpoolSize) {
		Properties p = new Properties();
		p.setProperty("log4j.rootLogger", "WARN,stdout");
		p.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
		p.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
		p.setProperty("log4j.appender.stdout.layout.ConversionPattern", "%d{ISO8601} %t %-5p %c %x - %m%n");
		PropertyConfigurator.configure(p);

		Hashtable<String, Object> cnf = new Hashtable<>();
		p.put("system.tpool.size", String.valueOf(ioTpoolSize));
		p.put("system.processing-tpool.size", String.valueOf(processingTpoolSize));
		Standalone.init(cnf);
		_wheelTimer = Standalone.getWheelTimer();
		_strictTimer = Standalone.getJdkTimer();
		_execs = Standalone.getPlatformExecutors();
	}

	public static PlatformExecutors getPlatformExecutors() {
		return _execs;
	}
	
	public static TimerService getWheelTimer() {
		return _wheelTimer;
	}
	
	public static TimerService getStrictTimer() {
		return _strictTimer;
	}

}
