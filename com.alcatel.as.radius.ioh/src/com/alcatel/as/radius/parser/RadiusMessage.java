// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.radius.parser;

import java.util.*;
import java.net.*;
import java.nio.*;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.reactor.*;

public class RadiusMessage {
    
    private InetSocketAddress _from;
    private byte[] _bytes;
    
    public RadiusMessage (InetSocketAddress from, ByteBuffer content) throws Exception {
	_from = from;
	_bytes = new byte[content.remaining ()];
	content.get (_bytes);
	parse ();
    }

    private void parse () throws Exception {
	if (_bytes.length < 20 || _bytes.length > 4096)
	    throw new Exception ("Invalid packet length : "+_bytes.length);
	int len = getLength ();
	if (_bytes.length < len)
	    throw new Exception ("Incoherent packet length : "+len+" > "+_bytes.length);
    }
    
    public int getCode (){ return _bytes[0] & 0xFF;}
    public int getIdentifier (){ return _bytes[1] & 0xFF;}
    public int getLength (){
	int l = _bytes[2] & 0xFF;
	l <<= 8;
	l |= _bytes[3] & 0xFF;
	return l;
    }
    
    public byte[] getBytes (){
	return _bytes;
    }
    public InetSocketAddress getFromAddress (){ return _from;}

    public ByteBuffer toByteBuffer (){ return ByteBuffer.wrap (_bytes);}

    public boolean isRequest (){
	switch (getCode ()){
	case 1: return true;
	case 4: return true;
	case 40: return true;
	case 43 : return true;
	}
	return false;
    }

}
