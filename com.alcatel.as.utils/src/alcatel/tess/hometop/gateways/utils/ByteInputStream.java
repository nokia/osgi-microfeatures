package alcatel.tess.hometop.gateways.utils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Same as ByteArrayInputStream except that this class is not synchronized and
 * there is an init method which may be used to reset the internal buffer.
 *
 */
public class ByteInputStream extends InputStream {
  public ByteInputStream() {
  }
  
  public ByteInputStream(byte[] buf) {
    this(buf, 0, buf.length);
  }
  
  public ByteInputStream(byte[] buf, int offset, int length) {
    init(buf, offset, length);
  }
  
  public void init(byte[] buf, int offset, int length) {
    init(buf, offset, length, false);
  }
  
  public void init(byte[] buf, int offset, int length, boolean copy) {
    closed = false;
    byte[] b = buf;
    
    if (copy) {
      b = new byte[length];
      System.arraycopy(buf, offset, b, 0, length);
      offset = 0;
    }
    
    this.buf = b;
    this.pos = offset;
    this.count = Math.min(offset + length, b.length);
    this.mark = offset;
  }
  
  public int read() {
    return (pos < count) ? (buf[pos++] & 0xff) : -1;
  }
  
  public int read(byte b[], int off, int len) {
    if (pos >= count) {
      return -1;
    }
    if (pos + len > count) {
      len = count - pos;
    }
    if (len <= 0) {
      return 0;
    }
    System.arraycopy(buf, pos, b, off, len);
    pos += len;
    return len;
  }
  
  public byte[] getBytes() {
    return (buf);
  }
  
  public int getPos() {
    return (pos);
  }
  
  public long skip(long n) throws IOException {
    if (pos + n > count) {
      n = count - pos;
    }
    
    if (n < 0) {
      return 0;
    }
    
    pos += n;
    return n;
  }
  
  public int available() throws IOException {
    int avail = count - pos;
    
    // Signal close only if all bytes have been consumed.
    
    if (avail <= 0 && closed == true) {
      throw new EOFException("Stream closed");
    }
    
    return (avail);
  }
  
  public boolean markSupported() {
    return true;
  }
  
  public void mark(int readAheadLimit) {
    mark = pos;
  }
  
  public void reset() {
    pos = mark;
  }
  
  public void close() {
    close(false);
  }
  
  public void close(boolean flush) {
    closed = true;
    
    if (flush)
      pos = count = 0;
  }
  
  protected byte buf[];
  protected int pos;
  protected int mark;
  protected int count;
  protected boolean closed;
}
