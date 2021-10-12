// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.sip.sipservlet.tools;
/**  Description of the Class */
public class LicenseException extends SecurityException {
    private static final long serialVersionUID = 1L;
    /**  Description of the Field */
	public final static int INVALID = 0;
	/**  Description of the Field */
	public final static int UNKNOWN = 1;
	/**  Description of the Field */
	public final static int EXPIRES = 2;
	private int type = INVALID;
	private String what;


	/**
	 *Constructor for the LicenseException object
	 *
	 * @param  type  Description of the Parameter
	 * @param  what  Description of the Parameter
	 */
	public LicenseException(String what, int type) {
		super();
		this.type = type;
		this.what = what;
	}


	/**
	 *  Gets the reason attribute of the LicenseException object
	 *
	 * @return    The reason value
	 */
	public int getReason() {
		return type;
	}


	/**
	 *  Gets the what attribute of the LicenseException object
	 *
	 * @return    The what value
	 */
	public String getWhat() {
		return what;
	}


	/**
	 *  Description of the Method
	 *
	 * @return    Description of the Return Value
	 */
	public String toString() {

		switch (type) {
						case 0:
							return "Invalid License for " + what;
						case 1:
							return "Unknown License for " + what;
						case 2:
							return "Expired License for " + what;
		}
		return super.toString();
	}

}

