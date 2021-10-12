// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent;

import java.util.concurrent.atomic.AtomicInteger;

public class SessionPolicy {
  
  static String COOKIE_SESSID = AgentProperties.COOKIE_JSESSIOND;
  static String PARAM_SESSID = COOKIE_SESSID.toLowerCase();
  
  public static enum Policy {
    NONE,
    CLIENT_IP,
    HTTP_HEADER,
    COOKIE,
  }
  
  private Policy policy;
  private String name;
  private AtomicInteger uniqueCounter = new AtomicInteger();

  static SessionPolicy getPolicy(String config, String cookieName, String headerName) {
    Policy policy = Policy.NONE;
    String name = "";
    if (config.compareToIgnoreCase(AgentProperties.POLICY_NONE) != 0) {
      if (config.compareToIgnoreCase(AgentProperties.POLICY_CLIENT_IP) == 0)
        policy = Policy.CLIENT_IP;
      else {
        String[] items = config.split(" ");
        if (items[0].trim().compareToIgnoreCase(AgentProperties.POLICY_HEADER) == 0)
          policy = Policy.HTTP_HEADER;
        else if (items[0].trim().compareToIgnoreCase(AgentProperties.POLICY_COOKIE) == 0) {
          policy = Policy.COOKIE;
        }
        switch (policy) {
        case HTTP_HEADER:
          name = headerName;
          break;

        case COOKIE:
          name = cookieName;
          COOKIE_SESSID = name;
          PARAM_SESSID = name.toLowerCase();
          break;

        default:
          break;
        }
      }
    }
    return new SessionPolicy(policy, name);
  }

  public SessionPolicy(Policy policy, String name) {
    this.policy = policy;
    this.name = name;
  }

  public Policy getPolicy() {
    return policy;
  }

  public String getName() {
    return name;
  }

  public int getUID() {
    int uid = 0;
    while(uid == 0) {
      uid = uniqueCounter.incrementAndGet() & 0x7FFFFFFF;
    }
    return uid;
  }
  
  public long getTmpId() {
    return 0xDDDDDDDD00000000L | getUID();
  }
  
  public long hash64(String key) {
    byte[] value = key.getBytes();
    return hash64(value, 0, value.length);
  }
  
  @Override
  public String toString() {
    return "SessionPolicy [policy=" + policy + ", name=" + name + "]";
  }

  // HASHING
  // http://gxp.googlecode.com/svn-history/r150/trunk/java/src/com/google/transconsole/common/messages/Hash.java
  // http://burtleburtle.net/bob/hash/evahash.html#hash64

  /** Default hash seed (64 bit) */
  private static final long SEED64 = 0x2b992ddfa23249d6L; // more of pi
  /** Hash constant (64 bit) */
  private static final long CONSTANT64 = 0xe08c1d668b756f82L; // more of golden

  /**
   * Hash byte array to a 64 bit value using the supplied seed.
   * @param value the bytes to hash
   * @param offset the starting position of value where bytes are used for the
   *        hash computation
   * @param length number of bytes of value that are used for the hash
   *        computation
   * @return 64 bit hash value
   */
  private static long hash64(byte[] value, int offset, int length) {
    long a = CONSTANT64;
    long b = a;
    long c = SEED64;
    int keylen;

    for (keylen = length; keylen >= 24; keylen -= 24, offset += 24) {
      a += word64At(value, offset);
      b += word64At(value, offset + 8);
      c += word64At(value, offset + 16);

      // mix64(a,b,c)
      a -= b; a -= c; a ^= c >> 43;
      b -= c; b -= a; b ^= a <<  9;
      c -= a; c -= b; c ^= b >>  8;
      a -= b; a -= c; a ^= c >> 38;
      b -= c; b -= a; b ^= a << 23;
      c -= a; c -= b; c ^= b >>  5;
      a -= b; a -= c; a ^= c >> 35;
      b -= c; b -= a; b ^= a << 49;
      c -= a; c -= b; c ^= b >> 11;
      a -= b; a -= c; a ^= c >> 12;
      b -= c; b -= a; b ^= a << 18;
      c -= a; c -= b; c ^= b >> 22;

    }
    
    // if (false) System.out.println("J_step-1: a=" + a + ",b=" + b + ",c=" + c);
    
    c += length;

    // if (false) System.out.println("J_step-2: c=" + c + ",len=" + keylen + ",offest=" + offset);

    switch (keylen) {  // deal with rest.  Cases fall through
      case 23: c += ((long)value[offset + 22]) << 56;
      case 22: c += (value[offset + 21] & 0xffL) << 48;
      case 21: c += (value[offset + 20] & 0xffL) << 40;
      case 20: c += (value[offset + 19] & 0xffL) << 32;
      case 19: c += (value[offset + 18] & 0xffL) << 24;
      case 18: c += (value[offset + 17] & 0xffL) << 16;
      case 17: c += (value[offset + 16] & 0xffL) << 8;
        // the first byte of c is reserved for the length
      case 16:
        b += word64At(value, offset + 8);
        a += word64At(value, offset);
        break;
      case 15: b += (value[offset + 14] & 0xffL) << 48;
      case 14: b += (value[offset + 13] & 0xffL) << 40;
      case 13: b += (value[offset + 12] & 0xffL) << 32;
      case 12: b += (value[offset + 11] & 0xffL) << 24;
      case 11: b += (value[offset + 10] & 0xffL) << 16;
      case 10: b += (value[offset +  9] & 0xffL) << 8;
      case  9: b += (value[offset +  8] & 0xffL);
      case  8:
        a += word64At(value, offset);
        break;
      case  7: a += (value[offset +  6] & 0xffL) << 48;
      case  6: a += (value[offset +  5] & 0xffL) << 40;
      case  5: a += (value[offset +  4] & 0xffL) << 32;
      case  4: a += (value[offset +  3] & 0xffL) << 24;
      case  3: a += (value[offset +  2] & 0xffL) << 16;
      case  2: a += (value[offset +  1] & 0xffL) << 8;
      case  1: a += (value[offset +  0] & 0xffL);
      // case 0: nothing left to add
        break;
      default:
        break;
    }

    // if (false) System.out.println("J_step-3: a=" + a + ",b=" + b + ",c=" + c);

    return mix64(a, b, c)  & Long.MAX_VALUE;
  }

  private static long word64At(byte[] bytes, int offset) {
    return (bytes[offset + 0] & 0xffL) +
          ((bytes[offset + 1] & 0xffL) <<  8) +
          ((bytes[offset + 2] & 0xffL) << 16) +
          ((bytes[offset + 3] & 0xffL) << 24) +
          ((bytes[offset + 4] & 0xffL) << 32) +
          ((bytes[offset + 5] & 0xffL) << 40) +
          ((bytes[offset + 6] & 0xffL) << 48) +
          ((bytes[offset + 7] & 0xffL) << 56);
  }

  /**
   * Mixes longs a, b, and c, and returns the final value of c.
   */
  private static long mix64(long a, long b, long c) {
    a -= b; a -= c; a ^= c >> 43;
    b -= c; b -= a; b ^= a <<  9;
    c -= a; c -= b; c ^= b >>  8;
    a -= b; a -= c; a ^= c >> 38;
    b -= c; b -= a; b ^= a << 23;
    c -= a; c -= b; c ^= b >>  5;
    a -= b; a -= c; a ^= c >> 35;
    b -= c; b -= a; b ^= a << 49;
    c -= a; c -= b; c ^= b >> 11;
    a -= b; a -= c; a ^= c >> 12;
    b -= c; b -= a; b ^= a << 18;
    c -= a; c -= b; c ^= b >> 22;
    return c;
  }

}
