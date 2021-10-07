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