package com.nextenso.mux.impl.ioh;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.SimpleMonitorable;
import com.alcatel.as.service.metering2.util.Meters;

public class MuxConnectionMetersImpl implements MuxConnectionMeters {

	protected SimpleMonitorable _mon;
	protected MeteringService _metering;

	protected Meter _muxStopMeter = Meters.NULL_METER;
	protected Meter _muxStartMeter = Meters.NULL_METER;
	protected Meter _listeningTcpMeter = Meters.NULL_METER, _sendTcpMeter = Meters.NULL_METER,
			_sendUdpMeter = Meters.NULL_METER, _readTcpMeter = Meters.NULL_METER, _readUdpMeter = Meters.NULL_METER,
			_channelsTcpOpenMeter = Meters.NULL_METER, _channelsTcpAcceptedOpenMeter = Meters.NULL_METER,
			_channelsTcpConnectedOpenMeter = Meters.NULL_METER, _channelsTcpClosedMeter = Meters.NULL_METER,
			_channelsTcpAcceptedClosedMeter = Meters.NULL_METER, _channelsTcpConnectedClosedMeter = Meters.NULL_METER,
			_channelsTcpAbortedMeter = Meters.NULL_METER, _channelsTcpAcceptedAbortedMeter = Meters.NULL_METER,
			_channelsTcpConnectedAbortedMeter = Meters.NULL_METER, _channelsTcpFailedMeter = Meters.NULL_METER,
			_channelsTcpAcceptedMeter = Meters.NULL_METER, _channelsTcpConnectedMeter = Meters.NULL_METER;
	protected Meter _channelsUdpOpenMeter = Meters.NULL_METER, _channelsUdpFailedMeter = Meters.NULL_METER,
			_channelsUdpClosedMeter = Meters.NULL_METER;
	protected Meter _channelsTcpConnectedDurationMeter = Meters.NULL_METER,
			_channelsTcpAcceptedDurationMeter = Meters.NULL_METER;
	protected Meter _failedSendSctpMeter = Meters.NULL_METER, _listeningSctpMeter = Meters.NULL_METER,
			_sendSctpMeter = Meters.NULL_METER, _readSctpMeter = Meters.NULL_METER,
			_channelsSctpOpenMeter = Meters.NULL_METER, _channelsSctpAcceptedOpenMeter = Meters.NULL_METER,
			_channelsSctpConnectedOpenMeter = Meters.NULL_METER, _channelsSctpClosedMeter = Meters.NULL_METER,
			_channelsSctpAcceptedClosedMeter = Meters.NULL_METER, _channelsSctpConnectedClosedMeter = Meters.NULL_METER,
			_channelsSctpFailedMeter = Meters.NULL_METER, _channelsSctpAcceptedMeter = Meters.NULL_METER,
			_channelsSctpConnectedMeter = Meters.NULL_METER;
	protected Meter _channelsSctpConnectedDurationMeter = Meters.NULL_METER,
			_channelsSctpAcceptedDurationMeter = Meters.NULL_METER;
	protected Meter _serversSctpOpenMeter = Meters.NULL_METER, _serversSctpFailedMeter = Meters.NULL_METER,
			_sctpAddressChangeMeter = Meters.NULL_METER;

	protected Meter _tcpAcceptedRateMeter = Meters.NULL_METER, _tcpConnectedRateMeter = Meters.NULL_METER,
			_sctpAcceptedRateMeter = Meters.NULL_METER, _sctpConnectedRateMeter = Meters.NULL_METER;

	protected Set<Integer> _connectedTcpSockets;
	protected Set<Integer> _acceptedTcpSockets;
	protected Set<Integer> _connectedSctpSockets;
	protected Set<Integer> _acceptedSctpSockets;

	public MuxConnectionMetersImpl(MeteringService metering, SimpleMonitorable monitorable) {
		_mon = monitorable;
		_metering = metering;
	}

	private Meter createIncrementalMeter(String name, Meter parent) {
		return _mon.createIncrementalMeter(_metering, name, parent);
	}
	
	private Meter createAbsoluteMeter(String name, Meter parent) {
		return _mon.createAbsoluteMeter(_metering, name);
	}

	public MuxConnectionMeters initMeters(String[] muxMeterConf) {
		boolean tcpCreated = false, udpCreated = false, sctpCreated = false;
		
		_mon.addMeter(Meters.createUptimeMeter(_metering));

		if (muxMeterConf == null) {
			setupTcpMeters();
			setupSctpMeters();
			setupUdpMeters();
		} else {
			for (String s : muxMeterConf) {
				switch (s.toLowerCase()) {
				case "udp":
					if (!udpCreated) {
						setupUdpMeters();
						udpCreated = true;
					}
					break;
				case "tcp":
					if (!tcpCreated) {
						setupTcpMeters();
						tcpCreated = true;
					}
					break;
				case "sctp":
					if (!sctpCreated) {
						setupSctpMeters();
						sctpCreated = true;
					}
					break;
				}
			}
		}
		return this;
	}

	@Override
	public void stop() {
		Meters.stopRateMeter(_tcpAcceptedRateMeter);
		Meters.stopRateMeter(_tcpConnectedRateMeter);
		Meters.stopRateMeter(_sctpAcceptedRateMeter);
		Meters.stopRateMeter(_sctpConnectedRateMeter);

		_mon.stop();
	}

	protected MuxConnectionMeters setupTcpMeters() {
		_acceptedTcpSockets = ConcurrentHashMap.newKeySet();
		_connectedTcpSockets = ConcurrentHashMap.newKeySet();

		_listeningTcpMeter = createIncrementalMeter("server.open.tcp", null);
		_sendTcpMeter = createIncrementalMeter("write.tcp", null);
		_readTcpMeter = createIncrementalMeter("read.tcp", null);
		_channelsTcpOpenMeter = createIncrementalMeter("channel.open.tcp", null);
		_channelsTcpAcceptedOpenMeter = createIncrementalMeter("channel.open.tcp.accept", _channelsTcpOpenMeter);
		_channelsTcpConnectedOpenMeter = createIncrementalMeter("channel.open.tcp.connect", _channelsTcpOpenMeter);

		_channelsTcpFailedMeter = createIncrementalMeter("channel.failed.tcp.connect", null);

		_channelsTcpClosedMeter = createIncrementalMeter("channel.closed.tcp", null);
		_channelsTcpAcceptedClosedMeter = createIncrementalMeter("channel.closed.tcp.accept", _channelsTcpClosedMeter);
		_channelsTcpConnectedClosedMeter = createIncrementalMeter("channel.closed.tcp.connect", _channelsTcpClosedMeter);

		//_channelsTcpAbortedMeter = createIncrementalMeter("channel.aborted.tcp", null);
		//_channelsTcpAcceptedAbortedMeter = createIncrementalMeter("channel.aborted.tcp.accept", _channelsTcpAbortedMeter);
		//_channelsTcpConnectedAbortedMeter= createIncrementalMeter("channel.aborted.tcp.connect",
		//		_channelsTcpAbortedMeter);

		_channelsTcpAcceptedMeter = createIncrementalMeter("channel.accepted.tcp", null);
		_channelsTcpConnectedMeter = createIncrementalMeter("channel.connected.tcp", null);

		_tcpAcceptedRateMeter = Meters.createRateMeter(_metering, _channelsTcpAcceptedMeter, 1000L);
		_tcpConnectedRateMeter = Meters.createRateMeter(_metering, _channelsTcpConnectedMeter, 1000L);
		
		_muxStopMeter = createIncrementalMeter("mux.stop", null);
		_muxStartMeter = createIncrementalMeter("mux.start", null);
		_mon.addMeter(_tcpAcceptedRateMeter);
		_mon.addMeter(_tcpConnectedRateMeter);

		return this;
	}

	protected MuxConnectionMeters setupSctpMeters() {
		_acceptedSctpSockets = ConcurrentHashMap.newKeySet();
		_connectedSctpSockets = ConcurrentHashMap.newKeySet();

		//_sctpAddressChangeMeter = createIncrementalMeter("server.address-change.sctp", null);

		_listeningSctpMeter = createIncrementalMeter("server.open.sctp", null);
		_sendSctpMeter = createIncrementalMeter("write.sctp", null);
		_readSctpMeter = createIncrementalMeter("read.sctp", null);
		//_failedSendSctpMeter = createIncrementalMeter("failed.sctp", null);
		_channelsSctpOpenMeter = createIncrementalMeter("channel.open.sctp", null);
		_channelsSctpAcceptedOpenMeter = createIncrementalMeter("channel.open.sctp.accept", _channelsSctpOpenMeter);
		_channelsSctpConnectedOpenMeter = createIncrementalMeter("channel.open.sctp.connect", _channelsSctpOpenMeter);

		_channelsSctpFailedMeter = createIncrementalMeter("channel.failed.sctp.connect", null);

		_channelsSctpClosedMeter = createIncrementalMeter("channel.closed.sctp", null);
		_channelsSctpAcceptedClosedMeter = createIncrementalMeter("channel.closed.sctp.accept", _channelsSctpClosedMeter);
		_channelsSctpConnectedClosedMeter = createIncrementalMeter("channel.closed.sctp.connect",
				_channelsSctpClosedMeter);

		_channelsSctpAcceptedMeter = createIncrementalMeter("channel.accepted.sctp", null);
		_channelsSctpConnectedMeter = createIncrementalMeter("channel.connected.sctp", null);

		_sctpAcceptedRateMeter = Meters.createRateMeter(_metering, _channelsSctpAcceptedMeter, 1000L);
		_sctpConnectedRateMeter = Meters.createRateMeter(_metering, _channelsSctpConnectedMeter, 1000L);
		_mon.addMeter(_sctpAcceptedRateMeter);
		_mon.addMeter(_sctpConnectedRateMeter);

		return this;
	}

	protected MuxConnectionMeters setupUdpMeters() {
		_sendUdpMeter = createIncrementalMeter("write.udp", null);
		_readUdpMeter = createIncrementalMeter("read.udp", null);
		_channelsUdpOpenMeter = createIncrementalMeter("channel.open.udp", null);
		_channelsUdpClosedMeter = createIncrementalMeter("channel.closed.udp", null);
		_channelsUdpClosedMeter = createIncrementalMeter("channel.failed.udp", null);

		return this;
	}

	@Override
	public void tcpSocketConnected(int sockId, boolean clientSocket) {
		if(_acceptedTcpSockets == null || _connectedTcpSockets == null) {
			return;
		}
		if (clientSocket) {
			_channelsTcpAcceptedOpenMeter.inc(1);
			_channelsTcpAcceptedMeter.inc(1);
			_acceptedTcpSockets.add(sockId);
		} else {
			_channelsTcpConnectedOpenMeter.inc(1);
			_channelsTcpConnectedMeter.inc(1);
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
			_channelsTcpAcceptedClosedMeter.inc(1);
		} else if(_connectedTcpSockets.contains(sockId)){
			_connectedTcpSockets.remove(sockId);
			_channelsTcpConnectedOpenMeter.dec(1);
			_channelsTcpConnectedClosedMeter.inc(1);
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
			_channelsTcpAcceptedAbortedMeter.inc(1);
		} else if(_connectedTcpSockets.contains(sockId)){
			_connectedTcpSockets.remove(sockId);
			_channelsTcpConnectedOpenMeter.dec(1);
			_channelsTcpConnectedAbortedMeter.inc(1);
		}
	}

	@Override
	public void sctpSocketConnected(int sockId, boolean fromClient) {
		if(_acceptedSctpSockets == null || _connectedSctpSockets == null) {
			return;
		}
		if (fromClient) {
			_channelsSctpAcceptedOpenMeter.inc(1);
			_channelsSctpAcceptedMeter.inc(1);
			_acceptedSctpSockets.add(sockId);
		} else {
			_channelsSctpConnectedOpenMeter.inc(1);
			_channelsSctpConnectedMeter.inc(1);
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
			_channelsSctpAcceptedClosedMeter.inc(1);
		} else if(_connectedSctpSockets.contains(sockId)){
			_connectedSctpSockets.remove(sockId);
			_channelsSctpAcceptedOpenMeter.dec(1);
			_channelsSctpConnectedClosedMeter.inc(1);
		}
	}

	@Override
	public void tcpSocketListening() {
		_listeningTcpMeter.inc(1);
	}

	@Override
	public void tcpSocketFailedConnect() {
		_channelsTcpFailedMeter.inc(1);
	}

	@Override
	public void sctpSocketFailedConnect() {
		_channelsSctpFailedMeter.inc(1);
	}

	@Override
	public void sctpSocketSendFailed() {
		_failedSendSctpMeter.inc(1);
	}

	@Override
	public void sctpPeerAddressChanged() {
		_sctpAddressChangeMeter.inc(1);
	}

	@Override
	public void udpSocketBound() {
		_channelsUdpOpenMeter.inc(1);
	}

	@Override
	public void udpSocketFailedBind() {
		_channelsUdpFailedMeter.inc(1);
	}

	@Override
	public void udpSocketClosed() {
		_channelsUdpClosedMeter.inc(1);
	}

	@Override
	public void tcpSocketData(int len) {
		_readTcpMeter.inc(len);
	}

	@Override
	public void sctpSocketData(int len) {
		_readSctpMeter.inc(len);
	}

	@Override
	public void udpSocketData(int len) {
		_readUdpMeter.inc(len);
	}

	@Override
	public void sendTcpSocketData(int remaining) {
		_sendTcpMeter.inc(remaining);
	}

	@Override
	public void sendSctpSocketData(int remaining) {
		_sendSctpMeter.inc(remaining);
	}

	@Override
	public void sendUdpSocketData(int remaining) {
		_sendUdpMeter.inc(remaining);
	}

	@Override
	public void sctpSocketListening() {
		_listeningSctpMeter.inc(1);
	}

	@Override
	public void setMuxStarted(boolean local, boolean started) {
		if(started) {
			_muxStartMeter.inc(1);
		} else {
			_muxStopMeter.inc(1);
		}
	}

}