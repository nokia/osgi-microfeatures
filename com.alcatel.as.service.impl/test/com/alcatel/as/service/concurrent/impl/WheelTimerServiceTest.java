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
