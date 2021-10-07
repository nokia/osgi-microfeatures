package testing;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.utils.Config;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.metering.Counter;

public class TestRandom
{
    static Logger _logger = Logger.getLogger("test");
    private static Counter _statDelta;

    public static void main(String args[]) throws Exception
    {
        ReactorProvider factory = ReactorProvider.provider();
        Reactor reactor = factory.newReactor("ReactorThread", false, _logger);
        reactor.setName("main");
        factory.aliasReactor("sip", reactor);
        reactor.start();

        PlatformExecutors execs = PlatformExecutors.getInstance();
        Executor executor = new Executor()
        {
            @Override
            public void execute(Runnable command)
            {
                command.run();
            }
        };
        Config system = new Config("system.properties");
        //MeteringService metering = new MeteringServiceImplStandalone(system);
        //_statDelta = metering.getCounter("as.stat.delta");

        Thread.sleep(100);

        int N = Integer.valueOf(args[0]);
        long percentageToCancel = Long.parseLong(args[1]);
        long t1 = System.currentTimeMillis();
        Future[] futures = new Future[N];
        for (int i = 0; i < N; i++)
        {
            final long delay = 7000;
            final long t = System.currentTimeMillis();
            futures[i] = execs.getThreadPoolExecutor("bob").schedule(new Runnable()
            {
                @Override
                public void run()
                {
                    long actualDelay = System.currentTimeMillis() - t;
                    long delta = actualDelay - delay;
                    //_logger.warn("timeout: delta=" + delta);
                    _statDelta.add(delta);
                }
            }, delay, TimeUnit.MILLISECONDS);
        }
        long toCancel = N * percentageToCancel / 100;
        long t2 = System.currentTimeMillis();
        _logger.warn("all timers scheduled (" + (t2 - t1) + "), will cancel " + toCancel);
        for (int i = 0; i < toCancel; i++)
        {
            futures[i].cancel(false);
        }
        Thread.sleep(Integer.MAX_VALUE);
    }
}
