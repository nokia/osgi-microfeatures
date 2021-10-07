package com.nextenso.proxylet.radius.auth;

import com.nextenso.proxylet.radius.RadiusMessage;

/**
 * This interface encapsulates an Access Request.
 */
public interface AccessRequest
		extends RadiusMessage {

	/**
	 * The Access-Request message code (1).
	 * 
	 * @deprecated
	 * @use AuthUtils.CODE_ACCESS_REQUEST
	 */
	@Deprecated
	public static final int ACCESS_REQUEST_CODE = AuthUtils.CODE_ACCESS_REQUEST;

	/**
	 * Gets the associated response.
	 * 
	 * @return The response.
	 */
	public AccessResponse getResponse();

	/**
	 * Sets the Radius Server which the Request will be sent to. <br/>
	 * This method overrides the default routing policy configured in the Agent. <br/>
	 * The server parameter should be in the format "host:port" or "host". If the
	 * port is not specified, the default radius ports is assumed (1812). <br/>
	 * The secret parameter may be null if the Agent configuration specifies the
	 * secret for that host.
	 * 
	 * @param server The radius server to connect to, in the format host[:port].
	 * @param secret The radius secret to use, may be null (see above).
	 */
	public void setServer(String server, byte[] secret);

	/**
	 * Sets the User-Password. <br/>
	 * The User-Password is not handled like the regular Radius Attributes.
	 * 
	 * @param password The password.
	 */
	public void setPassword(byte[] password);

	/**
	 * Gets the User-Password.
	 * 
	 * @return The password.
	 */
	public byte[] getPassword();

}
