// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.impl.sh;

import java.io.IOException;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.ImsAnswerListener;
import com.alcatel_lucent.as.ims.diameter.sh.ProfileUpdateAnswer;
import com.alcatel_lucent.as.ims.diameter.sh.ProfileUpdateRequest;
import com.alcatel_lucent.as.ims.diameter.sh.ShUtils;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.client.DiameterClientListener;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;

/**
 * The Profile Update Request implementation.
 */
public class PurImpl
		extends ShRequestImpl
		implements ProfileUpdateRequest, DiameterClientListener {

	private ImsAnswerListener<ProfileUpdateRequest, ProfileUpdateAnswer> _listener;

	/**
	 * 
	 * Constructor for this class.
	 * 
	 * @param request The request.
	 * @param version The version.
	 */
	public PurImpl(DiameterClientRequest request, Version version) {
		super(request, version);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.ProfileUpdateRequest#execute()
	 */
	public ProfileUpdateAnswer execute()
		throws DiameterMissingAVPException, IOException {
		checkParameters();
		DiameterClientResponse response = getRequest().execute();

		return new PuaImpl(response, getVersion());
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.ProfileUpdateRequest#execute(com.alcatel_lucent.as.ims.diameter.ImsAnswerListener)
	 */
	public void execute(ImsAnswerListener<ProfileUpdateRequest, ProfileUpdateAnswer> listener)
		throws DiameterMissingAVPException {
		checkParameters();
		_listener = listener;
		getRequest().execute(this);
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClientListener#handleException(com.nextenso.proxylet.diameter.client.DiameterClientRequest,
	 *      java.io.IOException)
	 */
	public void handleException(DiameterClientRequest request, java.io.IOException ioe) {
		_listener.handleException(this, ioe);
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClientListener#handleResponse(com.nextenso.proxylet.diameter.client.DiameterClientRequest,
	 *      com.nextenso.proxylet.diameter.client.DiameterClientResponse)
	 */
	public void handleResponse(DiameterClientRequest request, DiameterClientResponse response) {
		ProfileUpdateAnswer updateResp = new PuaImpl(response, getVersion());
		_listener.handleAnswer(this, updateResp);
	}

	/**
	 * Checks if mandatory AVPs have been set on the request.
	 * 
	 * @exception DiameterMissingAVPException if any mandatory AVP is missing
	 */
	private void checkParameters()
		throws DiameterMissingAVPException {
		DiameterClientRequest request = getRequest();
		DiameterAVPDefinition def = ShUtils.getUserIdentityAvpDefinition(getVersion());
		if (request.getDiameterAVP(def) == null) {
			throw new DiameterMissingAVPException(def.getAVPCode());
		}

		def = ShUtils.getUserDataAvpDefinition(getVersion());
		if (request.getDiameterAVP(def) == null) {
			throw new DiameterMissingAVPException(def.getAVPCode());
		}
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.ProfileUpdateRequest#setUserData(byte[])
	 */
	public void setUserData(byte[] data) {
		setOctetStringAVP(data, ShUtils.getUserDataAvpDefinition(getVersion()), true);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.common.AbstractImsRequest#toString()
	 */
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder("Profile Update Request: ");
		res.append(super.toString());
		return res.toString();
	}

}
