// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.http;

import java.util.Enumeration;
import java.util.Properties;

import alcatel.tess.hometop.gateways.utils.Hashtable;
import alcatel.tess.hometop.gateways.utils.IntHashtable;
import alcatel.tess.hometop.gateways.utils.QuotedStringTokenizer;

/**
 * 
 * The HTTP Utils.
 */
public class HttpUtils {
  private final static IntHashtable REASONS = new IntHashtable();
  
  //
  // initialize http status codes
  //
  static {
    REASONS.put(200, "OK");
    REASONS.put(201, "Created");
    REASONS.put(202, "Accepted");
    REASONS.put(203, "Non-Authoritative Information");
    REASONS.put(204, "No Content");
    REASONS.put(205, "Reset Content");
    REASONS.put(206, "Partial Content");
    REASONS.put(300, "Multiple Choices");
    REASONS.put(301, "Moved Permanently");
    REASONS.put(302, "Temporary Redirect");
    REASONS.put(303, "See Other");
    REASONS.put(304, "Not Modified");
    REASONS.put(305, "Use Proxy");
    REASONS.put(400, "Bad Request");
    REASONS.put(401, "Unauthorized");
    REASONS.put(402, "Payment Required");
    REASONS.put(403, "Forbidden");
    REASONS.put(404, "Not Found");
    REASONS.put(405, "Method Not Allowed");
    REASONS.put(406, "Not Acceptable");
    REASONS.put(407, "Proxy Authentication Required");
    REASONS.put(408, "Request Time-Out");
    REASONS.put(409, "Conflict");
    REASONS.put(410, "Gone");
    REASONS.put(411, "Length Required");
    REASONS.put(412, "Precondition Failed");
    REASONS.put(413, "Request Entity Too Large");
    REASONS.put(414, "Request-URI Too Large");
    REASONS.put(415, "Unsupported Media Type");
    REASONS.put(500, "Internal Server Error");
    REASONS.put(501, "Not Implemented");
    REASONS.put(502, "Bad Gateway");
    REASONS.put(503, "Service Unavailable");
    REASONS.put(504, "Gateway Timeout");
    REASONS.put(505, "HTTP Version Not Supported");
  }
  
  /**
   * Returns the value of the specified header (without parameters).
   * 
   * @param msg the message where the header must be read
   * @param header the name of the header to read
   * @return the header's value, null if not found
   */
  public static final String getHeaderValue(String hdr) {
    if (hdr == null) {
      return null;
    }
    int i = hdr.indexOf(';');
    if (i > 0) {
      hdr = hdr.substring(0, i);
    }
    return hdr.trim();
  }
  
  /**
   * Retrieve a header parameter.
   */
  public static String getHeaderParam(String headerValue, String paramName) {
    if (headerValue == null || headerValue.indexOf(';') < 0) {
      return (null);
    }
    
    QuotedStringTokenizer tok = new QuotedStringTokenizer(headerValue, ";");
    
    for (int i = 0; tok.hasMoreTokens(); i++) {
      if (i == 0) {
        tok.nextToken();
        continue;
      }
      
      String param = tok.nextToken().trim();
      if (headerParamMatches(paramName, param)) {
        return (param);
      }
    }
    
    return (null);
  }
  
  /**
   * Gets a header parameter value.
   * 
   * @param headerValue The header value.
   * @param paramName The parameter name.
   * @return a header parameter value.
   */
  public static String getHeaderParamValue(String headerValue, String paramName) {
    String param = getHeaderParam(headerValue, paramName);
    
    if (param == null)
      return null;
    
    int equal = param.indexOf('=');
    
    if (equal < 0)
      return "";
    
    return (param.substring(equal + 1).trim());
  }
  
  /**
   * Sets a header parameter value.
   * 
   * @param headerValue The header value.
   * @param paramName The parameter name.
   * @param paramValue The parameter value.
   * @return The new hader value.
   */
  public static String setHeaderParamValue(String headerValue, String paramName, String paramValue) {
    StringBuilder buf;
    
    if (headerValue.indexOf(';') < 0) {
      buf = new StringBuilder(headerValue);
      buf.append(';');
      buf.append(paramName);
      buf.append('=');
      buf.append(paramValue);
      return (buf.toString());
    }
    
    QuotedStringTokenizer tok = new QuotedStringTokenizer(headerValue, ";");
    buf = new StringBuilder();
    
    boolean found = false;
    
    for (int i = 0; tok.hasMoreTokens(); i++) {
      if (i == 0) {
        // this is the header value (without parameters)
        buf.append(tok.nextToken().trim());
        continue;
      }
      
      String param = tok.nextToken().trim();
      
      if (headerParamMatches(paramName, param)) {
        found = true;
        buf.append(';');
        buf.append(paramName);
        buf.append('=');
        buf.append(paramValue);
      } else {
        buf.append(';');
        buf.append(param);
      }
    }
    
    if (!found) {
      buf.append(';');
      buf.append(paramName);
      buf.append('=');
      buf.append(paramValue);
    }
    
    return (buf.toString());
  }
  
  /**
   * Gets a parameter value.
   * @param s
   * @param paramName The parameter name.
   * @param sep The separator
   * @param removeQuotes true if the string must be unquoted.
   * @return The parameter value.
   */
  public static String getParam(String s, String paramName, String sep, boolean removeQuotes) {
    QuotedStringTokenizer tok = new QuotedStringTokenizer(s, sep, removeQuotes);
    
    for (int i = 0; tok.hasMoreTokens(); i++) {
      String param = tok.nextToken().trim();
      if (headerParamMatches(paramName, param)) {
        return (param);
      }
    }
    
    return (null);
  }
  
  /**
   * Extracts the parameters from a header value.
   * 
   * @param headerValue The header value.
   * @return The table of the parameters.
   */
  public static Hashtable extractHeaderParams(String headerValue) {
    Hashtable map = new Hashtable();
    return extractHeaderParams(headerValue, map);
  }
  
  /**
   * Extracts the parameters from a header value.
   * 
   * @param headerValue The header value.
   * @param defaultResult The default result.
   * @return The table of the parameters.
   */
  public static Hashtable extractHeaderParams(String headerValue, Hashtable defaultResult) {
    int semi = headerValue.indexOf(';');
    if (semi < 0) {
      return defaultResult;
    }
    String s = headerValue.substring(semi + 1).trim();
    return (extractParams(s, ";", true, defaultResult));
  }
  
  /**
   * Extracts the authorization parameters.
   * @param auth The authenticate header value.
   * @return The table of the parameters.
   */
  public static Hashtable extractAuthParams(String auth) {
    Hashtable map = new Hashtable();
    return extractAuthParams(auth, map);
  }
  
  /**
   * Extracts the authorization parameters.
   * @param auth The authenticate header value.
   * @return The table of the parameters.
   */
  public static Properties extractAuthParamsAsProperties(String auth) {
    Hashtable map = extractAuthParams(auth);
    
    Properties res = new Properties();
    for (Enumeration e = map.keys(); e.hasMoreElements();) {
      String key = (String) e.nextElement();
      res.setProperty(key, (String) map.get(key));
    }
    return res;
  }
  
  /**
   * Extracts the authorization parameters.
   * @param auth The authenticate header value.
   * @param map  The default table of the parameters.
   * @return The table of the parameters.
   */
  public static Hashtable extractAuthParams(String auth, Hashtable map) {
    int space = auth.indexOf(' ');
    if (space < 0) {
      return map;
    }
    String scheme = auth.substring(0, space).trim();
    map.put("scheme", scheme);
    String s = auth.substring(space + 1).trim();
    return (extractParams(s, ",", true, map));
  }
  
  /**
   * Gets the HTTP reason.
   * 
   * @param code The status of the response.
   * @return The reason.
   */
  public static String getHttpReason(int code) {
    String r = (String) REASONS.get(code);
    if (r == null) {
      return (String.valueOf(code));
    }
    
    return r;
  }
  
  /**
   * 
   * @param s
   * @param sep
   * @param removeQuotes
   * @param map
   * @return
   */
  private static Hashtable extractParams(String s, String sep, boolean removeQuotes, Hashtable map) {
    QuotedStringTokenizer tok = new QuotedStringTokenizer(s, sep, removeQuotes);
    for (int i = 0; tok.hasMoreTokens(); i++) {
      String param = tok.nextToken().trim();
      int equal = param.indexOf('=');
      
      if (equal >= 0) {
        map.put(param.substring(0, equal).trim().toLowerCase(), param.substring(equal + 1).trim());
      } else {
        map.put(param.trim().toLowerCase(), "");
      }
    }
    
    return (map);
  }
  
  /**
   * 
   * @param headerParam
   * @param otherHeaderParam
   * @return
   */
  private static boolean headerParamMatches(String headerParam, String otherHeaderParam) {
    otherHeaderParam = otherHeaderParam.trim();
    int equal = otherHeaderParam.indexOf('=');
    if (equal < 0) {
      if (otherHeaderParam.equalsIgnoreCase(headerParam)) {
        return (true);
      }
    } else if (otherHeaderParam.substring(0, equal).trim().equalsIgnoreCase(headerParam)) {
      return (true);
    }
    
    return (false);
  }
}
