package com.nokia.as.gpto.agent.impl;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

import com.nokia.as.gpto.agent.api.SessionContext;

public class SessionContextImpl implements SessionContext {

	private long sessionId;
	private final LocalDateTime startDate;
	private Object attachment;
	private final AtomicLong mainCounter;
	private final AtomicLong subCounter;
	private final long initialMainCounterValue;
	private final long initialSubCounterValue;
	
	public SessionContextImpl(long sessionId, LocalDateTime startDate, long initialMainCounter, long initialSubCounter) {
		this.startDate = startDate;
		this.sessionId = sessionId;
		this.mainCounter = new AtomicLong(initialMainCounter);
		this.initialMainCounterValue = initialMainCounter;
		this.initialSubCounterValue = initialSubCounter;
		this.subCounter = new AtomicLong(initialSubCounter);
	}

	@Override
	public SessionContext attach(Object obj) {
		this.attachment = obj;
		return this;
	}

	@Override
	public <T> T attachment() {
		return (T) attachment;
	}

	@Override
	public LocalDateTime getExecutionStart() {
		return startDate;
	}

	@Override
	public long getMainCounterValue() {
		return mainCounter.longValue();
	}
	
	@Override
	public Long getSessionId() {
		return sessionId;
	}
	
	public long incrementMainCounter(long inc) {
		return mainCounter.addAndGet(inc);
	}
	
	public void resetMainCounter() {
		mainCounter.set(initialMainCounterValue);
	}

	@Override
	public long getSubCounterValue() {
		return subCounter.longValue();
	}
	
	public long incrementSubCounter(long inc) {
		return subCounter.addAndGet(inc);
	}
	
	public void resetSubCounter() {
		subCounter.set(initialSubCounterValue);
	}
}
