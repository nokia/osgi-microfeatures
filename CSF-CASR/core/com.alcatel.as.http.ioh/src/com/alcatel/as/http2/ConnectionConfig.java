package com.alcatel.as.http2;

import org.apache.log4j.Logger;
import java.util.Map;
import java.util.concurrent.Executor;
import com.alcatel.as.http.parser.CommonLogFormat;
import com.alcatel.as.http.parser.HttpMeters;

public class ConnectionConfig {

    public static final Logger LOGGER = Logger.getLogger ("as.http2");

    public static final String PROP_STREAMS_CLOSE_DELAY = "http2.streams.close.delay";
    public static final String PROP_CONN_PING_DELAY = "http2.connection.ping.delay";
    public static final String PROP_CONN_INIT_DELAY = "http2.connection.init.delay";
    public static final String PROP_CONN_WRITE_BUFFER = "http2.connection.write.buffer";
    public static final String PROP_CONN_IDLE_TIMEOUT = "http2.connection.idle.timeout";
    public static final String PROP_CONN_SNI_MATCH = "http2.connection.sni.match";

    public static final long PROP_STREAMS_CLOSE_DELAY_DEF = Long.getLong (PROP_STREAMS_CLOSE_DELAY, 250L);
    public static final long PROP_CONN_PING_DELAY_DEF = Long.getLong (PROP_CONN_PING_DELAY, 10000L);
    public static final long PROP_CONN_INIT_DELAY_DEF = Long.getLong (PROP_CONN_INIT_DELAY, 1000L);
    public static final int PROP_CONN_WRITE_BUFFER_DEF = Integer.getInteger (PROP_CONN_WRITE_BUFFER, 512*1024); // 512KB by def
    public static final long PROP_CONN_IDLE_TIMEOUT_DEF = Long.getLong (PROP_CONN_IDLE_TIMEOUT, -1L);
    public static final boolean PROP_CONN_SNI_MATCH_DEF = Boolean.getBoolean (PROP_CONN_SNI_MATCH);

    public static final String DEF_SERVER_READ_TIMEOUT = "31000"; // 31secs by def : when there is no server ping setup

    public static final String PROP_CONN_PRIOR_KNOWLEDGE = "http2.connection.prior-knowledge"; // this one is not used here : it is used by http2 users, just here to harmonize names
    public static final String PROP_ENABLED = "http2.enabled"; // this one is not used here : it is used by http2 users, just here to harmonize names

    // ALL FIELDS MUST BE CLONED in clone()
    public Logger _logger;
    protected Settings _settings;
    protected long _closeDelay = PROP_STREAMS_CLOSE_DELAY_DEF;
    protected long _pingDelay = PROP_CONN_PING_DELAY_DEF;
    protected long _initDelay = PROP_CONN_INIT_DELAY_DEF;
    protected boolean _priorK = true;
    protected boolean _enabled = true;
    protected int _writeBuffer = PROP_CONN_WRITE_BUFFER_DEF;
    protected CommonLogFormat _commonLogFormat = new CommonLogFormat ();
    protected HttpMeters _meters;
    protected Executor _writeExec;
    protected long _idleTimeout=PROP_CONN_IDLE_TIMEOUT_DEF;
    protected boolean _sniMatch = PROP_CONN_SNI_MATCH_DEF;

    public ConnectionConfig (Settings settings, Logger logger){
	_settings = settings;
	_logger = logger != null ? logger : LOGGER;
    }
    private ConnectionConfig (){} // for clone()
    public Settings settings (){ return _settings;}
    public Logger logger (){ return _logger;}
    public long closeDelay (){ return _closeDelay;}
    public ConnectionConfig closeDelay (long l){ _closeDelay = l; return this;}
    public long pingDelay (){ return _pingDelay;}
    public ConnectionConfig pingDelay (long l){ _pingDelay = l; return this;}
    public long initDelay (){ return _initDelay;}
    public ConnectionConfig initDelay (long l){ _initDelay = l; return this;}
    public boolean priorKnowledge (){ return _priorK;}
    public ConnectionConfig priorKnowledge (boolean priorK){ _priorK = priorK; return this;}
    public boolean enabled (){ return _enabled;}
    public ConnectionConfig enabled (boolean enabled){ _enabled = enabled; return this;}
    public int writeBuffer (){ return _writeBuffer;}
    public ConnectionConfig writeBuffer (int writeBuffer) { _writeBuffer = writeBuffer; return this;}
    public CommonLogFormat commonLogFormat (){ return _commonLogFormat;}
    public ConnectionConfig commonLogFormat (CommonLogFormat commonLogFormat) { _commonLogFormat = commonLogFormat; return this;}
    public HttpMeters meters (){ return _meters;}
    public ConnectionConfig meters (HttpMeters meters){ _meters = meters; return this;}
    public Executor writeExecutor (){ return _writeExec;}
    public ConnectionConfig writeExecutor (Executor ex){ _writeExec = ex; return this;}
    public long idleTimeout (){ return _idleTimeout;}
    public ConnectionConfig idleTimeout (long l){ _idleTimeout = l; return this;}
    public boolean sniMatch (){ return _sniMatch;}
    public ConnectionConfig sniMatch (boolean b){ _sniMatch = b; return this;}
    
    public ConnectionConfig load (boolean server, Map props){
	return load (server, props, null);
    }
    public ConnectionConfig load (boolean server, Map props, String prefix){
	if (server) _pingDelay = 0L; // no ping by def from server
	if (props == null) return this;
	boolean noprefix = prefix == null || prefix.length () == 0;
	String value = (String) props.get (noprefix ? PROP_STREAMS_CLOSE_DELAY : prefix + PROP_STREAMS_CLOSE_DELAY);
	if (value != null) _closeDelay = Long.parseLong (value);
	value = (String) props.get (noprefix ? PROP_CONN_PING_DELAY : prefix + PROP_CONN_PING_DELAY);
	if (value != null) _pingDelay = Long.parseLong (value);
	value = (String) props.get (noprefix ? PROP_CONN_INIT_DELAY : prefix + PROP_CONN_INIT_DELAY);
	if (value != null) _initDelay = Long.parseLong (value);
	value = (String) props.get (noprefix ? PROP_CONN_WRITE_BUFFER : prefix + PROP_CONN_WRITE_BUFFER);
	if (value != null) _writeBuffer = Integer.parseInt (value);
	value = (String) props.get (noprefix ? PROP_CONN_IDLE_TIMEOUT : prefix + PROP_CONN_IDLE_TIMEOUT);
	if (value != null) _idleTimeout = Long.parseLong (value);	
	if (server){
	    value = (String) props.get (noprefix ? PROP_CONN_PRIOR_KNOWLEDGE : prefix + PROP_CONN_PRIOR_KNOWLEDGE);
	    if (value != null) _priorK = Boolean.parseBoolean (value);
	    value = (String) props.get (noprefix ? PROP_ENABLED : prefix + PROP_ENABLED);
	    if (value != null) _enabled = Boolean.parseBoolean (value);
	    value = (String) props.get (noprefix ? PROP_CONN_SNI_MATCH : prefix + PROP_CONN_SNI_MATCH);
	    if (value != null) _sniMatch = Boolean.parseBoolean (value);
	    _commonLogFormat.configure (props);
	}
	return this;
    }

    public ConnectionConfig copy (){
	ConnectionConfig clone = new ConnectionConfig ();
	clone._logger = _logger;
	clone._settings = _settings;
	clone._closeDelay = _closeDelay;
	clone._pingDelay = _pingDelay;
	clone._initDelay = _initDelay;
	clone._priorK = _priorK;
	clone._enabled = _enabled;
	clone._writeBuffer = _writeBuffer;
	clone._commonLogFormat = _commonLogFormat;
	clone._meters = _meters;
	clone._writeExec = _writeExec;
	clone._idleTimeout = _idleTimeout;
	clone._sniMatch = _sniMatch;
	return clone;
    }
}
