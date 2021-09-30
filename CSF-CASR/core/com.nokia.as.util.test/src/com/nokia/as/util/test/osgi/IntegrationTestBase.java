package com.nokia.as.util.test.osgi;

import java.util.function.Consumer;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.lambda.ComponentBuilder;
import org.apache.felix.dm.lambda.DependencyManagerActivator;
import org.apache.felix.dm.lambda.FactoryPidAdapterBuilder;
import org.junit.After;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * Base class for OSGI integration tests
 */
public class IntegrationTestBase {
	
	/**
	 * The bundle context for our integration test
	 */
	protected final BundleContext _context = FrameworkUtil.getBundle(getClass()).getBundleContext();
	
	/**
	 * Creates dependencymanager, which can be used to declare service dependencies from tests.
	 */
	protected final DependencyManager _dm = new DependencyManager(_context);
    
    protected void component(Consumer<ComponentBuilder<?>> consumer) {
    	DependencyManagerActivator.component(_dm, consumer);
    }
    
    protected void factoryComponent(Consumer<FactoryPidAdapterBuilder> consumer) {
    	DependencyManagerActivator.factoryPidAdapter(_dm, consumer);
    }
    
	/**
	 * Cleanup all created components.
	 */
	@After
	public void after() {
		_dm.clear();
	}

}
