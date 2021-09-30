package com.alcatel.as.service.concurrent.impl;

import com.alcatel.as.service.concurrent.TimerService;

public class JdkTimerServiceTest extends BaseTimerServiceTest
{
    private static TimerService _ts;

    public JdkTimerServiceTest()
    {
        super("AsrCoreTest-Concurrent-JdkTimerService-");
        TestHelper.init(10,  0);
        _ts = TestHelper.getStrictTimer();
    }

    @Override
    protected TimerService getTimerService()
    {
        return _ts;
    }
}
