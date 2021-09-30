package com.alcatel_lucent.as.ims.diameter.charging.rf;

import java.util.Date;

import com.alcatel_lucent.as.ims.diameter.ImsAnswer;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.AccountingRecordType;

/**
 * The Accounting Answer (ACA).
 */
public interface AccountingAnswer
		extends ImsAnswer {

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
	 * Gets the user name (mapped to the User-Name AVP).
	 * 
	 * @return The user name.
	 */
	public String getUserName();

	/**
	 * Gets the interim interval (mapped to the Acct-Interim-Interval AVP).
	 * 
	 * @return The interval.
	 */
	public long getInterimInterval();

	/**
	 * Gets the event timestamp.
	 * 
	 * @return The timestamp (mapped to the Event-Timestamp AVP).
	 */
	public Date getEventTimeStamp();
}
