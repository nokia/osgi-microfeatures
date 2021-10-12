// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux.impl.ioh;

import java.util.Objects;

import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.SimpleMonitorable;
import com.nextenso.mux.util.MuxHandlerMeters;

public class AggregatingMuxConnectionMetersImpl extends MuxConnectionMetersImpl {

	private MuxHandlerMeters _parent;
	private Boolean _muxStarted;
	
 	public AggregatingMuxConnectionMetersImpl(MeteringService metering, SimpleMonitorable monitorable, MuxHandlerMeters parent) {
		super(metering, monitorable);
		_parent = Objects.requireNonNull(parent);
	}
	
	@Override
	public void tcpSocketConnected(int sockId, boolean clientSocket) {
		if(_acceptedTcpSockets == null || _connectedTcpSockets == null) {
			return;
		}
		if (clientSocket) {
			_channelsTcpAcceptedOpenMeter.inc(1);
			_parent.getChannelsTcpAcceptedOpenMeter().inc(1);
			_channelsTcpAcceptedMeter.inc(1);
			_parent.getChannelsTcpAcceptedMeter().inc(1);
			_acceptedTcpSockets.add(sockId);
		} else {
			_channelsTcpConnectedOpenMeter.inc(1);
			_parent.getChannelsTcpConnectedOpenMeter().inc(1);
			_channelsTcpConnectedMeter.inc(1);
			_parent.getChannelsTcpConnectedMeter().inc(1);
			_connectedTcpSockets.add(sockId);
		}
	}

	@Override
	public void tcpSocketClosed(int sockId) {
		if(_acceptedTcpSockets == null || _connectedTcpSockets == null) {
			return;
		}
		if (_acceptedTcpSockets.contains(sockId)) {
			_acceptedTcpSockets.remove(sockId);
			_channelsTcpAcceptedOpenMeter.dec(1);
			_parent.getChannelsTcpAcceptedOpenMeter().dec(1);
			_channelsTcpAcceptedClosedMeter.inc(1);
			_parent.getChannelsTcpAcceptedClosedMeter().inc(1);
		} else if(_connectedTcpSockets.contains(sockId)){
			_connectedTcpSockets.remove(sockId);
			_channelsTcpConnectedOpenMeter.dec(1);
			_parent.getChannelsTcpConnectedOpenMeter().dec(1);
			_channelsTcpConnectedClosedMeter.inc(1);
			_parent.getChannelsTcpConnectedClosedMeter().inc(1);
		}
	}

	@Override
	public void tcpSocketAborted(int sockId) {
		if(_acceptedTcpSockets == null || _connectedTcpSockets == null) {
			return;
		}
		if (_acceptedTcpSockets.contains(sockId)) {
			_acceptedTcpSockets.remove(sockId);
			_channelsTcpAcceptedOpenMeter.dec(1);
			_parent.getChannelsTcpAcceptedOpenMeter().dec(1);
			_channelsTcpAcceptedAbortedMeter.inc(1);
			_parent.getChannelsTcpAcceptedAbortedMeter().inc(1);
		} else if(_connectedTcpSockets.contains(sockId)){
			_connectedTcpSockets.remove(sockId);
			_channelsTcpConnectedOpenMeter.dec(1);
			_parent.getChannelsTcpConnectedOpenMeter().dec(1);
			_channelsTcpConnectedAbortedMeter.inc(1);
			_parent.getChannelsTcpConnectedAbortedMeter().inc(1);
		}
	}

	@Override
	public void sctpSocketConnected(int sockId, boolean fromClient) {
		if(_acceptedSctpSockets == null || _connectedSctpSockets == null) {
			return;
		}
		if (fromClient) {
			_channelsSctpAcceptedOpenMeter.inc(1);
			_parent.getChannelsSctpAcceptedOpenMeter().inc(1);
			_channelsSctpAcceptedMeter.inc(1);
			_parent.getChannelsSctpAcceptedMeter().inc(1);
			_acceptedSctpSockets.add(sockId);
		} else {
			_channelsSctpConnectedOpenMeter.inc(1);
			_parent.getChannelsSctpConnectedOpenMeter().inc(1);
			_channelsSctpConnectedMeter.inc(1);
			_parent.getChannelsSctpConnectedMeter().inc(1);
			_connectedSctpSockets.add(sockId);
		}
	}

	@Override
	public void sctpSocketClosed(int sockId) {
		if(_acceptedSctpSockets == null || _connectedSctpSockets == null) {
			return;
		}
		if (_acceptedSctpSockets.contains(sockId)) {
			_acceptedSctpSockets.remove(sockId);
			_channelsSctpAcceptedOpenMeter.dec(1);
			_parent.getChannelsSctpAcceptedOpenMeter().dec(1);
			_channelsSctpAcceptedClosedMeter.inc(1);
			_parent.getChannelsSctpAcceptedClosedMeter().inc(1);
		} else if(_connectedSctpSockets.contains(sockId)){
			_connectedSctpSockets.remove(sockId);
			_channelsSctpConnectedOpenMeter.dec(1);
			_parent.getChannelsSctpConnectedOpenMeter().dec(1);
			_channelsSctpConnectedClosedMeter.inc(1);
			_parent.getChannelsSctpConnectedClosedMeter().inc(1);
		}
	}

	@Override
	public void tcpSocketListening() {
		_listeningTcpMeter.inc(1);
		_parent.getListeningTcpMeter().inc(1);
	}

	@Override
	public void tcpSocketFailedConnect() {
		_channelsTcpFailedMeter.inc(1);
		_parent.getChannelsTcpFailedMeter().inc(1);
	}

	@Override
	public void sctpSocketFailedConnect() {
		_channelsSctpFailedMeter.inc(1);
		_parent.getChannelsSctpFailedMeter().inc(1);
	}

	@Override
	public void sctpSocketSendFailed() {
		_failedSendSctpMeter.inc(1);
		_parent.getFailedSendSctpMeter().inc(1);
	}

	@Override
	public void sctpPeerAddressChanged() {
		_sctpAddressChangeMeter.inc(1);
		_parent.getSctpAddressChangeMeter().inc(1);
	}

	@Override
	public void udpSocketBound() {
		_channelsUdpOpenMeter.inc(1);
		_parent.getChannelsUdpOpenMeter().inc(1);
	}

	@Override
	public void udpSocketFailedBind() {
		_channelsUdpFailedMeter.inc(1);
		_parent.getChannelsUdpFailedMeter().inc(1);
	}

	@Override
	public void udpSocketClosed() {
		_channelsUdpClosedMeter.inc(1);
		_parent.getChannelsUdpClosedMeter().inc(1);
	}

	@Override
	public void tcpSocketData(int len) {
		_readTcpMeter.inc(len);
		_parent.getReadTcpMeter().inc(len);
	}

	@Override
	public void sctpSocketData(int len) {
		_readSctpMeter.inc(len);
		_parent.getReadSctpMeter().inc(len);
	}

	@Override
	public void udpSocketData(int len) {
		_readUdpMeter.inc(len);
		_parent.getReadUdpMeter().inc(len);
	}

	@Override
	public void sendTcpSocketData(int remaining) {
		_sendTcpMeter.inc(remaining);
		_parent.getSendTcpMeter().inc(remaining);
	}

	@Override
	public void sendSctpSocketData(int remaining) {
		_sendSctpMeter.inc(remaining);
		_parent.getSendSctpMeter().inc(remaining);
	}

	@Override
	public void sendUdpSocketData(int remaining) {
		_sendUdpMeter.inc(remaining);
		_parent.getSendUdpMeter().inc(remaining);
	}

	@Override
	public void sctpSocketListening() {
		_listeningSctpMeter.inc(1);
		_parent.getListeningSctpMeter().inc(1);
	}
	
	@Override
	public void muxOpened(boolean local) {
		_parent.getIOHActiveMeter(local).inc(1);
	}
	
	@Override
	public void muxClosed(boolean local) {
		_parent.getIOHActiveMeter(local).dec(1);
		_parent.getIOHClosedMeter(local).inc(1);
	}
	
	@Override
	public void setMuxStarted(boolean local, boolean started) {
		if(started) {
			_muxStartMeter.inc(1);
		} else {
			_muxStopMeter.inc(1);
		}
		
		if(_muxStarted != null && started) {
			_parent.getIOHStoppedMeter(local).dec(1);
		} else if(!started) {
			_parent.getIOHStoppedMeter(local).inc(1);
		}		
		_muxStarted = started;
	}
}
