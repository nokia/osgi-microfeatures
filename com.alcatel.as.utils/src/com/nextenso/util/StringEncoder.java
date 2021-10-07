package com.nextenso.util;

import java.util.StringTokenizer;

/**
 * <UL>
 * <HR NOSHADE SIZE=2>
 *
 * <H3>Purpose, description</H3>
 * This class contains some tools to encode String.
 * <P>
 * <H3>Category</H3>
 * As category use one of the following:
 * <P>
 * <B>Toolkit :</B> Class is everywhere publicly available
 * <P>
 * <P>
 * <HR NOSHADE SIZE=2>
 *
 * <H3>Version Info</H3>
 * <TABLE BORDER=0 CELLPADDING="1" CELLSPACING="0">
 * <TR><TD><B>Revision</B>
 * <BR>
 *  </SMALL>
 *
 *
 * <TR BGCOLOR="#707070"><TD ALIGN="center" COLSPAN="2">
 *
 * <SMALL><FONT COLOR="#ff7700">
 *  This Package is Part of the Alcatel <B>HomeTop Solution</B>
 *  &copy; 1999 Alcatel Telecom / ATeSS (<A HREF="http://www.alcatel.com">http://www.alcatel.com</A>)
 * </FONT></SMALL>
 *
 * </TD>
 * </TABLE>
 *
 * <HR NOSHADE SIZE=2>
 * </UL>
 */
public class StringEncoder {
  /**
   * Encodes the String to WML format.
   * The ASCII characters 'a' through 'z', 'A' through 'Z', 
   * '0' through '9', and space ' ' remain the same. 
   * The ASCII characters  like &,',<,>," are escaped to the WML named character entities &amp;,&quot,&lt;...
   * All other characters are converted into the 
   * string "&#xx;"..
   * @return The encoded string, or null if original string is null.
   */
  public static String encodeToHTML(String s) {
    if (s == null)
      return null;
    int length = s.length();
    StringBuffer ret = new StringBuffer(length * 2);
    for (int i = 0; i < length; i++) {
      char c = s.charAt(i);
      if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == ' ')
        ret.append(c);
      else if (c == '&')
        ret.append("&amp;");
      else if (c == 34)
        ret.append("&quot;");
      else if (c == 39)
        ret.append("&acute;");
      else if (c == '<')
        ret.append("&lt;");
      else if (c == '>')
        ret.append("&gt;");
      else if (c == 160)
        ret.append("nbsp;");
      else if (c == 173)
        ret.append("&shy;");
      else if (c == '$')
        ret.append("$$");
      else {
        ret.append("&#");
        ret.append(Integer.toString(c));
        ret.append(";");
      }
    }
    return ret.toString();
  }
  
  /**
   * Encodes the String to HTML format.
   * The ASCII characters 'a' through 'z', 'A' through 'Z', 
   * '0' through '9', and space ' ' remain the same. 
   * All other characters are converted into the 
   * string "&#xx;"..
   * @return The encoded string, or null if original string is null.
   */
  public static String encodeToWML(String s) {
    if (s == null)
      return null;
    int length = s.length();
    StringBuffer ret = new StringBuffer(length * 2);
    for (int i = 0; i < length; i++) {
      char c = s.charAt(i);
      if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == ' ')
        ret.append(c);
      else {
        ret.append("&#");
        ret.append(Integer.toString(c));
        ret.append(";");
      }
    }
    return ret.toString();
  }
  
  /**
   * decodes the html to a string
   * @return The decoded string, or null if original string is null.
   */
  public static String HTMLDecode(String s) {
    if (s == null)
      return null;
    int length = s.length();
    StringBuffer ret = new StringBuffer(length);
    StringTokenizer tok = new StringTokenizer(s, "&");
    boolean first = true;
    while (tok.hasMoreTokens()) {
      if (first && (s.charAt(0) != '&')) {
        ret.append(tok.nextToken());
        first = false;
      } else {
        String c = ISO8859.HTMLtoISO("&" + tok.nextToken());
        ret.append(c);
      }
    }
    return ret.toString();
  }
  
  /**
   * decodes the a string containing html entity to a string containing ISO char.
   * @return The decoded string, or null if original string is null.
   */
  public static String decodeHTML(String s) {
    if (s == null)
      return null;
    int length = s.length();
    StringBuffer ret = new StringBuffer(length);
    StringTokenizer tok = new StringTokenizer(s, "&");
    boolean first = true;
    while (tok.hasMoreTokens()) {
      String token = tok.nextToken();
      int index = token.indexOf(';'); //first character ';'
      if (index <= 0) {
        if (first) {
          ret.append(token);
          first = false;
        } else {
          ret.append("&" + token);
        }
      } else {
        String htmlEntity = "&" + token.substring(0, index) + ";";
        String c = ISO8859.HTMLtoISO(htmlEntity);
        if (c.equals("&") && first) {
          ret.append(token.substring(index + 1));
          first = false;
        } else
          ret.append(c + token.substring(index + 1));
      }
    }
    return ret.toString();
  }
  
  /**
   * Encodes the String to URL format.
   * Uses  java.net.URLEncoder.encode (String s)
   * @return The encoded string, or null if original string is null.
   * @see java.net.URLEncoder.encode
   */
  public static String encodeToURL(String s) {
    if (s == null)
      return null;
    String enc = java.nio.charset.Charset.defaultCharset().name();
    try {
      return java.net.URLEncoder.encode(s, enc);
    } catch (java.io.UnsupportedEncodingException e) {
      throw new RuntimeException("Could not url encode " + s + " with default jvm charset: " + enc, e);
    }
  }
  
  /**
   * Transforms s into the HTML(javascript) propery format.
   * Currently, this only converts:
   * <UL>
   *  <LI>newlines to <TT>\n<TT>
   *  <LI>newlines to <TT>\r<TT>
   *  <LI>' to <TT>\'<TT>
   *  <LI>" to <TT>\'<TT>
   * </UL>
   * @return The encoded string, or null if original string is null.
   */
  public static String encodeToProperty(String s) {
    if (s == null)
      return "";
    int length = s.length();
    StringBuffer ret = new StringBuffer((int) (length * 1.2));
    
    for (int i = 0; i < length; i++) {
      char c = s.charAt(i);
      if (c == '\n') {
        ret.append("\\n");
      } else if (c == '\r') {
        ret.append("\\r");
      } else if (c == '\'') {
        ret.append("\\\'");
      } else if (c == '\"') {
        ret.append("\\\"");
      } else {
        ret.append(c);
      }
    }
    return ret.toString();
  } /* end encodeToProperty */
  
  public static String encodeToPropertyFile(String s) {
    if (s == null)
      return "";
    int length = s.length();
    StringBuffer ret = new StringBuffer((int) (length * 1.2));
    
    for (int i = 0; i < length; i++) {
      char c = s.charAt(i);
      if (c == '\n') {
        ret.append("\\n");
      } else if (c == '\r') {
        ret.append("\\r");
      } else {
        ret.append(c);
      }
    }
    return ret.toString();
  } /* end encodeToPropertyFile */
  
  public static void main(String[] args) {
    String test = HTMLDecode(args[0]);
    System.out.println(test);
    
  }
  
}
