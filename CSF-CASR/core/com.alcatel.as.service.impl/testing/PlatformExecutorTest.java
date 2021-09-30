package testing;

// Jdk
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;

import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;

// public class Test implements Runnable {
public class PlatformExecutorTest implements Runnable
{
    static Reactor reactor;
    static Logger _logger = Logger.getLogger("test");

    public static void main(String args[]) throws Exception
    {
        ReactorProvider factory = ReactorProvider.provider();
        reactor = factory.create("MyReactor");
        factory.aliasReactor("SipReactor", reactor);
        reactor.start();

        ScheduledFuture<String> sf = reactor.schedule(new Callable<String>()
        {
            @Override
            public String call() throws Exception
            {
                // TODO Auto-generated method stub
                return "OK";
            }
        }, 3000, TimeUnit.MILLISECONDS);
        _logger.warn("sf.getDelay=" + sf.getDelay(TimeUnit.MILLISECONDS));
        _logger.warn("getting sf ...");
        _logger.warn(sf.get());        
        _logger.warn("sf.getDelay=" + sf.getDelay(TimeUnit.MILLISECONDS));

        PlatformExecutors execs = PlatformExecutors.getInstance();
        ScheduledExecutorService E = execs.getExecutor("SipReactor");
        ScheduledFuture<String> f = E.schedule(new Callable<String>()
        {
            public String call()
            {
                try
                {
                    dump("MainReactor");
                    Thread.currentThread().sleep(1000);
                }
                catch (Exception e)
                {
                }
                return "R";
            }
        }, 1000, TimeUnit.MILLISECONDS);
        System.out.println("Waiting for f.get()");
        System.out.println("Call returns " + f.get());

        E.execute(new PlatformExecutorTest());
        Thread.sleep(Integer.MAX_VALUE);
    }

    public PlatformExecutorTest()
    {
    }

    static synchronized void dump(String msg)
    {
        PlatformExecutors execs = PlatformExecutors.getInstance();
        _logger.warn(msg);
        PlatformExecutor.WorkerThread wt = execs.getCurrentThreadContext().getCurrentWorkerThread();
        _logger.warn("currThreadContext.workerthread=" + ((wt != null) ? wt.getId() : null));
        PlatformExecutor curr = execs.getExecutor(PlatformExecutors.CURRENT_EXECUTOR);
        dump(curr, "dumping curr executor");
        curr = execs.getExecutor(PlatformExecutors.CURRENT_ROOT_EXECUTOR);
        dump(curr, "dumping current root executor");
    }

    static void dump(PlatformExecutor e, String msg)
    {
        _logger.warn(" - " + msg);
        if (e == null)
        {
            _logger.warn("\tNULL EXECUTOR");
            return;
        }
        _logger.warn("\texecutor.getId=" + e.getId());
        _logger.warn("\texecutor.isThreadPool=" + e.isThreadPoolExecutor());
    }

    public void run()
    {
        try
        {
            final PlatformExecutors execs = PlatformExecutors.getInstance();
            _logger.warn("PDR1: curr th=" + Thread.currentThread().getName());

            ScheduledExecutorService STPE = execs.getThreadPoolExecutor();
            ScheduledFuture<String> f = STPE.schedule(new Callable<String>()
            {
                public String call()
                {
                    dump("R1");
                    try
                    {
                        Thread.currentThread().sleep(1000);
                    }
                    catch (Exception e)
                    {
                    }
                    return "R1";
                }
            }, 1000, TimeUnit.MILLISECONDS);
            System.out.println("Call returns " + f.get());

            dump("Main Executor");
            final PlatformExecutor CURR = execs.getCurrentThreadContext().getCurrentExecutor();
            PlatformExecutor TP = execs.getThreadPoolExecutor();
            System.out.println("CURR=" + CURR);
            
            TP.execute(new Runnable()
            {
                public void run()
                {
                    _logger.warn("PDR2: curr th=" + Thread.currentThread().getName());
                    CURR.execute(new Runnable()
                    {
                        public void run()
                        {
                            _logger.warn("PDR3: curr th=" + Thread.currentThread().getName());
                        }
                    }, ExecutorPolicy.INLINE);
                }
            });

            TP = execs.getThreadPoolExecutor("bob");

            final long now = System.currentTimeMillis();
            TP.schedule(new Runnable()
            {
                public void run()
                {
                    _logger.warn(System.currentTimeMillis() - now);
                    dump("TEST");
                }
            }, 5, TimeUnit.SECONDS);

            Thread.sleep(10000);

            Callable<String> c1 = new Callable<String>()
            {
                public String call()
                {
                    dump("R1");
                    try
                    {
                        Thread.currentThread().sleep(1000);
                    }
                    catch (Exception e)
                    {
                    }
                    return "R1";
                }
            };

            Callable<String> c2 = new Callable<String>()
            {
                public String call()
                {
                    dump("R2");
                    try
                    {
                        Thread.currentThread().sleep(1000);
                    }
                    catch (Exception e)
                    {
                    }
                    return "R2";
                }
            };

            ScheduledFuture<String> ft1 = TP.schedule(c1, 2000, TimeUnit.MILLISECONDS);
            ScheduledFuture<String> ft2 = TP.schedule(c2, 2000, TimeUnit.MILLISECONDS);

            _logger.warn("Waiting for c1.call ...");
            _logger.warn("->c1=" + ft1.get());
            _logger.warn("Waiting for c2.call ...");
            _logger.warn("->c2=" + ft2.get());

            PlatformExecutor cb = execs.getExecutor(PlatformExecutors.CURRENT_CALLBACK_EXECUTOR);
            _logger.warn("curr cb executor id=" + cb.getId());
            cb.execute(new Runnable()
            {
                public void run()
                {
                    dump("CB1");
                }
            });

            final Future future = TP.scheduleAtFixedRate(new Runnable()
            {
                public void run()
                {
                    _logger.warn("TIMEOUT/PERIODIC" + ": " + ": curr thread=" + Thread.currentThread());
                }
            }, 3L, 3L, TimeUnit.SECONDS);

            TP.schedule(new Runnable()
            {
                public void run()
                {
                    _logger.warn("TIMEOUT" + ": " + ": curr thread=" + Thread.currentThread());
                    future.cancel(false);
                }
            }, 10, TimeUnit.SECONDS);

            PlatformExecutor tp = execs.getExecutor(PlatformExecutors.THREAD_POOL_EXECUTOR);
            tp.execute(new Runnable()
            {
                public void run()
                {
                    dump("CB2");
                }
            });

            tp = execs.getExecutor(PlatformExecutors.THREAD_POOL_EXECUTOR + "." + "bob");
            tp.execute(new Runnable()
            {
                public void run()
                {
                    dump("BOB1");
                    try
                    {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e)
                    {
                    }
                }
            });
            tp.execute(new Runnable()
            {
                public void run()
                {
                    dump("BOB2");
                    PlatformExecutor cb = execs.getExecutor(PlatformExecutors.CURRENT_CALLBACK_EXECUTOR);
                    cb.execute(new Runnable()
                    {
                        public void run()
                        {
                            dump("BOB2/callback1");
                            try
                            {
                                Thread.sleep(1000);
                            }
                            catch (InterruptedException e)
                            {
                            }
                        }
                    });
                    try
                    {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException e)
                    {
                    }
                    cb.execute(new Runnable()
                    {
                        public void run()
                        {
                            dump("BOB2/callback2");
                        }
                    });
                }
            });
        }

        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }
}
