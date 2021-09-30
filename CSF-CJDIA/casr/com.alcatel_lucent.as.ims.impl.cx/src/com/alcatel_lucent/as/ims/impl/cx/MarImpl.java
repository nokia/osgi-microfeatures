package com.alcatel_lucent.as.ims.impl.cx;

import java.io.IOException;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.ImsAnswerListener;
import com.alcatel_lucent.as.ims.diameter.cx.CxUtils;
import com.alcatel_lucent.as.ims.diameter.cx.MultimediaAuthAnswer;
import com.alcatel_lucent.as.ims.diameter.cx.MultimediaAuthRequest;
import com.alcatel_lucent.as.ims.diameter.cx.SIPAuthDataItem;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.client.DiameterClientListener;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;

/**
 * The MAR implementation.
 */
public class MarImpl
		extends CxRequestImpl
		implements MultimediaAuthRequest, DiameterClientListener {

	private ImsAnswerListener<MultimediaAuthRequest, MultimediaAuthAnswer> _listener;

	public MarImpl(DiameterClientRequest request, Version version) {
		super(request, version);
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.MultimediaAuthRequest#setSipAuthDataItem(com.alcatel_lucent.as.interfaces.gpp.diameter.cx.SIPAuthDataItem)
	 */
	public void setSipAuthDataItem(SIPAuthDataItem item) {
		DiameterAVPDefinition def = CxUtils.getSipAuthDataItemAVP(getVersion());
		if (def == null) {
			return;
		}

		if (item == null) {
			getRequest().removeDiameterAVP(def);
		} else {
			DiameterAVP avp = item.toAvp(getVersion());
			if (avp != null) {
				setOctetStringAVP(avp.getValue(), def, false);
			}
		}

	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.MultimediaAuthRequest#setSipNumberAuthItems(long)
	 */
	public void setSipNumberAuthItems(long nb) {
		DiameterAVPDefinition def = CxUtils.getSipNumberAuthItemsAVP(getVersion());
		if (def == null) {
			return;
		}
		setUnsigned32AVP(nb, def);
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.MultimediaAuthRequest#execute()
	 */
	public MultimediaAuthAnswer execute()
		throws DiameterMissingAVPException, IOException {
		checkAVPs();
		DiameterClientResponse response = getRequest().execute();

		return new MaaImpl(response, getVersion());
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.MultimediaAuthRequest#execute(com.alcatel_lucent.as.interfaces.gpp.diameter.cx.event.MultimediaAuthAnswerListener)
	 */
	public void execute(ImsAnswerListener<MultimediaAuthRequest, MultimediaAuthAnswer> listener)
		throws DiameterMissingAVPException {
		checkAVPs();
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
		MultimediaAuthAnswer authResp = new MaaImpl(response, getVersion());
		_listener.handleAnswer(this, authResp);
	}

	/**
	 * Checks if mandatory AVPs have been set on the request.
	 * 
	 * @exception DiameterMissingAVPException if any mandatory AVP is missing
	 */
	private void checkAVPs()
		throws DiameterMissingAVPException {
		DiameterClientRequest request = getRequest();

		DiameterAVPDefinition def = CxUtils.getPublicIdentityAVP(getVersion());
		if (def != null && request.getDiameterAVP(def) == null) {
			throw new DiameterMissingAVPException(def.getAVPCode());
		}

		if (request.getDiameterAVP(DiameterBaseConstants.AVP_USER_NAME) == null) {
			throw new DiameterMissingAVPException(DiameterBaseConstants.AVP_USER_NAME.getAVPCode());
		}

		def = CxUtils.getSipNumberAuthItemsAVP(getVersion());
		if (def != null && request.getDiameterAVP(def) == null) {
			throw new DiameterMissingAVPException(def.getAVPCode());
		}

		def = CxUtils.getSipAuthDataItemAVP(getVersion());
		if (def != null && request.getDiameterAVP(def) == null) {
			throw new DiameterMissingAVPException(def.getAVPCode());
		}

		def = CxUtils.getServerNameAVP(getVersion());
		if (def != null && request.getDiameterAVP(def) == null) {
			throw new DiameterMissingAVPException(def.getAVPCode());
		}
	}
}
