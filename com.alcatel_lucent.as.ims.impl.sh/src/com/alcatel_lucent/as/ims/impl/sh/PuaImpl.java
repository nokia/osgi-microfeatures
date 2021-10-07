package com.alcatel_lucent.as.ims.impl.sh;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.sh.ProfileUpdateAnswer;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;

/**
 * The Profile Update Answer (PUA) Implementation.
 */
public class PuaImpl
		extends ShResponseImpl
		implements ProfileUpdateAnswer {

	public PuaImpl(DiameterClientResponse response, Version version) {
		super(response, version);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder("Profile Update Answer (PUA): ");
		res.append(super.toString());
		return res.toString();
	}

}
