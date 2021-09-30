package com.nokia.as.gpto.agent.strategy.impl;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.nokia.as.gpto.agent.api.Scenario;
import com.nokia.as.gpto.agent.api.Strategy;
import com.nokia.as.gpto.agent.impl.ExecutionContextImpl;

public class ClosedLinearStrategy extends AbstractLinearStrategy {
	
	private static Logger LOG = Logger.getLogger(ClosedLinearStrategy.class);
	
	@Component(properties = {@Property(name = Strategy.Factory.PROP_NAME, value = "closed-linear")})
	public static class Factory implements Strategy.Factory {
		@ServiceDependency
		PlatformExecutors pfes;
		
		@Override
		public Strategy instanciate(Scenario scenario, ExecutionContextImpl ectx, JSONObject opts) {
			return new ClosedLinearStrategy(scenario, ectx, opts, pfes);
		}
	}
	
	protected class ClosedLoopRateSetter extends RateSetter {

		@Override
		public void run() {
 			//(x0, y0) = (start, startRate)
			//(x1, y1) = (end, endRate)
			long time = System.currentTimeMillis();
			
			float t = (time - startDate.get()) / (float) (endDate.get() - startDate.get());
			float value = startValue.get() + t * (endValue.get() - startValue.get());
			
			if(LOG.isDebugEnabled()) {
				LOG.debug("valueMax = " + endValue.get());
				LOG.debug("valueMin = " + startValue.get());
				LOG.debug("--------- computed rate is " + value);
			}
			int val = Math.round(value);
			
			AtomicInteger tickDownLatch = new AtomicInteger(val);
			SessionRunner[] sessionRunner = new SessionRunner[nbSessions];
			
			if (!stop.get()) {
				for (int i = 0; i < nbSessions; i++) {
					sessionRunner[i] = new SessionRunner(i, tickDownLatch);
					executors[i].execute(sessionRunner[i]);
				}
			}
			if(t >= 1) {
				popAction();
			}

		}
	}
	
	protected class SessionRunner implements Runnable {
		int idSession;
		AtomicInteger tickLatch;
		
		public SessionRunner(int id, AtomicInteger latch) {
			idSession = id;
			tickLatch = latch;
		}
		
		@Override
		public void run() {
//			LOG.debug("stop = " + stop.get() + "      LATCH = "+tickLatch.get());
			if (tickLatch.get() > 0 && !stop.get() && sessions[idSession].getMainCounterValue() < counterMax) {
				scenario.run(sessions[idSession])
					.thenRunAsync(() -> run(), executors[idSession]);
				tickLatch.decrementAndGet();
				executionCtx.incrementIterationCount();
				sessions[idSession].incrementMainCounter(counterStep);
			} 
		}
	}
	
	public ClosedLinearStrategy(Scenario sc, ExecutionContextImpl ectx, JSONObject opts, PlatformExecutors pfes) {
		super(sc, ectx, opts, pfes);
	}

	@Override
	protected AbstractLinearStrategy.RateSetter makeRateSetter() {
		return new ClosedLoopRateSetter();
	}
}
