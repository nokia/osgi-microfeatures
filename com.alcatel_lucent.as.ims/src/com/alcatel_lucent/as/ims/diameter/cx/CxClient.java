package com.alcatel_lucent.as.ims.diameter.cx;

import com.alcatel_lucent.as.ims.diameter.ImsClient;
import com.alcatel_lucent.as.ims.diameter.cx.CxConstants.ServerAssignmentType;
import com.alcatel_lucent.as.ims.diameter.cx.CxConstants.UserDataAlreadyAvailable;
import com.alcatel_lucent.as.ims.diameter.cx.CxConstants.UserDataRequestType;

/**
 * The Cx Client.
 */
public interface CxClient
		extends ImsClient {

	/**
	 * Creates a new User-Authorization request.
	 * 
	 * @param userName The user name.
	 * @param publicIdentity The user public identity.
	 * @param visitedNetworkId The visited network identifier.
	 * 
	 * @return The new User-Authorization request.
	 */
	public UserAuthorizationRequest createUAR(String userName, String publicIdentity, byte[] visitedNetworkId);

	/**
	 * Creates a new Server-Assignment request.
	 * 
	 * @param serverName The Sip URL of the server.
	 * @param serverAssignmentType The type of server assignment.
	 * @param requestType The type of the request.
	 * @param userProfileAlreadyAvailable indicates to the HSS if the user profile
	 *          is already available in the S-CSCF.
	 * @return The new Server-Assignment request.
	 */
	public ServerAssignmentRequest createSAR(String serverName, ServerAssignmentType serverAssignmentType, UserDataRequestType requestType,
			UserDataAlreadyAvailable userProfileAlreadyAvailable);

	/**
	 * Creates a new Location-Info request.
	 * 
	 * @param publicIdentity The user public identity
	 * @return The new Location-Info request.
	 */
	public LocationInfoRequest createLIR(String publicIdentity);

	/**
	 * Creates a new Multimedia-Auth request.
	 * 
	 * @param privateIdentity The user private identity
	 * @param userName The user public identity
	 * @param serverName The SIP URL of the server.
	 * @return The new Multimedia-Auth request.
	 */
	public MultimediaAuthRequest createMAR(String privateIdentity, String userName, String serverName);
	//
	//	/**
	//	 * Creates a new Registration-Termination request.
	//	 * 
	//	 * @param userName The user name.
	//	 * @param deRegistrationReason The deregistration reason.
	//	 * @return The new Registration-Termination request.
	//	 */
	//	public RegistrationTerminationRequest createRTR(String userName, DeregistrationReasonAVP deRegistrationReasonAVP);
	//
	//	/**
	//	 * Creates a new Push-Profile request.
	//	 * 
	//	 * @param userName the user name.
	//	 * @return The new Push-Profile request.
	//	 */
	//	public PushProfileRequest createPPR(String userName);

}
