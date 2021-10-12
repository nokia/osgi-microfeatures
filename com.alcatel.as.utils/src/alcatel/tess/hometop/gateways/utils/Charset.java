// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * The <code>Charset</code> class is meant to be used when you need to build
 * efficiently strings from bytes, and conversely when you need to build bytes 
 * from strings.
 *
 */
public class Charset {
  protected final static Map charsetsMapByMIB = new HashMap();
  protected final static Map charsetsMapByIANA = new HashMap();
  
  // We'll use this name in order to match the default platform charset encoding.
  public final static String DEFAULT_PLATFORM_CHARSET = "DEFAULT";
  
  // IANA MIBEnum values
  public static final int MIB_US_ASCII = 3;
  public static final int MIB_ISO_8859_1 = 4;
  public static final int MIB_ISO_8859_2 = 5;
  public static final int MIB_ISO_8859_3 = 6;
  public static final int MIB_ISO_8859_4 = 7;
  public static final int MIB_ISO_8859_5 = 8;
  public static final int MIB_ISO_8859_6 = 9;
  public static final int MIB_ISO_8859_7 = 10;
  public static final int MIB_ISO_8859_8 = 11;
  public static final int MIB_ISO_8859_9 = 12;
  public static final int MIB_SHIFT_JIS = 17;
  public static final int MIB_EUC_JP = 18;
  public static final int MIB_ISO_2022_JP = 39;
  public static final int MIB_UTF_8 = 106;
  public static final int MIB_UCS_2 = 1000;
  //public static final int MIB_UCS_4		= 1001;
  public static final int MIB_UTF_16BE = 1013;
  public static final int MIB_UTF_16LE = 1014;
  public static final int MIB_BIG5 = 2026;
  public static final int MIB_GB2312 = 2025;
  public static final int MIB_GBK = 113;
  
  public static final Charset CHARSET_US_ASCII = new Charset("US-ASCII", MIB_US_ASCII);
  public static final Charset CHARSET_ISO_8859_1 = new Charset("ISO-8859-1", MIB_ISO_8859_1);
  public static final Charset CHARSET_ISO_8859_2 = new Charset("ISO-8859-2", MIB_ISO_8859_2);
  public static final Charset CHARSET_ISO_8859_3 = new Charset("ISO-8859-3", MIB_ISO_8859_3);
  public static final Charset CHARSET_ISO_8859_4 = new Charset("ISO-8859-4", MIB_ISO_8859_4);
  public static final Charset CHARSET_ISO_8859_5 = new Charset("ISO-8859-5", MIB_ISO_8859_5);
  public static final Charset CHARSET_ISO_8859_6 = new Charset("ISO-8859-6", MIB_ISO_8859_6);
  public static final Charset CHARSET_ISO_8859_7 = new Charset("ISO-8859-7", MIB_ISO_8859_7);
  public static final Charset CHARSET_ISO_8859_8 = new Charset("ISO-8859-8", MIB_ISO_8859_8);
  public static final Charset CHARSET_ISO_8859_9 = new Charset("ISO-8859-9", MIB_ISO_8859_9);
  public static final Charset CHARSET_SHIFT_JIS = new Charset("Shift_JIS", MIB_SHIFT_JIS);
  public static final Charset CHARSET_BIG5 = new Charset("Big5", MIB_BIG5);
  public static final Charset CHARSET_GB2312 = new Charset("GB2312", MIB_GB2312);
  public static final Charset CHARSET_GBK = new Charset("GBK", MIB_GBK);
  public static final Charset CHARSET_EUC_JP = new Charset("EUC-JP", MIB_EUC_JP);
  public static final Charset CHARSET_ISO_2022_JP = new Charset("ISO-2022-JP", MIB_ISO_2022_JP);
  public static final Charset CHARSET_UTF_8 = new Charset("UTF-8", MIB_UTF_8);
  public static final Charset CHARSET_UTF_16BE = new Charset("UTF-16BE", MIB_UTF_16BE);
  public static final Charset CHARSET_UTF_16LE = new Charset("UTF-16LE", MIB_UTF_16LE);
  public static final Charset CHARSET_UCS_2 = new Charset("ISO-10646-UCS-2", MIB_UCS_2);
  public static final Charset CHARSET_DEFAULT = new Charset(DEFAULT_PLATFORM_CHARSET);
  
  public static String makeString(byte[] b, int off, int len) {
    return CHARSET_DEFAULT.getString(b, off, len);
  }
  
  public static String makeString(byte[] b, int off, int len, String charset)
      throws UnsupportedEncodingException {
    Charset cs = Charset.getCharset(charset);
    
    if (cs == null) {
      return (new String(b, off, len, charset));
    }
    
    return (cs.getString(b, off, len));
  }
  
  public static byte[] makeBytes(String s) {
    return CHARSET_DEFAULT.getBytes(s);
  }
  
  public static byte[] makeBytes(String s, String charset) throws UnsupportedEncodingException {
    Charset cs = Charset.getCharset(charset);
    
    if (cs == null) {
      return (s.getBytes(charset));
    }
    
    return (cs.getBytes(s));
  }
  
  public static byte[] makeBytes(byte[] b, int off, int len, String fromCharset, String toCharset)
      throws UnsupportedEncodingException {
    return (makeBytes(makeString(b, off, len, fromCharset), toCharset));
  }
  
  public static Charset getCharset(int mib) {
    return (Charset) charsetsMapByMIB.get(new Integer(mib));
  }
  
  public static Charset getCharset(String iana) {
    Object o = charsetsMapByIANA.get(iana);
    if (o != null) {
      return (Charset) o;
    }
    o = charsetsMapByIANA.get(Utils.toASCIILowerCase(iana));
    if (o == null && iana.indexOf('_') != -1) {
      // we try to transform '_' into '-'
      return getCharset(iana.replace('_', '-'));
    }
    return (Charset) o;
  }
  
  /*********************************************************
   *	
   * Utility class that wraps a charset
   *
   *********************************************************/
  
  public Charset(String _name) {
    name = _name;
    mibValue = -1;
    init();
  }
  
  public Charset(String _name, int _mib) {
    name = _name;
    mibValue = _mib;
    init();
  }
  
  public String getName() {
    return name;
  }
  
  public int getMIBEnum() {
    return mibValue;
  }
  
  public byte[] getBytes(char[] chars) {
    try {
      String s = new String(chars);
      return this.name == DEFAULT_PLATFORM_CHARSET ? s.getBytes() : s.getBytes(this.name);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException("Can't encode chars using encoding " + this.name);
    }
  }
  
  public byte[] getBytes(String s) {
    try {
      return this.name == DEFAULT_PLATFORM_CHARSET ? s.getBytes() : s.getBytes(this.name);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException("Can't encode string \"" + s + "\" with encoding " + this.name);
    }
  }
  
  public char[] getChars(byte[] b, int offset, int length) {
    try {
      return this.name == DEFAULT_PLATFORM_CHARSET ? new String(b, offset, length).toCharArray()
          : new String(b, offset, length, this.name).toCharArray();
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Could not create string (offset=" + offset + ", length=" + length, e);
    }
  }
  
  public String getString(byte[] b, int offset, int length) {
    try {
      return this.name == DEFAULT_PLATFORM_CHARSET ? new String(b, offset, length) : new String(b, offset,
          length, this.name);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException("Can't create a String from bytes, using encoding " + this.name);
    }
  }
  
  private void init() {
    charsetsMapByMIB.put(new Integer(mibValue), this);
    charsetsMapByIANA.put(name, this);
    charsetsMapByIANA.put(Utils.toASCIIUpperCase(name), this);
    charsetsMapByIANA.put(Utils.toASCIILowerCase(name), this);
    valid = true;
  }
  
  private String name;
  private int mibValue;
  private boolean valid = false;
  
  /********************************************************************/
  
  // we handle synonyms
  private static void registerSynonym(String name, Charset charset) {
    if (charset.valid) {
      charsetsMapByIANA.put(name, charset);
      charsetsMapByIANA.put(Utils.toASCIIUpperCase(name), charset);
      charsetsMapByIANA.put(Utils.toASCIILowerCase(name), charset);
    }
  }
  
  // all the synonyms we can think of
  static {
    // US-ASCII
    registerSynonym("ASCII", CHARSET_US_ASCII);
    registerSynonym("ASCII7", CHARSET_US_ASCII);
    registerSynonym("ASCII-7", CHARSET_US_ASCII);
    registerSynonym("ISO646-US", CHARSET_US_ASCII);
    // ISO-8859-1
    registerSynonym("ISO-LATIN-1", CHARSET_ISO_8859_1);
    registerSynonym("LATIN1", CHARSET_ISO_8859_1);
    registerSynonym("LATIN-1", CHARSET_ISO_8859_1);
    registerSynonym("LATIN 1", CHARSET_ISO_8859_1);
    registerSynonym("8859_1", CHARSET_ISO_8859_1);
    registerSynonym("ISO_8859-1:1987", CHARSET_ISO_8859_1);
    registerSynonym("ISO-IR-100", CHARSET_ISO_8859_1);
    registerSynonym("ISO_8859-1", CHARSET_ISO_8859_1);
    registerSynonym("ISO8859-1", CHARSET_ISO_8859_1);
    registerSynonym("L1", CHARSET_ISO_8859_1);
    registerSynonym("IBM819", CHARSET_ISO_8859_1);
    registerSynonym("IBM-819", CHARSET_ISO_8859_1);
    registerSynonym("CP819", CHARSET_ISO_8859_1);
    registerSynonym("819", CHARSET_ISO_8859_1);
    registerSynonym("csISOLatin1", CHARSET_ISO_8859_1);
    // ISO-8859-2
    registerSynonym("8859_2", CHARSET_ISO_8859_2);
    registerSynonym("ISO_8859-2:1987", CHARSET_ISO_8859_2);
    registerSynonym("ISO-IR-101", CHARSET_ISO_8859_2);
    registerSynonym("ISO_8859-2", CHARSET_ISO_8859_2);
    registerSynonym("ISO8859-2", CHARSET_ISO_8859_2);
    registerSynonym("LATIN2", CHARSET_ISO_8859_2);
    registerSynonym("L2", CHARSET_ISO_8859_2);
    registerSynonym("IBM912", CHARSET_ISO_8859_2);
    registerSynonym("IBM-912", CHARSET_ISO_8859_2);
    registerSynonym("CP912", CHARSET_ISO_8859_2);
    registerSynonym("912", CHARSET_ISO_8859_2);
    registerSynonym("csISOLatin2", CHARSET_ISO_8859_2);
    // ISO-8859-3
    registerSynonym("8859_3", CHARSET_ISO_8859_3);
    registerSynonym("ISO_8859-3:1988", CHARSET_ISO_8859_3);
    registerSynonym("ISO-IR-109", CHARSET_ISO_8859_3);
    registerSynonym("ISO_8859-3", CHARSET_ISO_8859_3);
    registerSynonym("ISO8859-3", CHARSET_ISO_8859_3);
    registerSynonym("LATIN3", CHARSET_ISO_8859_3);
    registerSynonym("L3", CHARSET_ISO_8859_3);
    registerSynonym("IBM913", CHARSET_ISO_8859_3);
    registerSynonym("IBM-913", CHARSET_ISO_8859_3);
    registerSynonym("CP913", CHARSET_ISO_8859_3);
    registerSynonym("913", CHARSET_ISO_8859_3);
    registerSynonym("csISOLatin3", CHARSET_ISO_8859_3);
    // ISO-8859-4
    registerSynonym("8859_4", CHARSET_ISO_8859_4);
    registerSynonym("ISO_8859-4:1988", CHARSET_ISO_8859_4);
    registerSynonym("ISO-IR-110", CHARSET_ISO_8859_4);
    registerSynonym("ISO_8859-4", CHARSET_ISO_8859_4);
    registerSynonym("ISO8859-4", CHARSET_ISO_8859_4);
    registerSynonym("LATIN4", CHARSET_ISO_8859_4);
    registerSynonym("L4", CHARSET_ISO_8859_4);
    registerSynonym("IBM914", CHARSET_ISO_8859_4);
    registerSynonym("IBM-914", CHARSET_ISO_8859_4);
    registerSynonym("CP914", CHARSET_ISO_8859_4);
    registerSynonym("914", CHARSET_ISO_8859_4);
    registerSynonym("csISOLatin4", CHARSET_ISO_8859_4);
    // ISO-8859-5
    registerSynonym("8859_5", CHARSET_ISO_8859_5);
    registerSynonym("ISO_8859-5:1988", CHARSET_ISO_8859_5);
    registerSynonym("ISO-IR-144", CHARSET_ISO_8859_5);
    registerSynonym("ISO_8859-5", CHARSET_ISO_8859_5);
    registerSynonym("ISO8859-5", CHARSET_ISO_8859_5);
    registerSynonym("cyrillic", CHARSET_ISO_8859_5);
    registerSynonym("csISOLatinCyrillic", CHARSET_ISO_8859_5);
    registerSynonym("IBM915", CHARSET_ISO_8859_5);
    registerSynonym("IBM-915", CHARSET_ISO_8859_5);
    registerSynonym("CP915", CHARSET_ISO_8859_5);
    registerSynonym("915", CHARSET_ISO_8859_5);
    // ISO-8859-6
    registerSynonym("8859_6", CHARSET_ISO_8859_6);
    registerSynonym("ISO_8859-6:1987", CHARSET_ISO_8859_6);
    registerSynonym("ISO-IR-127", CHARSET_ISO_8859_6);
    registerSynonym("ISO_8859-6", CHARSET_ISO_8859_6);
    registerSynonym("ISO8859-6", CHARSET_ISO_8859_6);
    registerSynonym("ECMA-114", CHARSET_ISO_8859_6);
    registerSynonym("ASMO-708", CHARSET_ISO_8859_6);
    registerSynonym("arabic", CHARSET_ISO_8859_6);
    registerSynonym("csISOLatinArabic", CHARSET_ISO_8859_6);
    registerSynonym("IBM1089", CHARSET_ISO_8859_6);
    registerSynonym("IBM-1089", CHARSET_ISO_8859_6);
    registerSynonym("CP1089", CHARSET_ISO_8859_6);
    registerSynonym("1089", CHARSET_ISO_8859_6);
    // ISO-8859-7
    registerSynonym("8859_7", CHARSET_ISO_8859_7);
    registerSynonym("ISO_8859-7:1987", CHARSET_ISO_8859_7);
    registerSynonym("ISO-IR-126", CHARSET_ISO_8859_7);
    registerSynonym("ISO_8859-7", CHARSET_ISO_8859_7);
    registerSynonym("ISO8859-7", CHARSET_ISO_8859_7);
    registerSynonym("ELOT_928", CHARSET_ISO_8859_7);
    registerSynonym("ECMA-118", CHARSET_ISO_8859_7);
    registerSynonym("greek", CHARSET_ISO_8859_7);
    registerSynonym("greek8", CHARSET_ISO_8859_7);
    registerSynonym("csISOLatinGreek", CHARSET_ISO_8859_7);
    registerSynonym("IBM813", CHARSET_ISO_8859_7);
    registerSynonym("IBM-813", CHARSET_ISO_8859_7);
    registerSynonym("CP813", CHARSET_ISO_8859_7);
    registerSynonym("813", CHARSET_ISO_8859_7);
    // ISO-8859-8
    registerSynonym("8859_8", CHARSET_ISO_8859_8);
    registerSynonym("ISO_8859-8:1988", CHARSET_ISO_8859_8);
    registerSynonym("ISO-IR-138", CHARSET_ISO_8859_8);
    registerSynonym("ISO_8859-8", CHARSET_ISO_8859_8);
    registerSynonym("ISO8859-8", CHARSET_ISO_8859_8);
    registerSynonym("hebrew", CHARSET_ISO_8859_8);
    registerSynonym("csISOLatinHebrew", CHARSET_ISO_8859_8);
    registerSynonym("IBM916", CHARSET_ISO_8859_8);
    registerSynonym("IBM-916", CHARSET_ISO_8859_8);
    registerSynonym("CP916", CHARSET_ISO_8859_8);
    registerSynonym("916", CHARSET_ISO_8859_8);
    // ISO-8859-9
    registerSynonym("8859_9", CHARSET_ISO_8859_9);
    registerSynonym("ISO-IR-148", CHARSET_ISO_8859_9);
    registerSynonym("ISO_8859-9", CHARSET_ISO_8859_9);
    registerSynonym("ISO8859-9", CHARSET_ISO_8859_9);
    registerSynonym("LATIN5", CHARSET_ISO_8859_9);
    registerSynonym("L5", CHARSET_ISO_8859_9);
    registerSynonym("IBM920", CHARSET_ISO_8859_9);
    registerSynonym("IBM-920", CHARSET_ISO_8859_9);
    registerSynonym("CP920", CHARSET_ISO_8859_9);
    registerSynonym("920", CHARSET_ISO_8859_9);
    registerSynonym("csISOLatin5", CHARSET_ISO_8859_9);
    // UTF-8
    registerSynonym("UTF8", CHARSET_UTF_8);
    registerSynonym("unicode-1-1-utf-8", CHARSET_UTF_8);
    // UTF-16
    registerSynonym("UTF16BE", CHARSET_UTF_16BE);
    registerSynonym("UTF16LE", CHARSET_UTF_16LE);
    // Shift_JIS
    registerSynonym("Shift-JIS", CHARSET_SHIFT_JIS);
    registerSynonym("MS_Kanji", CHARSET_SHIFT_JIS);
    registerSynonym("csShiftJIS", CHARSET_SHIFT_JIS);
    registerSynonym("windows-31j", CHARSET_SHIFT_JIS);
    registerSynonym("cswindows31j", CHARSET_SHIFT_JIS);
    registerSynonym("x-sjis", CHARSET_SHIFT_JIS);
    // Big5
    registerSynonym("Big-5", CHARSET_BIG5);
    registerSynonym("csBig5", CHARSET_BIG5);
    // GB2312
    registerSynonym("GB-2312", CHARSET_GB2312);
    registerSynonym("csGB2312", CHARSET_GB2312);
    // EUC-JP
    registerSynonym("EUC_JP", CHARSET_EUC_JP);
    registerSynonym("EUCJP", CHARSET_EUC_JP);
    registerSynonym("csEUCPkdFmtJapanese", CHARSET_EUC_JP);
    registerSynonym("x-euc-jp", CHARSET_EUC_JP);
    registerSynonym("x-eucjp", CHARSET_EUC_JP);
    registerSynonym("Extended_UNIX_Code_Packed_Format_for_Japanese", CHARSET_EUC_JP);
    // ISO-2022-JP
    registerSynonym("csISO2022JP", CHARSET_ISO_2022_JP);
    // ISO-10646-UCS-2
    registerSynonym("csUnicode", CHARSET_UCS_2);
    registerSynonym("ucs2", CHARSET_UCS_2);
    registerSynonym("ucs-2", CHARSET_UCS_2);
    // GBK
    registerSynonym("cp936", CHARSET_GBK);
    registerSynonym("ms936", CHARSET_GBK);
    registerSynonym("Windows-936", CHARSET_GBK);
    registerSynonym("GB13000", CHARSET_GBK);
  }
  
  public static void main(String args[]) throws Exception {
    byte b[] = "this is a bloody testtest string".getBytes();
    
    String s = makeString(b, 0, b.length);
    System.out.println(s);
    
    Charset cs = Charset.getCharset("utf-8");
    
    long t1 = System.currentTimeMillis();
    
    int max = Integer.parseInt(args[0]);
    boolean testme = Boolean.valueOf(args[1]).booleanValue();
    String utf8 = "utf-8";
    s = "";
    
    for (int i = 0; i < 400; i++)
      s += String.valueOf(i);
    
    System.out.println(s.length());
    
    b = s.getBytes(utf8);
    
    for (int i = 0; i < max; i++) {
      if (testme) {
        s = cs.getString(b, 0, b.length);
      } else {
        s = new String(b, 0, b.length, utf8);
      }
    }
    
    long t2 = System.currentTimeMillis();
    System.out.println("time=" + (t2 - t1));
  }
}
