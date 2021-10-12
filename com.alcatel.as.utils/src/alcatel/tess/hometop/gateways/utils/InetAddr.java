// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 * Helper class useful when parsing IPv4/IPv6/host addresses.
 * Unlike {@link java.net.InetAddress}, this class never performs any DNS resolution.
 */
public class InetAddr extends IPAddr {
  protected String _strAddr;
  
  /**
   * Makes a new Address, given its string representation.
   * The String can be either:<ul>
   * <li> a v4 ip address
   * <li> a v6 ip address, optionally enclosed between brackets (useful when parsing URIs)
   * <li> a host/domain address.
   * </ul>
   *
   * If the addr is a host/domain, then the {@link #toByteArray} method will return null.
   * @param addr a v4/v6 address, or a regular host/domain address.
   * @throws IllegalArgumentException if the provided address is not well formed.
   */
  public InetAddr(String addr) {
    this(addr, false);
  }
  
  public InetAddr(String addr, boolean sip) {
    _strAddr = addr;
    _binAddr = parseIPAddress(addr);
    if (_binAddr == null) {
      // Probably a domain name.
      if (addr == null || addr.length() == 0 ||
      // According to rfc952 and rfc1101 (3.1), domain names must starts with alpha.
          (!sip && !alpha.belongs(addr.charAt(0))) ||
          // According to rfc3261 only LAST domain name label must start with alpha.
          (sip && !alpha.belongs(addr.charAt(addr.lastIndexOf('.', addr.length() - 2) + 1)))) {
        throw new IllegalArgumentException("Invalid Address host name: " + addr);
      }
    } else if (isIPv6()) {
      // if addr is an IPv6 address, strip enclosing brackets from addr's string representation.
      _strAddr = stripIpv6Brackets(_strAddr);
    }
  }
  
  /**
   * Makes a new Inet Addres, given its binary representation. The binary representation of an
   * internet address may be retrieved using the {@link #toByteArray()} method.
   * @param addr a v4/v6 binary address.
   * @throws IllegalArgumentException if the provided binary address is not well formed.
   */
  public InetAddr(byte[] addr) {
    _binAddr = addr;
    _strAddr = toString(addr);
  }
  
  /**
   * Return the string representation for that internet address.
   * If this instance has been built from the binary representation of a given ip address,
   * then this method will reconstruct the string representation for the original binary address.
   * @return the string representation for that internet address.
   */
  public String toString() {
    return _strAddr;
  }
  
  /**
   * Write the string representation for that address into a given Writer stream.
   * @param out a Writer stream where we'll write the string representation for that address.
   * @throws IOException when writing to the given writer output stream.
   */
  public void writeTo(Writer out) throws IOException {
    out.write(_strAddr);
  }
  
  /**
   * Write the binary representation for that address into a given output stream.
   * If this inet addr is a host name (not an ip addr), then we write the host name, as a string; otherwise,
   * we write the binary representation for that ip address.
   * @param out an output stream where we'll write the binary representation for that address.
   * @throws IOException when writing to the given output stream.
   */
  public void writeTo(OutputStream out) throws IOException {
    if (_binAddr == null) {
      for (int i = 0; i < _strAddr.length(); i++) {
        out.write((int) _strAddr.charAt(i) & 0xFF);
      }
    } else {
      out.write(_binAddr);
    }
  }
  
  /**
   * Return the binary representation for that internet address.
   * @return the binary representation for that address, or null if the addres is actually a host/domain address.
   */
  public byte[] toByteArray() { // null if this address is a domain/host name.
    return _binAddr;
  }
  
  /**
   * Tests whether or not this address matches another given address.
   * There are three cases:<ul>
   * <li> If both address are ip addresses, then we'll compare
   *      the binary representation of both addresses. 
   * <li> If both addresses are host/domain, then we'll compare the string representation of both addresses, 
   *	  in a case insensitive manner.
   * <li> If this address is a host/domain address, and the other address is an ip address, then this method
   *	  won't match, because we don't perform any DNS resolution.
   * </ul>
   * @param addr either another InetAddr object.
   * @return true if this address equals the given address, false if not.
   */
  public boolean equals(Object addr) {
    InetAddr other = null;
    if (addr instanceof InetAddr) {
      other = (InetAddr) addr;
    } else {
      return false;
    }
    if (_binAddr == null) {
      return (_strAddr.equalsIgnoreCase(other.toString()));
    }
    return super.equals(addr);
  }
  
  /**
   * Return the hash code for that address. 
   * @return the hash code that that address. If this address is a host/domain address, we'll return the
   * hash code of the host (lowercase). Else, we'll return a hash code of the binary representation.
   */
  public int hashCode() {
    if (_binAddr == null) {
      return _strAddr.toLowerCase().hashCode();
    }
    return super.hashCode();
  }
  
  /**
   * Tests whether or not this internet address is an IP address (v4 or v6).
   * @return true if this address is a v4/v6 address, false if this address is a host/domain address.
   */
  public boolean isHostName() {
    return _binAddr == null;
  }
  
  /**
   * Tests whether or not this internet address is an IP address (v4 or v6).
   * @return true if this address is a v4/v6 address, false if this address is a host/domain address.
   */
  public boolean isIPAddress() {
    return isIPv4() || isIPv6();
  }
  
  // -------------------------- Private attributes ---------------------------------------------------
  
  // Bitmap used to parse addresses.
  private final static CharacterSet alpha = new CharacterSet();
  
  // Static initializer
  static {
    alpha.addChars('a', 'z');
    alpha.addChars('A', 'Z');
  }
}
