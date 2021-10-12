// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.util.junit4osgi.impl;

import java.io.File;
import java.util.Dictionary;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.junit.runner.JUnitCore;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.alcatel.as.service.shutdown.ShutdownService;
import com.nokia.as.util.junit4osgi.OsgiJunitRunner;

/**
 * We handle service dependencies in a queue running in the io threadpool.
 */
@Component
@Property(name = "asr.component.parallel", value = "true")
@Property(name = "asr.component.cpubound", value = "false")
public class JunitTracker {
	/**
	 * Each junit bundle must provide the following header in its manifest, and the
	 * header must indicates the number of junit tests that the bundle contains.
	 */
	private final static String JUNIT_TESTS = "Junit-Tests";

	/**
	 * The log service used to log messages.
	 */
	private LogService _log;

	/**
	 * Number of expected junit classes.
	 */
	private final AtomicInteger _expectedJunitClasses = new AtomicInteger(0);

	/**
	 * Name of the single junit class to run before exiting the JVM.
	 */
	private String _testToRunBeforeExiting;

	/**
	 * We need the bundle context to listen on FRAMEWORK_STARTED event
	 */
	@Inject
	private BundleContext _bctx;

	/**
	 * Use the shutdown service in order to properly halt the JVM.
	 */
	@ServiceDependency
	ShutdownService _shutdown;

	/**
	 * Dir where we'll write our junit xml reports.
	 */
	private volatile String _reportsDir;

	/**
	 * Binds the log factory. called before start().
	 */
	@ServiceDependency
	public void bind(LogServiceFactory lf) {
		_log = lf.getLogger("asr.junit.JunitTracker");
	}

	/**
	 * DM is initializing us. We register a framework listener because we want to
	 * count all bundles that have a "Junit-Tests: <N>" header in the OSGI manifest
	 * headers.
	 */
	@Start
	public void start() {
		_log.info("Junit4OSGi service initializing ...");
		_testToRunBeforeExiting = System.getProperty("junit4osgi.test", null);
		_reportsDir = Optional.ofNullable(System.getenv("AS_JUNIT4OSGI_REPORTSDIR"))
				.orElse("system-tests/test-reports/junit4OSGi");
		if (_testToRunBeforeExiting == null) {
			for (Bundle b : _bctx.getBundles()) {
				Dictionary<String, String> headers = b.getHeaders();
				if (b.getBundleId() != 0 && headers.get(JUNIT_TESTS) != null) {
					try {
						int expectedJunitTests = Integer.parseInt(headers.get(JUNIT_TESTS));
						_log.info("Will expect %d junit classes from bundle %s", expectedJunitTests, b.getLocation());
						_expectedJunitClasses.addAndGet(expectedJunitTests);
					} catch (NumberFormatException e) {
						_log.error("Ignoring invalid header %s from bundle %s", e, JUNIT_TESTS, b.getLocation());
					}
				}
			}
			_log.info("Will expect %s junit classes", _expectedJunitClasses);
		} else {
			_log.info("Will execute the %s test.", _testToRunBeforeExiting);
		}
	}

	/**
	 * A new matching bundle arrives. This dependency is optional (hence called
	 * after our start method).
	 */
	@ServiceDependency(filter = "(junit=true)", required = false /* to be called after start */)
	void bind(Object junitInstance) {
		if (_testToRunBeforeExiting != null && !junitInstance.getClass().getName().equals(_testToRunBeforeExiting)) {
			_log.info("Ignorig junit test: %s", junitInstance.getClass().getName());
			return;
		}

		_log.info("Found junit test: %s", junitInstance);
		final Class<?> clazz = junitInstance.getClass();
		OsgiJunitRunner.map(clazz, junitInstance);

		AtomicInteger failed = new AtomicInteger(0);
		AtomicInteger ok = new AtomicInteger(0);
		JUnitCore runner = new JUnitCore();
		File reports = new File(_reportsDir);
		reports.mkdirs();
		XmlWritingListener testListener = new XmlWritingListener(reports);

		testListener.startFile(clazz);
		runner.addListener(testListener);
		runner.run(clazz);
		testListener.closeFile();
		if (testListener.getFailures() > 0) {
			failed.addAndGet(testListener.getFailures());
		} else {
			ok.addAndGet(testListener.getFinished());
		}

		// If this was the last test to run, exit from the JVM !
		if (_testToRunBeforeExiting != null || _expectedJunitClasses.decrementAndGet() == 0) {
			_log.warn("Junit4Osgi: Tests done: passed: %d, failed: %d", ok.get(), failed.get());
			_shutdown.shutdown(this, failed.get() > 0 ? 1 : 0);
		}
	}
}
