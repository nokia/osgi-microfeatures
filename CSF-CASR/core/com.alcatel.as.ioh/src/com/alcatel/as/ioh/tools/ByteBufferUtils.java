package com.alcatel.as.ioh.tools;

import java.util.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.*;


public class ByteBufferUtils {

    public static final ByteBuffer[] VOID = new ByteBuffer[0];
    
    public static java.nio.charset.Charset UTF_8 = null;
    static {
	try{
	    UTF_8 = java.nio.charset.Charset.forName ("utf-8");
	}catch(Exception e){}
    }

    public static byte[] getUTF8 (String value){
	return value.getBytes (UTF_8);
    }
    public static String getUTF8 (byte[] value){
	return getUTF8 (value, 0, value.length);
    }
    public static String getUTF8 (byte[] value, int off, int len){
	return new String (value, off, len, UTF_8);
    }
    public static String getUTF8 (ByteBuffer buffer, boolean consume){
	// for debug mostly (toString of what we send)
	byte[] bytes = new byte[buffer.remaining ()];
	buffer.get (bytes);
	String s = getUTF8 (bytes, 0, bytes.length);
	if (consume == false) buffer.position (buffer.position () - bytes.length);
	return s;
    }
    
    public static String toUTF8String (boolean consume, ByteBuffer... buffs){
	// for DEBUG only !!!
	ByteBuffer buffer = aggregate (true, consume, buffs);
	return getUTF8 (buffer, false);
    }

    public static int remaining (ByteBuffer... buffs){
	int n = 0;
	for (ByteBuffer b : buffs) n += b.remaining ();
	return n;
    }

    public static ByteBuffer aggregate (boolean mustCopy, boolean consume, ByteBuffer... bufs){
	// if mustCopy = FALSE then consume is N/A since the caller gives away the buffers
	if (mustCopy){
	    ByteBuffer sum = ByteBuffer.allocate (remaining (bufs));
	    for (ByteBuffer b : bufs){
		int len = b.remaining ();
		sum.put (b);
		if (consume == false) b.position (b.position () - len);
	    }
	    return (ByteBuffer) sum.flip ();
	} else {
	    ByteBuffer sum = bufs[0];
	    if (bufs.length == 1){
		return sum;
	    }
	    int len = remaining (bufs);
	    if (sum.capacity () >= len){
		sum.compact ();
		for (int i=1; i<bufs.length; i++)
		    sum.put (bufs[i]);
		return (ByteBuffer) sum.flip ();
	    }
	    sum = ByteBuffer.allocate (len);
	    for (ByteBuffer b : bufs)
		sum.put (b);
	    return (ByteBuffer) sum.flip ();
	}
    }

    public static ByteBuffer[] prepend (ByteBuffer prefix, ByteBuffer... buffs){
	if (prefix == null) return buffs != null ? buffs : VOID;
	if (buffs == null || buffs.length == 0) return new ByteBuffer[]{prefix};
	ByteBuffer[] res = new ByteBuffer[1 + buffs.length];
	res[0] = prefix;
	System.arraycopy (buffs, 0, res, 1, buffs.length);
	return res;
    }

    public static ByteBuffer[] append (ByteBuffer suffix, ByteBuffer... buffs){
	if (suffix == null) return buffs != null ? buffs : VOID;
	if (buffs == null || buffs.length == 0) return new ByteBuffer[]{suffix};
	ByteBuffer[] res = new ByteBuffer[1 + buffs.length];
	System.arraycopy (buffs, 0, res, 0, buffs.length);
	res[buffs.length] = suffix;
	return res;
    }

    public static ByteBuffer[] aggregate (ByteBuffer[] a, ByteBuffer[] b){
	if (a == null || a.length == 0) return b != null ? b : VOID;
	if (b == null || b.length == 0) return a;
	ByteBuffer[] res = new ByteBuffer[a.length + b.length];
	System.arraycopy (a, 0, res, 0, a.length);
	System.arraycopy (b, 0, res, a.length, b.length);
	return res;
    }
    
}
