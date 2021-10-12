// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.discovery.impl.etcd;

//public enum EtcdProperty {
//	KEY("publish.key"), 
//	VALUE("publish.value"), 
//	TTL_IN_SECONDS("publish.ttl.seconds");
//
//	private String value;
//
//	EtcdProperty(final String value) {
//		this.value = value;
//	}
//
//	public String getValue() {
//		return value;
//	}
//
//	@Override
//	public String toString() {
//		return this.getValue();
//	}
//}

public interface EtcdProperty {
	public final static String KEY = "publish.key"; 
	public final static String VALUE = "publish.value";
	public final static String TTL_IN_SECONDS = "publish.ttl.seconds";
}