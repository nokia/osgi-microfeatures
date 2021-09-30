package com.alcatel.as.http.parser;

import java.nio.*;
import java.util.*;
import java.io.*;
import java.util.function.BiConsumer;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface HttpMessage {

    public static interface Header {
	public String getName ();
	public byte[] getRawValue ();
	public String getValue ();
	public int getValueAsInt (int def);
	public Header getNext ();
    }
    public boolean isRequest ();
    public boolean isFull ();
    public boolean isFirst ();
    public boolean isLast ();
    public boolean isChunked ();

    public <T> T getAgent ();
    public void setAgent (Object agent);

    public void attach (Object o);
    public <T> T attachment ();
    
    public String getMethod ();
    public String getURL ();
    public int getStatus ();
    public int getVersion ();

    public byte[] getBody ();
    public void setBody (byte[] body);

    public Header getHeader (String name);
    public void iterateHeaders (BiConsumer<String, String> consumer);
    
    public String getHeaderValue (String name);
    public int getHeaderValueAsInt (String name, int def);

    public HttpMessage addHeader (byte[] name, String value);
    public HttpMessage addHeader (byte[] name, byte[] value);
    public HttpMessage removeHeader (Header h);
    public HttpMessage removeHeaders ();

    public ByteBuffer[] toByteBuffers ();
    public ByteBuffer[] toByteBuffers (boolean firstLine, boolean headers, boolean body);
}
