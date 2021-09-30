package alcatel.tess.hometop.gateways.concurrent;

// Jdk
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Class similar to BufferedOutputStream, but with a concurrent flush method.
 * The call to the "flush" method will notify an underlying thread and this one 
 * will write the internal buffer to the wrapped output stream asynchronously.
 * <b>Notice that this stream is not thread safe. If you need a thread safe version, please
 * decorate this stream with the SynchronizedOutputStream class.
 */
public class AsyncOutputStream extends FilterOutputStream implements Runnable {
  /**
   * Creates a new asynchronous output stream.
   *
   * @param out The underlying output stream to be written asynchronously.
   */
  public AsyncOutputStream(OutputStream out) {
    this(out, 8 * 1024);
  }
  
  /**
   * Creates a new asynchronous output stream.
   *
   * @param out The underlying output stream to be written asynchronously.. 
   * @param bufferSize The underlying buffer size used to transmit the bytes
   *		       towards the writing thread.
   */
  public AsyncOutputStream(OutputStream out, int bufferSize) {
    super(out);
    this.buffer = new byte[roundToPower(bufferSize)];
    this.oneByte = new byte[1];
    this.lock = new Object();
    this.consumer = new Thread(this);
    this.consumer.setPriority(Thread.MIN_PRIORITY);
    this.consumer.setDaemon(true);
    this.consumer.start();
    this.writable = buffer.length;
  }
  
  /**
   * Stores a byte into the internal buffer. If the buffer is full,
   * then the buffer is internally flushed to the underlying output stream.
   * The method is not thread safe, and the caller must ensure 
   * that no other thread may call this method concurrently.
   * Use the SynchronizedOutputStream class for thread safety.
   *
   * @param b the byte to be sent asynchronously.
   * @exception  IOException  if an I/O error occurs.
   */
  public void write(byte b) throws IOException {
    oneByte[0] = b;
    write(oneByte, 0, 1);
  }
  
  /**
   * <p> This method stores bytes from the given array into this
   * stream's buffer, flushing the buffer to the underlying output stream as
   * needed if the internal buffer becomes full. If the requested length is 
   * at least as large as this stream's buffer, however, then this method will 
   * flush the buffer and write the bytes synchronously to the underlying output stream.  
   * The method is not thread safe, and the caller must ensure 
   * that no other threads may call this method concurrently.
   * Use the SynchronizedOutputStream class for thread safety.
   *
   * @param data the byte to be sent asynchronously.
   * @param off the start offset in the data.
   * @param len the number of bytes to write.
   * @exception  IOException  if an I/O error occurs.
   */
  public void write(byte[] data, int off, int len) throws IOException {
    int bytesToWrite;
    byte[] buffer = this.buffer;
    int distToEnd, n;
    
    checkErr();
    
    // Optimisation: if the the data size is greater than the buffer size,
    // then write the data directly to the underlying stream.
    
    if (len > buffer.length) {
      if (pendingBytes > 0) {
        drain();
      }
      
      out.write(data, off, len);
      return;
    }
    
    do {
      // Wait for buffer space if this one is full.
      
      if (writable == 0) {
        // No more space, drain the whole buffer.
        drain();
      }
      
      // Copy the data to the buffer.
      
      writable -= (bytesToWrite = Math.min(writable, len));
      distToEnd = buffer.length - putPos;
      n = Math.min(bytesToWrite, distToEnd);
      
      System.arraycopy(data, off, buffer, putPos, n);
      
      if (n < bytesToWrite) {
        System.arraycopy(data, off + distToEnd, buffer, 0, bytesToWrite - distToEnd);
      }
      
      putPos = (putPos + bytesToWrite) & (buffer.length - 1);
      pendingBytes += bytesToWrite;
      
      off += bytesToWrite;
      len -= bytesToWrite;
    } while (len > 0);
  }
  
  /**
   * Flushes this buffered output stream asynchronously. 
   * The method is not thread safe, and the caller must ensure 
   * that no other threads may call this method concurrently.
   * Use the SynchronizedOutputStream class for thread safety.
   * @exception IOException  if an I/O error occurs.
   */
  public void flush() throws IOException {
    synchronized (lock) {
      readable += pendingBytes;
      writable = buffer.length - readable;
      lock.notify();
    }
    
    pendingBytes = 0;
  }
  
  /**
   * Close this stream. All pending data will be written to the
   * underlying output stream. 
   * The method is not thread safe, and the caller must ensure 
   * that no other threads may call this method concurrently.
   * Use the SynchronizedOutputStream class for thread safety.
   * @exception  IOException  if an I/O error occurs.
   */
  public void close() throws IOException {
    try {
      if (writable != buffer.length) {
        drain();
      }
    }
    
    finally {
      consumer.interrupt();
      
      try {
        consumer.join();
      } catch (InterruptedException e) {
      }
      
      out.close();
    }
  }
  
  /**
   * Runnable implementation: this is the writing thread method.
   */
  public void run() {
    int bytesToWrite = 0, bytesWritten = 0;
    byte[] buffer = this.buffer;
    
    try {
      while (true) {
        synchronized (lock) {
          readable -= bytesWritten;
          lock.notify();
          
          // Wait for any data.
          
          while ((bytesToWrite = readable) == 0) {
            lock.wait();
          }
        }
        
        int distToEnd = buffer.length - getPos;
        int n = Math.min(bytesToWrite, distToEnd);
        
        // Write the data to the underlying output stream.
        
        out.write(buffer, getPos, n);
        if (n < bytesToWrite) {
          if (bytesToWrite - n > buffer.length) {
            System.err.println("OOPS: bytesToWrite-n=" + String.valueOf(bytesToWrite - n));
          }
          out.write(buffer, 0, bytesToWrite - n);
        }
        
        getPos = (getPos + bytesToWrite) & (buffer.length - 1);
        bytesWritten = bytesToWrite;
      }
    }
    
    catch (InterruptedException e) {
      // closed
      return;
    }
    
    catch (Throwable t) {
      err = t;
    }
    
    finally {
      synchronized (lock) {
        readable = 0;
        lock.notifyAll(); // wakeup threads blocked in drain()
      }
    }
  }
  
  // ---------------------------------------------------------------------------
  //	Private part.
  // ---------------------------------------------------------------------------
  
  private void drain() throws IOException {
    try {
      synchronized (lock) {
        readable += pendingBytes;
        lock.notify();
        
        while (readable > 0) {
          lock.wait();
        }
      }
    }
    
    catch (InterruptedException e) {
    }
    
    finally {
      pendingBytes = 0;
      writable = buffer.length;
    }
    
    checkErr();
  }
  
  private final void checkErr() throws IOException {
    if (err != null) {
      if (err instanceof IOException)
        throw (IOException) err;
      else {
        IOException ioe = new IOException("Communication error");
        ioe.initCause(err);
        throw ioe;
      }
    }
  }
  
  private final int roundToPower(int n) {
    int p = 1;
    while (p < n)
      p <<= 1;
    return (p);
  }
  
  // ---------------------------------------------------------------------------
  //	Class attributes.
  // ---------------------------------------------------------------------------
  
  /** Buffer used to send one single byte asynchronously. */
  protected byte[] oneByte;
  
  /** The consumer that reads bytes from the buffer and write them to the underlying output stream. */
  protected Thread consumer;
  
  /** Lock used by write and run methods. */
  protected Object lock;
  
  /** Exception caught by the consumer thread. */
  protected volatile Throwable err;
  
  /** The internal buffer used to send bytes asynchronously. */
  protected byte[] buffer;
  
  /** The next buffer position where some data can be inserted. */
  protected int putPos;
  
  /** The next buffer position where some data can be read from. */
  protected int getPos;
  
  /** Tells how many bytes are available from the buffer. */
  protected int readable;
  
  /** Tells how many bytes may be written to the buffer. */
  protected int writable;
  
  /** Tells how many bytes are ready for being writen to the underlying stream. */
  protected int pendingBytes;
}
