// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package testing;

// Jdk
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.concurrent.ThreadPool;
import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.TimerService;
import com.alcatel.as.util.serviceloader.ServiceLoader;

// public class Test implements Runnable {
public class PlatformExecutorTest3
{
    static Reactor reactor;
    static Logger _logger = Logger.getLogger("test");

    public static void main(String args[]) throws Exception
    {
        ReactorProvider factory = ReactorProvider.provider();
        reactor = factory.newReactor("main", false, _logger);
        reactor.setName("main");
        reactor.start();
        ThreadPool.getInstance().setSize(10);

        PlatformExecutors execs = PlatformExecutors.getInstance();
        //final PlatformExecutor E = execs.getExecutor("main");
        final PlatformExecutor E = execs.getThreadPoolExecutor();
        //final ScheduledThreadPoolExecutor E = new ScheduledThreadPoolExecutor(100);
        
        TimerService ts = ServiceLoader.getService(TimerService.class);
        Executor executor = new Executor() {
            @Override
            public void execute(Runnable command)
            {
               command.run();                
            }};
        
        Random rnd = new Random();
        final AtomicInteger timeouts = new AtomicInteger();
        for (int j = 0; j < 1000; j++)
        {
            for (int i = 0; i < 200; i++)
            {
                final long begin = System.currentTimeMillis();
                final long delay = rnd.nextInt(1000);
                //final Future<?> f = ThreadPool.getInstance().schedule(new Runnable()
                final Future<?> f = ts.schedule(executor, new Runnable()
                {
                    @Override
                    public void run()
                    {
                        long delta = System.currentTimeMillis() - begin;
                        if (delta < delay-1 || delta > delay + 64)
                        {
                            _logger.error("wrong timeout: expected expiration delay: " + delay
                                    + ", but actual expiration delay is " + delta);
                        }
                        timeouts.incrementAndGet();
                    }
                }, delay, TimeUnit.MILLISECONDS);
            }
            Thread.sleep(1);
        }

        _logger.warn("all timers scheduled");
        Thread.sleep(15000);
        _logger.warn("Test done, timeouts=" + timeouts);
        Thread.sleep(Integer.MAX_VALUE);
    }
}
