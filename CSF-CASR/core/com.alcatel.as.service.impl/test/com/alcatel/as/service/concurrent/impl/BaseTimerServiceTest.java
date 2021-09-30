package com.alcatel.as.service.concurrent.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.TimerService;

import junit.framework.TestCase;

public abstract class BaseTimerServiceTest extends TestCase {
	final static Logger _logger = Logger.getLogger(BaseTimerServiceTest.class);
	final static Executor _tpool = Executors.newFixedThreadPool(1);
	final String _descPrefix;

	final Executor _inlineExecutor = new Executor() {
		public void execute(Runnable command) {
			command.run();
		}
	};

	BaseTimerServiceTest(String descPrefix) {
		_descPrefix = descPrefix;
	}

	protected abstract TimerService getTimerService();

	@SuppressWarnings("static-access")
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		_logger.getLogger("as.stat").setLevel(Level.WARN);
		_logger.info("Initializing test: " + this);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		_logger.info("Tearing down ...");
	}

	// ------------------------------- junit tests

	public void testFixedDelayCancel() throws InterruptedException {
		super.setName(_descPrefix + "002 Fixed Delay Timer cancel test");
		final TimerService ts = getTimerService();

		final AtomicInteger count = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<Future<?>> futureRef = new AtomicReference<Future<?>>();
		Runnable task = () -> {
			_logger.warn("timeout 1");
			if (count.incrementAndGet() == 3) {
				futureRef.get().cancel(false);
				latch.countDown();
			}
		};

		futureRef.set(ts.scheduleWithFixedDelay(_tpool, task, 50, 50, TimeUnit.MILLISECONDS));
		assertTrue("test failed", latch.await(3000, TimeUnit.MILLISECONDS));
		sleep(100);
		assertTrue("test failed", count.get() == 3);
	}

	/**
	 * Checks that the TimerService methods are using the proper current thread
	 * executor.
	 */
	public void testTimersWithCurrentThreadExecutor() {
		super.setName(_descPrefix + "003 Timers With current thread executor test");

		final TimerService ts = getTimerService();
		final PlatformExecutors execs = TestHelper.getPlatformExecutors();
		final PlatformExecutor queue = execs.createQueueExecutor(execs.getProcessingThreadPoolExecutor());
		final CountDownLatch done = new CountDownLatch(1);

		Runnable task = () -> {
			_logger.warn("timeout 2");
			if (execs.getCurrentThreadContext().getCurrentExecutor().equals(queue)) {
				done.countDown();
			} else {
				_logger.warn("timer executed in wrong executor");
			}
		};

		queue.execute(() -> {
			ts.schedule(task, 10, TimeUnit.MILLISECONDS);
		});
		await(done, 3000);
	}

	/**
	 * Basic load test: we fire many timers, and check for proper expiration time.
	 */
	public void testWarm() {
		super.setName(_descPrefix + "004 Warm test");
		final TimerService ts = getTimerService();
		final Random rnd = new Random();
		final int TASKS = 100;
		final int THREADS = 10;
		Thread[] threads = new Thread[THREADS];

		for (int i = 0; i < 1; i++) {
			final Sequencer seq = new Sequencer();
			long t1 = System.currentTimeMillis();
			for (int j = 0; j < threads.length; j++) {
				threads[j] = new Thread() {
					@Override
					public void run() {
						Task[] tasks = new Task[TASKS / THREADS];
						for (int k = 0; k < tasks.length; k++) {
							tasks[k] = new Task(rnd.nextInt(1000), seq);
							tasks[k].schedule(ts, _tpool);
						}
					}
				};
				threads[j].start();
			}
			seq.waitForStep(TASKS, 20000);
			long t2 = System.currentTimeMillis();
			_logger.warn("scheduled " + TASKS + " timers in " + (t2 - t1) + " millis.");
		}
	}

	/**
	 * Basic load test: we fire many timers, and check for proper expiration time.
	 */
	public void testLoad() {
		super.setName(_descPrefix + "005 Timer load test");
		final TimerService ts = getTimerService();
		final int TIMERS = 10;
		final int THREADS = 10;
		final Thread[] threads = new Thread[THREADS];
		final Random rnd = new Random();
		final Task[] timers = new Task[TIMERS];
		int toBeCancelled = TIMERS * 90 / 100;
		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch scheduled = new CountDownLatch(THREADS);
		final Sequencer done = new Sequencer();
		final AtomicInteger expectedCancels = new AtomicInteger();

		for (int i = 0; i < timers.length; i++) {
			timers[i] = new Task(rnd.nextInt(10000), done);
		}
		final Set<Task> cancelSet = getCancelMap(timers, toBeCancelled);

		for (int i = 0; i < threads.length; i++) {
			final int i$ = i;
			threads[i] = new Thread(new Runnable() {
				public void run() {
					await(start, 10000);
					int startIndex = i$ * (TIMERS / THREADS);
					int endIndex = startIndex + (TIMERS / THREADS);
					for (int j = startIndex; j < endIndex; j++) {
						timers[j].schedule(ts, _tpool);
						if (cancelSet.contains(timers[j])) {
							if (timers[j].cancel()) {
								expectedCancels.incrementAndGet();
							}
						}
					}
					scheduled.countDown();
				}
			});
			threads[i].start();
		}

		// Fire timer scheduling
		long t1 = System.currentTimeMillis();
		start.countDown();

		// Wait for timer schedules
		await(scheduled, 50000);
		long t2 = System.currentTimeMillis();

		// Wait for all timers to expire.
		int expectedExpirations = TIMERS - expectedCancels.get();
		_logger.warn("scheduled " + TIMERS + " timers (expected expirations=" + expectedExpirations + ", elapsed="
				+ (t2 - t1) + ")");

		Thread stat = new Thread() {
			public void run() {
				while (!isInterrupted()) {
					try {
						sleep(1000);
					} catch (InterruptedException e) {
						return;
					}
					_logger.warn("expired=" + done.getCurrentStep());
				}
			}
		};
		stat.start();

		done.waitForStep(expectedExpirations, 30000);
		t2 = System.currentTimeMillis();
		_logger.warn("test done: expired tasks=" + expectedExpirations + ", elapsed=" + (t2 - t1));

		stat.interrupt();

		// check if timers have been scheduled properly.
		int expired = 0;
		int cancelled = 0;
		long deltaAvg = 0;
		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;

		for (int i = 0; i < timers.length; i++) {
			Task d = timers[i];
			if (d.isCancelled()) {
				assertTrue("task cancelled but expired", d.getExpirationTime() == -1);
				cancelled++;
			} else if (d.hasExpired()) {
				long actualDelay = d.getExpirationTime() - d.getScheduleTime();
				assertFalse(
						"delayed task expired too early: expected delay=" + d.getDelay() + ", actual=" + actualDelay,
						actualDelay < d.getDelay());

				deltaAvg += d.getDelta();
				min = Math.min(min, d.getDelta());
				max = Math.max(max, d.getDelta());
				expired++;
			}
		}

		assertTrue("did not get expected cancells", expectedCancels.get() == cancelled);
		assertTrue("expected expirations did not take place", expired == expectedExpirations);
		_logger.warn("all tasks expired properly: expired tasks=" + expired + ", average delta=" + deltaAvg / expired
				+ ", min=" + min + ", max=" + max);
	}

	public void testCancel() {
		super.setName(_descPrefix + "006 TImer Cancel test");
		final TimerService ts = getTimerService();

		final AtomicInteger count = new AtomicInteger();
		Runnable task = () -> {
			_logger.warn("timeout");
			count.incrementAndGet();
		};

		Future<?> f = ts.schedule(_tpool, task, 10, TimeUnit.MILLISECONDS);
		f.cancel(false);
		sleep(100);
		assertTrue("timer is not cancelled", count.get() == 0);
	}

	public void testFixedRateCancel() {
		super.setName(_descPrefix + "007 fixed rate timer cancel test");
		final TimerService ts = getTimerService();

		final AtomicInteger count = new AtomicInteger();
		final AtomicReference<Future<?>> futureRef = new AtomicReference<Future<?>>();
		Runnable task = new Runnable() {
			@Override
			public void run() {
				_logger.warn("timeout");
				if (count.incrementAndGet() == 3) {
					_logger.warn("cancel");
					futureRef.get().cancel(false);
				}
			}
		};

		futureRef.set(ts.scheduleAtFixedRate(_tpool, task, 50, 50, TimeUnit.MILLISECONDS));
		sleep(300);
		assertTrue("fixed rate timer is not cancelled", count.get() == 3);
	}

	public void testFixedDelayTimer() {
		super.setName(_descPrefix + "008 fixed delay timer test");
		final TimerService ts = getTimerService();

		final CountDownLatch latch = new CountDownLatch(5);
		final AtomicLong lastTimeout = new AtomicLong();

		Runnable task = new Runnable() {
			@Override
			public void run() {
				if (lastTimeout.get() == 0) {
					lastTimeout.set(System.currentTimeMillis());
				} else {
					long now = System.currentTimeMillis();
					long diff = now - lastTimeout.get();
					_logger.warn("testFixedDelayTimer: diff=" + diff);
					assertFalse("timer is not scheduled at fixed delay", (diff < 50 || diff >= 110));
					lastTimeout.set(now);
				}
				sleep(10);
				latch.countDown();
			}
		};

		Future<?> f = ts.scheduleWithFixedDelay(_tpool, task, 50, 50, TimeUnit.MILLISECONDS);
		assertTrue(await(latch, 5000));
		f.cancel(false);
	}

	public void testFixedRateTimer() {
		super.setName(_descPrefix + "009 fixed rate timer test");
		final TimerService ts = getTimerService();

		final AtomicInteger counter = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(10);
		final long start = System.currentTimeMillis();

		Runnable task = new Runnable() {
			@Override
			public void run() {
				long delta = System.currentTimeMillis() - start;
				_logger.info("testFixedTimer.timeout: delta=" + delta);

				int c = counter.incrementAndGet();
				if (delta < (50 * c) || delta > ((50 * c) + 50)) {
					_logger.warn("testFixedTimer.timeout: counter=" + c + ", delta=" + delta);
				}
				latch.countDown();
			}
		};

		Future<?> f = ts.scheduleAtFixedRate(_tpool, task, 50, 50, TimeUnit.MILLISECONDS);
		assertTrue(await(latch, 5000));
		f.cancel(false);
	}

	public void testExpiredTimers() {
		super.setName(_descPrefix + "010 expiration timer test");
		final TimerService ts = getTimerService();
		final Sequencer done = new Sequencer();
		final Random rnd = new Random();

		Task[] timers = new Task[1000];
		_logger.warn("scheduling " + timers.length + " timers ...");

		long t1 = System.currentTimeMillis();
		for (int i = 0; i < timers.length; i++) {
			timers[i] = new Task(rnd.nextInt(200), done);
			timers[i].schedule(ts, _tpool);
		}
		long t2 = System.currentTimeMillis();
		_logger.warn("Timers scheduled (elapsed time=" + (t2 - t1) + ")");

		done.waitForStep(timers.length, 20000);
		_logger.warn("checking for proper timers expiration.");
		long deltaAvg = 0;
		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;
		for (int i = 0; i < timers.length; i++) {
			assertFalse("Timer " + timers[i] + " did not expire", timers[i].getExpirationTime() == -1);
			assertTrue("Timer " + timers[i] + " has expired before expected date", timers[i].getDelta() >= 0);
			deltaAvg += timers[i].getDelta();
			min = Math.min(min, timers[i].getDelta());
			max = Math.max(max, timers[i].getDelta());
		}
		_logger.warn("average expiration delta=" + deltaAvg / timers.length + " (min=" + min + ", max=" + max);
	}


	private void sleep(long delay) {
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
		}
	}

	private boolean await(CountDownLatch latch) {
		return await(latch, Long.MAX_VALUE);
	}

	private boolean await(CountDownLatch latch, long max) {
		try {
			return latch.await(max, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			return false;
		}
	}

	private void countDown(CountDownLatch latch, int count) {
		for (int i = 0; i < count; i++) {
			latch.countDown();
		}
	}

	private Set<Task> getCancelMap(Task[] timers, int max) {
		Set<Task> set = new HashSet<Task>();
		Map<Integer, Task> map = new HashMap<Integer, Task>();
		Random rnd = new Random();
		for (int i = 0; i < max; i++) {
			loop: while (true) {
				int index = rnd.nextInt(timers.length);
				if (map.get(index) == null) {
					map.put(index, timers[index]);
					set.add(timers[index]);
					break loop;
				}
			}
		}
		return set;
	}
}
