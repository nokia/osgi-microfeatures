package com.alcatel_lucent.as.ims.impl.cx;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.cx.CxUtils;
import com.alcatel_lucent.as.ims.diameter.cx.MultimediaAuthAnswer;
import com.alcatel_lucent.as.ims.diameter.cx.SIPAuthDataItem;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The MAA implementation.
 */
public class MaaImpl
		extends CxResponseImpl
		implements MultimediaAuthAnswer {

	/**
	 * Constructor for this class.
	 * 
	 * @param response The diameter client response.
	 * @param version The version.
	 */
	public MaaImpl(DiameterClientResponse response, Version version) {
		super(response, version);
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.MultimediaAuthAnswer#getNumberAuthItems()
	 */
	public Long getNumberAuthItems() {
		DiameterAVPDefinition def = CxUtils.getSipNumberAuthItemsAVP(getVersion());
		if (def == null) {
			return null;
		}
		DiameterAVP avp = getResponse().getDiameterAVP(def);
		if (avp == null)
			return null;

		return Long.valueOf(Unsigned32Format.getUnsigned32(avp.getValue(), 0));
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.MultimediaAuthAnswer#getAuthenticationData()
	 */
	public List<SIPAuthDataItem> getAuthenticationData() {
		DiameterAVPDefinition def = CxUtils.getSipAuthDataItemAVP(getVersion());
		if (def == null) {
			return null;
		}
		List<SIPAuthDataItem> res = new ArrayList<SIPAuthDataItem>();

		DiameterAVP avp = getResponse().getDiameterAVP(def);
		if (avp != null) {
			for (int i = 0; i < avp.getValueSize(); i++) {
				byte[] value = avp.getValue(i);
				res.add(new SIPAuthDataItem(value, getVersion()));
			}
		}

		return res;
	}
}
