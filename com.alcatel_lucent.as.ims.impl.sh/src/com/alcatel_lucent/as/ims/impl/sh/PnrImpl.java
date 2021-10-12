// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.impl.sh;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.SupportedFeatures;
import com.alcatel_lucent.as.ims.diameter.sh.PushNotificationAnswer;
import com.alcatel_lucent.as.ims.diameter.sh.PushNotificationRequest;
import com.alcatel_lucent.as.ims.diameter.sh.ShUtils;
import com.alcatel_lucent.as.ims.diameter.sh.UserIdentity;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.DiameterRequest;
import com.nextenso.proxylet.diameter.DiameterResponse;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * The PNR Implementation.
 */
public class PnrImpl
		implements PushNotificationRequest {

	private PushNotificationAnswer _answer = null;
	private DiameterRequest _request = null;
	private Version _version = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param request The diameter request.
	 * @param version The Sh version.
	 */
	public PnrImpl(DiameterRequest request, Version version) {
		if (request == null) {
			throw new NullPointerException("no request");
		}
		if (version == null) {
			throw new NullPointerException("no version");
		}
		_request = request;
		_version = version;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.PushNotificationRequest#getAnswer()
	 */
	public PushNotificationAnswer getAnswer() {
		if (_answer == null) {
			DiameterResponse response = _request.getResponse();
			_answer = new PnaImpl(response);
		}
		return _answer;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.PushNotificationRequest#getAvp(com.nextenso.proxylet.diameter.DiameterAVPDefinition)
	 */
	public DiameterAVP getAvp(DiameterAVPDefinition definition) {
		DiameterAVP res = _request.getDiameterAVP(definition);
		return res;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.PushNotificationRequest#getSupportedFeatures()
	 */
	public List<SupportedFeatures> getSupportedFeatures() {
		DiameterAVPDefinition def = ShUtils.getSupportedFeaturesAVP(_version);
		if (def == null) {
			return null;
		}

		// Retrieve supported features AVP
		DiameterAVP avp = getAvp(def);

		if (avp == null) {
			return null;
		}

		List<SupportedFeatures> supportedFeatures = new ArrayList<SupportedFeatures>(avp.getValueSize());

		// Retrieve the different values of the supported supported features AVP
		for (int i = 0; i < avp.getValueSize(); i++) {
			byte[] data = avp.getValue(i);
			SupportedFeatures feature = new SupportedFeatures(data, _version);
			supportedFeatures.add(feature);
		}

		return supportedFeatures;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.PushNotificationRequest#getUserData()
	 */
	public byte[] getUserData() {
		DiameterAVPDefinition def = ShUtils.getUserDataAvpDefinition(_version);
		if (def == null) {
			return null;
		}

		DiameterAVP avp = getAvp(def);
		if (avp == null) {
			return null;
		}

		byte[] res = avp.getValue();
		return res;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.PushNotificationRequest#getUserIdentity()
	 */
	public UserIdentity getUserIdentity() {
		DiameterAVPDefinition def = ShUtils.getUserIdentityAvpDefinition(_version);
		if (def == null) {
			return null;
		}

		DiameterAVP avp = getAvp(def);
		if (avp == null) {
			return null;
		}
		
		UserIdentity res = new UserIdentity(avp, _version);
		return res;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.PushNotificationRequest#getWildcardedIMPU()
	 */
	public String getWildcardedIMPU() {
		DiameterAVPDefinition def = ShUtils.getWildcardedIMPUAVP(_version);
		
		DiameterAVP avp = getAvp(def);
		if (avp == null) {
			return null;
		}

		String res = UTF8StringFormat.getUtf8String(avp.getValue());
		return res;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.PushNotificationRequest#getWildcardedPSI()
	 */
	public String getWildcardedPSI() {
		DiameterAVPDefinition def = ShUtils.getWildcardedPSIAVP(_version);
		
		DiameterAVP avp = getAvp(def);
		if (avp == null) {
			return null;
		}

		String res = UTF8StringFormat.getUtf8String(avp.getValue());
		return res;
	}

}
