package com.nextenso.proxylet.diameter.client;

/**
 * The interface to implement to be notified of incoming requests.
 */
public interface DiameterRequestListener {

	/**
	 * Called when a request comes in.
	 * 
	 * @param request The request that arrived (some methods that apply to
	 *          outgoing requests, like execute(), are disabled).
	 * @param response The response to send after filling in the AVPs.
	 */
	public void handleRequest(DiameterClientRequest request, DiameterClientResponse response);

}
