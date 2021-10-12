// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.http.HttpUtils;

/**
 * This class regroup static misc methods.
 */
public class Utils {
  
  public final static String LINE_SEPARATOR = System.getProperty("line.separator");
  public final static String LINE_SEPARATOR2 = LINE_SEPARATOR + LINE_SEPARATOR;
  public final static byte[] NULL_BYTE_ARRAY = new byte[0];
  
  private static boolean propertiesLoaded = false;
  
  static {
    loadSystemProperties();
  }
  
  /**
   * Load a resource from the classpath, or from a given class loader,
   */
  public static URL getResource(String resource, ClassLoader ... classLoaders) {
    ClassLoader[] loaders = (ClassLoader[]) classLoaders;
    URL u = null;
    for (ClassLoader cl : loaders) {
      if (cl != null) {
        if ((u = cl.getResource(resource)) != null) {
          return u;
        }
      }
    }
    
    return null;
  }
  
  /**
   * Tells if a string match a simple pattern.
   * The pattern may contains a "*" (and only one).
   */
  public static boolean match(String str, String pattern) {
    boolean fromStart = false, fromEnd = false, fromMiddle = false;
    String patternStart = null, patternEnd = null;
    pattern = pattern.trim();
    
    if (pattern.charAt(0) == '*') {
      pattern = pattern.substring(1);
      fromEnd = true;
    } else if (pattern.charAt(pattern.length() - 1) == '*') {
      fromStart = true;
      pattern = pattern.substring(0, pattern.length() - 2);
    } else if (pattern.indexOf("*") != -1) {
      fromMiddle = true;
      patternStart = pattern.substring(0, pattern.indexOf("*"));
      patternEnd = pattern.substring(pattern.indexOf("*") + 1);
    }
    
    if (fromStart) {
      if (str.startsWith(pattern)) {
        return true;
      }
    } else if (fromEnd) {
      if (str.endsWith(pattern)) {
        return true;
      }
    } else if (fromMiddle) {
      if (str.startsWith(patternStart) && str.endsWith(patternEnd)) {
        return true;
      }
    } else {
      if (str.equals(pattern)) {
        return true;
      }
    }
    
    return false;
  }
  
  /**
   * Load propertis from $INSTALL_DIR/resource/system/*.properties and
   * append them into System properties.
   */
  public static synchronized void loadSystemProperties() {
    try {
      if (propertiesLoaded) {
        return;
      }
      
      URL propDir = (URL) ClassLoader.getSystemResource("properties");
      if (propDir == null) {
        propDir = Utils.class.getResource("properties");
      }
      
      if (propDir == null) {
        return;
      }
      
      File[] files = new File(propDir.getPath()).listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return (name.endsWith(".properties"));
        }
      });
      
      for (int i = 0; files != null && i < files.length; i++) {
        Properties p = new Properties();
        p.load(new FileInputStream(files[i]));
        Enumeration e = p.propertyNames();
        while (e.hasMoreElements()) {
          String name = (String) e.nextElement();
          String val = p.getProperty(name, null);
          
          if (val != null) {
            if (System.getProperty(name, null) == null) {
              System.setProperty(name, val);
            }
          }
        }
      }
    }
    
    catch (Throwable t) {
      t.printStackTrace();
    }
    
    finally {
      // Ensure that mandatory system properties are setup.
      if (System.getProperty("com.alcatel.as.agent.CurrentThreadExecutor") == null) {
        System.setProperty("com.alcatel.as.agent.CurrentThreadExecutor",
                           "alcatel.tess.hometop.gateways.concurrent.CurrentThreadExecutor");
      }
      
      if (System.getProperty("com.alcatel.as.agent.ThreadPoolExecutor") == null) {
        System.setProperty("com.alcatel.as.agent.ThreadPoolExecutor",
                           "alcatel.tess.hometop.gateways.concurrent.ThreadPoolExecutor");
      }
      
      if (System.getProperty("com.alcatel.as.agent.SchedulerExecutor") == null) {
        System.setProperty("com.alcatel.as.agent.SchedulerExecutor",
                           "alcatel.tess.hometop.gateways.concurrent.SchedulerExecutor");
      }
      
      propertiesLoaded = true;
    }
  }
  
  /**
   * Round Up a number to a power of two.
   */
  public static int round2(int x) {
    int log = 1;
    for (int n = x - 1; (n >>= 1) > 0;)
      log++;
    return (1 << log);
  }
  
  /**
   * Indicates if the content type contains binary data.
   */
  public static boolean isBinaryType(String contentType) {
    if (contentType == null) {
      return (false);
    }
    
    if (contentType.regionMatches(true, 0, "image/", 0, "image/".length())) {
      return (true);
    }
    
    if (contentType.regionMatches(true, 0, "audio/", 0, "audio/".length())) {
      return (true);
    }
    
    if (contentType.regionMatches(true, 0, "video/", 0, "video/".length())) {
      return (true);
    }
    
    if (binContentTypes.get(contentType) != null) {
      return (true);
    }
    
    return (false);
  }
  
  /**
   * The <code>getGMT</code> method returns the apache like
   * current time in gmt format.
   *
   * @return a <code>String</code> value
   */
  public static StringBuffer getGMT(StringBuffer buf) {
    buf.append(dtFormat.format(new Date()));
    buf.append(" ").append(GMTOffset);
    return (buf);
  }
  
  /**
   * Parse an exception into a string.
   * @param e The exception to parse
   * @return the parsed exception
   */
  public static String parse(Throwable e) {
    StringWriter buffer = new StringWriter();
    PrintWriter pw = new PrintWriter(buffer);
    
    e.printStackTrace(pw);
    
    return (buffer.toString());
  }
  
  /**
   * Method declaration
   * @return
   */
  public static String getStackTrace() {
    try {
      throw new Exception("stack:");
    } catch (Exception e) {
      return (parse(e));
    }
  }
  
  /**
   * Parse an exception into a string, and double quotes inside it.
   * This method should be used for sql aware exception management.
   * @param e The exception to parse
   * @return the parsed exception
   */
  public static String parseForSql(Exception e) {
    String err = parse(e);
    
    return (verifyQuote(err));
  }
  
  /**
   * Verify if a string have quotes and double them, if necessary.
   * @param word The string to parse
   * @return the parsed
   */
  public static String verifyQuote(String word) {
    String dummy = "";
    int pos = word.indexOf("'");
    
    if (pos > -1) {
      dummy = word.substring(0, pos);
      dummy += "''";
      dummy += verifyQuote(word.substring(pos + 1));
      
      return dummy;
    }
    
    return (word);
  }
  
  /**
   * Build a String from a String table.
   * 
   * @param a string table
   * @return the concataination of all element tables
   */
  public static String tabToString(String[] strtab) {
    String tab = "[";
    
    for (int i = 0; i < strtab.length; i++) {
      tab += strtab[i];
      
      if (i < (strtab.length - 1)) {
        tab += ",";
      }
    }
    
    tab += "]";
    
    return (tab);
  }
  
  /**
   * Create a class with its class given in args.
   * @param clazz The class to instanciate
   * @exception ClassNotFoundException if the class is not found
   */
  public static Object create(String className, Properties p) throws ClassNotFoundException {
    try {
      Class c = Class.forName(className);
      Constructor constr = c.getConstructor(new Class[] { Properties.class });
      
      return (constr.newInstance(new Object[] { p }));
    } catch (NoSuchMethodException e) {
      String x = "the " + className + " do not have such a constructor" + ", exception= " + e.toString();
      
      throw new ClassNotFoundException(x);
    } catch (InstantiationException e) {
      String x = "cannot instantiate abstract class " + className + ", exception= " + e.toString();
      
      throw new ClassNotFoundException(x);
    } catch (IllegalAccessException e) {
      String x = "cannot instantiate class " + className
          + " (not public or located in another package), exception= " + e.toString();
      
      throw new ClassNotFoundException(x);
    } catch (IllegalArgumentException e) {
      String x = "the " + className + " do not have such a constructor" + ", exception= " + e.toString();
      
      throw new ClassNotFoundException(x);
    } catch (InvocationTargetException e) {
      String x = "exception caught in " + className + " constructor: " + Utils.parse(e.getTargetException());
      
      throw new ClassNotFoundException(x);
    }
  }
  
  /**
   * Method declaration
   *
   * @param str
   * @return
   *
   * @throws NumberFormatException
   */
  public static byte[] toByteArray(String str) throws NumberFormatException {
    if (str == null) {
      return (null);
    }
    
    StringTokenizer tok = new StringTokenizer(str);
    int count = tok.countTokens();
    byte barray[] = new byte[count];
    
    for (int i = 0; i < count; i++) {
      String byt = tok.nextToken();
      
      barray[i] = (byte) Integer.decode(byt).intValue();
    }
    
    return (barray);
  }
  
  /**
   * Method declaration
   *
   * @param b
   * @return
   */
  public static int getUnsigned(byte b) {
    return ((int) b & 0xff);
  }
  
  /**
   * Removes all the spaces and control chars from a String.
   */
  public static String removeSpaces(String s) {
    StringBuffer buff = new StringBuffer();
    char c;
    boolean change = false;
    for (int i = 0; i < s.length(); i++)
      if ((c = s.charAt(i)) > ' ')
        buff.append(c);
      else
        change = true;
    return (change) ? buff.toString() : s;
  }
  
  private static final String[] table = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C",
      "D", "E", "F" };
  
  /**
   * Method declaration
   *
   * @param hdr
   * @param data
   * @return
   */
  public static String dumpByteArray(String hdr, Bytes data) {
    return (dumpByteArray(hdr, data.getBytes(), data.getLength()));
  }
  
  /**
   * Method declaration
   *
   * @param hdr
   * @param arr
   * @param length
   * @return
   */
  public static String dumpByteArray(String hdr, byte[] arr) {
    return (dumpByteArray(hdr, arr, arr.length));
  }
  
  /**
   * Method declaration
   *
   * @param hdr
   * @param arr
   * @param length
   * @return
   */
  public static String dumpByteArray(String hdr, byte[] arr, int length) {
    return dumpByteArray(hdr, arr, length, Integer.MAX_VALUE);
  }
  
  /**
   * Method declaration
   *
   * @param hdr
   * @param arr
   * @param length
   * @return
   */
  public static String toString(byte[] arr) {
    return (toString(arr, 0, arr.length));
  }
  
  public static String toString(byte[] arr, int off, int len) {
    return dumpByteArray("", arr, off, len, len);
  }
  
  public static String dumpByteArray(String hdr, byte[] arr, int length, int max) {
    return dumpByteArray(hdr, arr, 0, length, max);
  }
  
  public static String dumpByteArray(String hdr, byte[] arr, int offset, int length, int max) {
    //
    // Immediate return if we have a null or empty array
    //
    if (arr == null) {
      return hdr + "Null\n";
    }
    if (length == 0) {
      return hdr + "None\n";
    }
    
    //
    // Build our empty header
    //
    StringBuffer s = new StringBuffer(hdr);
    String emptyHdr = "";
    for (int i = 0; i < hdr.length(); ++i) {
      emptyHdr += " ";
    }
    
    //
    // See what we want to really dump, i.e.: up to the Max value...
    //
    if (length > max) {
      s.append('(').append(max).append(" bytes dumped out of ").append(length).append(")\n").append(emptyHdr);
      length = max;
    }
    
    //
    //  And dump our data
    //
    int off = 0;
    for (;;) {
      //
      //  Display hex values
      //
      for (int i = 0; i < 16; ++i) {
        if (i < length) {
          int val = (int) arr[offset + off + i] & 255;
          s.append(table[val / 16]).append(table[val & 15]).append(' ');
        } else {
          s.append("   ");
        }
      }
      
      //
      //  And add character values
      //
      s.append("    ");
      for (int i = 0; i < 16; ++i) {
        if (i < length) {
          int val = (int) arr[offset + off + i] & 255;
          if (val >= 0x20 && val < 0x7F) {
            s.append((char) val);
          } else {
            s.append('.');
          }
        } else {
          s.append(' ');
        }
      }
      
      //
      //  All set for this block. Get set for the next one
      //
      off += 16;
      if ((length -= 16) <= 0) {
        return s.toString();
      } else {
        s.append('\n');
        s.append(emptyHdr);
      }
    }
  }
  
  /**
   * Method declaration
   *
   * @param s
   * @return
   */
  public static String removeSurroundingQuotes(String s) {
    int length = s.length();
    int beginQuoteIndex = 0;
    int endQuoteIndex = length;
    
    if (s.charAt(0) == '\"') {
      beginQuoteIndex = 1;
    }
    
    if (s.charAt(length - 1) == '\"') {
      endQuoteIndex = length - 1;
    }
    
    if (beginQuoteIndex > 0 || endQuoteIndex < length) {
      return (s.substring(beginQuoteIndex, endQuoteIndex));
    } else {
      return (s);
    }
  }
  
  /**
   * Method declaration
   *
   * @param className
   * @return
   *
   * @throws ClassNotFoundException
   */
  public static Object create(String className) throws ClassNotFoundException {
    try {
      Class c = Class.forName(className);
      Constructor constr = c.getConstructor();
      return (constr.newInstance());
    } catch (NoSuchMethodException e) {
      String x = "the " + className + " do not have such a constructor" + ", exception= " + e.toString();
      
      throw new ClassNotFoundException(x);
    } catch (InstantiationException e) {
      String x = "cannot instantiate abstract class " + className + ", exception= " + e.toString();
      
      throw new ClassNotFoundException(x);
    } catch (IllegalAccessException e) {
      String x = "cannot instantiate class " + className
          + " (not public or located in another package), exception= " + e.toString();
      
      throw new ClassNotFoundException(x);
    } catch (IllegalArgumentException e) {
      String x = "the " + className + " do not have such a constructor" + ", exception= " + e.toString();
      
      throw new ClassNotFoundException(x);
    } catch (InvocationTargetException e) {
      String x = "exception caught in " + className + " constructor: " + Utils.parse(e.getTargetException());
      throw new ClassNotFoundException(x);
    } catch (ClassNotFoundException e) {
      String x = "could not find class: " + className;
      
      throw new ClassNotFoundException(x);
    }
  }
  
  /**
   * Test if a string is a target content-type.
   * @param ctype The String to test
   * @param target The expected content-type.
   *
   * @return true if the <code>ctype</code> parameter has the
   *	<code>target</code> content-type, or false if not.
   */
  public final static boolean isContentType(String ctype, String target) {
    if (ctype == null || target == null)
      return false;
    
    return (ctype.regionMatches(true, 0, target, 0, target.length()));
  }
  
  /**
   * Encode an url, changing ampersends to standard wml entities.
   */
  public final static String encodeWmlUrl(String url) {
    StringBuffer stringbuffer = new StringBuffer("");
    
    for (StringTokenizer stringtokenizer = new StringTokenizer(url, "&"); stringtokenizer.hasMoreTokens();) {
      stringbuffer.append(stringtokenizer.nextToken());
      
      if (stringtokenizer.hasMoreTokens())
        stringbuffer.append("&amp;");
    }
    
    return stringbuffer.toString();
  }
  
  /**
   * Return the current jvm environment (class path, jvm version, etc ...)
   */
  public static String getJvmEnv() {
    return (" -version:" + System.getProperty("java.version") + LINE_SEPARATOR + " -java vendor:"
        + System.getProperty("java.vendor") + LINE_SEPARATOR + " -osname:" + System.getProperty("os.name")
        + LINE_SEPARATOR + " -working dir:" + System.getProperty("user.dir") + LINE_SEPARATOR + " -compiler:"
        + System.getProperty("java.compiler") + LINE_SEPARATOR + " -file encoding:"
        + System.getProperty("file.encoding") + LINE_SEPARATOR + " -class path:"
        + System.getProperty("java.class.path") + LINE_SEPARATOR + " -library path:" + System
          .getProperty("java.library.path"));
  }
  
  //
  //  Encode a long value in a byte array
  //
  public static int putValue(int value, byte[] array, int offset) {
    array[offset++] = (byte) (value);
    array[offset++] = (byte) (value >> 8);
    array[offset++] = (byte) (value >> 16);
    array[offset++] = (byte) (value >> 24);
    return offset;
  }
  
  //
  //  Retrieve a long value from a byte array
  //
  public static int getValue(byte[] array, int offset) {
    int value = ((int) array[offset++]) & 0x0FF;
    value |= (((int) array[offset++]) << 8) & 0x0FF00;
    value |= (((int) array[offset++]) << 16) & 0x0FF0000;
    value |= (((int) array[offset++]) << 24) & 0x0FF000000;
    return value;
  }
  
  //
  // Retrieve an ip address from a big endian integer
  //
  public static String getIpAddr(int addr) {
    StringBuffer buf = new StringBuffer();
    buf.append(addr & 0xff);
    buf.append('.');
    buf.append((addr >> 8) & 0xff);
    buf.append('.');
    buf.append((addr >> 16) & 0xff);
    buf.append('.');
    buf.append((addr >> 24) & 0xff);
    return (buf.toString());
  }
  
  //
  // Convert a string ip address into a big endian integer
  //
  public static int getIpAddr(String addr) {
    StringTokenizer t = new StringTokenizer(addr, ".");
    int rawAddress = 0;
    
    try {
      rawAddress = Integer.parseInt(t.nextToken()) & 0x0FF;
      rawAddress |= ((Integer.parseInt(t.nextToken()) & 0x0FF) << 8);
      rawAddress |= ((Integer.parseInt(t.nextToken()) & 0x0FF) << 16);
      rawAddress |= ((Integer.parseInt(t.nextToken()) & 0x0FF) << 24);
    }
    
    catch (NumberFormatException e) {
      throw new IllegalArgumentException("Bad ip addr: " + addr);
    }
    
    return (rawAddress);
  }
  
  //
  //  Places a String in a byte array
  //
  public static int putString(String s, byte[] array, int offset) {
    byte[] tmp = Charset.makeBytes(s);
    if (tmp.length != 0) {
      System.arraycopy(tmp, 0, array, offset, tmp.length);
    }
    return offset + tmp.length;
  }
  
  //
  //  Places a byte array in a byte array
  //
  public static int putBytes(byte[] src, int len, byte[] array, int offset) {
    if (len != 0) {
      System.arraycopy(src, 0, array, offset, len);
    }
    return offset + len;
  }
  
  //
  //  Retrieve a byte array of the specified length
  //
  public static byte[] getBytes(byte[] array, int offset, int length) {
    byte[] ret = new byte[length];
    if (length != 0) {
      System.arraycopy(array, offset, ret, 0, length);
    }
    return ret;
  }
  
  //
  //  Retrieve a String off the specified byte buffer
  //
  //      NOTE:   This method is only here because of a bug in the
  //              String (byte[]) constructor which does not properly
  //              stop creating a string when it encounters a 0 in the
  //              byte array in some cases...
  //
  public static String getString(byte[] array, int offset, int length) {
    int size;
    for (size = 0; size < length; ++size) {
      if (array[offset + size] == 0) {
        break;
      }
    }
    return Charset.makeString(array, offset, size);
  }
  
  public static String getHttpReason(int code) {
    return HttpUtils.getHttpReason(code);
  }
  
  public static final int UPPER_TO_LOWER = (int) ('a' - 'A');
  
  /**
   * Indicates if the character is an ascii printable char. 
   */
  public static boolean isPrintable(int ch) {
    return (ch == '\r' || ch == '\n' || ch == '\t' || ((ch - 0x20) | (0x7E - ch)) >= 0);
  }
  
  public static boolean isPrintable(byte b) {
    int ch = (int) b & 0xff;
    return (ch == '\r' || ch == '\n' || ch == '\t' || ((ch - 0x20) | (0x7E - ch)) >= 0);
  }
  
  public static String toASCIIUpperCase(String s) {
    char[] upper = null;
    int length = s.length();
    for (int i = 0; i < length; i++) {
      char c = s.charAt(i);
      if ((c > '\u0060') && (c < '\u007b')) { // means 'a'<=c<='z'
        if (upper == null) {
          upper = s.toCharArray();
        }
        
        upper[i] = (char) (c - UPPER_TO_LOWER);
      }
    }
    return (upper != null) ? new String(upper) : s;
  }
  
  public static String toASCIILowerCase(char[] buf, int off, int len) {
    return (toASCIILowerCase(buf, off, len, true, false));
  }
  
  public static String toASCIILowerCase(char[] buf, int off, int len, boolean modif) {
    return (toASCIILowerCase(buf, off, len, modif, false));
  }
  
  public static String toASCIILowerCase(char[] buf, int off, int len, boolean modif, boolean trim) {
    char[] lower = buf;
    
    if (trim) {
      while ((off < len) && (buf[off] <= ' ')) {
        off++;
        len--;
      }
      
      while ((off < len) && (buf[off + len - 1] <= ' ')) {
        len--;
      }
    }
    
    if (modif == false) {
      char tmp[] = new char[len];
      System.arraycopy(lower, off, tmp, 0, len);
      lower = tmp;
    }
    
    for (int i = off; i < len; i++) {
      char c = lower[i];
      if ((c > '\u0040') && (c < '\u005b')) { // means 'A'<=c<='Z'
        lower[i] = (char) (c + UPPER_TO_LOWER);
      }
    }
    
    if (modif == false) {
      return (new String(lower, 0, len));
    } else {
      return (new String(lower, off, len));
    }
  }
  
  public static String toASCIILowerCase(String s) {
    char[] lower = null;
    int length = s.length();
    for (int i = 0; i < length; i++) {
      char c = s.charAt(i);
      if ((c > '\u0040') && (c < '\u005b')) { // means 'A'<=c<='Z'
        if (lower == null) {
          lower = s.toCharArray();
        }
        
        lower[i] = (char) (c + UPPER_TO_LOWER);
      }
    }
    return (lower != null) ? new String(lower) : s;
  }
  
  public static String capitalizeFirstLetter(String s) {
    char[] resp = null;
    boolean postDash = true;
    int length = s.length();
    for (int i = 0; i < length; i++) {
      char c = s.charAt(i);
      if (postDash) {
        // To Upper Case
        if ((c > '\u0060') && (c < '\u007b')) { // means 'a'<=c<='z'
          if (resp == null) {
            resp = s.toCharArray();
          }
          resp[i] = (char) (c - UPPER_TO_LOWER);
        }
      } else
      // to Lower Case
      if ((c > '\u0040') && (c < '\u005b')) { // means 'A'<=c<='Z'
        if (resp == null) {
          resp = s.toCharArray();
        }
        resp[i] = (char) (c + UPPER_TO_LOWER);
      }
      postDash = (c == '-');
    }
    
    return (resp != null) ? new String(resp) : s;
  }
  
  public static String trim(String s, int off, int length) {
    int len = length;
    int st = off;
    int max = off + length;
    
    while ((st < max) && (s.charAt(st) <= ' ')) {
      st++;
      len--;
    }
    
    while ((len > 0) && (s.charAt(st + len - 1)) <= ' ') {
      len--;
    }
    
    return (s.substring(st, st + len));
  }
  
  public static String trim(String s) {
    return (trim(s, 0, s.length()));
  }
  
  /**
   * convert hexadecimal strings to byte array
   */
  public static void parseHexaString(String line, OutputStream out) throws IOException {
    StringTokenizer tok = new StringTokenizer(line, " ");
    
    while (tok.hasMoreTokens()) {
      int hex = Integer.parseInt(tok.nextToken(), 16);
      out.write(hex);
    }
  }
  
  private static char ff = (char) 0xff;
  
  /**
   * Date formater used by the logAccess method.
   */
  private final static SimpleDateFormat dtFormat = new SimpleDateFormat("dd/MMM/yyyy:H:mm:ss:SSS");
  
  /**
   * GMT offset complying to the apache combined log format.
   */
  private static String GMTOffset;
  
  /**
   * List of binary content-types.
   */
  public static StringCaseHashtable binContentTypes = new StringCaseHashtable();
  
  public static void clearArray(Object[] arr) {
    int n = 0;
    
    while (n < arr.length) {
      int size = Math.min(NULL_ARRAY.length, arr.length - n);
      System.arraycopy(NULL_ARRAY, 0, arr, n, size);
      n += size;
    }
  }
  
  public static void clearArray(Object[] array, int off, int len) {
    int s = 0;
    
    while (len > 0) {
      System.arraycopy(NULL_ARRAY, 0, array, off, (s = Math.min(len, NULL_ARRAY.length)));
      off += s;
      len -= s;
    }
  }
  
  private final static Object[] NULL_ARRAY = new Object[128];
  
  /*
   * Encodes the given string with MD5
   * @param key : The string to encode.
   * @return The encoded MD5 string in hexadecimale on 32 bits.
   */
  public static String encode(String key) throws Exception {
    if (key == null)
      return null;
    
    byte[] uniqueKey = key.getBytes("ascii");
    byte[] hash = null;
    
    // Objet un objet qui permettra de crypter la chaine
    synchronized (md5) {
      hash = md5.digest(uniqueKey);
    }
    
    StringBuffer hashString = new StringBuffer();
    for (int i = 0; i < hash.length; ++i) {
      String hex = Integer.toHexString(hash[i]);
      if (hex.length() == 1) {
        hashString.append('0');
        hashString.append(hex.charAt(hex.length() - 1));
      } else {
        hashString.append(hex.substring(hex.length() - 2));
      }
    }
    return hashString.toString();
  }
  
  public static void dumpThreads(Logger logger, Level level) {
    StringWriter sw = new StringWriter();
    sw.write("Threads dump:\n");
    try {
      Map<Thread, StackTraceElement[]> mapStacks = Thread.getAllStackTraces();
      Iterator<Thread> threads = mapStacks.keySet().iterator();
      while (threads.hasNext()) {
        Thread thread = threads.next();
        StackTraceElement[] stes = mapStacks.get(thread);
        sw.write("\nThread [" + thread.getName() + " prio=" + thread.getPriority()
            + "] --> StackTrace elements ...\n");
        for (StackTraceElement ste : stes) {
          sw.write("\t" + ste.toString() + "\n");
        }
      }
      
      logger.log(level, sw.toString());
    } catch (Throwable t) {
      logger.warn("Exception while dumping state", t);
    }
  }
  
  public static void main(String args[]) throws Exception {
    Utils.loadSystemProperties();
    
    System.out.println(System.getProperty("com.alcatel.as.agent.CurrentThreadExecutor"));
    System.out.println(System.getProperty("com.alcatel.as.agent.ThreadPoolExecutor"));
    System.out.println(System.getProperty("com.alcatel.as.agent.SchedulerExecutor"));
    
    System.out.println(isContentType(args[0], args[1]));
    
    Object tab[] = new Object[257];
    for (int i = 0; i < tab.length; i++) {
      if (tab[i] != null) {
        System.out.println("not null at " + i);
        System.exit(1);
      }
      tab[i] = NULL_ARRAY;
    }
    
    clearArray(tab);
    for (int i = 0; i < tab.length; i++) {
      if (tab[i] != null) {
        System.out.println("not null at " + i);
        System.exit(1);
      }
    }
  }
  
  public static String replace(String str, String pattern, String replace) {
    int s = 0;
    int e = 0;
    StringBuffer result = new StringBuffer();
    
    while ((e = str.indexOf(pattern, s)) >= 0) {
      result.append(str.substring(s, e));
      result.append(replace);
      s = e + pattern.length();
    }
    result.append(str.substring(s));
    return result.toString();
  }
  
  private static MessageDigest md5;
  
  static {
    // Initialize md4 digest
    try {
      md5 = MessageDigest.getInstance("MD5");
    }
    
    catch (Throwable t) {
      throw new ExceptionInInitializerError(t);
    }
    
    //
    // Initialize the GMT offset used to format access logs that
    // comply with the apache access log format. The following code
    // is directly inspired from the apache source file "mod_log_config.c"
    // (func "log_request_time").
    //
    long tz = TimeZone.getDefault().getRawOffset() / 1000 / 60;
    
    char sign = (tz < 0 ? '-' : '+');
    
    if (tz < 0L) {
      tz = -tz;
    }
    
    DecimalFormat df = new DecimalFormat("00");
    GMTOffset = sign + df.format(tz / 60) + df.format(tz % 60);
    
    //
    // Initialize binary content types (see Utils.isBinaryType())
    //
    binContentTypes.put("application/vnd.wap.wbxml", "");
    binContentTypes.put("application/vnd.wap.wmlc", "");
    binContentTypes.put("application/vnd.wap.wmlscriptc", "");
    binContentTypes.put("application/pdf", "");
    binContentTypes.put("application/postscript", "");
    binContentTypes.put("application/x-gzip", "");
    binContentTypes.put("application/x-compress", "");
    binContentTypes.put("application/zip", "");
  }
}
