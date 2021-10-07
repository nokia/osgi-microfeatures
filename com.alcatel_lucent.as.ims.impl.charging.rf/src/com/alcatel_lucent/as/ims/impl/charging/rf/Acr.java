package com.alcatel_lucent.as.ims.impl.charging.rf;

import java.io.IOException;
import java.util.Date;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.ImsAnswerListener;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingUtils;
import com.alcatel_lucent.as.ims.diameter.charging.ServiceInformation;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.AccountingRecordType;
import com.alcatel_lucent.as.ims.diameter.charging.rf.AccountingAnswer;
import com.alcatel_lucent.as.ims.diameter.charging.rf.AccountingRequest;
import com.alcatel_lucent.as.ims.diameter.common.AbstractImsRequest;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.client.DiameterClientListener;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;

/**
 * The ACR Implementation.
 */
public class Acr
		extends AbstractImsRequest
		implements AccountingRequest, DiameterClientListener {

	private ImsAnswerListener<AccountingRequest, AccountingAnswer> _listener = null;
	private SessionClient _sessionClient = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param request The Diameter client request.
	 * @param version The 32.299 version.
	 */
	public Acr(DiameterClientRequest request, Version version) {
		super(request, version);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.rf.AccountingRequest#execute()
	 */
	public AccountingAnswer execute()
		throws DiameterMissingAVPException, IOException {
		DiameterClientResponse response = getRequest().execute();

		return new Aca(response, getVersion());
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.rf.AccountingRequest#execute(com.alcatel_lucent.as.ims.diameter.ImsAnswerListener)
	 */
	public void execute(ImsAnswerListener<AccountingRequest, AccountingAnswer> listener)
		throws DiameterMissingAVPException {
		_listener = listener;
		getRequest().execute(this);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.rf.AccountingRequest#getApplicationId()
	 */
	public long getApplicationId() {
		long res = getUnsigned32AVP(DiameterBaseConstants.AVP_ACCT_APPLICATION_ID);
		return res;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.rf.AccountingRequest#getRecordNumber()
	 */
	public long getRecordNumber() {
		long res = getUnsigned32AVP(DiameterBaseConstants.AVP_ACCOUNTING_RECORD_TYPE);
		return res;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.rf.AccountingRequest#getRecordType()
	 */
	public AccountingRecordType getRecordType() {
		int avpValue = getEnumeratedAVP(DiameterBaseConstants.AVP_ACCOUNTING_RECORD_TYPE);
		AccountingRecordType res = AccountingRecordType.getData(avpValue);
		return res;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.rf.AccountingRequest#setEventTimestamp(java.util.Date)
	 */
	public void setEventTimestamp(Date timestamp) {
		setTimeAVP(timestamp, DiameterBaseConstants.AVP_EVENT_TIMESTAMP);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.rf.AccountingRequest#setInterimInterval(long)
	 */
	public void setInterimInterval(long interval) {
		setUnsigned32AVP(interval, DiameterBaseConstants.AVP_ACCT_INTERIM_INTERVAL);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.rf.AccountingRequest#setServiceContextId(java.lang.String)
	 */
	public void setServiceContextId(String identifier) {
		setUTF8StringAVP(identifier, ChargingUtils.getServiceContextIdAVP());
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.rf.AccountingRequest#setServiceInformation(com.alcatel_lucent.as.ims.diameter.charging.ServiceInformation)
	 */
	public void setServiceInformation(ServiceInformation info) {
		DiameterAVPDefinition def = ChargingUtils.getServiceInformationAVP(getVersion());
		if (def == null) {
			return;
		}

		byte[] data = null;
		if (info != null) {
			DiameterAVP avp = info.toAvp(getVersion());
			data = avp.getValue();
		}
		setAVP(data, def, false);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.common.AbstractImsRequest#setPublicUserID(java.lang.String)
	 */
	@Override
	public void setPublicUserID(String id) {
		// not used in Rf	
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.common.AbstractImsRequest#setServerName(java.lang.String)
	 */
	@Override
	public void setServerName(String scscfName) {
		// not used in Rf		
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClientListener#handleException(com.nextenso.proxylet.diameter.client.DiameterClientRequest,
	 *      java.io.IOException)
	 */
	public void handleException(DiameterClientRequest request, IOException ioe) {
		if (_listener != null) {
			_listener.handleException(this, ioe);
		}
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClientListener#handleResponse(com.nextenso.proxylet.diameter.client.DiameterClientRequest,
	 *      com.nextenso.proxylet.diameter.client.DiameterClientResponse)
	 */
	public void handleResponse(DiameterClientRequest request, DiameterClientResponse response) {
		AccountingAnswer aca = new Aca(response, getVersion());
		notifyInterimIterval(aca.getInterimInterval());
		if (_listener != null) {
			_listener.handleAnswer(this, aca);
		}
	}

	/**
	 * Sets the client.
	 * 
	 * @param sessionClient The session client to know which client needs to be
	 *          notified.
	 */
	public void setSessionClient(SessionClient sessionClient) {
		_sessionClient = sessionClient;
	}

	/**
	 * Notifies the interim interval to the client.
	 * 
	 * @param interval The interim interval value in seconds.
	 */
	public void notifyInterimIterval(long interval) {
		if (_sessionClient != null) {
			_sessionClient.notifyInterimIterval(interval);
		}
	}

}
