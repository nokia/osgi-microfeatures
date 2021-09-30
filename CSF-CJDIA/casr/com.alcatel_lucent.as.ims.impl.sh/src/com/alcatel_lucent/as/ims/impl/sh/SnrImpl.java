package com.alcatel_lucent.as.ims.impl.sh;

import java.io.IOException;
import java.util.Date;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.ImsAnswerListener;
import com.alcatel_lucent.as.ims.diameter.sh.ShUtils;
import com.alcatel_lucent.as.ims.diameter.sh.SubscribeNotificationsAnswer;
import com.alcatel_lucent.as.ims.diameter.sh.SubscribeNotificationsRequest;
import com.alcatel_lucent.as.ims.diameter.sh.ShConstants.DataReference;
import com.alcatel_lucent.as.ims.diameter.sh.ShConstants.IdentitySet;
import com.alcatel_lucent.as.ims.diameter.sh.ShConstants.SendDataIndication;
import com.alcatel_lucent.as.ims.diameter.sh.ShConstants.SubsReqType;
import com.alcatel_lucent.as.ims.diameter.sh.UserIdentity;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.client.DiameterClientListener;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;

/**
 * The Subscribe Notifications Request (SNR) implementation.
 */
public class SnrImpl
		extends ShRequestImpl
		implements SubscribeNotificationsRequest, DiameterClientListener {

	private ImsAnswerListener<SubscribeNotificationsRequest, SubscribeNotificationsAnswer> _listener;

	/**
	 * Constructor for this class.
	 * 
	 * @param request The diameter request.
	 * @param version The Sh version.
	 */
	public SnrImpl(DiameterClientRequest request, Version version) {
		super(request, version);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.impl.sh.ShRequestImpl#setServerName(java.lang.String)
	 */
	@Override
	public void setServerName(String serverName) {
		setUTF8StringAVP(serverName, ShUtils.getServerNameAvpDefinition(getVersion()));
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.SubscribeNotificationsRequest#execute()
	 */
	public SubscribeNotificationsAnswer execute()
		throws DiameterMissingAVPException, IOException {
		checkParameters();
		DiameterClientResponse response = getRequest().execute();

		return new SnaImpl(response, getVersion());
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.SubscribeNotificationsRequest#execute(com.alcatel_lucent.as.ims.diameter.ImsAnswerListener)
	 */
	public void execute(ImsAnswerListener<SubscribeNotificationsRequest, SubscribeNotificationsAnswer> listener)
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
		SubscribeNotificationsAnswer subsNotifResp = new SnaImpl(response, getVersion());
		_listener.handleAnswer(this, subsNotifResp);
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
		if (request.getDiameterAVP(def) == null)
			throw new DiameterMissingAVPException(def.getAVPCode());

		def = ShUtils.getSubsReqTypeAvpDefinition(getVersion());
		if (request.getDiameterAVP(def) == null) {
			throw new DiameterMissingAVPException(def.getAVPCode());
		}

		def = ShUtils.getDataReferenceAvpDefinition(getVersion());
		if (request.getDiameterAVP(def) == null) {
			throw new DiameterMissingAVPException(def.getAVPCode());
		}
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.SubscribeNotificationsRequest#addDataReference(com.alcatel_lucent.as.ims.diameter.sh.ShConstants.DataReference)
	 */
	public void addDataReference(DataReference dataReference) {
		DiameterAVPDefinition def = ShUtils.getDataReferenceAvpDefinition(getVersion());
		if (def == null || dataReference == null) {
			return;
		}
		addAVP(EnumeratedFormat.toEnumerated(dataReference.getValue()), def, false);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.SubscribeNotificationsRequest#addServiceIndication(byte[])
	 */
	public void addServiceIndication(byte[] serviceIndication) {
		DiameterAVPDefinition def = ShUtils.getServiceIndicationAvpDefinition(getVersion());
		if (def == null || serviceIndication == null) {
			return;
		}
		addAVP(serviceIndication, def, true);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.SubscribeNotificationsRequest#setExpiryTime(java.util.Date)
	 */
	public void setExpiryTime(Date time) {
		DiameterAVPDefinition def = ShUtils.getExpiryTimeAVP(getVersion());
		if (def == null) {
			return;
		}
		setTimeAVP(time, def);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.SubscribeNotificationsRequest#setSendDataIndication(com.alcatel_lucent.as.ims.diameter.sh.ShConstants.SendDataIndication)
	 */
	public void setSendDataIndication(SendDataIndication indication) {
		DiameterAVPDefinition def = ShUtils.getSendDataIndicationAVP(getVersion());
		if (def == null) {
			return;
		}
		if (indication == null) {
			getRequest().removeDiameterAVP(def);
			return;
		}
		setEnumeratedAVP(indication.getValue(), def);

	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.SubscribeNotificationsRequest#setSubsReqType(com.alcatel_lucent.as.ims.diameter.sh.ShConstants.SubsReqType)
	 */
	public void setSubsReqType(SubsReqType type) {
		DiameterAVPDefinition def = ShUtils.getSubsReqTypeAvpDefinition(getVersion());
		if (def == null) {
			return;
		}
		if (type == null) {
			getRequest().removeDiameterAVP(def);
			return;
		}
		setEnumeratedAVP(type.getValue(), def);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.SubscribeNotificationsRequest#addDSAITag(byte[])
	 */
	public void addDSAITag(byte[] tag) {
		DiameterAVPDefinition def = ShUtils.getDSAITagAVP(getVersion());
		addAVP(tag, def, false);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.SubscribeNotificationsRequest#addIdentitySet(com.alcatel_lucent.as.ims.diameter.sh.ShConstants.IdentitySet)
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
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder("Subscribe Notifications Request: ");
		res.append(super.toString());
		return res.toString();
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.SubscribeNotificationsRequest#getSubsReqType()
	 */
	public SubsReqType getSubsReqType() {
		DiameterAVPDefinition def = ShUtils.getSubsReqTypeAvpDefinition(getVersion());
		if (def == null) {
			return null;
		}

		return SubsReqType.getData(this.getEnumeratedAVP(def));
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.SubscribeNotificationsRequest#getUserIndentity()
	 */
	public UserIdentity getUserIndentity() {
		DiameterAVP avp = getRequest().getDiameterAVP(ShUtils.getUserIdentityAvpDefinition(getVersion()));
		UserIdentity userIdentity = null;
		if (avp != null) {
			userIdentity = new UserIdentity(avp, getVersion());
		}
		return userIdentity;
	}

}
