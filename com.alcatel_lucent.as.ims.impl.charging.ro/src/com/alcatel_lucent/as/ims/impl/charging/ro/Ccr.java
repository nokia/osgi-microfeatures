// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.impl.charging.ro;

import java.io.IOException;
import java.util.Date;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.ImsAnswerListener;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.AocRequestType;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.MultipleServicesIndicator;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.RequestedAction;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.TerminationCause;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingUtils;
import com.alcatel_lucent.as.ims.diameter.charging.MultipleServicesCreditControl;
import com.alcatel_lucent.as.ims.diameter.charging.SubscriptionId;
import com.alcatel_lucent.as.ims.diameter.charging.UserEquipmentInfo;
import com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlAnswer;
import com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlRequest;
import com.alcatel_lucent.as.ims.diameter.common.AbstractImsRequest;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.client.DiameterClientListener;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;

/**
 * The CCR.
 */
public class Ccr
		extends AbstractImsRequest
		implements CreditControlRequest, DiameterClientListener {

	private ImsAnswerListener<CreditControlRequest, CreditControlAnswer> _listener;

	protected Ccr(DiameterClientRequest request, Version version) {
		super(request, version);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlRequest#execute()
	 */
	public CreditControlAnswer execute()
		throws DiameterMissingAVPException, IOException {
		DiameterClientResponse response = getRequest().execute();
		return new Cca(response, getVersion());
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlRequest#execute(com.alcatel_lucent.as.ims.diameter.ImsAnswerListener)
	 */
	public void execute(ImsAnswerListener<CreditControlRequest, CreditControlAnswer> listener)
		throws DiameterMissingAVPException {
		_listener = listener;
		getRequest().execute(this);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlRequest#setEventTimestamp(java.util.Date)
	 */
	public void setEventTimestamp(Date timestamp) {
		setTimeAVP(timestamp, DiameterBaseConstants.AVP_EVENT_TIMESTAMP);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlRequest#addSubscriptionId(com.alcatel_lucent.as.ims.diameter.charging.SubscriptionId)
	 */
	public void addSubscriptionId(SubscriptionId subscriptionId) {
		if (subscriptionId != null) {
			addAvp(subscriptionId.toAvp());
		}
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlRequest#setTerminationCause(com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.TerminationCause)
	 */
	public void setTerminationCause(TerminationCause cause) {
		if (cause != null) {
			setEnumeratedAVP(cause.getValue(), DiameterBaseConstants.AVP_TERMINATION_CAUSE);
		} else {
			setAVP(null, DiameterBaseConstants.AVP_TERMINATION_CAUSE, false);
		}
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlRequest#setRequestedAction(com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.RequestedAction)
	 */
	public void setRequestedAction(RequestedAction action) {
		DiameterAVPDefinition def = ChargingUtils.getRequestedActionAVP();
		if (action != null) {
			setEnumeratedAVP(action.getValue(), def);
		} else {
			setAVP(null, def, false);
		}
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlRequest#setAocRequestType(com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.AocRequestType)
	 */
	public void setAocRequestType(AocRequestType type) {
		DiameterAVPDefinition def = ChargingUtils.getAocRequestTypeAVP(getVersion());
		if (def != null) {
			if (type != null) {
				setEnumeratedAVP(type.getValue(), def);
			} else {
				setAVP(null, def, false);
			}
		}
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlRequest#setMultipleServicesIndicator(com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.MultipleServicesIndicator)
	 */
	public void setMultipleServicesIndicator(MultipleServicesIndicator indicator) {
		DiameterAVPDefinition def = ChargingUtils.getMultipleServiceIndicatorAVP();
		if (indicator != null) {
			setEnumeratedAVP(indicator.getValue(), def);
		} else {
			setAVP(null, def, false);
		}

	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlRequest#addMultipleServicesCreditControl(com.alcatel_lucent.as.ims.diameter.charging.MultipleServicesCreditControl)
	 */
	public void addMultipleServicesCreditControl(MultipleServicesCreditControl parameter) {
		if (parameter != null) {
			addAvp(parameter.toAvp(getVersion()));
		}
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlRequest#setCcCorrelationId(byte[])
	 */
	public void setCcCorrelationId(byte[] info) {
		setAVP(info, ChargingUtils.getCcCorrelationIdAVP(), true);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlRequest#setUserEquipmentInfo(com.alcatel_lucent.as.ims.diameter.charging.UserEquipmentInfo)
	 */
	public void setUserEquipmentInfo(UserEquipmentInfo info) {
		DiameterAVPDefinition def = ChargingUtils.getUserEquipmentInfoAVP();
		if (info != null) {
			setAVP(info.toAvp());
		} else {
			setAVP(null, def, false);
		}
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.common.AbstractImsRequest#setPublicUserID(java.lang.String)
	 */
	@Override
	public void setPublicUserID(String id) {
		// Not used in Ro
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.common.AbstractImsRequest#setServerName(java.lang.String)
	 */
	@Override
	public void setServerName(String scscfName) {
		// Not used in Ro
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClientListener#handleResponse(com.nextenso.proxylet.diameter.client.DiameterClientRequest,
	 *      com.nextenso.proxylet.diameter.client.DiameterClientResponse)
	 */
	public void handleResponse(DiameterClientRequest request, DiameterClientResponse response) {
		if (_listener != null) {
			Cca cca = new Cca(response, getVersion());
			_listener.handleAnswer(this, cca);
		}
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
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlRequest#getRequestNumber()
	 */
	@Override
	public long getRequestNumber() {
		return getUnsigned32AVP(ChargingUtils.getCcRequestNumberAVP());
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlRequest#setRequestNumber(long)
	 */
	@Override
	public void setRequestNumber(long number) {
		setUnsigned32AVP(number, ChargingUtils.getCcRequestNumberAVP());
	}

}
