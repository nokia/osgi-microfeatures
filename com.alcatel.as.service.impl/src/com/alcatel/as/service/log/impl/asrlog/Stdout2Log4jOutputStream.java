// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.log.impl.asrlog;

import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Redirects stdout/stderr to the default log4j logger.
 */
public class Stdout2Log4jOutputStream extends OutputStream {
	private final Level level;
	private final Logger logger;
	private final static PrintStream ERR = new PrintStream(
			new BufferedOutputStream(new FileOutputStream(FileDescriptor.err), 128));
	private final static String LINE_SEPARATOR = System.getProperty("line.separator");
	private final StringBuffer buf = new StringBuffer();

	// Queue used to redirect stdout/stderr messages to log4j.
	// (we must redirect stdout/stderr to log4j, in a separate thread, in order to
	// avoid potential
	// dead locks.
	private final static LinkedBlockingQueue<Object[]> _stdoutQueue = new LinkedBlockingQueue<Object[]>();

	Stdout2Log4jOutputStream(Logger logger, Level level) {
		this.logger = logger;
		this.level = level;
	}

	public void write(final int data) throws IOException {
		buf.append((char) data);
	}

	public void flush() throws IOException {
		if (buf.length() > 0) {
			try {
				final String s = buf.toString();
				if (!s.equals(LINE_SEPARATOR)) {
					StackTraceElement[] stelems = Thread.currentThread().getStackTrace();
					boolean inLog4j = false;
					for (int i = 0; i < stelems.length; i++) {
						if (stelems[i].getClassName().indexOf("org.apache.log4j") != -1) {
							inLog4j = true;
							break;
						}
					}
					if (inLog4j) {
						ERR.println(s);
						ERR.flush();
					} else {
						try {
							Object[] loginfo = new Object[3];
							loginfo[0] = s;
							loginfo[1] = logger;
							loginfo[2] = level;
							_stdoutQueue.put(loginfo);
						} catch (InterruptedException e) {
						}
					}
				}
			} catch (Throwable t) {
				try {
					ERR.println("Got unexpected exception while flushing stdout logs: " + parse(t));
				} catch (Throwable t2) {
				}
			} finally {
				buf.setLength(0);
			}
		}
	}

	private static String parse(Throwable e) {
		StringWriter buffer = new StringWriter();
		PrintWriter pw = new PrintWriter(buffer);
		e.printStackTrace(pw);
		return (buffer.toString());
	}

	/**
	 * This thread is in charge of redirect stdout/stderr to log4j (this is required
	 * to avoid potential dead locks). TODO: use the thread pool executor ...
	 */
	public static class StdoutRedirectorThread extends Thread {
		StdoutRedirectorThread() {
			super("StdoutThread");
			setDaemon(true);
		}

		public void run() {
			while (true) {
				try {
					Object[] loginfo = _stdoutQueue.take();
					String msg = (String) loginfo[0];
					Logger logger = (Logger) loginfo[1];
					Level level = (Level) loginfo[2];
					logger.log(level, msg);
				} catch (InterruptedException e) {
					// We have been stopped from Activator.stop() method.
				} catch (Throwable t) {
					ERR.println("Could not write stdout/stderr log to log4j");
					t.printStackTrace(ERR);
				}
			}
		}
	}
}
