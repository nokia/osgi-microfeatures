package com.nextenso.proxylet.diameter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.nextenso.proxylet.diameter.DiameterPeer.Protocol;

/**
 * This class stores all the DiameterPeers known to the System.
 * <p/>
 * A DiameterPeer may be specified in the System configuration or may be added
 * dynamically.
 */
public abstract class DiameterPeerTable {

	protected DiameterPeerTable() {}
	private final static List<CapabilitiesListener> _capabilitiesListeners = new CopyOnWriteArrayList<CapabilitiesListener>();
	private volatile static DiameterPeerTable _instance;
	
	/**
	 * Gets the list of listeners to modify capabilities messages before sending
	 * them.
	 * 
	 * @return The list of listeners.
	 */
	 public final List<CapabilitiesListener> getCapabilitiesListeners() {
		return _capabilitiesListeners;
	}

	/**
	 * Gets the unique DiameterPeerTable instance.
	 * 
	 * @return The DiameterPeerTable instance.
	 */
	public static DiameterPeerTable getDiameterPeerTable() {
		DiameterPeerTable instance = _instance;
		if (instance == null) {
			throw new RuntimeException("Diameter peer table not yet initialized.");
		}
		return instance;
	}
	
	/**
	 * Sets the unique DiameterPeerTable instance. This method is only meant to be called from the diameter agent implementation.
	 * @param instance The DiameterPeerTable instance
	 */
	public static void setDiameterPeerTable(DiameterPeerTable instance) {
		_instance = instance;
	}

	/**
	 * Gets a peer according to its Origin-Host.
	 * 
	 * @param originHost The Origin-Host.
	 * @return The peer, or <code>null</code> if no peer was found matching the
	 *         specified Origin-Host
	 * @deprecated use getDiameterPeer(DiameterPeer,String).
	 * 
	 */
	@Deprecated
	public abstract DiameterPeer getDiameterPeer(String originHost);

	/**
	 * Gets a peer according to its Origin-Host.
	 * 
	 * @param localPeer The local Peer.
	 * @param originHost the Origin-Host.
	 * @return the peer, or <code>null</code> if no peer was found matching the
	 *         specified Origin-Host.
	 * @since ASR 4.0
	 */
	public abstract DiameterPeer getDiameterPeer(DiameterPeer localPeer, String originHost);

	/**
	 * Gets peers according to their Origin-Host.
	 * This is useful when many peers with the same Origin-Host are connected.
	 * 
	 * @param localPeer The local Peer.
	 * @param originHost the Origin-Host.
	 * @return the list of peers (may be empty)
	 */
	public abstract List<DiameterPeer> getDiameterPeers(DiameterPeer localPeer, String peerOriginHost);

	/**
	 * Gets the peer that will be used for routing, according to the destination
	 * realm and the diameter application.
	 * 
	 * @param destRealm The destination realm.
	 * @param application The diameter application.
	 * @return The peer, or <code>null</code> if no peer could be determined.
	 * @deprecated use DiameterRouteTable object.
	 */
	@Deprecated
	public abstract DiameterPeer getDiameterPeer(String destRealm, long application);

	/**
	 * Gets the peer that will be used for routing, according to the destination
	 * realm and the diameter application.
	 * 
	 * @param localPeer The local Peer.
	 * @param destRealm The destination realm.
	 * @param application The diameter application.
	 * @return The peer, or <code>null</code> if no peer could be determined.
	 * @deprecated use DiameterRouteTable object.
	 */
	@Deprecated
	public abstract DiameterPeer getDiameterPeer(DiameterPeer localPeer, String destRealm, long application);

	/**
	 * Gets the peer object wrapping the local peer. <br/>
	 * The local peer wraps the local Diameter configuration which is broadcast to
	 * other Diameter peers during capabilities exchange.
	 * 
	 * @return The unique local peer
	 * @deprecated use getLocalDiameterPeers().
	 */
	@Deprecated
	public abstract DiameterPeer getLocalDiameterPeer();
    
	/**
	 * Gets the peer object wrapping the local peer requested. <br/>
	 * 
	 * @param localPeerName the name of the local peer to look up
	 * @return The requested local peer (or the default one if the param is null)
	 */
	public abstract DiameterPeer getLocalDiameterPeer(String localPeerName);

	/**
	 * Gets the peers wrapping the Local peer. <br/>
	 * The Local peer wraps the local Diameter configuration which is broadcast to
	 * other Diameter Peers during capabilities exchange.
	 * 
	 * @return The local diameter peers
	 */
	public abstract List<DiameterPeer> getLocalDiameterPeers();

	/**
	 * Creates a new static peer with default protocol (TCP).
	 * 
	 * @param localPeer The local peer.
	 * @param remoteOriginHost The remote origin host.
	 * @param host The remote host IP address.
	 * @param port The remote host port.
	 * @param secure true if secure.
	 * @return The new peer.
	 * @deprecated use
	 *             {@link DiameterPeerTable#newDiameterPeer(Map)}
	 */
	@Deprecated
	public DiameterPeer newDiameterPeer(DiameterPeer localPeer, String remoteOriginHost, String host, int port, boolean secure){
		return newDiameterPeer (localPeer, remoteOriginHost, host, port, secure, null);
	}

	/**
	 * Removes a peer from the table.
	 * 
	 * It closes the peer, removes it from the table and removes the associated
	 * routes.
	 * 
	 * @param peer The peer to be remove from the table.
	 * @exception IllegalArgumentException if the peer is not a static or a remote
	 *              peer.
	 */
	public abstract void removePeer(DiameterPeer peer)
		throws IllegalArgumentException;

	/**
	 * Creates a new static peer.
	 * 
	 * @param localPeer The local peer.
	 * @param remoteOriginHost The remote origin host.
	 * @param host The remote host IP address.
	 * @param port The remote host port.
	 * @param secure true if secure.
	 * @param protocol The protocol to use.
	 * @return The new peer.
	 * @deprecated use
	 *             {@link DiameterPeerTable#newDiameterPeer(Map)}
	 */
	public DiameterPeer newDiameterPeer(DiameterPeer localPeer, String remoteOriginHost, String host, int port, boolean secure, Protocol protocol){
		Map<String, Object> map = new HashMap<> ();
		map.put (PROP_LOCAL_PEER, localPeer);
		map.put (PROP_REMOTE_ORIGIN_HOST, remoteOriginHost);
		map.put (PROP_REMOTE_HOST, host);
		map.put (PROP_REMOTE_PORT, port);
		map.put (PROP_CONNECTION_SECURE, secure);
		map.put (PROP_CONNECTION_PROTOCOL, protocol);
		return newDiameterPeer (map);
	}

	/**
	 * Creates a new static peer.
	 * In this method, a list of remote IPs is provided that will be tested sequentially until one is accepting the connection request.
	 * 
	 * @param localPeer The local peer.
	 * @param remoteOriginHost The remote origin host.
	 * @param hosts The list of remote host IP addresses.
	 * @param port The remote host port.
	 * @param secure true if secure.
	 * @param protocol The protocol to use.
	 * @return The new peer.
	 * @deprecated use
	 *             {@link DiameterPeerTable#newDiameterPeer(Map)}
	 */
	public DiameterPeer newDiameterPeer(DiameterPeer localPeer, String remoteOriginHost, List<String> hosts, int port, boolean secure, Protocol protocol){
		Map<String, Object> map = new HashMap<> ();
		map.put (PROP_LOCAL_PEER, localPeer);
		map.put (PROP_REMOTE_ORIGIN_HOST, remoteOriginHost);
		map.put (PROP_REMOTE_HOST, hosts);
		map.put (PROP_REMOTE_PORT, port);
		map.put (PROP_CONNECTION_SECURE, secure);
		map.put (PROP_CONNECTION_PROTOCOL, protocol);
		return newDiameterPeer (map);
	}

	/**
	 * Gets the list of the peers.
	 * 
	 * @param localPeer The local peer.
	 * @return The list of the peers for this local peer or null if not found.
	 */
	public abstract List<DiameterPeer> getDiameterPeers(DiameterPeer localPeer);

	/**
	 * Adds a listener for peers.
	 * 
	 * @param listener The listener to be added.
	 */
	public abstract void addListener(DiameterPeerListener listener);

	/**
	 * Removes a listener.
	 * 
	 * @param listener The listener to be removed.
	 */
	public abstract void removeListener(DiameterPeerListener listener);

	/**
	 * The mandatory property name indicating the local peer.
	 * The property value must be a DiameterPeer Object.
	 */
	public static final String PROP_LOCAL_PEER = "local.peer";
	/**
	 * The mandatory property name indicating the remote peer origin Host.
	 * The property value must be a String.
	 */
	public static final String PROP_REMOTE_ORIGIN_HOST = "remote.origin.host";
	/**
	 * The mandatory property name indicating the remote peer host(s).
	 * The property value must be a String or a list of Strings.
	 */
	public static final String PROP_REMOTE_HOST = "remote.host";
	/**
	 * The mandatory property name indicating the remote peer port.
	 * The property value must be an Integer.
	 */
	public static final String PROP_REMOTE_PORT = "remote.port";
	/**
	 * The optional property name indicating if the connection is secure.
	 * The property value must be a Boolean. It is False by default.
	 */
	public static final String PROP_CONNECTION_SECURE = "connection.secure";
	/**
	 * The mandatory property name indicating the protocol.
	 * The property value must be a DiameterPeer.Protocol Object.
	 */
	public static final String PROP_CONNECTION_PROTOCOL = "connection.protocol";
	/**
	 * The property name indicating the delay to perform the L4 connection (in milliseconds).
	 * This property must be used via DiameterPeer.setParameters()
	 */
	public static final String PROP_CONNECTION_TIMEOUT = "connection.timeout";
	/**
	 * The property name indicating the inactivity delay to send a DWR (in milliseconds).
	 * This property must be used via DiameterPeer.setParameters()
	 */
	public static final String PROP_DWR_DELAY = "dwr.delay";
	/**
	 * The property name indicating the delay to receive the CEA (in milliseconds).
	 * This property must be used via DiameterPeer.setParameters()
	 */
	public static final String PROP_CEA_DELAY = "cea.delay";
	/**
	 * The property name indicating the DPA delay (in milliseconds).
	 * This property must be used via DiameterPeer.setParameters()
	 */
	public static final String PROP_DPA_DELAY = "dpa.delay";
	/**
	 * The property name indicating the tls protocols to use (comma separated if many).
	 * This property must be used via DiameterPeer.setParameters() prior to connecting
	 */
	public static final String PROP_SECURE_PROTOCOL = "secure.protocol";
	/**
	 * The property name indicating the tls ciphers to use (comma separated if many).
	 * This property must be used via DiameterPeer.setParameters() prior to connecting
	 */
	public static final String PROP_SECURE_CIPHER = "secure.cipher";
	/**
	 * The property name indicating the keystore to use.
	 * This property must be used via DiameterPeer.setParameters() prior to connecting
	 */
	public static final String PROP_SECURE_KEYSTORE_FILE = "secure.keystore.file";
	/**
	 * The property name indicating the keystore password to use.
	 * This property must be used via DiameterPeer.setParameters() prior to connecting
	 */
	public static final String PROP_SECURE_KEYSTORE_PASSWORD = "secure.keystore.pwd";
	/**
	 * The property name indicating the keystore type.
	 * This property must be used via DiameterPeer.setParameters() prior to connecting
	 */
	public static final String PROP_SECURE_KEYSTORE_TYPE = "secure.keystore.type";
	/**
	 * The property name indicating if security must be enabled only when a TLS 
	 * Inband-security-ID AVP is detected.
	 * This property must be used via DiameterPeer.setParameters() prior to connecting
	 */
	public static final String PROP_SECURE_DELAYED = "secure.delayed";
	
	/**
	 * The optional property name indicating the new peer Id.
	 * The property value must be an Integer. The contract is the following:
	 * <lr>
	 * <li>not provided : a first lookup is performed (an existing peer can be returned)
	 * <li>set to -1 : a new connection will be established, but the peer Id will be set by the container and no sharing with other jdiameters is possible.
	 * <li>else : a new connection will be established and the peer will be assigned this id (but cast to a Long via & 0xFFFFFFFFL : so avoid negative values). But sharing with other jdiameters is possible if values are common.
	 * </lr>
	 * Note : a peer Id is a Long. Restricting this property to a positive Integer is a way for the container to generate ids that do not collide.
	 */
	public static final String PROP_PEER_ID = "peer.id";
	
	public abstract DiameterPeer newDiameterPeer(Map<String, Object> props);

    

}
