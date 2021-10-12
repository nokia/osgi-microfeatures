// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.sh;

/**
 * User Data Answer (UDA)
 */
public interface UserDataAnswer
		extends ShAnswer {

	/**
	 * Gets the requested data.
	 * 
	 * @return The requested data.
	 */
	public byte[] getUserData();

}
