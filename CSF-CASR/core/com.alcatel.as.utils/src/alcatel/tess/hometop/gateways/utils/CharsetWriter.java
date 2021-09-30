package alcatel.tess.hometop.gateways.utils;

// Jdk
import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

/**
 * This class may be used to convert java string to bytes using specified encoding.
 */
public class CharsetWriter extends OutputStreamWriter implements Recyclable {
  
  public static CharsetWriter acquire(OutputStream out, String encoding) throws UnsupportedEncodingException {
    ObjectPool p = (ObjectPool) pools.getObject(encoding);
    if (p == null) {
      throw new UnsupportedEncodingException("Encoding " + encoding + " not supported");
    }
    
    CharsetWriter writer = (CharsetWriter) p.acquire();
    writer.setOutputStream(out);
    return writer;
  }
  
  public void recycle() {
    ObjectPool p = (ObjectPool) pools.getObject(getEncoding());
    if (p != null) {
      p.release(this);
    }
  }
  
  // -----------------------------------------------------------------------------------------------
  //				Recyclable
  // -----------------------------------------------------------------------------------------------
  
  /**
   * Recycle a object into the pool. This method is called by the 
   * ObjectPool when this object come back to the pool
   */
  
  public void recycled() {
    wrapper.setOutputStream(null);
  }
  
  /**
   * Is this object in a valid State ? This method may be called at any
   * time by the ObjectPool class to check if this object is currently 
   * in a consistent state.
   *
   * @return Always true.
   */
  public boolean isValid() {
    return true;
  }
  
  // -----------------------------------------------------------------------------------------------
  //				Private methods
  // -----------------------------------------------------------------------------------------------
  
  private CharsetWriter(String encoding) throws UnsupportedEncodingException {
    this(new OutputStreamWrapper(), encoding);
  }
  
  private CharsetWriter(OutputStreamWrapper wrapper, String encoding) throws UnsupportedEncodingException {
    super(wrapper, encoding);
    this.wrapper = wrapper;
  }
  
  private void setOutputStream(OutputStream out) {
    this.wrapper.setOutputStream(out);
  }
  
  private static void registerWriter(final String encoding, String[] synonyms) {
    ObjectPool pool = new ObjectPool(new ObjectPoolFactory() {
      public Recyclable newInstance() {
        try {
          return new CharsetWriter(encoding);
        } catch (UnsupportedEncodingException e) {
          throw new RuntimeException(e);
        }
      }
    });
    
    pools.putObject(encoding, pool);
    for (int i = 0; i < synonyms.length; i++) {
      pools.putObject(synonyms[i], pool);
    }
  }
  
  // -----------------------------------------------------------------------------------------------
  //				Private attributes
  // -----------------------------------------------------------------------------------------------
  
  /** Output Stream where string are encoded. */
  private OutputStreamWrapper wrapper;
  
  /** List of well known charsets. */
  private static StringCaseHashtable pools = new StringCaseHashtable();
  
  /** Static initializer. */
  static {
    registerWriter("US-ASCII", new String[] { "ASCII", "ASCII7", "ASCII-7", "ISO646-US" });
    
    registerWriter("ISO-8859-1", new String[] { "ISO-LATIN-1", "LATIN1", "LATIN-1", "LATIN 1", "8859_1",
        "ISO_8859-1:1987", "ISO-IR-100", "ISO_8859-1", "ISO8859-1", "L1", "IBM819", "IBM-819", "CP819",
        "819", "csISOLatin1" });
    
    registerWriter("ISO-8859-2", new String[] { "8859_2", "ISO_8859-2:1987", "ISO-IR-101", "ISO_8859-2",
        "ISO8859-2", "LATIN2", "L2", "IBM912", "IBM-912", "CP912", "912", "csISOLatin2" });
    
    registerWriter("ISO-8859-3", new String[] { "8859_3", "ISO_8859-3:1988", "ISO-IR-109", "ISO_8859-3",
        "ISO8859-3", "LATIN3", "L3", "IBM913", "IBM-913", "CP913", "913", "csISOLatin3" });
    
    registerWriter("ISO-8859-4", new String[] { "8859_4", "ISO_8859-4:1988", "ISO-IR-110", "ISO_8859-4",
        "ISO8859-4", "LATIN4", "L4", "IBM914", "IBM-914", "CP914", "914", "csISOLatin4" });
    
    registerWriter("ISO-8859-5", new String[] { "8859_5", "ISO_8859-5:1988", "ISO-IR-144", "ISO_8859-5",
        "ISO8859-5", "cyrillic", "csISOLatinCyrillic", "IBM915", "IBM-915", "CP915", "915" });
    
    registerWriter("ISO-8859-6", new String[] { "8859_6", "ISO_8859-6:1987", "ISO-IR-127", "ISO_8859-6",
        "ISO8859-6", "ECMA-114", "ASMO-708", "arabic", "csISOLatinArabic", "IBM1089", "IBM-1089", "CP1089",
        "1089" });
    
    registerWriter("ISO-8859-7", new String[] { "8859_7", "ISO_8859-7:1987", "ISO-IR-126", "ISO_8859-7",
        "ISO8859-7", "ELOT_928", "ECMA-118", "greek", "greek8", "csISOLatinGreek", "IBM813", "IBM-813",
        "CP813", "813" });
    
    registerWriter("ISO-8859-8", new String[] { "8859_8", "ISO_8859-8:1988", "ISO-IR-138", "ISO_8859-8",
        "ISO8859-8", "hebrew", "csISOLatinHebrew", "IBM916", "IBM-916", "CP916", "916" });
    
    registerWriter("ISO-8859-9", new String[] { "8859_9", "ISO-IR-148", "ISO_8859-9", "ISO8859-9", "LATIN5",
        "L5", "IBM920", "IBM-920", "CP920", "920", "csISOLatin5" });
    
    registerWriter("Shift_JIS", new String[] { "Shift-JIS", "MS_Kanji", "csShiftJIS", "windows-31j",
        "cswindows31j", "x-sjis" });
    
    registerWriter("Big5", new String[] { "Big-5", "csBig5" });
    
    registerWriter("GB2312", new String[] { "GB-2312", "csGB2312" });
    
    registerWriter("GBK", new String[] { "cp936", "ms936", "Windows-936", "GB13000" });
    
    registerWriter("EUC-JP", new String[] { "EUC_JP", "EUCJP", "csEUCPkdFmtJapanese", "x-euc-jp", "x-eucjp",
        "Extended_UNIX_Code_Packed_Format_for_Japanese" });
    
    registerWriter("ISO-2022-JP", new String[] { "csISO2022JP" });
    
    registerWriter("UTF-8", new String[] { "UTF8", "unicode-1-1-utf-8" });
    
    registerWriter("UTF-16BE", new String[] { "UTF16BE" });
    
    registerWriter("UTF-16LE", new String[] { "UTF16LE" });
    
    registerWriter("ISO-10646-UCS-2", new String[] { "csUnicode", "ucs2", "ucs-2" });
  }
  
  // -----------------------------------------------------------------------------------------------
  //				Iner classe used to wrapped underlying output stream.
  // -----------------------------------------------------------------------------------------------
  
  private static class OutputStreamWrapper extends FilterOutputStream {
    OutputStreamWrapper() {
      super(null);
    }
    
    void setOutputStream(OutputStream out) {
      this.out = out;
    }
  }
  
  public static void main(String args[]) throws Exception {
    //     ByteOutputStream out = ByteOutputStream.acquire(1024);
    //     CharsetWriter writer = CharsetWriter.acquire(out, "ASCII");
    //     writer.write("foo");
    //     writer.flush();
    //     System.out.println(writer + ":" + Utils.toString(out.toByteArray()));
    //     out.recycle();
    //     writer.recycle();
    
    //     out = ByteOutputStream.acquire(1024);
    //     writer = CharsetWriter.acquire(out, "ASCII");
    //     writer.write("foo");
    //     writer.flush();
    //     System.out.println(writer + ":" + Utils.toString(out.toByteArray()));
    //     out.recycle();
    //     writer.recycle();
  }
}
