package com.alcatel_lucent.as.ims.diameter.gaa;

import com.alcatel_lucent.as.ims.diameter.ImsClient;

/**
 * The Zh Client.
 */
public interface ZhClient
		extends ImsClient {

	/**
	 * Creates a new Multimedia-Auth request.
	 * 
	 * @return The new Multimedia-Auth request.
	 */
	public MultimediaAuthRequest createMAR();
}
