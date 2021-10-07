package com.alcatel_lucent.as.ims.diameter.charging.rf;

/**
 * The Interim Listener.
 */
public interface InterimListener {

	/**
	 * Called when an INTERIM must be sent.
	 * 
	 * @param client The client.
	 */
	void doInterim(RfSessionClient client);

}
