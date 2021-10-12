// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.service.dns.impl;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.service.dns.DNSRequest;
import com.alcatel_lucent.as.service.dns.DNSResponse;
import com.alcatel_lucent.as.service.dns.Record;

/**
 * The response object used by the DNSClient and DNSHelper
 */
public class Response<R extends Record> implements DNSResponse<R>{

	private DNSRequest _request = null;
	private List<R> _results = new ArrayList<R>();

	/**
	 * Constructor for this class.
	 * 
	 * @param request The associated request.
	 * @param results The results.
	 */
	public Response(DNSRequest<R> request, List<R> results) {
		_request = request;
		if (results != null) {
			_results.addAll(results);
		}
	}

	/**
	 * Returns a list of DNS records resulting from the DNS request. The list size
	 * could be 0 if DNS request is successful but no DNS record is found for the
	 * given name.
	 * 
	 * @return A list of DNS Records.
	 */
	public List<R> getRecords() {
		return _results;
	}

	/**
	 * Gets the associated request.
	 * 
	 * @return The request.
	 */
	public DNSRequest getDNSRequest() {
		return _request;
	}

}