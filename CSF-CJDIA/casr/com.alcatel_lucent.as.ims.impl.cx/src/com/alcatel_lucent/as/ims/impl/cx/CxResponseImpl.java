package com.alcatel_lucent.as.ims.impl.cx;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.common.AbstractImsAnswer;
import com.alcatel_lucent.as.ims.diameter.cx.CxAnswer;
import com.alcatel_lucent.as.ims.diameter.cx.CxUtils;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;

/**
 * The Cx Response implementation.
 */
public class CxResponseImpl
		extends AbstractImsAnswer
		implements CxAnswer {

	public CxResponseImpl(DiameterClientResponse response, Version version) {
		super(response, version);
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.common.ImsAnswer#getPublicIdentity()
	 */
	@Override
	public String getPublicIdentity() {
		DiameterAVPDefinition def = CxUtils.getPublicIdentityAVP(getVersion());
		String res = getUTF8StringAVP(def);
		return res;
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.CxResponse#getServerName()
	 */
	public String getServerName() {
		DiameterAVPDefinition def = CxUtils.getServerNameAVP(getVersion());
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
		res.append("\nServer-Name: ").append(getServerName());
		res.append(super.toString());
		return res.toString();
	}

}
