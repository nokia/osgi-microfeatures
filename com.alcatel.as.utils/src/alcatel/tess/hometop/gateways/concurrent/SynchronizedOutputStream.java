package alcatel.tess.hometop.gateways.concurrent;

// Jdk
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Class used to synchronize a thread unsafe outputstream.
 */
public class SynchronizedOutputStream extends FilterOutputStream {
  /**
   * Creates synchronized output stream filter built on top of the specified 
   * underlying output stream. 
   *
   * @param   out   the underlying thread unsafe output stream.
   */
  public SynchronizedOutputStream(OutputStream out) {
    super(out);
  }
  
  /**
   * Writes thread-safely the specified <code>byte</code> to this output stream. 
   * @param      b   the <code>byte</code>.
   * @exception  IOException  if an I/O error occurs.
   */
  public synchronized void write(byte b) throws IOException {
    out.write(b);
  }
  
  /**
   * Writes thread-safely the specified <code>bytes</code> to this output stream. 
   * @param      data     the data.
   * @param      off   the start offset in the data.
   * @param      len   the number of bytes to write.
   * @exception  IOException  if an I/O error occurs.
   */
  public synchronized void write(byte[] data, int off, int len) throws IOException {
    out.write(data, off, len);
  }
  
  /**
   * Flushes thread-safely this output stream and forces any buffered output bytes 
   * to be written out to the stream. 
   *
   * @exception  IOException  if an I/O error occurs.
   * @see        java.io.FilterOutputStream#out
   */
  public synchronized void flush() throws IOException {
    out.flush();
  }
  
  /**
   * Closes thread-safely this output stream and releases any system resources 
   * associated with the stream. 
   *
   * @exception  IOException  if an I/O error occurs.
   */
  public synchronized void close() throws IOException {
    out.close();
  }
}
