package com.nokia.as.thirdparty.jaeger.itest;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.nokia.as.util.test.osgi.Ensure;
import com.nokia.as.util.test.osgi.IntegrationTestBase;

import io.jaegertracing.internal.JaegerTracer;

@RunWith(MockitoJUnitRunner.class)
public class JaegerServiceIntegrationTest extends IntegrationTestBase {

	private final Ensure _ensure = new Ensure();

	@Test
	public void testService() {
		component(comp -> comp.factory(MyJaegerServiceTester::new)
							  .withSvc(JaegerTracer.class, svc -> svc.required()
									  						   .filter("(service.name=ServiceName1)")
									  						   .add(MyJaegerServiceTester::bindTracer))
							  .withSvc(JaegerTracer.class, svc -> svc.required()
									  					       .filter("(service.name=ServiceName2)")
									  					       .add(MyJaegerServiceTester::bindTracer)));
		_ensure.waitForStep(2, 2);
	}

	public class MyJaegerServiceTester {

		void bindTracer(JaegerTracer tracer) {
			Logger.getLogger(this.getClass()).debug("BOUND TRACER" + tracer);
			tracer.close();
			_ensure.inc();
		}

	}

}
