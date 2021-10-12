// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.impl.sh;

import java.util.Date;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.sh.ShUtils;
import com.alcatel_lucent.as.ims.diameter.sh.SubscribeNotificationsAnswer;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;

/**
 * The Subscribe Notifications Answer (SNA) implementation.
 */
public class SnaImpl
		extends ShResponseImpl
		implements SubscribeNotificationsAnswer {

	/**
	 * Constructor for this class.
	 * 
	 * @param response The response.
	 * @param version The version.
	 */
	public SnaImpl(DiameterClientResponse response, Version version) {
		super(response, version);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.SubscribeNotificationsAnswer#getExpiryTime()
	 */
	public Date getExpiryTime() {
		DiameterAVPDefinition def = ShUtils.getExpiryTimeAVP(getVersion());
		Date res = getTimeAVP(def);
		return res;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.SubscribeNotificationsAnswer#getUserData()
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
		StringBuilder res = new StringBuilder("Subscribe Notifications Answer (SNA): ");
		if (getUserData() != null) {
			res.append("\nUserData=").append(new String(getUserData()));
		}
		res.append("\nExpiryTime=").append(getExpiryTime());
		res.append(super.toString());
		return res.toString();
	}

}
