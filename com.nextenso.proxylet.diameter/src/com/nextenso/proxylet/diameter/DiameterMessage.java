// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.diameter;

import java.net.InetSocketAddress;
import java.util.Enumeration;
import org.osgi.annotation.versioning.ProviderType;
import com.nextenso.proxylet.ProxyletData;

/**
 * This interface encapsulates a Diameter message.
 * <p/>
 * It contains all the methods common to Diameter requests and Diameter
 * responses. <br/>
 * The accessible fields of the message are (See rfc3588 paragraph 3) :
 * <ul>
 * <li>a command code (read-only)
 * <li>an application identifier (read-only)
 * <li>a list of DiameterAVPs which can be modified (the list is ordered)
 * </ul>
 */
@ProviderType
public interface DiameterMessage
		extends ProxyletData {

	/**
	 * Gets the Diameter version.
	 * 
	 * @return The Diameter version - the current version is 1
	 */
	public int getVersion();

	/**
	 * Gets the message command code.
	 * 
	 * @return The command code.
	 */
	public int getDiameterCommand();

	/**
	 * Gets the message application-id.
	 * 
	 * @return The application identifier.
	 */
	public long getDiameterApplication();

	/**
	 * Gets the session this message belongs to. <br/>
	 * The associated session is <code>null</code> if the message contains no
	 * Session-Id AVP.
	 * 
	 * @return The associated session, possibly <code>null</code>.
	 */
	public DiameterSession getDiameterSession();

	/**
	 * Gets the AVP located at the specified index.
	 * 
	 * @param index The index in the AVP list.
	 * @return The AVP.
	 */
	public DiameterAVP getDiameterAVP(int index);

	/**
	 * Gets an AVP according to its code and its vendor identifier. <br/>
	 * The AVP list is iterated until an AVP with the specified code and vendor
	 * identifier is found.
	 * 
	 * @param code The AVP code.
	 * @param vendorId The AVP vendor identifier.
	 * @return The AVP or <code>null</code> if the AVP list contains no such AVP.
	 */
	public DiameterAVP getDiameterAVP(long code, long vendorId);

	/**
	 * Gets an AVP according to its definition. <br/>
	 * The AVP list is iterated until an AVP with the specified definition is
	 * found.
	 * 
	 * @param definition The AVP definition.
	 * @return The AVP or <code>null</code> if the AVP list contains no such AVP.
	 */
	public DiameterAVP getDiameterAVP(DiameterAVPDefinition definition);

	/**
	 * Adds an AVP to the end of the list.
	 * 
	 * @param avp The AVP to add.
	 */
	public void addDiameterAVP(DiameterAVP avp);

	/**
	 * Adds an AVP at the specified index in the list.
	 * 
	 * @param index The index.
	 * @param avp The AVP to add.
	 */
	public void addDiameterAVP(int index, DiameterAVP avp);

	/**
	 * Adds an AVP according to its definition. <br/>
	 * If an AVP with this definition is already contained in the list, it is
	 * returned. If there is no such AVP in the list, a new AVP is instantiated
	 * with this definition and added to the end of the list. <br/>
	 * This is a compact method to perform:
	 * <p>
	 * <code>
	 * DiameterAVP avp = message.getDiameterAVP (definition);
	 * <br/>if (avp == null) {
	 * <br/>    list.addDiameterAVP (avp = new DiameterAVP (definition));
	 * <br/>}
	 * </code>
	 * 
	 * @param definition The AVP definition.
	 * @return The AVP which was either retrieved or created and added.
	 */
	public DiameterAVP addDiameterAVP(DiameterAVPDefinition definition);

	/**
	 * Removes the AVP located at the specified index.
	 * 
	 * @param index The index.
	 * @return the removed AVP.
	 */
	public DiameterAVP removeDiameterAVP(int index);

	/**
	 * Removes an AVP according to its code and its vendor identifier. <br/>
	 * The AVP list is iterated until an AVP with the specified code and vendor
	 * identifier is found and removed.
	 * 
	 * @param code The AVP code.
	 * @param vendorId The AVP vendor identifier.
	 * @return The removed AVP or <code>null</code> if the AVP list contains no
	 *         such AVP.
	 */
	public DiameterAVP removeDiameterAVP(long code, long vendorId);

	/**
	 * Removes an AVP according to its definition. <br/>
	 * The AVP list is iterated until an AVP with the specified definition is
	 * found and removed.
	 * 
	 * @param definition The AVP definition.
	 * @return The removed AVP or <code>null</code> if the AVP list contains no
	 *         such AVP.
	 */
	public DiameterAVP removeDiameterAVP(DiameterAVPDefinition definition);

	/**
	 * Removes all the AVPS.
	 */
	public void removeDiameterAVPs();

	/**
	 * Gets all the AVPS.
	 * 
	 * @return An Enumeration of the AVPS.
	 */
	public Enumeration getDiameterAVPs();

	/**
	 * Gets the number of AVPs.
	 * 
	 * @return The number of AVPs.
	 */
	public int getDiameterAVPsSize();

	/**
	 * Gets the index of an AVP in the AVP list according to its code and vendor
	 * identifier.
	 * 
	 * @param code The AVP code.
	 * @param vendorId The AVP vendor identifier.
	 * @return The index, or -1 if no such AVP was found.
	 */
	public int getDiameterAVPIndex(long code, long vendorId);

	/**
	 * Gets the index of an AVP in the AVP list according to its definition.
	 * 
	 * @param definition The AVP definition.
	 * @return The index, or -1 if no such AVP was found.
	 */
	public int getDiameterAVPIndex(DiameterAVPDefinition definition);

	/**
	 * Gets the client peer.
	 * 
	 * @return The client Diameter peer.
	 */
	public DiameterPeer getClientPeer();

	/**
	 * Gets the server peer. <br/>
	 * It may be <code>null</code> for a request whose server DiameterPeer has not
	 * been resolved yet.
	 * 
	 * @return The server Diameter peer.
	 */
	public DiameterPeer getServerPeer();

	/**
	 * Gets the address where the message has been received.
	 * 
	 * @return The address or null if not known or if the message has not been
	 *         received.
	 */
	public InetSocketAddress getReceptionAddress();

	/**
	 * Indicates whether the message supports Route-Record AVP.
	 * 
	 * @return true (default value) or false if the AVP is supported or not. 
	 * @see #setSupportingRouteRecord(boolean)
	 */
	public boolean isSupportingRouteRecord();

	/**
	 * Sets the value to indicates whether the message supports Route-Record AVP.
	 * 
	 * @param support true or false if the AVP is supported or not.
	 */
	public void setSupportingRouteRecord(boolean support);

	/**
	 * Adds all the AVPs from another message to this message.
	 * If an AVP already exists in this message, then it is not added (regardless of the values).
	 * 
	 * @param fromMessage the other message to read the AVPs from
	 * @param clone true to clone the AVPs, false to point directly to the other's AVPs
	 */
	public void addDiameterAVPs (DiameterMessage fromMessage, boolean clone);
	
	/**
	 * Sets all the AVPs from another message to this message.
	 * Current AVPs are all removed first.
	 * 
	 * @param fromMessage the other message to read the AVPs from
	 * @param clone true to clone the AVPs, false to point directly to the other's AVPs
	 */
	public void setDiameterAVPs (DiameterMessage fromMessage, boolean clone);

	/**
	 * Removes all instances of an AVP according to its code and its vendor identifier. <br/>
	 * The AVP list is iterated until all AVPs with the specified code and vendor
	 * identifier are found and removed.
	 * 
	 * @param code The AVP code.
	 * @param vendorId The AVP vendor identifier.
	 * @return The number of removed AVPs.
	 */
	public int removeDiameterAVPs (long code, long vendorId);

	/**
	 * Sets the DiameterApplication object with the modified values.
	 * It should be called only after getting the current object using getApplication() method.
	 * update the values in the current object and call the setApplication()
	 *
	 * @param app DiameterApplication
	 */
	public void setApplication(DiameterApplication app);

	/**
	 * Gets the Diameter application object.
	 *
	 * @return DiameterApplication object.
	 */
	public DiameterApplication getApplication();

}
