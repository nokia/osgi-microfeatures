package com.nextenso.diameter.agent.httpgw;

import java.util.*;
import java.net.*;
import java.nio.*;

public class DiameterMessage {

    private static final int[] PADDING = {0, 3, 2, 1};

    public static enum Type {
	Capabilities, Watchdog, Disconnection, Application
    }

    private Type _type;
    private byte[] _bytes;
    private int _bytesOffset;
    private boolean _req;
    private int _resultCode = -1;
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
	clone._resultCode = _resultCode;
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
    public boolean isProxiable (){ return ((_bytes[4] & 0x10) == 0x10); }
    public boolean isError (){ return ((_bytes[4] & 0x10) == 0x20); }
    public boolean isRetransmitted (){ return ((_bytes[4] & 0x10) == 0x30); }

    private void parse (){
	s1 : switch ((int) getApplicationID ()){
	case 0:
	    switch (getCommandCode ()){
	    case 257: _type = Type.Capabilities; break s1;
	    case 280: _type = Type.Watchdog; break s1;
	    case 282: _type = Type.Disconnection; break s1;
	    default: _type = Type.Application; break s1;
	    }
	case 10:
	    switch (getCommandCode ()){
	    case 328: _type = Type.Capabilities; break s1; // RFC 6737 CUR
	    }
	default:
	    _type = Type.Application;
	}
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
	if (_resultCode == -1)
	    _resultCode = getIntAvp (0xFF_FF_FF_FF, 0, -1); // this is a hack
	return _resultCode;
    }
    public void resetResultCode (){ // to clear the cached _resultCode if needed
	_resultCode = -1;
    }

    public int[] getSessionId (boolean deep){
	if (deep) return indexOf (263, 0);
	if (_bytes.length < 28) return null;
	// else assume this is the first AVP if present - as it should
	if (_bytes[23] == 7 && // start with the less probable
	    _bytes[22] == 1 &&
	    _bytes[21] == 0 &&
	    _bytes[20] == 0 &&
	    (_bytes[24] & (byte) 0x80) == 0
	    ){
	    int avplen = getIntValue (_bytes, 25, 3);
	    if (avplen < 8) return null; // broken !
	    return new int[]{20, avplen + PADDING[avplen & 0x03], 28, avplen - 8};
	}
	return null;
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
    public DiameterMessage updateCommand (int appId, int code){
	byte flags = _bytes[4];
	DiameterUtils.setIntValue (code, _bytes, 4);
	_bytes[4] = flags;
	DiameterUtils.setIntValue (appId, _bytes, 8);
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
	boolean lookingForResult = code == 0xFF_FF_FF_FF;
	if (lookingForResult) code = 268;
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
	    boolean matchCode = (avpcode == code);
	    if (lookingForResult && !matchCode){
		matchCode = avpcode == 297; // experimental result
	    }
	    if (!matchCode){
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
	    if (!lookingForResult || avpcode != 297){
		return new int[]{off, reallen, off + valueOff, avplen};
	    } else {
		// Experimental result
		// this is a hack to avoid to look up result THEN experimental result
		return indexOf (298, 0, bytes, off + valueOff, avplen, false);
	    }
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
