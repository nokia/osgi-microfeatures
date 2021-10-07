package com.nokia.as.gpto.agent.strategy.impl;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.nokia.as.gpto.agent.api.Scenario;
import com.nokia.as.gpto.agent.api.Strategy;
import com.nokia.as.gpto.agent.impl.CountdownLatchFuture;
import com.nokia.as.gpto.agent.impl.ExecutionContextImpl;
import com.nokia.as.gpto.agent.impl.SessionContextImpl;

public class ClosedLoopStrategy implements Strategy {
	private static Logger LOG = Logger.getLogger(ClosedLoopStrategy.class);

	protected PlatformExecutors pfes;

	protected Map<SessionContextImpl, ExecutorService> executors;
	protected Set<SessionContextImpl> sessions;

	protected Scenario scenario;
	protected ExecutionContextImpl executionCtx;
	protected int nbSessions;
	protected Runnable shutdownCB;
	protected CountDownLatch shutdownLatch;
	protected CountdownLatchFuture shutdownFuture;

	protected long groupCounterStart;
	protected long groupCounterStep;
	protected long groupCounterMax;

	protected long deviceCounterStart;
	protected long deviceCounterStep;
	protected long deviceCounterMax;

	protected AtomicBoolean stop;
	protected AtomicLong sessionCounter;

	public static final String SESSIONS_PROP = "sessions";
	public static final String DEVICE_COUNTER_PROP = "deviceCounter";
	public static final String GROUP_COUNTER_PROP = "groupCounter";
	public static final String INITIAL_VALUE_PROP = "initialValue";
	public static final String STEP_PROP = "step";
	public static final String MAX_PROP = "max";

	public static final String FACTORY_NAME = "closed-loop";

	@Component(properties = { @Property(name = Strategy.Factory.PROP_NAME, value = FACTORY_NAME) })
	public static class Factory implements Strategy.Factory {
		@ServiceDependency
		PlatformExecutors pfes;

		@Override
		public Strategy instanciate(Scenario scenario, ExecutionContextImpl ectx, JSONObject opts) {
			return new ClosedLoopStrategy(scenario, ectx, opts, pfes);
		}
	}

	public ClosedLoopStrategy(Scenario scenario, ExecutionContextImpl ectx, JSONObject opts, PlatformExecutors pfes) {
		this.scenario = scenario;
		this.pfes = pfes;
		executionCtx = ectx;
		stop = new AtomicBoolean(false);
		sessionCounter = new AtomicLong();
		parseJSONConfiguration(opts);
	}

	@Override
	public void startScheduling(Runnable shutdownCallBack) {
		sessions = new ConcurrentSkipListSet<>();
		executors = new ConcurrentHashMap<>();

		CountDownLatch latch = new CountDownLatch(nbSessions);
		shutdownLatch = new CountDownLatch(nbSessions);
		shutdownFuture = new CountdownLatchFuture(shutdownLatch);
		shutdownFuture.stop();
		shutdownFuture.thenRunAsync(() -> {
			scenario.dispose().thenRun(shutdownCallBack);
		});

		for (int i = 0; i < nbSessions; i++) {
			SessionContextImpl session = initSession();
			ExecutorService sessionExecutor = executors.get(session);
			sessionExecutor.execute(() -> {
				scenario.beginSession(session).whenComplete((ok, ex) -> {
					if (ok && ex == null) {
						latch.countDown();
					}
				});
			});
		}

		boolean finished = false;

		try {
			finished = latch.await(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			finished = false;
		}

		if (!finished) {
			// TODO better cleanup
			if (LOG.isDebugEnabled())
				LOG.debug("failed to start, canceling");
			shutdownCallBack.run();
			return;
		}

		for (Map.Entry<SessionContextImpl, ExecutorService> entry : executors.entrySet()) {
			entry.getValue().submit(() -> doExecuteSession(entry.getKey(), entry.getValue()));
		}
	}

	private SessionContextImpl initSession() {
		SessionContextImpl session = new SessionContextImpl(sessionCounter.addAndGet(1), LocalDateTime.now(),
				groupCounterStart, deviceCounterStart);
		ExecutorService sessionExecutor = pfes.createQueueExecutor(pfes.getProcessingThreadPoolExecutor());
		executors.put(session, sessionExecutor);

		return session;
	}

	private void disposeSession(final SessionContextImpl session, final ExecutorService executor, boolean countDown) {
		executor.submit(() -> {
			scenario.endSession(session).thenRunAsync(() -> {
				sessions.remove(session);
				executors.remove(session);
				if (countDown) {
					shutdownLatch.countDown();
				}
			}, executor);
		});
	}

	@Override
	public void updateProperties(JSONObject value) throws IllegalArgumentException {
	}

	private void doExecuteSession(final SessionContextImpl sctx, final ExecutorService executor) {
		if (!stop.get() && sctx.getMainCounterValue() <= groupCounterMax) {
			scenario.run(sctx).whenCompleteAsync((isOk, err) -> {
				if (isOk) {
					executor.submit(() -> this.doExecuteSession(sctx, executor));
				} else if (err == null) {
					disposeSession(sctx, executor, false);
					SessionContextImpl newSession = initSession();
					ExecutorService newExecutor = executors.get(newSession);

					executor.submit(() -> scenario.beginSession(newSession)
							.thenRunAsync(() -> this.doExecuteSession(newSession, newExecutor)), executor);
				}
			});
			executionCtx.incrementIterationCount();
			if (sctx.incrementSubCounter(deviceCounterStep) > deviceCounterMax) {
				sctx.incrementMainCounter(groupCounterStep);
				sctx.resetSubCounter();
			}
			;
		} else {
			disposeSession(sctx, executor, true);
		}
	}

	public void parseJSONConfiguration(JSONObject conf) {
		nbSessions = conf.optInt(SESSIONS_PROP, 1);

		if (LOG.isDebugEnabled())
			LOG.debug("received json " + conf.toString());

		JSONObject deviceCounterConf = conf.optJSONObject(DEVICE_COUNTER_PROP);
		if (deviceCounterConf != null) {
			deviceCounterStart = deviceCounterConf.optLong(INITIAL_VALUE_PROP, 0);
			deviceCounterStep = deviceCounterConf.optLong(STEP_PROP, 0);
			deviceCounterMax = deviceCounterConf.optLong(MAX_PROP, 1L);
		}

		JSONObject groupCounterConf = conf.optJSONObject(GROUP_COUNTER_PROP);
		if (groupCounterConf != null) {
			groupCounterStart = groupCounterConf.optLong(INITIAL_VALUE_PROP, 0);
			groupCounterStep = groupCounterConf.optLong(STEP_PROP, 0);
			groupCounterMax = groupCounterConf.optLong(MAX_PROP, 1L);
		}
	}

	@Override
	public CompletableFuture<Void> stopScheduling() {
		stop.set(true);
		return shutdownFuture;
	}

}
