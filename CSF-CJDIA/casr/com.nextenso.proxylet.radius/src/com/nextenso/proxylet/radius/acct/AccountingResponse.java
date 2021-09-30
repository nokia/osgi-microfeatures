package com.nextenso.proxylet.radius.acct;

import com.nextenso.proxylet.radius.RadiusMessage;

/**
 * This interface encapsulates an Accounting response.
 */
public interface AccountingResponse
		extends RadiusMessage {

	/**
	 * The Accounting-Response message code (5).
	 * 
	 * @deprecated
	 * @use AcctUtils.CODE_ACCOUNTING_REQUEST
	 */
	@Deprecated
	public static final int ACCT_RESPONSE_CODE = AcctUtils.CODE_ACCOUNTING_RESPONSE;

	/**
	 * The Disconnect-Ack message code (41).
	 * 
	 * @deprecated
	 * @use DisconnectUtils.CODE_DISCONNECT_ACK
	 */
	@Deprecated
	public static final int DISCONNECT_ACK_CODE = DisconnectUtils.CODE_DISCONNECT_ACK;

	/**
	 * The Disconnect-Nak message code (42).
	 * 
	 * @deprecated
	 * @use DisconnectUtils.CODE_DISCONNECT_NAK
	 */
	@Deprecated
	public static final int DISCONNECT_NAK_CODE = DisconnectUtils.CODE_DISCONNECT_NAK;

	/**
	 * Gets the associated request.
	 * 
	 * @return The request.
	 */
	public AccountingRequest getRequest();

	/**
	 * Sets the message code. This method should be used when responding to a
	 * Disconnect Request.
	 * 
	 * @param code The code.
	 */
	public void setCode(int code);

}
