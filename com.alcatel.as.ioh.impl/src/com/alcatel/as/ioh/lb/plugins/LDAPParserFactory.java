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
import org.apache.log4j.Logger;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ConfigurationPolicy;

@Component(immediate=true, configurationPolicy = ConfigurationPolicy.OPTIONAL, property={"parser.id=ldap"})
public class LDAPParserFactory implements ParserFactory {
    
    public static final Logger LOGGER = Logger.getLogger ("as.ioh.lb.ldap");

    public static final String PROP_LDAP_BUFFER = "parser.ldap.buffer";
    
    @Activate
    public void start (){
    }

    public Object newParserConfig (Map<String, Object> props){
	return new LDAPParserConfig (props);
    }

    public Parser newParser (Object config, int neededBuffer){
	if (neededBuffer == 0)
	    return new StreamingLDAPParser ((LDAPParserConfig) config);
	if (neededBuffer == -1)
	    return new BufferedLDAPParser ((LDAPParserConfig) config);
	throw new IllegalArgumentException ();
    }
    
    public String toString (){ return "LDAPParserFactory[id=ldap]";}

    protected static class LDAPParserConfig {
	
	protected LDAPParserConfig (Map<String, Object> props){
	}
    }

    private static class StreamingLDAPParser implements Parser {

	private int _needed = 0;
	private int _parsingIndex = 0;
	private boolean _parsed;
	private boolean _newMessage = true;
	private int _neededLen = -1;

	protected StreamingLDAPParser (LDAPParserConfig config){
	}
	
	public Chunk parse (java.nio.ByteBuffer buffer){
	    int remaining = buffer.remaining ();
	    if (remaining == 0) return null;

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
		    _neededLen = -1;
		}
		return chunk;
	    }
	    
	    // parsing length
	    ByteBuffer duplicate = buffer.duplicate ();
	    int origPos = buffer.position ();
	    for (int i=0; i<remaining; i++){
	    	int v = buffer.get () & 0xFF;
	    	if (_parsingIndex++ == 1){
	    	    if ((v & 0x7F) == v){
	    		_needed = v;
	    		_neededLen = 0;
			break;
	    	    } else {
			_needed = 0;
	    		_neededLen = v & 0x7F;
			if (_neededLen == 0) throw new RuntimeException ("Illegal length : specified length is invalid : "+_neededLen+" bytes");
			if (_neededLen > 4) throw new RuntimeException ("Illegal length : specified length is too large : "+_neededLen+" bytes");
		    }
		} else if (_neededLen > 0){
	    	    _neededLen--;
		    _needed = _needed << 8;
		    _needed |= v;
		    if (_neededLen == 0) break;
	    	}
	    }
	    if (_neededLen == 0){
		// done parsing len
		_parsed = true;
		int size = Math.min (buffer.remaining (), _needed); // dont use variable remaining ! since it changed since then.
		buffer.position (buffer.position () + size);
		int diffPos = buffer.position () - origPos;
		duplicate.limit (duplicate.position () + diffPos);
		_needed -= size;
		Chunk chunk = new Chunk (_newMessage).setData (duplicate, false);
		if (_needed == 0){
		    _parsed = false;
		    _newMessage = true;
		    _parsingIndex = 0;
		    _neededLen = -1;
		} else {
		    _newMessage = false;
		}
		return chunk;
	    }
	    // len not yet fully parsed
	    Chunk chunk = new Chunk (_newMessage).setData (duplicate, false);
	    _newMessage = false;
	    return chunk;
	}
	
    }

    
    private static class BufferedLDAPParser implements Parser {

	private ByteBuffer _cache = null;
	private int _index = -1;
	private int _needed = -1;

	protected BufferedLDAPParser (LDAPParserConfig config){
	}
	
	public Chunk parse (java.nio.ByteBuffer buffer){
	    if (_cache != null){
		// _cache is in read mode - need to set it to write mode
		_cache.compact (); // cannot do it at the end of parseCache since the returned chunck points to its data
		if (_cache.remaining () < buffer.remaining ()){
		    ByteBuffer tmp = ByteBuffer.allocate ((_cache.capacity () + buffer.remaining ())*2);
		    _cache.flip (); // put in read mode
		    tmp.put (_cache);
		    _cache = tmp;
		}
		_cache.put (buffer);
		_cache.flip (); // put it back to read mode
		return parseCache ();
	    }

	    if (buffer.remaining () == 0) return null;
	    // check if the buffer suffices
	    int orig_pos = buffer.position ();
	    int len = getMessageLength (buffer, true);
	    if (len == -1)
		return initCache (buffer);
	    if (buffer.remaining () >= len){
		// it suffices !
		// set the type
		int orig_limit = buffer.limit ();
		int new_limit = buffer.position () + len;
		ChunkInfo info = new ChunkInfo ();
		int id = getInteger (buffer);
		info._type = buffer.get () & 0xFF;
		buffer.position (orig_pos); // rewind
		buffer.limit (new_limit); // set this msg limit
		ByteBuffer duplicate = buffer.duplicate ();
		buffer.position (new_limit); // move forward
		buffer.limit (orig_limit); // reset orig limit
		return new Chunk (true).setData (duplicate, false).attach (info).setDescription (getType (info._type)).setId (id);
	    }
	    buffer.position (orig_pos);
	    return initCache (buffer);
	}

	private Chunk initCache (ByteBuffer buffer){
	    _cache = ByteBuffer.allocate (buffer.remaining () * 2);
	    _cache.put (buffer);
	    _cache.flip (); // must be in read mode in parse()
	    return null; // this is a trick to return null in parse()
	}
	private Chunk parseCache (){
	    // _cache is in read mode
	    int orig_pos = _cache.position ();
	    int len = getMessageLength (_cache, true);
	    if (len == -1)
		return null;	    
	    if (_cache.remaining () >= len){
		// it suffices !
		// set the type & id
		int orig_limit = _cache.limit ();
		int new_limit = _cache.position () + len;
		ChunkInfo info = new ChunkInfo ();
		int id = getInteger (_cache);
		info._type = _cache.get () & 0xFF;
		_cache.position (orig_pos);
		_cache.limit (new_limit);
		ByteBuffer duplicate = _cache.duplicate ();
		_cache.position (new_limit);
		_cache.limit (orig_limit); // _cache will be compacted in next parse()
		if (_cache.remaining () == 0)
		    _cache = null; // remove use of cache
		return new Chunk (true).setData (duplicate, false).attach (info).setDescription (getType (info._type)).setId (id);
	    }
	    _cache.position (orig_pos);
	    return null;
	}
	
    }
    
    /********************** Generic LDAP stuff  **************/

    public static class ChunkInfo {
	public int _type;
	public String toString (){ return "type="+Integer.toHexString (_type);}
    }
    
    public static String getType (int type){
	switch (type){
	case 0x60 : return "Bind Request Protocol Op"; 
	case 0x61 : return "Bind Response Protocol Op"; 
	case 0x42 : return "Unbind Request Protocol Op"; 
	case 0x63 : return "Search Request Protocol Op"; 
	case 0x64 : return "Search Result Entry Protocol Op"; 
	case 0x65 : return "Search Result Done Protocol Op"; 
	case 0x66 : return "Modify Request Protocol Op"; 
	case 0x67 : return "Modify Response Protocol Op"; 
	case 0x68 : return "Add Request Protocol Op"; 
	case 0x69 : return "Add Response Protocol Op";
	case 0x4a : return "Delete Request Protocol Op"; 
	case 0x6b : return "Delete Response Protocol Op"; 
	case 0x6c : return "Modify DN Request Protocol Op"; 
	case 0x6d : return "Modify DN Response Protocol Op"; 
	case 0x6e : return "Compare Request Protocol Op"; 
	case 0x6f : return "Compare Response Protocol Op"; 
	case 0x50 : return "Abandon Request Protocol Op"; 
	case 0x73 : return "Search Result Reference Protocol Op"; 
	case 0x77 : return "Extended Request Protocol Op"; 
	case 0x78 : return "Extended Response Protocol Op"; 
	case 0x79 : return "Intermediate Response Protocol Op";
	default: return "UNKNOWN : 0x"+Integer.toHexString (type);
	}
    }

    public static int getInteger (ByteBuffer data){
	int b = data.get () & 0xFF;
	if (b != 0x02) throw new IllegalArgumentException ("Failed to parse Integer : type is 0x"+Integer.toHexString (b));
	b = data.get () & 0xFF; // length of the Integer
	int id = 0;
	for (int i=0; i<b; i++){
	    id <<= 8;
	    id |= data.get () & 0xFF;
	}
	return id;
    }
    // if there is not enough : returns -1 and resets position
    public static int getMessageLength (ByteBuffer data, boolean readSequenceTag){
	int orig = data.position ();
	if (readSequenceTag){
	    if (data.remaining () < 2) return -1;
	    int seq = data.get () & 0xFF;
	    if (seq != 0x30) throw new IllegalArgumentException ("Invalid SEQUENCE tag : "+Integer.toHexString (seq));
	} else {
	    if (data.remaining () == 0) return -1;
	}
	int b = data.get () & 0xFF;
	boolean mbytes = ((b & 0x80) == 0x80);
	if (mbytes){
	    int count = b & 0x7F;
	    if (count == 0 || count  > 4)
		throw new IllegalArgumentException ("Failed to parse Length : start with 0x"+Integer.toHexString (b));
	    if (data.remaining () < count){
		data.position (orig);
		return -1;
	    }
	    int l = 0;
	    for (int i=0; i<count; i++){
		l <<= 8;
		l |= data.get () & 0xFF;
	    }
	    return l;
	} else
	    return b;
    }
    
}
