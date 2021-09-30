package alcatel.tess.hometop.gateways.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.Writer;
import java.util.Arrays;

import sun.net.util.IPAddressUtil;

/**
 * Helper class useful when parsing IPv4/IPv6 addresses.
 * Unlike {@link java.net.InetAddress}, this class never performs any DNS resolution.
 */
public class IPAddr implements Serializable {
  protected byte[] _binAddr;
  
  protected IPAddr() {
  }
  
  /**
   * Makes a new Address, given its string representation.
   * The String can be either:<ul>
   * <li> a v4 ip address
   * <li> a v6 ip address, optionally enclosed between brackets (useful when parsing URIs)
   * </ul>
   *
   * @param addr a v4/v6 address.
   * @throws IllegalArgumentException if the provided address is not well formed.
   */
  public IPAddr(String addr) {
    _binAddr = parseIPAddress(addr);
    if (_binAddr == null) {
      throw new IllegalArgumentException("Invalid addr: " + addr);
    }
  }
  
  /**
   * Makes a new Inet Addres, given its binary representation. 
   * @param addr a v4/v6 binary address.
   */
  public IPAddr(byte[] addr) {
    _binAddr = addr;
  }
  
  /**
   * Return the string representation for that internet address.
   * @return the string representation for that internet address.
   */
  public String toString() {
    return toString(_binAddr);
  }
  
  /**
   * Write the string representation for that address into a given Writer stream.
   * @param out a Writer stream where we'll write the string representation for that address.
   * @throws IOException when writing to the given writer output stream.
   */
  public void writeTo(Writer out) throws IOException {
    out.write(toString(_binAddr));
  }
  
  /**
   * Write the binary representation for that address into a given output stream.
   * @param out an output stream where we'll write the binary representation for that address.
   * @throws IOException when writing to the given output stream.
   */
  public void writeTo(OutputStream out) throws IOException {
    out.write(_binAddr);
  }
  
  /**
   * Return the binary representation for that internet address.
   * @return the binary representation for that address.
   */
  public byte[] toByteArray() {
    return _binAddr;
  }
  
  /**
   * Return the binary representation for that internet address.
   * @params ip An ip address optionally enclosed between brackets (useful when parsing URIs)
   * @return the binary representation for that address.
   * @throws IllegalArgumentException if the parameter is not well formed.
   */
  public static byte[] toByteArray(String ip) {
    byte[] addr = parseIPAddress(ip);
    if (addr == null) {
      throw new IllegalArgumentException("Invalid addr: " + ip);
    }
    return addr;
  }
  
  /**
   * Tests whether or not this address matches another given address.
   * @param addr either another IPAddr object or String.
   * @return true if this address equals the given address, false if not.
   */
  public boolean equals(Object addr) {
    byte[] binAddr = null;
    if (addr instanceof IPAddr) {
      binAddr = ((IPAddr) addr).toByteArray();
    } else if (addr instanceof String) {
      binAddr = parseIPAddress((String) addr);
    } else {
      return false;
    }
    return Arrays.equals(toByteArray(), binAddr);
  }
  
  /**
   * Return the hash code for that address. 
   * @return the hash code that that address.
   */
  public int hashCode() {
    byte[] addr = (byte[]) _binAddr;
    if (addr.length == 4) {
      // IPv4
      int h = addr[3] & 0xFF;
      h |= ((addr[2] << 8) & 0xFF00);
      h |= ((addr[1] << 16) & 0xFF0000);
      h |= ((addr[0] << 24) & 0xFF000000);
      return h;
    } else {
      // IPv6
      return Arrays.hashCode(_binAddr);
    }
  }
  
  /**
   * Tests whether or not this internet address is an IPv4 address.
   * @return true if this address is a IPv4 address, false otherwise.
   */
  public boolean isIPv4() {
    return _binAddr != null && _binAddr.length == 4;
  }
  
  /**
   * Tests whether or not this internet address is an IPv6 address.
   * @return true if this address is a IPv6 address, false otherwise.
   */
  public boolean isIPv6() {
    return _binAddr != null && _binAddr.length > 4;
  }
  
  /**
   * Tests whether or not a given internet address is an IP address (v4 or v6).
   * @return true if this address is a v4/v6 address, false otherwise.
   */
  public static boolean isIPAddress(String ip) {
    return parseIPAddress(ip) != null;
  }
  
  /**
   * Returns a new Inet Address, given its string representation,
   * or null if the specified string is not an ip address.
   * @param addr a v4/v6 string address.
   */
  public static IPAddr getIPAddress(String ip) {
    byte[] binAddr = parseIPAddress(ip);
    return (binAddr == null ? null : new IPAddr(binAddr));
  }
  
  // ----------------------- Protected methods --------------------------------------------------------------
  
  protected static byte[] parseIPAddress(String ip) {
    // parse the string addr and get its corresponding binary representation.
    byte[] addr = getIPv4AsBytes(ip);
    if (addr == null) {
      if (ip.startsWith("[") || ip.indexOf(':') != -1) {
        // We are fairly certain that addr is ipv6.
        addr = IPAddressUtil.textToNumericFormatV6(stripIpv6Brackets(ip));
      }
    }
    return addr;
  }
  
  /**
   * Return the string representation for a given binary internet address.
   * @return the string representation for a given binary internet address
   * @throws IllegalArgumentException if the provided binary address could not be decoded.
   */
  protected String toString(byte[] addr) {
    try {
      StringBuilder sb;
      if (addr.length == 4) {
        sb = new StringBuilder(15);
      } else {
        sb = new StringBuilder(39);
      }
      
      if (addr.length == 4) {
        // IPv4
        writeByteTo(addr[0] & 0xff, sb);
        sb.append(".");
        writeByteTo(addr[1] & 0xff, sb);
        sb.append(".");
        writeByteTo(addr[2] & 0xff, sb);
        sb.append(".");
        writeByteTo(addr[3] & 0xff, sb);
      } else {
        // IPv6
        for (int i = 0; i < 8; i++) {
          sb.append(Integer.toHexString(((addr[i << 1] << 8) & 0xff00) | (addr[(i << 1) + 1] & 0xff)));
          if (i < 7) {
            sb.append(":");
          }
        }
      }
      return sb.toString();
    }
    
    catch (IOException e) {
      throw new IllegalArgumentException("Could not decode the binary internet address: "
          + Utils.toString(addr), e);
    }
  }
  
  /** 
   * Strip off eventual IPV6 enclosing brackets.
   */
  protected static String stripIpv6Brackets(String ip) {
    if (ip.startsWith("[") && ip.endsWith("]")) {
      return ip.substring(1, ip.length() - 1);
    }
    return ip;
  }
  
  // ----------------------- Private methods -----------------------------------------------------------------
  
  /** 
   * Computes the binary representation for a v4 address.
   * @return the binary representation for the v4 address, null if the provided ip addr is not IPv4.
   * @throws IllegalArgumentException if the provided address looks like an IPv4 address, but contains invalid 
   * digits numbers.
   */
  private static byte[] getIPv4AsBytes(String ip) {
    // IPv4address = 1*3DIGIT "." 1*3DIGIT "." 1*3DIGIT "." 1*3DIGIT
    int dot1 = -1, dot2 = -1, dot3 = -1, dots = 0;
    int length = ip.length();
    char c = 0;
    
    for (int i = 0; i < length; i++) {
      c = ip.charAt(i);
      if (!ipv4Charset.belongs(c)) {
        return null;
      }
      
      if (c == '.') {
        switch (++dots) {
        case 1:
          dot1 = i;
          break;
        case 2:
          dot2 = i;
          break;
        case 3:
          dot3 = i;
          break;
        default:
          return null;
        }
      }
    }
    
    if (dots != 3) {
      return null;
    }
    int n1 = parseInt(ip, 0, dot1);
    if (n1 < 0 || n1 > 255) {
      return null;
    }
    int n2 = parseInt(ip, dot1 + 1, dot2);
    if (n2 < 0 || n2 > 255) {
      return null;
    }
    int n3 = parseInt(ip, dot2 + 1, dot3);
    if (n3 < 0 || n3 > 255) {
      return null;
    }
    int n4 = parseInt(ip, dot3 + 1, ip.length());
    if (n4 < 0 || n4 > 255) {
      return null;
    }
    return new byte[] { (byte) n1, (byte) n2, (byte) n3, (byte) n4 };
  }
  
  /**
   * Write the byte number into a string.
   */
  private void writeByteTo(int from, StringBuilder to) throws IOException {
    boolean write0 = false;
    switch (from / 100) {
    case 0:
      break;
    case 1:
      write0 = true;
      to.append('1');
      from -= 100;
      break;
    case 2:
      write0 = true;
      to.append('2');
      from -= 200;
      break;
    }
    switch (from / 10) {
    case 0:
      if (write0)
        to.append('0');
      break;
    case 1:
      to.append('1');
      break;
    case 2:
      to.append('2');
      break;
    case 3:
      to.append('3');
      break;
    case 4:
      to.append('4');
      break;
    case 5:
      to.append('5');
      break;
    case 6:
      to.append('6');
      break;
    case 7:
      to.append('7');
      break;
    case 8:
      to.append('8');
      break;
    case 9:
      to.append('9');
      break;
    }
    switch (from % 10) {
    case 0:
      to.append('0');
      break;
    case 1:
      to.append('1');
      break;
    case 2:
      to.append('2');
      break;
    case 3:
      to.append('3');
      break;
    case 4:
      to.append('4');
      break;
    case 5:
      to.append('5');
      break;
    case 6:
      to.append('6');
      break;
    case 7:
      to.append('7');
      break;
    case 8:
      to.append('8');
      break;
    case 9:
      to.append('9');
      break;
    }
  }
  
  /**
   * Efficiently parses an integer from a substring.
   */
  private static int parseInt(String s, int start, int end) {
    final int radix = 10;
    int result = 0; // Accumulates negatively (avoid MIN_VALUE overflow).
    
    for (int i = start; i < end; i++) {
      char c = s.charAt(i);
      int digit = c - '0';
      int newResult = result * radix - digit;
      if (newResult > result)
        return -1; // overflow
      result = newResult;
    }
    // Requires one valid digit character and checks for opposite overflow.
    if ((result == Integer.MIN_VALUE))
      throw new NumberFormatException("Overflow");
    return -result;
  }
  
  // -------------------------- Private attributes ---------------------------------------------------
  
  // Bitmap used to parse addresses.
  private final static CharacterSet ipv4Charset = new CharacterSet(".0123456789");
}
