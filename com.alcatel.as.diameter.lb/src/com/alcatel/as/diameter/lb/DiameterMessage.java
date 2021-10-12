// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.diameter.lb;

import java.util.*;
import java.net.*;
import java.nio.*;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.reactor.*;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public class DiameterMessage {

    public static enum Type {
	Capabilities, Watchdog, Disconnection, Application
    }

    private Type _type;
    private byte[] _bytes;
    private int _bytesOffset;
    private boolean _req;
    // if other fields are added --> dont forget them in clone()

    private DiameterMessage(){} // used for cloning only

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

    public DiameterMessage clone (){
	byte[] copy = new byte[_bytes.length];
	System.arraycopy (_bytes, 0, copy, 0, _bytes.length);
	DiameterMessage clone = new DiameterMessage ();
	clone._bytes = copy;
	clone._type = _type;
	clone._req = _req;
	clone._bytesOffset = _bytesOffset; // should not be used though...
	return clone;
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

    public int getLength (){ return _bytes.length; }

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
	return getIntAvp (268, 0, -1);
    }

    public DiameterMessage updateHopIdentifier (int hopId){
	DiameterUtils.setIntValue (hopId, _bytes, 12);
	return this;
    }
    public DiameterMessage updateIdentifiers (int hopId, int endId){
	DiameterUtils.setIntValue (hopId, _bytes, 12);
	DiameterUtils.setIntValue (endId, _bytes, 16);
	return this;
    }

    // sets the byte[] and updates the length
    public void updateContent (byte[] content){
	_bytes = content;
	DiameterUtils.setIntValue ((1 << 24) + _bytes.length, _bytes, 0); // 1 << 24 to restore the diameter version
    }

    public int[] indexOf (int code, int vid){
	return indexOf (code, vid, _bytes, 0, _bytes.length, true);
    }
    public byte[] getAvp (int code, int vid){
	return getAvp (code, vid, _bytes, 0, _bytes.length, true);
    }
    public int getIntAvp (int code, int vid, int def){
	int[] pos = indexOf (code, vid, _bytes, 0, _bytes.length, true);
	if (pos == null) return def;
	if (pos[3] != 4) return def;
	return getIntValue (_bytes, pos[2], 4);
    }
    public byte[] getGroupedAvp (int parentCode, int parentVid, int code, int vid){
	int[] pos = indexOf (parentCode, parentVid, _bytes, 0, _bytes.length, true);
	if (pos == null) return null;
	return getAvp (code, vid, _bytes, pos[2], pos[3], false);
    }
    public DiameterMessage removeValue (int off, int len){
	return replaceValue (off, len, null, 0, 0);
    }
    public DiameterMessage insertValue (int index, byte[] data, int off, int len){
	return replaceValue (index, 0, data, off, len);
    }
    public DiameterMessage replaceValue (int index, int origLen, byte[] data, int off, int len){
	if (origLen == len){
	    // we replace a section with another of same length
	    if (len > 0) System.arraycopy (data, off, _bytes, index, len);
	    return this;
	}
	byte[] tmp = new byte[_bytes.length - origLen + len];
	if (index > 0) System.arraycopy (_bytes, 0, tmp, 0, index);
	if (data != null) System.arraycopy (data, off, tmp, index, len);
	int nextOff = index + origLen;
	int remaining = _bytes.length - nextOff;
	if (remaining > 0) System.arraycopy (_bytes, nextOff, tmp, index + len, remaining);
	updateContent (tmp);
	return this;
    }
    // we assume the value is padded : use DiameterUtils to do so
    public boolean replaceAVPValue (int code, int vid, byte[] value, int off, int valueLenWithPadding, int paddingLen){
	int[] avp = indexOf (code, vid);
	if (avp == null) return false;
	int headerLen = avp[2] - avp[0];
	int oldValueLenWithPadding = avp[1] - headerLen;
	replaceValue (avp[2], oldValueLenWithPadding, value, off, valueLenWithPadding);
	// now update the avp len in the avp header
	int newTotalLenNoPadding = headerLen + valueLenWithPadding - paddingLen;
	_bytes[avp[0]+5] = (byte) (newTotalLenNoPadding >> 16);
	_bytes[avp[0]+6] = (byte) (newTotalLenNoPadding >> 8);
	_bytes[avp[0]+7] = (byte) (newTotalLenNoPadding);
	return true;
    }
    public static byte[] getAvp (int code, int vid, byte[] bytes, int bytesOff, int bytesLen, boolean skipHeader){
	int[] pos = indexOf (code, vid, bytes, bytesOff, bytesLen, skipHeader);
	if (pos == null) return null;
	byte[] value = new byte[pos[3]];
	System.arraycopy (bytes, pos[2], value, 0, value.length);
	return value;
    }
    // returns : avp offset, avp length(incl.padding), avp value offset, avp value length
    public static int[] indexOf (int code, int vid, byte[] bytes, int bytesOff, int bytesLen, boolean skipHeader){
	int bytesLimit = bytesOff + bytesLen;
	int off = skipHeader ? bytesOff + 20 : bytesOff;
	while (off+8 < bytesLimit){ // note that an empty avp value returns null --> arguable
	    //note that we assume that code and vendorid are unsigned int (not long) which seems realistic
	    int avpcode = getIntValue (bytes, off, 4);
	    int avplen = getIntValue (bytes, off+5, 3);
	    boolean hasvid = (bytes[off+4] & 0x80) == 0x80;
	    if (avplen < (hasvid ? 12 : 8)) return null; // malformed avp
	    int reallen = avplen;
	    switch (avplen%4){
	    case 1: reallen+=3; break;
	    case 2: reallen+=2; break;
	    case 3: reallen+=1; break;
	    }
	    int nextOffset = off+reallen;
	    if (nextOffset > bytesLimit) // this is a protection against malformed avp: if reallen exceeds data size
		return null;
	    if (avpcode != code){
		off = nextOffset;
		continue;
	    }
	    int avpvid = hasvid ? getIntValue (bytes, off+8, 4) : 0;
	    if (avpvid != vid){
		off = nextOffset;
		continue;
	    }
	    int valueOff = hasvid ? 12 : 8;
	    avplen = avplen - valueOff;
	    return new int[]{off, reallen, off + valueOff, avplen};
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
	    .append ((_type == Type.Capabilities && !isRequest ()) ? (",res="+getResultCode ()) : "")
	    .append ((_type == Type.Disconnection && isRequest ()) ? (",cause="+getIntAvp (273, 0, -1)) : "")
	    .append (']')
	    .toString ();
    }
}
