package com.alcatel_lucent.as.ims.diameter.sh;

import java.io.IOException;

import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.ImsAnswerListener;
import com.alcatel_lucent.as.ims.diameter.cx.CxConstants.SessionPriority;
import com.alcatel_lucent.as.ims.diameter.sh.ShConstants.CurrentLocation;
import com.alcatel_lucent.as.ims.diameter.sh.ShConstants.DataReference;
import com.alcatel_lucent.as.ims.diameter.sh.ShConstants.IdentitySet;
import com.alcatel_lucent.as.ims.diameter.sh.ShConstants.RequestedDomain;

/**
 * The User Data Request (UDR)
 */
public interface UserDataRequest
		extends ShRequest {

	/**
	 * Adds the data reference (mapped on the Data-Reference diameter AVP).
	 * 
	 * @param dataReference The data reference.
	 */
	public void addDataReference(DataReference dataReference);

	/**
	 * Adds the DSAI tag (mapped on the DSAI-TAg diameter AVP).
	 * 
	 * @param tag The tag.
	 */
	public void addDSAITag(byte[] tag);

	/**
	 * Adds the type of identity set that the HSS shall provide (mapped to the
	 * Identity-Set diameter AVP).
	 * 
	 * @param type The set value.
	 */
	public void addIdentitySet(IdentitySet type);

	/**
	 * Sets the access domain for which certain data are requested (mapped to the
	 * Requested-Domain diameter AVP).
	 * 
	 * @param domain The access domain for which data are requested.
	 */
	public void setRequestedDomain(RequestedDomain domain);

	/**
	 * Indicates whether an active location retrieval has to be initiated or not
	 * (mapped to the Current-Location diameter AVP).
	 * 
	 * @param location The value.
	 */
	public void setCurrentLocation(CurrentLocation location);

	/**
	 * Adds the Service Indication that identifies a service in an AS (mapped to
	 * the Service-Indication diameter AVP).
	 * 
	 * @param serviceIndication the service indication to be added.
	 */
	public void addServiceIndication(byte[] serviceIndication);

	/**
	 * Sets the AS server SIP URL.
	 * 
	 * Used with the User Identity and Data Reference as key to identity the
	 * filter criteria.
	 * 
	 * @param serverName The SIP URL of the server name.
	 */
	public void setServerName(String serverName);

	/**
	 * Sets the session priority.
	 * 
	 * Used to indicate to the HSS the session's priority.
	 * 
	 * @param priority The session priority.
	 */
	public void setSessionPriority(SessionPriority priority);

	/**
	 * Sends the request.
	 * 
	 * @return the UDA received from the HSS to the AS
	 * @exception DiameterMissingAVPException if a mandatory or conditional AVP is
	 *              missing.
	 * @exception IOException if an error occurs when the message is sent.
	 */
	public UserDataAnswer execute()
		throws DiameterMissingAVPException, IOException;

	/**
	 * Sends asynchronously the request to the HSS.
	 * 
	 * @param listener The listener to be called when a response is available.
	 * @exception DiameterMissingAVPException if a mandatory or conditional AVP is
	 *              missing
	 */
	public void execute(ImsAnswerListener<UserDataRequest, UserDataAnswer> listener)
		throws DiameterMissingAVPException;
}
