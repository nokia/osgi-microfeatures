// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.service.dns;

/**
 * This is a utility Class used to perform DNS requests.
 * <p/>
 * An instance can be retrieved via
 * <code>DNSFactory.getInstance().newDNSClient()</code>
 * <h3>Examples of API usage</h3>
 * <h4>Make an SRV request synchronously</h4>
 * <ul>
 * <CODE><PRE>
DNSClient client = DNSFactory.getInstance().newDNSClient();

DNSRequest&lt;RecordSRV&gt; request = client.newDNSRequest("_sip._udp.myservice", RecordSRV.class);

DNSResponse&lt;RecordSRV&gt; response = request.execute();
List&lt;RecordSRV&gt; records = response.getRecords();
if (! records.isEmpty()) {
  for (RecordSRV record : records) {
    System.out.println("My Service is running on port " + record.getPort() + " on host " + record.getTarget());
  }
} else {
  System.out.println("No host is running my service");
}	
 	
</PRE></CODE>
 * </ul>
 * <h4>Make an SRV request asynchronously</h4>
 * <ul>
 * <li>First of all, you need a DNSListener implementation: this is the listener
 * called once the DNS response comes. <CODE><PRE>
public class MySRVListener implements DNSListener&lt;RecordSRV&gt; {
  // the dns request completed. 
  public void dnsRequestCompleted(DNSResponse&lt;RecordSRV&gt; response) {
    List&lt;RecordSRV&gt; records = response.getRecords();
    if (! records.isEmpty()) {
      for (RecordSRV record : records) {
        System.out.println("My Service is running on port " + record.getPort() + " on host " + record.getTarget());
      }
    } else {
      System.out.println("No server is running my service");
    }
  }
}
</PRE></CODE>
 * <li>Then, you call the asynchronous execute() method of DNSClientRequest
 * <CODE><PRE>
DNSClient client = DNSFactory.getInstance().newDNSClient();
DNSRequest&lt;RecordSRV&gt; request = client.newDNSRequest("_sip._udp.myservice", RecordSRV.class);
request.execute(new MySRVListener());
</PRE></CODE>
 * </ul>
 */

public interface DNSClient {

	/**
	 * Instantiates a new request.
	 * 
	 * @param name
	 *            The DNS query. It may either be an absolute (fully qualified)
	 *            or a relative domain name.
	 * @param type
	 *            The Record class corresponding to the type of query (e.g:
	 *            RecordA.class).
	 * @return A new request
	 */
	public <R extends Record> DNSRequest<R> newDNSRequest(
			String name, Class<R> type);

	int mode();

}