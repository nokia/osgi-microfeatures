// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.common;

import com.alcatel_lucent.as.ims.Version;

/**
 * The Ims Message.
 */
public class ImsMessage {

	private Version _version;

	/**
	 * 
	 * Constructor for this class.
	 * 
	 * @param version The version.
	 */
	protected ImsMessage(Version version) {
		_version = version;
	}

	/**
	 * Gets the version of the message.
	 * 
	 * @return The version.
	 */
	protected Version getVersion() {
		return _version;
	}

}
