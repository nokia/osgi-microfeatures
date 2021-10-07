package com.nextenso.http.agent.parser;

import java.util.Date;
import java.util.StringTokenizer;

import alcatel.tess.hometop.gateways.utils.RfcDateParser;

import com.nextenso.proxylet.http.HttpCookie;
import com.nextenso.proxylet.http.HttpURL;

/**
 * Class declaration
 *
 *
 * @author
 */
public class CookieParser {
  
  /**
   * Sets the path and domain to a Cookie given a url
   *
   * @param url
   */
  public static void applyDefaults(HttpCookie cookie, HttpURL url) {
    if (url == null)
      // might happen when filtering responses only in the http agent
      return;
    
    if (cookie.getDomain() == null) {
      cookie.setDomain(url.getHost());
    }
    
    if (cookie.getPath() == null) {
      String path = url.getPath();
      int i = path.lastIndexOf('/');
      cookie.setPath(path.substring(0, i + 1));
    }
  }
  
  /**
   * Parses a cookie String (version 0 - Netscape)
   *
   * @param s
   */
  public static HttpCookie parse(String s) {
    try {
      StringTokenizer stringtokenizer = new StringTokenizer(s, ";");
      
      String nameAndValue = stringtokenizer.nextToken().trim();
      int eq = nameAndValue.indexOf('=');
      String name = nameAndValue.substring(0, eq).trim();
      String value = nameAndValue.substring(eq + 1).trim();
      HttpCookie cookie = new HttpCookie(name, value);
      cookie.setVersion(0);
      
      while (stringtokenizer.hasMoreTokens()) {
        String s1 = stringtokenizer.nextToken().trim();
        
        if (s1.equalsIgnoreCase("secure")) {
          cookie.setSecure(true);
        } else {
          int i = s1.indexOf('=');
          
          if (i == -1)
            continue;
          
          String s2 = s1.substring(0, i);
          String s3 = s1.substring(i + 1);
          
          if (s2.equalsIgnoreCase("path")) {
            cookie.setPath(s3);
          } else if (s2.equalsIgnoreCase("domain")) {
            cookie.setDomain(s3);
          } else if (s2.equalsIgnoreCase("expires")) {
            cookie.setExpirationDate(parseExpireDate(s3));
          } else if (s2.equalsIgnoreCase("max-age")) {
            cookie.setMaxAge(Integer.parseInt(s3));
          }
        }
      }
      return cookie;
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid Cookie");
    }
  }
  
  public static HttpCookie parse(String s, HttpURL url) {
    HttpCookie cookie = parse(s);
    applyDefaults(cookie, url);
    return cookie;
  }
  
  /**
   * Returns the version 0 (Netscape) string value of a cookie.
   * @return
   */
  public static String toString(HttpCookie cookie) {
    return (toString(cookie, 0));
  }
  
  /**
   * The <code>toString</code> method returns a string representing the
   * HttpCookie object.
   *
   * @param cookie a <code>HttpCookie</code> value
   * @param cookieVersion an <code>int</code> value equals to the desired cookie version,
   *	or -1 if the cookie must be parsed using its own version.
   *	version
   * @return a <code>String</code> value
   */
  public static String toString(HttpCookie cookie, int version) {
    StringBuffer buff = new StringBuffer(cookie.getName()).append('=').append(cookie.getValue());
    
    switch (version) {
    case 0:
      if (cookie.getExpirationDate() != Long.MAX_VALUE) {
        buff.append("; Expires=" + RfcDateParser.format(new Date(cookie.getExpirationDate())));
      }
      break;
    
    case 1:
      buff.append("; Version=1");
      
      if (cookie.getComment() != null)
        buff.append("; Comment=").append(cookie.getComment());
      
      if (cookie.getMaxAge() != Integer.MAX_VALUE) {
        buff.append("; Max-Age=" + cookie.getMaxAge());
      }
      
      break;
    
    default:
      throw new IllegalArgumentException("Invalid cookie version: " + version);
    }
    
    if (cookie.getPath() != null)
      buff.append("; Path=").append(cookie.getPath());
    
    if (cookie.getDomain() != null)
      buff.append("; Domain=").append(cookie.getDomain());
    
    if (cookie.getSecure())
      buff.append("; Secure");
    
    return buff.toString();
  }
  
  /**
   * Parses the expiration date.
   *
   * @param s
   * @return
   */
  private static long parseExpireDate(String s) {
    
    Date date = RfcDateParser.parse(s);
    
    if (date != null) {
      return date.getTime();
    } else {
      return Long.MAX_VALUE;
    }
  }
  
}
