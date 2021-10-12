// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.http;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * This Class wraps the content of a message (request or response).
 */
public interface HttpBody extends HttpObject {
        
    /**
     * Returns an OutputStream that appends to the content.
     * @return the OutputStream.
     */
    public OutputStream getOutputStream ();
  
    /**
     * Returns an InputStream that reads the content.
     * <br/>Note: the returned InputStream is not thread safe.
     * @return the InputStream.
     */
    public InputStream getInputStream ();
  
    /**
     * Retrieves a clone of the content of the message as binary data using
     * a byte array.
     * @return a byte array containing the content of the message.
     */
    public byte [] getContent ();

    /**
     * Returns the internal byte array used by the implementing class to store the data.
     * <br/><b>This array is unlikely to have the same bounds as the real data.</b>
     * This array is only meaningful if the structure of the implementing class is known.
     * <br/>Returns <code>null</code> if the implementing class does not use a byte array to store the data.
     * <br/><b>Note that modifying that array must be done carefully</b>.
     *
     * @return	a byte array storing the raw data in the implementing class.
     */
    public byte [] getInternalContent ();
  
    /**
     * Retrieves the content of the message as a String.
     *
     * @return	a String containing the content of the message
     * @throws UnsupportedEncodingException if the character encoding of the message is not supported.
     */
    public String getContentAsString () throws UnsupportedEncodingException;
  
    /**
     * Returns the content size in bytes.
     * @return the content size.
     */
    public int getSize ();
  
    /**
     * Clears the content.
     */
    public void clearContent ();

    /**
     * Sets the message content using the platform default encoding.
     *
     * @param data	the content as a String.
     */
    public void setContent (String data);

    /**
     * Sets the message content.
     *
     * @param data	the content as a String.
     * @param encoding	the encoding to use.
     * @throws UnsupportedEncodingException if the encoding is not supported.
     */
    public void setContent (String data, String encoding)
	throws UnsupportedEncodingException;
    
    /**
     * Same as setContent(data, false).
     * 
     * @deprecated 
     * @param data	the message content.
     */
    public void setContent (byte [] data);

    /**
     * Sets the message content.
     * <br/>If the copy parameter is set to true, the content must not reference the data directly.
     * <br/>If the copy parameter is set to false, the content may reference the data directly.
     *
     * @param data	the message content.
     * @param copy	specifies if the data must copied or not.
     */
    public void setContent (byte [] data, boolean copy);

    /**
     * Same as setContent(data, offset, length, false).
     *
     * @deprecated
     * @param data	the message content.
     * @param offset	the offset the content starts from.
     * @param length	the content length.
     */
    public void setContent (byte [] data, int offset, int length);

    /**
     * Sets the message content.
     * <br/>If the copy parameter is set to true, the content must not reference the data directly.
     * <br/>If the copy parameter is set to false, the content may reference the data directly.
     *
     * @param data	the byte array which includes the message content.
     * @param offset	the offset at which the content starts in the specified byte array.
     * @param length	the content length.
     * @param copy	specifies if the data must copied or not.
     */
    public void setContent (byte [] data, int offset, int length, boolean copy);

    /**
     * Appends some data to the message content.
     *
     * @param data	the content to append.
     */
    public void appendContent (byte [] data);

    /**
     * Appends some data to the message content using the platform default encoding.
     *
     * @param data	the content to append.
     */
    public void appendContent (String data);
  
    /**
     * Appends some data to the message content.
     *
     * @param data	the content to append.
     * @param encoding	the encoding to use.
     * @throws UnsupportedEncodingException if the encoding is not supported.
     */
    public void appendContent (String data, String encoding)
	throws UnsupportedEncodingException;
  
    /**
     * Appends one byte to the message content.
     * <br/>The provided int is cast into a byte.
     *
     * @param b	the byte.
     */
    public void appendContent (int b);

    /**
     * Appends some data to the message content.
     *
     * @param data	the byte array which includes the data to append.
     * @param offset	the offset at which the data start in the specified byte array.
     * @param length	the data length.
     */
    public void appendContent (byte [] data, int offset, int length);

    /**
     * Appends data from an input stream into this message content.
     * <br/>This method reads the InputStream until it reaches the end of the stream. It may block while doing it.
     *
     * @param in	the input stream used to append data to this message.
     * @throws IOException if an I/O error occurs.
     */
    public void appendContent (InputStream in) throws IOException;

    /**
     * Appends n bytes from an input stream into this message content.
     * <br/>This method may block while reading the InputStream.
     *
     * @param in	the input stream used to append data to this message.
     * @param n   the number of bytes to append.
     * @throws IOException if an I/O error occurs.
     */
    public void appendContent (InputStream in, int n) throws IOException;
    
    /**
     * Appends a vector of nio Byte buffers.
     * @param buffers one (or several) byte buffers to be sent atomically
     * @throws IOException on any error.
     */
    public void appendContent(ByteBuffer ... buffers) throws IOException;
}
