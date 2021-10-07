package testing;

import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import alcatel.tess.hometop.gateways.concurrent.QueueDispatcher;

import com.alcatel.as.service.concurrent.impl.*;

public class EventDispatcherTest
{
    final static Random _rnd = new Random();
    final static HashSet<Thread> _workers = new HashSet<Thread>();

    static synchronized void check()
    {
        if (!_workers.add(Thread.currentThread()))
        {
            System.out.println("\t !! Thread " + Thread.currentThread() + " reused by another actor !!");
        }
    }

    static class Actor implements Runnable
    {
        private String _name;
        private int _counter = 0;
        private QueueDispatcher _dispatcher;
        private CountDownLatch _latch;
        
        Actor(String name, QueueDispatcher dispatcher, CountDownLatch latch)
        {
            _name = name;
            _dispatcher = dispatcher;
            _latch = latch;
        }

        public String getName()
        {
            return _name;
        }

        @Override
        public void run()
        {
            try
            {
                Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
            }
            _counter++;
//            System.out.println("actor[" + _name + "] count[" + _counter + "] thread["
//                    + Thread.currentThread().getName() + "]");
            check();
            _latch.countDown();
        }
    }

    public static void main(String[] args) throws Exception
    {
        int N = Integer.parseInt(args[0]);
        ThreadPoolExecutor tp = (ThreadPoolExecutor) Executors.newFixedThreadPool(N);
        tp.prestartAllCoreThreads();
        QueueDispatcher dispatcher = new QueueDispatcher(tp, 1024);

        for (int j = 0; j < Integer.MAX_VALUE; j ++)
        {
            CountDownLatch latch = new CountDownLatch(N);
            for (int i = 0; i < N; i++)
            {
                Actor actor = new Actor("Actor-" + _rnd.nextInt(100000) + "-" + i, dispatcher, latch);
                dispatcher.dispatch(actor.getName(), actor);
            }
            latch.await();
            _workers.clear();
            System.out.println("Iteration " + j);
        }
    }
}
