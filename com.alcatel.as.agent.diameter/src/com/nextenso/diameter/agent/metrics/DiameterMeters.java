package com.nextenso.diameter.agent.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.SimpleMonitorable;
import com.alcatel.as.service.metering2.util.Meters;
import com.nextenso.diameter.agent.impl.DiameterMessageFacade;
import com.nextenso.proxylet.diameter.DiameterMessage;
import com.nextenso.proxylet.diameter.DiameterRequest;
import com.nextenso.proxylet.diameter.DiameterResponse;

public abstract class DiameterMeters  {

	protected static enum Type {
			Capabilities, Watchdog, Disconnection, Application
		}

	protected SimpleMonitorable _mon;
	protected Map<Integer, Meter> _appMeters = new HashMap<>();
	protected Map<Long, Meter> _appRespMeters = new HashMap<>();
	protected Map<Integer, Boolean> _appRespActive;
	protected Meter _readMsgMeter = Meters.NULL_METER;
	protected Meter _readReqMeter = Meters.NULL_METER;
	protected Meter _readRespMeter = Meters.NULL_METER;
	protected Meter _readCERMeter = Meters.NULL_METER;
	protected Meter _readCEAMeter = Meters.NULL_METER;
	protected Meter _readDWRMeter = Meters.NULL_METER;
	protected Meter _readDWAMeter = Meters.NULL_METER;
	protected Meter _readDPRMeter = Meters.NULL_METER;
	protected Meter _readDPAMeter = Meters.NULL_METER;
	protected Meter _readAppReqMeter = Meters.NULL_METER;
	protected Meter _readAppRespMeter = Meters.NULL_METER;
	protected Meter _readCURMeter = Meters.NULL_METER;
	protected Meter _readCUAMeter = Meters.NULL_METER;
	protected Meter _writeMsgMeter = Meters.NULL_METER;
	protected Meter _writeReqMeter = Meters.NULL_METER;
	protected Meter _writeRespMeter = Meters.NULL_METER;
	protected Meter _writeCERMeter = Meters.NULL_METER;
	protected Meter _writeCEAMeter = Meters.NULL_METER;
	protected Meter _writeDWRMeter = Meters.NULL_METER;
	protected Meter _writeDPRMeter = Meters.NULL_METER;
	protected Meter _writeAppReqMeter = Meters.NULL_METER;
	protected Meter _writeAppRespMeter = Meters.NULL_METER;
	protected Meter _writeCURMeter = Meters.NULL_METER;
	protected Meter _writeCUAMeter = Meters.NULL_METER;
	protected Meter _parseFailedMeter = Meters.NULL_METER;
	protected Meter _writeDWAMeter = Meters.NULL_METER;
	protected Meter _writeDPAMeter = Meters.NULL_METER;
	
	protected Meter _readReqRateMeter = Meters.NULL_METER;
	protected Meter _writeReqRateMeter = Meters.NULL_METER;
	protected Meter _readRespRateMeter = Meters.NULL_METER;
	protected Meter _writeRespRateMeter = Meters.NULL_METER;
	
	public static boolean parseAppRespCounter(String s, Map<String, List<Integer>> store) {
		if (s == null)
			return false;
		int index = s.indexOf(':');
		if (index == -1 || index == 0 || index == (s.length() - 1))
			return false;
		String s1 = s.substring(0, index);
		String s2 = s.substring(index + 1);
		try {
			String name = s1;
			int result = Integer.parseInt(s2);
			List<Integer> list = store.get(name);
			if (list == null)
				store.put(name, list = new ArrayList<>());
			list.add(result);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	protected Type getType(DiameterMessage msg) {
		if(msg.getDiameterApplication() == 0L){
		    switch (msg.getDiameterCommand()){
		    case 257: return Type.Capabilities;
		    case 280: return Type.Watchdog; 
		    case 282: return Type.Disconnection;
		    default: return Type.Application; 
		    }
		} else if(msg.getDiameterApplication() == 10L) {
			switch(msg.getDiameterCommand()) {
		    case 328: return Type.Capabilities; // RFC 6737 CUR
		    default: return Type.Application;
		    }
		} else {
		    return Type.Application;
		}
	}
	
	protected void createMeters(MeteringService metering, SimpleMonitorable dest, boolean makeRates) {
		_readMsgMeter = dest.createIncrementalMeter(metering, "read.msg", null);
		_readReqMeter = dest.createIncrementalMeter(metering, "read.msg.req", _readMsgMeter);
		_readRespMeter = dest.createIncrementalMeter(metering, "read.msg.resp", _readMsgMeter);
		_readCERMeter = dest.createIncrementalMeter(metering, "read.msg.req.CER", _readReqMeter);
		_readCEAMeter = dest.createIncrementalMeter(metering, "read.msg.resp.CEA", _readRespMeter);
		_readCURMeter = dest.createIncrementalMeter(metering, "read.msg.req.CUR", _readReqMeter);
		_readCUAMeter = dest.createIncrementalMeter(metering, "read.msg.resp.CUA", _readRespMeter);
		//_readDWRMeter = dest.createIncrementalMeter(metering, "read.msg.req.DWR", _readReqMeter);
		//_readDWAMeter = dest.createIncrementalMeter(metering, "read.msg.resp.DWA", _readRespMeter);
		_readDPRMeter = dest.createIncrementalMeter(metering, "read.msg.req.DPR", _readReqMeter);
		_readDPAMeter = dest.createIncrementalMeter(metering, "read.msg.resp.DPA", _readRespMeter);
		_readAppReqMeter = dest.createIncrementalMeter(metering, "read.msg.req.App", _readReqMeter);
		_readAppRespMeter = dest.createIncrementalMeter(metering, "read.msg.resp.App", _readRespMeter);
		_writeMsgMeter = dest.createIncrementalMeter(metering, "write.msg", null);
		_writeReqMeter = dest.createIncrementalMeter(metering, "write.msg.req", _writeMsgMeter);
		_writeRespMeter = dest.createIncrementalMeter(metering, "write.msg.resp", _writeMsgMeter);
		_writeCERMeter = dest.createIncrementalMeter(metering, "write.msg.req.CER", _writeReqMeter);
		_writeCEAMeter = dest.createIncrementalMeter(metering, "write.msg.resp.CEA", _writeRespMeter);
		_writeCURMeter = dest.createIncrementalMeter(metering, "write.msg.req.CUR", _writeReqMeter);
		_writeCUAMeter = dest.createIncrementalMeter(metering, "write.msg.resp.CUA", _writeRespMeter);
		//_writeDWRMeter = dest.createIncrementalMeter(metering, "write.msg.req.DWR", _writeReqMeter);
		//_writeDWAMeter = dest.createIncrementalMeter(metering, "write.msg.resp.DWA", _writeRespMeter);
		_writeDPRMeter = dest.createIncrementalMeter(metering, "write.msg.req.DPR", _writeReqMeter);
		_writeDPAMeter = dest.createIncrementalMeter(metering, "write.msg.resp.DPA", _writeRespMeter);
		_writeAppReqMeter = dest.createIncrementalMeter(metering, "write.msg.req.App", _writeReqMeter);
		_writeAppRespMeter = dest.createIncrementalMeter(metering, "write.msg.resp.App", _writeRespMeter);
		
		_parseFailedMeter = dest.createIncrementalMeter(metering, "parser.error", null);

		if (makeRates) {
			_readReqRateMeter = Meters.createRateMeter(metering, _readReqMeter, 1000L);
			dest.addMeter(_readReqRateMeter);
			_readRespRateMeter = Meters.createRateMeter(metering, _readRespMeter, 1000L);
			dest.addMeter(_readRespRateMeter);
			_writeReqRateMeter = Meters.createRateMeter(metering, _writeReqMeter, 1000L);
			dest.addMeter(_writeReqRateMeter);
			_writeRespRateMeter = Meters.createRateMeter(metering, _writeRespMeter, 1000L);
			dest.addMeter(_writeRespRateMeter);
		}
	}
	
	public void incClientReadMeter(DiameterMessageFacade msg) {
		if(msg == null) {
			return;
		}
		
		if(msg.isRequest()) {
			incClientReadReqMeter(msg.getRequestFacade());
		} else {
			incClientReadRespMeter(msg.getResponseFacade());
		}
	}
	
	public void incClientWriteMeter(DiameterMessageFacade msg) {
		if(msg == null) {
			return;
		}
		
		if(msg.isRequest()) {
			incClientWriteReqMeter(msg.getRequestFacade());
		} else {
			incClientWriteRespMeter(msg.getResponseFacade());
		}
	}

	public abstract void socketClosed();
	
	public abstract void incParseFailedMeter();
	
	protected void incClientReadReqMeter(DiameterRequest msg) {
		switch (getType(msg)) {
		case Capabilities:
			if (msg.getDiameterApplication() == 0L) {
				incReadCERMeter();
			} else {
				incReadCURMeter();
			}
			return;
		case Watchdog:
			incReadDWRMeter();
			return;
		case Disconnection:
			incReadDPRMeter();
			return;
		case Application:
			incReadAppReqMeter(msg);
			return;
		}
	}
	
	protected void incClientReadRespMeter(DiameterResponse msg) {
		switch (getType(msg)) {
		case Capabilities:
			if (msg.getDiameterApplication() == 0L) {
				incReadCEAMeter();
			} else {
				incReadCUAMeter();
			}
			return;
		case Watchdog:
			incReadDWAMeter();
			return;
		case Disconnection:
			incReadDPAMeter();
			return;
		case Application:
			incReadAppRespMeter(msg);
			return;
		}
	}
	
	protected void incClientWriteReqMeter(DiameterRequest msg) {
		switch (getType(msg)) {
		case Capabilities:
			if (msg.getDiameterApplication() == 0L) {
				incWriteCERMeter();
			} else {
				incWriteCURMeter();
			}
			return;
		case Watchdog:
			incWriteDWRMeter();
			return;
		case Disconnection:
			incWriteDPRMeter();
			return;
		case Application:
			incSendAppReqMeter(msg);
			return;
		}
	}
	
	protected void incClientWriteRespMeter(DiameterResponse msg) {
		switch (getType(msg)) {
		case Capabilities:
			if (msg.getDiameterApplication() == 0L) {
				incWriteCEAMeter();
			} else {
				incWriteCUAMeter();
			}
			return;
		case Watchdog:
			incWriteDWAMeter();	
			return;
		case Disconnection:
			incWriteDPAMeter();
			return;
		case Application:
			incSendAppRespMeter(msg);
			//incSendAppRespMeterByResult(msg); // _parent is handled in it
			return;
		}
	}
	
	protected abstract void incReadCERMeter();
	
	protected abstract void incReadCEAMeter();
	
	protected abstract void incReadCURMeter();
	
	protected abstract void incReadCUAMeter();
	
	protected abstract void incReadDWRMeter();
	
	protected abstract void incReadDWAMeter();
	
	protected abstract void incReadDPRMeter();
	
	protected abstract void incReadDPAMeter();
	
	protected abstract void incWriteCERMeter();
	
	protected abstract void incWriteCEAMeter();
	
	protected abstract void incWriteCURMeter();
	
	protected abstract void incWriteCUAMeter();
	
	protected abstract void incWriteDWRMeter();
	
	protected abstract void incWriteDWAMeter();
	
	protected abstract void incWriteDPRMeter();
	
	protected abstract void incWriteDPAMeter();
	
	protected void incSendAppReqMeter(DiameterMessage msg) {
		Meter meter = _appMeters.get(msg.getDiameterCommand() | 0x01_00_00_00);
		meter = meter != null ? meter : _writeAppReqMeter;
		meter.inc(1);
	}

	protected void incSendAppRespMeter(DiameterMessage msg) {
		Meter meter = _appMeters.get(msg.getDiameterCommand() | 0x03_00_00_00);
		meter = meter != null ? meter : _writeAppRespMeter;
		meter.inc(1);
	}

	protected void incReadAppReqMeter(DiameterMessage msg) {
		Meter meter = _appMeters.get(msg.getDiameterCommand());
		meter = meter != null ? meter : _readAppReqMeter;
		meter.inc(1);
	}

	protected void incReadAppRespMeter(DiameterMessage msg) {
		Meter meter = _appMeters.get(msg.getDiameterCommand() | 0x02_00_00_00);
		meter = meter != null ? meter : _readAppRespMeter;
		meter.inc(1);
	}
}