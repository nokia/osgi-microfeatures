// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.http;

import java.util.Hashtable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;

/**
 * HttpUtils wraps static utility methods and constants related to HTTP.
 */

public class HttpUtils {
        
    // we prevent creating instances
    private HttpUtils(){}

    
    /**
     * Gets a default http reason string corresponding to a given http response 
     * code.
     * 
     * @param code	An http response status code
     * @return		The string corresponding to the http response status 
     *			code
     */
    public static String getHttpReason (int code) {
	String r = (String) reasons.get (new Integer (code));
	return (r != null)? r : String.valueOf (code);
    }
  
    /**
     * Same as decodeURL(String s, "8859_1") : assumes iso-8859-1 as encoding (usual case).
     * 
     * @param s   the String to decode
     * @return   the decoded String (null if the argument is null)
     */
    public static String decodeURL(String s) {
	try{
	    return decodeURL(s, "8859_1");
	}catch(UnsupportedEncodingException e){
	    // unlikely
	    return null;
	}
    }
    
    /**
     * Decodes an URL-encoded String given its original encoding.
     * 
     * @param s          the String to decode
     * @param encoding   the original String encoding before url-encoding
     * @return   the decoded String (null if the argument is null)
     */
    public static String decodeURL(String s, String encoding) throws UnsupportedEncodingException{
	if (s == null)
	    return null;
	if (encoding == null)
	    throw new NullPointerException ("encoding is null");;
	ByteArrayOutputStream os = new ByteArrayOutputStream();
        for(int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
	    case '+':
		os.write(32);
		break;
	    case '%':
		try {
		    os.write(Integer.parseInt(s.substring(i+1,i+3),16));
		} catch (NumberFormatException e) {
		    throw new IllegalArgumentException();
		}
		i += 2;
		break;
	    default:
		os.write((int)c);
		break;
            }
        }
	return new String (os.toByteArray(), encoding);
    }
  
    /**
     * Same as java.net.URLEncoder.encode(String s).
     * No optimization was made. The method was added for conveniency.
     * 
     * @param s   the String to encode
     * @return   the encoded String (null if the argument is null)
     */
    public static String encodeURL(String s) {
	if (s == null)
	    return null;
	return java.net.URLEncoder.encode(s);
    }
  
    /**
     * Indicates if the specified String is a valid HTTP URL.
     *
     * @param s   the String to check
     * @return   true or false
     */
    public static boolean isValidURL(String s) {
	if (s == null)
            return false;

	int n = 0;

	// we check the protocol
	if (s.startsWith(HTTP))
            n = 7;
	else if (s.startsWith(HTTPS))		
            n = 8;
	else if (s.startsWith(FTP))		
            n = 6;						
	else
            return false;

	// we check that there is more than just the protocol
	if (n == s.length())
            return false;
            
	int indexSlash = s.indexOf('/', n); // we skip 'http(s)://'
	if (indexSlash == -1)
            // no file is specified
            return isValidURLAuthority(s.substring(n));
	if (indexSlash == s.length() - 1)
            // no file is specified (single '/' at the String end)
            return isValidURLAuthority(s.substring(n, indexSlash));
	return ( isValidURLAuthority(s.substring(n, indexSlash)) &&
		 isValidURLFile(s.substring(indexSlash + 1)));
    }

    /**
     * Indicates if the specified String is a valid Authority (host[:port]).
     *
     * @param s   the String to check
     * @return   true or false
     */
    public static boolean isValidURLAuthority(String s) {
	if (s == null)
            return false;
	int index = s.indexOf(':');
	if (index == -1)
            return isValidHost(s);
	if (index == s.length() - 1)
            // the port is missing
            return false;
	return (isValidHost(s.substring(0, index)) &&
		isValidURLPort(s.substring(index + 1)));
    }

    /**
     * Indicates if the specified String is a valid URL file (path[?query][#ref]).
     *
     * @param s   the String to check
     * @return   true or false
     */
    public static boolean isValidURLFile(String s) {
	if (s == null)
            return false;
	int indexRef = s.indexOf('#');
	int indexQuery = s.indexOf('?');
	if (indexRef == -1)
            indexRef = s.length();
	if (indexQuery == -1 || indexQuery > indexRef)
            indexQuery = indexRef;
	boolean valid = isValidURLPath(s.substring(0, indexQuery));
	if (indexQuery != indexRef)
            valid = (valid && isValidURLQuery(s.substring(indexQuery, indexRef)));
	return (valid);
    }

    /**
     * Indicates if the specified String is a valid Host (name or IP).
     *
     * @param host   the String to check
     * @return      true or false
     */
    public static boolean isValidHost(String host) {
	if (host == null || host.length()==0)
            return false;
	if (isDigit(host.charAt(0))) // then likely an IP
            return (isValidHostNumber(host) || isValidHostName(host));
	else
            return (isValidHostName(host) || isValidHostNumber(host));
    }
  

    /**
     * Indicates if the specified String is a valid Host name.
     *
     * @param name   the String to check
     * @return      true or false
     */
    public static boolean isValidHostName(String name) {
	if (name == null)
            return false;
	int dot = name.lastIndexOf('.');
	if (dot == -1)
	    return isValidURLTopLabel(name, 0, name.length());
	if (dot == 0 || dot == name.length() - 1)
	    return false;
	return ( isValidURLDomainLabelSegment(name, 0, dot+1) &&
		 isValidURLTopLabel(name, dot+1, name.length()));
    }


    /**
     * Indicates if the specified String is a valid Host number (IP).
     *
     * @param ip   the String to check
     * @return    true or false
     */
    public static boolean isValidHostNumber(String ip) {
	if (ip == null)
	    return false;
	int start = 0, stop = 0;
	for (int i = 0; i < 4; i++) {
	    stop = (i == 3)? ip.length() : ip.indexOf('.', stop);
	    if (stop == -1 || stop == ip.length() - 1)
		return false;
	    if (!isDigits(ip, start, stop))
		return false;
	    start = ++stop;
	}
	return true;
    }

    /**
     * Indicates if the specified String is a valid port number.
     *
     * @param s   the String to check
     * @return      true or false
     */
    public static boolean isValidURLPort(String s) {
	if (s == null)
	    return false;
	try {
	    return isValidURLPort(Integer.parseInt(s));
	} catch (NumberFormatException e) {
	    return false;
	}
    }

    /**
     * Indicates if the specified int is a valid port number.
     *
     * @param i      the int to check
     * @return      true or false
     */
    public static boolean isValidURLPort(int i) {
	return (i > 0 && i < 65536);
    }


    /**
     * Indicates if the specified String is a valid URL path.
     *
     * @param s   the String to check
     * @return   true or false
     */
    public static boolean isValidURLPath(String s) {
	if (s == null)
	    return false;
	return isHpath(s);
    }

    /**
     * Indicates if the specified String is a valid URL query (a leading '?' is accepted).
     *
     * @param s   the String to check
     * @return   true or false
     */
    public static boolean isValidURLQuery(String s) {
	if (s == null)
	    return false;
	if (s.length() == 0)
	    return true;
	int offset = 0;
	if (s.charAt(0) == '?')
            if (s.length() == 1)
		return true;
            else
		offset = 1;
	// search happens to match hsegment
	return isHsegment(s, offset);
    }

  
  
    /***************************************************
     *********Inner methods for URL validation **********
     ***************************************************/

    private static boolean isDigit(char c) {
	// fast check (if not digit then likely > '9')
	return ((c <= '9') && (c >= '0'));
    }

    private static boolean isLowAlpha(char c) {
	// fast check (if not low alpha then likely < 'a')
	return ((c >= 'a') && (c <= 'z'));
    }

    private static boolean isHiAlpha(char c) {
	return ((c >= 'A') && (c <= 'Z'));
    }

    private static boolean isAlpha(char c) {
	return (isLowAlpha(c) || isHiAlpha(c));
    }

    private static boolean isAlphaDigit(char c) {
	return (isDigit(c) || isAlpha(c));
    }

    private static boolean isDigits(String s, int start, int stop) {
	if (start >= stop)
	    return false;
	for (int i = start; i < stop; i++) {
	    if (!isDigit(s.charAt(i)))
		return false;
	}
	return true;
    }

    private static boolean isSafe(char c) {
	return (c == '$'  || c == '-' || c == '_' || c == '.' || c == '+');
    }

    private static boolean isExtra(char c) {
	return (c == '!' || c == '*' || c == '\'' || c == '(' ||
		c == ')' || c == ',');
    }

    private static boolean isHex(char c) {
	return ((isDigit(c)) || (c >= 'A' && c <= 'F') ||
		(c >= 'a' && c <= 'f'));
    }

    // we assume that '%' at index 'offset' was checked
    private static boolean isEscape(String s, int offset) {
	if ((s.length()-offset) < 3)
            return false;
	if (!isHex(s.charAt(offset+1)))
	    return false;
	if (!isHex(s.charAt(offset+2)))
	    return false;
	return true;
    }

    private static boolean isUnreserved(char c) {
	return (isAlpha(c) || isDigit(c) || isSafe(c) ||
		isExtra(c));
    }


    private static boolean isHsegmentSpecialChar(char c) {
	return (c == '&' || c == '=' || c == '@' || c == ':' ||
		c == ';');
    }

    private static boolean isHsegment(String s, int offset) {
	for (int i = offset; i < s.length();) {
	    char c = s.charAt(i);
	    if (isUnreserved(c) || isHsegmentSpecialChar(c)){
		i++;
		continue;
	    }
	    if (c == '%') {
		// escape
		if (!isEscape(s, i))
		    return false;
		i += 3;
		continue;
	    }
	    return false;
	}
	return true;
    }

    private static boolean isHpath(String s) {
	// same as isHsegment except that there can be '/'
	// a specific method is needed since isValidURLQuery uses isHsegment
	for (int i = 0; i < s.length();) {
	    char c = s.charAt(i);
	    if (c == '/' || isUnreserved(c) || isHsegmentSpecialChar(c)){
		i++;
		continue;
	    }
	    if (c == '%') {
		// escape
		if (!isEscape(s, i))
		    return false;
		i += 3;
		continue;
	    }
	    return false;
	}
	return true;
    }

    private static boolean isValidURLTopLabel(String s, int start, int stop) {
	if (start >= stop)
	    return false;
	if (stop-start == 1)
	    return isAlpha(s.charAt(start));
	char c_start = s.charAt(start);
	char c_end = s.charAt(stop - 1);
	if (!isAlpha(c_start))
	    return false;
	if (!isAlphaDigit(c_end))
	    return false;
	for (int i = start+1; i < stop - 1; i++)
	    if (!isAlphaDigitHyphen(s.charAt(i)))
		return false;
	return true;
    }

    private static boolean isValidURLDomainLabelSegment(String s, int start, int stop) {
	int begin = start, end = start;
	while (end < stop) {
	    end = s.indexOf('.', end);
	    if (end == -1 || end >= stop)
		return false;
	    if (!isValidURLDomainLabel(s, begin, end))
		return false;
	    begin = ++end;
	}
	return true;
    }

    private static boolean isValidURLDomainLabel(String s, int start, int stop) {
	if (start >= stop)
	    return false;
	if (stop-start == 1)
	    return isAlphaDigit(s.charAt(start));
	char c_start = s.charAt(start);
	char c_end = s.charAt(stop - 1);
	if (!isAlphaDigit(c_start))
	    return false;
	if (!isAlphaDigit(c_end))
	    return false;
	for (int i = start+1; i < stop - 1; i++)
	    if (!isAlphaDigitHyphen(s.charAt(i)))
		return false;
	return true;
    }

    private static boolean isAlphaDigitHyphen(char c) {
	return (isAlphaDigit(c) || c == '-');
    }

    // http constants
    /**
     * The URL prefix "http://"
     */
    public final static String HTTP = "http://";
    /**
     * The URL prefix "https://"
     */
    public final static String HTTPS = "https://";
    /**
     * The URL prefix "ftp://"
     */
    public final static String FTP = "ftp://";
    /**
     * The HTTP version "HTTP/2.0"
     */
    public final static String HTTP_20 = "HTTP/2.0";
    /**
     * The HTTP version "HTTP/1.1"
     */
    public final static String HTTP_11 = "HTTP/1.1";
    /**
     * The HTTP version "HTTP/1.0"
     */
    public final static String HTTP_10 = "HTTP/1.0";
    /**
     * The HTTP header "accept-charset"
     */
    public final static String ACCEPT_CHARSET = "Accept-Charset";
    /**
     * The HTTP header "cache-control"
     */
    public final static String CACHE_CONTROL = "Cache-Control";
    /**
     * The HTTP header "connection"
     */
    public final static String CONNECTION = "Connection";
    /**
     * The HTTP header "proxy-connection"
     */
    public final static String PROXY_CONNECTION = "Proxy-Connection";
    /**
     * The HTTP header "date"
     */
    public final static String DATE = "Date";
    /**
     * The HTTP header "pragma"
     */
    public final static String PRAGMA = "Pragma";
    /**
     * The HTTP header "transfer-encoding"
     */
    public final static String TRANSFER_ENCODING = "Transfer-Encoding";
    /**
     * The HTTP header "upgrade"
     */
    public final static String UPGRADE = "Upgrade";
    /**
     * The HTTP header "via"
     */
    public final static String VIA = "Via";
    /**
     * The HTTP header "from"
     */
    public final static String FROM = "From";
    /**
     * The HTTP header "warning"
     */
    public final static String WARNING = "Warning";
    /**
     * The HTTP header "accept"
     */
    public final static String ACCEPT = "Accept";
    /**
     * The HTTP header "host"
     */
    public final static String HOST = "Host";
    /**
     * The HTTP header "accept-encoding"
     */
    public final static String ACCEPT_ENCODING = "Accept-Encoding";
    /**
     * The HTTP header "accept-ranges"
     */
    public final static String ACCEPT_RANGES = "Accept-Ranges";
    /**
     * The HTTP header "accept-language"
     */
    public final static String ACCEPT_LANGUAGE = "Accept-Language";
    /**
     * The HTTP header "age"
     */
    public final static String AGE = "Age";
    /**
     * The HTTP header "etag"
     */
    public final static String ETAG = "Etag";
    /**
     * The HTTP header "location"
     */
    public final static String LOCATION = "Location";
    /**
     * The HTTP header "proxy-authenticate"
     */
    public final static String PROXY_AUTHENTICATE = "Proxy-Authenticate";
    /**
     * The HTTP header "proxy-authorization"
     */
    public final static String PROXY_AUTHORIZATION = "Proxy-Authorization";
    /**
     * The HTTP header "authorization"
     */
    public final static String AUTHORIZATION = "Authorization";
    /**
     * The HTTP header "retry-after"
     */
    public final static String RETRY_AFTER = "Retry-After";
    /**
     * The HTTP header "server"
     */
    public final static String SERVER = "Server";
    /**
     * The HTTP header "vary"
     */
    public final static String VARY = "Vary";
    /**
     * The HTTP header "www-authenticate"
     */
    public final static String WWW_AUTHENTICATE = "Www-Authenticate";
    /**
     * The HTTP header "content-base"
     */
    public final static String CONTENT_BASE = "Content-Base";
    /**
     * The HTTP header "allow"
     */
    public final static String ALLOW = "Allow";
    /**
     * The HTTP header "content-encoding"
     */
    public final static String CONTENT_ENCODING = "Content-Encoding";
    /**
     * The HTTP header "content-language"
     */
    public final static String CONTENT_LANGUAGE = "Content-Language";
    /**
     * The HTTP header "content-length"
     */
    public final static String CONTENT_LENGTH = "Content-Length";
    /**
     * The HTTP header "content-location"
     */
    public final static String CONTENT_LOCATION = "Content-Location";
    /**
     * The HTTP header "content-md5"
     */
    public final static String CONTENT_MD5 = "Content-Md5";
    /**
     * The HTTP header "content-range"
     */
    public final static String CONTENT_RANGE = "Content-Range";
    /**
     * The HTTP header "content-type";
     */
    public final static String CONTENT_TYPE = "Content-Type";
    /**
     * The HTTP header "expect"
     */
    public final static String EXPECT = "Expect";
    /**
     * The HTTP header "expires
     */
    public final static String EXPIRES = "Expires";
    /**
     * The HTTP header "last-modified"
     */
    public final static String LAST_MODIFIED = "Last-Modified";
    /**
     * The HTTP header "if-modified-since"
     */
    public final static String IF_MODIFIED_SINCE = "If-Modified-Since";
    /**
     * The HTTP header "if-none-match"
     */
    public final static String IF_NONE_MATCH = "If-None-Match";
    /**
     * The HTTP header "if-unmodified-since"
     */
    public final static String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
    /**
     * The HTTP header "if-match"
     */
    public final static String IF_MATCH = "If-Match";
    /**
     * The HTTP header "if-range"
     */
    public final static String IF_RANGE = "If-Range";
    /**
     * The HTTP header "referer"
     */
    public final static String REFERER = "Referer";
    /**
     * The HTTP header "range"
     */
    public final static String RANGE = "Range";
    /**
     * The HTTP header "max-forwards"
     */
    public final static String MAX_FORWARDS = "Max-Forwards";
    /**
     * The HTTP header "cookie"
     */
    public final static String COOKIE = "Cookie";
    /**
     * The HTTP header "keep-alive"
     */
    public final static String KEEP_ALIVE = "Keep-Alive";
    /**
     * The HTTP header "close"
     */
    public final static String CLOSE = "Close";
    /**
     * The HTTP header "set-cookie"
     */
    public final static String SET_COOKIE = "Set-Cookie";
    /**
     * The HTTP header "user-agent"
     */
    public final static String USER_AGENT = "User-Agent";
    
    // http methods ...
    /**
     * The HTTP method "OPTIONS"
     */
    public final static String METHOD_OPT = "OPTIONS";
    /**
     * The HTTP method "GET"
     */
    public final static String METHOD_GET = "GET";
    /**
     * The HTTP method "HEAD"
     */
    public final static String METHOD_HEAD = "HEAD";
    /**
     * The HTTP method "POST"
     */
    public final static String METHOD_POST = "POST";
    /**
     * The HTTP method "PUT"
     */
    public final static String METHOD_PUT = "PUT";
    /**
     * The HTTP method "DELETE"
     */
    public final static String METHOD_DELETE = "DELETE";
    /**
     * The HTTP method "TRACE"
     */
    public final static String METHOD_TRACE = "TRACE";
    /**
     * The HTTP method "CONNECT"
     */
    public final static String METHOD_CONNECT = "CONNECT";
    
    // misc. values
    /**
     * The utility String "localhost"
     */
    public final static String LOCALHOST = "localhost";
    
    private static Hashtable reasons = new Hashtable ();
    
    static {
	reasons.put (new Integer (200), "OK");
	reasons.put (new Integer (201), "Created");
	reasons.put (new Integer (202), "Accepted");
	reasons.put (new Integer (203), "Non-Authoritative Information");
	reasons.put (new Integer (204), "No Content");
	reasons.put (new Integer (205), "Reset Content");
	reasons.put (new Integer (206), "Partial Content");
	reasons.put (new Integer (300), "Multiple Choices");
	reasons.put (new Integer (301), "Moved Permanently");
	reasons.put (new Integer (302), "Temporary Redirect");
	reasons.put (new Integer (303), "See Other");
	reasons.put (new Integer (304), "Not Modified");
	reasons.put (new Integer (305), "Use Proxy");
	reasons.put (new Integer (400), "Bad Request");
	reasons.put (new Integer (401), "Unauthorized");
	reasons.put (new Integer (402), "Payment Required");
	reasons.put (new Integer (403), "Forbidden");
	reasons.put (new Integer (404), "Not Found");
	reasons.put (new Integer (405), "Method Not Allowed");
	reasons.put (new Integer (406), "Not Acceptable");
	reasons.put (new Integer (407), "Proxy Authentication Required");
	reasons.put (new Integer (408), "Request Time-Out");
	reasons.put (new Integer (409), "Conflict");
	reasons.put (new Integer (410), "Gone");
	reasons.put (new Integer (411), "Length Required");
	reasons.put (new Integer (412), "Precondition Failed");
	reasons.put (new Integer (413), "Request Entity Too Large");
	reasons.put (new Integer (414), "Request-URI Too Large");
	reasons.put (new Integer (415), "Unsupported Media Type");
	reasons.put (new Integer (500), "Internal Server Error");
	reasons.put (new Integer (501), "Not Implemented");
	reasons.put (new Integer (502), "Bad Gateway");
	reasons.put (new Integer (503), "Service Unavailable");
	reasons.put (new Integer (504), "Gateway Timeout");
	reasons.put (new Integer (505), "HTTP Version Not Supported");
    }
  
  
}
