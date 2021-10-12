// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.impl.cx;

import java.io.IOException;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.ImsAnswerListener;
import com.alcatel_lucent.as.ims.diameter.cx.CxUtils;
import com.alcatel_lucent.as.ims.diameter.cx.RestorationInfo;
import com.alcatel_lucent.as.ims.diameter.cx.ServerAssignmentAnswer;
import com.alcatel_lucent.as.ims.diameter.cx.ServerAssignmentRequest;
import com.alcatel_lucent.as.ims.diameter.cx.CxConstants.MultipleRegistrationIndication;
import com.alcatel_lucent.as.ims.diameter.cx.CxConstants.ServerAssignmentType;
import com.alcatel_lucent.as.ims.diameter.cx.CxConstants.UserDataAlreadyAvailable;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.client.DiameterClientListener;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * 
 * The SAR Implementation.
 */
public class SarImpl
		extends CxRequestImpl
		implements ServerAssignmentRequest, DiameterClientListener {

	private ImsAnswerListener<ServerAssignmentRequest, ServerAssignmentAnswer> _listener;

	/**
	 * Constructor for this class.
	 * 
	 * @param request The diameter client request.
	 * @param version The Cx Version.
	 */
	public SarImpl(DiameterClientRequest request, Version version) {
		super(request, version);
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.ServerAssignmentRequest#addPublicIdentity(java.lang.String)
	 */
	public void addPublicIdentity(String publicUserId) {
		DiameterAVPDefinition def = CxUtils.getPublicIdentityAVP(getVersion());
		if (def == null || publicUserId == null) {
			return;
		}

		byte[] id = UTF8StringFormat.toUtf8String(publicUserId);
		addAVP(id, def, false);
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.ServerAssignmentRequest#setPublicIdentities(java.util.List)
	 */
	public void setPublicIdentities(List<String> publicIdentities) {
		DiameterAVPDefinition def = CxUtils.getPublicIdentityAVP(getVersion());
		if (def == null) {
			return;
		}

		if (publicIdentities == null) {
			getRequest().removeDiameterAVP(def);
			return;
		}

		for (String id : publicIdentities) {
			byte[] idValue = UTF8StringFormat.toUtf8String(id);
			addAVP(idValue, def, false);
		}
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.ServerAssignmentRequest#setMultipleRegistrationIndication(com.alcatel_lucent.as.interfaces.gpp.diameter.cx.CxConstants.MultipleRegistrationIndication)
	 */
	public void setMultipleRegistrationIndication(MultipleRegistrationIndication indication) {
		DiameterAVPDefinition def = CxUtils.getMultipleRegistrationIndicationAVP(getVersion());
		if (def != null && indication == null) {
			getRequest().removeDiameterAVP(def);
			return;
		}
		setEnumeratedAVP(indication.getValue(), def);
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.ServerAssignmentRequest#setSCSCFRestorationInfo(com.alcatel_lucent.as.interfaces.gpp.diameter.cx.RestorationInfo)
	 */
	public void setSCSCFRestorationInfo(RestorationInfo information) {
		DiameterAVPDefinition def = CxUtils.getRestorationInfoAVP(getVersion());
		if (def == null) {
			return;
		}
		if (information == null) {
			getRequest().removeDiameterAVP(def);
			return;
		}
		DiameterAVP avp = information.toAvp(getVersion());
		if (avp != null) {
			setOctetStringAVP(avp.getValue(), def, false);
		}
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.ServerAssignmentRequest#setWildcardedIMPU(java.lang.String)
	 */
	public void setWildcardedIMPU(String impu) {
		DiameterAVPDefinition def = CxUtils.getWildcardedIMPUAVP(getVersion());
		setUTF8StringAVP(impu, def);
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.ServerAssignmentRequest#setWildcardedPSI(java.lang.String)
	 */
	public void setWildcardedPSI(String psi) {
		DiameterAVPDefinition def = CxUtils.getWildcardedPSIAVP(getVersion());
		setUTF8StringAVP(psi, def);
	}

	/**
	 * Sets the type of update the S-CSCF requests in the HSS.
	 * 
	 * @param assignmentType the requested type of update
	 */
	public void setServerAssignmentType(ServerAssignmentType assignmentType) {
		DiameterAVPDefinition def = CxUtils.getServerAssignmentTypeAVP(getVersion());
		if (def != null && assignmentType == null) {
			getRequest().removeDiameterAVP(def);
			return;
		}
		setEnumeratedAVP(assignmentType.getValue(), def);
	}

	/**
	 * Sets if the user profile is already available in the S-CSCF.
	 * 
	 * @param availability user profile availability on the S-CSCF
	 */
	public void setUserProfileAlreadyAvailable(UserDataAlreadyAvailable availability) {
		DiameterAVPDefinition def = CxUtils.getUserDataAlreadyAvailableAVP(getVersion());
		if (def != null && availability == null) {
			getRequest().removeDiameterAVP(def);
			return;
		}
		setEnumeratedAVP(availability.getValue(), def);
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.ServerAssignmentRequest#execute()
	 */
	public ServerAssignmentAnswer execute()
		throws DiameterMissingAVPException, IOException {
		checkParameters(getRequest());
		DiameterClientResponse response = getRequest().execute();

		return new SaaImpl(response, getVersion());
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.ServerAssignmentRequest#execute(com.alcatel_lucent.as.interfaces.gpp.diameter.cx.event.ServerAssignmentAnswerListener)
	 */
	public void execute(ImsAnswerListener<ServerAssignmentRequest, ServerAssignmentAnswer> listener)
		throws DiameterMissingAVPException {
		checkParameters(getRequest());
		this._listener = listener;
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
		ServerAssignmentAnswer resp = new SaaImpl(response, getVersion());
		_listener.handleAnswer(this, resp);
	}

	/**
	 * Checks if mandatory AVPs have been set on the request.
	 * 
	 * @exception DiameterMissingAVPException if any mandatory AVP is missing
	 */
	private void checkParameters(DiameterClientRequest request)
		throws DiameterMissingAVPException {
		DiameterAVPDefinition def = CxUtils.getServerNameAVP(getVersion());
		if (def != null && request.getDiameterAVP(def) == null) {
			throw new DiameterMissingAVPException(def.getAVPCode());
		}

		def = CxUtils.getServerAssignmentTypeAVP(getVersion());
		if (def != null && request.getDiameterAVP(def) == null) {
			throw new DiameterMissingAVPException(def.getAVPCode());
		}

		def = CxUtils.getUserDataAlreadyAvailableAVP(getVersion());
		if (def != null && request.getDiameterAVP(def) == null) {
			throw new DiameterMissingAVPException(def.getAVPCode());
		}
	}

}
