package com.alcatel_lucent.as.ims.diameter.sh;

/**
 * User Data Answer (UDA)
 */
public interface UserDataAnswer
		extends ShAnswer {

	/**
	 * Gets the requested data.
	 * 
	 * @return The requested data.
	 */
	public byte[] getUserData();

}
