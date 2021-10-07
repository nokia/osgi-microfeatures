package com.nextenso.proxylet.diameter.client;

import java.io.IOException;

import com.nextenso.proxylet.diameter.DiameterResponse;

/**
 * The response Object used by the DiameterClient.
 */
public interface DiameterClientResponse
		extends DiameterResponse {

	/**
	 * Gets the associated request.
	 * 
	 * @return The request.
	 */
	public DiameterClientRequest getDiameterClientRequest();

	/**
	 * Gets the associated client.
	 * 
	 * @return The client.
	 */
	public DiameterClient getDiameterClient();

	/**
	 * Sends out the response. <br/>
	 * This method is only relevant when responding to an incoming request.
	 * 
	 * @throws IOException if the response could not be sent.
	 */
	public void send()
		throws IOException;

	/**
	 * Sets the result code stored in the Result-Code AVP. <br/>
	 * This method is only relevant when responding to an incoming request.
	 * 
	 * @param code The result code.
	 * @see com.nextenso.proxylet.diameter.util.DiameterBaseConstants values
	 *      defined in RFC 3588.
	 */
	public void setResultCode(long code);

	/**
	 * Sets the response error flag. <br/>
	 * This method is only relevant when responding to an incoming request.
	 * 
	 * @param flag The flag value (true or false).
	 */
	public void setErrorFlag(boolean flag);

}
