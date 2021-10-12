// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.concurrent.impl;

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.alcatel.as.service.concurrent.TimerService;

public class jdkTimerServiceImplStandalone implements TimerService {
	private final TimerService _timer;
	
	public jdkTimerServiceImplStandalone() {
		Standalone.init(new Hashtable());
		_timer = Standalone.getJdkTimer();
	}

	public ScheduledFuture<?> schedule(Executor taskExecutor, Runnable task, long delay, TimeUnit unit) {
		return _timer.schedule(taskExecutor, task, delay, unit);
	}

	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		return _timer.schedule(command, delay, unit);
	}

	public ScheduledFuture<?> scheduleWithFixedDelay(Executor taskExecutor, Runnable task, long initDelay, long delay,
			TimeUnit unit) {
		return _timer.scheduleWithFixedDelay(taskExecutor, task, initDelay, delay, unit);
	}

	public void execute(Runnable command) {
		_timer.execute(command);
	}

	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
		return _timer.schedule(callable, delay, unit);
	}

	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		return _timer.scheduleAtFixedRate(command, initialDelay, period, unit);
	}

	public void shutdown() {
		_timer.shutdown();
	}

	public ScheduledFuture<?> scheduleAtFixedRate(Executor taskExecutor, Runnable task, long initDelay, long delay,
			TimeUnit unit) {
		return _timer.scheduleAtFixedRate(taskExecutor, task, initDelay, delay, unit);
	}

	public List<Runnable> shutdownNow() {
		return _timer.shutdownNow();
	}

	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		return _timer.scheduleWithFixedDelay(command, initialDelay, delay, unit);
	}

	public boolean isShutdown() {
		return _timer.isShutdown();
	}

	public boolean isTerminated() {
		return _timer.isTerminated();
	}

	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return _timer.awaitTermination(timeout, unit);
	}

	public <T> Future<T> submit(Callable<T> task) {
		return _timer.submit(task);
	}

	public <T> Future<T> submit(Runnable task, T result) {
		return _timer.submit(task, result);
	}

	public Future<?> submit(Runnable task) {
		return _timer.submit(task);
	}

	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		return _timer.invokeAll(tasks);
	}

	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		return _timer.invokeAll(tasks, timeout, unit);
	}

	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		return _timer.invokeAny(tasks);
	}

	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return _timer.invokeAny(tasks, timeout, unit);
	}
}
