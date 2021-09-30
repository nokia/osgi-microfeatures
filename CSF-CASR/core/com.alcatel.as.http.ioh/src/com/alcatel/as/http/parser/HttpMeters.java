package com.alcatel.as.http.parser;

import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.SimpleMonitorable;
import com.alcatel.as.service.metering2.ValueSupplier;

public class HttpMeters extends SimpleMonitorable {
    
    protected MeteringService _metering;

    protected Meter _channelsTcpOpenMeter, _channelsTcpClosedMeter;
    protected Meter _readReqsMeter, _readReqsRateMeter;
    protected Meter _readGETMeter, _readPOSTMeter, _readPUTMeter, _readDELETEMeter, _readPATCHMeter,
	_readHEADMeter, _readOPTIONSMeter, _readOTHERMeter;
    protected Meter _parserErrorMeter;

    protected Meter _writeRespMeter, _resp100Meter, _resp101Meter, _resp200Meter, _resp201Meter, _resp202Meter, _resp203Meter, _resp204Meter, _resp205Meter, _resp206Meter, _resp300Meter, _resp301Meter, _resp302Meter, _resp303Meter, _resp304Meter, _resp305Meter, _resp306Meter, _resp307Meter, _resp308Meter, _resp310Meter, _resp400Meter, _resp401Meter, _resp402Meter, _resp403Meter, _resp404Meter, _resp405Meter, _resp406Meter, _resp407Meter, _resp408Meter, _resp409Meter, _resp410Meter, _resp411Meter, _resp412Meter, _resp413Meter, _resp414Meter, _resp415Meter, _resp416Meter, _resp417Meter, _resp426Meter, _resp428Meter, _resp429Meter, _resp431Meter, _resp500Meter, _resp501Meter, _resp502Meter, _resp503Meter, _resp504Meter, _resp505Meter, _resp506Meter, _resp509Meter, _resp510Meter, _resp520Meter, _resp999Meter;


    public HttpMeters(String name, String desc, MeteringService metering) {
	super(name, desc);
	_metering = metering;
    }

    public Meter createIncrementalMeter(String name, Meter parent) {
	return createIncrementalMeter(_metering, name, parent);
    }

    public Meter createAbsoluteMeter(String name) {
	return createAbsoluteMeter(_metering, name);
    }

    public Meter createValueSuppliedMeter(String name, ValueSupplier supplier) {
	return createValueSuppliedMeter(_metering, name, supplier);
    }

    public Meter createRateMeter(Meter target) {
	Meter rate = com.alcatel.as.service.metering2.util.Meters.createRateMeter(_metering, target, 1000);
	addMeter(rate);
	return rate;
    }

    public void stopRateMeter(Meter meter) {
	com.alcatel.as.service.metering2.util.Meters.stopRateMeter(meter);
    }

    public HttpMeters init(String prefix) {
	if (prefix == null) prefix = "";
	else if (!prefix.endsWith (".")) prefix = prefix + ".";
	
	_channelsTcpOpenMeter = createIncrementalMeter(prefix+"channel.open", null);
	_channelsTcpClosedMeter = createIncrementalMeter(prefix+"channel.closed", null);
		
	_readReqsMeter = createIncrementalMeter(prefix+"read.req", null);
	_readReqsRateMeter = createRateMeter(_readReqsMeter);

	_readGETMeter = createIncrementalMeter(prefix+"read.req.GET", _readReqsMeter);
	_readPUTMeter = createIncrementalMeter(prefix+"read.req.PUT", _readReqsMeter);
	_readPOSTMeter = createIncrementalMeter(prefix+"read.req.POST", _readReqsMeter);
	_readDELETEMeter = createIncrementalMeter(prefix+"read.req.DELETE", _readReqsMeter);
	_readHEADMeter = createIncrementalMeter(prefix+"read.req.HEAD", _readReqsMeter);
	_readPATCHMeter = createIncrementalMeter(prefix+"read.req.PATCH", _readReqsMeter);
	_readOPTIONSMeter = createIncrementalMeter(prefix+"read.req.OPTIONS", _readReqsMeter);
	_readOTHERMeter = createIncrementalMeter(prefix+"read.req.OTHER", _readReqsMeter);

	_parserErrorMeter = createIncrementalMeter(prefix+"failed.parser", null);

	_writeRespMeter = createIncrementalMeter(prefix+"write.resp", null);
	_resp100Meter = createIncrementalMeter(prefix+"write.resp.100", _writeRespMeter);
	_resp101Meter = createIncrementalMeter(prefix+"write.resp.101", _writeRespMeter);
	_resp200Meter = createIncrementalMeter(prefix+"write.resp.200", _writeRespMeter);
	_resp201Meter = createIncrementalMeter(prefix+"write.resp.201", _writeRespMeter);
	_resp202Meter = createIncrementalMeter(prefix+"write.resp.202", _writeRespMeter);
	_resp203Meter = createIncrementalMeter(prefix+"write.resp.203", _writeRespMeter);
	_resp204Meter = createIncrementalMeter(prefix+"write.resp.204", _writeRespMeter);
	_resp205Meter = createIncrementalMeter(prefix+"write.resp.205", _writeRespMeter);
	_resp206Meter = createIncrementalMeter(prefix+"write.resp.206", _writeRespMeter);
	_resp300Meter = createIncrementalMeter(prefix+"write.resp.300", _writeRespMeter);
	_resp301Meter = createIncrementalMeter(prefix+"write.resp.301", _writeRespMeter);
	_resp302Meter = createIncrementalMeter(prefix+"write.resp.302", _writeRespMeter);
	_resp303Meter = createIncrementalMeter(prefix+"write.resp.303", _writeRespMeter);
	_resp304Meter = createIncrementalMeter(prefix+"write.resp.304", _writeRespMeter);
	_resp305Meter = createIncrementalMeter(prefix+"write.resp.305", _writeRespMeter);
	_resp306Meter = createIncrementalMeter(prefix+"write.resp.306", _writeRespMeter);
	_resp307Meter = createIncrementalMeter(prefix+"write.resp.307", _writeRespMeter);
	_resp308Meter = createIncrementalMeter(prefix+"write.resp.308", _writeRespMeter);
	_resp310Meter = createIncrementalMeter(prefix+"write.resp.310", _writeRespMeter);
	_resp400Meter = createIncrementalMeter(prefix+"write.resp.400", _writeRespMeter);
	_resp401Meter = createIncrementalMeter(prefix+"write.resp.401", _writeRespMeter);
	_resp402Meter = createIncrementalMeter(prefix+"write.resp.402", _writeRespMeter);
	_resp403Meter = createIncrementalMeter(prefix+"write.resp.403", _writeRespMeter);
	_resp404Meter = createIncrementalMeter(prefix+"write.resp.404", _writeRespMeter);
	_resp405Meter = createIncrementalMeter(prefix+"write.resp.405", _writeRespMeter);
	_resp406Meter = createIncrementalMeter(prefix+"write.resp.406", _writeRespMeter);
	_resp407Meter = createIncrementalMeter(prefix+"write.resp.407", _writeRespMeter);
	_resp408Meter = createIncrementalMeter(prefix+"write.resp.408", _writeRespMeter);
	_resp409Meter = createIncrementalMeter(prefix+"write.resp.409", _writeRespMeter);
	_resp410Meter = createIncrementalMeter(prefix+"write.resp.410", _writeRespMeter);
	_resp411Meter = createIncrementalMeter(prefix+"write.resp.411", _writeRespMeter);
	_resp412Meter = createIncrementalMeter(prefix+"write.resp.412", _writeRespMeter);
	_resp413Meter = createIncrementalMeter(prefix+"write.resp.413", _writeRespMeter);
	_resp414Meter = createIncrementalMeter(prefix+"write.resp.414", _writeRespMeter);
	_resp415Meter = createIncrementalMeter(prefix+"write.resp.415", _writeRespMeter);
	_resp416Meter = createIncrementalMeter(prefix+"write.resp.416", _writeRespMeter);
	_resp417Meter = createIncrementalMeter(prefix+"write.resp.417", _writeRespMeter);
	_resp426Meter = createIncrementalMeter(prefix+"write.resp.426", _writeRespMeter);
	_resp428Meter = createIncrementalMeter(prefix+"write.resp.428", _writeRespMeter);
	_resp429Meter = createIncrementalMeter(prefix+"write.resp.429", _writeRespMeter);
	_resp431Meter = createIncrementalMeter(prefix+"write.resp.431", _writeRespMeter);
	_resp500Meter = createIncrementalMeter(prefix+"write.resp.500", _writeRespMeter);
	_resp501Meter = createIncrementalMeter(prefix+"write.resp.501", _writeRespMeter);
	_resp502Meter = createIncrementalMeter(prefix+"write.resp.502", _writeRespMeter);
	_resp503Meter = createIncrementalMeter(prefix+"write.resp.503", _writeRespMeter);
	_resp504Meter = createIncrementalMeter(prefix+"write.resp.504", _writeRespMeter);
	_resp505Meter = createIncrementalMeter(prefix+"write.resp.505", _writeRespMeter);
	_resp506Meter = createIncrementalMeter(prefix+"write.resp.506", _writeRespMeter);
	_resp509Meter = createIncrementalMeter(prefix+"write.resp.509", _writeRespMeter);
	_resp510Meter = createIncrementalMeter(prefix+"write.resp.510", _writeRespMeter);
	_resp520Meter = createIncrementalMeter(prefix+"write.resp.520", _writeRespMeter);
	_resp999Meter = createIncrementalMeter(prefix+"write.resp.999", _writeRespMeter);
		
	return this;
    }

    @Override
    public void stop() {
	stopRateMeter(_readReqsRateMeter);
	super.stop();
    }

    public Meter getOpenChannelsMeter() {
	return _channelsTcpOpenMeter;
    }

    public Meter getClosedChannelsMeter() {
	return _channelsTcpClosedMeter;
    }

    public Meter getParserErrorMeter() {
	return _parserErrorMeter;
    }

    public Meter getReadReqMeter(String method) {
	switch (method) {
	case "GET":
	    return _readGETMeter;
	case "PUT":
	    return _readPUTMeter;
	case "POST":
	    return _readPOSTMeter;
	case "DELETE":
	    return _readDELETEMeter;
	case "HEAD":
	    return _readHEADMeter;
	case "PATCH":
	    return _readPATCHMeter;
	case "OPTIONS":
	    return _readOPTIONSMeter;
	default:
	    return _readOTHERMeter;
	}
    }

    public Meter getWriteRespMeter (int status){
	switch (status){
	case 100: return _resp100Meter;
	case 101: return _resp101Meter;
	case 200: return _resp200Meter;
	case 201: return _resp201Meter;
	case 202: return _resp202Meter;
	case 203: return _resp203Meter;
	case 204: return _resp204Meter;
	case 205: return _resp205Meter;
	case 206: return _resp206Meter;
	case 300: return _resp300Meter;
	case 301: return _resp301Meter;
	case 302: return _resp302Meter;
	case 303: return _resp303Meter;
	case 304: return _resp304Meter;
	case 305: return _resp305Meter;
	case 306: return _resp306Meter;
	case 307: return _resp307Meter;
	case 308: return _resp308Meter;
	case 310: return _resp310Meter;
	case 400: return _resp400Meter;
	case 401: return _resp401Meter;
	case 402: return _resp402Meter;
	case 403: return _resp403Meter;
	case 404: return _resp404Meter;
	case 405: return _resp405Meter;
	case 406: return _resp406Meter;
	case 407: return _resp407Meter;
	case 408: return _resp408Meter;
	case 409: return _resp409Meter;
	case 410: return _resp410Meter;
	case 411: return _resp411Meter;
	case 412: return _resp412Meter;
	case 413: return _resp413Meter;
	case 414: return _resp414Meter;
	case 415: return _resp415Meter;
	case 416: return _resp416Meter;
	case 417: return _resp417Meter;
	case 426: return _resp426Meter;
	case 428: return _resp428Meter;
	case 429: return _resp429Meter;
	case 431: return _resp431Meter;
	case 500: return _resp500Meter;
	case 501: return _resp501Meter;
	case 502: return _resp502Meter;
	case 503: return _resp503Meter;
	case 504: return _resp504Meter;
	case 505: return _resp505Meter;
	case 506: return _resp506Meter;
	case 509: return _resp509Meter;
	case 510: return _resp510Meter;
	case 520: return _resp520Meter;
	default: return _resp999Meter;
	}
    }

}
