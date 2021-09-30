package com.nokia.as.gpto.itest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import com.nokia.as.gpto.agent.api.ExecutionContext;
import com.nokia.as.gpto.agent.api.Scenario;
import com.nokia.as.gpto.agent.api.SessionContext;

public class TestScenario implements Scenario {

	private AtomicInteger counter;
	
	public static class Factory implements Scenario.Factory {
		
		private AtomicInteger counter;
		
		public Factory(AtomicInteger testCounter) {
			this.counter = testCounter;
		}

		@Override
		public Scenario instanciate(ExecutionContext ectx) {
			return new TestScenario(ectx, counter);
		}

		@Override
		public String help() {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	public TestScenario(ExecutionContext ectx, AtomicInteger testCounter) {
		this.counter = testCounter;
	}

	@Override
	public CompletableFuture<Boolean> init() {
		return CompletableFuture.completedFuture(true);
	}

	@Override
	public CompletableFuture<Boolean> beginSession(SessionContext sctx) {
		return CompletableFuture.completedFuture(true);
	}

	@Override
	public CompletableFuture<Boolean> run(SessionContext sctx) {
		counter.incrementAndGet();
		return CompletableFuture.completedFuture(true);
	}

	@Override
	public CompletableFuture<Void> endSession(SessionContext sctx) {
		System.out.println("endSession");
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> dispose() {
		return CompletableFuture.completedFuture(null);
	}

}
