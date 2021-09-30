package com.alcate.as.service.concurrent.impl.itest;

import java.util.Dictionary;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.nokia.as.util.test.osgi.Ensure;
import com.nokia.as.util.test.osgi.IntegrationTestBase;

@RunWith(MockitoJUnitRunner.class)
public class FactoryPidTest extends IntegrationTestBase {

	/**
	 * Helper used to check if important steps are executed in the right order.
	 */
	private final Ensure m_ensure = new Ensure();
	
	private final static String FACTORY_PID = "com.alcatel.as.service.concurrent.impl.itest.MyFactoryComponent";
			
	/**
	 * We first need to be injected with the PlatformExecutors services.
	 */
	@Before
	public void before() {		
		factoryComponent(comp -> comp
				.factory(MyFactoryComponent::new)
				.start(m_ensure::inc)
				.update(MyFactoryComponent::updated)
				.factoryPid(FACTORY_PID));
		m_ensure.waitForStep(1); // make sure our component is started.
	}

	/**
	 * Validates the processing thread pool.
	 */
	@Test
	public void testFactoryComponent() throws InterruptedException {
		m_ensure.waitForStep(3); // make sure two instances of MyFactoryComponent service are instantiated
	}
	
	public class MyFactoryComponent {
		void updated(Dictionary<String, Object> conf) {
			System.out.println("MyFactoryComponent.updated: conf=" + conf);
			m_ensure.inc();
		}
	}
	
}