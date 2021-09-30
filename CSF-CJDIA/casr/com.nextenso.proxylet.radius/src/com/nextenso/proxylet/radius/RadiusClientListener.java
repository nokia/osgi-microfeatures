package com.nextenso.proxylet.radius;

public interface RadiusClientListener {

	/**
	 * Called when the response to a request is available.
	 * 
	 * @param client The client.
	 * @param response The response. The radius response or -1 if any problem.
	 */
	public void handleResponse(RadiusClient client, int response);

}
