package alcatel.tess.hometop.gateways.http;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

/**
 * Some useful methods for Digest Authentication.
 * @see RFC 2617
 */
public class DigestUtils {
  
  private static StringBuilder sb = new StringBuilder();
  
  private static final char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
      'e', 'f' };
  
  /**
   * Generates the digest response according to RFC 2617.
   * 
   * @param user The username.
   * @param password The password.
   * @param realm The realm.
   * @param method The method.
   * @param uri The uri.
   * @param algorithm The algorithm.
   * @param qop THe qop.
   * @param nonce The nonce.
   * @param cnonce The cnonce.
   * @param nonceCount The nonce count.
   * @return The response value.
   */
  public static String getDigestResponse(String user, String password, String realm, String method,
                                         String uri, String algorithm, String qop, String nonce,
                                         String cnonce, int nonceCount) {
    if (algorithm == null) {
      algorithm = "MD5";
    }
    
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("MD5");
    } catch (Exception e) {
      throw new IllegalArgumentException("Unsupported algorithm in HTTP Digest authentication: " + algorithm);
    }
    
    String ha1 = computeHA1(user, realm, password, algorithm, nonce, cnonce, md);
    String ha2 = computeHA2(method, uri, md);
    StringBuilder buffer = new StringBuilder();
    
    if (qop == null) {
      buffer.append(ha1).append(':').append(nonce).append(ha2);
    } else { // qop == "auth"
      String sNonceCount = DigestUtils.formatNC(nonceCount);
      buffer.append(ha1).append(':').append(nonce).append(':').append(sNonceCount).append(':').append(cnonce);
      buffer.append(':').append("auth").append(':').append(ha2);
    }
    String s = buffer.toString();
    byte[] digest = md.digest(getBytes(s));
    buffer.setLength(0);
    
    toHexString(digest, buffer);
    return buffer.toString();
  }
  
  /**
   * Generates the digest response according to RFC 2617.
   * 
   * @param ha1 The HA1 result.
   * @param method The method.
   * @param uri The uri.
   * @param algorithm The algorithm.
   * @param qop THe qop.
   * @param nonce The nonce.
   * @param cnonce The cnonce.
   * @param nonceCount The nonce count.
   * @return The response value.
   */
  public static String getDigestResponse(String ha1, String method, String uri, String qop, String nonce,
                                         String cnonce, int nonceCount) {
    
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("MD5");
    } catch (Exception e) {
      throw new IllegalArgumentException("Unsupported algorithm in HTTP Digest authentication: MD5");
    }
    
    String ha2 = computeHA2(method, uri, md);
    StringBuilder buffer = new StringBuilder();
    
    if (qop == null) {
      buffer.append(ha1).append(':').append(nonce).append(ha2);
    } else { // qop == "auth"
      String sNonceCount = DigestUtils.formatNC(nonceCount);
      buffer.append(ha1).append(':').append(nonce).append(':').append(sNonceCount).append(':').append(cnonce);
      buffer.append(':').append("auth").append(':').append(ha2);
    }
    String s = buffer.toString();
    byte[] digest = md.digest(getBytes(s));
    buffer.setLength(0);
    
    toHexString(digest, buffer);
    return buffer.toString();
  }
  
  /**
   * Gets a int  value for the nonce-count.
   * 
   * @param nc The nonce-count.
   * @return The int value.
   */
  public static int getNC(String nc) {
    return Integer.parseInt(nc, 16);
  }
  
  /**
   * Formats the nonce-count into the RFC 2617 format.
   * 
   * @param nc The int value of the nonce-count.
   * @return The nonce-count in the RFC 2617 format.
   */
  public static String formatNC(int nc) {
    StringBuilder buffer = new StringBuilder(Integer.toHexString(nc));
    while (buffer.length() < 8) {
      buffer.insert(0, '0');
    }
    return buffer.toString();
  }
  
  // --------------------------------------------------------------------------------------------
  //                                   Private methods
  // --------------------------------------------------------------------------------------------
  
  /**
   * Puts the data into the output string buffer in hexadecimal format.
   * 
   * @param data The input data.
   * @param out The output string buffer
   */
  private static void toHexString(byte[] data, StringBuilder out) {
    if (data.length != 16) {
      throw new IllegalArgumentException("invalid md5 data size: " + data.length);
    }
    
    for (int i = 0; i < 16; i++) {
      int high = ((data[i] >>> 4) & 0xf);
      out.append(HEX[high]);
      int low = (data[i] & 0x0f);
      out.append(HEX[low]);
    }
  }
  
  /**
   * Gets bytes for the string for ISO-8859-1 encoding.
   * 
   * @param s The string.
   * @return The bytes.
   */
  private static byte[] getBytes(String s) {
    try {
      return s.getBytes("ISO-8859-1");
    } catch (UnsupportedEncodingException ignored) {
      return s.getBytes(); // Not possible
    }
  }
  
  /**
   *  Computes the RFC 2617 A1 algorithm for request digest.
   *  
   * @param user The username
   * @param realm The realm.
   * @param password The password.
   * @param algorithm The algorithm.
   * @param nonce The nonce.
   * @param cnonce The client nonce.
   * @param md The message digest.
   * @return The result.
   */
  public static String computeHA1(String user, String realm, String password, String algorithm, String nonce,
                                  String cnonce, MessageDigest md) {
    synchronized (sb) {
      try {
        sb.setLength(0);
        sb.append(user);
        sb.append(':');
        sb.append(realm);
        sb.append(':');
        sb.append(password);
        
        if ("MD5-sess".equalsIgnoreCase(algorithm)) {
          byte[] digest = md.digest(getBytes(sb.toString()));
          sb.setLength(0);
          toHexString(digest, sb);
          sb.append(':').append(nonce).append(':').append(cnonce);
        }
        
        byte[] digest = md.digest(getBytes(sb.toString()));
        sb.setLength(0);
        toHexString(digest, sb);
        
        return sb.toString();
      }
      
      finally {
        sb.setLength(0);
      }
    }
  }
  
  /**
   *  Computes the RFC 2617 A2 algorithm for request digest.
   * @param method The method.
   * @param uri The Digest URI.
   * @param md The message digest.
   * @return The result.
   */
  public static String computeHA2(String method, String uri, MessageDigest md) {
    synchronized (sb) {
      try {
        sb.setLength(0);
        sb.append(method).append(':').append(uri);
        byte[] digest = md.digest(getBytes(sb.toString()));
        sb.setLength(0);
        toHexString(digest, sb);
        return sb.toString();
      }
      
      finally {
        sb.setLength(0);
      }
    }
  }
  
}
