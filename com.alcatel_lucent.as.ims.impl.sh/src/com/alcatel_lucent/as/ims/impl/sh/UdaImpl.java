// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.impl.sh;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.sh.ShUtils;
import com.alcatel_lucent.as.ims.diameter.sh.UserDataAnswer;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;

/**
 * The UDA implementation.
 */
public class UdaImpl
		extends ShResponseImpl
		implements UserDataAnswer {

	/**
	 * Constructor for this class.
	 * 
	 * @param response The response.
	 * @param version The Sh version.
	 */
	public UdaImpl(DiameterClientResponse response, Version version) {
		super(response, version);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.UserDataAnswer#getUserData()
	 */
	public byte[] getUserData() {
		DiameterAVPDefinition def = ShUtils.getUserDataAvpDefinition(getVersion());
		return getOctetStringAVP(def);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder("Subsbcribe Notifications Answer (SNA): ");
		res.append("\nUserData=").append(new String(getUserData()));
		res.append(super.toString());
		return res.toString();
	}

}
