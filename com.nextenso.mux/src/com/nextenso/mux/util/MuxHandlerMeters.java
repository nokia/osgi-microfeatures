package com.nextenso.mux.util;

import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.SimpleMonitorable;
import com.alcatel.as.service.metering2.util.Meters;

public class MuxHandlerMeters {
	
	protected MeteringService _metering;
	protected SimpleMonitorable _mon;
	
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
	protected Meter _iohActiveMeter = Meters.NULL_METER,
			_iohClosedMeter = Meters.NULL_METER, _iohLocalActiveMeter = Meters.NULL_METER, 
			_iohRemoteActiveMeter = Meters.NULL_METER, _iohStoppedMeter = Meters.NULL_METER,
			_iohLocalStoppedMeter = Meters.NULL_METER, _iohRemoteClosedMeter = Meters.NULL_METER,
			_iohRemoteStoppedMeter = Meters.NULL_METER;
	
	public MuxHandlerMeters(MeteringService ms, SimpleMonitorable mon) {
		this._mon = mon;
		this._metering = ms;
	}
	
	private Meter createIncrementalMeter(String name, Meter parent) {
		return _mon.createIncrementalMeter(_metering, name, parent);
	}
	
	public MuxHandlerMeters initMeters(String[] muxMeterConf) {
		boolean tcpCreated = false, udpCreated = false, sctpCreated = false;

		_mon.addMeter(Meters.createUptimeMeter(_metering));
		_iohActiveMeter = createIncrementalMeter("ioh", null);
		_iohLocalActiveMeter = createIncrementalMeter("ioh.local", _iohActiveMeter);
		_iohRemoteActiveMeter = createIncrementalMeter("ioh.remote", _iohActiveMeter);
		_iohStoppedMeter = createIncrementalMeter("ioh.stopped", null);
		_iohLocalStoppedMeter = createIncrementalMeter("ioh.local.stopped", _iohStoppedMeter);
		_iohRemoteStoppedMeter = createIncrementalMeter("ioh.remote.stopped", _iohStoppedMeter);
		
		_iohClosedMeter = createIncrementalMeter("ioh.closed", null);
		_iohRemoteClosedMeter = createIncrementalMeter("ioh.remote.closed", _iohClosedMeter);
		
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

	public void stop() {
		Meters.stopRateMeter(_tcpAcceptedRateMeter);
		Meters.stopRateMeter(_tcpConnectedRateMeter);
		Meters.stopRateMeter(_sctpAcceptedRateMeter);
		Meters.stopRateMeter(_sctpConnectedRateMeter);

		_mon.stop();
	}

	protected MuxHandlerMeters setupTcpMeters() {
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
		//_channelsTcpConnectedAbortedMeter = createIncrementalMeter("channel.aborted.tcp.connect",
		//		_channelsTcpAbortedMeter);

		_channelsTcpAcceptedMeter = createIncrementalMeter("channel.accepted.tcp", null);
		_channelsTcpConnectedMeter = createIncrementalMeter("channel.connected.tcp", null);

		_tcpAcceptedRateMeter = Meters.createRateMeter(_metering, _channelsTcpAcceptedMeter, 1000L);
		_tcpConnectedRateMeter = Meters.createRateMeter(_metering, _channelsTcpConnectedMeter, 1000L);
		_mon.addMeter(_tcpAcceptedRateMeter);
		_mon.addMeter(_tcpConnectedRateMeter);

		return this;
	}

	protected MuxHandlerMeters setupSctpMeters() {
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

	protected MuxHandlerMeters setupUdpMeters() {
		_sendUdpMeter = createIncrementalMeter("write.udp", null);
		_readUdpMeter = createIncrementalMeter("read.udp", null);
		_channelsUdpOpenMeter = createIncrementalMeter("channel.open.udp", null);
		_channelsUdpClosedMeter = createIncrementalMeter("channel.closed.udp", null);
		_channelsUdpFailedMeter = createIncrementalMeter("channel.failed.udp", null);

		return this;
	}
	
	public Meter getListeningTcpMeter() {
		return _listeningTcpMeter;
	}
	public Meter getSendTcpMeter() {
		return _sendTcpMeter;
	}
	public Meter getSendUdpMeter() {
		return _sendUdpMeter;
	}
	public Meter getReadTcpMeter() {
		return _readTcpMeter;
	}
	public Meter getReadUdpMeter() {
		return _readUdpMeter;
	}
	public Meter getChannelsTcpOpenMeter() {
		return _channelsTcpOpenMeter;
	}
	public Meter getChannelsTcpAcceptedOpenMeter() {
		return _channelsTcpAcceptedOpenMeter;
	}
	public Meter getChannelsTcpConnectedOpenMeter() {
		return _channelsTcpConnectedOpenMeter;
	}
	public Meter getChannelsTcpClosedMeter() {
		return _channelsTcpClosedMeter;
	}
	public Meter getChannelsTcpAcceptedClosedMeter() {
		return _channelsTcpAcceptedClosedMeter;
	}
	public Meter getChannelsTcpConnectedClosedMeter() {
		return _channelsTcpConnectedClosedMeter;
	}
	public Meter getChannelsTcpAbortedMeter() {
		return _channelsTcpAbortedMeter;
	}
	public Meter getChannelsTcpAcceptedAbortedMeter() {
		return _channelsTcpAcceptedAbortedMeter;
	}
	public Meter getChannelsTcpConnectedAbortedMeter() {
		return _channelsTcpConnectedAbortedMeter;
	}
	public Meter getChannelsTcpFailedMeter() {
		return _channelsTcpFailedMeter;
	}
	public Meter getChannelsTcpAcceptedMeter() {
		return _channelsTcpAcceptedMeter;
	}
	public Meter getChannelsTcpConnectedMeter() {
		return _channelsTcpConnectedMeter;
	}
	public Meter getChannelsUdpOpenMeter() {
		return _channelsUdpOpenMeter;
	}
	public Meter getChannelsUdpFailedMeter() {
		return _channelsUdpFailedMeter;
	}
	public Meter getChannelsUdpClosedMeter() {
		return _channelsUdpClosedMeter;
	}
	public Meter getChannelsTcpConnectedDurationMeter() {
		return _channelsTcpConnectedDurationMeter;
	}
	public Meter getChannelsTcpAcceptedDurationMeter() {
		return _channelsTcpAcceptedDurationMeter;
	}
	public Meter getFailedSendSctpMeter() {
		return _failedSendSctpMeter;
	}
	public Meter getListeningSctpMeter() {
		return _listeningSctpMeter;
	}
	public Meter getSendSctpMeter() {
		return _sendSctpMeter;
	}
	public Meter getReadSctpMeter() {
		return _readSctpMeter;
	}
	public Meter getChannelsSctpOpenMeter() {
		return _channelsSctpOpenMeter;
	}
	public Meter getChannelsSctpAcceptedOpenMeter() {
		return _channelsSctpAcceptedOpenMeter;
	}
	public Meter getChannelsSctpConnectedOpenMeter() {
		return _channelsSctpConnectedOpenMeter;
	}
	public Meter getChannelsSctpClosedMeter() {
		return _channelsSctpClosedMeter;
	}
	public Meter getChannelsSctpAcceptedClosedMeter() {
		return _channelsSctpAcceptedClosedMeter;
	}
	public Meter getChannelsSctpConnectedClosedMeter() {
		return _channelsSctpConnectedClosedMeter;
	}
	public Meter getChannelsSctpFailedMeter() {
		return _channelsSctpFailedMeter;
	}
	public Meter getChannelsSctpAcceptedMeter() {
		return _channelsSctpAcceptedMeter;
	}
	public Meter getChannelsSctpConnectedMeter() {
		return _channelsSctpConnectedMeter;
	}
	public Meter getChannelsSctpConnectedDurationMeter() {
		return _channelsSctpConnectedDurationMeter;
	}
	public Meter getChannelsSctpAcceptedDurationMeter() {
		return _channelsSctpAcceptedDurationMeter;
	}
	public Meter getServersSctpOpenMeter() {
		return _serversSctpOpenMeter;
	}
	public Meter getServersSctpFailedMeter() {
		return _serversSctpFailedMeter;
	}
	public Meter getSctpAddressChangeMeter() {
		return _sctpAddressChangeMeter;
	}
	public Meter getSctpAcceptedRateMeter() {
		return _sctpAcceptedRateMeter;
	}
	public Meter getSctpConnectedRateMeter() {
		return _sctpConnectedRateMeter;
	}
	
	public Meter getIOHActiveMeter(boolean local) {
		return local ? _iohLocalActiveMeter : _iohRemoteActiveMeter;
	}
	
	public Meter getIOHClosedMeter(boolean local) {
		return local ? Meters.NULL_METER : _iohRemoteClosedMeter;
	}
	
	public Meter getIOHStoppedMeter(boolean local) {
		return local ? _iohLocalStoppedMeter : _iohRemoteStoppedMeter;
	}
	
	public SimpleMonitorable getMonitorable() {
		return _mon;
	}
}
