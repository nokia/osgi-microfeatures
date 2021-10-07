package com.nextenso.util;

import java.io.UnsupportedEncodingException;

/**
 * <UL>
 * <HR NOSHADE SIZE=2>
 *
 * <H3>Purpose, description</H3>
 * This class contains some utilities for manipulating Strings.
 * <P>
 * <H3>Category</H3>
 * <P>
 * <B>Toolkit :</B> Class is everywhere publicly available
 * <P>
 * <P>
 * <HR NOSHADE SIZE=2>
 *
 * <H3>Version Info</H3>
 * <TABLE BORDER=0 CELLPADDING="1" CELLSPACING="0">
 * <TR><TD><B>Revision</B>
 *     <TD><B>$Revision: 1713 $</B>
 * <TR><TD>Last Modification:
 *     <TD>$Date: 2013-03-11 17:52:26 +0100 (Mon, 11 Mar 2013) $
 * <TR><TD>ID:
 *     <TD>$Id$
 * <TR><TD>Name:
 *     <TD>$Name$
 * <TR><TD>Author:
 * <TR><TD><SMALL>Log:</SMALL>
 *     <TD><SMALL>
 * <BR> $Log$
 * <BR> Reactor
 * <BR>
 * <BR> Revision 1.1  2004/04/21 12:29:28  thomas.froment
 * <BR> move StringEncoder from GatewaysUtils to Utils
 * <BR>
 * <BR> Revision 1.1  2003/06/25 12:48:34  thomas.froment
 * <BR> add utility classes for string encoding
 * <BR>
 * <BR> Revision 1.2  2002/01/16 09:12:18  arjun.panday
 * <BR> import StandaloneJef from FR2402
 * <BR>
 * <BR> Revision 1.1.4.1  2001/11/28 17:21:20  alfonso.palacios-sureda
 * <BR> Merge new module from DEVSMSLOC
 * <BR>
 * <BR> Revision 1.1.2.1  2001/11/20 09:33:32  yann.desnoues
 * <BR> First real standalone Version (for SMS GW only).
 * <BR>
 * <BR> Revision 1.1  2001/10/12 14:55:27  yann.desnoues
 * <BR> First version for standalone Jef. Those classes are are dependencies from Jef.
 * <BR>
 * <BR> Revision 1.8  2001/05/04 07:39:21  eric.auge
 * <BR> Add trim() method so that the Push is independent of the UMS !
 * <BR>
 * <BR> Revision 1.7  2000/12/13 15:22:06  ea
 * <BR> Much more better comments ;-)
 * <BR>
 * <BR> Revision 1.6  2000/12/01 10:03:07  sc
 * <BR> much better comment
 * <BR>
 * <BR> Revision 1.5  2000/12/01 09:57:34  sc
 * <BR> added a comment
 * <BR>
 * <BR> Revision 1.4  2000/12/01 09:22:17  sc
 * <BR> added a url encoder
 * <BR>
 * <BR> Revision 1.3  2000/10/25 13:32:32  ea
 * <BR> Define convert method in public static
 * <BR>
 * <BR> Revision 1.2  2000/10/25 13:31:50  ea
 * <BR> Add convert method to manage charset encoding
 * <BR>
 * <BR> Revision 1.1  1999/05/12 08:38:13  sr
 * <BR> First commit : Utility for String manipulation/display.
 * <BR> Currently this class only contains one method.
 * <BR>
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
public class StringUtil {
  /**
   * Returns a simple string representation of an array of String.
   * @return "null", or "<empty>", or [val1, val2, ...]
   */
  public static String arrayToString(String[] array) {
    if (array == null)
      return ("null");
    if (array.length == 0)
      return ("<empty>");
    StringBuffer sb = new StringBuffer();
    sb.append("[");
    sb.append(array[0]);
    for (int i = 1; i < array.length; i++) {
      sb.append(",");
      sb.append(array[i]);
    }
    sb.append("]");
    return (sb.toString());
  }
  
  /**
   * Converts a raw String in the specified encoding charset.
   * @param rawString a String in raw format (not in Unicode)
   * @param encoding for instance ISO-8859-1, GB2312, UTF-8, ...
   * @exception UnsupportedEncodingException bad encoding parameter
   */
  public static String convert(String rawString, String encoding) throws UnsupportedEncodingException {
    
    char[] orig = rawString.toCharArray();
    byte[] dest = new byte[orig.length];
    for (int j = 0; j < orig.length; j++) {
      dest[j] = (byte) (orig[j] & 0xFF);
    }
    return new String(dest, encoding);
  }
  
  /**
   * Encodes a string in UTF8 for URL parameters.
   * Each such character is converted to UTF-8 as one or more bytes, each of
     * these bytes is escaped with the URI escaping mechanism
   * (i.e. converted to %HH, where HH is the hexadecimal notation of the byte value),
   * and the original character is replaced by the resulting character sequence. 
   * <a href="http://www.w3.org/TR/charmod/#URIs">W3C Character Model</a>
   * @param s the string to encode 
   * @internal
   */
  public static String encodeURL(String s) {
    if (s == null)
      return "";
    int length = s.length();
    StringBuffer ret = new StringBuffer(length * 2);
    
    try {
      byte[] utf8Bytes = s.getBytes("UTF8");
      
      for (int k = 0; k < utf8Bytes.length; k++) {
        String hex = byteToHex(utf8Bytes[k]);
        ret.append("%" + (hex.length() == 1 ? "0" + hex : hex));
      }
      return ret.toString();
    } catch (UnsupportedEncodingException e) {
      return "";
    }
  }
  
  /**
   * convert a byte to an hex string
   * @param b the byte to convert 
   * @internal
   */
  static public String byteToHex(byte b) {
    // Returns hex String representation of byte b
    char hexDigit[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    char[] array = { hexDigit[(b >> 4) & 0x0f], hexDigit[b & 0x0f] };
    return new String(array);
  }
  
  /**
   * Make sure the specified string does not exceed a maximum size.
   * For JSP purpose.
   * @internal
   */
  public static String trim(String addr, int size) {
    if (addr.length() > size) {
      addr = addr.substring(0, size - 3) + "...";
    }
    return addr;
  }
}
