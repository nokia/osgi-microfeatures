package com.alcatel.as.http.parser;

import com.alcatel.as.ioh.*;
import org.apache.log4j.Logger;

import com.alcatel.as.ioh.engine.tools.*;

import java.nio.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.function.*;

public class HttpParser implements MessageParser<HttpMessageImpl> {

    public static final Logger LOGGER = Logger.getLogger ("http.ioh.parser");

    public static final byte[] VOID = new byte[0];

    private static final HttpMessageFilter DEF_FILTER = new HttpMessageFilter (){};
    
    private HttpMessageImpl message;
    private MyStreamTokenizer tok = new MyStreamTokenizer (256, 10*1024); // set max length for an url : 10K (start with 256B for usual cases)
    private boolean readHeaders = false, readWholeFirstLine = false;
    private FLState readFirstLine = FLState.HEAD;
    private String hName;
    private HState state = HState.HNAME;
    private MyStreamTokenizer.Syntax syntax = syntaxHeaderName;
    private int contentLen = -1;
    private int bodyLeftToRead = 0;
    private boolean _chunked = false;
    private int chunkLeftToRead = -1;
    private ChunkState chunkState;
    private boolean _skipChunkDelimiters, _skipResponseBody;
    private HttpMessageFilter _filter = DEF_FILTER;
    private Supplier _peepHeader = () -> {return message.peepHeader ();};

    private static MyStreamTokenizer.Syntax syntaxFirstLine = new MyStreamTokenizer.Syntax ()
	.setWordCharacters (0, 127)
	.allowUTF8WordCharacters ()
	.setSpaceCharacter (' ');
    private static MyStreamTokenizer.Syntax syntaxFirstLineUpperCase = new MyStreamTokenizer.Syntax ()
	.setWordCharacters (0, 127)
	.allowUTF8WordCharacters ()
	.setSpaceCharacter (' ')
	.setUpperCase ();
    private static MyStreamTokenizer.Syntax syntaxHeaderName = new MyStreamTokenizer.Syntax ()
	.setWordCharacters (0, 127)
	.setLowerCase ()
	.setDelimiterCharacter (':')
	.setDelimiterCharacter (' ')
	.setDelimiterCharacter ('\t');
    private static MyStreamTokenizer.Syntax syntaxHeaderDelim = new MyStreamTokenizer.Syntax ()
	.setDelimiterCharacter (':')
	.setEOFDelimiter ()
	.setSpaceCharacter (' ')
	.setSpaceCharacter ('\t');
    private static MyStreamTokenizer.Syntax syntaxHeaderValue = new MyStreamTokenizer.Syntax ()
	.setWordCharacters (0, 255)
	.ignoreWord ();
    
    private static MyStreamTokenizer.Syntax syntaxChunk = new MyStreamTokenizer.Syntax ()
	.setWordCharacters ('0', '9')
	.setWordCharacters ('a', 'f')
	.setWordCharacters ('A', 'F')
	.setEOFDelimiter ();
    
    private static enum ChunkState { SIZE, DATA, DATA_CR, DATA_LF, LAST, LAST_CR, LAST_LF}; // if _skipChunkDelimiters=false, then only SIZE, DATA, LAST are used
    
    private static enum HState {
	HNAME, HSEP, HVALUE
    }
    private static enum FLState { HEAD, CENTER, TAIL, DONE}

    public HttpParser (){
    }
    public HttpParser skipChunkDelimiters (){ _skipChunkDelimiters = true; return this;}
    public HttpParser skipResponseBody (){ _skipResponseBody = true; return this;} // useful for HEAD responses

    public HttpParser filter (HttpMessageFilter f){ _filter = f; return this;}
    
    public void reset (){
	message = null;
	readHeaders = false;
	readFirstLine = FLState.HEAD;
	readWholeFirstLine = false;
	hName = null;
	state = HState.HNAME;
	syntax = syntaxHeaderName;
	contentLen = -1;
    }

    public HttpMessageImpl parseMessage (ByteBuffer buffer)
    {
	try{
	    return parseFirstLine (buffer) ? (parseHeaders(buffer) ? readBody(buffer) : null) : null;
	} catch (RuntimeException re){
	    throw re;
	} catch (Throwable t){
	    throw new RuntimeException (t);
	}
    }

    private boolean parseFirstLine (ByteBuffer buffer) throws Exception
    {
	if (readWholeFirstLine) return true;
	if (message == null){
	    message = new HttpMessageImpl ();
	}
	InputStream in = message.setInputByteBuffer (buffer);
	whileloop: while (true){
	    switch (tok.read (in, readFirstLine == FLState.HEAD ? syntaxFirstLineUpperCase : syntaxFirstLine)){
	    case MyStreamTokenizer.READ_WORD:
		parsingFL : switch (readFirstLine){
		case HEAD:
		    _filter.init (message); // do not init before actual message content is here (else we init a not-yet existing message)
		    String method = new String (tok.getWord (), 0, tok.getWordLength ());
		    tok.resetWord ();
		    if (method.startsWith ("HTTP/1.")){
			message.setIsRequest (false);
			readFirstLine = FLState.CENTER;
		    } else if (method.equals ("PRI")) {
			throw new Http2Exception (message.startHeaders ().toByteBuffers (true, false, false));
		    } else {
			message.setMethod (method);
			_filter.method (message, method);
			readFirstLine = FLState.CENTER;
		    }
		    break parsingFL;
		case CENTER:
		    if (message.isRequest ()){
			String url = getWord (true);
			message.setURL (url);
			_filter.url (message, url);
		    } else{
			message.setStatus (StreamTokenizerUtils.getWordAsInt (tok, "Invalid Status code"));
			_filter.status (message, message.getStatus ());
		    }
		    readFirstLine = FLState.TAIL;
		    break parsingFL;
		case TAIL:
		    if (message.isRequest ()){
			String version = new String (tok.getWord (), 0, tok.getWordLength ());		    
			switch (version){
			case "HTTP/1.0":
			case "HTTP/1.1":
			    readFirstLine = FLState.DONE;
			    break parsingFL;
			default:
			    throw new IOException ("Invalid HTTP version");
			}
		    }else{ // this is the reason - dont care
			readFirstLine = FLState.DONE;
			break parsingFL;
		    }
		case DONE:
		    if (message.isRequest ()) throw new IOException ("Invalid First Line");
		}
		continue whileloop;
	    case MyStreamTokenizer.READ_EOL:
		if (readFirstLine != FLState.DONE) throw new IOException ("Invalid First Line");
		readWholeFirstLine = true;
		message.startHeaders ();
		tok.resetWord ();
		break whileloop;
	    case MyStreamTokenizer.READ_EOF:
		break whileloop;
	    }
	}
	return readWholeFirstLine;		
    }
	 
    private boolean parseHeaders (ByteBuffer buffer) throws Exception
    {
	if (readHeaders) return true;
	InputStream in = message.setInputByteBuffer (buffer);
	whileloop : while (true){
	    int i;
	    switch (i = tok.read (in, syntax)){
	    case MyStreamTokenizer.READ_WORD:
		switch (state){
		case HNAME:
		    if (hName != null)
			readHeader ();
		    hName = getWord (true);
		    state = HState.HSEP;
		    syntax = syntaxHeaderDelim;
		    continue whileloop;
		case HSEP: throw new IOException ("Invalid header name");
		case HVALUE:
		    getWord (false);
		    continue whileloop;
		}
	    case MyStreamTokenizer.READ_EOL:
		switch (state){
		case HNAME:
		    if (hName != null)
			readHeader ();
		    readHeaders = true;
		    break whileloop;
		case HSEP: throw new IOException ("Invalid header name "+hName);
		case HVALUE:
		    syntax = syntaxHeaderName;
		    state = HState.HNAME;
		    continue whileloop;
		}
	    case MyStreamTokenizer.READ_EOF:
		break whileloop;
	    case ':':
		if (state == HState.HNAME) throw new IOException ("Invalid header name");
		message.readHeaderSep ();
		syntax = syntaxHeaderValue;
		state = HState.HVALUE;
		continue whileloop;
	    case ' ':
	    case '\t':
		if (state == HState.HSEP) continue whileloop; // check if possible since declared as space
		if (hName == null)
		    // may happen if the first header line starts with ' '
		    throw new IOException ("Invalid header parsing state");
		syntax = syntaxHeaderValue;
		state = HState.HVALUE;
		continue whileloop;
	    }
	}
	if (readHeaders){
	    if (contentLen == -1){
		String tenc = message.getHeaderValue ("transfer-encoding");
		_chunked = tenc != null && tenc.toLowerCase ().indexOf ("chunked") != -1;
		if (!_chunked){
		    // if no content-length --> assume 0 - the agent will check if OK
		    contentLen = 0;
		    bodyLeftToRead = 0;
		    return true;
		}
		bodyLeftToRead = -1;
		chunkState = ChunkState.SIZE;
		tok.resetWord ();
	    } else {
		_chunked = false;
		if (_skipResponseBody) bodyLeftToRead = 0;
		else bodyLeftToRead = contentLen;
	    }
	    if (_chunked) message.setIsChunked ();
	}
	return readHeaders;
    }

    private void readHeader () throws IOException {
	if (hName.equals ("content-length")){
	    // cannot filter content-length - not planned
	    HttpMessage.Header header = message.readHeader (hName);
	    contentLen = header.getValueAsInt (-1);
	    if (contentLen < 0)
		throw new IOException ("Invalid Content-Length : "+header.getValue ());
	} else {	    
	    if (_filter.header (message,
				hName,
				_peepHeader
				))
		message.readHeader (hName);
	    else
		message.ignoreHeader (hName);
	}
	hName = null;
    }

    private String getWord (boolean interested){
	if (interested) return StreamTokenizerUtils.getWordTrimmed (tok);
	else {
	    tok.resetWord ();
	    return null;
	}
    }
    
    private HttpMessageImpl readBody (ByteBuffer buffer) throws Exception
    {
	int leftToRead = bodyLeftToRead;
	if (_chunked){
	    if (chunkState == ChunkState.SIZE)
		readChunkSize (buffer); // may change the chunkState
	    if (_skipChunkDelimiters){
		int available = buffer.remaining ();
		switch (chunkState){
		case DATA_CR:
		    if (available >= 2){
			buffer.position (buffer.position () + 2);
			chunkState = ChunkState.SIZE;
			return readBody (buffer);
		    }
		    if (available == 1){
			buffer.position (buffer.position () + 1);
			message.setBody (VOID);
			chunkState = ChunkState.DATA_LF;
			return returnMessage (false, 0);
		    }
		    if (available == 0){
			message.setBody (VOID);
			return returnMessage (false, 0);
		    }
		case DATA_LF:
		    if (available >= 1){
			buffer.position (buffer.position () + 1);
			chunkState = ChunkState.SIZE;
			return readBody (buffer);
		    }
		    if (available == 0){
			message.setBody (VOID);
			return returnMessage (false, 0);
		    }
		case LAST_CR:
		    if (available >= 2){
			buffer.position (buffer.position () + 2);
			message.setBody (VOID);
			return returnMessage (true, 0);
		    }
		    if (available == 1){
			buffer.position (buffer.position () + 1);
			message.setBody (VOID);
			chunkState = ChunkState.LAST_LF;
			return returnMessage (false, 0);
		    }
		    if (available == 0){
			message.setBody (VOID);
			return returnMessage (false, 0);
		    }
		case LAST_LF:
		    if (available >= 1){
			buffer.position (buffer.position () + 1);
			message.setBody (VOID);
			return returnMessage (true, 0);
		    }
		    if (available == 0){
			message.setBody (VOID);
			return returnMessage (false, 0);
		    }
		}
	    }
	    leftToRead = chunkLeftToRead;
	} else {
	    if (leftToRead == 0){
		message.setBody (VOID);
		return returnMessage (true, 0);
	    }
	}
	int available = buffer.remaining ();
	int readSize = Math.min(available, leftToRead);
	boolean done = false;
	//System.out.println ("available="+available+", readSize="+readSize+", bodyLeftToRead="+leftToRead);
	if (readSize > 0){
	    byte[] body =  new byte[readSize];
	    buffer.get(body);
	    message.setBody (body);
	    if (_chunked){
		chunkLeftToRead -= readSize;
		if (chunkLeftToRead == 0){
		    if (_skipChunkDelimiters){
			if (chunkState == ChunkState.DATA)
			    chunkState = ChunkState.DATA_CR;
		    } else {
			if (chunkState == ChunkState.DATA)
			    chunkState = ChunkState.SIZE;
			else if (chunkState == ChunkState.LAST)
			    done = true;
		    }
		}
	    } else{
		bodyLeftToRead -= readSize;
		done = bodyLeftToRead == 0;
	    }
	} else {
	    message.setBody (VOID);
	}
	return returnMessage (done, readSize);
    }
    private void readChunkSize (final ByteBuffer buffer) throws Exception {
	InputStream in = new InputStream (){
		public int read() throws IOException {
		    if (buffer.hasRemaining())
			return buffer.get() & 0xFF;
		    return -1;
		}};
	int pos = buffer.position ();
	int i;
	whileloop : while (true){
	    switch (i = tok.read (in, syntaxChunk)){
	    case MyStreamTokenizer.READ_WORD:
		continue whileloop;
	    case MyStreamTokenizer.READ_EOL:
		switch (chunkState){
		case SIZE:
		    String size = getWord (true);
		    try{ chunkLeftToRead = Integer.parseInt (size, 16);}
		    catch(Exception e){
			throw new IOException ("Invalid Chunk Size : "+size);
		    }
		    if (chunkLeftToRead < 0) throw new IOException ("Invalid Chunk Size : "+size);
		    if (_skipChunkDelimiters){
			chunkState = chunkLeftToRead == 0 ? ChunkState.LAST_CR : ChunkState.DATA;
		    } else {
			chunkState = chunkLeftToRead == 0 ? ChunkState.LAST : ChunkState.DATA;
			chunkLeftToRead += buffer.position () - pos + 2; // TRAIL CRLF
			buffer.position (pos);
		    }
		    return;
		}
	    case MyStreamTokenizer.READ_EOF:
		if (_skipChunkDelimiters){
		    chunkLeftToRead = 0;
		} else {
		    chunkLeftToRead = buffer.position () - pos;
		    buffer.position (pos);
		}
		return;
	    }
	}
    }
    private static void restore (HttpMessageImpl message, ByteBuffer buffer, int from){
	int now = buffer.position ();
	buffer.position (from);
	byte[] data = new byte[now - from];
	buffer.put (data);
	message.setBody (data);
    }
    private HttpMessageImpl returnMessage (boolean done, int readBody){
	if (message.isFirst () == false && readBody == 0 && !done)
	    // it is an intermediary with no body
	    return null;
	try{
	    return message.done (done);
	}finally{
	    if (done) reset ();
	}
    }

    // returns the status for a response
    // called on msg from agent - hence reliable <-- important since checks are skipped for perfs
    // returns null if this is not the beginning of a response
    public static String getStatus (ByteBuffer[] msg){
	ByteBuffer buff = msg[0];
	byte[] array = buff.array ();
	int pos = buff.position ();
	if (buff.remaining () < 12) return null;
	if (array[pos] != (byte)'H' ||
	    array[pos+1] != (byte)'T' ||
	    array[pos+2] != (byte)'T' ||
	    array[pos+3] != (byte)'P' ||
	    array[pos+4] != (byte)'/' ||
	    array[pos+5] != (byte)'1' ||
	    array[pos+6] != (byte)'.'
	    ) return null;
	return getFromUTF8 (array, pos+9, 3);
    }
    
    public static java.nio.charset.Charset UTF_8 = null;
    static {
	try{
	    UTF_8 = java.nio.charset.Charset.forName ("utf-8");
	}catch(Exception e){}
    }
    
    public static byte[] getUTF8 (String value){
	return value.getBytes (UTF_8);
    }
    public static String getFromUTF8 (byte[] bytes, int off, int len){
	return new String (bytes, off, len, UTF_8);
    }
    
    public static void main (String[] a) throws Exception {
	BufferedReader r = new BufferedReader (new FileReader (a[0]));
	String s = null;
	StringBuilder sb = new StringBuilder ();
	while ((s=r.readLine ())!=null) sb.append (s).append ("\r\n");
	r.close ();
	//System.out.println (sb.toString ());
	byte[] http = sb.toString ().getBytes ("utf-8");
	
	HttpMessageImpl msg = null;
	HttpParser parser = new HttpParser ();
	long last = System.currentTimeMillis ();
	int step = 200000;
	for (int i=1; i<10000000; i++){
	    msg = parser.parseMessage (ByteBuffer.wrap (http));
	    //String via = msg.getHeaderValue ("via");
	    //String cl = msg.getHeaderValue ("content-length");
	    //for (int j=0; j<10; j++) {String from = msg.getHeader ("from").getValueAsUri ();}
	    if (i%step == 0){
		long now = System.currentTimeMillis ();
		//System.out.println (((now - last)*1000)/step+" for 1000 parsings / "+(1000*step)/(now-last)+" parsings per sec");
		System.out.println ((1000*step)/(now-last)+" parsings per sec");
		last = now;
	    }
	}
	System.out.println ("Parsed :\n["+msg+"]");
	
	if (true) return;
	ByteBuffer buff = ByteBuffer.allocate (10000);
	for (int i = 0; i<http.length; i++){
	    //System.out.println ("read new char:"+(char)http[i]+" "+http[i]);
	    buff.put (http[i]);
	    buff.flip ();
	    msg = parser.parseMessage (buff);
	    buff.clear ();
	}
	System.out.println (msg);
    }

    public static class Http2Exception extends RuntimeException {
	private ByteBuffer[] _read;
	public Http2Exception (ByteBuffer[] read){ super (); _read = read;}
	public ByteBuffer[] getReadBytes (){ return _read;}
    }

    // return hostName, portNb, indexOfStartPath
    public static Object[] parseURL (String url, boolean hasScheme){
	int len = url.length ();
	int index = 0;
	if (hasScheme){ // skip scheme
	    index = url.indexOf ("://");
	    if (index == -1) return null;
	    index+=3;
	}
	int start = index;
	while (index < len){
	    char c = url.charAt (index);
	    if (c == ':' || c == '/') break;
	    if (c == '@'){
		start = index+1;
		if (start == len) return null;
	    }
	    index++;
	}
	if (start == index) return null;
	int[] port = index < len ? parsePort (url, index+1) : new int[]{80, index};
	if (port == null) return null;
	return new Object[]{url.substring (start, index), port[0], port[1]};
    }
    private static int[] parsePort (String url, int from){
	int port = 0;
	while (from < url.length ()){
	    char c = url.charAt (from);
	    if (c < '0' || c > '9'){
		break;
	    }
	    port*=10;
	    port+=(int) (c - '0');
	    from++;
	}
	if (port > 0xFFFF) return null;
	if (port == 0) port = 80;
	return new int[]{port, from};
    }
}
