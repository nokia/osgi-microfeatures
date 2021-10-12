// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This executor ensures that at most one task is running at any time, but it
 * achieves this without creating a thread or locking anything during the
 * execution of a task.
 * 
 * Unlike the SerialExecutore, the current executing thread can reschedule a
 * task in the executor, and the task will then be run immediately (inline
 * execution)
 * 
 */
public class ReentrantSerialExecutor implements Executor {
	/** All tasks scheduled are stored there and only one thread may run them. */
	protected final ConcurrentLinkedQueue<Runnable> _tasks = new ConcurrentLinkedQueue<Runnable>();

	/** The Thread that is currently executing this executor */
	AtomicReference<Thread> _executingThread = new AtomicReference<>();

	public interface BytesHandler {
		void handleBytes(byte[] data, int off, int len);
	}

	@Override
	public void execute(Runnable task) {
		Thread currThread = Thread.currentThread();
		if (_executingThread.get() == currThread) {
			// we are already the master, execute task now
			executeNow(task);
		} else {
			if (_executingThread.compareAndSet(null, currThread)) {
				// we became the master: execute task now, and execute tasks scheduled by other threads
				executeNow(task);
				executeTasks(currThread);
			} else {
				// we are not the master, enqueue our task, and retry to become master
				_tasks.add(task);
				if (_executingThread.compareAndSet(null, currThread)) {
					executeTasks(currThread);
				}
			}
		}
	}

	public void execute(byte[] data, int off, int len, BytesHandler handler) {
		Thread currThread = Thread.currentThread();
		if (_executingThread.get() == currThread) {
			executeNow(() -> handler.handleBytes(data, off, len));
		} else {
			if (_executingThread.compareAndSet(null, currThread)) {
				// we are the master thread, execute the task now without copying data
				executeNow(() -> handler.handleBytes(data, off, len));
				// executes tasks potentially scheduled by other threads
				executeTasks(currThread);
			} else {
				// another master thread is currently running, we need to copy the byte array
				byte[] copy = new byte[len];
				System.arraycopy(data, off, copy, 0, len);
				_tasks.add(() -> handler.handleBytes(copy, 0, len));
				// retry to become the master thread
				if (_executingThread.compareAndSet(null, currThread)) {
					executeTasks(currThread);
				}
			}
		}
	}

	private void executeTasks(Thread currThread) {
		do {
			try {
				// Only one thread at a time is running this method, so there is no possible
				// contention.
				Runnable task;
				ConcurrentLinkedQueue<Runnable> tasks = _tasks;

				while ((task = tasks.poll()) != null) {
					executeNow(task);
				}
			} finally {
				_executingThread.set(null);
			}
		} while (!_tasks.isEmpty() && _executingThread.compareAndSet(null, currThread));
	}

	private void executeNow(Runnable task) {
		try {
			task.run();
		} catch (Throwable t) {
			Utils.logger.warn("reactor task execution exception", t);
		}
	}
}
