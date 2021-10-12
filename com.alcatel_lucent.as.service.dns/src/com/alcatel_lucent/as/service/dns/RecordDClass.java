// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.service.dns;

/**
 * The DNS Class according to RFC 1035 section 3.2.4.
 */
public enum RecordDClass {
	/**
	 * Must be used when the DClass is ignored.
	 */
	UNKNOWN(0),
	/**
	 * The Internet.
	 */
	IN(1),
	/**
	 * The CSNET class (Obsolete - used only for examples in some obsolete RFCs)
	 */
	CS(2),
	/**
	 * The CHAOS class
	 */
	CH(3),
	/**
	 * Hesiod
	 */
	HS(4);

	private int _id = 0;

	private RecordDClass() {}

	private RecordDClass(int id) {
		setId(id);
	}

	private void setId(int id) {
		_id = id;
	}

	public int getId() {
		return _id;
	}

	public static RecordDClass getDClass(int id) {
		switch (id) {
			case 1:
				return IN;
			case 2:
				return CS;
			case 3:
				return CH;
			case 4:
				return HS;
			case 0:
				return UNKNOWN;
			default:
				return null;
		}
	}

}
