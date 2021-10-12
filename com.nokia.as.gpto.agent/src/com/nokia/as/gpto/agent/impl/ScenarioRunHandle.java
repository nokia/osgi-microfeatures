// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.gpto.agent.impl;

import java.time.LocalDateTime;

public class ScenarioRunHandle {
	private String runId;
	private LocalDateTime start;

	public ScenarioRunHandle(String runId, LocalDateTime start) {
		super();
		this.runId = runId;
		this.start = start;
	}

	public String getRunId() {
		return runId;
	}

	public LocalDateTime getStartDate() {
		return start;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((runId == null) ? 0 : runId.hashCode());
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
		ScenarioRunHandle other = (ScenarioRunHandle) obj;
		if (runId == null) {
			if (other.runId != null)
				return false;
		} else if (!runId.equals(other.runId))
			return false;
		if (start == null) {
			if (other.start != null)
				return false;
		} else if (!start.equals(other.start))
			return false;
		return true;
	}

}
