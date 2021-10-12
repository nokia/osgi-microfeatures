// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.management.gs.impl;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.io.StringWriter;
import java.util.Map;
import java.util.HashMap;

import com.alcatel.as.service.management.ShutdownService;
import com.alcatel.as.service.management.Shutdownable;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

public class ShutdownServiceImpl implements ShutdownService {
        private final static Logger _logger = Logger.getLogger(ShutdownServiceImpl.class.getName());

	public ShutdownServiceImpl() {
	}

	Set<Shutdownable> _listeners;
	@Override
	public ShutdownService register(Shutdownable service) {
		if (_listeners == null)
			_listeners = new HashSet<Shutdownable>();
		_listeners.add(service);
		return this;
	}
	
	private void shutdown(final Runnable callback, long retrytimeout,final int timeout) {
		Iterator<Shutdownable> it = _listeners.iterator();
		int retry = 0;
		while (it.hasNext()) {
			int serviceretry = it.next().shutdown();
			if (serviceretry == 0)
				it.remove();
			else if (serviceretry < retry)
				retry = serviceretry;
			else {
				if (retry == 0)
					retry=Math.min(timeout,serviceretry);
			}
		}
		if (retry > 0) {
			try {
				Thread.sleep(retry);
			} catch (InterruptedException e) {
			} finally {
				if (System.currentTimeMillis() < retrytimeout)
					shutdown(callback, retrytimeout,timeout);
				else
					callback.run();
			}
		} else {
			callback.run();
		}

	}

	@Override
	public void shutdown(final Runnable callback) {
		shutdown(callback,DELAY);
	}

	@Override
	public void shutdown(final Runnable callback,final int timeout) {
		if (_listeners != null) {
			shutdown(callback, System.currentTimeMillis() + timeout,timeout);
		} else {
			callback.run();
		}
	}

	@Override
	public void halt(int status, boolean dumpThreads) {
	    if (dumpThreads) {
		dumpThreads();
	    }
	    _logger.warn("Halting JVM ...");
	    LogManager.shutdown();
	    Runtime.getRuntime().halt(status);
	}

        private void dumpThreads() {
	    StringWriter sw = new StringWriter();
	    sw.write("Threads dump:\n");
	    try {
		Map<Thread, StackTraceElement[]> mapStacks = Thread.getAllStackTraces();
		Iterator<Thread> threads = mapStacks.keySet().iterator();
		while (threads.hasNext()) {
		    Thread thread = threads.next();
		    StackTraceElement[] stes = mapStacks.get(thread);
		    sw.write("\nThread [" + thread.getName() + " prio=" + thread.getPriority()
			     + "] --> StackTrace elements ...\n");
		    for (StackTraceElement ste : stes) {
			sw.write("\t" + ste.toString() + "\n");
		    }
		}
      
		_logger.warn(sw.toString());
	    } catch (Throwable t) {
		_logger.warn("Exception while dumping state", t);
	    }
	}
}

