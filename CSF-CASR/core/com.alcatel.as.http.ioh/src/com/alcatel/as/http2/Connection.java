package com.alcatel.as.http2;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import com.alcatel.as.http2.headers.*;
import com.alcatel.as.http2.hpack.*;
import com.alcatel.as.http2.frames.*;
import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import com.alcatel.as.http.parser.*;

public class Connection implements HPACKEncoder.EncoderSouth {

    private static final AtomicLong SEED = new AtomicLong (1);
    private static final byte[] PREFACE = new byte[]{(byte)0x50, (byte)0x52, (byte)0x49, (byte)0x20, (byte)0x2a, (byte)0x20, (byte)0x48, (byte)0x54, (byte)0x54, (byte)0x50, (byte)0x2f, (byte)0x32, (byte)0x2e, (byte)0x30, (byte)0x0d, (byte)0x0a, (byte)0x0d, (byte)0x0a, (byte)0x53, (byte)0x4d, (byte)0x0d, (byte)0x0a, (byte)0x0d, (byte)0x0a};
    // we could go up to 7F_FF_FF_FF but lets mimic curl and max to 40_00_00_00
    private static final int MAX_RECV_FC_WINDOW = 0x40_00_00_00;
    // trigger WU at 50%
    private static final int TRIGGER_RECV_FC_WINDOW = 0x20_00_00_00;

    private long _id;
    private int _prefaceIndex;
    private ConnectionConfig _config;
    private boolean _server;
    private FrameReader _frameReader;
    private Map<Integer, Stream> _streams = new HashMap<> ();
    private Map<Integer, Stream> _closedStreams1;
    private Map<Integer, Stream> _closedStreams2;
    private Stream _currentStream;
    private int _highestStreamId = 0;
    private Executor _writeExec, _readExec;
    private PlatformExecutor _writePExec, _readPExec;
    private TcpChannel _channel;
    private String _toString;
    private Logger _logger;
    private boolean _stopRead, _stopWrite;
    private Settings _remoteSettings; // managed in _writeExec
    private long _remoteInitialWindowSize; // managed in _readExec
    private int _nextStreamId = -1;
    private int _nextType = SettingsFrame.TYPE;
    private Http2RequestListener _reqListener;
    private Object _attachment;
    private boolean _closeDelayed;
    private HPACKDecoder _hpackDecoder;
    private HPACKEncoder _hpackEncoder;
    private int _sendFCWindow = (int) Settings.DEF_INITIAL_WINDOW_SIZE;
    private int _recvFCWindow = MAX_RECV_FC_WINDOW;
    private ByteBuffer _sendHeadersBuffer;
    private boolean _pinging;
    private ConnectionListener _connListener;
    private ConnectionListenerState _connListenerState = CONN_LISTENER_STATE_VOID;
    private OnAvailable _onAvailable;
    private boolean _trackIdleness;
    private long _idleTimeout = -1L;
    private Future _idlenessTaskFuture;
    private long _lastRead = -1L, _lastWrite = -1L;
    private List<String> _SNIs;
    private boolean _initSNIs;
    
    public Connection (boolean server, ConnectionConfig conf, TcpChannel channel, Http2RequestListener reqListener){
	_id = SEED.getAndIncrement ();
	_config = conf;
	_server = server;
	_logger = conf.logger ();
	_channel = channel;
	_initSNIs = server && channel.isSecure () && conf.sniMatch ();
	_readExec = channel.getInputExecutor ();
	_reqListener = reqListener;
	_frameReader = new FrameReader (_config);
	_hpackDecoder = new HPACKDecoder (_decoderConfig);
	_sendHeadersBuffer = ByteBuffer.allocate (512); // TODO make it configurable
	_writeExec = conf.writeExecutor ();
	if (_writeExec == null) _writeExec = new SerialExecutor ();
	if (_writeExec instanceof PlatformExecutor) _writePExec = (PlatformExecutor) _writeExec;
	if (_readExec instanceof PlatformExecutor) _readPExec = (PlatformExecutor) _readExec;
	_onAvailable = new OnAvailable (this::isAvailable, this::scheduleInWriteExecutor, logger ());
	_toString = new StringBuilder ().append (_server ? "ServerConnection[" : "ClientConnection[").append (channel).append (']').toString ();
    }
    public String toString (){ return _toString;}
    public ConnectionConfig config (){ return _config;}
    public Executor readExecutor (){ return _readExec;}
    public Executor writeExecutor (){ return _writeExec;}
    public TcpChannel channel (){ return _channel;}
    public Settings remoteSettings (){ return _remoteSettings;}
    public boolean isServer (){ return _server;}
    public Logger logger (){ return _logger;}
    public <T> T attachment (){ return (T) _attachment;}
    public Connection attach (Object o){ _attachment = o; return this;}
    public Http2RequestListener reqListener (){ return _reqListener;}
    public HPACKDecoder hpackDecoder (){ return _hpackDecoder;}
    public HPACKEncoder hpackEncoder (){ return _hpackEncoder;}
    public ByteBuffer getSendHeadersBuffer (){ return _sendHeadersBuffer;}
    public CommonLogFormat commonLogFormat (){ return _config.commonLogFormat ();}
    public HttpMeters meters (){ return _config.meters ();}
    public long id (){ return _id;}
    public Connection listener (ConnectionListener listener){
	_connListener = listener;
	if (_connListener != null)
	    _connListenerState = new ConnectionListenerStateInit ();
	return this;
    }
    public Future scheduleInReadExecutor (Runnable r, long delay){
	if (_readPExec != null)
	    return _readPExec.schedule (r, delay, TimeUnit.MILLISECONDS);
	return ConnectionFactory.INSTANCE._timerS.schedule (_readExec, r, delay, TimeUnit.MILLISECONDS);
    }
    public Future scheduleInWriteExecutor (Runnable r, long delay){
	if (_writePExec != null)
	    return _writePExec.schedule (r, delay, TimeUnit.MILLISECONDS);
	return ConnectionFactory.INSTANCE._timerS.schedule (_writeExec, r, delay, TimeUnit.MILLISECONDS);
    }

    // can be called from anywhere / useful to update the idleTimeout for a graceful close
    // not expected before opened succeeded
    public void idleTimeout (long idleTimeout){
	if (idleTimeout <= 0L) return;
	_readExec.execute ( () -> {
		if (_stopRead) return;
		_idleTimeout = idleTimeout;
		if (_idlenessTaskFuture != null)
		    _idlenessTaskFuture.cancel (true);
		_idlenessTaskFuture = scheduleInReadExecutor (_idlenessTask, 100L); // we dont wait for _idlenessTask : we test now
	    });
    }

    /****************** Control methods : called from anywhere ***************/
    public Connection skipPRI (boolean skipPRI){ // when PRI was already read to detect http2
	if (!skipPRI) return this;
	// must be called after init (true)
	_prefaceIndex = 4; // "PRI "
	return this;
    }
    public Connection init (){
	if (_closeDelayed = _config.closeDelay () > 0){
	    _closedStreams1 = new HashMap<> ();
	    _closedStreams2 = new HashMap<> ();
	    scheduleInReadExecutor (_closedStreamsTask, _config.closeDelay ());
	}
	scheduleInReadExecutor (this::initTimeout, _config.initDelay ());
	if (!_server){
	    _prefaceIndex = -1;
	    _writeExec.execute (() -> {
		    _channel.send (ByteBuffer.wrap (PREFACE), true);
		    SettingsFrame sf = Frame.newFrame (SettingsFrame.TYPE);
		    sendNow (null, sf.set (_config.settings ()));
		    WindowUpdateFrame wuf = WindowUpdateFrame.newWindowUpdateFrame (0, MAX_RECV_FC_WINDOW - (int) Settings.DEF_INITIAL_WINDOW_SIZE);
		    sendNow (null, wuf);
		});
	}
	return this;
    }
    private void initTimeout (){
	if (_nextType == SettingsFrame.TYPE){
	    if (_logger.isInfoEnabled ()) _logger.info (this+" : init timeout : closing");
	    _writeExec.execute (() -> {_connListenerState.failed ();});
	    closeNow (Http2Error.Code.PROTOCOL_ERROR, "Not timely initialized.");
	    return;
	}
    }
    
    public void close (){
	close (Http2Error.Code.NO_ERROR, "");
    }
    public void close (Http2Error.Code code, String msg){
	close (new ConnectionError (code, msg != null ? msg : ""));
    }
    public void close (ConnectionError ce){
	// used by streams from _writeExec
	_readExec.execute (() -> {closeNow (ce);});
    }
    // must be called in _readExec
    public void closeNow (){
	closeNow (Http2Error.Code.NO_ERROR, "");
    }
    // must be called in _readExec
    public void closeNow (Http2Error.Code code, String msg){
	closeNow (new ConnectionError (code, msg != null ? msg : ""));
    }
    // must be called in _readExec
    public void closeNow (ConnectionError ce){
	if (_stopRead) return;
	_stopRead = true;
	final GoAwayFrame go = Frame.newFrame (GoAwayFrame.TYPE);
	go.set (_highestStreamId, ce.code ().value (), ce.getMessage ());
	send (null, go);
    }
    
    // we could add update(Settings settings) but this is not needed for now

    public int openStreams (){ return _streams.size ();}
    public int closedStreams (){
	if (_closeDelayed) return _closedStreams1.size () + _closedStreams2.size ();
	return 0;
    }

    // must be called in _writeExec - reflects closed state
    public boolean stopWrite (){ return _stopWrite;}
    // must be called in _readExec - reflects closed state
    public boolean stopRead (){ return _stopRead;}

    /****************** TcpChanned callbacks : called from _readExec ***************/

    public void closed (){
	_stopRead = true;
	if (_idlenessTaskFuture != null){
	    _idlenessTaskFuture.cancel (true);
	    _idlenessTaskFuture = null;
	}
	List<Stream> list = new ArrayList<> (_streams.size ());
	for (Stream stream : _streams.values ()){
	    list.add (stream); // cannot call stream.connectionClosed --> closed(stream) would make a concurrent modification exception
	}
	for (Stream stream : list) stream.connectionClosed ();
	_streams.clear ();
	_writeExec.execute (() -> {_stopWrite = true; _onAvailable.closed (); _connListenerState.closed ();});
    }

    public void receiveTimeout (){
	if (_config.pingDelay () == 0L){
	    if (_logger.isInfoEnabled ()) _logger.info (this+" : receiveTimeout : no ping setup");
	    closeNow (Http2Error.Code.NO_ERROR, "Inactivity timeout");
	    return;
	}
	if (_pinging){
	    if (_logger.isInfoEnabled ()) _logger.info (this+" : receiveTimeout : no ping ack : closing");
	    closeNow (Http2Error.Code.PROTOCOL_ERROR, "No response to PING");
	    return;
	}
	if (_logger.isTraceEnabled ()) _logger.trace (this+" : receiveTimeout : pinging");
	_pinging = true;
	PingFrame pf = Frame.newFrame (PingFrame.TYPE);
	send (null, pf.set ());
    }
    
    public void received (ByteBuffer buffer) {
	_pinging = false;
	if (_stopRead){
	    buffer.position (buffer.limit ());
	    return;
	}
	if (_initSNIs){
	    _initSNIs = false;
	    _SNIs = new ArrayList<> ();
	    for (javax.net.ssl.SNIHostName sni : _channel.getClientRequestedServerNames()) {
		String host = sni.getAsciiName();
		if (_logger.isDebugEnabled ())
		    _logger.debug (this+" : client SNI : "+host);
		_SNIs.add (host);
	    }
	    if (_logger.isDebugEnabled () && _SNIs.size () == 0)
		_logger.debug (this+" : no client SNI");
	}
	if (_prefaceIndex != -1){
	    int remaining = buffer.remaining ();
	    int prefaceLeft = Math.min (PREFACE.length - _prefaceIndex, remaining);
	    for (int i=0; i<prefaceLeft; i++)
		if (buffer.get () != PREFACE[_prefaceIndex++]){
		    if (_logger.isInfoEnabled ())
			_logger.info (this+" : invalid preface");
		    buffer.position (buffer.limit ());
		    _stopRead = true;
		    _channel.close ();
		    return;
		}
	    if (_prefaceIndex == PREFACE.length){
		_prefaceIndex = -1;
		// send out settings
		send (null, new SettingsFrame ().set (_config.settings()));
		send (null, WindowUpdateFrame.newWindowUpdateFrame (0, MAX_RECV_FC_WINDOW - (int) Settings.DEF_INITIAL_WINDOW_SIZE));
	    } else
		return;
	}
	Frame frame = null;
	try{
	    while ((frame = _frameReader.read (buffer)) != null){
		if (_nextType != -1){
		    if (frame.type () != _nextType)
			throw new ConnectionError (Http2Error.Code.PROTOCOL_ERROR, "Expected Frame type : "+_nextType+" : received : "+frame.type ());
		}
		if (_nextStreamId != -1){
		    if (frame.streamId () != _nextStreamId)
			throw new ConnectionError (Http2Error.Code.PROTOCOL_ERROR, "Expected Stream id : "+_nextStreamId+" : received : "+frame.streamId ());
		}

		int id = frame.streamId ();
		StreamError error = null;
		try{
		    frame.parse (); // can throw a StreamError or a ConnectionError
		    if (_logger.isDebugEnabled ())
			_logger.log (frame.type () == PingFrame.TYPE ? Level.TRACE : Level.DEBUG, this+" : recv : "+frame);
		}catch(StreamError se){
		    if (_logger.isInfoEnabled ())
			_logger.info (this+" : StreamError : "+se);
		    error = se;
		}
		
		if (frame.isControlFrame ()){
		    processControlFrame (frame);
		    continue;
		}

		if (_trackIdleness)
		    _lastRead = System.currentTimeMillis ();

		if (frame.type () == DataFrame.TYPE){
		    // we must keep the recv window updated whatever the stream status
		    // but if this is the last data frame, we can skip the window update for the stream
		    DataFrame df = (DataFrame) frame;
		    updateRecvFCWindow (df.needsWindowUpate () ? id : 0,
					df.totalPayloadSize ());
		}

		Stream stream = getStream (id);
		boolean newStream = stream == null;
		if (newStream){
		    // firefox & nghttp2 send a PriorityFrame before creating the streams!
		    // TODO handle PriorityFrame for a non-existing stream
		    switch (frame.type ()){
		    case DataFrame.TYPE: // late DataFrame : send a reset
			if (_logger.isDebugEnabled ())
			    _logger.debug (this+" : late frame : "+frame+" : reset");
			resetLateFrame (frame);
		    case PriorityFrame.TYPE:
		    case WindowUpdateFrame.TYPE: // late WindowUpdateFrame : do not reset this one
                    case ResetFrame.TYPE: // late ResetFrame : do not reset a reset
			continue;
		    }
		    try{
			stream = createStream (frame);
		    }catch(ConnectionError e){
			// we used to throw the exception
			// by precaution, we now assume this is a late frame and reset
			if (_logger.isDebugEnabled ())
			    _logger.debug (this+" : late frame : "+frame+" : reset");
			if (frame.type () == HeadersFrame.TYPE ||
			    frame.type () == ContinuationFrame.TYPE)
			    StreamStateMachine.consumeHeaders (this, frame);
			resetLateFrame (frame);
			continue;
		    }
		    if (openStreams () > _config.settings ().MAX_CONCURRENT_STREAMS){
			if (_logger.isInfoEnabled ())
			    _logger.info (this+" : reject : "+stream+" : reached max openStreams="+_config.settings ().MAX_CONCURRENT_STREAMS);
			stream.reject ();
			continue;
		    }
		}
		if (error == null)
		    stream.received (frame); // state machine errors are managed by the stream with callbacks
		else
		    stream.error (error);
		
		if (_stopRead){
		    // we got a connection error in the stream state mgmt
		    buffer.position (buffer.limit ());
		    return;
		}

		// we set the _highestStreamId once we know that it was processed ok
		// so a frame generating a conn error does not include its own id
		if (newStream && error == null) _highestStreamId = id;
	    }
	}catch(ConnectionError ce){ // ConnectionError in reading or in parsing
	    if (_logger.isInfoEnabled ())
		_logger.info (this+" : error while reading frames : "+ce);
	    buffer.position (buffer.limit ());
	    closeNow (ce);
	    return;
	}
	return;
    }

    /******************  Private methods : called in _readExec ***************/

    // when we recv data
    private void updateRecvFCWindow (int id, int sizeRecv){
	if (sizeRecv == 0) return;
	if (id != 0) send (null, WindowUpdateFrame.newWindowUpdateFrame (id, sizeRecv));
	_recvFCWindow -= sizeRecv;
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : decreased RECV flow control window to : "+_recvFCWindow);
	if (_recvFCWindow <= TRIGGER_RECV_FC_WINDOW){
	    send (null, WindowUpdateFrame.newWindowUpdateFrame (0, MAX_RECV_FC_WINDOW - _recvFCWindow));
	    _recvFCWindow = MAX_RECV_FC_WINDOW;
	}
    }

    // when we receive late frames
    private void resetLateFrame (Frame frame){
	reset (frame, Http2Error.Code.STREAM_CLOSED.value ());
    }
    private void reset (Frame frame, long errCode){
	ResetFrame rf = Frame.newFrame (ResetFrame.TYPE);
	rf.set (0x00, frame.streamId ());
	rf.set (errCode);
	send (null, rf);
    }

    private Stream getStream (int id) {
	Stream stream = _streams.get (id);
	if (_closeDelayed){
	    if (stream == null)
		stream = _closedStreams1.get (id);
	    if (stream == null)
		stream = _closedStreams2.get (id);
	}
	return stream;
    }
    private Stream createStream (Frame frame) throws ConnectionError {
	int id = frame.streamId ();
	if (id <= _highestStreamId)
	    throw new ConnectionError (Http2Error.Code.PROTOCOL_ERROR,
				       "New Stream Id too low : "+id+"<="+_highestStreamId
				       );
	if ((id & 0x01) == 0x01){ // odd : expected from client
	    if (!_server) // we are client !
		throw new ConnectionError (Http2Error.Code.PROTOCOL_ERROR,
					   "Remote server sent an odd stream id : "+id
					   );
	} else { // even : expected from server
	    if (_server) // we are server !
		throw new ConnectionError (Http2Error.Code.PROTOCOL_ERROR,
					   "Remote client sent an even stream id : "+id
					   );
	}
	Stream stream = Stream.newRecvStream (this, frame);
	if (stream == null)
	    throw new ConnectionError (Http2Error.Code.PROTOCOL_ERROR, "Unexpected Initial Frame type : "+frame.type ());
	_streams.put (id, stream);
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : created : "+stream);
	return stream.accept ();
    }
    
    private void processControlFrame (Frame frame){
	switch (frame.type ()){
	case GoAwayFrame.TYPE:
	    // no action yet as a server
	    return;
	case PingFrame.TYPE:
	    PingFrame ping = (PingFrame) frame;
	    if (ping.ack ()) return; // ack to our ping
	    send (null, ping.makeAck ());
	    return;
	case SettingsFrame.TYPE:
	    SettingsFrame sf = (SettingsFrame) frame;
	    if (sf.ack ()) return;
	    int diffInitialWindowSize = 0;
	    if (_nextType == SettingsFrame.TYPE){
		_nextType = -1; // reset the need to get settings in the first frame
		_lastRead = _lastWrite = System.currentTimeMillis (); // init them even if !_trackIdleness (in case idleTimeout(n) is called later)
		if (_config.pingDelay () > 0L)
		    // let the read.timeout delay possibly configured in the endpoint
		    _channel.setSoTimeout (_config.pingDelay ());
		_remoteInitialWindowSize = sf.settings ().INITIAL_WINDOW_SIZE;
		if (_config.idleTimeout () > 0L){
		    _trackIdleness = true;
		    _idleTimeout = _config.idleTimeout ();
		    _idlenessTaskFuture = scheduleInReadExecutor (_idlenessTask, _idleTimeout + 100); // we add 100ms to avoid schedule offsets
		}
	    } else {
		long newInitialWindowSize = sf.settings ().INITIAL_WINDOW_SIZE;
		diffInitialWindowSize = (int) (newInitialWindowSize - _remoteInitialWindowSize);
		_remoteInitialWindowSize = newInitialWindowSize;
		if (diffInitialWindowSize != 0 &&
		    _logger.isDebugEnabled ())
		    _logger.debug (this+" : new INITIAL_WINDOW_SIZE  : diff="+diffInitialWindowSize);
	    }
	    // update all open streams if INITIAL_WINDOW_SIZE was changed
	    // if diffInitialWindowSize < 0 : do it now, else after the ack
	    if (diffInitialWindowSize < 0){
		for (Stream stream : _streams.values ())
		    stream.updateSendFCWindow (diffInitialWindowSize);
	    }
	    SettingsFrame sfAck = SettingsFrame.makeAck ();
	    _writeExec.execute (() -> {
		    // schedule the ack in the _writeExec to avoid applying the old ones after the ack
		    _remoteSettings = sf.settings ();
		    sendNow (null, sfAck);
		    if (_hpackEncoder == null){
			_hpackEncoder = new HPACKEncoder (_encoderConfig);
			if (_encoderConfig.getHeaderTableSize() != Settings.DEF_HEADER_TABLE_SIZE ) {
				_hpackEncoder.update (_encoderConfig);
            		}
		    } else
			_hpackEncoder.update (_encoderConfig);
		    _connListenerState.opened (); // idempotent
		});
	    if (diffInitialWindowSize > 0){
		for (Stream stream : _streams.values ())
		    stream.updateSendFCWindow (diffInitialWindowSize);
	    }	    
	    return;
	case WindowUpdateFrame.TYPE:
	    WindowUpdateFrame wuf = (WindowUpdateFrame) frame;
	    _writeExec.execute (() -> {
		    int maxCurrent = 0x7F_FF_FF_FF - wuf.increment ();
		    if (_sendFCWindow > maxCurrent){
			close (new ConnectionError (Http2Error.Code.FLOW_CONTROL_ERROR, "Total connection FC_WINDOW exceeds 0x7FFFFFFF"));
		    } else {
			boolean wasBlocked = _sendFCWindow <= 0;
			_sendFCWindow += wuf.increment ();
			if (_logger.isDebugEnabled ())
			    _logger.debug (Connection.this+" : increased SEND flow control window to : "+_sendFCWindow);
			if (wasBlocked && _sendFCWindow > 0)
			    writeUnBlocked ();
		    }
		});
	    return;
	}
    }

    Runnable _closedStreamsTask = new Runnable (){
	    public void run (){
		if (_stopRead) return;
		_closedStreams2.clear ();
		Map<Integer, Stream> tmp = _closedStreams2;
		_closedStreams2 = _closedStreams1;
		_closedStreams1 = tmp;
		scheduleInReadExecutor (this, _config.closeDelay ());
	    }
	};

    Runnable _idlenessTask = new Runnable (){
	    public void run (){
		if (_stopRead) return;
		long now = System.currentTimeMillis ();
		long idle = Math.min (now - _lastRead, now - _lastWrite);
		if (idle >= _idleTimeout){
		    closeNow (Http2Error.Code.NO_ERROR, "Idle timeout");
		} else {
		    _idlenessTaskFuture = scheduleInReadExecutor (this, _idleTimeout - idle  + 100); // add 100ms
		}
	    }
	};

    /****************** Streams callbacks from state machine : called from _readExec ***************/

    protected void error (ConnectionError ce){
	closeNow (ce);
    }
    
    protected void closed (Stream stream){
	_streams.remove (stream.id ());
	if (_closeDelayed)
	    _closedStreams1.put (stream.id (), stream);
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : closed : "+stream);
    }

    protected void setNextFrame (int streamId, int type){
	_nextStreamId = streamId;
	_nextType = type;
    }

    /******************  public methods : called in misc cases ***************/

    // called from _readExec
    public void register (Stream stream, int initialSendFCWindow){
	register (stream);
	int diff = (int) (_remoteInitialWindowSize - initialSendFCWindow);
	if (diff != 0)
	    stream.updateSendFCWindow (diff);
    }
    public void register (Stream stream){
	_streams.put (stream.id (), stream);
    }

    // called from _readExec
    public boolean sniMatch (String value){
	if (_SNIs == null) return true;
	// we assume a fqdn - but there could be a port
	// we reject IPV6 (in case the : hits an ipv6)
	int i = value.lastIndexOf (':');
	if (i == 0) return false; // not possible normally
	if (i != -1) value = value.substring (0, i);
	for (String sni : _SNIs){
	    if (sni.equalsIgnoreCase (value))
		return true;
	}
	return false;
    }

    // can be called from anywhere
    public void send (Stream stream, Frame frame){
	frame.copyPayload ();
	_writeExec.execute (() -> {
		sendNow (stream, frame);
	    });
    }
    
    // must be called in _writeExec
    public void sendNow (Stream stream, Frame frame){
	if (_stopWrite && frame.type () != GoAwayFrame.TYPE) return; // we let the goaway frame go through: _stopWrite is also set when encoding
	if (_logger.isDebugEnabled ())
	    _logger.log (frame.type () == PingFrame.TYPE ? Level.TRACE : Level.DEBUG, this+" : send : "+frame);
	if (frame.type () == DataFrame.TYPE){
	    _sendFCWindow -= frame.payload ().remaining ();
	    if (_logger.isDebugEnabled ())
		_logger.debug (this+" : decrease flow control window to : "+_sendFCWindow);
	}
	int buffer = _channel.getSendBufferSize ();
	if (buffer > _config.writeBuffer ()){
	    if (_logger.isInfoEnabled ())
		_logger.info (this+" : buffer full : shutdown");
	    _stopWrite = true;
	    _channel.shutdown ();
	    return;
	}
	_stopWrite = frame.type () == GoAwayFrame.TYPE;
	frame.copyPayload ();
	_readExec.execute (() -> {
		// we used to send in _writeExec
		// but we had an issue : response received before stream registered (read op already scheduled)
		if (_trackIdleness && !frame.isControlFrame ()) _lastWrite = System.currentTimeMillis ();
		if (stream != null) stream.sent (frame);
		_channel.send (frame.getHeading (), false);
		_channel.send (frame.payload (), !frame.isCopy ());
		if (frame.type () == GoAwayFrame.TYPE)
		    _channel.close ();
	    });
    }
    
    // the following must be called in _writeExec and sequentially
    public void encodeHeader (Header h) {
	_hpackEncoder.encode (h, this);
    }
    public void encodeHeader (Header h, String value) {
	_hpackEncoder.encode (h, value, this);
    }
    public void encodeHeader (String name, String value) {
	_hpackEncoder.encode (name, value, this);
    }
    public void sendHeaders (Stream stream, boolean done){
	_sendHeadersStream = stream;
	_sendHeadersEndStream = done;
	_hpackEncoder.last_or_end_or_finish_or_stop_or_complete (this);
    }
    public void sendData (Stream stream, ByteBuffer data, boolean copy, boolean done){
	if (data == null){
	    if (!done) return;
	    data = ByteBuffer.allocate (0);
	    data.flip ();
	    copy = false;
	}
	if (data.remaining () == 0 && !done) return;
	int maxSize = (int) _remoteSettings.MAX_FRAME_SIZE;
	if (data.remaining () > maxSize) copy = true; // TODO check if necessary
	while (true){
	    boolean last = maxSize >= data.remaining ();
	    DataFrame df = Frame.newFrame (DataFrame.TYPE);
	    df.set ((done && last) ? 0x01 : 0x00,
		    stream.id ());
	    ByteBuffer chunk = last ? data : data.duplicate ();
	    if (!last){
		chunk.limit (chunk.position () + maxSize);
		data.position (data.position () + maxSize);
	    }
	    df.payload (chunk, !copy);
	    sendNow (last ? stream : null, df); // inhibit state machine callbacks until the last
	    if (last) break;
	}
    }
    public void sendPriority (int streamId, boolean exclusive, int streamDepId, int weight){
	PriorityFrame pf = Frame.newFrame (PriorityFrame.TYPE);
	sendNow (null, pf.set (streamId, exclusive, streamId, weight));
    }

    /************** methods related to sendFCWindow : called in _writeExec **********/

    public int sendFCWindow (){ return _sendFCWindow;}
    
    public void onWriteAvailable (Runnable success, Runnable failure, long delay){
	_onAvailable.add (success, failure, delay);
    }

    public boolean isAvailable (){ return _sendFCWindow > 0;}
    private void writeUnBlocked (){
	_onAvailable.available ();
    }
    
    /******************  Implements HPack Config ***************/

    private Config _decoderConfig = new Config (){
	    public long getConnectionId (){ return _id;}
	    public int getHeaderTableSize (){ return (int) _config.settings ().HEADER_TABLE_SIZE;}
	    public int getMaxHeaderListSize (){ return (int) _config.settings ().MAX_HEADER_LIST_SIZE;}
	    public Logger getLogger () { return logger ();}
	    public Executor getExecutor (){ return readExecutor ();}
	    public int getMaxFrameSize (){ return (int) _config.settings ().MAX_FRAME_SIZE;}
	    public String encodingCharset(){ return "utf-8";}
	};
    private Config _encoderConfig = new Config (){
	    public long getConnectionId (){ return _id;}
	    public int getHeaderTableSize (){ return (int) Math.min (_remoteSettings.HEADER_TABLE_SIZE, _decoderConfig.getHeaderTableSize ());}
	    public int getMaxHeaderListSize (){ return (int) Math.min (_remoteSettings.MAX_HEADER_LIST_SIZE, _decoderConfig.getMaxHeaderListSize ());}
	    public Logger getLogger () { return logger ();}
	    public Executor getExecutor (){ return writeExecutor ();}
	    public int getMaxFrameSize (){ return (int) _remoteSettings.MAX_FRAME_SIZE;}
	    public String encodingCharset(){ return "utf-8";}
	};

    /******************  Implements HPACKEncoder.EncoderSouth // called in _writeExec ***************/

    private Stream _sendHeadersStream;
    private boolean _sendHeadersEndStream;

    public void put(byte [] bytes){
	put (bytes, 0, bytes.length);
    }
    private void put (byte[] bytes, int off, int len){
	if (len == 0) return;
	int size = Math.min (len, _sendHeadersBuffer.remaining ());
	_sendHeadersBuffer.put (bytes, off, size);
	if (_sendHeadersBuffer.remaining () == 0){
	    expandSendHeadersBuffer ();
	    put (bytes, off + size, len - size);
	}	    
    }
    public void put(byte b){
	_sendHeadersBuffer.put (b);
	if (_sendHeadersBuffer.remaining () == 0)
	    expandSendHeadersBuffer ();
    }
    private void expandSendHeadersBuffer (){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : expandSendHeadersBuffer to : "+_sendHeadersBuffer.capacity () * 2);
	ByteBuffer tmp = ByteBuffer.allocate (_sendHeadersBuffer.capacity () * 2);
	_sendHeadersBuffer.flip ();
	tmp.put (_sendHeadersBuffer);
	_sendHeadersBuffer = tmp;
    }

    public void last_or_end_or_finish_or_stop_or_complete (){
	_sendHeadersBuffer.flip ();
	int maxSize = (int) _remoteSettings.MAX_FRAME_SIZE;
	HeadersFrame hf = Frame.newFrame (HeadersFrame.TYPE);
	if (maxSize >= _sendHeadersBuffer.remaining ()){
	    hf.set (HeadersFrame.flags (true, _sendHeadersEndStream),
		    _sendHeadersStream.id ());
	    hf.payload (_sendHeadersBuffer, false);
	    sendNow (_sendHeadersStream, hf);
	} else {
	    hf.set (HeadersFrame.flags (false, _sendHeadersEndStream),
		    _sendHeadersStream.id ());
	    ByteBuffer chunk = _sendHeadersBuffer.duplicate ();
	    int newPosition = _sendHeadersBuffer.position () + maxSize;
	    chunk.limit (newPosition);
	    _sendHeadersBuffer.position (newPosition);
	    hf.payload (chunk, false);
	    sendNow (null, hf); // inhibit state machine callbacks until the last
	    while (true){
		ContinuationFrame cf = Frame.newFrame (ContinuationFrame.TYPE);
		boolean last = maxSize >= _sendHeadersBuffer.remaining ();
		cf.set (ContinuationFrame.flags (last), _sendHeadersStream.id ());
		cf.endStream (_sendHeadersEndStream);
		chunk = last ? _sendHeadersBuffer : _sendHeadersBuffer.duplicate ();
		if (!last){
		    newPosition = _sendHeadersBuffer.position () + maxSize;
		    chunk.limit (newPosition);
		    _sendHeadersBuffer.position (newPosition);
		}		
		cf.payload (chunk, false);
		sendNow (last ? _sendHeadersStream : null, cf);
		if (last) break;
	    }
	}
	_sendHeadersBuffer.clear ();
    }

    public void error(){
	_logger.warn (this+" : error in HPACKEncoder : closing");
	_stopWrite = true; // inhibit right now later send
	close (new ConnectionError (Http2Error.Code.INTERNAL_ERROR, "Error in HPACKEncoder"));
    }

    /***************** ConnectionListener ***********/

    protected static interface ConnectionListenerState {
	default void failed (){}
	default void closed (){}
	default void opened (){}
    }
    private static ConnectionListenerState CONN_LISTENER_STATE_VOID = new ConnectionListenerState (){};
    private class ConnectionListenerStateInit implements ConnectionListenerState {
	public void failed (){
	    _connListener.failed (Connection.this);
	    _connListenerState = CONN_LISTENER_STATE_VOID; // inhibit the close
	}
	public void closed (){ failed ();}
	public void opened (){
	    _connListener.opened (Connection.this);
	    _connListenerState = new ConnectionListenerStateOpen ();
	}
    }
    private class ConnectionListenerStateOpen implements ConnectionListenerState {
	public void opened (){ _connListener.updated (Connection.this);}
	public void closed (){ _connListener.closed (Connection.this);}
    }
}
