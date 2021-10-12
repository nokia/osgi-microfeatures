// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

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

@Component(immediate=true, configurationPolicy = ConfigurationPolicy.OPTIONAL, property={"parser.id=length"})
public class LengthParserFactory implements ParserFactory {

    public static final String PROP_LENGTH_VALUE = "parser.length.value";
    public static final String PROP_LENGTH_OFF = "parser.length.off";
    public static final String PROP_LENGTH_LEN = "parser.length.len";
    public static final String PROP_LENGTH_ADD = "parser.length.add";
    
    @Activate
    public void start (){
    }

    public Object newParserConfig (Map<String, Object> props){
	return new LengthParserConfig (props);
    }

    public Parser newParser (Object config, int neededBuffer){
	switch (neededBuffer){
	case -1:
	    throw new RuntimeException (this+" : neededBuffer="+neededBuffer+" : not supported");
	case 0:
	    return new LengthParser ((LengthParserConfig) config);
	default:
	    return new StreamingParserBuffer (new LengthParser ((LengthParserConfig) config), neededBuffer);
	}
    }
    
    public String toString (){ return "LengthParserFactory[id=length]";}

    private static final int getInt (String key, Map<String, Object> props, int def){
	String s = (String) props.get (key);
	if (s == null) return def;
	return Integer.parseInt (s);
    }

    protected static class LengthParserConfig {

	private boolean _fixed; // abs or variable
	private int _abs, _off, _len, _add;
	private int[] _coef;
	
	protected LengthParserConfig (Map<String, Object> props){
	    _abs = getInt (PROP_LENGTH_VALUE, props, -1);
	    _fixed = (_abs > 0);

	    if (!_fixed){
		_off = getInt (PROP_LENGTH_OFF, props, -1);
		_len = getInt (PROP_LENGTH_LEN, props, -1);
		_add = getInt (PROP_LENGTH_ADD, props, 0);
		if (_off == -1 || _len == -1)
		    throw new RuntimeException ("Invalid LengthParser properties : need offset and length properties");
		_coef = new int[_off + _len];
		for (int i=0; i<_off; i++) _coef[i] = 0;
		_coef[_off + _len -1] = 1;
		for (int i=(_off + _len -2); i >= _off; i--)
		    _coef[i] = _coef[i+1] * 256;
	    }
	}
    }

    protected static class LengthParser implements Parser {

	private boolean _fixed; // config : abs or variable
	private int _abs, _off, _len, _add; // config
	private int[] _coef; // config
	private int _needed; // dynamic
	private boolean _newMessage = true; // dynamic
	// for variable parsing
	private boolean _parsed;
	private int _parsingIndex;
	
	protected LengthParser (LengthParserConfig config){
	    _fixed = config._fixed;	    
	    if (_fixed){
		_abs = config._abs;
		_needed = _abs;
	    } else {
		_off = config._off;
		_len = config._len;
		_add = config._add;
		_coef = config._coef;
		_needed = 0;
		_parsed = false;
		_parsingIndex = 0;
	    }
	}

	public Chunk parse (java.nio.ByteBuffer buffer){
	    int remaining = buffer.remaining ();
	    if (remaining == 0) return null;
	    
	    if (_fixed){ // fixed length
		int size = Math.min (remaining, _needed);
		ByteBuffer duplicate = buffer.duplicate ();
		duplicate.limit (duplicate.position () + size);
		buffer.position (buffer.position () + size);
		_needed -= size;
		Chunk chunk = new Chunk (_newMessage).setData (duplicate, false);
		if (_needed == 0){
		    _needed = _abs;
		    _newMessage = true;
		} else {
		    _newMessage = false;
		}
		return chunk;
	    }

	    if (_parsed){ // length parsed
		int size = Math.min (remaining, _needed);
		ByteBuffer duplicate = buffer.duplicate ();
		duplicate.limit (duplicate.position () + size);
		buffer.position (buffer.position () + size);
		_needed -= size;
		Chunk chunk = new Chunk (_newMessage).setData (duplicate, false);
		if (_needed == 0){
		    _parsed = false;
		    _newMessage = true;
		    _parsingIndex = 0;
		} else {
		    _newMessage = false;
		}
		return chunk;
	    }
	    
	    // parsing length
	    ByteBuffer duplicate = buffer.duplicate ();
	    for (int i=0; i<remaining; i++){
		int v = buffer.get () & 0xFF;
		_needed += v * _coef[_parsingIndex];
		_parsingIndex++;
		if (_parsingIndex == _coef.length){
		    // done parsing len
		    _parsed = true;
		    _needed += _add;
		    // by def, we remove what we read
		    _needed -= _off+_len;
		    if (_needed < 0)
			throw new RuntimeException ("Illegal length : specified length is too small");
		    int size = Math.min (buffer.remaining (), _needed); // dont use variable remaining ! since it changed since then.
		    duplicate.limit (duplicate.position () + (i+1) + size);
		    buffer.position (buffer.position () + size);
		    _needed -= size;
		    Chunk chunk = new Chunk (_newMessage).setData (duplicate, false);
		    if (_needed == 0){
			_parsed = false;
			_newMessage = true;
			_parsingIndex = 0;
		    } else {
			_newMessage = false;
		    }
		    return chunk;
		}
	    }
	    // len not yet fully parsed
	    Chunk chunk = new Chunk (_newMessage).setData (duplicate, false);
	    _newMessage = false;
	    return chunk;
	}
	
    }

    public static void main (String[] s) throws Exception {

	Map<String, Object> props = new HashMap<> ();
	props.put (PROP_LENGTH_VALUE, "3");
	
	Parser parser = new LengthParserFactory ().newParser (props, 0);
	byte[] data1 = new byte[]{1};
	byte[] data2 = new byte[]{1, 2};
	byte[] data3 = new byte[]{1, 2, 3};
	byte[] data4 = new byte[]{1, 2, 3, 4};
	byte[] data5 = new byte[]{1, 2, 3, 4, 5};

	Chunk chunk = parser.parse (ByteBuffer.wrap (data5));
	assert chunk.newMessage ();
	assert chunk.getData ().remaining () == 3 ; // miss 0

	chunk = parser.parse (ByteBuffer.wrap (data1));
	assert chunk.newMessage ();
	assert chunk.getData ().remaining () == 1; // miss 2

	chunk = parser.parse (ByteBuffer.wrap (data1));
	assert !chunk.newMessage ();
	assert chunk.getData ().remaining () == 1; // miss 1

	chunk = parser.parse (ByteBuffer.wrap (data1));
	assert !chunk.newMessage ();
	assert chunk.getData ().remaining () == 1;

	props = new HashMap<> ();
	props.put (PROP_LENGTH_OFF, "1");
	props.put (PROP_LENGTH_LEN, "2");
	parser = new LengthParserFactory ().newParser (props, 0);
	byte[] data = new byte[]{1, 0, 3};
	chunk = parser.parse (ByteBuffer.wrap (data));
	assert chunk.newMessage ();
	assert chunk.getData ().remaining () == 3;
	chunk = parser.parse (ByteBuffer.wrap (data));
	assert chunk.newMessage ();
	assert chunk.getData ().remaining () == 3;
	data = new byte[]{1, 0, 5, 0, 0};
	chunk = parser.parse (ByteBuffer.wrap (data));
	assert chunk.newMessage ();
	assert chunk.getData ().remaining () == 5;

	data = new byte[]{1, 0};
	chunk = parser.parse (ByteBuffer.wrap (data));
	assert chunk.newMessage ();
	assert chunk.getData ().remaining () == 2;
	data = new byte[]{5, 0, 0};
	chunk = parser.parse (ByteBuffer.wrap (data));
	assert !chunk.newMessage ();
	assert chunk.getData ().remaining () == 3;

	data = new byte[]{1};
	chunk = parser.parse (ByteBuffer.wrap (data));
	assert chunk.newMessage ();
	assert chunk.getData ().remaining () == 1;
	data = new byte[]{0, 5, 0, 0};
	chunk = parser.parse (ByteBuffer.wrap (data));
	assert !chunk.newMessage ();
	assert chunk.getData ().remaining () == 4;

	data = new byte[]{1, 0, 5, 0, 0};
	chunk = parser.parse (ByteBuffer.wrap (data));
	assert chunk.newMessage ();
	assert chunk.getData ().remaining () == 5;

	data = new byte[300];
	data[1] = (byte)1;
	data[2] = (byte)44;
	chunk = parser.parse (ByteBuffer.wrap (data));
	assert chunk.newMessage ();
	assert chunk.getData ().remaining () == 300;
	chunk = parser.parse (ByteBuffer.wrap (data));
	assert chunk.newMessage ();
	assert chunk.getData ().remaining () == 300;

	parser = new LengthParserFactory ().newParser (props, 3);
	data = new byte[]{0};
	chunk = parser.parse (ByteBuffer.wrap (data));
	assert chunk == null;
	data = new byte[]{0};
	chunk = parser.parse (ByteBuffer.wrap (data));
	assert chunk == null;
	data = new byte[]{3};
	chunk = parser.parse (ByteBuffer.wrap (data));
	assert chunk != null;
	assert chunk.getData ().remaining () == 3;
	
    }
    
}
