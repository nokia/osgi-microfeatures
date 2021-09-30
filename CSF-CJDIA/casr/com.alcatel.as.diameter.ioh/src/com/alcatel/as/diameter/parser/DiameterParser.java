package com.alcatel.as.diameter.parser;

import java.util.*;
import java.net.*;
import java.nio.*;
import java.io.*;

import org.apache.log4j.Logger;


import com.alcatel.as.ioh.*;

import com.nextenso.proxylet.diameter.DiameterAVP;

public class DiameterParser implements MessageParser<DiameterMessage> {

    private final byte[] _buflen = new byte[4];
    private int _buflenOffset = 0; // we have fully read length if _buflenOffset == 4
    private DiameterMessage _msg;

    public DiameterParser (){
    }
    public void reset (){
	_buflenOffset = 0;
	_msg = null;
    }
    
    public DiameterMessage parseMessage (ByteBuffer buffer)
    {
	if (_msg == null)
	    return readLength(buffer) ? readBody(buffer) : null;
	else
	    return readBody (buffer);
    }
    
    private boolean readLength(ByteBuffer buffer)
    {
        int n = Math.min(_buflen.length - _buflenOffset, buffer.remaining());
        buffer.get(_buflen, _buflenOffset, n);
        _buflenOffset += n;
	
        if (_buflenOffset == _buflen.length)
        {
            // length fully parsed.
            int length = get_24from32(_buflen);
	    if (length < 20) throw new RuntimeException ("Invalid message (length="+length+")");
            _msg = new DiameterMessage(_buflen, length);
            return true; // length fully parsed
        }

        return false; // length not fully parsed
    }

    private DiameterMessage readBody(ByteBuffer buffer)
    {
        if (_msg.append(buffer))
        {
	    try{
		return _msg.check ();
	    }finally{
		reset ();
	    }
        }
        return null; // body not fully parsed
    }

    // we store the first 4 bytes while the len is actually in the last 3
    static final int get_24from32(byte[] from)
    {
	// dont call get_24from32 (from, 0) to avoid 3 additions
	int res = from[1] & 0xFF;
        res <<= 8;
        res |= from[2] & 0xFF;
        res <<= 8;
        res |= from[3] & 0xFF;
        return res;
    }
    static final int get_24from32(byte[] from, int off)
    {
        int res = from[off+1] & 0xFF;
        res <<= 8;
        res |= from[off+2] & 0xFF;
        res <<= 8;
        res |= from[off+3] & 0xFF;
        return res;
    }

    public static final long getUnsigned32 (byte[] data, int offset){
	long res = data[offset] & 0xFF;
	res <<= 8;
	res |= data[offset + 1] & 0xFF;
	res <<= 8;
	res |= data[offset + 2] & 0xFF;
	res <<= 8;
	res |= data[offset + 3] & 0xFF;
	return res;
    }

    public static final long getUnsigned48 (ByteBuffer buff){
	long res = buff.get () & 0xFF;
	res <<= 8;
	res |= buff.get () & 0xFF;
	res <<= 8;
	res |= buff.get () & 0xFF;
	res <<= 8;
	res |= buff.get () & 0xFF;
	res <<= 8;
	res |= buff.get () & 0xFF;
	res <<= 8;
	res |= buff.get () & 0xFF;
	return res;
    }
    
    public static interface Handler {
	void flags (int flags);
	void code (int code);
	void application (long id);
	void hopId (long id);
	void endId (long id);
	boolean newAVP (long code, long vendorId, int flags, byte[] data, int off, int len);
    }
    public static void parseMessage (DiameterMessage message, Handler handler) throws IOException {
	parseMessage (message.getBytes (), handler);
    }
    public static void parseMessage (byte[] data, Handler handler) throws IOException {
	parseMessage (data, 0, data.length, handler);
    }
    public static void parseMessage (byte[] data, int off, int len, Handler handler) throws IOException {
	int limit = off + len;
	if (limit > data.length){
	    throw new EOFException ();
	}
	if (len < 20) throw new EOFException ();
	off += 4; //skip len - assume length was already checked
	handler.flags (data[off] & 0xFF);
	handler.code (get_24from32 (data, off));
	off += 4; //done code
	handler.application (getUnsigned32 (data, off));
	off += 4; //done app
	handler.hopId (getUnsigned32 (data, off));
	off += 4; //done hId
	handler.endId (getUnsigned32 (data, off));
	off += 4; //done eId
	len -= 20;
	while (true) {
	    if (len == 0) {
		return;
	    }
	    if (len < 8) {
		throw new IOException("Invalid message length");
	    }

	    long code = getUnsigned32(data, off);
	    off += 4;
	    int flags = data[off++] & 0xFF;
	    int avpLen = data[off++] & 0xFF;
	    avpLen <<= 8;
	    avpLen |= data[off++] & 0xFF;
	    avpLen <<= 8;
	    avpLen |= data[off++] & 0xFF;

	    int dataLen = avpLen - 8;

	    boolean hasVendorId = (flags & 0x80) == 0x80;
	    long vendorId = 0L;
	    if (hasVendorId) {
		if (len < 12) {
		    throw new IOException("Invalid message length");
		}
		vendorId = getUnsigned32(data, off);
		off += 4;
		dataLen -= 4;
	    }
	    if (dataLen < 0) {
		throw new IOException("Invalid avp data length");
	    }
	    if (off + dataLen > limit){
		throw new IOException("Invalid avp data length");
	    }
				
	    len -= avpLen;
				
	    // pad
	    int neededZeros = (4 - avpLen % 4) % 4;
	    if (neededZeros > 0) {
		len -= neededZeros;
		if (len < 0) throw new IOException ("Invalid avp value (no padding)");
	    }
	    if (handler.newAVP (code, vendorId, flags, data, off, dataLen))
		return;
				
	    off += dataLen + neededZeros;
	}
    }

    public static StringBuilder toString (final DiameterMessage message, final StringBuilder sb){
	try{
	    Handler handler = new Handler (){
		    int index;
		    public void flags (int flags){
			sb.append ("Flags=");
			sb.append ((flags & 0x80) == 0x80 ? "Req" : "-");
			sb.append ((flags & 0x40) == 0x40 ? " Proxy" : " -");
			sb.append ((flags & 0x20) == 0x20 ? " Error" : " -");
			sb.append ((flags & 0x10) == 0x10 ? " Retr" : " -");
			sb.append ("\n");
		    }
		    public void code (int code){
			sb.append ("Code=").append (code).append ('\n');
		    }
		    public void application (long id){ sb.append ("ApplicationId=0x").append (Long.toHexString (id)).append ("\n");}
		    public void hopId (long id){ sb.append ("HopByHopIdentifier=0x").append (Long.toHexString (id)).append ("\n");}
		    public void endId (long id){ sb.append ("EndToEndIdentifier=0x").append (Long.toHexString (id));}
		    public boolean newAVP (long code, long vendorId, int flags, byte[] data, int off, int len){
			DiameterAVP avp = new DiameterAVP (code, vendorId, flags);
			avp.setValue (data, off, len, false);
			sb.append ('\n').append (avp.toString (1));
			return false;
		    }};
	    sb.append (message.isRequest () ? "DiameterRequest\n" : "DiameterResponse\n");
	    parseMessage (message, handler);
	    return sb;
	}catch(Exception e){ e.printStackTrace ();return sb;}
    }

}
