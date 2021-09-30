package com.alcatel.as.ioh.lb.plugins;

import com.alcatel.as.ioh.lb.*;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.nio.*;

import com.alcatel.as.ioh.client.TcpClient.Destination;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ConfigurationPolicy;

@Component(immediate=true, configurationPolicy = ConfigurationPolicy.OPTIONAL, property={"parser.id=token"})
public class TokenParserFactory implements ParserFactory {

    public static final String PROP_TOKEN_SEP = "parser.token.separator";
    public static final String PROP_TOKEN_BUFFER = "parser.token.buffer";

    @Activate
    public void start (){
    }

    public Object newParserConfig (Map<String, Object> props){
	return new TokenParserConfig (props);
    }

    public Parser newParser (Object config, int neededBuffer){
	if (neededBuffer != 0 && neededBuffer != -1)
	    throw new RuntimeException (this+" : neededBuffer="+neededBuffer+" : not supported");
	return new TokenParser ((TokenParserConfig) config, neededBuffer);
    }

    public String toString (){ return "TokenParserFactory[id=token]";}

    protected static class TokenParserConfig {

	private String _word;
	private int _wordLen, _buffLen;
	
	protected TokenParserConfig (Map<String, Object> props){
	    _word = (String) props.get (PROP_TOKEN_SEP);
	    if (_word == null){
		_word = "#"; // the legacy default value
	    } else {
		_word = _word.replace ("\\n", "\n");
		_word = _word.replace ("\\r", "\r");
		_word = _word.replace ("\\t", "\t");
	    }
	    _wordLen = _word.length ();

	    String s = (String) props.get (PROP_TOKEN_BUFFER);
	    if (s == null) s = "10000"; // 10K by def : max message size
	    _buffLen = Integer.parseInt (s);
	}
    }
    
    protected static class TokenParser implements Parser {

	private ByteBuffer _buffer;
	private String _word;
	private int _wordLen, _wordPos;
	private boolean _newMessage = true;
	
	protected TokenParser (TokenParserConfig config, int neededBuffer){
	    _word = config._word;
	    _wordLen = config._wordLen;
	    
	    if (neededBuffer == -1){ // need to buffer all message --> allocate local cache
		_buffer = ByteBuffer.allocate (config._buffLen);
	    }
	}

	public Chunk parse (java.nio.ByteBuffer buffer){
	    if (_buffer != null) return parseFull (buffer);

	    // parse chunk - incomplete
	    int remaining = buffer.remaining ();
	    if (remaining == 0) return null;
	    ByteBuffer duplicate = (ByteBuffer) buffer.duplicate ();
	    for (int i=0; i<remaining; i++){
		byte b = buffer.get ();
		char c = (char) (b & 0xFF);
		if (_word.charAt (_wordPos) == c){
		    _wordPos++;
		    if (_wordPos == _wordLen){
			_wordPos = 0;
			duplicate.limit (buffer.position ());
			Chunk chunk = new Chunk (_newMessage).setData (duplicate, false);
			_newMessage = true;
			return chunk;
		    }
		} else {
		    // reset : but c maybe the first char !, so dont reset to 0 too quickly
		    if (_word.charAt (0) == c) _wordPos = 1;
		    else _wordPos = 0;
		}
	    }
	    Chunk chunk = new Chunk (_newMessage).setData (duplicate, false);
	    _newMessage = false;
	    return chunk;
	}

	private Chunk parseFull (java.nio.ByteBuffer buffer){
	    int remaining = buffer.remaining ();
	    for (int i=0; i<remaining; i++){
		byte b = buffer.get ();
		if (_buffer.hasRemaining ())
		    _buffer.put (b);
		else {
		    throw new RuntimeException ("Buffer exceeded");
		}
		char c = (char) (b & 0xFF);
		if (_word.charAt (_wordPos) == c){
		    _wordPos++;
		    if (_wordPos == _wordLen){
			_buffer.flip ();
			_wordPos = 0;
			return new Chunk (true).setData (_buffer, false);
		    }
		} else {
		    // reset : but c maybe the first char !, so dont reset to 0 too quickly
		    if (_word.charAt (0) == c) _wordPos = 1;
		    else _wordPos = 0;
		}
	    }
	    return null;
	}
    }
    
}
