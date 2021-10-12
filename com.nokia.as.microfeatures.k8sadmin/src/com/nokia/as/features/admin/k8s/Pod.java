// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.features.admin.k8s;

public class Pod {
	
	public final String name;
	private String ip;
	public final int port;
	private DeployStatus status;
	
	public Pod(String name, String ip, int port) {
		this.name = name;
		this.ip = ip;
		this.port = port;
		this.status = DeployStatus.UNREADY;
	}
	
	public Pod ready(boolean ready) {
		if(ready) status = DeployStatus.READY;
		else status = DeployStatus.UNREADY;
		return this;
	}
	
	public DeployStatus status() {
		return this.status;
	}
	
	public boolean isReady() {
		return this.status.equals(DeployStatus.READY);
	}
	
	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		Pod other = (Pod) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
