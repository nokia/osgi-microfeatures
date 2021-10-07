package com.alcatel_lucent.as.ims.diameter.sh;

import java.util.List;

import com.alcatel_lucent.as.ims.diameter.SupportedFeatures;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;

/**
 * The push notification request (PNR).
 */
public interface PushNotificationRequest {

	/**
	 * Gets the list of supported features
	 * 
	 * @return The list of the supported features.
	 */
	public List<SupportedFeatures> getSupportedFeatures();

	/**
	 * Gets the Wilcarded PSI.
	 * 
	 * @return The Wildcarded PSI of null if not found.
	 */
	public String getWildcardedPSI();

	/**
	 * Gets the Wildcarded public user identity.
	 * 
	 * @return The Wildcarded IMPU of null if not found.
	 */
	public String getWildcardedIMPU();

	/**
	 * Gets the requested data.
	 * 
	 * @return The requested data.
	 */
	public byte[] getUserData();

	/**
	 * Gets the user identity.
	 * 
	 * @return The user identity.
	 */
	public UserIdentity getUserIdentity();

	/**
	 * Gets an AVP according to its definition.
	 * 
	 * @param definition The definition of the AVP to be retrieved.
	 * @return The AVP or null if not found.
	 */
	public DiameterAVP getAvp(DiameterAVPDefinition definition);

	/**
	 * Gets the associated answer to be filled and then sent.
	 * 
	 * @return The answer.
	 */
	public PushNotificationAnswer getAnswer();
}
