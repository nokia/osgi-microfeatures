package com.nokia.as.gpto.scenarii.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.metering2.MeteringService;
import com.nokia.as.gpto.agent.api.ExecutionContext;
import com.nokia.as.gpto.agent.api.Scenario;
import com.nokia.as.gpto.agent.api.SessionContext;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;

public class HttpScenario implements Scenario {
	private static final Logger LOG = Logger.getLogger(HttpScenario.class);

	private ExecutionContext ectx;
	private Reactor reactor;
	private ReactorProvider reactors;
	private MeteringService meteringService;
	private HttpClientMetering httpMeters;
	private HttpClientConfig httpConfig;
	private Map<Long, LongAdder> latencyBuckets;

	private HttpRequest request;

	@Component(provides = Scenario.Factory.class, properties = {
			@Property(name = Scenario.Factory.PROP_NAME, value = "http_scenario") })
	public static class Factory implements Scenario.Factory {
		@ServiceDependency(required = true)
		MeteringService meteringService;

		@ServiceDependency
		ReactorProvider _reactors;

		@Override
		public Scenario instanciate(ExecutionContext ectx) {
			return new HttpScenario(_reactors, meteringService, ectx);
		}

		@Override
		public String help() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public HttpScenario(ReactorProvider rp, MeteringService ms, ExecutionContext ectx) {
		this.ectx = ectx;
		this.meteringService = ms;
		this.reactors = rp;
	}

	@Override
	public CompletableFuture<Boolean> init() {
		if (LOG.isDebugEnabled())
			LOG.debug("Entering init!");

		latencyBuckets = new ConcurrentHashMap<>();

		httpMeters = new HttpClientMetering(meteringService, ectx.getMonitorable(), ectx.getExecutionId());
		httpMeters.createIncrementalMeter("gpto.execution." + ectx.getExecutionId(), null);
		httpMeters.init();

		try {
			httpConfig = new HttpClientConfig(ectx.getOpts());

		} catch (IllegalArgumentException e) {
			if (LOG.isDebugEnabled())
				LOG.debug("got exception " + e);
			CompletableFuture<Boolean> futureBoolean = new CompletableFuture<>();

			futureBoolean.completeExceptionally(e);
			return futureBoolean;
		}
		reactor = reactors.create("http.client." + ectx.getExecutionId());
		reactor.start();

		request = new BasicRequestBuilder()
				.setMethod(httpConfig.getMethodType())
				.setUri(httpConfig.getURL())
				.setContentType(httpConfig.getContentType())
				.setHeaders(httpConfig.getHeaders())
				.setBody(httpConfig.getBody())
				.build();

		return CompletableFuture.completedFuture(true);
	}

	@Override
	public CompletableFuture<Boolean> beginSession(SessionContext sctx) {
		HttpClient client = new HttpClient(reactors, reactor, PlatformExecutors.getInstance(), httpConfig, httpMeters);
		sctx.attach(client);

		return client.connect().thenApplyAsync((a) -> true);
	}

	@Override
	public CompletableFuture<Boolean> run(SessionContext ctx) {
		HttpClient client = ctx.attachment();

		HashMap<String, String> map = new HashMap<>();
		map.put("$group$", Long.toString(ctx.getMainCounterValue()));
		map.put("$device$", Long.toString(ctx.getSubCounterValue()));

		if (LOG.isDebugEnabled()) {
			LOG.debug("sending  \n" + request.toString(map));
		}

		return client.execute(request, map).thenApplyAsync((httpMessage) -> {
			long duration = httpMessage.getDuration();
			latencyBuckets.computeIfAbsent(duration, (key) -> new LongAdder()).increment();
			httpMeters.getTotalResponseLatency().inc(duration);
//			LOG.debug("latency: " + duration+" ms   /  min="+min+"      max="+max);
			httpMeters.getLastResponseLatency().set(duration);
			
			LOG.trace("received : \n" + httpMessage.getMessage());
			return httpConfig.getMaxQueryBeforeReset() < 0 ? true
					: client.getRequestCount() < httpConfig.getMaxQueryBeforeReset();
		});
	}

	@Override
	public CompletableFuture<Void> endSession(SessionContext sctx) {
		if (LOG.isDebugEnabled())
			LOG.debug("Session closed");
		HttpClient client = sctx.attachment();
		return client.close();
	}

	@Override
	public CompletableFuture<Void> dispose() {
		reactor.stop();
		if (LOG.isDebugEnabled())
			LOG.debug("Reactor stopped");
		return CompletableFuture.completedFuture(null);
	}
}
