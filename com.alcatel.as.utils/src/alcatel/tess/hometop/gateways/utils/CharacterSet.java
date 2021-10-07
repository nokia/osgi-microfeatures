package alcatel.tess.hometop.gateways.utils;

public class CharacterSet {
  public static final String UPALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  public static final String LALPHA = "abcdefghijklmnopqrstuvwxyz";
  public static final String DIGIT = "0123456789";
  
  public CharacterSet(String ... chars) {
    _charset = new boolean[256];
    
    for (int i = 0; i < _charset.length; i++) {
      for (int j = 0; j < chars.length; j++) {
        if (chars[j].indexOf(i) > -1) {
          _charset[i] = true;
          break;
        }
      }
    }
  }
  
  public void addChar(char c) {
    _charset[c & 0xff] = true;
  }
  
  public void addChars(char start, char end) {
    for (int i = start; i <= end; i++) {
      _charset[i & 0xff] = true;
    }
  }
  
  public boolean belongs(int c) {
    return (c > 255) ? false : _charset[c];
  }
  
  /**
   * Escape a URI header parameter.
   */
  public String escape(String s) {
    if (s == null) {
      return null;
    }
    
    StringBuffer sb = null;
    for (int i = 0; i < s.length(); i++) {
      int c = s.charAt(i);
      if (belongs(c)) {
        if (sb != null) {
          sb.append((char) c);
        }
      } else {
        if (sb == null) {
          sb = new StringBuffer(s.length());
          sb.append(s.substring(0, i));
        }
        sb.append('%');
        String hex = Integer.toString(c, 16).toUpperCase();
        if (hex.length() == 1) {
          sb.append('0');
          sb.append(hex);
        } else {
          sb.append(hex);
        }
      }
    }
    
    return sb == null ? s : sb.toString();
  }
  
  /**
   * Returns a copy of s with occurrences of "%XX", where X is an upper
   * or lower case hex character, replaced with the character equivalent.
   *
   * @throws IllegalArgumentException if a '%' character without two
   *     imediately tailing hex characters is present in the input string
   */
  public String unescape(String s) {
    if (s == null) {
      return null;
    }
    
    StringBuffer sb = null;
    for (int i = 0; i < s.length(); i++) {
      int ascii = s.charAt(i) & 0xffff;
      if (ascii == '%') {
        if (i >= s.length() - 2) {
          throw new IllegalArgumentException("Invalid escaped character found in uri: " + s);
        }
        
        if (sb == null) {
          sb = new StringBuffer();
          sb.append(s, 0, i);
        }
        
        int escaped = 0;
        try {
          escaped = Integer.parseInt(s.substring(i + 1, i + 3), 16);
        }
        
        catch (NumberFormatException e) {
          throw new IllegalArgumentException("Invalid escaped character found in uri: " + s, e);
        }
        
        if (escaped <= -1) {
          throw new IllegalArgumentException("Invalid escaped character found in uri: " + s);
        }
        
        sb.append((char) escaped);
        i += 2;
        continue;
      }
      
      if (sb != null) {
        sb.append(s.charAt(i));
      }
    }
    
    if (sb != null) {
      return sb.toString();
    }
    
    return s;
  }
  
  private boolean[] _charset;
}
