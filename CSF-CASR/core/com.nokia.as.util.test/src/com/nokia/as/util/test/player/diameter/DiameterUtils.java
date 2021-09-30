package com.nokia.as.util.test.player.diameter;

import java.util.concurrent.atomic.*;
import java.net.*;
import java.io.*;


public class DiameterUtils {

    public static class Avp {
	private int _code, _vid;
	private byte[] _value;
	private boolean _mandatory;
	public Avp (int code, int vid, boolean mandatory, byte[] value){
	    _code = code;
	    _vid = vid;
	    _value = value;
	    _mandatory = mandatory;
	}
	public int getCode (){
	    return _code;
	}
	public int getVendorId (){
	    return _vid;
	}
	public boolean isMandatory (){
	    return _mandatory;
	}
	public byte[] getValue (){
	    return _value;
	}
	private boolean writeTo (OutputStream os) throws IOException {
	    if (_value == null) return false;
	    boolean hasvid = _vid != 0;
	    writeIntValue (_code, os);
	    int flags = 0;
	    if (_mandatory) flags |= 0x40;
	    if (hasvid) flags |= 0x80;
	    os.write (flags);
	    int len = 8 + _value.length + (hasvid ? 4 : 0);
	    os.write (len >> 16);
	    os.write (len >> 8);
	    os.write (len);
	    if (hasvid) writeIntValue (_vid, os);
	    os.write (_value);
	    switch (len%4){
	    case 1: os.write (0);
	    case 2: os.write (0);
	    case 3: os.write (0);
	    }
	    return true;
	}
    }

    private static final AtomicInteger hopSeed = new AtomicInteger (0);
    private static final int endSeed = (int) (System.currentTimeMillis () << 20);

    public static DiameterMessage makeDwr (DiameterMessage cer){
	return makeRequest (0, 280,
			    new Avp (264, 0, true, cer.getAvp (264, 0)),
			    new Avp (296, 0, true, cer.getAvp (296, 0))
			    );
    }

    public static DiameterMessage makeRequest (int appId, int code, Avp... avps){
	return makeMessage (true, appId, code, newHopIdentifier (), newEndIdentifier (), avps);
    }

    public static DiameterMessage makeResponse (DiameterMessage req, Avp... avps){
	return buildMessage (false, req.getApplicationID (), req.getCommandCode (),
			     req.getHopIdentifier (), req.getEndIdentifier (),
			     new Avp[]{
				 new Avp (263, 0, true, req.getAvp (263, 0)), // session-id
				 new Avp (264, 0, true, req.getAvp (293, 0)), // set orighost with desthost
				 new Avp (296, 0, true, req.getAvp (283, 0)), // set origrealm with destrealm
				 new Avp (293, 0, true, req.getAvp (264, 0)), // set desthost with orighost
				 new Avp (283, 0, true, req.getAvp (296, 0))}, // set destrealm with origrealm
			     avps
			     );	
    }

    public static DiameterMessage makeMessage (boolean req, long appId, int code, int hopId, int endId, Avp... avps){
	return buildMessage (req, appId, code, hopId, endId, avps, null);
    }
    private static DiameterMessage buildMessage (boolean req, long appId, int code, int hopId, int endId, Avp[] defAvps, Avp[] avps){
	try{
	    ByteArrayOutputStream baos = new ByteArrayOutputStream ();
	    baos.write (1); // version
	    baos.write (0); // length will be set later
	    baos.write (0); // length will be set later
	    baos.write (0); // length will be set later
	    baos.write (req ? 0x80 : 0); // command flags
	    baos.write (code >> 16); // command code
	    baos.write (code >> 8); // command code
	    baos.write (code); // command code
	    writeIntValue ((int)appId, baos);
	    writeIntValue (hopId, baos);
	    writeIntValue (endId, baos);
	    if (defAvps != null)
		for (Avp avp: defAvps)
		    avp.writeTo (baos);
	    if (avps != null)
		for (Avp avp: avps)
		    avp.writeTo (baos);
	    byte[] result = baos.toByteArray ();
	    result[1] = (byte)(result.length >> 16);
	    result[2] = (byte)(result.length >> 8);
	    result[3] = (byte)result.length;
	    return new DiameterMessage (result);
	}catch(IOException ignore){
	    //cannot happen
	    return null;
	}
    }

    public static DiameterMessage updateHopIdentifier (DiameterMessage msg){
	return updateHopIdentifier (msg, newHopIdentifier ());
    }

    public static DiameterMessage updateIdentifiers (DiameterMessage msg){
	return updateIdentifiers (msg, newHopIdentifier (), newEndIdentifier ());
    }
    
    public static DiameterMessage updateHopIdentifier (DiameterMessage msg, int hopId){
	byte[] msgBytes = msg.getBytes ();
	setIntValue (hopId, msgBytes, 12);
	return msg;
    }

    public static DiameterMessage updateIdentifiers (DiameterMessage msg, int hopId, int endId){
	byte[] msgBytes = msg.getBytes ();
	setIntValue (hopId, msgBytes, 12);
	setIntValue (endId, msgBytes, 16);
	return msg;
    }

    public static byte[] setIntValue (int value, byte[] bytes, int off){
	bytes[off++] = (byte)(value >> 24);
	bytes[off++] = (byte)(value >> 16);
	bytes[off++] = (byte)(value >> 8);
	bytes[off] = (byte)value;
	return bytes;
    }

    public static void writeIntValue (int value, OutputStream os) throws IOException {
	os.write (value >> 24);
	os.write (value >> 16);
	os.write (value >> 8);
	os.write (value);
    }

    public static int newHopIdentifier (){
	return hopSeed.incrementAndGet ();
    }

    public static int newEndIdentifier (){
	return endSeed | (hopSeed.incrementAndGet () & 0xFFFFF);
    }
}
