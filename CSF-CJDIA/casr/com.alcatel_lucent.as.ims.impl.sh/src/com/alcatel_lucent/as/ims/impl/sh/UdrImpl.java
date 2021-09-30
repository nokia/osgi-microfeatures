package com.alcatel_lucent.as.ims.impl.sh;

import java.io.IOException;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.ImsAnswerListener;
import com.alcatel_lucent.as.ims.diameter.cx.CxConstants.SessionPriority;
import com.alcatel_lucent.as.ims.diameter.sh.ShUtils;
import com.alcatel_lucent.as.ims.diameter.sh.UserDataAnswer;
import com.alcatel_lucent.as.ims.diameter.sh.UserDataRequest;
import com.alcatel_lucent.as.ims.diameter.sh.ShConstants.CurrentLocation;
import com.alcatel_lucent.as.ims.diameter.sh.ShConstants.DataReference;
import com.alcatel_lucent.as.ims.diameter.sh.ShConstants.IdentitySet;
import com.alcatel_lucent.as.ims.diameter.sh.ShConstants.RequestedDomain;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.client.DiameterClientListener;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;

/**
 * The User Data Request (UDR) implementation.
 */
public class UdrImpl
		extends ShRequestImpl
		implements UserDataRequest, DiameterClientListener {

	private ImsAnswerListener<UserDataRequest, UserDataAnswer> _listener;

	/**
	 * Constructor for this class.
	 * 
	 * @param request The diameter request.
	 * @param version The Sh version.
	 */
	public UdrImpl(DiameterClientRequest request, Version version) {
		super(request, version);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.UserDataRequest#addServiceIndication(byte[])
	 */
	public void addServiceIndication(byte[] serviceIndication) {
		DiameterAVPDefinition def = ShUtils.getServiceIndicationAvpDefinition(getVersion());
		addAVP(serviceIndication, def, true);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.UserDataRequest#addDataReference(com.alcatel_lucent.as.ims.diameter.sh.ShConstants.DataReference)
	 */
	public void addDataReference(DataReference dataReference) {
		if (dataReference == null) {
			return;
		}
		DiameterAVPDefinition def = ShUtils.getDataReferenceAvpDefinition(getVersion());
		byte[] data = EnumeratedFormat.toEnumerated(dataReference.getValue());
		addAVP(data, def, false);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.UserDataRequest#addIdentitySet(com.alcatel_lucent.as.ims.diameter.sh.ShConstants.IdentitySet)
	 */
	public void addIdentitySet(IdentitySet type) {
		if (type == null) {
			return;
		}
		DiameterAVPDefinition def = ShUtils.getIdentitySetAVP(getVersion());
		byte[] data = EnumeratedFormat.toEnumerated(type.getValue());
		addAVP(data, def, false);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.UserDataRequest#setRequestedDomain(com.alcatel_lucent.as.ims.diameter.sh.ShConstants.RequestedDomain)
	 */
	public void setRequestedDomain(RequestedDomain domain) {
		DiameterAVPDefinition def = ShUtils.getRequestedDomainAvpDefinition(getVersion());
		if (def == null) {
			return;
		}

		if (domain != null) {
			setEnumeratedAVP(domain.getValue(), def);
		} else {
			getRequest().removeDiameterAVP(def);
		}
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.UserDataRequest#setCurrentLocation(com.alcatel_lucent.as.ims.diameter.sh.ShConstants.CurrentLocation)
	 */
	public void setCurrentLocation(CurrentLocation location) {
		DiameterAVPDefinition def = ShUtils.getCurrentLocationAvpDefinition(getVersion());
		if (def == null) {
			return;
		}

		if (location != null) {
			setEnumeratedAVP(location.getValue(), def);
		} else {
			getRequest().removeDiameterAVP(def);
		}
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.UserDataRequest#setSessionPriority(com.alcatel_lucent.as.ims.diameter.cx.CxConstants.SessionPriority)
	 */
	public void setSessionPriority(SessionPriority priority) {
		DiameterAVPDefinition def = ShUtils.getSessionPriorityAVP(getVersion());
		if (def == null) {
			return;
		}

		if (priority != null) {
			setEnumeratedAVP(priority.getValue(), def);
		} else {
			getRequest().removeDiameterAVP(def);
		}
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.UserDataRequest#addDSAITag(byte[])
	 */
	public void addDSAITag(byte[] tag) {
		DiameterAVPDefinition def = ShUtils.getDSAITagAVP(getVersion());
		addAVP(tag, def, false);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.UserDataRequest#execute()
	 */
	public UserDataAnswer execute()
		throws DiameterMissingAVPException, IOException {
		checkParameters();
		DiameterClientResponse response = getRequest().execute();

		return new UdaImpl(response, getVersion());
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.UserDataRequest#execute(com.alcatel_lucent.as.ims.diameter.ImsAnswerListener)
	 */
	public void execute(ImsAnswerListener<UserDataRequest, UserDataAnswer> listener)
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
		UserDataAnswer pullResp = new UdaImpl(response, getVersion());
		_listener.handleAnswer(this, pullResp);
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
		def = ShUtils.getDataReferenceAvpDefinition(getVersion());
		if (request.getDiameterAVP(def) == null) {
			throw new DiameterMissingAVPException(def.getAVPCode());
		}
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder("User Data Request: ");
		res.append(super.toString());
		return res.toString();
	}

}
