package testing;

// Jdk
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.TimerService;
import com.alcatel.as.util.serviceloader.ServiceLoader;

// public class Test implements Runnable {
public class PlatformExecutorTest2
{
    static Reactor reactor;
    static Logger _logger = Logger.getLogger("test");
    static AtomicInteger _scheduled = new AtomicInteger();
    static AtomicInteger _executed = new AtomicInteger();
    static AtomicInteger _cancelled = new AtomicInteger();

    static class Stat extends Thread
    {
        @Override
        public void run()
        {
            while (true)
            {
                try
                {
                    sleep(1000);
                    long scheduled = _scheduled.get();
                    _scheduled.set(0);
                    long cancelled = _cancelled.get();
                    _cancelled.set(0);
                    long executed = _executed.get();
                    _executed.set(0);
                    _logger.warn("scheduled: " + scheduled + ", cancelled=" + cancelled + ", executeed="
                            + executed);
                }
                catch (InterruptedException e)
                {
                }
            }
        }
    }

    public static void main(String args[]) throws Exception
    {
        long initialDelay = 0;
        long t1 = 40;
        boolean doLoad = false;

        if (args.length == 2)
        {
            initialDelay = Long.parseLong(args[0]);
            t1 = Long.parseLong(args[1]);
        }

        if (args.length == 3)
        {
            doLoad = Boolean.valueOf(args[2]);
        }

        ReactorProvider factory = ReactorProvider.provider();
        reactor = factory.newReactor("main", false, _logger);
        reactor.setName("main");
        reactor.start();

        PlatformExecutors execs = PlatformExecutors.getInstance();
        //final PlatformExecutor E = execs.getExecutor("main");
        final PlatformExecutor E = execs.getThreadPoolExecutor();

        final long begin = System.currentTimeMillis();
        E.schedule(new Runnable()
        {
            @Override
            public void run()
            {
                _logger.warn("timeout: " + (System.currentTimeMillis() - begin));
            }
        }, 802, TimeUnit.MILLISECONDS);

        if (!doLoad)
        {
            // test fixed delay periodic timer
            final Future fixedDelay = E.scheduleWithFixedDelay(new Runnable()
            {
                @Override
                public void run()
                {
                    _logger.warn("fixed delay timeout: " + (System.currentTimeMillis() - begin));
                }
            }, 500, 1000, TimeUnit.MILLISECONDS);

            E.schedule(new Runnable()
            {
                @Override
                public void run()
                {
                    _logger.warn("cancelling period task ... " + fixedDelay.cancel(false));
                    // test fixed rate periodic timer
                    final Future<?> fixedRate = E.scheduleAtFixedRate(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            _logger.warn("fixed rate timeout: " + (System.currentTimeMillis() - begin));
                            try
                            {
                                Thread.sleep(500);
                            }
                            catch (InterruptedException e)
                            {
                            }
                        }
                    }, 500, 1000, TimeUnit.MILLISECONDS);

                    E.schedule(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            _logger.warn("cancelling fixed rate timer:" + fixedRate.cancel(false));
                        }
                    }, 3000, TimeUnit.MILLISECONDS);
                }
            }, 4000, TimeUnit.MILLISECONDS);
        }

        // Perform a load test
        if (doLoad)
        {
            Stat stat = new Stat();
            stat.start();

            int M = 50000;
            final Random rnd = new Random();

            while (true)
            {
                Future[] tasks = new Future[M];
                for (int i = 0; i < M; i++)
                {
                    tasks[i] = E.schedule(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            _executed.incrementAndGet();
                        }
                    }, 3000 /*rnd.nextInt(200)*/, TimeUnit.MILLISECONDS);
                    _scheduled.incrementAndGet();
                }
                Thread.sleep(100);
                for (int i = 0; i < M; i++)
                {
                    if (tasks[i].cancel(false))
                    {
                        _cancelled.incrementAndGet();
                    }
                }
                if (M + 50000 <= 2000000)
                {
                    M += 50000;
                }
            }
        }

        Thread.sleep(Integer.MAX_VALUE);
    }
}
