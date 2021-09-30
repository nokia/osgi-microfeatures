package alcatel.tess.hometop.gateways.utils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;

/**
 * The ByteBuffer and Bytes classes are similar to the java.lan.StringBuffer,
 * and manage byte arrays efficiently
 * 
 */
public class ByteBuffer extends InputStream implements Cloneable {
  /**
   * Constructor declaration
   */
  public ByteBuffer() {
    this(DEFAULT_INITIAL_SIZE);
  }
  
  /**
   * Constructor declaration
   *
   * @param initialSize
   */
  public ByteBuffer(int initialSize) {
    data = new byte[initialSize];
    length = 0;
  }
  
  /**
   * Constructor declaration
   *
   * @param data
   * @param length
   */
  public ByteBuffer(byte[] data, int length) {
    this.data = data;
    this.length = length;
  }
  
  /**
   * Constructor declaration
   *
   * @param data
   */
  public ByteBuffer(byte[] data) {
    this.data = data;
    this.length = data.length;
  }
  
  /**
   * Returns an InputStream that reads from the data.
   * <b>NOTE: the returned stream is not thread safe</b>.
   */
  public InputStream getInputStream() {
    currInStream = 0;
    return this;
  }
  
  /**
   * Returns an OutputStream that appends to the data.
   * <b>NOTE: the returned stream is not thread safe</b>.
   */
  public OutputStream getOutputStream() {
    // the os is instanciated when needed for the first time
    if (os == null)
      os = new OutputStreamWrapper();
    return os;
  }
  
  /**
   * Write this buffer to an output stream.
   */
  public void writeTo(OutputStream out) throws IOException {
    out.write(data, 0, length);
  }
  
  /**
   * Method declaration
   *
   * @param b
   */
  public void append(byte b) {
    expand(length + 1);
    
    data[length++] = b;
  }
  
  /**
   * Method declaration
   *
   * @param barray
   */
  public void append(byte[] barray) {
    if (barray == null)
      return;
    append(barray, 0, barray.length);
  }
  
  /**
   * Method declaration
   *
   * @param barray
   * @param offset
   * @param length
   */
  public void append(byte[] barray, int offset, int length) {
    if (barray == null) {
      return;
    }
    
    expand(this.length + length);
    
    System.arraycopy(barray, offset, this.data, this.length, length);
    
    this.length += length;
  }
  
  /**
   * Method declaration
   */
  public void trim() {
    if (data.length != this.length) {
      byte[] data = new byte[this.length];
      
      System.arraycopy(this.data, 0, data, 0, this.length);
      this.data = data;
    }
  }
  
  /**
   * Same as shiftLeft (0, len)
   */
  public void shiftLeft(int len) {
    shiftLeft(0, len);
  }
  
  /**
   * Removes the segment [offset, offset+len] by shiftling left the following segment (which goes from offset+len to the end of the data).
   * No change is performed on the InputStream index which may be corrupted afterwards.
   */
  public void shiftLeft(int offset, int len) {
    System.arraycopy(data, offset + len, data, offset, length - offset - len);
    length -= len;
  }
  
  /**
   * Method declaration
   * @return
   */
  public int size() {
    return (length);
  }
  
  /**
   * Method declaration
   * @return
   */
  public void setSize(int size) {
    this.length = size;
  }
  
  /**
   * Method declaration
   * @return
   */
  public int bufferSize() {
    return (data.length);
  }
  
  /**
   * Method declaration
   */
  public void init() {
    length = 0;
    currInStream = 0;
    closed = false;
  }
  
  /**
   * Method declaration
   *
   * @param data
   * @param length
   */
  public void init(byte[] data, int offset, int length, boolean copy) {
    if (offset != 0)
      copy = true;
    if (copy) {
      this.length = 0; // useful if expand makes an arraycopy
      expand(length);
      System.arraycopy(data, offset, this.data, 0, length);
    } else {
      // we know offset == 0
      this.data = data;
    }
    this.length = length;
    currInStream = 0;
    closed = false;
  }
  
  /**
   * Method declaration
   *
   * @param length
   */
  public void init(int newLength) {
    if (this.data.length != newLength) {
      this.data = new byte[newLength];
    }
    this.length = 0;
    currInStream = 0;
    closed = false;
  }
  
  /**
   * Method declaration
   *
   * @param in
   *
   * @throws IOException
   */
  public int append(InputStream in) throws IOException {
    int bytesRead = 0;
    int n = 0;
    
    try {
      while (true) {
        
        // we make sure we have room for at least 1 byte
        expand(this.length + 1);
        
        n = in.read(this.data, this.length, this.data.length - this.length);
        
        // Test if we have reach end of stream.
        if (n == -1) {
          break;
        }
        
        length += n;
        bytesRead += n;
      }
    }
    
    catch (InterruptedIOException e) {
      length += e.bytesTransferred;
      bytesRead += e.bytesTransferred;
      e.bytesTransferred = bytesRead;
      throw e;
    }
    
    return (bytesRead);
  }
  
  /**
   * Method declaration
   *
   * @param in
   * @param maxSize
   *
   * @throws IOException
   */
  public int append(InputStream in, int maxsize) throws IOException {
    return append(in, maxsize, true, false);
  }
  
  public int append(InputStream in, int maxsize, boolean doBlock, boolean notifyEOF) throws IOException {
    if (maxsize == 0)
      // avoid useless blocking
      return 0;
    
    expand(this.length + maxsize);
    
    int bytesRead = 0;
    int n = 0;
    
    try {
      do {
        if ((n = in.read(this.data, this.length + bytesRead, maxsize - bytesRead)) == -1) {
          if (notifyEOF)
            return -1;
          else
            break;
        }
        
        bytesRead += n;
      } while (doBlock && bytesRead < maxsize);
    }
    
    catch (InterruptedIOException e) {
      bytesRead += e.bytesTransferred;
      e.bytesTransferred = bytesRead;
      throw e;
    }
    
    finally {
      this.length += bytesRead;
    }
    
    return (bytesRead);
  }
  
  /**
   * Method declaration
   * @return
   */
  public byte[] toByteArray(boolean copy) {
    if (copy) {
      byte[] buf = new byte[length];
      System.arraycopy(data, 0, buf, 0, length);
      return buf;
    } else {
      return data;
    }
  }
  
  public byte byteAt(int index) {
    return (data[index]);
  }
  
  /**
   * Method declaration
   * @return
   */
  public String toString() {
    return (Utils.dumpByteArray("", data, length));
  }
  
  public int read() throws IOException {
    if (currInStream == length)
      return (-1);
    
    return ((int) data[currInStream++] & 0xff);
  }
  
  public int read(byte b[], int off, int len) throws IOException {
    if (len == 0)
      return (0);
    
    len = Math.min(len, length - currInStream);
    
    if (len == 0) {
      return (-1);
    }
    
    System.arraycopy(data, currInStream, b, off, len);
    currInStream += len;
    return (len);
  }
  
  public int available() throws IOException {
    int avail = length - currInStream;
    
    if (avail <= 0 && closed == true) {
      throw new EOFException("Stream closed");
    }
    
    return (avail);
  }
  
  public void close() {
    close(false);
  }
  
  /**
   * Close the input stream, possibly aborting all not yet read data.
   * @param flush true if data not yet read must be flushed, false if not
   **/
  public void close(boolean flush) {
    closed = true;
    
    if (flush)
      currInStream = length;
  }
  
  public void mark(int readlimit) {
    mark = currInStream;
  }
  
  public void reset() throws IOException {
    currInStream = mark;
  }
  
  public boolean markSupported() {
    return (true);
  }
  
  /**
   * Returns the index of the next byte read by the InputStream.
   */
  public int position() {
    return currInStream;
  }
  
  public static void main(String args[]) throws Exception {
    byte[] tmp = new byte[] { (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd' };
    
    ByteBuffer buf = new ByteBuffer(tmp);
    int c;
    for (int i = 0; i < 10; i++) {
      if ((c = buf.read()) != -1) {
        System.out.println((char) c);
        buf.append((byte) c);
      }
    }
    buf.close();
  }
  
  /**
   * Method declaration
   * @return
   *
   * @throws CloneNotSupportedException
   */
  protected Object clone() throws CloneNotSupportedException {
    ByteBuffer copy = (ByteBuffer) super.clone();
    
    copy.data = new byte[data.length]; // keep length (trim might have been called)
    
    System.arraycopy(data, 0, copy.data, 0, length);
    
    return (copy);
  }
  
  /**
   * We guarantee that the required size will be available.
   * The current implementation takes a 50% margin for future needs.
   */
  private void expand(int len) {
    if (this.data.length >= len)
      return;
    
    int newLength = len + (len / 2);
    byte[] newData = new byte[newLength];
    System.arraycopy(this.data, 0, newData, 0, this.length);
    this.data = newData;
  }
  
  private class OutputStreamWrapper extends OutputStream {
    
    OutputStreamWrapper() {
    }
    
    public void write(int b) {
      append((byte) b);
    }
    
    public void write(byte[] b) {
      write(b, 0, b.length);
    }
    
    public void write(byte[] b, int off, int len) {
      append(b, off, len);
    }
  }
  
  private final static int DEFAULT_INITIAL_SIZE = 16;
  private byte[] data;
  private int length;
  private int currInStream;
  private int mark;
  private OutputStream os;
  private boolean closed;
}
