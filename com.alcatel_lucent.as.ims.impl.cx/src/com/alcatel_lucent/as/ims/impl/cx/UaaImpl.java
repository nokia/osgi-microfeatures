package com.alcatel_lucent.as.ims.impl.cx;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.cx.CxUtils;
import com.alcatel_lucent.as.ims.diameter.cx.ServerCapabilities;
import com.alcatel_lucent.as.ims.diameter.cx.UserAuthorizationAnswer;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

public class UaaImpl
		extends CxResponseImpl
		implements UserAuthorizationAnswer {

	public UaaImpl(DiameterClientResponse response, Version version) {
		super(response, version);
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.UserAuthorizationAnswer#getServerCapabilities()
	 */
	public ServerCapabilities getServerCapabilities() {
		DiameterAVPDefinition def = CxUtils.getServerCapabilitiesAVP(getVersion());
		if (def == null) {
			return null;
		}
		DiameterAVP avp = getResponse().getDiameterAVP(def);
		if (avp == null) {
			return null;
		}

		return new ServerCapabilities(avp, getVersion());
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.UserAuthorizationAnswer#getWildcardedIMPU()
	 */
	public String getWildcardedIMPU() {
		DiameterAVPDefinition def = CxUtils.getWildcardedIMPUAVP(getVersion());
		if (def == null) {
			return null;
		}

		DiameterAVP avp = getResponse().getDiameterAVP(def);
		if (avp == null) {
			return null;
		}
		String res = UTF8StringFormat.getUtf8String(avp.getValue());
		return res;
	}

}
