package com.alcatel.as.http.parser;

import com.alcatel.as.ioh.tools.*;
import org.apache.log4j.Logger;
import java.nio.*;
import java.net.*;
import java.util.*;
import java.io.*;
import java.util.function.*;

import static com.alcatel.as.http.parser.HttpParser.getUTF8;
import static com.alcatel.as.http.parser.HttpParser.UTF_8;

public class HttpMessageImpl extends InputStream implements HttpMessage {
    
    public static final Logger LOGGER = Logger.getLogger ("http.ioh.parser.pdu");
    private static final byte[] VOID = new byte[0];

    protected boolean _isRequest = true;
    protected int _startHeaders = 0;
    protected byte[] _body;
    protected String _method, _url;
    protected int _status = -1;
    protected int _prevprevLF = -1, _prevLF;
    protected int _hSep;
    protected Map<String, HeaderImpl> _headersMap = new HashMap<> ();
    protected ByteBuffer _input;
    protected CompositeByteArray _headers = new CompositeByteArray (512, 500, 100); // we limit to 50K
    protected Object _attachment, _agent;
    protected boolean _dismantled, _isFirst = true, _isLast = false, _isChunked;
    protected ByteBuffer _firstLineReplaced;

    public class HeaderImpl implements HttpMessage.Header {
	protected int _start, _end, _startValue;
	protected String _name, _value;
	protected byte[] _raw;
	protected HeaderImpl _next;
	public HeaderImpl (String name, String value){
	    _name = name;
	    _value = value;
	}
	private HeaderImpl (String name, int start, int end, int startValue){
	    _name = name;
	    _start = start;
	    _end = end;
	    _startValue = startValue;
	}
	public HttpMessage.Header getNext (){ return _next;}
	private byte[] parseRawValue (){
	    if (_raw != null) return _raw;
	    byte[] raw = extractRawValue ();
	    boolean trimmed = false;
	    int stop = raw.length - 1;
	    while (stop >= 0){
		if (raw[stop] <= 0x20){
		    stop--;
		    trimmed = true;
		    continue;
		}
		break;
	    }
	    if (!trimmed)
		return _raw = raw;
	    if (stop == -1) return _raw = new byte[0];
	    _raw = new byte[stop + 1];
	    System.arraycopy (raw, 0, _raw, 0, _raw.length);
	    return _raw;
	}
	private String parseValue (){
	    if (_value != null) return _value;
	    return _value = new String (parseRawValue (), UTF_8);
	}
	public String getName (){ return _name;}
	public String getValue (){ return parseValue ();}
	public byte[] getRawValue (){ return parseRawValue ();}
	public int getValueAsInt (int def){
	    try{
		return Integer.parseInt (parseValue ());
	    }catch(Exception e){
		return def;
	    }
	}
	private java.io.InputStream getRawStream (){
	    return _headers.getInputStream (_start, _end - _start);
	}
	private byte[] extractRawValue (){
	    int len = _end - _startValue - 2; // skip tail CRLF
	    byte[] b = null;
	    int off = 0;
	    java.io.InputStream in = _headers.getInputStream (_startValue);
	    try{
		for (int i=0; i<len; i++){
		    int x = in.read ();
		    if (x == 0xD || x == 0xA) x = 0x20; // CRLF
		    if (b == null){
			// trim the head on the fly
			if (x == 0x20 || x == 0x09) continue;
			b = new byte[len - i];
		    }
		    b[off++] = (byte)x;
		}
	    }catch(IOException e){return null;}
	    return b != null ? b : new byte[0];
	}
    }
	
    public HttpMessageImpl (){
    }

    public boolean isRequest (){ return _isRequest;}
    
    public void attach (Object o){ _attachment = o;}
    public <T> T attachment (){ return (T) _attachment;}

    public HttpMessageImpl setIsRequest (boolean isRequest){ _isRequest = isRequest; return this;}
    
    public <T> T getAgent (){ return (T) _agent;}
    public void setAgent (Object agent){ _agent = agent;}

    public void setFirstLine (ByteBuffer firstLine){
	_firstLineReplaced = firstLine;
	_dismantled = true;
    }
    
    /*****************************
     * External API *
     *****************************/
    public boolean isFull (){ return _isFirst & _isLast;}
    public boolean isFirst () { return _isFirst;}
    public boolean isLast () { return _isLast;}
    public boolean isChunked (){ return _isChunked;}
    
    public String getMethod (){ return _method;}
    public String getURL (){ return _url;}
    public int getStatus (){ return _status;}
    public int getVersion (){
	// return 0 for HTTP/1.0 and 1 for HTTP/1.1
	byte b = _headers.get (_startHeaders - 3);
	return (b & 0xFF) - (int)'0';
    }

    public byte[] getBody (){ return _body;}

    public Header getHeader (String name){
	if (_headersMap == null) return null;
	return _headersMap.get (name);
    }
    public String getHeaderValue (String name){
	Header h = getHeader (name);
	return h != null ? h.getValue () : null;
    }
    public int getHeaderValueAsInt (String name, int def){
	Header h = getHeader (name);
	return h != null ? h.getValueAsInt (def) : def;
    }

    public HttpMessage addHeader (byte[] name, String value){
	return addHeader (name, getUTF8 (value));
    }
    public HttpMessage addHeader (byte[] name, byte[] value){
	_headers.cut (2); // remove tailCRLF
	addHeader (_headers, name, value, true);
	return this;
    }
    private void addHeader (CompositeByteArray dest, byte[] name, byte[] value, boolean tailCRLF){
	dest.add (name, 0, name.length);
	dest.add ((byte)':');
	dest.add ((byte)' ');
	dest.add (value, 0, value.length);
	dest.add ((byte)'\r');
	dest.add ((byte)'\n');
	if (tailCRLF){
	    dest.add ((byte)'\r');
	    dest.add ((byte)'\n');
	}
    }
    public HttpMessage removeHeader (Header h){
	HeaderImpl hi = (HeaderImpl)h;
	return removeHeader (hi._start, hi._end);
    }
    private HttpMessage removeHeader (int start, int end){
	_headers.remove (start, end);
	_dismantled = true;
	return this;
    }
    public HttpMessage removeHeaders (){
	_headers.cutFrom (_startHeaders);
	_headers.add ((byte)'\r');
	_headers.add ((byte)'\n');
	_dismantled = false;
	return this;
    }

    public void iterateHeaders (BiConsumer<String, String> consumer){
	for (String name: _headersMap.keySet ()){
	    HeaderImpl h = _headersMap.get (name);
	    while (h != null){
		consumer.accept (name, h.getValue ());
		h = h._next;
	    }
	}
    }
    
    /*****************************
     * Parsing callbacks *
     *****************************/

    public HttpMessageImpl setHasMore (){
	_isFirst = false;
	return this;
    }

    public HttpMessageImpl setIsChunked (){
	_isChunked = true;
	return this;
    }

    public HttpMessageImpl done (boolean finished){
	_isLast = finished;
	return this;
    }

    public InputStream setInputByteBuffer (ByteBuffer buffer){
	_input = buffer;
	return this;
    }
    
    private boolean _storeLF = true;
    public int read() throws IOException {
	if (_input.hasRemaining()) {
	    byte b = _input.get();
	    int position = _headers.add (b);
	    if (b == (byte)10){ // LF
		if (_storeLF){
		    if (_prevprevLF == -1) _prevprevLF = position - 1;
		    else _prevLF = position - 1;
		    _storeLF = false;
		}
	    }
	    else _storeLF = _storeLF || b != (byte)13; // CR
	    return b & 0xFF;
	}
	return -1;
    }

    public void setMethod (String s){ _method = s;}
    public void setURL (String s){ _url = s;}
    public void setStatus (int i){ _status = i;}

    public HttpMessageImpl startHeaders (){
	_startHeaders = _headers.limit ();
	return this;
    }

    // NOT USED - left for baseline
    public void readHeader (){
	_prevprevLF = _prevLF;
    }
    
    public HeaderImpl readHeader (String name){
	HeaderImpl existingHeader = _headersMap.get (name);
	HeaderImpl newHeader = new HeaderImpl (name, _prevprevLF+1, _prevLF+1, _hSep);
	if (existingHeader == null){
	    _headersMap.put (name, newHeader);
	} else {
	    while (true){
		HeaderImpl next = existingHeader._next;
		if (next == null){
		    existingHeader._next = newHeader;
		    break;
		}
		existingHeader = next;
	    }
	}
	_prevprevLF = _prevLF;
	return newHeader;
    }
    public void ignoreHeader (String name){
	_headers.remove (_prevprevLF+1, _prevLF+1);
	_prevprevLF = _prevLF;
    }
    public String peepHeader (){ // message is not modified
	return new HeaderImpl (null, _prevprevLF+1, _prevLF+1, _hSep).getValue ();
    }

    public void readHeaderSep (){
	_hSep = _headers.limit ();
    }
    
    public void setBody (byte[] body)
    {
	_body = body != null ? body : VOID;
    }
    
    /*****************************
     * Send out. *
     *****************************/
    
    private static final ByteBuffer[] empty = new ByteBuffer[0];

    public ByteBuffer[] toByteBuffers (){
	return toByteBuffers (_isFirst, _isFirst, true);
    }

    public ByteBuffer[] toByteBuffers (boolean firstLine, boolean headers, boolean body){
	if (body) body = _body.length > 0;
	if (firstLine && headers){
	    // shorcut
	    if (!_dismantled && _headers.nbBlocks () == 1)
		return body ? new ByteBuffer []{_headers.toByteBuffer (), ByteBuffer.wrap (_body)} :
	    new ByteBuffer []{_headers.toByteBuffer ()};
	}
	ByteBuffer firstLineBuffer = null;
	int startHeaders = _startHeaders;
	if (firstLine){
	    if (_firstLineReplaced != null){
		firstLineBuffer = _firstLineReplaced;
	    } else {
		if (headers){
		    startHeaders = 0;
		}else{
		    firstLineBuffer = _headers.toByteBuffer (0, _startHeaders);
		}
	    }
	}
	ByteBuffer[] headersBuffer = headers ? _headers.toByteBuffers (startHeaders) : empty;
	int firstLineIndex = firstLineBuffer != null ? 1 : 0;
	ByteBuffer[] res = new ByteBuffer[firstLineIndex + headersBuffer.length + (body? 1 : 0)];
	if (firstLineBuffer != null) res[0] = firstLineBuffer;
	if (headersBuffer.length > 0) System.arraycopy (headersBuffer, 0, res, firstLineIndex, headersBuffer.length);
	if (body) res[res.length - 1] = ByteBuffer.wrap (_body);
	return res;
    }

    /*****************************
     * Misc. *
     *****************************/
    
    public String toString (){
	return "isFirst="+_isFirst+", isLast="+_isLast+"\n"+ByteBufferUtils.toUTF8String (false, toByteBuffers (true, true, true)).replace ("\r", "_");
    }

}
