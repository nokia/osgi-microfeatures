// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.service.dns;

/**
 * This interface is a callback used to notify a DNS client about an
 * asynchronous DNS response.
 */

public interface DNSListener<R extends Record> {

	/**
	 * This method is invoked when the response to an asynchronous DNS query is
	 * successfully received.
	 * 
	 * @param response The DNS response.
	 */
	public void dnsRequestCompleted(DNSResponse<R> response);
}