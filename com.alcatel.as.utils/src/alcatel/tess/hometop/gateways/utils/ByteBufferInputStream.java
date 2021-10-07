package alcatel.tess.hometop.gateways.utils;

import java.io.IOException;
import java.io.InputStream;

public class ByteBufferInputStream extends InputStream {
  private java.nio.ByteBuffer _buffer;
  
  public ByteBufferInputStream(java.nio.ByteBuffer buf) {
    _buffer = buf;
  }
  
  public synchronized int read() throws IOException {
    if (!_buffer.hasRemaining()) {
      return -1;
    }
    return (_buffer.get() & 0XFF);
  }
  
  public synchronized int read(byte[] bytes, int off, int len) throws IOException {
    // Read only what's left
    len = Math.min(len, _buffer.remaining());
    _buffer.get(bytes, off, len);
    return len;
  }
  
  public synchronized int available() throws IOException {
    return _buffer.remaining();
  }
}
