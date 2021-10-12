// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.service.dns.impl;

import java.io.Serializable;

import com.alcatel_lucent.as.service.dns.DNSClient;
import com.alcatel_lucent.as.service.dns.DNSRequest;
import com.alcatel_lucent.as.service.dns.Record;
import com.alcatel_lucent.as.service.dns.RecordA;
import com.alcatel_lucent.as.service.dns.RecordAAAA;
import com.alcatel_lucent.as.service.dns.RecordCName;
import com.alcatel_lucent.as.service.dns.RecordNAPTR;
import com.alcatel_lucent.as.service.dns.RecordSRV;

public class Client implements DNSClient, Serializable {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
	private DNSProperties _properties = null;

	public Client() {
		this(null);
	}

	public Client(DNSProperties properties) {
		_properties = properties;
		if (_properties == null) {
			_properties = new DNSProperties();
		}
	}

	/**
	 * Gets the properties.
	 * 
	 * @return The properties.
	 */
	public DNSProperties getProperties() {
		return _properties;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder(
				"[Client server]");
		return res.toString();
	}

	public <R extends Record> DNSRequest<R> newDNSRequest(
			String query, Class<R> type) {

		if (query == null) {
			return null;
		}

		DNSRequest<R> res = null;
		if (type == RecordA.class) {
			res = (DNSRequest<R>) new RequestA(this, query);
		} else if (type == RecordAAAA.class) {
			res = (DNSRequest<R>) new RequestAAAA(this,
					query);
		} else if (type == RecordNAPTR.class) {
			res = (DNSRequest<R>) new RequestNAPTR(this,
					query);
		} else if (type == RecordSRV.class) {
			res = (DNSRequest<R>) new RequestSRV(this,
					query);
		} else if (type == RecordCName.class) {
			res = (DNSRequest<R>) new RequestCName(this,
					query);
		}
		return res;
	}

	@Override
	public int mode() {
		return _properties.ipvMode();
	}

}
