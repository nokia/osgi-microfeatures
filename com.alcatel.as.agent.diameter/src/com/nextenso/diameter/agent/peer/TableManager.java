// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.peer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Dictionary;

import org.apache.log4j.Logger;
import com.alcatel.as.util.config.ConfigHelper;
import com.alcatel.as.service.concurrent.PlatformExecutors;

import com.nextenso.diameter.agent.PropertiesDeclaration;
import com.nextenso.diameter.agent.Utils;
import com.nextenso.diameter.agent.peer.xml.PeersParser;
import com.nextenso.diameter.agent.DiameterProperties;
import com.nextenso.proxylet.diameter.DiameterConnectionFilter;
import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterPeer.Protocol;
import com.nextenso.proxylet.diameter.DiameterPeerListener;
import com.nextenso.proxylet.diameter.DiameterPeerTable;
import com.nextenso.proxylet.diameter.DiameterRoute;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;

import alcatel.tess.hometop.gateways.utils.ConfigException;

import org.osgi.framework.BundleContext;
import org.apache.felix.dm.annotation.api.*;

@Component(provides={}, factoryMethod="getInstance")
public class TableManager extends DiameterPeerTable {
		
	private static final Logger LOGGER = Logger.getLogger("agent.diameter.peertablemanager");

	@Inject
	private BundleContext _bctx;

	private final Map<String, PeerTable> PEERS_TABLES = Utils.newConcurrentHashMap();
	private final Map<String, LocalPeer> LOCAL_PEERS = Utils.newConcurrentHashMap();
	private final List<ListenerWithExecutor> _listeners = new CopyOnWriteArrayList<ListenerWithExecutor>();

	/**
	 * Diameter Agent configuration.
	 */
	private Dictionary<String, String> _agentConfig;
	
	/**
	 * Our single instance.
	 */
	private final static TableManager _singleton = new TableManager();
	
	/**
	 * Flag used to make sure we don't register our service twice.
	 */
	private final static AtomicBoolean _registered = new AtomicBoolean(false);
	
	public static TableManager getInstance() {
		return _singleton;
	}
	
	@ServiceDependency
	protected void bindPlatformExecutors(PlatformExecutors pfExecutors) {
		Utils.setPlatformExecutors(pfExecutors); // why ?
	}

	@ConfigurationDependency(pid="diameteragent")
	public void setAgentConfig(Dictionary<String, String> cnf) {
		_agentConfig = cnf;
	}
	
	@Start
	void start() {
		try {
			if (ConfigHelper.getBoolean (_agentConfig, PropertiesDeclaration.PEER_TABLE_IMMEDIATE, false)){
				registerService(_bctx);
			}
		} catch (Exception e) {
			LOGGER.error("Can't register DiameterPeerTable service", e);
		}		
	}
		
	/**
	 * Register the Table Manager in the OSGI registry and also set the PeerTable implementation.
	 */
	public void registerService(BundleContext bctx) {
		if (_registered.compareAndSet(false, true)) {			
			LOGGER.debug("registering diameter peer table");
			DiameterPeerTable.setDiameterPeerTable(getInstance());
			bctx.registerService(DiameterPeerTable.class.getName(), getInstance(), null);
		}
	}
	
	/**
	 * Called when a peer is connected.
	 * 
	 * @param peer The connected peer.
	 */
	public void connected(DiameterPeer peer) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("connected... " + this + ", nbListener=" + _listeners.size());
		}

		for (final DiameterPeerListener listener : _listeners) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("connected: call the listener=" + listener);
			}
			listener.connected(peer);
		}
	}

	/**
	 * Called when a peer is disconnected.
	 * 
	 * @param peer The disconnected peer.
	 * @param disconnectReason The reason.
	 */
	public void disconnected(final DiameterPeer peer, final int disconnectReason) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("disconnected...");
		}
		for (final DiameterPeerListener listener : _listeners) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("disconnected: call the listener=" + listener);
			}
			listener.disconnected(peer, disconnectReason);
		}

	}

	public void connectionFailed(final DiameterPeer peer, final String message) {
		for (final DiameterPeerListener listener : _listeners) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("connectionFailed: call the listener=" + listener);
			}
			listener.connectionFailed(peer, message);
		}
	}
	
	public void sctpAddressChanged (final DiameterPeer peer, String addr, int port, DiameterPeerListener.SctpAddressEvent event){
		for (final DiameterPeerListener listener : _listeners) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("sctpAddressChanged: call the listener=" + listener);
			}
			listener.sctpAddressChanged(peer, addr, port, event);
		}
	}

	/**
	 * Initializes an handler connection.
	 * 
	 * @param handlerName The handler connection.
	 * @throws ConfigException
	 * @throws IllegalArgumentException
	 */
	public void muxOpened(String handlerName)
		throws ConfigException, IllegalArgumentException {
		if (handlerName == null) {
			return;
		}

		// The  local peer and the tables do not change and they are 
		// created only if they did not exist
		if (PEERS_TABLES.get(handlerName) == null) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("muxOpened: unknown handlerName -> create a local peer and peer table.");
			}

			String originHost = Utils.getClientOriginHost(handlerName);
			LocalPeer localPeer = new LocalPeer(originHost, handlerName, null);
			
			PeerTable table = new PeerTable();
			table.init(localPeer);
			// TODO use a copy of  this local peer
			table.addLocalPeer(localPeer);
			// TODO add a copy of  this local peer to all tables (for all handlers)
			// TODO add a copy of all the other local peers

			// insert in a thread safe manner
			PEERS_TABLES.put(handlerName, table);
			LOCAL_PEERS.put(handlerName, localPeer);
			Utils.getRouteTableManager().muxOpened(handlerName);
		} else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("muxOpened: use the already created tables for peers and routes");
			}
		}

	}
	// do not do it in muxOpened : CSFAR-1905 : the localPeer was notified too soon
	public void connected (LocalPeer localPeer){
		// indicate that the local peer is connected
		for (DiameterPeerListener listener : _listeners) {
			listener.connected(localPeer);
		}
	}

	public void muxClosed(String handlerName) {
		if (handlerName == null) {
			return;
		}

		// TODO remove this local peer from all tables (for all handlers)
		destroy(handlerName);
	}

	public void destroy(String handlerName) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("destroy: handler=" + handlerName);
		}
		DiameterPeer peer = LOCAL_PEERS.remove(handlerName);
		if (peer != null) {
			for (final DiameterPeerListener listener : _listeners) {
				listener.disconnected(peer, 0);
			}
		}

		PeerTable table = PEERS_TABLES.remove(handlerName);
		if (table != null) {
			table.closePeers();
		}
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeerTable#getDiameterPeer(java.lang.String)
	 */
	@Override
	@Deprecated
	public DiameterPeer getDiameterPeer(String originHost) {
		return getDiameterPeer(null, originHost);
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeerTable#getDiameterPeer(com.nextenso.proxylet.diameter.DiameterPeer,
	 *      java.lang.String)
	 */
	@Override
	public DiameterPeer getDiameterPeer(DiameterPeer localPeer, String originHost) {
		DiameterPeer res = null;
		if (localPeer == null) {
			// look in all tables for the first found peer
			for (String name : PEERS_TABLES.keySet()) {
				PeerTable table = PEERS_TABLES.get(name);
				res = table.getDiameterPeer(originHost);
				if (res != null) {
					return res;
				}
			}
		} else {
			String handlerName = ((Peer) localPeer).getHandlerName();
			PeerTable table = PEERS_TABLES.get(handlerName);
			if (table != null) {
				res = table.getDiameterPeer(originHost);
			}
		}

		return res;
	}
	public List<DiameterPeer> getDiameterPeers(DiameterPeer localPeer, String originHost) {
		// we expect local Peer not null
		if (localPeer == null) throw new NullPointerException ("Local Peer must not be null");
		String handlerName = ((Peer) localPeer).getHandlerName();
		PeerTable table = PEERS_TABLES.get(handlerName);
		if (table == null) return new ArrayList<DiameterPeer> (); // not expected though
		return table.getPeersByOriginHost(originHost);
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeerTable#getLocalDiameterPeer()
	 */
	@Override
	@Deprecated
	public DiameterPeer getLocalDiameterPeer() {
		List<DiameterPeer> peers = getLocalDiameterPeers();
		DiameterPeer res = null;
		if (peers != null && !peers.isEmpty()) {
			res = peers.get(0);
		}
		return res;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeerTable#getLocalDiameterPeers()
	 */
	@Override
	public List<DiameterPeer> getLocalDiameterPeers() {
		List<DiameterPeer> res = new ArrayList<DiameterPeer>();
		res.addAll(LOCAL_PEERS.values());
		return res;
	}

	public DiameterPeer getLocalDiameterPeer(String handlerName) {
	    if (handlerName == null) return getLocalDiameterPeer (); // for compliancy in API
		DiameterPeer res = null;
		if (handlerName != null) {
			res = LOCAL_PEERS.get(handlerName);
		}
		return res;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeerTable#getDiameterPeer(java.lang.String,
	 *      long)
	 */
	@Override
	@Deprecated
	public DiameterPeer getDiameterPeer(String destRealm, long application) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getDiameterPeer (destRealm, app id)");
		}
		List<DiameterRoute> routes = Utils.getRouteTableManager().getDiameterRoutes(destRealm, application, 0);
		if (routes.isEmpty()) {
			return null;
		}
		// search for first connected peer 
		for (DiameterRoute route : routes) {
			DiameterPeer peer = route.getRoutingPeer();
			if (peer.isConnected() && !peer.isQuarantined()) {
				return peer;
			}
		}
		return null;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeerTable#getDiameterPeer(com.nextenso.proxylet.diameter.DiameterPeer,
	 *      java.lang.String, long)
	 */
	@Override
	@Deprecated
	public DiameterPeer getDiameterPeer(DiameterPeer localPeer, String destRealm, long application) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getDiameterPeer (" + localPeer + " ," + destRealm + ", " + application + ")");
		}
		List<DiameterRoute> routes = Utils.getRouteTableManager().getDiameterRoutes(localPeer, destRealm, application, 0);
		if (routes.isEmpty()) {
			return null;
		}
		// search for first connected peer 
		for (DiameterRoute route : routes) {
			DiameterPeer peer = route.getRoutingPeer();
			if (peer.isConnected() && !peer.isQuarantined()) {
				return peer;
			}
		}
		return null;
	}

	/**
	 * Creates a new remote peer for an handler.
	 * 
	 * @param handlerName The handler name.
	 * @param originHost The remote origin host.
	 * @param originRealm The remote origin realm
	 * @param host The host.
	 * @param port The port.
	 * @param secure true if the connection is secure.
	 * @return The new remote peer.
	 */
	public RemotePeer newRemotePeer(String handlerName, String originHost, String originRealm, String host, int port, boolean secure, Protocol protocol) {

		if (handlerName == null) {
			return null;
		}

		RemotePeer res = null;
		PeerTable table = PEERS_TABLES.get(handlerName);
		if (table != null) {
			res = table.newPeer(handlerName, originHost, originRealm, host, port, secure, protocol);
		}

		return res;
	}

	/**
	 * Retrieves the peer table attached to an handler.
	 * 
	 * @param handlerName The handler name.
	 * @return The peer table or null if not found.
	 */
	public PeerTable getPeerTable(String handlerName) {
		if (handlerName == null) {
			return null;
		}
		PeerTable table = PEERS_TABLES.get(handlerName);
		return table;
	}

	public DiameterPeer getDiameterPeerById(String handlerName, long peerId) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getDiameterPeerById: peer Id=" + peerId + ", handlerName=" + handlerName);
		}
		Peer res = null;
		if (handlerName != null) {
			PeerTable table = getPeerTable(handlerName);
			if (table != null) {
				res = table.getPeerById(peerId);
			} else {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("getDiameterPeerById: no peer table for this handler name");
				}
			}
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getDiameterPeerById: res=" + res);
		}

		return res;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeerTable#addListener(com.nextenso.proxylet.diameter.DiameterPeerListener)
	 */
	@Override
	public void addListener(DiameterPeerListener listener) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("addListener: " + this + " add the listener=" + listener);
		}

		_listeners.add(new ListenerWithExecutor(listener));
	}

	/**
	 * 
	 * @see com.nextenso.proxylet.diameter.DiameterPeerTable#removeListener(com.nextenso.proxylet.diameter.DiameterPeerListener)
	 */
	@Override
	public void removeListener(DiameterPeerListener listener) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("removeListener: " + this + " remove the listener=" + listener);
		}
		for (ListenerWithExecutor l : _listeners) {
			if (listener == l.getListener()) {
				_listeners.remove(l);
				break;
			}
		}
	}

	@Override
	public DiameterPeer newDiameterPeer(Map<String, Object> props){
		DiameterPeer localPeer = (DiameterPeer) props.get (PROP_LOCAL_PEER);
		String remoteOriginHost = (String) props.get (PROP_REMOTE_ORIGIN_HOST);
		Object host = props.get (PROP_REMOTE_HOST);
		Integer port = (Integer) props.get (PROP_REMOTE_PORT);
		Boolean secure = (Boolean) props.get (PROP_CONNECTION_SECURE);
		Protocol protocol = (Protocol) props.get (PROP_CONNECTION_PROTOCOL);
		Integer peerId = (Integer) props.get (PROP_PEER_ID);
		
		if (remoteOriginHost == null) {
			throw new NullPointerException("The origin host cannot be null");
		}
		if (localPeer == null) {
			throw new NullPointerException("The local peer cannot be null");
		}
		if (localPeer.isLocalDiameterPeer () == false)
			throw new IllegalArgumentException (localPeer+" : is not a local peer");

		if (protocol == null) protocol = Protocol.TCP;
		if (secure == null) secure = Boolean.FALSE;
		if (host == null) throw new IllegalArgumentException ("The remote host(s) must be specified");
		if (port == null) throw new IllegalArgumentException ("The remote port must be specified");
		List<String> hosts = null;
		if (host instanceof String){
			hosts = new ArrayList<> (1);
			hosts.add ((String) host);
		} else {
			hosts = (List) host;
			if (hosts.size () == 0)
				throw new IllegalArgumentException ("The remote host(s) must be specified");
		}
		
		String handlerName = ((Peer) localPeer).getHandlerName();
		PeerTable table = PEERS_TABLES.get(handlerName);
		if (table == null) {
			throw new RuntimeException ("The local peer table cannot be found : "+handlerName);
		}
		if (LOGGER.isDebugEnabled ())
		    LOGGER.debug ("newDiameterPeer : "+props);
		if (peerId == null){
			// legacy behavior
			// we do a lookup first
			DiameterPeer peer = table.getDiameterPeer(remoteOriginHost);
			if (peer == null) {
				// the peer id shall be generated by jdiameter and will be > 0xFFFFFFFFL
				// the connection may be shared with other jdiameters
				StaticPeer sPeer = new StaticPeer((LocalPeer)localPeer, Peer.SEED_REMOTE_I_SHARED.incrementAndGet (), remoteOriginHost, hosts, port, secure, protocol, false);
				peer = table.addStaticPeer(sPeer);
			} else {
			    if (DiameterProperties.peerReconnectEnabled () == false) return null; // CSFAR-3032 : no reconnect allowed --> dont return the existing peer
			}
			return peer;
		}
		if (peerId == -1){
		    // the peer id shall be generated by jdiameter and will be > 0xFFFFFFFFL
		    // the connection will NOT be shared with other jdiameters
		    StaticPeer sPeer = new StaticPeer((LocalPeer)localPeer, Peer.SEED_REMOTE_I_NOT_SHARED.incrementAndGet (), remoteOriginHost, hosts, port, secure, protocol, false);
		    return table.addStaticPeer(sPeer);
		} else {
		    long peerIdAsLong = ((long) peerId) & 0xFFFFFFFFL;
		    // we still do a lookup mixing peer-id and remoteOriginHost : indeed the application may still use duplicates (P&C does it without check)
		    for (DiameterPeer peer : table.getPeersByOriginHost(remoteOriginHost)){
			if (peer.getId () == peerIdAsLong){
			    if (DiameterProperties.peerReconnectEnabled () == false) return null; // CSFAR-3032 : no reconnect allowed --> dont return the existing peer
			    return peer;
			}
		    }
		    // the peer id is used and is <= 0xFFFFFFFFL
		    // the connection will be shared with other jdiameters
		    StaticPeer sPeer =  new StaticPeer((LocalPeer)localPeer, peerIdAsLong, remoteOriginHost, hosts, port, secure, protocol, false);
		    return table.addStaticPeer(sPeer);
		}
	}
	

	/**
	 * 
	 * @see com.nextenso.proxylet.diameter.DiameterPeerTable#getDiameterPeers(com.nextenso.proxylet.diameter.DiameterPeer)
	 */
	@Override
	public List<DiameterPeer> getDiameterPeers(DiameterPeer localPeer) {
		if (localPeer == null) {
			return null;
		}

		String handlerName = ((Peer) localPeer).getHandlerName();
		PeerTable table = PEERS_TABLES.get(handlerName);
		if (table == null) {
			return null;
		}

		List<DiameterPeer> res = new ArrayList<DiameterPeer>();
		res.addAll(table.getPeers());
		return res;
	}

	public void updateWhiteListFilters() {
		try {

			PeersParser parser = new PeersParser();
			List<DiameterConnectionFilter> filters = parser.parseWhiteListFilters();
			Utils.setWhiteListFilters(filters);

			// close the no more accepted remote peers
			for (PeerTable table : PEERS_TABLES.values()) {
				for (DiameterPeer peer : table.getPeers()) {
					if (!peer.isLocalDiameterPeer() && !peer.isLocalInitiator()
							&& Utils.checkConnectionFilters(peer) != DiameterBaseConstants.RESULT_CODE_DIAMETER_SUCCESS) {
						if (LOGGER.isInfoEnabled()) {
							LOGGER.info("updateDynamicPeers: connection filter have been changed -> disconnected peer=" + peer);
						}
						peer.disconnect(DiameterBaseConstants.VALUE_DISCONNECT_CAUSE_DO_NOT_WANT_TO_TALK_TO_YOU);
					}
				}
			}
		}
		catch (ConfigException e) {
			LOGGER.warn("Cannot read updated dynamic peers -> ignore new defined peers");
		}

	}

	@Override
	public void removePeer(DiameterPeer peer)
		throws IllegalArgumentException {
		RemotePeer sPeer = null;
		if (peer instanceof RemotePeer) {
			sPeer = (RemotePeer) peer;
		} else {
			throw new IllegalArgumentException("Not a static or remote peer");
		}

		String handlerName = sPeer.getHandlerName();
		PeerTable table = PEERS_TABLES.get(handlerName);
		if (table != null) table.removePeer(sPeer);

		// remove from the route table
		RouteTableManager routeManager = Utils.getRouteTableManager();
		routeManager.removePeer(sPeer);

	}
}
