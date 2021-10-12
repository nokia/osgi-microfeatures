// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.service.dns;

/**
 * The Record Type.
 */
public enum RecordType {
	/**
	 * CNAME type.
	 */
	CNAME("CNAME"),
	/**
	 * A type (IPV4).
	 */
	A("A"),
	/**
	 *  AAAA type (IPV6).
	 */
	AAAA("AAAA"), 
	/**
	 *  NAPTR type.
	 */
	NAPTR("NAPTR"), 
	/**
	 *  SRV type.
	 */
	SRV("SRV")
	;

private String _name = null;
private RecordType(String name) {
	_name = name;
}
/**
 * @see java.lang.Enum#toString()
 */
@Override
public String toString() {
		return _name;
}

}
