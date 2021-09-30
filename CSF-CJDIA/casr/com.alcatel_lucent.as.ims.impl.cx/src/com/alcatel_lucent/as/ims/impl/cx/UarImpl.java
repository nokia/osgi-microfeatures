package com.alcatel_lucent.as.ims.impl.cx;

import java.io.IOException;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.ImsAnswerListener;
import com.alcatel_lucent.as.ims.diameter.cx.CxUtils;
import com.alcatel_lucent.as.ims.diameter.cx.UserAuthorizationAnswer;
import com.alcatel_lucent.as.ims.diameter.cx.UserAuthorizationRequest;
import com.alcatel_lucent.as.ims.diameter.cx.CxConstants.UserAuthorizationType;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.client.DiameterClientListener;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;

/**
 * The UAR implementation.
 */
public class UarImpl
		extends CxRequestImpl
		implements UserAuthorizationRequest, DiameterClientListener {

	private ImsAnswerListener<UserAuthorizationRequest, UserAuthorizationAnswer> _listener;

	public UarImpl(DiameterClientRequest request, Version version) {
		super(request, version);
	}

	/**
	 * Sets the Visited-Network-Id.
	 * 
	 * @param id The identifier.
	 */
	public void setVisitedNetworkId(byte[] id) {
		DiameterAVPDefinition def = CxUtils.getVisitedNetworkIdentifierAVP(getVersion());
		if (def == null) {
			return;
		}
		setOctetStringAVP(id, def, true);
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.UserAuthorizationRequest#setAuthorizationType(com.alcatel_lucent.as.interfaces.gpp.diameter.cx.CxConstants.UserAuthorizationType)
	 */
	public void setAuthorizationType(UserAuthorizationType authType) {
		DiameterAVPDefinition def = CxUtils.getUserAuthorizationTypeAVP(getVersion());
		if (def == null) {
			return;
		}
		if (authType == null) {
			getRequest().removeDiameterAVP(def);
			return;
		}
		setEnumeratedAVP(authType.getValue(), def);
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.UserAuthorizationRequest#setUARFlags(java.lang.Long)
	 */
	public void setUARFlags(Long flags) {
		DiameterAVPDefinition def = CxUtils.getUARFlagsAVP(getVersion());
		if (def == null) {
			return;
		}
		if (flags == null) {
			getRequest().removeDiameterAVP(def);
			return;
		}
		setUnsigned32AVP(flags, def);
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.UserAuthorizationRequest#execute()
	 */
	public UserAuthorizationAnswer execute()
		throws DiameterMissingAVPException, IOException {
		checkParameters();
		DiameterClientResponse response = getRequest().execute();

		return new UaaImpl(response, getVersion());
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.UserAuthorizationRequest#execute(com.alcatel_lucent.as.interfaces.gpp.diameter.cx.event.UserAuthorizationAnswerListener)
	 */
	public void execute(ImsAnswerListener<UserAuthorizationRequest, UserAuthorizationAnswer> listener)
		throws DiameterMissingAVPException {
		checkParameters();
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
		UserAuthorizationAnswer resp = new UaaImpl(response, getVersion());
		_listener.handleAnswer(this, resp);
	}

	/**
	 * Checks if mandatory AVPs have been set on the request.
	 * 
	 * @exception DiameterMissingAVPException if any mandatory AVP is missing
	 */
	private void checkParameters()
		throws DiameterMissingAVPException {
		DiameterClientRequest request = getRequest();
		DiameterAVPDefinition def = CxUtils.getPublicIdentityAVP(getVersion());
		if (def != null && request.getDiameterAVP(def) == null) {
			throw new DiameterMissingAVPException(def.getAVPCode());
		}

		def = CxUtils.getVisitedNetworkIdentifierAVP(getVersion());
		if (def != null && request.getDiameterAVP(def) == null) {
			throw new DiameterMissingAVPException(def.getAVPCode());
		}

		def = DiameterBaseConstants.AVP_USER_NAME;
		if (request.getDiameterAVP(def) == null) {
			throw new DiameterMissingAVPException(def.getAVPCode());
		}
	}

}
