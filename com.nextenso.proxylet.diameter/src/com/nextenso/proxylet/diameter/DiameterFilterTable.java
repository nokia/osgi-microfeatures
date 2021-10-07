package com.nextenso.proxylet.diameter;

import java.util.Collection;

/**
 * The Diameter Filter Table.<BR>
 * 
 * This table manages some lists :
 * <UL>
 * <LI>an incoming socket filter list to accept the connections.
 * <LI>an incoming socket filter list to refuse the connections.
 * </UL>
 * <BR>
 * These lists are synchronized for all the access and management methods but
 * need to be synchronized if they are used with iteration operations.
 * <p/>
 * Initially, the lists were named whitelist/blacklist.<br/>
 * In order to enable the API users to adopt an inclusive terminology without breaking the API, allowlist/denylist were added (pointing to the same lists underneath).
 * 
 */
public interface DiameterFilterTable {

	/**
	 * Gets the list of all the filter to refuse incoming socket connection.
	 * 
	 * @return The list of all the filter to refuse incoming socket connection.
	 *         The list
	 */
	public Collection<DiameterConnectionFilter> getIncomingSocketBlackList();

	/**
	 * Gets the list of all the filter to accept incoming socket connection.
	 * 
	 * @return The list of all the filter to accept incoming socket connection.
	 */
	public Collection<DiameterConnectionFilter> getIncomingSocketWhiteList();

	/**
	 * This method is identical to getIncomingSocketBlackList() but was added to avoid the blacklist wording.
	 * 
	 * @return The list of all the filter to refuse incoming socket connection.
	 *         The list
	 */
	public default Collection<DiameterConnectionFilter> getIncomingSocketDenyList(){ return getIncomingSocketBlackList ();}

	/**
	 * This method is identical to getIncomingSocketWhiteList() but was added to avoid the whitelist wording.
	 * 
	 * @return The list of all the filter to accept incoming socket connection.
	 */
	public default Collection<DiameterConnectionFilter> getIncomingSocketAllowList(){ return getIncomingSocketWhiteList ();}
    
	/**
	 * Applies the lists.
	 * 
	 * It closes the opened connections if they do not satisfy the conditions
	 * (black and white lists) any more.
	 */
	public void applyLists();

}
