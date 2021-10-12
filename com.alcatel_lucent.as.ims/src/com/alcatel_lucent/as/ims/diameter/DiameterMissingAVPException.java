// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter;

import com.nextenso.proxylet.diameter.DiameterAVPDefinition;

/**
 * This exception is thrown when a mandatory AVP is absent in a request. It is
 * also thrown when a conditional AVP should be present in a request (i.e. all
 * conditions are fulfilled) but is absent.
 */
public class DiameterMissingAVPException
		extends Exception {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
	private long _missingAVPCode;
	private String _missingAVPName;

	/**
	 * Constructor.
	 * 
	 * @param code The missing AVP code.
	 */
	public DiameterMissingAVPException(long code) {
		_missingAVPCode = code;
	}

	/**
	 * Constructor.
	 * 
	 * @param definition The definition of the missing AVP.
	 */
	public DiameterMissingAVPException(DiameterAVPDefinition definition) {
		_missingAVPCode = definition.getAVPCode();
		_missingAVPName = definition.getAVPName();
	}

	/**
	 * Constructor.
	 * 
	 * @param code The missing AVP code.
	 * @param name The missing AVP name
	 */
	public DiameterMissingAVPException(long code, String name) {
		_missingAVPCode = code;
		_missingAVPName = name;
	}

	/**
	 * Gets the code of the missing AVP.
	 * 
	 * @return The code of the missing AVP.
	 */
	public long getMissingAVPCode() {
		return _missingAVPCode;
	}

	/**
	 * Gets the name of the missing AVP.
	 * 
	 * @return The name of the missing AVP.
	 */
	public String getMissingAVPName() {
		return _missingAVPName;
	}

	/**
	 * Returns a short description of this object.
	 * 
	 * @see java.lang.Throwable#toString()
	 */
	@Override
	public String toString() {
		if (_missingAVPName != null) {
			return "AVP " + _missingAVPName + " not provided";
		}

		return "AVP with code " + _missingAVPCode + " not provided";
	}
}
