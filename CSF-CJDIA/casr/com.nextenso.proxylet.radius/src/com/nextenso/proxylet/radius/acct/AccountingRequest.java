package com.nextenso.proxylet.radius.acct;

import com.nextenso.proxylet.radius.RadiusMessage;

/**
 * This interface encapsulates an Accounting Request.
 */
public interface AccountingRequest
		extends RadiusMessage {

	/**
	 * The Accounting-Request message code (4).
	 * 
	 * <BR\>
	 * See AcctUtils.CODE_ACCOUNTING_REQUEST
	 * 
	 * @deprecated
	 */
	@Deprecated
	public static final int ACCT_REQUEST_CODE = AcctUtils.CODE_ACCOUNTING_REQUEST;

	/**
	 * The Disconnect-Request message code (40).
	 * 
	 * <BR\>
	 * See DisconnectUtils.CODE_DISCONNECT_REQUEST
	 * 
	 * @deprecated
	 */
	@Deprecated
	public static final int DISCONNECT_REQUEST_CODE = DisconnectUtils.CODE_DISCONNECT_REQUEST;

	/**
	 * Gets the associated response.
	 * 
	 * @return The response.
	 */
	public AccountingResponse getResponse();

	/**
	 * Sets the Radius server to which the request will be sent. <br/>
	 * This method overrides the default routing policy configured in the Agent. <br/>
	 * The server parameter should be in the format "host:port" or "host". If the
	 * port is not specified, the default radius ports is assumed (1813). <br/>
	 * The secret parameter may be null if the Agent configuration specifies the
	 * secret for that host.
	 * 
	 * @param server The radius server to connect to, in the format host[:port].
	 * @param secret The radius secret to use, may be null (see above).
	 */
	public void setServer(String server, byte[] secret);

}
