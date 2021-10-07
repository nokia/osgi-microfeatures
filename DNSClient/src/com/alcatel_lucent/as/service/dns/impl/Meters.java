package com.alcatel_lucent.as.service.dns.impl;

import com.alcatel.as.service.metering2.*;

import com.alcatel_lucent.as.service.dns.*;

public class Meters extends SimpleMonitorable {
    
    public static final String MONITORABLE_NAME = "as.service.dns";

    private Set _aSet, _aaaaSet, _cnameSet, _naptrSet, _srvSet;
    public Meter _reqsMeter, _dnsMeter;

    public Meters (){
	super (MONITORABLE_NAME, "Meters for DnsFactory");
    }

    public Meters init (MeteringService metering){
	_reqsMeter = createIncrementalMeter (metering, "*:reqs", null);
	_dnsMeter = createIncrementalMeter (metering, "*:reqs.dns", null);
	_aSet = new Set (RecordType.A, this).register (metering);
	_aaaaSet = new Set (RecordType.AAAA, this).register (metering);
	_cnameSet = new Set (RecordType.CNAME, this).register (metering);
	_naptrSet = new Set (RecordType.NAPTR, this).register (metering);
	_srvSet = new Set (RecordType.SRV, this).register (metering);
	return this;
    }

    public Set getSet (RecordType type){
	switch (type){
	case A : return _aSet;
	case AAAA : return _aaaaSet;
	case CNAME : return _cnameSet;
	case NAPTR : return _naptrSet;
	case SRV : return _srvSet;
	}
	return null;
    }
    
    public static class Set {
	protected Meters _parent;
	public RecordType _type;
	public Meter _reqsMeter, _reqsOKMeter, _reqsKOMeter, _dnsMeter, _dnsOKMeter, _dnsKOMeter;
	public Meter _cacheHitMeter, _cacheEntriesMeter;
	public Meter _hostsHitMeter;
	
	public Set (RecordType type, Meters parent){
	    _type = type;
	    _parent = parent;
	}
	private Set register (MeteringService metering){
	    _reqsMeter = _parent.createIncrementalMeter (metering, _type+":reqs", _parent._reqsMeter);
	    _reqsOKMeter = _parent.createIncrementalMeter (metering, _type+":reqs.success", null);
	    _reqsKOMeter = _parent.createIncrementalMeter (metering, _type+":reqs.failed", null);
	    _dnsMeter = _parent.createIncrementalMeter (metering, _type+":reqs.dns", _parent._dnsMeter);
	    _dnsOKMeter = _parent.createIncrementalMeter (metering, _type+":reqs.dns.success", null);
	    _dnsKOMeter = _parent.createIncrementalMeter (metering, _type+":reqs.dns.failed", null);
	    _cacheHitMeter = _parent.createIncrementalMeter (metering, _type+":cache.hits", null);
	    _cacheEntriesMeter = _parent.createAbsoluteMeter (metering, _type+":cache.entries");
	    _hostsHitMeter = _parent.createIncrementalMeter (metering, _type+":hosts.hits", null);
	    return this;
	}
    }
}
