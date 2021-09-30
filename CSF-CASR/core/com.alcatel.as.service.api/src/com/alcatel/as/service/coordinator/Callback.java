package com.alcatel.as.service.coordinator;

/**
 * Coordination or Participant callback.
 */
public interface Callback {
	/**
	 * A Coordination or a Participant is done.
	 * @param error null or a non null exception in case the coordination or the participant could not complete.
	 */
	void joined(Throwable error);
}

