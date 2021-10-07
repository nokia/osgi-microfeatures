package alcatel.tess.hometop.gateways.utils;

// Jdk
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class EmptyInputStream extends InputStream {
  public final static EmptyInputStream INSTANCE = new EmptyInputStream();
  
  public EmptyInputStream() {
  }
  
  public void close() {
  }
  
  public int read(byte b[], int off, int len) throws IOException {
    return -1;
  }
  
  public int read() throws IOException {
    return -1;
  }
  
  public int read(byte b[]) throws IOException {
    return -1;
  }
  
  public long skip(long n) throws IOException {
    return 0L;
  }
  
  public int available() throws IOException {
    throw new EOFException("Stream closed");
  }
}
