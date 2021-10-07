package com.nokia.as.thirdparty.jaeger.impl;

import java.time.Instant;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import io.jaegertracing.internal.clock.Clock;

public class PreciseClock implements Clock {
    @Override
    public long currentTimeMicros()
    {
        Instant lNow = Instant.now();
        
        return TimeUnit.MICROSECONDS.convert(lNow.toEpochMilli(), TimeUnit.MILLISECONDS) +
                lNow.getLong(ChronoField.MICRO_OF_SECOND);
    }
    @Override
    public long currentNanoTicks()
    {
        return System.nanoTime();
    }

    @Override
    public boolean isMicrosAccurate()
    {
        return true;
    }
} 

