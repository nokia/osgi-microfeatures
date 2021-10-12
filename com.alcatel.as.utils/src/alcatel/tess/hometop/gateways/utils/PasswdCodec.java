// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

import java.util.Random;

/**
 * simple codec for storing passwords to config files.
 * rotate string by random offset (1 digit)
 * prepend offset to string
 * base64 encode
 */
public class PasswdCodec {
  
  public static String encode(String input) {
    if (input.length() < 2)
      throw new IllegalArgumentException("too short!");
    int offset = new Random().nextInt(Math.min(4, input.length() - 1)) + 1; //1 digit, avoid 0 and input.length
    // rotate and shift input
    byte[] bytes = input.getBytes();
    byte[] bb = new byte[bytes.length + 1];
    bb[0] = (byte) offset;
    for (int i = 0; i < bytes.length; i++) {
      bb[i + 1] = (byte) (bytes[(i + offset) % bytes.length] + offset);
    }
    // return base64
    //System.out.println("encoding "+new String(bb));
    return Coder.uuencode(bb);
  }
  
  public static String decode(String input) {
    // decode bas64
    byte[] bb = Coder.uudecodeInBytes(input);
    int offset = (int) bb[0];
    // rotate backwards
    byte[] bytes = new byte[bb.length - 1];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) (bb[1 + (i + (bytes.length - offset)) % bytes.length] - offset);
    }
    //System.out.println("decoding "+new String(bb));
    // rebuild original string
    return new String(bytes);
  }
  
  public static void main(String ... args) throws Exception {
    if (args.length < 2) {
      usage();
      System.exit(0);
    }
    if ("-e".equals(args[0])) {
      System.out.println(encode(args[1]));
    }
    /*else if ("-d".equals(args[0])) {
      System.out.println(decode(args[1]));
    }*/
    else
      usage();
  }
  
  public static void usage() {
    System.out.println("PasswdCodec -e input");
  }
}
