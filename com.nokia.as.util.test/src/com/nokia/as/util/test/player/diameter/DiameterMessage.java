// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.util.test.player.diameter;

import java.util.*;
import java.net.*;
import java.nio.*;

public class DiameterMessage {

    public static enum Type {
	Capabilities, Watchdog, Disconnection, Application
    }

    private Type _type;
    private byte[] _bytes;
    private int _bytesOffset;
    private boolean _req;

    public DiameterMessage (byte[] content){
	_bytes = content;
	parse ();
    }

    public DiameterMessage (byte[] bufLen, int size){
	_bytes = new byte[size];
	System.arraycopy (bufLen, 0, _bytes, 0, _bytesOffset = bufLen.length);
    }

    public DiameterMessage check (){
	if (_bytes[0] != (byte)1) throw new RuntimeException ("Invalid message: Diameter version is : "+(_bytes[0]&0xFF));
	return this;
    }

    public boolean append(ByteBuffer buffer)
    {
        int n = Math.min(buffer.remaining(), _bytes.length - _bytesOffset);
        buffer.get(_bytes, _bytesOffset, n);
        _bytesOffset += n;
	if (_bytesOffset == _bytes.length){
	    parse ();
	    return true;
	} else
	    return false;
    }

    public boolean isRequest (){
	return _req;
    }

    private void parse (){
	if (getApplicationID () == 0){
	    switch (getCommandCode ()){
	    case 257: _type = Type.Capabilities; break;
	    case 280: _type = Type.Watchdog; break;
	    case 282: _type = Type.Disconnection; break;
	    default:
		_type = Type.Application;
	    }
	} else
	    _type = Type.Application;
	_req = ((_bytes[4] & 0x80) == 0x80);
    }

    public Type getType (){
	return _type;
    }

    public boolean isTwin (DiameterMessage other){
	if (_type != other._type) return false;
	for (int i=12; i<16; i++)
	    if (_bytes[i] != other._bytes[i]) return false;
	for (int i=16; i<20; i++)
	    if (_bytes[i] != other._bytes[i]) return false;
	return true;
    }

    public byte[] getBytes (){
	return _bytes;
    }

    public int getHopIdentifier (){
	return getIntValue (_bytes, 12, 4);
    }

    public int getEndIdentifier (){
	return getIntValue (_bytes, 16, 4);
    }

    public int getCommandCode (){
	return getIntValue (_bytes, 5, 3);
    }

    public long getApplicationID (){
	return getLongValue (_bytes, 8, 4);
    }

    public int getResultCode (){
	byte[] avp = getAvp (268, 0);
	if (avp == null) return -1;
	return getIntValue (avp, 0, 4);
    }
    
    public byte[] getAvp (int code, int vid){
	int off = 20;
	while (off+8 < _bytes.length){
	    //note that we assume that code and vendorid are unsigned int (not long) which seems realistic
	    int avpcode = getIntValue (_bytes, off, 4);
	    int avplen = getIntValue (_bytes, off+5, 3);
	    boolean hasvid = (_bytes[off+4] & 0x80) == 0x80;
	    if (avplen < (hasvid ? 12 : 8)) return null; // malformed avp
	    int reallen = avplen;
	    switch (avplen%4){
	    case 1: reallen+=3; break;
	    case 2: reallen+=2; break;
	    case 3: reallen+=1; break;
	    }
	    int nextOffset = off+reallen;
	    if (nextOffset > _bytes.length) // this is a protection against malformed avp: if reallen exceeds data size
		return null;
	    if (avpcode != code){
		off = nextOffset;
		continue;
	    }
	    int avpvid = hasvid ? getIntValue (_bytes, off+8, 4) : 0;
	    if (avpvid != vid){
		off = nextOffset;
		continue;
	    }
	    int valueOff = hasvid ? 12 : 8;
	    byte[] value = new byte[avplen - valueOff];
	    System.arraycopy (_bytes, off+valueOff, value, 0, value.length);
	    return value;
	}
	return null;
    }

    public static int getIntValue (byte[] bytes, int off, int len){
	int value = bytes[off++] & 0xFF;
	for (int i=1; i<len; i++){
	    value <<= 8;
	    value |= bytes[off++] & 0xFF;
	}
	return value;
    }
    public static long getLongValue (byte[] bytes, int off, int len){
	return ((long)getIntValue (bytes, off, len))&0xFFFFFFFFL;
    }

    public String toString (){
	return new StringBuilder ()
	    .append (isRequest () ? "Request[" : "Response[")
	    .append (_type)
	    .append (",hopId=")
	    .append (getHopIdentifier ())
	    .append (",len=")
	    .append (_bytes.length)
	    .append (']')
	    .toString ();
    }
}
