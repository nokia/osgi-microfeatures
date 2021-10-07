package com.alcatel_lucent.as.service.dns;

/**
 * IPv6 Address Record - maps a domain name to an IPv6 address.
 */
public class RecordAAAA
		extends RecordAddress {
	
	public RecordAAAA() {
		super(RecordType.AAAA);
	}

	/**
	 * Creates an AAAA Record from the given data
	 * 
	 * @param name The record's name.
	 * @param dclass The record's class.
	 * @param ttl The record's TTL.
	 * @param address The record's address.
	 */
	public RecordAAAA(String name, RecordDClass dclass, long ttl, String address) {
		super(RecordType.AAAA, name, dclass, ttl, address);
	}

	/**
	 * Creates an AAAA Record from the given data
	 * 
	 * @param name The record's name.
	 * @param address The record's address.
	 */
	public RecordAAAA(String name,  String address) {
		super(RecordType.AAAA, name, address);
	}

}
