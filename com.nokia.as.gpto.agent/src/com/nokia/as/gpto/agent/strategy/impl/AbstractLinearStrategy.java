package com.nokia.as.gpto.agent.strategy.impl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.nokia.as.gpto.agent.api.Scenario;
import com.nokia.as.gpto.agent.api.Strategy;
import com.nokia.as.gpto.agent.impl.CountdownLatchFuture;
import com.nokia.as.gpto.agent.impl.ExecutionContextImpl;
import com.nokia.as.gpto.agent.impl.SessionContextImpl;

public abstract class AbstractLinearStrategy implements Strategy {
	private static Logger LOG = Logger.getLogger(AbstractLinearStrategy.class);

	protected PlatformExecutors pfes;

	protected Float valueMin;
	protected Float valueMax;
	protected String propName;
	protected Float duration;
	protected AtomicBoolean stop = new AtomicBoolean(false);
	protected CountDownLatch shutDownLatch;
	protected Queue<SawToothValues> actionQueue;
	protected ScheduledExecutorService scheduledExecutor;
	protected ScheduledExecutorService scheduledChangeRate;
	protected ExecutorService[] executors;
	protected ScheduledFuture<?> scheduledFuture;
	protected Scenario scenario;
	protected ExecutionContextImpl executionCtx;
	protected SessionContextImpl[] sessions;
	protected int nbSessions;
	protected Runnable shutdownCB;
	
	protected long counterStart;
	protected long counterStep;
	protected long counterMax;	
	
	public static final String MAX_PROP = "end";
	public static final String MIN_PROP = "start";
	public static final String RAMP_DURATION_PROP = "duration";
	public static final String SESSIONS_PROP = "sessions";
	public static final String LOOP_PROP = "loop";
	public static final String STEPS_PROP = "steps"; 
	
	protected abstract class RateSetter implements Runnable {
		protected AtomicLong startDate;
		protected AtomicLong endDate;
		protected AtomicInteger startValue;
		protected AtomicInteger endValue;
		protected Long strategyStartDate;
		
		public RateSetter() {			
			startValue 	= new AtomicInteger();
			endValue 	= new AtomicInteger();
			startDate 	= new AtomicLong();
			endDate 	= new AtomicLong();
			strategyStartDate = System.currentTimeMillis();
		}
		
		public void popAction() {
			SawToothValues action = actionQueue.poll();
			if (action != null) {
				if(LOG.isDebugEnabled()) LOG.debug("Initialized new values");
				startDate.set(System.currentTimeMillis());
				endDate.set(startDate.get() + action.duration);
				startValue.set(action.start);
				endValue.set(action.end);
				if(LOG.isDebugEnabled()) LOG.debug(endDate.get() - startDate.get() + " ms duration");
			} else {
				if(LOG.isDebugEnabled()) LOG.debug("no more action, shutting down");
				stopScheduling().thenRunAsync(() -> {
					shutdownCB.run();
				});

			}
		}
		
		@Override
		public abstract void run();
	}
	
	public class SawToothValues {
		int start;
		int end;
		long duration;
		
		public SawToothValues(int start, int end, long duration) {
			this.start = start;
			this.end = end;
			this.duration = duration;
		}

		@Override
		public String toString() {
			return "SawToothValues [start=" + start + ", end=" + end + ", duration=" + duration + "]";
		}
	}
	
	public AbstractLinearStrategy(Scenario sc, ExecutionContextImpl ectx, JSONObject opts, PlatformExecutors pfes) {
		this.pfes = pfes;

		scenario = sc;
		executionCtx = ectx;
		actionQueue = parseConfiguration(opts);
		LOG.debug("got configuration " + actionQueue);
	}

	@Override
	public void startScheduling(Runnable shutdownCallback) {
		LOG.debug("Start Scheduling with " + nbSessions + " sessions");
		sessions = new SessionContextImpl[nbSessions];
		executors = new ExecutorService[nbSessions];
		
		CountDownLatch latch = new CountDownLatch(nbSessions);
		
		for (int i = 0; i < nbSessions; i++) {
			final int _i = i;
			sessions[i] = new SessionContextImpl(0, LocalDateTime.now(), counterStart, 0);
			executors[i] = pfes.createQueueExecutor(pfes.getProcessingThreadPoolExecutor());
			executors[i].execute(() -> {
				scenario.beginSession(sessions[_i]).whenComplete((ok,ex) -> {
					if(ok && ex == null) {
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
				
		if(!finished) {
			//TODO better cleanup
			if(LOG.isDebugEnabled()) LOG.debug("failed to start, canceling");
			shutdownCallback.run();
			return;
		}
		
		scheduledExecutor = Executors.newScheduledThreadPool(1);
		scheduledChangeRate = Executors.newScheduledThreadPool(1);
		// duration in ms
		RateSetter setter = makeRateSetter();
		
		this.shutdownCB = shutdownCallback;		
		setter.popAction();
		scheduledFuture = scheduledExecutor.scheduleAtFixedRate(setter, 0, 1000, TimeUnit.MILLISECONDS);
	}

	@Override
	public void updateProperties(JSONObject value) throws IllegalArgumentException {
	}

	@Override
	public CompletableFuture<Void> stopScheduling() {
		if(LOG.isDebugEnabled()) LOG.debug("Stopping executionstrategy");
		scheduledFuture.cancel(false);
		scheduledExecutor.shutdown();
		
		shutDownLatch = new CountDownLatch(nbSessions);
		stop.set(true);
		
		IntStream.range(0, nbSessions).forEach( (i) ->
			executors[i].execute(() ->  {
				scenario.endSession(sessions[i]).thenRun(() -> shutDownLatch.countDown());
			}));
		
		CountdownLatchFuture future = new CountdownLatchFuture(shutDownLatch);
		return future.stop().thenRunAsync(() -> {
			scenario.dispose();
			LOG.debug("Scenario disposed");
		});
	}
	
	protected Queue<SawToothValues> parseConfiguration(JSONObject conf) {
		nbSessions = conf.optInt(SESSIONS_PROP, 1);
		
		if(LOG.isDebugEnabled()) LOG.debug("received json " + conf.toString());
		
		JSONArray steps = conf.optJSONArray(STEPS_PROP);
		if(steps == null) {
			throw new IllegalArgumentException();
		}
		
		Queue<SawToothValues> queue = new LinkedList<>();
		queue.addAll(parseSteps(steps));
		
		JSONObject counterConf = conf.optJSONObject("counterConfiguration");
		if(counterConf != null) {
			counterStart = counterConf.optLong("initialValue", 0);
			counterStep  = counterConf.optLong("step", 0);
			counterMax   = counterConf.optLong("max", Long.MAX_VALUE);
		} else {
			counterMax = Long.MAX_VALUE;
		}
		return queue;
	}
	
	private List<SawToothValues> parseSteps(JSONArray array) {
		List<SawToothValues> steps = new LinkedList<>();
		
		for(int i = 0; i < array.length(); i++) {
			JSONObject obj = array.optJSONObject(i);
			if(obj == null) {
				continue;
			}
			
			steps.addAll(parseStep(obj));
		}
		
		return steps;
	}
	
	private List<SawToothValues> parseStep(JSONObject obj) {
		try {
			if(obj.has(LOOP_PROP) && obj.has(STEPS_PROP)) {
				int loopCount = obj.getInt(LOOP_PROP);
				List<SawToothValues> result = new LinkedList<>();
				List<SawToothValues> steps = parseSteps(obj.getJSONArray(STEPS_PROP));
				for(int i = 0; i < loopCount; i++) {
					result.addAll(steps);
				}
				return result;
			}
			Integer begin = obj.getInt(MIN_PROP);
			Integer end   = obj.getInt(MAX_PROP);
			Integer duration = obj.getInt(RAMP_DURATION_PROP);
		
			return Collections.singletonList(new SawToothValues(begin, end, duration));
		
		} catch (JSONException e) {
			return Collections.emptyList();
		}
	}
	
	protected abstract RateSetter makeRateSetter();
}
