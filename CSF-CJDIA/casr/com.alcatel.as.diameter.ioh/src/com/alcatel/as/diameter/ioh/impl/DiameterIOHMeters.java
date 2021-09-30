package com.alcatel.as.diameter.ioh.impl;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Future;
import org.apache.log4j.Logger;

import com.alcatel.as.ioh.engine.IOHMeters;

import com.alcatel.as.diameter.parser.DiameterMessage;

import com.alcatel.as.service.concurrent.*;
import alcatel.tess.hometop.gateways.reactor.*;

import org.osgi.framework.*;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.util.*;

public class DiameterIOHMeters extends SimpleMonitorable {

    protected Map<Integer, Meter> _appMeters = new HashMap<> ();
    protected Map<Long, Meter> _appRespMeters = new HashMap<> ();
    protected Map<Integer, Boolean> _appRespActive;
    protected Meter _readMsgMeter, _readReqMeter, _readRespMeter;
    protected Meter _readCERMeter, _readCEAMeter, _readDWRMeter, _readDWAMeter, _readDPRMeter, _readDPAMeter, _readAppReqMeter, _readAppRespMeter, _readCURMeter, _readCUAMeter;
    protected Meter _writeMsgMeter, _writeReqMeter, _writeRespMeter;
    protected Meter _writeCERMeter, _writeCEAMeter, _writeDWRMeter, _writeDWAMeter, _writeDPRMeter, _writeDPAMeter, _writeAppReqMeter, _writeAppRespMeter, _writeCURMeter, _writeCUAMeter;
    protected Meter _readDefAppReqMeter, _readDefAppRespMeter, _writeDefAppReqMeter, _writeDefAppRespMeter; // they are pitfalls if code 0 is assigned to a command

    protected Meter _errorNoRemoteReadCER;

    protected AtomicLong _txProcessingMeter, _reqProcessingMeter, _respProcessingMeter;

    protected DiameterIOHMeters _parent;

    public DiameterIOHMeters (String name, String desc){
	super (name, desc);
    }
    
    public void initClient (MeteringService metering, DiameterIOHMeters parent){
	createMeters (metering, this, true);
	addMeter (Meters.createUptimeMeter (metering));
	_parent = parent;
    }
    public void initDiameterIOHEngine (IOHMeters meters){
	createMeters (meters.getMetering (), meters, true);
	_errorNoRemoteReadCER = meters.createIncrementalMeter (meters.getMetering (), "error.noremote.read.cer", null);
    }
    public void initDiameterMuxClient (IOHMeters meters){
	createMeters (meters.getMetering (), meters, false);
    }
    public void initLatencyMeters (IOHMeters meters){
	createLatencyMeters (meters.getMetering (), meters);
    }
    public void addAppCounter (MeteringService metering, SimpleMonitorable dest, int code, String nameR, String nameA, Map<String, List<Integer>> respResults){
	if (code == 0){
	    // pitfall case
	    _readDefAppReqMeter = dest.createIncrementalMeter (metering, "read.msg.req.App."+nameR, _readAppReqMeter);
	    _readDefAppRespMeter = dest.createIncrementalMeter (metering, "read.msg.resp.App."+nameA, _readAppRespMeter);
	    _writeDefAppReqMeter = dest.createIncrementalMeter (metering, "write.msg.req.App."+nameR, _writeAppReqMeter);
	    _writeDefAppRespMeter = dest.createIncrementalMeter (metering, "write.msg.resp.App."+nameA, _writeAppRespMeter);
	} else {
	    // code is over 3 bytes : 1 byte on the left for read/write req/resp
	    // read = 0x00_00_00_00
	    // write = 0x01_00_00_00
	    // req = 0x00_00_00_00
	    // resp = 0x02_00_00_00
	    _appMeters.put (code, dest.createIncrementalMeter (metering, "read.msg.req.App."+nameR, _readAppReqMeter));
	    _appMeters.put (code | 0x02_00_00_00, dest.createIncrementalMeter (metering, "read.msg.resp.App."+nameA, _readAppRespMeter));
	    _appMeters.put (code | 0x01_00_00_00, dest.createIncrementalMeter (metering, "write.msg.req.App."+nameR, _writeAppReqMeter));
	    _appMeters.put (code | 0x03_00_00_00, dest.createIncrementalMeter (metering, "write.msg.resp.App."+nameA, _writeAppRespMeter));
	}

	List<Integer> list = respResults.get (nameR);
	if (list == null) list = respResults.get (nameA);
	if (list != null){
	    long codel = (long) code;
	    if (_appRespActive == null) _appRespActive = new HashMap<> ();
	    _appRespActive.put (code, true);
	    for (int result : list){
		long resultl = ((long)result) << 32;
		_appRespMeters.put (resultl | codel | 0x02_00_00_00L, dest.createIncrementalMeter (metering, "read.msg.resp.App."+nameA+"."+result, null));
		_appRespMeters.put (resultl | codel | 0x03_00_00_00L, dest.createIncrementalMeter (metering, "write.msg.resp.App."+nameA+"."+result, null));
	    }
	}
    }
    
    private void createMeters (MeteringService metering, SimpleMonitorable dest, boolean makeRates){
	_readMsgMeter = dest.createIncrementalMeter (metering, "read.msg", null);
	_readReqMeter = dest.createIncrementalMeter (metering, "read.msg.req", _readMsgMeter);
	_readRespMeter = dest.createIncrementalMeter (metering, "read.msg.resp", _readMsgMeter);
	_readCERMeter = dest.createIncrementalMeter (metering, "read.msg.req.CER", _readReqMeter);
	_readCEAMeter = dest.createIncrementalMeter (metering, "read.msg.resp.CEA", _readRespMeter);
	_readCURMeter = dest.createIncrementalMeter (metering, "read.msg.req.CUR", _readReqMeter);
	_readCUAMeter = dest.createIncrementalMeter (metering, "read.msg.resp.CUA", _readRespMeter);
	_readDWRMeter = dest.createIncrementalMeter (metering, "read.msg.req.DWR", _readReqMeter);
	_readDWAMeter = dest.createIncrementalMeter (metering, "read.msg.resp.DWA", _readRespMeter);
	_readDPRMeter = dest.createIncrementalMeter (metering, "read.msg.req.DPR", _readReqMeter);
	_readDPAMeter = dest.createIncrementalMeter (metering, "read.msg.resp.DPA", _readRespMeter);
	_readAppReqMeter = dest.createIncrementalMeter (metering, "read.msg.req.App", _readReqMeter);
	_readAppRespMeter = dest.createIncrementalMeter (metering, "read.msg.resp.App", _readRespMeter);
	_writeMsgMeter = dest.createIncrementalMeter (metering, "write.msg", null);
	_writeReqMeter = dest.createIncrementalMeter (metering, "write.msg.req", _writeMsgMeter);
	_writeRespMeter = dest.createIncrementalMeter (metering, "write.msg.resp", _writeMsgMeter);
	_writeCERMeter = dest.createIncrementalMeter (metering, "write.msg.req.CER", _writeReqMeter);
	_writeCEAMeter = dest.createIncrementalMeter (metering, "write.msg.resp.CEA", _writeRespMeter);
	_writeCURMeter = dest.createIncrementalMeter (metering, "write.msg.req.CUR", _writeReqMeter);
	_writeCUAMeter = dest.createIncrementalMeter (metering, "write.msg.resp.CUA", _writeRespMeter);
	_writeDWRMeter = dest.createIncrementalMeter (metering, "write.msg.req.DWR", _writeReqMeter);
	_writeDWAMeter = dest.createIncrementalMeter (metering, "write.msg.resp.DWA", _writeRespMeter);
	_writeDPRMeter = dest.createIncrementalMeter (metering, "write.msg.req.DPR", _writeReqMeter);
	_writeDPAMeter = dest.createIncrementalMeter (metering, "write.msg.resp.DPA", _writeRespMeter);
	_writeAppReqMeter = dest.createIncrementalMeter (metering, "write.msg.req.App", _writeReqMeter);
	_writeAppRespMeter = dest.createIncrementalMeter (metering, "write.msg.resp.App", _writeRespMeter);

	if (makeRates){
	    dest.addMeter (Meters.createRateMeter (metering, _readReqMeter, 1000L));
	    dest.addMeter (Meters.createRateMeter (metering, _readRespMeter, 1000L));
	    dest.addMeter (Meters.createRateMeter (metering, _writeReqMeter, 1000L));
	    dest.addMeter (Meters.createRateMeter (metering, _writeRespMeter, 1000L));
	}

	_readDefAppReqMeter = _readAppReqMeter;
	_readDefAppRespMeter = _readAppRespMeter;
	_writeDefAppReqMeter = _writeAppReqMeter;
	_writeDefAppRespMeter = _writeAppRespMeter;
    }

    private void createLatencyMeters (MeteringService metering, SimpleMonitorable dest){
	_reqProcessingMeter = new AtomicLong ();
	_respProcessingMeter = new AtomicLong ();
	_txProcessingMeter = new AtomicLong ();
	dest.createValueSuppliedMeter (metering, "latency.proc.req", this::getReqProcessingMeter);
	dest.createValueSuppliedMeter (metering, "latency.proc.resp", this::getRespProcessingMeter);
	dest.createValueSuppliedMeter (metering, "latency.proc.tx", this::getTxProcessingMeter);
    }
    
    public long getReqProcessingMeter (){ return _reqProcessingMeter.get () / 10L;}
    public long getRespProcessingMeter (){ return _respProcessingMeter.get () / 10L;}
    public long getTxProcessingMeter (){ return _txProcessingMeter.get () / 10L;}

    public Meter getSendAppReqMeter (DiameterMessage msg){
	Meter meter = _appMeters.get (msg.getCommandCode () | 0x01_00_00_00);
	return meter != null ? meter : _writeDefAppReqMeter;
    }
    public Meter getSendAppRespMeter (DiameterMessage msg){
	Meter meter = _appMeters.get (msg.getCommandCode () | 0x03_00_00_00);
	return meter != null ? meter : _writeDefAppRespMeter;
    }
    public Meter getReadAppReqMeter (DiameterMessage msg){
	Meter meter = _appMeters.get (msg.getCommandCode ());
	return meter != null ? meter : _readDefAppReqMeter;
    }
    public Meter getReadAppRespMeter (DiameterMessage msg){
	Meter meter = _appMeters.get (msg.getCommandCode () | 0x02_00_00_00);
	return meter != null ? meter : _readDefAppRespMeter;
    }
    public void incSendAppRespMeterByResult (DiameterMessage msg){
	if (_appRespActive == null) return;
	int code = msg.getCommandCode ();
	if (_appRespActive.get (code) == null){
	    code = 0;
	    if (_appRespActive.get (code) == null) return;
	}
	int result = msg.getResultCode ();
	if (result == -1) return;
	long resultl = ((long) result) << 32;
	long key = resultl | ((long)code) | 0x03_00_00_00L;
	Meter meter = _appRespMeters.get (key);
	if (meter == null){ // 9999 code as pitfall
	    key = (9999L << 32) | ((long)code) | 0x03_00_00_00L;
	    meter = _appRespMeters.get (key);
	}
	if (meter != null){
	    meter.inc (1);
	    if (_parent != null){
		meter = _parent._appRespMeters.get (key);
		meter.inc (1);
	    }
	}
    }
    public void incReadAppRespMeterByResult (DiameterMessage msg){
	if (_appRespActive == null) return;
	int code = msg.getCommandCode ();
	if (_appRespActive.get (code) == null){
	    code = 0;
	    if (_appRespActive.get (code) == null) return;
	}
	int result = msg.getResultCode ();
	if (result == -1) return;
	long resultl = ((long) result) << 32;
	long key = resultl | ((long)code) | 0x02_00_00_00L;
	Meter meter = _appRespMeters.get (key);
	if (meter == null){ // 9999 code as pitfall
	    key = (9999L << 32) | ((long)code) | 0x02_00_00_00L;
	    meter = _appRespMeters.get (key);
	}
	if (meter != null){
	    meter.inc (1);
	    if (_parent != null){
		meter = _parent._appRespMeters.get (key);
		meter.inc (1);
	    }
	}
    }
    
    public Meter getSendMeter (DiameterMessage msg){
	switch (msg.getType ()){
	case Capabilities :
	    if (msg.getApplicationID () == 0L)
		return msg.isRequest () ? _writeCERMeter : _writeCEAMeter;
	    else
		return msg.isRequest () ? _writeCURMeter : _writeCUAMeter;
	case Watchdog : return msg.isRequest () ? _writeDWRMeter : _writeDWAMeter;
	case Disconnection : return msg.isRequest () ? _writeDPRMeter : _writeDPAMeter;
	case Application : return msg.isRequest () ? getSendAppReqMeter (msg) : getSendAppRespMeter (msg);
	}
	return null;
    }
    public Meter getReadMeter (DiameterMessage msg){
	switch (msg.getType ()){
	case Capabilities :
	    if (msg.getApplicationID () == 0L)
		return msg.isRequest () ? _readCERMeter : _readCEAMeter;
	    else
		return msg.isRequest () ? _readCURMeter : _readCUAMeter;
	case Watchdog : return msg.isRequest () ? _readDWRMeter : _readDWAMeter;
	case Disconnection : return msg.isRequest () ? _readDPRMeter : _readDPAMeter;
	case Application : return msg.isRequest () ? getReadAppReqMeter (msg) : getReadAppRespMeter (msg);
	}
	return null;
    }
    public void incClientSendMeter (DiameterMessage msg){
	switch (msg.getType ()){
	case Capabilities :
	    if (msg.isRequest ()){
		if (msg.getApplicationID () == 0L){
		    _writeCERMeter.inc (1);
		    _parent._writeCERMeter.inc (1);
		} else {
		    _writeCURMeter.inc (1);
		    _parent._writeCURMeter.inc (1);
		}
	    } else {
		if (msg.getApplicationID () == 0L){
		    _writeCEAMeter.inc (1);
		    _parent._writeCEAMeter.inc (1);
		} else {
		    _writeCUAMeter.inc (1);
		    _parent._writeCUAMeter.inc (1);
		}
	    }
	    return;
	case Watchdog :
	    if (msg.isRequest ()){
		_writeDWRMeter.inc (1);
		_parent._writeDWRMeter.inc (1);
	    } else {
		_writeDWAMeter.inc (1);
		_parent._writeDWAMeter.inc (1);
	    }
	    return;
	case Disconnection :
	    if (msg.isRequest ()){
		_writeDPRMeter.inc (1);
		_parent._writeDPRMeter.inc (1);
	    } else {
		_writeDPAMeter.inc (1);
		_parent._writeDPAMeter.inc (1);
	    }
	    return;
	case Application :
	    if (msg.isRequest ()){
		getSendAppReqMeter (msg).inc (1);
		_parent.getSendAppReqMeter (msg).inc (1);
	    } else {
		getSendAppRespMeter (msg).inc (1);
		_parent.getSendAppRespMeter (msg).inc (1);
		incSendAppRespMeterByResult (msg); // _parent is handled in it
	    }
	    return;
	}
    }

    // not thread safe !
    public void calcMessageLatency (DiameterMessage msg){
	long ts1 = msg.timestamp1 ();
	//if (ts1 == 0L) return; normally already tested in DiameterIOHEngine before calling this
	if (msg.isRequest ()){
	    // this is a proxied req - else ts1 = 0
	    long now = System.currentTimeMillis ();
	    long elapsed = 10*(now - ts1);
	    long value = _reqProcessingMeter.get ();
	    long newValue = value + (elapsed >> 3) - (value >> 3);
	    _reqProcessingMeter.set (newValue);
	} else {
	    long now = System.currentTimeMillis ();
	    long elapsed = 10*(now - ts1);
	    long value = _txProcessingMeter.get ();
	    long newValue = value + (elapsed >> 3) - (value >> 3);
	    _txProcessingMeter.set (newValue);
	    long ts2 = msg.timestamp2 ();
	    if (ts2 == 0L){
		// response generated by us
	    } else {
		// response received from downstream server
		elapsed = 10*(now - ts2);
		value = _respProcessingMeter.get ();
		newValue = value + (elapsed >> 3) - (value >> 3);
		_respProcessingMeter.set (newValue);
	    }
	}
    }

    // expected format examples:
    // <property name="diameter.ioh.meter.app">TDR:8388734</property>
    //  read.msg.req.App.TDR
    //	read.msg.resp.App.TDA
    //	write.msg.req.App.TDR
    //	write.msg.resp.App.TDA
    // code 0 is used as pitfall: example : <property name="diameter.ioh.meter.app">XXR:0</property>
    // <property name="diameter.ioh.meter.app">TD:8388734</property> // not compatible with meters by result codes
    //  read.msg.req.App.TD
    //	read.msg.resp.App.TD
    //	write.msg.req.App.TD
    //	write.msg.resp.App.TD
    public static Object[] parseAppCounter (String s){
	if (s == null) return null;
	int index = s.indexOf (':');
	if (index == -1 || index == 0 || index == (s.length () -1)) return null;
	String s1 = s.substring (0, index);
	String s2 = s.substring (index+1);
	try{
	    int code = Integer.parseInt (s1);
	    String nameR = s2;
	    String nameA = nameR.endsWith ("R") ? nameR.substring (0, nameR.length () - 1)+"A" : nameR;
	    return new Object[]{code, nameR, nameA};
	}catch(Exception e){
	    try{
		int code = Integer.parseInt (s2);
		String nameR = s1;
		String nameA = nameR.endsWith ("R") ? nameR.substring (0, nameR.length () - 1)+"A" : nameR;
		return new Object[]{code, nameR, nameA};
	    }catch(Exception ee){
		return null;
	    }
	}
    }
    // <property name="diameter.ioh.meter.app.resp">TDA:2001</property>
    // TDA:0 will create all result code meters, plus 9999 as pitfall
    public static boolean parseAppRespCounter (String s, Map<String, List<Integer>> store){
	if (s == null) return false;
	int index = s.indexOf (':');
	if (index == -1 || index == 0 || index == (s.length () -1)) return false;
	String s1 = s.substring (0, index);
	String s2 = s.substring (index+1);
	boolean allresults = s2.equals ("0");
	if (allresults){
	    String name = s1;
	    int[] results = new int[]{
		1001,
		2001, 2002,
		3001, 3002, 3003, 3004, 3005, 3006, 3007, 3008, 3009, 3010,
		4001, 4002, 4003,
		5001, 5002, 5003, 5004, 5005, 5006, 5007, 5008, 5009, 5010, 5011, 5012, 5013, 5014, 5015, 5016, 5017,
		9999
	    };
	    List<Integer> list = new ArrayList<> ();
	    store.put (name, list);
	    for (int result : results){
		list.add (result);
	    }
	    return true;
	}
	try{
	    String name = s1;
	    int result = Integer.parseInt (s2);
	    List<Integer> list = store.get (name);
	    if (list == null) store.put (name, list = new ArrayList<> ());
	    list.add (result);
	    return true;
	}catch(Exception e){
	    return false;
	}
    }
}
