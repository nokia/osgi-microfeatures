// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.peer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.utils.ConfigException;

import com.nextenso.diameter.agent.Utils;
import com.nextenso.diameter.agent.DiameterProperties;
import com.nextenso.diameter.agent.peer.xml.PeersParser;
import com.nextenso.proxylet.diameter.DiameterConnectionFilter;
import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterPeer.Protocol;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;

/**
 * The Peer Table.
 */
public class PeerTable {

	private static final Logger LOGGER = Logger.getLogger("agent.diameter.peertable");

	private final ReentrantReadWriteLock _rwLock = new ReentrantReadWriteLock();
	private final ReadLock _readLock = _rwLock.readLock();
	private final WriteLock _writeLock = _rwLock.writeLock();
	private final List<StaticPeer> _staticPeers = new ArrayList<StaticPeer>();
	private final Map<Long, Peer> _peersById = Utils.newConcurrentHashMap();
	private final Map<String, List<Peer>> _peersByOriginHost = Utils.newConcurrentHashMap();

	public PeerTable() {}

	public void init(LocalPeer localPeer)
		throws ConfigException {
		PeersParser parser = new PeersParser();
		List<StaticPeer> peers = parser.parseStaticPeers(localPeer);
		setStaticPeers(peers);
		
		// TODO : set the filters per handler --> hence per localPeer
		// this code belows seem broken : same filters are added each time a PeerTable is inited !
		//List<DiameterConnectionFilter> connectionFilters = parser.parseWhiteListFilters();
		//Utils.setWhiteListFilters(connectionFilters);
	}

	/**
	 * Sets the static peers.
	 * 
	 * @param peers The peers.
	 */
	private void setStaticPeers(List<StaticPeer> peers) {

		_writeLock.lock();
		try {
			if (!_staticPeers.isEmpty()) {
				return;
			}

			_staticPeers.clear();
			_peersById.clear();
			_peersByOriginHost.clear();

			for (StaticPeer peer : peers) {
				addStaticPeer(peer);
			}
		}
		finally {
			_writeLock.unlock();
		}

	}

	public StaticPeer addStaticPeer(StaticPeer peer) {
		_writeLock.lock();
		try {
			if (_staticPeers.contains (peer))
				// CSFS-6114 in case we switched local peer : by precaution, always call addStaticPeer and make it idempotent
				return peer;
			_staticPeers.add(peer);
			_peersById.put(peer.getId(), peer);
			addPeerByOriginHost(peer.getOriginHost(), peer);
		}
		finally {
			_writeLock.unlock();
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("addStaticPeer: add peer=" + peer);
		}

		return peer;

	}

	public void removePeer(RemotePeer sPeer) { // this is actually remoteStaticPeer (should be renamed)
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("removePeer: peer=" + sPeer);
		}
		
		_writeLock.lock();
		try {
			_staticPeers.remove(sPeer);
			_peersById.remove(sPeer.getId());
			removePeerByOriginHost(sPeer);
		}
		finally {
			_writeLock.unlock();
		}
		sPeer.close();
	}

	/**
	 * Adds the local peer if needed.
	 * 
	 * @param localPeer The local peer to be added.
	 */
	public void addLocalPeer(LocalPeer localPeer) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("updateLocalPeer: local peer=" + localPeer);
		}

		// add local peer in case it is not among the static peers
		_writeLock.lock ();
		try{
			Peer peer = getPeerByOriginHost(localPeer.getOriginHost());
			if (peer != null){
				// unexpected though
				return;
			}
			addPeerByOriginHost(localPeer.getOriginHost(), localPeer);
		}
		finally {
			_writeLock.unlock();
		}
	}

	/**
	 * Called When Mux is connected.
	 */
	public void muxOpened() {
		_readLock.lock();
		try {
			for (Peer peer : _staticPeers) {
				if (peer.isLocalDiameterPeer()) {
					continue;
				}
				StaticPeer sp = (StaticPeer) peer;
				if (!sp.isConfigured ()) return;
				// else deadlock possible / CSFAR-1905
				sp.connect();
			}
		}
		finally {
			_readLock.unlock();
		}
	}

	/**
	 * Called when a conn-ack arrives on a I-Socket
	 * 
	 * @param peerId The peer identifier.
	 * @return The peer or null if not found.
	 */
	public Peer getPeerById(long peerId) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getPeerById: peerid=" + peerId);
		}

		_readLock.lock();
		try {
			return _peersById.get(peerId);
		}
		finally {
			_readLock.unlock();
		}

	}

	/**
	 * Gets a peer according to its origin host.
	 * 
	 * @param originHost The origin host to retrieve.
	 * @return The peer or null if unknown.
	 */
	private Peer getPeerByOriginHost(String originHost) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getPeerByOriginHost: originHost=" + originHost);
		}
		if (originHost == null) {
			return null;
		}

		Peer res;
		originHost = originHost.toLowerCase(Locale.getDefault());
		_readLock.lock();
		try {
			List<Peer> peers = _peersByOriginHost.get(originHost);
			res = peers != null ? peers.get (0) : null;
		}
		finally {
			_readLock.unlock();
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getPeerByOriginHost: res=" + res);
		}

		return res;
	}

	
	protected List<DiameterPeer> getPeersByOriginHost(String originHost) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getPeersByOriginHost: originHost=" + originHost);
		}
		if (originHost == null) {
			return null;
		}

		originHost = originHost.toLowerCase(Locale.getDefault());
		_readLock.lock();
		List<DiameterPeer> res = null;
		try {
			List<Peer> peers = _peersByOriginHost.get(originHost);
			if (peers == null){
				res = new ArrayList<> (1);
			} else {
				res = new ArrayList<> (peers.size ());
				res.addAll (peers);
			}
		}
		finally {
			_readLock.unlock();
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getPeerByOriginHost: res=" + res);
		}

		return res;
	}
	
	/**
	 * Called when a CER is received.
	 * 
	 * @param handlerName The handler name.
	 * @param originHost The origin host.
	 * @param originRealm The origin realm.
	 * @param host The host.
	 * @param port The port.
	 * @param secure true if secure.
	 * @return The peer.
	 */
	public synchronized RemotePeer newPeer(String handlerName, String originHost, String originRealm, String host, int port, boolean secure,
			Protocol protocol) {
		RemotePeer peer = null;
		if (DiameterProperties.prohibitMultipleRemoteOriginHost ()){
			Peer tmp = getPeerByOriginHost(originHost);
			if (tmp instanceof LocalPeer){
			    if (LOGGER.isInfoEnabled ())
				LOGGER.info ("Rejecting new remote peer (host="+host+", port="+port+", protocol="+protocol+") : originHost identical to ours : "+originHost);
			    return null;
			}
			peer = (RemotePeer) tmp;
			if (peer != null) {
				return peer;
			}
		}
		peer = new RemotePeer(handlerName, originHost, originRealm, host, port, secure, protocol);
		
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("newPeer: peer=" + peer);
		}
		addPeerByOriginHost(originHost, peer);
		return peer;
	}

	private void addPeerByOriginHost(String originHost, Peer peer) {
		originHost = originHost.toLowerCase(Locale.getDefault());
		_writeLock.lock();
		try {
			List<Peer> peers = _peersByOriginHost.get(originHost);
			if (peers == null)
				_peersByOriginHost.put(originHost, peers = new ArrayList<Peer> ());
			else if (LOGGER.isInfoEnabled ())
				LOGGER.info ("addPeerByOriginHost : peer(s) already connected with the same originHost : "+peers.size ());
			peers.add (peer);
		}
		finally {
			_writeLock.unlock();
		}
	}

	/**
	 * Called when a peer is disconnected.
	 * 
	 * @param peer The disconnected peer.
	 */
	public void disconnected(RemotePeer peer) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("disconnected: remove peer=" + peer);
		}

		if (!(peer instanceof StaticPeer)) {
			removePeerByOriginHost(peer);
		}
	}

	private void removePeerByOriginHost(Peer peer) {
		// IMPORTANT : looking up with the peer origin host would seem smarter
		// but some people register a static peer with a remoteOriginHost which is not the actual one !
		// hence we need to iterate to find the peer and remove it
		_writeLock.lock();
		try {
			String key = null;
			loop : for (Map.Entry<String, List<Peer>> entry : _peersByOriginHost.entrySet()) {
				List<Peer> value = entry.getValue ();
				if (value.remove (peer)){
				    if (value.size () == 0)
					key = entry.getKey();
				    break loop;
				}
			}

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("removePeerByOriginHost:  key=" + key);
			}
			if (key != null)
				_peersByOriginHost.remove(key);
		} finally {
			_writeLock.unlock();
		}


	}

	/**
	 * Gets the peer.
	 * 
	 * @param originHost The origin host.
	 * @return The peer.
	 */
	public DiameterPeer getDiameterPeer(String originHost) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getDiameterPeer (originHost)");
		}
		return getPeerByOriginHost(originHost);
	}

	/**
	 * Gets a list with all remote and static peers.
	 * 
	 * It creates a new list for each call.
	 * 
	 * @return The list of known peers.
	 */
	public List<DiameterPeer> getPeers() {
		_readLock.lock ();
		try{
			List<DiameterPeer> res = new ArrayList<DiameterPeer>();
			for (List<Peer> peers : _peersByOriginHost.values ())
				res.addAll(peers);
			return res;
		}finally{
			_readLock.unlock ();
		}
	}

	public void closePeers() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("closePeers: table=" + this);
		}
		_readLock.lock ();
		try{
			for (List<Peer> peers : _peersByOriginHost.values()){
				for (Peer peer : peers) {
					if (peer.isLocalDiameterPeer()) {
						continue;
					}
					if (peer instanceof StaticPeer) {
						((StaticPeer) peer).cancelTimer();
					}

					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("closePeers: disconnect peer=" + peer);
					}
					try {
						peer.disconnect(DiameterBaseConstants.VALUE_DISCONNECT_CAUSE_REBOOTING);
					}
					catch (IllegalStateException e) {
						LOGGER.warn("Cannot disconnect the peer " + peer + " - " + e);
					}
				}
			}
		}finally{
			_readLock.unlock ();
		}
	}

}
