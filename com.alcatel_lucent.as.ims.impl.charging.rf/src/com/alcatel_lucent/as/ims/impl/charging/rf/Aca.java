package com.alcatel_lucent.as.ims.impl.charging.rf;

import java.util.Date;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.AccountingRecordType;
import com.alcatel_lucent.as.ims.diameter.charging.rf.AccountingAnswer;
import com.alcatel_lucent.as.ims.diameter.common.AbstractImsAnswer;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;

/**
 * The ACA implementation.
 */
public class Aca
		extends AbstractImsAnswer
		implements AccountingAnswer {

	/**
	 * Constructor for this class.
	 * 
	 * @param response The client response.
	 * @param version The Version.
	 */
	public Aca(DiameterClientResponse response, Version version) {
		super(response, version);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.rf.AccountingAnswer#getApplicationId()
	 */
	public long getApplicationId() {
		long res = getUnsigned32AVP(DiameterBaseConstants.AVP_ACCT_APPLICATION_ID);
		return res;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.rf.AccountingAnswer#getEventTimeStamp()
	 */
	public Date getEventTimeStamp() {
		Date res = getTimeAVP(DiameterBaseConstants.AVP_EVENT_TIMESTAMP);
		return res;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.rf.AccountingAnswer#getInterimInterval()
	 */
	public long getInterimInterval() {
		Long res = getUnsigned32AVP(DiameterBaseConstants.AVP_ACCT_INTERIM_INTERVAL);
		if (res == null) {
			res = Long.valueOf(0);
		}
		return res;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.rf.AccountingAnswer#getRecordNumber()
	 */
	public long getRecordNumber() {
		long res = getUnsigned32AVP(DiameterBaseConstants.AVP_ACCOUNTING_RECORD_TYPE);
		return res;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.rf.AccountingAnswer#getRecordType()
	 */
	public AccountingRecordType getRecordType() {
		int avpValue = getEnumeratedAVP(DiameterBaseConstants.AVP_ACCOUNTING_RECORD_TYPE);
		AccountingRecordType res = AccountingRecordType.getData(avpValue);
		return res;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.common.AbstractImsAnswer#getPublicIdentity()
	 */
	@Override
	public String getPublicIdentity() {
		// not used in Rf
		return null;
	}

}
