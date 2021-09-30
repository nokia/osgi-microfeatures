package com.nokia.as.gpto.agent.strategy.impl;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.nokia.as.gpto.agent.api.Scenario;
import com.nokia.as.gpto.agent.api.Strategy;
import com.nokia.as.gpto.agent.impl.ExecutionContextImpl;

public class OpenLinearStrategy extends AbstractLinearStrategy {
	private static Logger LOG = Logger.getLogger(AbstractLinearStrategy.class);

	@Component(properties = {@Property(name = Strategy.Factory.PROP_NAME, value = "open-linear")})
	public static class Factory implements Strategy.Factory {
		@ServiceDependency
		PlatformExecutors pfes;
		
		@Override
		public Strategy instanciate(Scenario scenario, ExecutionContextImpl ectx, JSONObject opts) {
			return new OpenLinearStrategy(scenario, ectx, opts, pfes);
		}
	}
	
	public OpenLinearStrategy(Scenario sc, ExecutionContextImpl ectx, JSONObject opts, PlatformExecutors pfes) {
		super(sc, ectx, opts, pfes);
	}
	
	protected class OpenLoopRateSetter extends RateSetter {
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
			if (!stop.get()) {
				for (int i = 0; i < val; i++) {
					int idSession = i % nbSessions;
					executors[idSession].execute(() ->  {
						scenario.run(sessions[idSession]);
						executionCtx.incrementIterationCount();
						sessions[idSession].incrementMainCounter(counterStep);
					});
				}
			}
			if(t >= 1) {
				popAction();
			}
		}
	}

	@Override
	protected RateSetter makeRateSetter() {
		// TODO Auto-generated method stub
		return new OpenLoopRateSetter();
	}

}
