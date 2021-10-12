// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.autoconfig.bnd;

import java.util.HashMap;
import java.util.Map;

public class VersionedSingletonConfiguration extends VersionedConfiguration implements Comparable<VersionedSingletonConfiguration> {
	
	public final String version;
	public final String pid;
	public final Map<String, Object> props = new HashMap<>();
	
	public VersionedSingletonConfiguration(String version, String pid) {
		this.version = version;
		this.pid = pid;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((pid == null) ? 0 : pid.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
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
		VersionedSingletonConfiguration other = (VersionedSingletonConfiguration) obj;
		if (pid == null) {
			if (other.pid != null)
				return false;
		} else if (!pid.equals(other.pid))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "VersionedSingletonConfiguration [version=" + version + ", pid=" + pid + ", props=" + props + "]";
	}

	@Override
	public int compareTo(VersionedSingletonConfiguration o) {
		if(pid.equals(o.pid)) return version.compareTo(o.version);
		return pid.compareTo(o.pid);
	}
}
