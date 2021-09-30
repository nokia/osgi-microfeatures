package com.nokia.as.gpto.itest;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.nokia.as.gpto.agent.api.Scenario;
import com.nokia.as.gpto.agent.api.Scenario.Factory;
import com.nokia.as.gpto.agent.api.ScenarioRunner;
import com.nokia.as.gpto.agent.api.ScenarioRunner.ScenarioListener;
import com.nokia.as.gpto.agent.strategy.impl.ClosedLoopStrategy;
import com.nokia.as.gpto.common.msg.api.AgentRegistration;
import com.nokia.as.gpto.common.msg.api.ExecutionState;
import com.nokia.as.util.test.osgi.Ensure;
import com.nokia.as.util.test.osgi.IntegrationTestBase;

@RunWith(MockitoJUnitRunner.class)
public class ScenarioRunnerTest extends IntegrationTestBase {

	private final Ensure _ensure = new Ensure();
	private volatile ScenarioRunner controller;
	private volatile AtomicInteger counter;

	@Before
	public void before() {
		component(comp -> comp.impl(this).start(_ensure::inc).withSvc(ScenarioRunner.class, true));
		_ensure.waitForStep(1); // make sure our component is started.
	}
	
	@Test
	public void testListingStrategy() {
		Assert.assertTrue(controller.getAvailableStrategies().size() > 0);
	}

	
	@Test
	public void testAddingFactory() {		
		ScenarioListener listener = new ScenarioListener() {

			@Override
			public void onScenarioFactoryChange(Map<String, Factory> factories) {
				_ensure.inc();
			}

			@Override
			public void onStrategyFactoryChange(Map<String, com.nokia.as.gpto.agent.api.Strategy.Factory> factories) {}

			@Override
			public void onExecutionStateChange(int executionId, String scenarioName, String strategyName,
					ExecutionState state) {}

			@Override
			public void onMetricTick(Map<String, AgentRegistration> registry) {
				// TODO Auto-generated method stub
				
			}
		};
		
		controller.addFactoryListener(listener);
		
		Scenario.Factory factory = new TestScenario.Factory(null);
		Dictionary<String, Object> props = new Hashtable<>();
		props.put(Scenario.Factory.PROP_NAME, "test_scenario1");
		
		controller.onScenarioFactoryPublish(factory, props);
		_ensure.waitForStep(2, 5);
			
		Assert.assertTrue(controller.getAvailableScenarii().contains("test_scenario1"));
		controller.removeFactoryListener(listener);
		_ensure.inc();
	}
	
	
	@Test
	public void testRunningScenario() {
		Ensure ensureRun = new Ensure();
		
		ScenarioListener listener = new ScenarioListener() {

			boolean finished = false;
			
			@Override
			public void onScenarioFactoryChange(Map<String, Factory> factories) {
				ensureRun.inc();
			}

			@Override
			public void onStrategyFactoryChange(Map<String, com.nokia.as.gpto.agent.api.Strategy.Factory> factories) {}

			@Override
			public void onExecutionStateChange(int executionId, String scenarioName, String strategyName,
					ExecutionState state) {
				if(state == ExecutionState.STOPPED) {
					ensureRun.inc();
				}
			}

			@Override
			public void onMetricTick(Map<String, AgentRegistration> registry) {
				// TODO Auto-generated method stub
				
			}
		};
		
		controller.addFactoryListener(listener);
		
		counter = new AtomicInteger();
		Scenario.Factory factory = new TestScenario.Factory(counter);
		Dictionary<String, Object> props = new Hashtable<>();
		props.put(Scenario.Factory.PROP_NAME, "test_scenario2");
		
		controller.onScenarioFactoryPublish(factory, props);

		controller.prepareScenarioExecution("test_scenario2",
				ClosedLoopStrategy.FACTORY_NAME, 
				new JSONObject(), 
				generateLoopConfig(), 
				1).thenRun(() -> {
					controller.startExecution(1);
		});
		
		ensureRun.waitForStep(2);		
		Assert.assertEquals(1000, counter.get());
	}
	
	private JSONObject generateLoopConfig() {
		JSONObject obj = new JSONObject();
		try {
			JSONObject deviceCounter = new JSONObject();
			JSONObject groupCounter = new JSONObject();
			obj.put(ClosedLoopStrategy.SESSIONS_PROP, 10);
			
			deviceCounter.put(ClosedLoopStrategy.INITIAL_VALUE_PROP, 1);
			deviceCounter.put(ClosedLoopStrategy.STEP_PROP, 1);
			deviceCounter.put(ClosedLoopStrategy.MAX_PROP, 0);
			obj.put(ClosedLoopStrategy.DEVICE_COUNTER_PROP, deviceCounter);
			
			groupCounter.put(ClosedLoopStrategy.INITIAL_VALUE_PROP, 1);
			groupCounter.put(ClosedLoopStrategy.STEP_PROP, 1);
			groupCounter.put(ClosedLoopStrategy.MAX_PROP, 100);
			obj.put(ClosedLoopStrategy.GROUP_COUNTER_PROP, groupCounter);
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return obj;
	}
}
