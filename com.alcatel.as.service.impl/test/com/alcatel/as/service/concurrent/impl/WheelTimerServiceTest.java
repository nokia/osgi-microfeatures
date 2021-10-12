// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.concurrent.impl;

import com.alcatel.as.service.concurrent.TimerService;

public class WheelTimerServiceTest extends BaseTimerServiceTest {
	private static TimerService _ts;

	public WheelTimerServiceTest() {
		super("AsrCoreTest-Concurrent-WheelTimerService-");
		TestHelper.init(10, 0);
		_ts = TestHelper.getWheelTimer();
	}

	@Override
	protected TimerService getTimerService() {
		return _ts;
	}
}
