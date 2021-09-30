package alcatel.tess.hometop.gateways.utils;

// Jdk
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class ByteOutputStream extends OutputStream implements Recyclable {
  
  /**
   * Creates a new byte array output stream. The buffer capacity is 
   * initially 32 bytes, though its size increases if necessary. 
   */
  public ByteOutputStream() {
    this(32);
  }
  
  /**
   * Creates a new byte array output stream, with a buffer capacity of 
   * the specified size, in bytes. 
   *
   * @param   size   the initial size.
   * @exception  IllegalArgumentException if size is negative.
   */
  public ByteOutputStream(int size) {
    if (size < 0) {
      throw new IllegalArgumentException("Negative initial size: " + size);
    }
    if (size == 0) {
      buf = NULL_BUF;
    } else {
      buf = new byte[size];
    }
  }
  
  /**
   * Acquire a ByteOutputStream from a pool.
   */
  public static ByteOutputStream acquire(int initialSize) {
    ByteOutputStream out = new ByteOutputStream(initialSize);
    return out;
  }
  
  /**
   * Release this stream into the pool.
   */
  public void recycle() {
  }
  
  /**
   * Writes the complete contents of this byte array output stream to 
   * the specified output stream argument, as if by calling the output 
   * stream's write method using <code>out.write(buf, 0, count)</code>.
   *
   * @param      out   the output stream to which to write the data.
   * @exception  IOException  if an I/O error occurs.
   */
  public void writeTo(OutputStream out) throws IOException {
    out.write(buf, 0, count);
  }
  
  /**
   * Creates a newly allocated byte array. Its size is the current 
   * size of this output stream and the valid contents of the buffer 
   * have been copied into it. 
   *
   * @return  the current contents of this output stream, as a byte array.
   * @see     java.io.ByteOutputStream#size()
   */
  public byte[] toByteArray() {
    byte newbuf[] = new byte[count];
    System.arraycopy(buf, 0, newbuf, 0, count);
    return newbuf;
  }
  
  public byte[] toByteArray(boolean copy) {
    if (copy)
      return (toByteArray());
    
    return (this.buf);
  }
  
  /**
   * Returns the current size of the buffer.
   *
   * @return  the value of the <code>count</code> field, which is the number
   *          of valid bytes in this output stream.
   * @see     java.io.ByteOutputStream#count
   */
  public int size() {
    return count;
  }
  
  /**
   * Converts the buffer's contents into a string, translating bytes into
   * characters according to the platform's default character encoding.
   *
   * @return String translated from the buffer's contents.
   * @since   JDK1.1
   */
  public String toString() {
    return new String(buf, 0, count);
  }
  
  /**
   * Converts the buffer's contents into a string, translating bytes into
   * characters according to the specified character encoding.
   *
   * @param   enc  a character-encoding name.
   * @return String translated from the buffer's contents.
   * @throws UnsupportedEncodingException
   *         If the named encoding is not supported.
   * @since   JDK1.1
   */
  public String toString(String enc) throws UnsupportedEncodingException {
    return new String(buf, 0, count, enc);
  }
  
  /**
   * Creates a newly allocated string. Its size is the current size of 
   * the output stream and the valid contents of the buffer have been 
   * copied into it. Each character <i>c</i> in the resulting string is 
   * constructed from the corresponding element <i>b</i> in the byte 
   * array such that:
   * <blockquote><pre>
   *     c == (char)(((hibyte &amp; 0xff) &lt;&lt; 8) | (b &amp; 0xff))
   * </pre></blockquote>
   *
   * @deprecated This method does not properly convert bytes into characters.
   * As of JDK&nbsp;1.1, the preferred way to do this is via the
   * <code>toString(String enc)</code> method, which takes an encoding-name
   * argument, or the <code>toString()</code> method, which uses the
   * platform's default character encoding.
   *
   * @param      hibyte    the high byte of each resulting Unicode character.
   * @return     the current contents of the output stream, as a string.
   * @see        java.io.ByteOutputStream#size()
   * @see        java.io.ByteOutputStream#toString(String)
   * @see        java.io.ByteOutputStream#toString()
   */
  public String toString(int hibyte) {
    return new String(buf, hibyte, 0, count);
  }
  
  /**
   * Release the internal byte array, and reacquired a new one, unless the capacity is 0
   */
  public void reset(int capacity) {
    count = 0;
    if (capacity == 0) {
      buf = NULL_BUF;
    } else {
      buf = new byte[capacity];
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
    reset(0);
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
  //				OutputStream
  // -----------------------------------------------------------------------------------------------
  
  /**
   * Writes the specified byte to this byte array output stream. 
   *
   * @param   b   the byte to be written.
   */
  public void write(int b) {
    int newcount = count + 1;
    if (newcount > buf.length) {
      byte newbuf[] = new byte[Math.max(buf.length << 1, newcount)];
      System.arraycopy(buf, 0, newbuf, 0, count);
      buf = newbuf;
    }
    buf[count] = (byte) b;
    count = newcount;
  }
  
  /**
   * Writes <code>len</code> bytes from the specified byte array 
   * starting at offset <code>off</code> to this byte array output stream.
   *
   * @param   b     the data.
   * @param   off   the start offset in the data.
   * @param   len   the number of bytes to write.
   */
  public void write(byte b[], int off, int len) {
    if (len == 0) {
      return;
    }
    int newcount = count + len;
    if (newcount > buf.length) {
      byte newbuf[] = new byte[Math.max(buf.length << 1, newcount)];
      System.arraycopy(buf, 0, newbuf, 0, count);
      buf = newbuf;
    }
    System.arraycopy(b, off, buf, count, len);
    count = newcount;
  }
  
  /**
   * Resets the <code>count</code> field of this byte array output 
   * stream to zero, so that all currently accumulated output in the 
   * ouput stream is discarded. The output stream can be used again, 
   * reusing the already allocated buffer space. 
   *
   * @see     java.io.ByteArrayInputStream#count
   */
  public void reset() {
    reset(32);
  }
  
  /**
   * Closing a <tt>ByteOutputStream</tt> has no effect. The methods in
   * this class can be called after the stream has been closed without
   * generating an <tt>IOException</tt>.
   * <p>
   *
   */
  public void close() throws IOException {
    reset(0);
  }
  
  /** 
   * The buffer where data is stored. 
   */
  protected byte buf[];
  
  /**
   * The number of valid bytes in the buffer. 
   */
  protected int count;
  
  /**
   * Null buffer.
   */
  protected final static byte[] NULL_BUF = new byte[0];
  
}
