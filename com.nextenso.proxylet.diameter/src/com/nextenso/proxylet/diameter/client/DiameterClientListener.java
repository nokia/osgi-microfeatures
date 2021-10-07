package com.nextenso.proxylet.diameter.client;

/**
 * The interface to implement to execute asynchronous requests.
 */
public interface DiameterClientListener {

	/**
	 * Called when the response to a request is available.
	 * 
	 * @param request The request.
	 * @param response The response.
	 */
	public void handleResponse(DiameterClientRequest request, DiameterClientResponse response);

	/**
	 * Called when an Exception occurs while executing a request
	 * 
	 * @param request The request.
	 * @param ioe The exception.
	 */
	public void handleException(DiameterClientRequest request, java.io.IOException ioe);

}
