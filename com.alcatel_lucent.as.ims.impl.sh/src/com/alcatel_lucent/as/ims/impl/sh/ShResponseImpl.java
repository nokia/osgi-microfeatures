// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.impl.sh;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.common.AbstractImsAnswer;
import com.alcatel_lucent.as.ims.diameter.sh.ShAnswer;
import com.alcatel_lucent.as.ims.diameter.sh.ShUtils;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;

/**
 * The abstract Sh Response.
 */
public class ShResponseImpl
		extends AbstractImsAnswer
		implements ShAnswer {

	public ShResponseImpl(DiameterClientResponse response, Version version) {
		super(response, version);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.common.AbstractImsAnswer#getPublicIdentity()
	 */
	@Override
	public String getPublicIdentity() {
		DiameterAVPDefinition def = ShUtils.getPublicIdentityAvpDefinition(getVersion());
		String res = getUTF8StringAVP(def);
		return res;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.ShAnswer#getWildcardedPSI()
	 */
	public String getWildcardedPSI() {
		DiameterAVPDefinition def = ShUtils.getWildcardedPSIAVP(getVersion());
		String res = getUTF8StringAVP(def);
		return res;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.ShAnswer#getWildcardedIMPU()
	 */
	public String getWildcardedIMPU() {
		DiameterAVPDefinition def = ShUtils.getWildcardedIMPUAVP(getVersion());
		String res = getUTF8StringAVP(def);
		return res;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder();
		res.append("\nPublic-Identity: ").append(getPublicIdentity());
		res.append("\nWildcarded-PSI: ").append(getWildcardedPSI());
		res.append("\nWildcarded-IMPU: ").append(getWildcardedIMPU());
		res.append(super.toString());
		return res.toString();
	}

}
