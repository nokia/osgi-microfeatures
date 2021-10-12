// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.service.dns;

import java.util.List;

import org.apache.log4j.Logger;

import com.alcatel_lucent.as.service.dns.DNSHelper.Listener;

public interface Helper {
	public final static Logger LOGGER = Logger
			.getLogger("dns.helper");

	void getByName(final String hostname,
			final Listener<RecordAddress> listener);

	List<RecordAddress> getByName(String hostname);
}
