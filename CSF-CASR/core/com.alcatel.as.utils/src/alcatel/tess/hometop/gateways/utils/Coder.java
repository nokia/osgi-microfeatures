package alcatel.tess.hometop.gateways.utils;

import java.io.UnsupportedEncodingException;

/** This class encodes/decodes stuff to/from the web.
 */
public class Coder {
  
  /** dont construct this
   */
  private Coder() {
    // nah.
  }
  
  /** the base64 characters 
   */
  private static final int pr2six[] = { 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64,
      64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64,
      62, 64, 64, 64, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 64, 64, 64, 64, 64, 64, 64, 0, 1, 2, 3, 4,
      5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 64, 64, 64, 64, 64, 64,
      26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51,
      64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64,
      64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64,
      64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64,
      64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64,
      64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64,
      64, 64, 64 };
  
  /** The base64 characters.
   */
  private static final char uu_base64[] = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
      'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g',
      'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0',
      '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/' };
  
  /** uudecode(base64) the given string.
   *  useful for decoding basic Authentication 
   * @param str the String to decode.
   * @return the decoded string.
   */
  public static String uudecode(String base64string) {
    StringBuffer ret = new StringBuffer();
    
    while ((base64string.length() % 4) != 0)
      base64string += "="; // that should be safe.
    int i = 0;
    int c1 = 0, c2 = 0, c3 = 0, c4 = 0;
    while (i < base64string.length() && pr2six[base64string.charAt(i)] <= 63) {
      c1 = pr2six[base64string.charAt(i)];
      c2 = pr2six[base64string.charAt(i + 1)];
      c3 = pr2six[base64string.charAt(i + 2)];
      c4 = pr2six[base64string.charAt(i + 3)];
      ret.append((char) (c1 << 2 | c2 >> 4));
      ret.append((char) ((c2 << 4 | c3 >> 2) % 256));
      ret.append((char) ((c3 << 6 | c4) % 256));
      i += 4;
    }
    
    if (c3 > 63)
      ret.setLength(ret.length() - 2);
    else if (c4 > 63)
      ret.setLength(ret.length() - 1);
    return ret.toString();
  }
  
  /** uudecode(base64) the given string.
   *  useful for decoding basic Authentication 
   * @param str the String to decode.
   * @return the decoded string.
   */
  public static byte[] uudecodeInBytes(String base64string) {
    ByteBuffer ret = new ByteBuffer();
    
    while ((base64string.length() % 4) != 0)
      base64string += "="; // that should be safe.
    int i = 0;
    int c1 = 0, c2 = 0, c3 = 0, c4 = 0;
    while (i < base64string.length() && pr2six[base64string.charAt(i)] <= 63) {
      c1 = pr2six[base64string.charAt(i)];
      c2 = pr2six[base64string.charAt(i + 1)];
      c3 = pr2six[base64string.charAt(i + 2)];
      c4 = pr2six[base64string.charAt(i + 3)];
      
      char c5 = (char) (c1 << 2 | c2 >> 4);
      char c6 = (char) ((c2 << 4 | c3 >> 2) % 256);
      char c7 = (char) ((c3 << 6 | c4) % 256);
      
      ret.append((byte) (c5 & 0xff));
      ret.append((byte) (c6 & 0xff));
      ret.append((byte) (c7 & 0xff));
      
      i += 4;
    }
    
    if (c3 > 63)
      ret.setSize(ret.size() - 2);
    else if (c4 > 63)
      ret.setSize(ret.size() - 1);
    
    return ret.toByteArray(true);
  }
  
  /** uuencode(base64) the given String.
   *  useful for encoding basic authentication.
   * @param str the String to encode.
   * @return the encoded string.
   */
  public static String uuencode(String str) {
    StringBuffer ret = new StringBuffer();
    char ch, ch1, ch2, ch3;
    int i;
    
    for (i = 0; i + 2 < str.length(); i += 3) {
      ch1 = str.charAt(i);
      ch2 = str.charAt(i + 1);
      ch3 = str.charAt(i + 2);
      ch = uu_base64[((ch1 >> 2) & 077)];
      ret.append(ch);
      
      ch = uu_base64[(((ch1 << 4) & 060) | ((ch2 >> 4) & 017))];
      ret.append(ch);
      
      ch = uu_base64[(((ch2 << 2) & 074) | ((ch3 >> 6) & 03))];
      ret.append(ch);
      
      ch = uu_base64[(ch3 & 077)];
      ret.append(ch);
    }
    
    // are we done yet?
    if (i == str.length())
      return ret.toString();
    
    // no so handle the trailing characters.
    ch1 = str.charAt(i);
    ch2 = str.length() > i + 1 ? str.charAt(i + 1) : (char) 0;
    
    ch = uu_base64[((ch1 >> 2) & 077)];
    ret.append(ch);
    
    ch = uu_base64[(((ch1 << 4) & 060) | ((ch2 >> 4) & 017))];
    ret.append(ch);
    
    if (str.length() > i + 1) {
      ch = uu_base64[((ch2 << 2) & 074)];
      ret.append(ch);
    } else {
      ret.append('=');
    }
    ret.append('=');
    return ret.toString();
  }
  
  /** uuencode(base64) the given String.
   *  useful for encoding basic authentication.
   * @param str the String to encode.
   * @return the encoded string.
   */
  public static String uuencode(byte[] data) {
    return (uuencode(data, 0, data.length));
  }
  
  /** uuencode(base64) the given String.
   *  useful for encoding basic authentication.
   * @param str the String to encode.
   * @return the encoded string.
   */
  public static String uuencode(byte[] data, int offset, int length) {
    StringBuffer ret = new StringBuffer();
    char ch, ch1, ch2, ch3;
    int i;
    int end = offset + length;
    
    for (i = offset; i + 2 < end; i += 3) {
      ch1 = (char) (data[i] & 0xff);
      ch2 = (char) (data[i + 1] & 0xff);
      ch3 = (char) (data[i + 2] & 0xff);
      ch = uu_base64[((ch1 >> 2) & 077)];
      ret.append(ch);
      
      ch = uu_base64[(((ch1 << 4) & 060) | ((ch2 >> 4) & 017))];
      ret.append(ch);
      
      ch = uu_base64[(((ch2 << 2) & 074) | ((ch3 >> 6) & 03))];
      ret.append(ch);
      
      ch = uu_base64[(ch3 & 077)];
      ret.append(ch);
    }
    
    // are we done yet?
    if (i == end)
      return ret.toString();
    
    // no so handle the trailing characters.
    ch1 = (char) (data[i] & 0xff);
    ch2 = end > i + 1 ? (char) (data[i + 1] & 0xff) : (char) 0;
    
    ch = uu_base64[((ch1 >> 2) & 077)];
    ret.append(ch);
    
    ch = uu_base64[(((ch1 << 4) & 060) | ((ch2 >> 4) & 017))];
    ret.append(ch);
    
    if (end > i + 1) {
      ch = uu_base64[((ch2 << 2) & 074)];
      ret.append(ch);
    } else {
      ret.append('=');
    }
    ret.append('=');
    return ret.toString();
  }
  
  /** URLDecode a string. This is useful when your handling CGI-arguments..
   * @param enc the encoded String.
   * @return an unescaped String. ('%dd' => 'c', '+' => ' ')
   */
  public static String URLdecode(String enc) {
    enc = enc.replace('+', ' ');
    int i = 0;
    while (true) {
      i = enc.indexOf('%', i);
      if (i < 0)
        break;
      try {
        char c = (char) Integer.parseInt(enc.substring(i + 1, i + 3), 16);
        enc = enc.substring(0, i) + c + enc.substring(i + 3);
      } catch (NumberFormatException e) {
        // ignore
      }
    }
    return enc;
  }
  
  /** URLEncode a string. Only a convenience function for 
   *  java.net.URLEncoder.encode ()
   * @param str the String to URLEncode.
   */
  public static String URLEncode(String str) {
    String enc = java.nio.charset.Charset.defaultCharset().name();
    try {
      return java.net.URLEncoder.encode(str, enc);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Could not url encode " + str + " with default jvm charset: " + enc, e);
    }
  }
  
  public static void main(String[] args) throws Exception {
    GetOpt opt = new GetOpt(args, "-e: -d:");
    String arg = null;
    byte[] in = null;
    
    if (args.length == 0) {
      GetOpt.usage(Coder.class, " -e <string to uuencode> -d <string to uudecode>");
    }
    
    while ((arg = opt.nextArg()) != null) {
      if (arg.equals("e")) {
        System.out.println(uuencode(opt.nextString()));
        continue;
      }
      
      if (arg.equals("d")) {
        System.out.println(uudecode(opt.nextString()));
        continue;
      }
      
      GetOpt.usage("Bad argument: " + arg + "=" + opt.nextString(), Coder.class,
                   " -e <string to uuencode> -d <string to uudecode>");
    }
  }
}
