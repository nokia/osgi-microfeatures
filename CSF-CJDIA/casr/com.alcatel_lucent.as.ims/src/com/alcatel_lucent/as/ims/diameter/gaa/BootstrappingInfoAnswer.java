package com.alcatel_lucent.as.ims.diameter.gaa;

import java.util.Date;

import com.alcatel_lucent.as.ims.diameter.ImsAnswer;
import com.alcatel_lucent.as.ims.diameter.gaa.GaaConstants.GbaType;

/**
 * The Bootstrapping-Info-Answer (BIA).
 */
public interface BootstrappingInfoAnswer
		extends ImsAnswer {

	/**
	 * Gets the User-Name AVP value..
	 * 
	 * @return The IMPI.
	 */
	public String getUserName();

	/**
	 * Gets the ME-Key-Material AVP value.
	 * 
	 * @return The Mobile Equipment key material.
	 */
	public byte[] getMEKeyMaterial();

	/**
	 * Gets the UICC-Key-Material AVP value.
	 * 
	 * @return The UICC key material.
	 */
	public byte[] getUICCKeyMaterial();

	/**
	 * Gets the Key-ExpiryTime AVP value.
	 * 
	 * @return The expiry time of the key.
	 */
	public Date getKeyExpiryTime();

	/**
	 * Gets the BootstrapInfoCreationInfo AVP value.
	 * 
	 * @return The bootstrapinfo creation time of the key.
	 */
	public Date getBootstrapInfoCreationTime();

	/**
	 * Gets the GBA-UserSecSettings AVP value.
	 * 
	 * @return The relevant USSs.
	 */
	public byte[] getGBAUserSecSettings();

	public GbaType getGbaType();
}
