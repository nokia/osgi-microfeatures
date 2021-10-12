// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.diameter;

import java.util.List;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This interface encapsulates a Diameter Peer.
 * <p/>
 * There is a unique connection between the Local Diameter Peer and a Remote
 * Diameter Peer. This interface contains the information exchanged during the
 * capabilities exchange and some information about the connection.
 * 
 * <p/>
 * When calling connect() or disconnect(int) methods, the implementation should
 * implement the following state machine:
 * 
 * <p align=center>
 * <img src="ConnectionStateMachine.png"/>
 * </p>
 * 
 * <p/>
 * The dotted lines indicates the state changes with a listener method call,
 * these listener are called when the machine enters in a stable state and when
 * the state changes.
 * <p/>
 * Note that these methods can throw Exception (generally IllegalStateException)
 * in order to mean that the method has been called whereas the machine is not
 * in a stable state (CONNECTED or DISCONNECTED). <br>
 * They can also throw an UnsupportedOperationException exception if the
 * implemented peer does not support the operation.
 */
@ProviderType
public interface DiameterPeer {

	/**
	 * The Protocol.
	 */
	public enum Protocol {
		/**
		 * TCP
		 */
		TCP,
		/**
		 * SCTP
		 */
		SCTP;
	}

	/**
	 * The value that specifies if the connection with the Peer must be encrypted.
	 */
	public static final int ENC_LEVEL_REQUIRED = 1;

	/**
	 * The value that specifies if the connection with the Peer should be
	 * encrypted.
	 */
	public static final int ENC_LEVEL_PREFERRED = 2;

	/**
	 * The value that specifies if the connection with the Peer may be encrypted.
	 */
	public static final int ENC_LEVEL_OPTIONAL = 3;

	/**
	 * The value that specifies if the connection with the Peer must not be
	 * encrypted.
	 */
	public static final int ENC_LEVEL_FORBIDDEN = 4;

	/**
	 * The default Diameter port (3868).
	 */
	public static final int DIAMETER_PORT = 3868;

	/**
	 * Returns the peer's ID.
	 * This ID can be set in the peers' configuration (for static peers). Else it is assigned by the container for dynamic peers.
	 * @return the ID.
	 */
	public long getId();
	
	/**
	 * Indicates if this DiameterPeer Object encapsulates the local DiameterPeer.
	 * 
	 * @return true if this is the local DiameterPeer, false if it is a remote or
	 *         static DiameterPeer
	 */
	public boolean isLocalDiameterPeer();
	/**
	 * Indicates the name of this local peer.
	 * A local peer name is set by configuration : it has no diameter semantics.
	 * 
	 * @return the name of the local peer, or null if this is not a local peer.
	 */
	public String getLocalDiameterPeerName ();

	/**
	 * Gets all the supported applications.
	 * 
	 * @return A copy of the list of the supported applications.
	 */

	public List<DiameterApplication> getSupportedApplications();

	/**
	 * Sets the list of the supported applications and notifies all the connected
	 * clients and servers that the supported application list has changed.<BR>
	 * 
	 * This method can only be called on local peers.
	 * 
	 * @param applications The supported applications. If the application list is
	 *          null, it does nothing. This list is copied.
	 * @exception UnsupportedOperationException if this is not a local peer.
	 */
	public void setSupportedApplications(List<DiameterApplication> applications)
		throws UnsupportedOperationException;

	/**
	 * Indicates that the stack is acting as a relay.<BR>
	 * It empties the application list and notifies all the connected clients and
	 * servers that the supported application list has changed. <BR>
	 * This method can only be called on local peers.
	 * 
	 * @exception UnsupportedOperationException if this is not a local peer.
	 */
	public void setRelay()
		throws UnsupportedOperationException;

	/**
	 * Indicates whether the peer is acting as a relay.
	 * 
	 * @return true if the peer is acting as a relay.
	 */
	public boolean isRelay();

	/**
	 * Gets the peer Origin-Host.
	 * 
	 * @return The Origin-Host as specified in the capabilities exchange.
	 */
	public String getOriginHost();

	/**
	 * Gets the peer Origin-Realm.
	 * 
	 * @return The Origin-Realm as specified in the capabilities exchange.
	 */
	public String getOriginRealm();

	/**
	 * Gets the Peer IP addresses. <br/>
	 * Each IP address is represented as a byte[] which can be interpreted via
	 * com.nextenso.proxylet.diameter.util.AddressFormat.
	 * 
	 * @return The IP addresses as specified in the capabilities exchange message.
	 * @see com.nextenso.proxylet.diameter.util.AddressFormat IP address
	 *      formatting.
	 */
	public byte[][] getHostIPAddresses();

	/**
	 * Gets the peer vendor identifier.
	 * 
	 * @return The vendor-id as specified in the capabilities exchange message.
	 */
	public long getVendorId();

	/**
	 * Gets the product name.
	 * 
	 * @return The product name as specified in the capabilities exchange message.
	 */
	public String getProductName();

	/**
	 * Gets the Origin-State-Id.
	 * 
	 * @return the Origin-State-Id as specified in the capabilities exchange
	 *         message.
	 */
	public long getOriginStateId();

	/**
	 * Gets the peer supported vendor identifiers.
	 * 
	 * @return The supported vendor-ids as specified in the capabilities exchange
	 *         message.
	 */
	public long[] getSupportedVendorIds();

	/**
	 * Gets the supported authentication applications.
	 * 
	 * @return The supported Auth-Applications as specified in the capabilities
	 *         exchange message.
	 */
	public long[] getAuthApplications();

	/**
	 * Gets the supported accounting applications.
	 * 
	 * @return The supported accounting applications as specified in the
	 *         capabilities exchange message.
	 */
	public long[] getAcctApplications();

	/**
	 * Gets the peer supported vendor-specific application identifiers.
	 * 
	 * @return The supported vendor-specific applications as specified in the
	 *         capabilities exchange message.
	 * @deprecated use getSupportedApplications with
	 *             DiameterApplication.isVendorSpecific() method.
	 */
	@Deprecated
	public long[] getVendorSpecificApplications();

	/**
	 * Gets the peer supported vendor-specific applications.
	 * 
	 * @return The supported vendor-id applications as specified in the
	 *         capabilities exchange.
	 * @deprecated use getSupportedApplications with
	 *             DiameterApplication.isVendorSpecific() method.
	 */
	@Deprecated
	public List<DiameterApplication> getSpecificApplications();

	/**
	 * Gets the Inband-Security-Id.
	 * 
	 * @return The Inband-Security-Id as specified in the capabilities exchange
	 *         message.
	 */
	public long[] getInbandSecurityId();

	/**
	 * Gets the firmware revision.
	 * 
	 * @return The firmware revision as specified in the capabilities exchange
	 *         message.
	 */
	public long getFirmwareRevision();

	/**
	 * Gets the peer host (name or IP address).
	 * 
	 * @return The host.
	 */
	public String getHost();

	/**
	 * Gets the remote peer IP addresses : a single one for TCP (same as getHost), the primary plus the secondary(ies) for SCTP.
	 * For SCTP, this is the list specified during the handshake : it is static, it is not kept up to date via the various sctp events.
	 * 
	 * @return The IPs
	 */
	public List<String> getHosts();

	/**
	 * Gets the list of peer hosts (name or IP address) specified when the remote peer was declared.
	 * 
	 * @return The hosts.
	 */
	public List<String> getConfiguredHosts();

	/**
	 * Gets the Peer port.
	 * 
	 * @return The port.
	 */
	public int getPort();

	/**
	 * Indicates if the connection is encrypted.
	 * 
	 * @return true if encrypted, false otherwise.
	 */
	public boolean isEncrypted();

	/**
	 * Disconnects and destroys the peer.
	 * 
	 * @param disconnectCause The disconnect cause.
	 */
	public void disconnect(int disconnectCause);

	/**
	 * Connects the peer to the server.
	 */
	public void connect();
	
	/**
	 * Connects the peer to the server using a local port and some local ip addresses.
	 * 
	 * @param localPort the local port to be used when connecting the peer to the server
	 * @param localIPs the local ip addresses to be used when connecting the peer to the server (can't be null)
	 */
	public void connect(int localPort, String... localIPs);

	/**
	 * Indicates if the connection is open.
	 * 
	 * @return true if open, false otherwise.
	 */
	public boolean isConnected();

	/**
	 * Indicates whether the peer has been locally initiated. <BR>
	 * See RFC 3588 section 5.6.
	 * 
	 * @return true if the peer has been locally initiated.
	 */
	public boolean isLocalInitiator();

	/**
	 * Gets the local peer.
	 * 
	 * @return The local peer or null if unknown.
	 */
	public DiameterPeer getLocalDiameterPeer();

	/**
	 * Adds a listener to handle connection and disconnection
	 * 
	 * @param listener The listener to be added.
	 */
	public void addListener(DiameterPeerListener listener);

	/**
	 * Removes a listener.
	 * 
	 * @param listener The listener to be removed.
	 */
	public void removeListener(DiameterPeerListener listener);

	/**
	 * Sets the time-out value before sending again a request when no answer has
	 * been received.
	 * 
	 * @param seconds The value of the time-out in seconds.
	 */
	public void setRetryTimeout(Integer seconds);

	/**
	 * Sets the time-out value before sending again a request when no answer has
	 * been received.
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

	/**
	 * Sets the number of retries when trying to send a request when no answer has
	 * been received.
	 * 
	 * @param nb The number of retries.
	 */
	public void setNbRetries(Integer nb);

	/**
	 * Gets the number of retries when trying to send a request when no answer has
	 * been received.
	 * 
	 * @return The number of retries. It returns null if no specific value has
	 *         been set for this peer.
	 */
	public Integer getNbRetries();

	/**
	 * Gets the used protocol.
	 * 
	 * @return The protocol
	 */
	public Protocol getProtocol();

	/**
	 * Gets the quarantine delay.
	 * 
	 * @return The quarantine delay in milliseconds. It returns null if no
	 *         specific delay has been set.
	 */
	public Long getQuarantineDelay();

	/**
	 * Sets the quarantine delay.
	 * 
	 * @param delayInMs The quarantine delay in milliseconds or null to unset it.
	 */
	public void setQuarantineDelay(Long delayInMs);

	/**
	 * Indicates whether the peer is quarantined
	 * 
	 * @return true if the peer is quarantined, else false.
	 */
	public boolean isQuarantined();

	/**
	 * Quarantines the peer.
	 */
	public void quarantine();

	/**
	 * Provides the raw AVPs provided by the remote peer when the capabilities exchange took place.
	 * Returns an empty array for a local peer.
	 * They must not be modified by the application.
	 *
	 * @return the raw AVPs
	 */
	public DiameterAVP[] getCapabilitiesExchangeAVPs ();

	/**
	 * Sets SCTP socket options (applicable when SCTP is used).
	 * 
	 * @param options a Map&lt;SctpSocketOption, SctpSocketParam&gt;
	 */
	public void setSctpSocketOptions (java.util.Map options);

	/**
	 * Provides a Map where applications can store peer-specific attributes.
	 * <p/>
	 * The returned Map is thread-safe (typically a ConcurrentHashMap).
	 *
	 * @return a map
	 */
	public java.util.Map<Object, Object> getAttributes ();

	/**
	 * Sets peer parameters.
	 * See DiameterPeerTable for supported parameters.
	 * 
	 * @param params a Map of parameters
	 */
	public void setParameters (java.util.Map<String, String> params);
}
