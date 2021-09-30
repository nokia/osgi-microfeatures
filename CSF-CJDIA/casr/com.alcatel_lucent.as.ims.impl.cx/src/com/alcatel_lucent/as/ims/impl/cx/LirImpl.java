package com.alcatel_lucent.as.ims.impl.cx;

import java.io.IOException;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.ImsAnswerListener;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.cx.CxUtils;
import com.alcatel_lucent.as.ims.diameter.cx.LocationInfoAnswer;
import com.alcatel_lucent.as.ims.diameter.cx.LocationInfoRequest;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.client.DiameterClientListener;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;

/**
 * The LIR implementation.
 */
public class LirImpl
		extends CxRequestImpl
		implements LocationInfoRequest, DiameterClientListener {

	private ImsAnswerListener<LocationInfoRequest, LocationInfoAnswer> _listener;

	public LirImpl(DiameterClientRequest request, Version version) {
		super(request, version);
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.LocationInfoRequest#execute()
	 */
	public LocationInfoAnswer execute()
		throws DiameterMissingAVPException, IOException {
		checkParameters();
		DiameterClientResponse response = getRequest().execute();

		return new LiaImpl(response, getVersion());
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.LocationInfoRequest#execute(com.alcatel_lucent.as.interfaces.gpp.diameter.cx.event.LocationInfoAnswerListener)
	 */
	public void execute(ImsAnswerListener<LocationInfoRequest, LocationInfoAnswer> listener)
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
		LocationInfoAnswer resp = new LiaImpl(response, getVersion());
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
	}

}
