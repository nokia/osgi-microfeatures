// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.thirdparty.jaeger.itest;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.nokia.as.util.test.osgi.Ensure;
import com.nokia.as.util.test.osgi.IntegrationTestBase;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.reporters.InMemoryReporter;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.spi.Reporter;
import io.jaegertracing.spi.Sampler;
import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

@RunWith(MockitoJUnitRunner.class)
public class JaegerIntegrationTest extends IntegrationTestBase {

	private final Ensure _ensure = new Ensure();

	@Test
	public void testService() {
		component(comp -> comp.factory(MyJaegerTester::new));
		_ensure.waitForStep(1);
	}

	public class MyJaegerTester {

		void start() throws Exception {
			Reporter report = new InMemoryReporter();
			Sampler sampler = new ConstSampler(true);
			JaegerTracer tracer = new Configuration("testService").getTracerBuilder()
															.withReporter(report)
															.withSampler(sampler)
                                                            .build();
			GlobalTracer.register(tracer);
			Scope scope = tracer.buildSpan("basic span").startActive(true);
			TimeUnit.SECONDS.sleep(1);
			//end the scope, clean up the reporter and the sampler
			scope.close();
			tracer.close();
			_ensure.step(1); // our component must be started first
		}

	}

}
