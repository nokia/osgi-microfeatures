// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging.rf;

import java.io.IOException;
import java.util.Date;

import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.ImsAnswerListener;
import com.alcatel_lucent.as.ims.diameter.ImsRequest;
import com.alcatel_lucent.as.ims.diameter.charging.ServiceInformation;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.AccountingRecordType;

/**
 * The accounting request (ACR).
 * 
 * Some AVPs cannot be set because they are set by the client when the request
 * is created (Accounting-Record-Type, Accounting-Record-Number,
 * Acct-Application-Id)
 */
public interface AccountingRequest
		extends ImsRequest {

	/**
	 * Gets the record type (mapped to the Accounting-Record-Type AVP).
	 * 
	 * @return The record type.
	 */
	public AccountingRecordType getRecordType();

	/**
	 * Gets the record number (mapped to the Accounting-Record-Number AVP).
	 * 
	 * @return The record number.
	 */
	public long getRecordNumber();

	/**
	 * Gets the application identifier (mapped to the Acct-Application-Id AVP).
	 * 
	 * @return The identifier.
	 */
	public long getApplicationId();

	/**
	 * Sets the user name (mapped to the User-Name AVP).
	 * 
	 * @param username The user name.
	 */
	public void setUserName(String username);

	/**
	 * Sets the interim interval (mapped to the Acct-Interim-Interval AVP).
	 * 
	 * @param interval The interim interval.
	 */
	public void setInterimInterval(long interval);

	/**
	 * Sets the event timestamp (mapped to the Event-Timestamp AVP).
	 * 
	 * @param timestamp The timestamp.
	 */
	public void setEventTimestamp(Date timestamp);

	/**
	 * Sets the service context identifier (mapped to the Service-Context-Id).
	 * 
	 * @param identifier The identifier.
	 */
	public void setServiceContextId(String identifier);

	/**
	 * Sets the service information (mapped to the Service-Information).
	 * 
	 * @param info The service information.
	 */
	public void setServiceInformation(ServiceInformation info);

	/**
	 * Sends the request to a server.
	 * 
	 * @return The answer received from the server.
	 * @exception DiameterMissingAVPException if a mandatory or conditional AVP is
	 *              missing
	 * @exception IOException if an error occurs when sending the request.
	 */
	public AccountingAnswer execute()
		throws DiameterMissingAVPException, IOException;

	/**
	 * Sends asynchronously the request to a server.
	 * 
	 * @param listener The listener.
	 * @exception DiameterMissingAVPException if a mandatory or conditional AVP is
	 *              missing
	 */
	public void execute(ImsAnswerListener<AccountingRequest, AccountingAnswer> listener)
		throws DiameterMissingAVPException;
}
