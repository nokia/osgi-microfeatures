package com.alcatel.as.diameter.parser;

import java.util.concurrent.atomic.*;
import java.net.*;
import java.io.*;
import java.nio.charset.Charset;

import org.apache.log4j.Logger;

public class DiameterUtils {
    
    public static Charset UTF8 = null;
    static {
	try{ UTF8 = Charset.forName ("utf-8");
	}catch(Exception e){}// cannot happen
    }
    
    public static final Avp AVP_RESULT_2001 = new Avp (268, 0, true, getIntValue(2001));

    public static class Avp {
	private int _code, _vid;
	private byte[] _value;
	private int _flags;
	public Avp (int code, int vid, boolean mandatory, byte[] value){
	    _code = code;
	    _vid = vid;
	    _value = value;
	    if (vid == 0)
		_flags = mandatory ? 0x40 : 0;
	    else
		_flags = mandatory ? 0xC0 : 0x80;
	}
	public int getCode (){
	    return _code;
	}
	public int getVendorId (){
	    return _vid;
	}
	public boolean isMandatory (){
	    return (_flags & 0x40) == 0x40;
	}
	public byte[] getValue (){
	    return _value;
	}
	public boolean writeTo (OutputStream os) throws IOException {
	    if (_value == null) return false;
	    boolean hasvid = _vid != 0;
	    writeIntValue (_code, os);
	    os.write (_flags);
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

    public static final Logger LOGGER_DIAMETER = Logger.getLogger ("as.diameter.parser");
    
    private static final AtomicInteger hopSeed = new AtomicInteger (0);
    private static final int endSeed = (int) (System.currentTimeMillis () << 20);

    public static DiameterMessage makeDwr (DiameterMessage msg){
	return makeRequest (0, 280,
			    new Avp (264, 0, true, msg.getAvp (264, 0)),
			    new Avp (296, 0, true, msg.getAvp (296, 0))
			    );
    }
    // the msg below is not a dwr : no need to copy identifiers
    public static DiameterMessage makeDwa (DiameterMessage msg){
	return makeMessage (false, 0, 280,
			    0, 0,
			    new Avp[]{
				AVP_RESULT_2001,
				new Avp (264, 0, true, msg.getAvp (264, 0)),
				new Avp (296, 0, true, msg.getAvp (296, 0))
			    }
			    );
    }
    public static DiameterMessage makeDwa (DiameterMessage incomingDwr, DiameterMessage templateDwa){
	return templateDwa.updateIdentifiers (incomingDwr.getHopIdentifier (), incomingDwr.getEndIdentifier ()).clone ();
    }

    public static DiameterMessage cloneCea (DiameterMessage cea, DiameterMessage cer){
	byte[] clone = new byte[cea.getBytes ().length];
	System.arraycopy (cea.getBytes (), 0, clone, 0, clone.length);
	DiameterMessage ceaClone = new DiameterMessage (clone);
	return ceaClone.updateIdentifiers (cer.getHopIdentifier (), cer.getEndIdentifier ());
    }
    //public static final byte[] BUSY = getIntValue (1);
    public static DiameterMessage makeDpr (DiameterMessage msg, int dprReasonCode){
	return makeRequest (0, 282,
			    new Avp (264, 0, true, msg.getAvp (264, 0)),
			    new Avp (296, 0, true, msg.getAvp (296, 0)),
			    //new Avp (273, 0, true, BUSY) // Disconnect cause = BUSY
			    new Avp (273, 0, true, getIntValue(dprReasonCode)) 
			    );
    }
    // the dwr holds the originHost/realm info
    public static DiameterMessage makeDpa (DiameterMessage dwr, DiameterMessage dpr){
	return buildMessage (false, 0, 282,
			     dpr.getHopIdentifier (), dpr.getEndIdentifier (),
			     new Avp[]{
				 AVP_RESULT_2001,
				 new Avp (264, 0, true, dwr.getAvp (264, 0)),
				 new Avp (296, 0, true, dwr.getAvp (296, 0))
			     }, null);
    }

    public static DiameterMessage makeRequest (int appId, int code, Avp... avps){
	return makeMessage (true, appId, code, newHopIdentifier (), newEndIdentifier (), avps);
    }

    public static DiameterMessage makeResponse (DiameterMessage req, Avp origH, Avp origR, Avp... avps){
	return buildMessage (false, req.getApplicationID (), req.getCommandCode (),
			     req.getHopIdentifier (), req.getEndIdentifier (),
			     new Avp[]{
				 new Avp (263, 0, true, req.getAvp (263, 0)), // session-id
				 origH, origR},
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
	return msg.updateHopIdentifier (newHopIdentifier ());
    }

    public static DiameterMessage updateIdentifiers (DiameterMessage msg){
	return msg.updateIdentifiers (newHopIdentifier (), newEndIdentifier ());
    }
    
    public static byte[] getIntValue (int value){
	return setIntValue (value, new byte[4], 0);
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

    public static byte[] padValue (byte[] value){
	int padLen = (4 - (value.length%4))%4;
	if (padLen == 0) return value;
	byte[] tmp = new byte[value.length + padLen];
	System.arraycopy (value, 0, tmp, 0, value.length);
	return tmp;
    }

    public static int newHopIdentifier (){
	return hopSeed.incrementAndGet ();
    }

    public static int newEndIdentifier (){
	return endSeed | (hopSeed.incrementAndGet () & 0xFFFFF);
    }

    public static DiameterMessage updateOrigin (DiameterMessage msg, byte[] host, int hostPadLen, byte[] realm, int realmPadLen){
	if (host != null){
	    msg.replaceAVPValue (264, 0, host, 0, host.length, hostPadLen);
	}
	if (realm != null){
	    msg.replaceAVPValue (296, 0, realm, 0, realm.length, realmPadLen);
	}
	return msg;
    }
    public static DiameterMessage updateDestination (DiameterMessage msg, byte[] host, int hostPadLen, byte[] realm, int realmPadLen){
	if (host != null){
	    msg.replaceAVPValue (293, 0, host, 0, host.length, hostPadLen);
	}
	if (realm != null){
	    msg.replaceAVPValue (283, 0, realm, 0, realm.length, realmPadLen);
	}
	return msg;
    }
}
