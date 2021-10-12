// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.peer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.alcatel.as.service.metering.Counter;
import com.alcatel.as.service.metering.Rate;
import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.util.sctp.SctpSocketOption;
import com.alcatel.as.util.sctp.SctpSocketParam;
import com.nextenso.diameter.agent.DiameterProperties;
import com.nextenso.diameter.agent.RequestManager;
import com.nextenso.diameter.agent.Utils;
import com.nextenso.diameter.agent.impl.DiameterMessageFacade;
import com.nextenso.diameter.agent.impl.DiameterResponseFacade;
import com.nextenso.diameter.agent.metrics.DiameterMeters;
import com.nextenso.diameter.agent.peer.statemachine.DiameterStateMachine;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.socket.SimpleSctpSocket;
import com.nextenso.mux.socket.SimpleSocket;
import com.nextenso.mux.socket.SimpleTcpSocket;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterPeer.Protocol;
import com.nextenso.proxylet.diameter.DiameterPeerListener;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;

import alcatel.tess.hometop.gateways.utils.ByteOutputStream;
import alcatel.tess.hometop.gateways.utils.IPAddr;

/**
 * The peer socket.
 */
public abstract class PeerSocket
		extends SimpleSocket {

	private static final RequestManager REQUEST_MGR_GLOBAL = new RequestManager (){
			@Override
			public void close (){} // do not clean requests - let timeouts occur
		};

	protected static final String EMPTY = "";
	private static final Logger LOGGER = Logger.getLogger("agent.diameter.peersocket");
	private static final int SCTP_MAX_OUT_STREAM = 5;
	private static final int SCTP_MAX_IN_STREAM = 5;

	private DiameterStateMachine _stateMachine;
	private boolean _ignoreCloseEvent;
	private volatile boolean _closed;
	private String _handlerName = null;
	private RequestManager _requestManager = new RequestManager();
	private Protocol _protocol = Protocol.TCP;
	private SimpleTcpSocket _tcpSocket = null;
	private SimpleSctpSocket _sctpSocket = null;
	private InetSocketAddress[] _addresses;
	private int _hostIndex = 0; // to keep track of the next host to try
	private String[] _localIPs;
	private int _localPort;
	private Map<SctpSocketOption, SctpSocketParam> _sctpOptions;
	private Map<String, String> _params;
	private DiameterMeters _meters;
	private List<String> _hosts;
        private volatile boolean _active = false; // TODO maybe volatile not needed.
        private final List<PeerAddrChangeEvent> _delayedEvents = new ArrayList<>(1);

        private static class PeerAddrChangeEvent {
	    final String addr;
	    final int port;
	    final MuxHandler.SctpAddressEvent event;

	    PeerAddrChangeEvent(String addr, int port, MuxHandler.SctpAddressEvent event) {
		this.addr = addr;
		this.port = port;
		this.event = event;
	    }
	}

	protected abstract Logger getLogger();

	private PeerSocket(MuxConnection connection, String[] localIPs, int localPort, Protocol protocol, int socketId) {
		super(connection, -2, socketId, null, 0);
		_localIPs = localIPs;
		_localPort = localPort;
		_protocol = protocol;
		if (_protocol == null) {
			_protocol = Protocol.TCP;
		}
		_requestManager = DiameterProperties.globalRequests () ? REQUEST_MGR_GLOBAL : new RequestManager ();
	}

	/**
	 * Creates a new Initiator socket (acts as client).
	 */
	protected PeerSocket(DiameterStateMachine stateMachine, MuxConnection connection, RemotePeer peer, String[] localIPs, int localPort, Protocol protocol, Map<SctpSocketOption, SctpSocketParam> sctpOptions, Map<String, String> params) {
		this(connection, localIPs, localPort, protocol, 0);
		if (getLogger().isDebugEnabled()) {
			getLogger().debug("constr: state=" + stateMachine + ", connection=" + connection + ", peer=" + peer + ", localIP=" + localIPs+", localPort="+localPort);
		}
		_stateMachine = stateMachine;
		_ignoreCloseEvent = false;
		_handlerName = peer.getHandlerName();
		_sctpOptions = sctpOptions;
		_params = params;
		if (!createSockets ()) throw new RuntimeException ("Cannot create an Initiator Socket with no destination host");
	}
	private boolean createSockets (){
		// this method is called many times since there are many configured hosts for the remote peer
		RemotePeer peer = _stateMachine.getPeer ();
		String host = getNextHost (peer);
		if (host == null) return false;
		if (getProtocol() == Protocol.TCP) {
		    String localIP = _localIPs != null && _localIPs.length == 1 ? _localIPs[0] : null;
		    if (localIP == null) localIP = DiameterProperties.getClientSrcTcp (_handlerName);
		    int localPort = _localPort != -1 ? _localPort : DiameterProperties.getClientSrcPortTcp (_handlerName);
		    _tcpSocket = new SimpleTcpSocket(getMuxConnection (), host, EMPTY, peer.getPort(), localIP, localPort, peer.isEncrypted(), peer.getId());
		    if (_params != null) _tcpSocket.setParams (_params);
		} else if (getProtocol() == Protocol.SCTP) {
		    String[] localIPs = _localIPs;
		    if (localIPs == null || localIPs.length == 0) localIPs = DiameterProperties.getClientSrcSctp (_handlerName);
		    int localPort = _localPort != -1 ? _localPort : DiameterProperties.getClientSrcPortSctp (_handlerName);
		    _sctpSocket = new SimpleSctpSocket(getMuxConnection (), host, peer.getPort(), localIPs, localPort, peer.isEncrypted(), peer.getId(), SCTP_MAX_OUT_STREAM, SCTP_MAX_IN_STREAM);
		    if (_sctpOptions != null) _sctpSocket.setOptions (_sctpOptions);
		    if (_params != null) _sctpSocket.setParams (_params);
		}
		return true;
	}

	/**
	 * Creates a new Responder socket (acts as server).
	 */
	protected PeerSocket(MuxConnection connection, int socketId, long connectionId, String remoteIP, int remotePort, String localIP, int localPort,
			     String virtualIP, int virtualPort, boolean secure, Protocol protocol, String[] remoteIPs, String[] localIPs) {
		this(connection, localIPs, localPort, protocol, socketId);
		if (getLogger().isDebugEnabled()) {
			getLogger().debug("constr: connection=" + connection + ",  id=" + socketId);
		}
		_ignoreCloseEvent = true;
		_handlerName = Utils.getHandlerName(connection);
		if (getProtocol() == Protocol.TCP) {
			_tcpSocket = new SimpleTcpSocket(connection, socketId, remoteIP, remotePort, localIP, localPort, virtualIP, virtualPort, secure, true, 0);
			_hosts = new ArrayList<> (1);
			_hosts.add (remoteIP);
		} else if (getProtocol() == Protocol.SCTP) {
			_sctpSocket = new SimpleSctpSocket(connection, socketId, connectionId, remoteIPs, remotePort, localIPs, localPort, SCTP_MAX_OUT_STREAM, SCTP_MAX_IN_STREAM, secure);
			_hosts = new ArrayList<> (remoteIPs.length);
			for (int i=0; i<remoteIPs.length; i++) _hosts.add (remoteIPs[i]);
		}
		setLocalInetSocketAddresses (localIPs, localPort);
	}
	
	private void setLocalInetSocketAddresses (String[] ips, int port){
		_addresses = new InetSocketAddress[ips.length];
		int index = 0;
		for (String ip: ips) _addresses[index++] = getInetSocketAddress (ip, port);
	}
	private void setLocalInetSocketAddresses (String ip, int port){
		_addresses = new InetSocketAddress[]{getInetSocketAddress (ip, port)};
	}
	private static InetSocketAddress getInetSocketAddress (String ip, int port){
		try{
			byte[] ipB = IPAddr.toByteArray(ip);
			InetAddress ia = InetAddress.getByAddress(ipB);
			return new InetSocketAddress(ia, port);
		}catch(Exception e){
			LOGGER.warn ("Failed to setLocalInetSocketAddress : ip="+ip+", port="+port, e);
		}
		return null;
	}
	public InetSocketAddress getLocalInetSocketAddress (){
		return _addresses[0];
	}
	public InetSocketAddress[] getLocalInetSocketAddresses (){
		return _addresses;
	}
	private String getNextHost (Peer peer){
		List<String> hosts = peer.getConfiguredHosts ();
		if (_hostIndex >= hosts.size ()) return null;
		String host = hosts.get (_hostIndex++);
		peer.setHost (host);
		return host;
	}
	private void resetNextHost (){
		_hostIndex = 0;
	}

	/**
	 * Gets the state machine.
	 * 
	 * @return The state machine.
	 */
	public DiameterStateMachine getStateMachine() {
		return _stateMachine;
	}

	// when we want to disconnect in R-Reject after unexpected R-Conn-CER was received
	public void unbindStateMachine() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("unbindStateMachine: _stateMachine=" +_stateMachine);
		}
		if (_stateMachine != null) {
			_stateMachine.unbind(this);
		}
		_stateMachine = null;
		_ignoreCloseEvent = true;
	}

	/**
	 * Indicates whether the peer socket is an initiator socket.
	 * 
	 * An initiator socket is a socket opened to send data (acts as client).
	 * 
	 * @return true if it is an initiator socket.
	 */
	public abstract boolean isInitiator();

	/**
	 * Indicates whether the peer socket is a responder socket.
	 * 
	 * An responder socket is a socket opened to receive data (acts as server).
	 * 
	 * @return true if the peer socket is a responder socket.
	 */
	public final boolean isResponder() {
		return (!isInitiator());
	}

	/**
	 * Gets the request manager.
	 * 
	 * @return The request manager.
	 */
	public RequestManager getRequestManager() {
		return _requestManager;
	}

	/**
	 * Opens the socket.
	 * 
	 * @return true if the socket is opened.
	 */
	public boolean open() {
		if (getProtocol() == Protocol.TCP) {
			return _tcpSocket.open(-1L);
		}
		if (getProtocol() == Protocol.SCTP) {
			return _sctpSocket.open(-1L);
		}
		return false;
	}
	
	public void setDiameterMeters(DiameterMeters meters) {
		_meters = meters;
	}
	
	public DiameterMeters getDiameterMeters() {
		return _meters;
	}

	/**
	 * Called when the Initiator socket is connected.
	 * 
	 * @param socketId The socket id.
	 * @param remoteIP The remote IP address.
	 * @param localIP The local IP address.
	 * @param localPort The local Port.
	 * @return true if connected.
	 */
	public void tcpConnected(int socketId, String remoteIP, String localIP, int localPort) {
		_tcpSocket.set(socketId, remoteIP, localIP, localPort);
		_hosts = new ArrayList<> (1);
		_hosts.add (remoteIP);
		setLocalInetSocketAddresses (localIP, localPort);
		getStateMachine().connected(this);
	}

	public void sctpConnected(int socketId, String[] remoteIPs, String[] localIPs, int localPort, int maxOutStreams, int maxInStreams) {
		_sctpSocket.set(socketId, remoteIPs, localIPs, localPort, maxOutStreams, maxInStreams);
		_hosts = new ArrayList<> (remoteIPs.length);
		for (int i=0; i<remoteIPs.length; i++) _hosts.add (remoteIPs[i]);
		setLocalInetSocketAddresses (localIPs, localPort);
		getStateMachine().connected(this);
	}

	/**
	 * Called when the static peer cannot be connected.
	 */
	public void notConnected() {
		String host = _stateMachine.getPeer ().getHost ();
		if (createSockets () && open ()){
			if (LOGGER.isInfoEnabled ())
				LOGGER.info("notConnected to : "+host+" : trying next : "+_stateMachine.getPeer ().getHost ());
			return;
		}
		if (LOGGER.isInfoEnabled ()) LOGGER.info("notConnected to : "+host);
		resetNextHost ();
		getStateMachine().notConnected(this);
	}

	/**
	 * Disconnects the peer socket.
	 */
	public void disconnect(boolean reset) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("disconnect: socket id=" + getSockId()+" reset="+reset);
		}
		if (!_closed) {
		    if (reset) reset ();
		    else close();
		}
	}

	/**
	 * Closes the peer socket.
	 */
	public void closed() { closed (getSockId ());}
	public void closed(int sockId) { // we pass the sockId to avoid socket mixup (closed coming after a reconnect)
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("closed: socket id=" + sockId + ", this socket id ="+getSockId());
		}
		if (sockId != getSockId ()){ // only possible for I-Socket when we retried before the close came
		    return;
		}
		resetNextHost ();
		_closed = true;
		if (isCloseEventIgnored()) {
			LOGGER.debug("closed: ignore close event -> do not call peerDisconnected");
			return;
		}
		try {
			getStateMachine().peerDisconnected(this);
		}
		catch (IllegalStateException e) {
			// the socket is already closed
		}
		
		if(_meters != null) {
			_meters.socketClosed();
		}
		
		// call it after the remote peer disconnection was propagated
		getRequestManager().close();

	}
    
	public void sctpPeerAddressChanged(String addr, int port, MuxHandler.SctpAddressEvent event) {
	    if (! _active) {
		_delayedEvents.add(new PeerAddrChangeEvent(addr, port, event));
	    } else {
		getStateMachine ().sctpPeerAddressChanged (addr, port, DiameterPeerListener.SctpAddressEvent.valueOf(event.name()));
	    }
	}

	public void activate(boolean active) {
	    _active = active;
	    if (active) {
		for (PeerAddrChangeEvent delayedEvent : _delayedEvents) {
		    getStateMachine ().sctpPeerAddressChanged (delayedEvent.addr, delayedEvent.port, DiameterPeerListener.SctpAddressEvent.valueOf(delayedEvent.event.name()));
		}
		_delayedEvents.clear();
	    } 
	}
	
	protected boolean isCloseEventIgnored() {
		return _ignoreCloseEvent;
	}

	/**
	 * Processes the message.
	 * 
	 * @param message The message.
	 */
	public void processMessage(DiameterMessageFacade message) {
		getStateMachine().processMessage(message, this);
	}

	/**
	 * Gets the name of the handler this socket is associated..
	 * 
	 * @return The name of the bundle.
	 */
	public String getHandlerName() {
		return _handlerName;
	}

	private DiameterAVP initialOriginHost = null;
	private DiameterAVP initialOriginRealm = null;
	public boolean handleRoutingAVPs (DiameterMessageFacade msg){
		if (msg.getDiameterApplication () == 0){
			// FOR CER/CEA/DWR/DWA/DPR/DPA, we replace with the peer that we pretend to be
			// this peer may have been set by a CapabilitiesListener hence unknown to agent
			if (initialOriginHost != null){
				DiameterAVP avp = msg.getDiameterAVP (DiameterBaseConstants.AVP_ORIGIN_HOST);
				if (avp != null) avp.setValue (initialOriginHost.getValue (), false);
				avp = msg.getDiameterAVP (DiameterBaseConstants.AVP_ORIGIN_REALM);
				if (avp != null) avp.setValue (initialOriginRealm.getValue (), false);
				return true;
			}
		}
		return false;
	}
	/**
	 * Writes the message on the socket.
	 * 
	 * @param message The message.
	 * @return true if sent.
	 */
	public boolean write(DiameterMessageFacade message) {
	    if (initialOriginHost == null){
		    // FOR CER/CEA/DWR/DWA/DPR/DPA, we replace with the peer that we pretend to be
		    // this peer may have been set by a CapabilitiesListener hence unknown to agent
		    initialOriginHost = (DiameterAVP) message.getDiameterAVP (DiameterBaseConstants.AVP_ORIGIN_HOST).clone();
		    initialOriginRealm = (DiameterAVP) message.getDiameterAVP (DiameterBaseConstants.AVP_ORIGIN_REALM).clone();
		    if (LOGGER.isDebugEnabled ())
			    LOGGER.debug (this+" : storing originHost and originRealm for this PeerSocket : "+initialOriginHost+" / "+initialOriginRealm);
	    }
	    ByteOutputStream out = new ByteOutputStream(128);
	    message.writeStackTimestamp (out);
	    message.getBytes(out);
	    byte[] buffer = out.toByteArray(false);

		boolean sent = false;

		// we "give" our buffer to the mux I/O layer
		if (getProtocol() == Protocol.TCP) {
			sent = _tcpSocket.sendData(buffer, 0, out.size(), false);
		}
		if (getProtocol() == Protocol.SCTP) {
		    //sent = _sctpSocket.sendData(buffer, 0, out.size(), false);
		    // #DCTPD01108451 : added time to live and unordered
		    sent = _sctpSocket.getMuxConnection().sendSctpSocketData(_sctpSocket.getSockId(), null, DiameterProperties.getSctpUnordered (), true, 0, 0, DiameterProperties.getSctpTimeToLive (), false, java.nio.ByteBuffer.wrap(buffer, 0, out.size()));
		}

		if (sent) {
			if(_meters != null) {
				_meters.incClientWriteMeter(message);
			}
			
			Counter sizeCounter = Utils.getMsgSizeCounter(message, true);
			if (sizeCounter != null) {
				sizeCounter.add(out.size());
			}
			Rate nbRate = Utils.getMsgNbRate(message, true);
			if (nbRate != null) {
				nbRate.hit();
			}
		}

		return sent;
	}

	/**
	 * @see com.nextenso.mux.socket.SimpleTcpSocket#toString()
	 */
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder();
		res.append(" handler name=").append(getHandlerName());
		res.append(", protocol=").append(getProtocol());
		res.append(", ");
		if (getProtocol() == Protocol.TCP) {
			res.append(_tcpSocket);
		} else if (getProtocol() == Protocol.SCTP) {
			res.append(_sctpSocket);
		} else {
			res.append("No socket");
		}
		return res.toString();
	}

	public void processCER(DiameterStateMachine stateMachine) {
		_ignoreCloseEvent = false;
		_stateMachine = stateMachine;
	}
	
	public void rejectCER (DiameterMessageFacade cer, DiameterMessageFacade.ParsingException pe){
		if (LOGGER.isInfoEnabled ()) LOGGER.info(this + " : exception while parsing CER : "+pe.getMessage ());
		DiameterResponseFacade resp = cer.getResponseFacade ();
		resp.setLocalOrigin(true);
		resp.setResultCode (pe.result ());
		resp.setOriginHostAVP ();
		resp.setOriginRealmAVP ();
		resp.addDiameterAVP((DiameterAVP) DiameterProperties.getProductNameAvp().clone());
		resp.addDiameterAVP((DiameterAVP) DiameterProperties.getOriginStateIdAvp().clone());
		DiameterAVP errMsg = pe.errMessageAVP ();
		if (errMsg != null) resp.addDiameterAVP (errMsg);
		if (pe.failedAVP () != null) resp.addDiameterAVP (pe.failedAVP ());
		resp.send (this);
		disconnect (false);
	}	

	public Protocol getProtocol() {
		return _protocol;
	}

	/**
	 * @see com.nextenso.mux.socket.SimpleSocket#close()
	 */
	@Override
	public boolean close() {
		getRequestManager().close();

		if (getProtocol() == Protocol.TCP) {
			return _tcpSocket.close();
		}
		if (getProtocol() == Protocol.SCTP) {
			return _sctpSocket.close();
		}
		return false;
	}
	@Override
	public boolean reset() {
		getRequestManager().close();

		if (getProtocol() == Protocol.TCP) {
			return _tcpSocket.reset();
		}
		if (getProtocol() == Protocol.SCTP) {
			return _sctpSocket.reset();
		}
		return false;
	}

	protected boolean isSecure() {
		if (getProtocol() == Protocol.TCP) {
			return _tcpSocket.isSecure();
		}
		if (getProtocol() == Protocol.SCTP) {
			return _sctpSocket.isSecure();
		}
		return false;
	}

	protected int getRemotePort() {
		if (getProtocol() == Protocol.TCP) {
			return _tcpSocket.getRemotePort();
		}
		if (getProtocol() == Protocol.SCTP) {
			return _sctpSocket.getRemotePort();
		}
		return 0;
	}

	protected String getRemoteAddr() {
		if (getProtocol() == Protocol.TCP) {
			return _tcpSocket.getRemoteAddr();
		}
		if (getProtocol() == Protocol.SCTP) {
			return _sctpSocket.getRemoteIPs()[0];
		}
		return null;
	}

	
	/**
	 * @see com.nextenso.mux.socket.SimpleSocket#getLocalIPString()
	 */
	@Override
	public String getLocalIPString() {
		if (getProtocol() == Protocol.TCP) {
			return _tcpSocket.getLocalIPString();
		}
		if (getProtocol() == Protocol.SCTP) {
			return _sctpSocket.getLocalIPString();
		}
		return null;
	}

	@Override
	public boolean open(long timeout) {
		if (getProtocol() == Protocol.TCP) {
			return _tcpSocket.open(timeout);
		}
		if (getProtocol() == Protocol.SCTP) {
			return _sctpSocket.open(timeout);
		}
		return false;
	}

	/**
	 * @see com.nextenso.mux.socket.SimpleSocket#getType()
	 */
	@Override
	public int getType() {
		if (getProtocol() == Protocol.TCP) {
			return TYPE_TCP;
		}
		if (getProtocol() == Protocol.SCTP) {
			return TYPE_SCTP;
		}
		return -1;
	}

	/**
	 * @see com.nextenso.mux.socket.SimpleSocket#getSockId()
	 */
	@Override
	public int getSockId() {
		if (getProtocol() == Protocol.TCP) {
			return _tcpSocket.getSockId();
		}
		if (getProtocol() == Protocol.SCTP) {
			return _sctpSocket.getSockId();
		}

		return super.getSockId();
	}

	public void disableRead (){
		getMuxConnection ().disableRead (getSockId ());
	}
	public void enableRead (){
		getMuxConnection ().enableRead (getSockId ());
	}

	// called when connected
	public void setSctpSocketOptions (java.util.Map options){
		if (getProtocol() == Protocol.SCTP) {
			_sctpSocket.setOptions (_sctpOptions = options);
		} else {
			LOGGER.warn (this+" : cannot set SCTP options on a TCP socket");
		}
	}
	public void setParameters (java.util.Map<String, String> params){
		if (getProtocol() == Protocol.SCTP) {
			_sctpSocket.setParams (_params = params);
		} else {
			_tcpSocket.setParams (_params = params);
		}
	}

    public List<String> getHosts(){
	return _hosts;
    }
}
