package com.nextenso.proxylet.diameter;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This interface encapsulates a Diameter request.
 * <p/>
 * It extends DiameterMessage and contains some methods specific to requests.
 */
@ProviderType
public interface DiameterRequest
		extends DiameterMessage {

	/**
	 * Indicates if the P flag is set (See RFC 3588 paragraph 3).
	 * 
	 * @return true if the P flag is set, false otherwise.
	 */
	public boolean hasProxyFlag();

	/**
	 * Indicates if the T flag is set (See RFC 3588 paragraph 3).
	 * 
	 * @return true if the T flag is set, false otherwise.
	 */
	public boolean hasRetransmissionFlag();

	/**
	 * Sets the P flag.
	 * 
	 * @param flag true to set the flag, false to remove it.
	 */
	public void setProxyFlag(boolean flag);

	/**
	 * Sets the T flag.
	 * 
	 * @param flag true to set the flag, false to remove it.
	 */
	public void setRetransmissionFlag(boolean flag);

	/**
	 * Returns the end to end identifier.
	 *
	 *@return the end to end identifier
	 */
	public int getEndToEndIdentifier ();

	/**
	 * Returns the original hop by hop identifier of the request when it came in.
	 * Indeed the hop by hop identifier is modified when the request is sent out (when proxied).
	 *
	 *@return the hop by hop identifier
	 */
	public int getIncomingHopByHopIdentifier ();

	/**
	 * Returns the hop by hop identifier of the request when it is sent out (when proxied).
	 * Indeed the hop by hop identifier is modified when the request is sent out (when proxied).
	 *
	 *@return the hop by hop identifier
	 */
	public int getOutgoingHopByHopIdentifier ();

	/**
	 * Gets the associated response.
	 * 
	 * @return the associated response.
	 */
	public DiameterResponse getResponse();
    
	/**
	 * Sets the time-out value before sending again a request when no answer has
	 * been received. If set, this value overrides the one set for the remote peer.
	 * 
	 * @param seconds The value of the time-out in seconds.
	 */
	public void setRetryTimeout(Integer seconds);

	/**
	 * Sets the time-out value before sending again a request when no answer has
	 * been received. If set, this value overrides the one set for the remote peer.
	 * 
	 * @param milliseconds The value of the time-out in milliseconds.
	 */
	public void setRetryTimeoutInMs(Integer milliseconds);

	/**
	 * Gets the time-out value before sending again a request when no answer has
	 * been received.
	 * 
	 * @return The time-out value in seconds. It returns null if no specific value
	 *         has been set for this peer.
	 */
	public Integer getRetryTimeout();

	/**
	 * Gets the time-out value before sending again a request when no answer has
	 * been received.
	 * 
	 * @return The time-out value in milliseconds. It returns null if no specific
	 *         value has been set for this peer.
	 */
	public Integer getRetryTimeoutInMs();


}
