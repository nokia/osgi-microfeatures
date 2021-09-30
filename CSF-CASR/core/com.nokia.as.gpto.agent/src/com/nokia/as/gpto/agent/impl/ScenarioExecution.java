package com.nokia.as.gpto.agent.impl;

import java.time.LocalDateTime;
import java.util.Optional;

public class ScenarioExecution {
	public enum Mode {
		FIXED_RATE, FIXED_DELAY;
	}
	
	//TODO: don't expose this to client
	private final int runId;
	private volatile LocalDateTime start;
	private volatile long nanoTimeStart;
	private volatile float rate;
	private volatile Mode mode;

	public ScenarioExecution(int runId) {
		super();
		this.runId = runId;
		this.start = start;

		mode = Mode.FIXED_DELAY;
	}

	public int getRunId() {
		return runId;
	}

	public void setStartDate(LocalDateTime startDate, long nanoTime) {
		start = startDate;
		nanoTimeStart = nanoTime;
	}

	public Optional<LocalDateTime> getStartDate() {
		return Optional.ofNullable(start);
	}

	public float getRate() {
		return rate;
	}

	public void setRate(float rate) {
		this.rate = rate;
	}

	public Mode getMode() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public long getNanoTimeStart() {
		return nanoTimeStart;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mode == null) ? 0 : mode.hashCode());
		result = prime * result + (int) (nanoTimeStart ^ (nanoTimeStart >>> 32));
		result = prime * result + Float.floatToIntBits(rate);
		result = prime * result + runId;
		result = prime * result + ((start == null) ? 0 : start.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ScenarioExecution other = (ScenarioExecution) obj;
		if (mode != other.mode)
			return false;
		if (nanoTimeStart != other.nanoTimeStart)
			return false;
		if (Float.floatToIntBits(rate) != Float.floatToIntBits(other.rate))
			return false;
		if (runId != other.runId)
			return false;
		if (start == null) {
			if (other.start != null)
				return false;
		} else if (!start.equals(other.start))
			return false;
		return true;
	}

}
