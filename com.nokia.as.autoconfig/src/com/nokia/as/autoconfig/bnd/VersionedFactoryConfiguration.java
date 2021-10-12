// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.autoconfig.bnd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VersionedFactoryConfiguration extends VersionedConfiguration implements Comparable<VersionedFactoryConfiguration> {
	
	public final String version;
	public final String pid;
	public final List<Map<String, Object>> props = new ArrayList<>();
	
	public VersionedFactoryConfiguration(String version, String pid) {
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
		VersionedFactoryConfiguration other = (VersionedFactoryConfiguration) obj;
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
		return "VersionedFactoryConfiguration [version=" + version + ", pid=" + pid + ", props=" + props + "]";
	}

	@Override
	public int compareTo(VersionedFactoryConfiguration o) {
		if(pid.equals(o.pid)) return version.compareTo(o.version);
		return pid.compareTo(o.pid);	
	}
	
}
