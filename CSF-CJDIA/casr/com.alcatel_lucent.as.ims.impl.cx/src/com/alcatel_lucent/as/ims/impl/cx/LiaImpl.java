package com.alcatel_lucent.as.ims.impl.cx;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.cx.CxUtils;
import com.alcatel_lucent.as.ims.diameter.cx.LocationInfoAnswer;
import com.alcatel_lucent.as.ims.diameter.cx.ServerCapabilities;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * The LIA implementation.
 */
public class LiaImpl
		extends CxResponseImpl
		implements LocationInfoAnswer {

	public LiaImpl(DiameterClientResponse response, Version version) {
		super(response, version);
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.LocationInfoAnswer#getServerCapabilities()
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
		ServerCapabilities res = new ServerCapabilities(avp, getVersion());
		return res;
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.LocationInfoAnswer#getWildcardedIMPU()
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

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.LocationInfoAnswer#getWildcardedPSI()
	 */
	public String getWildcardedPSI() {
		DiameterAVPDefinition def = CxUtils.getWildcardedPSIAVP(getVersion());
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
