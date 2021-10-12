// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.http;

import java.util.Enumeration;
import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayOutputStream;

import java.util.Hashtable;

/**
 * Utility class that currently supports "multipart/form-data" and "multipart/mixed".
 */

public class MultipartData {
    
    /**
     * The MIME type "multipart/form-data"
     */
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    /**
     * The MIME type "multipart/mixed"
     */
    public static final String MULTIPART_MIXED = "multipart/mixed";
	
    private String mime;
    private String boundary;
    private byte[] boundaryBytes;
    private Hashtable parts = new Hashtable ();
	
    /**
     * Indicates if the message contains multipart/(form-data or mixed) from its headers.
     * @param headers the headers of the message
     * @return true or false
     */
    public static boolean isMultipartData (HttpHeaders headers){
	String ctype = headers.getHeader (HttpUtils.CONTENT_TYPE);
	if (ctype == null)
	    return false;
	return (ctype.startsWith (MULTIPART_FORM_DATA) || ctype.startsWith (MULTIPART_MIXED));
    }
	
    /**
     * Creates a new MultipartData with the specified boundary and mime-type.
     * @param mime_type the mime_type
     * @param boundary the boundary ("--" will be prepended)
     */
    public MultipartData (String mime_type, String boundary){
	this.mime = mime_type;
	this.boundary = boundary;
	boundaryBytes = ("--"+boundary).getBytes ();
    }
	
    /**
     * Creates a new MultipartData with the specified mime-type, boundary and data.
     * @param mime_type the mime_type
     * @param boundary the boundary ("--" will be prepended)
     * @param data the data
     * @throws IllegalArgumentException if the data are invalid.
     */
    public MultipartData (String mime_type, String boundary, byte[] data){
	this (mime_type, boundary);
	try{
	    parse (data);
	}catch(Exception e){
	    throw new IllegalArgumentException ("Invalid data");
	}
    }
	
    /**
     * Creates a new MultipartData given its content-type (which includes the mime-type and boundary).
     * @param contentType the content-type
     * @throws IllegalArgumentException if the content-type is invalid (boundary is missing for example).
     */
    public MultipartData (String contentType){
	if (contentType == null)
	    throw new IllegalArgumentException ("Invalid content-type");
	contentType = clean (contentType);
	if (contentType.startsWith (MULTIPART_FORM_DATA))
	    mime = MULTIPART_FORM_DATA;
	else if (contentType.startsWith (MULTIPART_MIXED))
	    mime = MULTIPART_MIXED;
	else
	    throw new IllegalArgumentException ("Invalid content-type");
	boundary = getParameter (";boundary=", contentType);
	if (boundary == null)
	    throw new IllegalArgumentException ("Invalid content-type");
	boundaryBytes = ("--"+boundary).getBytes ();
    }
	
    /**
     * Creates a new MultipartData given its content-type (which includes the mime-type and boundary) and data.
     * @param contentType the content-type
     * @param data the data
     * @throws IllegalArgumentException if the contentType or the data are invalid.
     */
    public MultipartData (String contentType, byte[] data){
	this (contentType);
	try{
	    parse (data);
	}catch(Exception e){
	    throw new IllegalArgumentException ("Invalid data");
	}
    }
	
    /**
     * Creates a new MultipartData from the specified request.
     * @param request the request to parse
     * @throws IllegalArgumentException if the contentType or the data are invalid.
     */
    public MultipartData (HttpRequest request){
	this (  request.getHeaders ().getHeader (HttpUtils.CONTENT_TYPE),
		request.getBody ().getContent ());
    }
	
    /**
     * Returns the mime-type.
     * @return the mime-type
     */
    public String getMimeType (){
	return mime;
    }
	
    /**
     * Returns the boundary.
     * @return the boundary (does not include the prefix "--").
     */
    public String getBoundary (){
	return boundary;
    }
	
    /**
     * Returns the content-type.
     * @return the content-type
     */
    public String getContentType (){
	return new StringBuffer ().append (mime).append (";boundary=\"").append (boundary).append ("\"").toString ();
    }
	
    /**
     * Returns the data.
     * The returned array is dynamically generated.
     * @return the data
     */
    public byte[] getData (){
	ByteArrayOutputStream os = new ByteArrayOutputStream ();
	Enumeration enumeration = parts.keys ();
	while (enumeration.hasMoreElements ()){
	    os.write (boundaryBytes, 0, boundaryBytes.length);
	    os.write ((int)'\r');
	    os.write ((int)'\n');
	    Part part = (Part)parts.get ((String)enumeration.nextElement ());
	    StringBuffer headers = new StringBuffer ();
	    headers.append ("content-disposition:").append (part.getContentDisposition ());
	    headers.append ("\r\n");
	    headers.append ("content-type:").append (part.getContentType ());
	    headers.append ("\r\n");
	    if (part.getTransferEncoding () != null){
		headers.append ("content-transfer-encoding:").append (part.getTransferEncoding ());
		headers.append ("\r\n");
	    }
	    headers.append ("\r\n");
	    byte[] b = headers.toString ().getBytes ();
	    os.write (b, 0, b.length);
	    b = part.getData ();
	    os.write (b, 0, b.length);
	    os.write ((int)'\r');
	    os.write ((int)'\n');
	}
	os.write (boundaryBytes, 0, boundaryBytes.length);
	os.write ((int)'-');
	os.write ((int)'-');
	return os.toByteArray ();
    }
	
    /**
     * Fills a request with this multipart (updates the method, content-type and body).
     * @param request the request to fill.
     */
    public void dumpIntoRequest (HttpRequest request){
	request.getProlog ().setMethod (HttpUtils.METHOD_POST);
	request.getHeaders ().setHeader (HttpUtils.CONTENT_TYPE, getContentType ());
	request.getBody ().setContent (getData (), false);
    }
	
    /**
     * Returns the parts names.
     * @return an Enumeration of the parts names.
     */
    public Enumeration getPartsNames () {
	return parts.keys ();
    }
	
    /**
     * Adds a new part.
     * @param part the Part to add.
     */
    public void addPart (Part part){
	if (part.getName () == null){
	    if (part.getFilename () == null)
		throw new IllegalArgumentException ("Part has no name");
	    else
		parts.put (part.getFilename (), part);
	}
	else
	    parts.put (part.getName (), part);
    }
	
    /**
     * Removes a part given its name.
     * @param name the part name.
     * @return the removed Part, or <code>null</null> if no Part was removed.
     */
    public Part removePart (String name){
	return (Part)parts.remove (name);
    }
	
    /**
     * Returns a part given its name.
     * @param name the part name.
     * @return the Part or <code>null</null> if no Part was found.
     */
    public Part getPart (String name) {
	return (Part)parts.get (name);
    }
	
    /************************
        private methods
    ************************/
	
    private void parse (byte[] data)
        throws Exception{
	int index1 = getBoundaryIndex (data, 0);
	if (index1 == -1)
	    throw new Exception ("Invalid data");
	index1 += boundaryBytes.length;
	if (data[index1] == (byte)'\r')
	    index1++;
	if (data[index1] == (byte)'\n')
	    index1++;
	while (true){
	    int index2 = getBoundaryIndex (data, index1);
	    if (index2 == -1)
		throw new Exception ("Invalid data");
	    if (index1 < index2){
		Part part = new Part (data, index1, index2);
		if (part.getName () != null)
		    parts.put (part.getName (), part);
		else
		    parts.put (part.getFilename (), part);
	    }
	    index1 = index2+boundaryBytes.length;
	    if (data[index1] == (byte)'-' && data[index1+1] == (byte)'-')
		break;
	    if (data[index1] == (byte)'\r')
		index1++;
	    if (data[index1] == (byte)'\n')
		index1++;
	}
    }
	
    private int getBoundaryIndex (byte[] data, int offset){
	if (offset >= data.length)
	    return -1;
	int end = data.length-boundaryBytes.length;
	loop:for (int i=offset; i<=end; i++){
	    for (int k=0; k<boundaryBytes.length; k++){
		if (data[i+k] != boundaryBytes[k])
		    continue loop;
	    }
	    return i;
	}
	return -1;
    }
	
	
    private static int getEOL (byte[] data, int offset, int end){
	byte n = (byte)'\n';
	for (int i=offset; i<end; i++)
	    if (data[i] == n)
		return i+1;
	return -1;
    }
	
    private static String getParameter (String param, String header){
	int index = header.indexOf (param);
	if (index == -1)
	    return null;
	index += param.length ();
	if (index == header.length ())
	    return "";
	int end = header.indexOf (';', index);
	if (end == -1)
	    end = header.length ();
	// we strip quotes
	if (header.charAt (index) == '"')
	    return header.substring (index+1, end-1);
	else
	    return header.substring (index, end);
    }
	
    private static String clean (String s){
	StringBuffer buff = new StringBuffer ();
	boolean inQuotes = false;
	for (int i=0; i<s.length (); i++){
	    char c = s.charAt (i);
	    if (c == '"'){
		inQuotes = (!inQuotes);
		buff.append (c);
	    }
	    else{
		if (inQuotes)
		    buff.append (c);
		else
		    if (c > ' ')
			//buff.append (Character.toLowerCase (c));
			buff.append (c);
	    }
	}
	return buff.toString ();
    }
	
	
    /**
     * This inner class encapsulates a Multipart Part from a form.
     * It is usually a parameter or a file.
     */
    
    public static class Part {
	private String disposition, name, filename, ctype="text/plain", transferEnc;
	private byte[] value;
		
	/**
	 * The constructor.
	 * Accepts multipart/form-data or multipart/mixed for the mime-type.
	 * @param mime_type the mime_type
	 * @param data the data
	 * @throws IllegalArgumentException if the mime-type is invalid
	 */
	public Part (String mime_type, byte[] data){
	    this.value = data;
	    if (MULTIPART_FORM_DATA.equals (mime_type))
		disposition = "form-data";
	    else if (MULTIPART_MIXED.equals (mime_type))
		disposition = "attachment";
	    else throw new IllegalArgumentException ("Unsupported MIME type");
	}
		
	private Part (byte[] data, int offset, int end)
	    throws Exception{
	    while(true){
		int index = getEOL (data, offset, end);
		if (index == -1)
		    throw new Exception ("Invalid Part");
		String header;
		if (data[index-2] == (byte)'\r')
		    header = clean (new String (data, offset, index-offset-2));
		else
		    header = clean (new String (data, offset, index-offset-1));
		offset = index;
				
		if ("".equals (header))
		    break;
		int limit = header.indexOf (':');
		if (limit == -1)
		    throw new Exception ("Invalid Part");
		String name = header.substring (0, limit);
		if ("content-disposition".equalsIgnoreCase (name))
		    parseContentDisposition (header.substring (limit+1));
		else if ("content-type".equalsIgnoreCase (name))
		    ctype = header.substring (limit+1);
		else if ("content-transfer-encoding".equalsIgnoreCase (name))
		    transferEnc = header.substring (limit+1);
	    }
	    if (name == null && filename == null)
		throw new Exception ("Invalid Part");
	    if (data[end-1] == (byte)'\n')
		end--;
	    if (data[end-1] == (byte)'\r')
		end--;
	    value = new byte[end-offset];
	    System.arraycopy (data, offset, value, 0, end-offset);
	}
		
	private void parseContentDisposition (String header){
	    int index = header.indexOf (';');
	    if (index == -1){
		disposition = header;
		name = null;
		filename = null;
	    }
	    else{
		disposition = header.substring (0, index);
		name = getParameter (";name=", header);
		filename = getParameter (";filename=", header);
	    }
	}
		
	/**
	 * Returns the content-disposition.
	 * @return the content-disposition if set, <code>null</code> otherwise.
	 */
	public String getContentDisposition (){
	    if (name == null && filename == null)
		return disposition;
	    StringBuffer buff = new StringBuffer (disposition);
	    if (name != null)
		buff.append (";name=\"").append (name).append ("\"");
	    if (filename != null)
		buff.append (";filename=\"").append (filename).append ("\"");
	    return buff.toString ();
	}
	/**
	 * Sets the content-disposition.
	 * @param header the content-disposition
	 */
	public void setContentDisposition (String header){
	    if (header == null)
		throw new NullPointerException ("Content disposition is null");
	    parseContentDisposition (clean (header));
	}
	/**
	 * Returns the part name.
	 * @return the part name if set, <code>null</code> otherwise.
	 */
	public String getName (){
	    return name;
	}
	/**
	 * Sets the part name.
	 * @param name the part name.
	 */
	public void setName (String name){
	    this.name = name;
	}
	/**
	 * Returns the file name.
	 * @return the file name if set, <code>null</code> otherwise.
	 */
	public String getFilename (){
	    return filename;
	}
	/**
	 * Sets the file name.
	 * @param filename the file name
	 */
	public void setFilename (String filename){
	    this.filename = filename;
	}
	/**
	 * Returns the content-type.
	 * @return the content-type.
	 */
	public String getContentType (){
	    return ctype;
	}
	/**
	 * Sets the content-type.
	 * @param type the content-type.
	 */
	public void setContentType (String type){
	    this.ctype = type;
	}
	/**
	 * Returns the character encoding.
	 * @return the character encoding if set, <code>null</code> otherwise.
	 */
	public String getCharacterEncoding (){
	    String charset = getParameter (";charset=", ctype);
	    return (charset != null)? charset : "iso-8859-1";
	}
	/**
	 * Sets the character encoding.
	 * @param enc the character encoding
	 */
	public void setCharacterEncoding (String enc){
	    this.ctype += ";charset=" + enc;
	}
	/**
	 * Returns the transfer-encoding.
	 * @return the transfer encoding if set, <code>null</code> otherwise.
	 */
	public String getTransferEncoding (){
	    return transferEnc;
	}
	/**
	 * Sets the transfer-encoding.
	 * @param enc the transfer encoding.
	 */
	public void setTransferEncoding (String enc){
	    this.transferEnc = enc;
	}
	/**
	 * Returns the data.
	 * @return the internal byte array used to store the data.
	 */
	public byte[] getData (){
	    return value;
	}
	/**
	 * Returns the data as a String.
	 * @return the data as a String or <code>null</code> if the character encoding is not set or not supported.
	 */
	public String getDataAsString (){
	    try{
		return new String (value, getCharacterEncoding ());
	    }catch(UnsupportedEncodingException e){
		return null;
	    }
	}
	/**
	 * Specifies if the part is a parameter.
	 * @return true or false
	 */
	public boolean isParameter (){
	    if (filename != null)
		return false;
	    return (ctype.startsWith ("text/plain"));
	}
	/**
	 * Specifies if the part is a file.
	 * @return true or false.
	 */
	public boolean isFile (){
	    if (filename != null)
		return true;
	    if (ctype.startsWith (MULTIPART_MIXED))
		return false;
	    return (!ctype.startsWith ("text/plain"));
	}
	/**
	 * Specifies if it is a multipart/mixed part.
	 * @return true or false
	 */
	public boolean isMutipleFiles (){
	    return (ctype.startsWith (MULTIPART_MIXED));
	}
    }
	
}
