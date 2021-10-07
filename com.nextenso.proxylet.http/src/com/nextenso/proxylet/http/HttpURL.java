package com.nextenso.proxylet.http;

import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.net.URL;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Hashtable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayInputStream;

/**
 * This class encapsulates an http url<br/>
 * It makes it easy to read or modify the fields<br/>
 * The url looks like:<br/>
 * url : http://host[:port][/file]<br/>
 * file : path[?query][#ref]<br/>
 * See RFC 1738 for details.
 */
public class HttpURL extends URLParametersParser implements Cloneable {
  /**
   * The http scheme "http".
   */
  public static final String PROTOCOL_HTTP = "http";
  /**
   * The https scheme "https".
   */
  public static final String PROTOCOL_HTTPS = "https";
  /**
   * The uplink:download scheme "uplink:download".
   */
  public static final String PROTOCOL_UPLINK_DOWNLOAD = "uplink:download";
  /**
   * The ftp scheme "ftp".
   */
  public static final String PROTOCOL_FTP = "ftp";
  
  protected String protocol;
  protected String ftpUrl;
  
  /**
   * Secure or not
   */
  protected boolean secure;
  /**
   * The host (can be a name or an IP)
   */
  protected String host;
  /**
   * The port number : must be >0
   */
  protected int port;
  /**
   * The path
   */
  protected String path;
  /**
   * The ref : what comes after '#'
   */
  protected String ref;
  
  //User for a ftp session
  protected String user;
  
  //Password for a ftp session
  protected String password;
  
  /**
   * Private Constructor for cloning
   */
  protected HttpURL(){
    super(null);
  }
  
  /**
   * Constructs a new HttpURL from a java.net.URL
   * @param url The java.net.URL
   * @exception MalformedURLException when the url is not a valid http url
   */
  public HttpURL(URL url) throws MalformedURLException {
    this(url.toString());
  }
  
  /**
   * Constructs a new HttpURL given the host and file (http on port 80 is assumed)
   * @param host The host (name or IP)
   * @param file The file (can be null or blank)
   * @exception MalformedURLException when the url is not a valid http url (invalid host or file)
   */
  public HttpURL(String host,
		 String file) throws MalformedURLException {
    this(host, 80, file);
  }
  
  /**
   * Constructs a new HttpURL given the host, port and file (http is assumed)
   * @param host The host (name or IP)
   * @param port The port
   * @param file The file (can be null or blank)
   * @exception MalformedURLException when the url is not a valid http url (invalid host, port or file)
   */
  public HttpURL(String host, int port,
		 String file) throws MalformedURLException {
    super(null);
    setSecure(false);
    setHost(host);
    setPort(port);
    setFile(file);
    this.protocol = PROTOCOL_HTTP;
  }
  
  /**
   * Constructs a new HttpURL from its String representation.
   * It can optionally start with "url:"<br/>
   * @param spec The String representation
   * @exception MalformedURLException when the url is not a valid http url
   */
  public HttpURL(String spec) throws MalformedURLException {
    super(null);
    setValue(spec);
  }
  
  /**
   * Sets the protocol security.
   *
   * @param secure (true == https , false == http)
   */
  protected void setSecure(boolean secure) {
    this.secure = secure;
  }
  
  /**
   * Specifies if the protocol used is secure.
   * @return true if https - false if http
   */
  public boolean isSecure(){
    return secure;
  }
  
  /**
   * Sets the url to match the specified String representation.
   * The String can optionally start with "url:"<br/>
   * @param spec The String representation
   * @exception MalformedURLException when the url is not a valid http url
   */
  public void setValue(String spec) throws MalformedURLException {
    
    if (spec == null)
      throw new MalformedURLException("Invalid URL (null)");
    // we trim
    int start = 0, limit = spec.length();
    while ((limit > 0) && (spec.charAt(limit - 1) <= ' ')) {
      limit--; //eliminate trailing whitespaces
    }
    while ((start < limit) && (spec.charAt(start) <= ' ')) {
      start++; // eliminate leading whitespaces
    }
    if (spec.regionMatches(true, start, "url:", 0, 4)) {
      start += 4;
    }
    if (spec.regionMatches(false, start, HttpUtils.HTTP, 0, HttpUtils.HTTP.length())) {
      start = setValueProtocolHttp(start);
    } else if (spec.regionMatches(false, start, HttpUtils.HTTPS, 0, HttpUtils.HTTPS.length())) {
      start = setValueProtocolHttps(start);
    } else if (spec.regionMatches(false, start, PROTOCOL_UPLINK_DOWNLOAD, 0, PROTOCOL_UPLINK_DOWNLOAD.length())) {
      setValueProtocolUplink(spec, start, limit);
      return;
    } else if (spec.regionMatches(false, start, PROTOCOL_FTP, 0, PROTOCOL_FTP.length())) {
      start = setValueProtocolFtp(spec, start, limit);
    } else {
      throw new MalformedURLException("Invalid protocol");
    }
    
    if (start >= limit)
      throw new MalformedURLException("Invalid URL (no host)");
    
    // we determine the index where the authority ends and the file starts.
    int index = determineIndexFile(spec, start, limit);

    setAuthority(spec.substring(start, index));
    setFile(spec.substring(index, limit));
  }

  private int determineIndexFile(String spec, int start, int limit) {
	int index = limit;
    boolean parsingIPV6 = (spec.charAt(start) == '[');
	
    loop: for (int i=start; i<limit; i++){
      switch (spec.charAt(i)){
      case ']':
	if (parsingIPV6) {
	  parsingIPV6 = false;
	  break;
	}
      case '/':
      case '?':
      case '#':
	if (parsingIPV6) {
	  break;
	}
	index = i; break loop;
      }
    }
	return index;
}

  private int setValueProtocolFtp(String spec, int start, int limit) throws MalformedURLException {
	protocol = PROTOCOL_FTP;
	ftpUrl = spec.substring(start, limit);
	start += HttpUtils.FTP.length();
	setSecure(false);
	setPort(21);
	return start;
}

  private void setValueProtocolUplink(String spec, int start, int limit) {
	protocol = PROTOCOL_UPLINK_DOWNLOAD;
      setSecure(false);
      port = -1;
      path = spec.substring(start, limit);
}

private int setValueProtocolHttps(int start) throws MalformedURLException {
	protocol = PROTOCOL_HTTPS;
      start += HttpUtils.HTTPS.length();
      setSecure(true);
      setPort(443);
	return start;
}

private int setValueProtocolHttp(int start) throws MalformedURLException {
	protocol = PROTOCOL_HTTP;
      start += HttpUtils.HTTP.length();
      setSecure(false);
      setPort(80);
	return start;
}
  
  /**
   * Sets the host.The format of the host conforms to RFC 2732, i.e. for a literal IPv6 address, 
   * this method accepts the IPv6 address enclosed in square brackets ('[' and ']').
   *
   * @param host The host (name or IP)
   * @exception MalformedURLException when the host is not valid
   */
  public void setHost(String host) throws MalformedURLException {
    if (protocol == PROTOCOL_FTP || host.charAt(0) == '[' /* IPV6 */) {
      //We are in the FTP case
      this.host = host;
    } else {
      if (!HttpUtils.isValidHost(host))
	throw new MalformedURLException("Invalid host");
      // we switch the host to lower case if upper letters are present (not often)
      char c;
      for (int i=0; i<host.length(); i++){
	c = host.charAt(i);
	if ((c > '\u0040') && (c < '\u005b')){ // means 'A'<=c<='Z'
	  this.host = host.toLowerCase();
	  return;
	}
      }
      this.host = host;
    }
  }
  
  
  /**
   * Sets the port
   * @param port The port
   * @exception MalformedURLException when the port is not valid
   */
  public void setPort(String port) throws MalformedURLException {
    if (port == null)
      throw new MalformedURLException("Invalid port number (null)");
    try{
      setPort(Integer.parseInt(port));
    }
    catch(NumberFormatException e){
      throw new MalformedURLException("Invalid port number ("+
				      port + ")");
    }
  }
  
  /**
   * Sets the port
   * @param port The port
   * @exception MalformedURLException when the port is not valid
   */
  public void setPort(int port) throws MalformedURLException {
    if (!HttpUtils.isValidURLPort(port))
      throw new MalformedURLException("Invalid port number ("+
				      port + ")");
    this.port = port;
  }
  
  /**
   * Sets the file (the path, query and ref are set).
   * A slash ('/') is added at the beginning of the file if it does not start with one.
   * @param file The file (given as URL-encoded)
   * @exception MalformedURLException when the file is not valid
   */
  public void setFile(String file) throws MalformedURLException {
    if (file == null || file.length() == 0 || "/".equals(file)) {
      setPath("/");
      setRef("");
      setQuery("");
      return;
    }
    int refIndex = file.indexOf('#');
    if ((refIndex == -1) || (refIndex == file.length()-1))
      setRef("");
    else
      setRef(file.substring(refIndex+1));
    if (refIndex == -1)
      refIndex = file.length();
    int queryIndex = file.indexOf('?');
    if (queryIndex > refIndex || queryIndex == -1)
      queryIndex = refIndex;
    setPath(file.substring(0, queryIndex));
    if (queryIndex < refIndex)
      setQuery(file.substring(queryIndex, refIndex));
    else
      setQuery("");
  }
  
  /**
   * Sets the authority ( authority := (port == 80) ? host : host + ":" + port ).
   * @param auth The authority
   * @exception MalformedURLException when the host or the port is invalid
   */
  public void setAuthority(String auth) throws MalformedURLException {
    if (auth == null)
      throw new MalformedURLException("Invalid authority (null)");
    
    if ( !(protocol == PROTOCOL_FTP) ) {
      if (auth.charAt(0) == '[') {
	// IPV6 URL
	int lastIpv6Bracket = auth.indexOf(']', 1);
	if (lastIpv6Bracket == -1) {
	  throw new MalformedURLException("Invalid authority (missing ipv6 end bracket): " + auth);
	}
	setHost (auth.substring(0, lastIpv6Bracket+1));
	int index = auth.indexOf(':', lastIpv6Bracket+1);
	if (index == auth.length() - 1)
	  throw new MalformedURLException("Invalid authority (missing port)");
	if (index != -1) {
	  setPort(auth.substring(index+1));
	}
      } else {
	// Not an IPV6 URL
	int index = auth.indexOf(':');
	if (index == auth.length() - 1)
	  throw new MalformedURLException("Invalid authority (missing port)");
	if (index == -1)
	  setHost(auth);
	else {
	  setHost(auth.substring(0, index));
	  setPort(auth.substring(index+1));
	}
      }
    } else {
      //We are in the FTP case
      int arrobasIndex = auth.indexOf('@');
      int columnIndex = auth.indexOf(':', arrobasIndex);
      if (columnIndex == -1) {
	setHost(auth.substring(arrobasIndex+1));
      } else {
	setHost(auth.substring(arrobasIndex+1, columnIndex));
	setPort(auth.substring(columnIndex+1));
      }
    }
  }
  
  /**
   * Sets the path.
   * A slash ('/') is added at the beginning of the path if it does not start with one.
   * @param path The path (given as URL-encoded)
   * @exception MalformedURLException when the path is invalid
   */
  public void setPath(String path) throws MalformedURLException {
    if (path == null || path.length() == 0){
      this.path = "/";
      return;
    }
    //if (!HttpUtils.isValidURLPath(path))
    //throw new MalformedURLException("Invalid path");
    
    // we add a leading '/' if missing
    this.path = (path.charAt(0) == '/')? path : '/'+path;
  }
  
  /**
   * Sets the ref (ignores the first leading '#' if present).
   * @param ref The ref (given as URL-encoded)
   * @exception MalformedURLException when the ref is invalid
   */
  public void setRef(String ref) throws MalformedURLException {
    if (ref == null || ref.length() == 0 || "#".equals(ref))
      this.ref = "";
    else
      // we remove the leading '#' if present
      this.ref = (ref.charAt(0) == '#')? ref.substring(1) : ref;
  }
  
  /**
   * Sets the query (ignores the first leading '?' if present).
   * NOTE: this method DOES NOT validate the query string
   * @param query The query (given as URL-encoded)
   */
  public void setQuery(String query){
    if (query == null || query.length() == 0 || "?".equals(query)) {
      clearQuery();
      return;
    }
    // we removed the query validation (because of Yahoo)
    // we don't parse the query by default
    // we cache the value
    reset(query);
  }
   

  /**
   * Returns the protocol.
   * @return the protocol.
   */
  public String getProtocol() {
    return protocol;
  }
  
  /**
   * Gets the host name of this URL. The format of the host conforms to RFC 2732, i.e. for a literal IPv6 address, 
   * this method will return the IPv6 address enclosed in square brackets ('[' and ']').
   * @return the host
   */
  public String getHost() {
    return host;
  }
  
  /**
   * Returns the port
   * @return the port
   */
  public int getPort() {
    return port;
  }
  
  /**
   * Returns the file ( path [?query] [#ref] ) - always start with '/'
   * @return the file (as URL-encoded)
   */
  public String getFile() {
    String query = getQuery();
    boolean hasNoQuery = ("".equals(query));
    boolean hasNoRef = ("".equals(ref));
    if (hasNoQuery && hasNoRef)
      return path;
    StringBuffer buffer = new StringBuffer(path);
    if (!hasNoQuery) {
      buffer.append('?');
      buffer.append(query);
    }
    if (!hasNoRef) {
      buffer.append('#');
      buffer.append(ref);
    }
    return buffer.toString();
  }
  
  /**
   * Returns the authority ( host [":" + port] )
   * @return the authority
   */
  public String getAuthority() {
    if (isSecure()){
      return (port == 443) ? host : host + ":" + port;
    }
    else
      return ( (port == 80) || (port == 21) ) ? host : host + ":" + port;
  }
  
  /**
   * Returns the path - always start with '/'
   * @return the path (as URL-encoded)
   */
  public String getPath() {
    return path;
  }
  
  /**
   * Returns the ref - does not prefix with '#'
   * @return the ref (as URL-encoded)
   */
  public String getRef() {
    return ref;
  }
     
  /**
   * Performs a comparison based on the String representation.
   * Only another HttpURL or a java.net.URL can be equal
   * @param o another Object
   * @return true or false
   */
  public boolean equals(Object o) {
    // TODO: implements IPV6 host/ipaddr comparison

    if (!((o instanceof HttpURL)||(o instanceof URL)) || o == null)
      return false;
    String url = null;
    if (o instanceof HttpURL)
      url = ((HttpURL) o).toString();
    else
      url = ((URL) o).toString();
    return (this.toString().equals(url));
  }
  
  
  /**
   * Returns a java.net.URL from this object.
   * <b>NOTE: the returned URL will ALWAYS use http:// as the protocol</b> since java.net.URL does not support https.
   * @return the twin URL
   */
  public URL toURL() {
    boolean isSecure = secure;
    setSecure(false);
    try {
      return new URL(toString());
    } catch (MalformedURLException e) {
      // should never happen
      return null;
    } finally {
      setSecure(isSecure);
    }
  }
  
  /**
   * Returns a String representation
   * @return the String representation
   */
  public String toString() {
    if (protocol == PROTOCOL_UPLINK_DOWNLOAD){
      return path;
    }
    if (protocol == PROTOCOL_FTP){
      return ftpUrl;
    }    
    StringBuffer buffer = new StringBuffer();
    buffer.append(getProtocol());
    buffer.append("://");
    buffer.append(getAuthority());
    buffer.append(getFile());
    return buffer.toString();
  }
  
  /**
   * Returns a hashed value for this object
   * @return the hashed value
   */
  public int hashCode() {
    return toString().hashCode();
  }
  
  /**
   * Clones the URL
   */
  public Object clone(){
    HttpURL clone = new HttpURL();
    clone.secure = secure;
    clone.host = host;
    clone.port = port;
    clone.protocol = protocol;
    clone.user = user;
    clone.password = password;
    clone.ftpUrl = ftpUrl;
    clone.path = path;
    clone.ref = ref;
    clone.reset(getQuery());
    return clone;
  }
  
  /**
   * Returns a full String representation FOR DEBUGGING ONLY
   * @return the String representation
   */
  public String dump() {
    String s = "URL:<"+toString() + ">\n";
    s += "Secure:<"+isSecure() + ">\n";
    s += "Auth:<"+getAuthority() + ">\n";
    s += "Host:<"+host + ">\n";
    s += "Port:<"+port + ">\n";
    s += "File:<"+getFile() + ">\n";
    s += "Path:<"+path + ">\n";
    s += "Query:<"+getQuery() + "> Decoded :<"+
      HttpUtils.decodeURL(getQuery()) + ">\n";
    s += "Ref:<"+ref + ">\n";
    return s;
  }
  
}














