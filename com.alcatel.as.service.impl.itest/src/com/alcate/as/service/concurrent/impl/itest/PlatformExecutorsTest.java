// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcate.as.service.concurrent.impl.itest;

import java.util.Random;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.nokia.as.util.test.osgi.Ensure;
import com.nokia.as.util.test.osgi.IntegrationTestBase;

/**
 * Tests for the PlatformExecutors service. We extends a small helper class which provides 
 * a BundleContext, as well as a DependencyManager instance. The helper class also
 * allows to create OSGI components using the 'component'.
 * We also uses a "Ensure" helper class which allows to verify that important steps are 
 * getting executed in the correct order. 
 */
@RunWith(MockitoJUnitRunner.class)
public class PlatformExecutorsTest extends IntegrationTestBase {

	/**
	 * Helper used to check if important steps are executed in the right order.
	 */
	private final Ensure m_ensure = new Ensure();
	
	/**
	 * The PlatformExecutors service (injected using DM).
	 */
	private volatile PlatformExecutors _execs;
	
	/**
	 * Random used by our step method.
	 */
	private final Random _rnd = new Random();
		
	/**
	 * We first need to be injected with the PlatformExecutors services.
	 */
	@Before
	public void before() {
		component(comp -> comp.impl(this).start(m_ensure::inc).withSvc(PlatformExecutors.class, true));
		m_ensure.waitForStep(1); // make sure our component is started.
	}

	/**
	 * Validates the processing thread pool.
	 */
	@Test
	public void testProcessingThreadPool() throws InterruptedException {
		_execs.getProcessingThreadPoolExecutor().execute(() -> m_ensure.step(2));
		m_ensure.waitForStep(2); // make sure our task has been scheduled
	}
	
	/**
	 * Validates the io blocking thread pool.
	 */
	@Test
	public void testBlockinghreadPool() throws InterruptedException {
		_execs.getIOThreadPoolExecutor().execute(() -> m_ensure.step(2));
		m_ensure.waitForStep(2); // make sure our task has been scheduled
	}
	
	/**
	 * Validates the queue executor.
	 */
	@Test
	public void testQueue() throws Throwable {
		PlatformExecutor queue = _execs.createQueueExecutor(_execs.getProcessingThreadPoolExecutor());
		IntStream.range(2, 101).forEach(count -> queue.execute(() -> step(count)));
		m_ensure.waitForStep(100); // make sure our task has been scheduled
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