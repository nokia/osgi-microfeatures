package com.alcatel_lucent.as.ims.diameter.charging.ro;

import java.io.IOException;
import java.util.Date;

import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.ImsAnswerListener;
import com.alcatel_lucent.as.ims.diameter.ImsRequest;
import com.alcatel_lucent.as.ims.diameter.charging.MultipleServicesCreditControl;
import com.alcatel_lucent.as.ims.diameter.charging.SubscriptionId;
import com.alcatel_lucent.as.ims.diameter.charging.UserEquipmentInfo;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.AocRequestType;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.MultipleServicesIndicator;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.RequestedAction;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.TerminationCause;

/**
 * The Credit Control Request (CCR).
 */
public interface CreditControlRequest
		extends ImsRequest {

	/**
	 * Sends the request to a server.
	 * 
	 * @return The answer received from the server.
	 * @exception DiameterMissingAVPException if a mandatory or conditional AVP is
	 *              missing
	 * @exception IOException if an error occurs when sending the request.
	 */
	public CreditControlAnswer execute()
		throws DiameterMissingAVPException, IOException;

	/**
	 * Sends asynchronously the request to a server.
	 * 
	 * @param listener The listener.
	 * @exception DiameterMissingAVPException if a mandatory or conditional AVP is
	 *              missing
	 */
	public void execute(ImsAnswerListener<CreditControlRequest, CreditControlAnswer> listener)
		throws DiameterMissingAVPException;

	/**
	 * Sets the user name (mapped to the User-Name AVP).
	 * 
	 * @param username The user name.
	 */
	public void setUserName(String username);

	/**
	 * Sets the event time stamp (mapped to the Event-Timestamp AVP).
	 * 
	 * @param timestamp The time stamp.
	 */
	public void setEventTimestamp(Date timestamp);

	/**
	 * Adds a subscription identifier (mapped to the Subscription-Id AVP).
	 * 
	 * @param subscriptionId The identifier to be added.
	 */
	public void addSubscriptionId(SubscriptionId subscriptionId);

	/**
	 * Sets the termination cause (mapped to the Termination-Cause AVP).
	 * 
	 * @param cause The cause.
	 */
	public void setTerminationCause(TerminationCause cause);

	/**
	 * Sets the requested action (mapped to the Requested-Action AVP).
	 * 
	 * It contains the requested action being sent by Credit-Control-Request
	 * command where the CC-Request-Type is set to EVENT_REQUEST
	 * 
	 * @param action The action.
	 */
	public void setRequestedAction(RequestedAction action);

	/**
	 * Sets the AoC request type (mapped to the AoC-Request-Type AVP).
	 * 
	 * @param type The type.
	 */
	public void setAocRequestType(AocRequestType type);

	/**
	 * Sets the multiple services indicator (mapped to the
	 * Multiple-Services-Indicator AVP).
	 * 
	 * @param indicator The indicator.
	 */
	public void setMultipleServicesIndicator(MultipleServicesIndicator indicator);

	/**
	 * Adds an additional 3GPP specific charging parameter (mapped to the
	 * Multiple-Services-Credit-Control AVP).
	 * 
	 * @param parameter The parameter.
	 */
	public void addMultipleServicesCreditControl(MultipleServicesCreditControl parameter);

	/**
	 * Sets the information to correlate credit-control requests (mapped to the
	 * CC-Correlation-Id AVP).
	 * 
	 * @param info The information.
	 */
	public void setCcCorrelationId(byte[] info);

	/**
	 * Sets the indication the identity and capability of the terminal the
	 * subscriber is using (mapped to the User-Equipment-Info AVP).
	 * 
	 * @param info The indication.
	 */
	public void setUserEquipmentInfo(UserEquipmentInfo info);

	/**
	 * Gets the the request number (mapped to the CC-Request-Number AVP).
	 * 
	 * @return The CC-Request-Number.
	 */
	public long getRequestNumber();

	/**
	 * Sets the request number (mapped to the CC-Request-Number AVP).
	 * 
	 * Please note that using this method changes the number of this request but it
	 * does not modify the remembered request number in the client.
	 * 
	 * @param number The request number.
	 */
	public void setRequestNumber(long number);
}
