// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcate.as.service.concurrent.impl.itest;

import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.nokia.as.util.test.osgi.Ensure;
import com.nokia.as.util.test.osgi.IntegrationTestBase;

/**
 * Tests if the two default platform executors services (processing/io) are registered in the registry.
 */
@RunWith(MockitoJUnitRunner.class)
public class PlatformExecutorsServicesTest extends IntegrationTestBase {

	/**
	 * Helper used to check if important steps are executed in the right order.
	 */
	private final Ensure m_ensure = new Ensure();
	
	/**
	 * Processing thread pool
	 */
	private volatile ScheduledExecutorService _processingExec;
	
	/**
	 * blocking thread pool
	 */
	private volatile ScheduledExecutorService _blockingExec;

	/**
	 * Random used by our step method.
	 */
	private final Random _rnd = new Random();
		
	/**
	 * We first need to be injected with the PlatformExecutors services.
	 */
	@Before
	public void before() {
		component(comp -> comp.impl(this).start(m_ensure::inc)
				  .withSvc(ScheduledExecutorService.class, "(type=casr.processing)", "_processingExec", true)
				  .withSvc(ScheduledExecutorService.class, "(type=casr.blocking)", "_blockingExec", true));
		m_ensure.waitForStep(1); // make sure our component is started.
	}

	/**
	 * Validates the processing thread pool.
	 */
	@Test
	public void testProcessingThreadPool() throws InterruptedException {
		_processingExec.execute(() -> m_ensure.step(2));
		m_ensure.waitForStep(2); // make sure our task has been scheduled
	}
	
	/**
	 * Validates the io blocking thread pool.
	 */
	@Test
	public void testBlockinghreadPool() throws InterruptedException {
		_blockingExec.execute(() -> m_ensure.step(2));
		m_ensure.waitForStep(2); // make sure our task has been scheduled
	}
		
	/**
	 * Wait a bit randomly, then increment the Ensure tool with a given step.
	 * @param step
	 */
	private void step(int step) {
		try {
			Thread.currentThread().sleep(_rnd.nextInt(2));
		} catch (InterruptedException e) {}
		try {
			m_ensure.step(step);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
}