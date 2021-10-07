package com.nextenso.diameter.agent.peer;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

import com.alcatel.as.util.sctp.*;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.nextenso.diameter.agent.DiameterProperties;
import com.nextenso.diameter.agent.PropertiesDeclaration;
import com.nextenso.diameter.agent.Utils;

public class StaticPeer
		extends RemotePeer
		implements Runnable {

	private Future _future = null;
	private static final Logger LOGGER = Logger.getLogger("agent.diameter.peer.static");

	private String[] _srcIPs;
	private int _srcPort = -1;
	private Map<SctpSocketOption, SctpSocketParam> _sctpOptions;
	private Map<String, String> _params;
	private boolean _configured;
	private boolean _firstConnect = true;

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		connect();
	}

	public StaticPeer(LocalPeer localPeer, long id, String originHost, List<String> hosts, int port, boolean secure, Protocol protocol, boolean autoReconnect) {
		super(localPeer.getHandlerName (), id, originHost, null, hosts, port, secure, protocol);
		
		setLocalOriginHost(localPeer.getOriginHost());
		setLocalOriginRealm(localPeer.getOriginRealm());

		if (autoReconnect){
			_configured = true;
			PlatformExecutor executor = Utils.getCurrentExecutor();
			_future = Utils.scheduleAtFixedRate(executor, this, DiameterProperties.getTcTimer(), DiameterProperties.getTcTimer(), TimeUnit.SECONDS);
		}
	}

	public boolean isConfigured (){ // indicates if the static peer is part of the peers.xml
		return _configured;
	}
	
	public void setSrc (String[] ips, int port){
		_srcIPs = ips;
		_srcPort = port;
	}

	/**
	 * Connects.
	 */
	@Override
	public void connect() {
		connect (_srcPort, _srcIPs);
	}
	@Override
	public void connect(int localPort, String... localIPs){
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("connect: handler=" + getHandlerName());
		}
		if (_firstConnect){
		    _firstConnect = false;
		} else if (DiameterProperties.peerReconnectEnabled () == false){
		    throw new RuntimeException ("DiameterPeer reconnection is not enabled (property "+PropertiesDeclaration.ENABLE_PEER_RECONNECT+" set to false)");
		}
		if (isDisconnected()) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("connect: isDisconnected-> connect");
			}
			getStateMachine().start(_params, _sctpOptions, localPort, localIPs);
		} else if (isDisconnecting()) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("connect: isDisconnecting-> IllegalStateException");
			}
			throw new IllegalStateException("Peer is disconnecting -> cannot connect");
		} else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("connect: is not disconnected-> no nothing");
			}
		}
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#isLocalInitiator()
	 */
	@Override
	public boolean isLocalInitiator() {
		return true;
	}

	/**
	 * @see com.nextenso.diameter.agent.peer.RemotePeer#getLogger()
	 */
	@Override
	protected Logger getLogger() {
		return LOGGER;
	}

	public void cancelTimer() {
		if (_future != null) {
			_future.cancel(true);
			_future = null;
		}
	}

	@Override
	public void close() {
	cancelTimer();

		super.close();
	}

    
	public void setSctpSocketOptions (java.util.Map options){
		_sctpOptions = options; // store for later connect
		if (getStateMachine ().isConnected ()){ // apply now if connected
			super.setSctpSocketOptions (options);
		}
	}
	public void setParameters (java.util.Map<String, String> params){
		_params = params; // store for later connect
		if (getStateMachine ().isConnected ()){ // apply now if connected
			super.setParameters (params);
		}
	}
}
