// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package $basePackageName$;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.nokia.as.util.test.osgi.Ensure;
import com.nokia.as.util.test.osgi.IntegrationTestBase;

@RunWith(MockitoJUnitRunner.class)
public class ExampleIntegrationTest extends IntegrationTestBase {

	private final Ensure _ensure = new Ensure();

	@Test
	public void testService() {
		component(comp -> comp.impl(new MyServiceImpl()).provides(MyService.class));
		component(comp -> comp.impl(new MyComponent()).withSvc(MyService.class, true /* required */));
		_ensure.waitForStep(3); // wait until both components are started and service is invoked
	}

	public interface MyService {
		void doit();
	}

	public class MyServiceImpl implements MyService {
		
		void start() {
			_ensure.step(1); // our component must be started first
		}

		@Override
		public void doit() {
			_ensure.step(3); // the MyComponent.start() method is called us (step 3, all is well).
		}
	}

	public class MyComponent {
		private volatile MyService _service; // injected

		public void start() {
			_ensure.step(2);  // we have been injected after step 1
			_service.doit();
		}
	}

}
