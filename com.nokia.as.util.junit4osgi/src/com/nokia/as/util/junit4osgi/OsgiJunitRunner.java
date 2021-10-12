// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.util.junit4osgi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

/**
 * A junit test must use this runner in order to be run as a service within an OSGI container.
 * 
 * Example:
 * 
 * @Component(provides=Object.class, properties=@Property(name="asr.junit", value="true"))
 * @RunWith(OsgiJunitRunner.class)  
 * public class CalculatorTest {
 *
 *     @ServiceDependency
 *     LogServiceFactory _logFactory;
 * 
 *     @ServiceDependency
 *     volatile PlatformExecutors _execs;
 *  
 *     LogService _log;
 * 
 *     @Before
 *     public void before() {
 *       _log = _logFactory.getLogger(CalculatorTest.class);
 *       _log.warn("before: executors service=%s", _execs);
 *     }
 * 
 *     @Test
 *     public void evaluatesExpression() {
 *        _log.warn("evaluatesExpression: " + this);
 *        Calculator calculator = new Calculator();
 *        int sum = calculator.evaluate("1+2+3");
 *        assertEquals(6, sum);
 *     }
 * 
 *     @After
 *     public void after() {
 *       _log.warn("after");
 *     } 
 * }
 */
public class OsgiJunitRunner extends BlockJUnit4ClassRunner {  
    /**
     * Name of the mandatory service property that each junit service must provide (value=true)
     */
    public final static String JUNIT = "junit";
    
    // Mapping between Junit classes and their correspdong OSGi component instance.
    private final static Map<Class<?>, Object> _junitInstances = new ConcurrentHashMap<>();
    
    // Our logger
    private final static Logger _log = Logger.getLogger("asr.junit.OsgiJunitRunner");
  
    // Map a Junit class to its OSGi Component instance (only called by asr junit4osgi implementation)
	public static void map(Class<?> type, Object instance) {
        _junitInstances.put(type, instance);
    }
  
    public OsgiJunitRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }
    
    static void nonNull(Object obj) {
    	if (obj == null) throw new NullPointerException();
    }
  
    @Override
    protected Object createTest() throws Exception {
        Object junitInstance = _junitInstances.get(getTestClass().getJavaClass());
        nonNull(junitInstance);
        return junitInstance;
    }
    
    @Override
    protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
		final AtomicBoolean failed = new AtomicBoolean(false);
		RunListener listener = new RunListener() {
			public void testFailure(Failure failure) throws Exception {
				failed.set(true);
			}
		};
		notifier.addListener(listener);
		super.runChild(method, notifier);
		if (method.getAnnotation(Ignore.class) == null) {
			String ok = failed.get() ? "KO" : "OK";
			_log.warn("test result: " + ok);
		}
		notifier.removeListener(listener);
    }
}
