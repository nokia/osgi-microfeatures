package com.alcatel.as.service.concurrent.impl;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.PlatformExecutors;

/**
 * Diagnostic component used to do some casr runtime jvm diagnostics
 */
@Property(name = "osgi.command.scope", value = "casr.service.concurrent")
@Property(name = "osgi.command.function", value = { "diag", "block" })
@Descriptor("CASR Concurrent Library commands")
public class GogoShell {

	/**
	 * Our Logger.
	 */
	private final static Logger _log = Logger.getLogger(GogoShell.class);

	/**
	 * Last time the "check" method has been executed in the processing threadpool.
	 * the attribute is volatile because it's updated from the processing thread pool 
	 * and checked from our daemon thread
	 */
	private volatile long _lastCheckExecTime;

	/**
	 * Last time we have scheduled the "check" method in the processing threadpool
	 */
	private long _lastCheckScheduleTime;

	/**
	 * Our thread which regularly schedule the "check" method in the processing
	 * thread poool
	 */
	private volatile Thread _thread;

	/**
	 * Max time in millis that can be used to execute the "check" method in the
	 * processing threadpool
	 */
	private long _maxScheduleTime;

	/**
	 * We periodically schedule our "check" method in the threadpool, based on the
	 * following period in millis
	 */
	private long _pollInterval;

	/**
	 * Max number of thread stack traces to dump. -1 means no limit
	 */
	private long _maxStackTraces;

	/**
	 * Number of thread stack traces dumped
	 */
	private long _threadStackTraces;

	/**
	 * Keep old stack traces in a stack
	 */
	private final LinkedList<StackTrace> _stackTracesHistory = new LinkedList<>();

	/**
	 * Max stack traces we store in stack trace history
	 */
	private final static int MAX_HISTORY = 10;

	/**
	 * Service used to obtain the processing threadpool executor
	 */
	private final PlatformExecutors _execs;

	/**
	 * We record all stacktraces using this tuple class.
	 */
	private static class StackTrace {
		final String stackTrace;
		final Date time;

		StackTrace(String stackTrace, Date time) {
			this.stackTrace = stackTrace;
			this.time = time;
		}
	}

	/**
	 * Creates a new GogoShell instance
	 * @param cnf 
	 */
	GogoShell(PlatformExecutorsImpl execs) {
		_execs = execs;
	}

	void start(Map<String, Object> cnf) {
		_threadStackTraces = 0;
		_maxScheduleTime = getLong(cnf, PlatformExecutorsImpl.CONF_MONITORING_MAXSCHEDULE, -1);
		_pollInterval = getLong(cnf, PlatformExecutorsImpl.CONF_MONITORING_POLL, -1);
		_maxStackTraces = getLong(cnf, PlatformExecutorsImpl.CONF_MONITORING_MAXSTACKTRACES, -1);
		_log.info("configured platform executors monitoring with: maxScheduleTime=" + _maxScheduleTime + ", poll="
				+ _pollInterval + ", maxStackTraces=" + _maxStackTraces);

		if (_maxScheduleTime <= 0 || _pollInterval <= 0 || _maxStackTraces <= 0) {
			_log.info("PlatformExecutors monitoring disabled from configuration : monitoring.max.schedule=-1");
			return;
		}

		_log.info("Starting monitoring thread");

		_lastCheckScheduleTime = System.currentTimeMillis();
		_lastCheckExecTime = 0;
		_execs.getProcessingThreadPoolExecutor().execute(this::check);
		_thread = new Thread(this::checkerLoop);
		_thread.setDaemon(true);
		_thread.start();
	}

	void modified(Map<String, Object> cnf) {
		stop();
		start(cnf);
	}

	void stop() {
		if (_thread != null) {
			_log.info("Stopping monitoring thread");
			_thread.interrupt();
			try {
				_thread.join(10000);
			} catch (InterruptedException e) {
				_log.warn("Could not stop PlatformExecutors monitoring thread");
			}
		}
	}

	private long getLong(Map<String, Object> cnf, String key, long defVal) {
		Object value = cnf == null ? null : cnf.get(key);
		if (value == null) {
			return defVal;
		} else {
			return Long.valueOf(value.toString());
		}
	}

	private void check() {
		if (_log.isTraceEnabled())
			_log.trace("checked");
		_lastCheckExecTime = System.currentTimeMillis();
	}

	private void checkerLoop() {
		long counter = 0; // counter used to avoid dumping threads during each loop iteration

		_log.info("monitoring thread started");

		while (true) {
			try {
				if (_pollInterval == -1) {
					_log.info("stopping, disabled by configuration");
				}
				Thread.sleep(_pollInterval);
				if (_lastCheckExecTime == 0) {
					// timeout not yet executed
					if ((System.currentTimeMillis() - _lastCheckScheduleTime) > _maxScheduleTime) {
						// schedule time too long, dump stacks
						if ((++counter) == 1)
							dumpStacks();
					}
				} else {
					// timeout expired, check if schedule time is longer than max schedule time
					if (_lastCheckExecTime - _lastCheckScheduleTime > _maxScheduleTime) {
						// will only log if blockedCounter == 1
						if ((++counter) == 1)
							dumpStacks();
					}

					if (_log.isTraceEnabled()) {
						_log.trace("scheduling threadpool checker (counter=" + counter + ")");
					}
					counter = 0;
					_lastCheckExecTime = 0;
					_lastCheckScheduleTime = System.currentTimeMillis();
					_execs.getProcessingThreadPoolExecutor().execute(this::check);
				}
			}

			catch (InterruptedException e) {
				_log.debug("monitoring thread interrupted");
				break;
			}

			catch (Exception e) {
				_log.warn("unexpected monitoring thread exception", e);
				break;
			}
		}

		_log.info("monitoring thread stopped");
	}

	private void dumpStacks() {
		_threadStackTraces ++;
		if (_threadStackTraces <= _maxStackTraces) {
			_log.warn("Detected blocked processing threadpool, dumping stacktraces ...");
			String stackTrace = getStackTracesAndRecord();
			_log.warn(stackTrace);
		} else {
			if (_threadStackTraces == (_maxStackTraces + 1)) {
				_log.warn("Blocked processing threadpool detected too many times, stopping to dump stack traces ("
						+ _threadStackTraces + ")");
			}
			getStackTracesAndRecord(); // will record the stacktrace, so it can be consulted using gogo diag command
		}
	}

	/**
	 * Generate stack traces , and record them in our history of stack traces
	 */
	private String getStackTracesAndRecord() {
		StringBuilder sw = new StringBuilder();
		Map<Thread, StackTraceElement[]> mapStacks = Thread.getAllStackTraces();
		Iterator<Thread> threads = mapStacks.keySet().iterator();
		while (threads.hasNext()) {
			Thread thread = threads.next();
			StackTraceElement[] stes = mapStacks.get(thread);
			sw.append("\nThread [" + thread.getName() + " prio=" + thread.getPriority()
					+ "] --> StackTrace elements ...\n");
			for (StackTraceElement ste : stes) {
				sw.append("\t" + ste.toString() + "\n");
			}
		}

		String stackTrace = sw.toString();
		_stackTracesHistory.addLast(new StackTrace(stackTrace, new Date()));
		if (_stackTracesHistory.size() > MAX_HISTORY) {
			_stackTracesHistory.removeFirst();
		}

		return stackTrace;
	}

	/**
	 * Gogo shell command
	 */
	@Descriptor("Detects if the processing threadpool has been blocked in the past")
	public void diag() {
		System.out.println("Checking if processing threadpool was blocked recently ...");
		for (StackTrace stackTrace : _stackTracesHistory) {
			System.out.println(
					"---------------------------------------------------- Blocked processing threadpool detected at "
							+ stackTrace.time + ":");
			System.out.println(stackTrace.stackTrace);
		}
	}

	@Descriptor("Block the processing threadpool")
	public void block(
			@Descriptor("Number of milliseconds to block (default: threads will be blocked for 6 seconds)") @Parameter(names = {
					"-d" }, absentValue = "6000") long millis,
			@Descriptor("Number of threads to block (by default, the number of cores is used as the number of threads to block)") @Parameter(names = {
					"-t" }, absentValue = "-1") int threads) {
		if (threads == -1) {
			threads = Runtime.getRuntime().availableProcessors();
		}
		for (int i = 0; i < threads; i++) {
			_execs.getProcessingThreadPoolExecutor().execute(() -> {
				_log.warn("Blocking threadpool for " + millis + " msecs ...");
				try {
					Thread.sleep(millis);
				} catch (Exception e) {
				}
				_log.warn("unblocked");
			});
		}
	}

}
