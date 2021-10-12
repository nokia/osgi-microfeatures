// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.service.dns;
import java.util.List;

/**
 * The response object used by the DNSClient and DNSHelper
 */
public interface  DNSResponse<R extends Record> {
	/**
	 * Returns a list of DNS records resulting from the DNS request. The list size
	 * could be 0 if DNS request is successful but no DNS record is found for the
	 * given name.
	 * 
	 * @return A list of DNS Records.
	 */
	public List<R> getRecords() ;

	/**
	 * Gets the associated request.
	 * 
	 * @return The request.
	 */
	public DNSRequest getDNSRequest() ;

}