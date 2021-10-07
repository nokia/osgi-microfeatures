package com.alcatel_lucent.as.service.dns;

/**
 * Address Record - maps a domain name to an Internet address.
 */
public class RecordA
		extends RecordAddress {
	
	public RecordA() {
		super(RecordType.A);
	}

	/**
	 * Creates an A Record from the given data
	 * 
	 * @param name The record's name.
	 * @param dclass The record's class.
	 * @param ttl The record's TTL.
	 * @param address The record's address.
	 */
	public RecordA(String name, RecordDClass dclass, long ttl, String address) {
		super(RecordType.A, name, dclass, ttl, address);
	}
	
	/**
	 * Creates an A Record from the given data
	 * 
	 * @param name The record's name.
	 * @param address The record's address.
	 */
	public RecordA(String name,  String address) {
		super(RecordType.A, name, address);
	}

}
