// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.concurrent.impl;

import java.util.Map;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.TimerService;

public class Standalone {
	private volatile static PlatformExecutorsImpl _pfExecsImpl;
	private volatile static JdkTimerServiceImpl _jdkTimerImpl;
	private volatile static WheelTimerServiceImpl _wheelTimerImpl;	

	public static synchronized void init(Map<String, Object> cnf) {
		if (_pfExecsImpl == null) {
			Meters meters = Helper.getStandaloneMeters();
			_jdkTimerImpl = new JdkTimerServiceImpl();
			_wheelTimerImpl = new WheelTimerServiceImpl();
			_pfExecsImpl = new PlatformExecutorsImpl();

			_jdkTimerImpl.setPlatformExecutors(_pfExecsImpl);	
			_jdkTimerImpl.bindMeters(meters);
			
			_wheelTimerImpl.setPlatformExecutors(_pfExecsImpl);	
			_wheelTimerImpl.bindMeters(meters);
			
			_pfExecsImpl.bindTimerService(_jdkTimerImpl);
			_pfExecsImpl.bindMeters(meters);
			
			_pfExecsImpl.start(cnf);		
			_wheelTimerImpl.start();
			_jdkTimerImpl.start(cnf);
		}	
	}
	
	public static PlatformExecutors getPlatformExecutors() {
		return _pfExecsImpl;
	}
	
	public static TimerService getWheelTimer() {
		return _wheelTimerImpl;
	}
	
	public static TimerService getJdkTimer() {
		return _jdkTimerImpl;
	}
}
