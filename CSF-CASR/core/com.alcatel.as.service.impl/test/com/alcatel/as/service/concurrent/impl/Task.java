package com.alcatel.as.service.concurrent.impl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.TimerService;

public class Task implements Runnable
{
    private final static AtomicInteger _index = new AtomicInteger();
    final static Logger _logger = Logger.getLogger(Task.class);

    private final long _delay;
    private final int _id;
    private volatile long _scheduleTime;
    private volatile long _expirationTime = -1;
    private volatile ScheduledFuture<?> _future;
    private volatile boolean _shouldBeCancelled;
    private final Runnable _runnable;
    private final CountDownLatch _latch;

    Task(long delay)
    {
        _delay = delay;
        _id = _index.incrementAndGet();
        _latch = null;
        _runnable = null;
    }

    Task(long delay, Runnable r)
    {
        _delay = delay;
        _id = _index.incrementAndGet();
        _latch = null;
        _runnable = r;
    }

    Task(long delay, CountDownLatch latch)
    {
        _delay = delay;
        _id = _index.incrementAndGet();
        _latch = latch;
        _runnable = null;
    }

    public String toString()
    {
        return _expirationTime != -1 ? ("Timer[" + _id + "]: scheduleTime= " + _scheduleTime + ", delay="
                                            + _delay + ", actual delay= " + (_expirationTime - _scheduleTime)
                                            + ", delta= " + ((_expirationTime - _scheduleTime) - _delay)
                                            + ", expect expiry time= " + (_scheduleTime + _delay)
                                            + ", expirationTime= " + _expirationTime)
                                    : ("Timer[" + _id + "]: scheduleTime= " + _scheduleTime + ", delay= "
                                            + _delay + ", expect expiry time= " + (_scheduleTime + _delay));
    }

    Future<?> schedule(TimerService ts, Executor exec)
    {
        _scheduleTime = System.currentTimeMillis();
        _future = ts.schedule(exec, this, _delay, TimeUnit.MILLISECONDS);
        return _future;
    }

    @Override
    public void run()
    {
        _expirationTime = System.currentTimeMillis();
        if (_runnable != null)
        {
            _runnable.run();
        }
        if (_latch != null)
        {
            _latch.countDown();
        }
    }

    public boolean hasExpired()
    {
        return _expirationTime != -1;
    }

    public long getDelay()
    {
        return _delay;
    }

    public long getScheduleTime()
    {
        return _scheduleTime;
    }

    public long getExpirationTime()
    {
        return _expirationTime;
    }

    public long getDelta()
    {
        return (_expirationTime - _scheduleTime) - _delay;
    }

    public boolean cancel()
    {
        boolean cancelled = _future.cancel(false);
        if (cancelled)
        {
            _shouldBeCancelled = true;
        }
        return cancelled;
    }

    public boolean shouldBeCancelled()
    {
        return _shouldBeCancelled;
    }

    public boolean isCancelled()
    {
        return _future.isCancelled();
    }

    public Future<?> getFuture()
    {
        return _future;
    }
}
