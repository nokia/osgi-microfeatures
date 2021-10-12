// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.service.dns;

import java.io.Externalizable;
/**
*
* The request Object used by the DNSClient and DNSHelper.
*/

public interface DNSRequest<R extends Record> {
	
	/**
	* Gets the associated DNSClient.
	* @return The DNSClient.
	*/
	public DNSClient getDNSClient();
	
	/** 
	* Gets the DNS query name of this DNS request.
	* @return The DNS query name.
	*/
	public String getName();
	
	/**                                                                                         
	* Gets the DNS query type of this DNS request.
	* @return The DNS query type.
	*/
	public RecordType getType();
	
	/**
	* Synchronously executes the DNS request.
	* @return The DNS Client response.
	*/
	public DNSResponse<R> execute() ;
	
	/**
	* Asynchronously executes the DNS request.
	* @param listener The object called when the DNSClientResponse is received. 
	*/
	public void execute(DNSListener<R> listener);
	
	/**
	* Attaches the given Object to the request.
	* @param attachment The attachment.
	*/
	public void attach(Externalizable attachment);
	
	/**
	* Gets the attachment.
	* @return The attachment.
	*/
	public Externalizable attachment();
}