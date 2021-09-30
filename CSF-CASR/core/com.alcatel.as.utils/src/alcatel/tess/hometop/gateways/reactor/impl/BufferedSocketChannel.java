package alcatel.tess.hometop.gateways.reactor.impl;

// Jdk
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

public class BufferedSocketChannel {
  /**
   * Constant used to check if bueffered bytes exceeds a given limit
   */
  private final static int ONE_MB = 1024 * 1024;
  
  /**
   * Last time we have logged a warn message, when send buffers queue is full.
   */
  private long _lastBuffWarnTime;
  
  /**
   * This socket local address.
   */
  private final InetSocketAddress _local;
  
  /**
   * This socket remote address.
   */
  private final InetSocketAddress _remote;
  
  /**
   * This socket channel
   */
  private final SocketChannel _channel;
  
  /**
   * List of buffers to be sent out. The element is either
   * a ByteBuffer or a ByteBuffer[]
   */
  private final ConcurrentLinkedQueue<Object> _queue = new ConcurrentLinkedQueue<Object>();
  
  /**
   * Our socket logger.
   */
  private final Logger _logger;
    
  /**
   * count of bytes buffered in our _queue
   */
  private final AtomicInteger _bufferedBytes = new AtomicInteger();
  
  /**
   * Close flag
   */
  private final AtomicBoolean _closed = new AtomicBoolean(); 
    
  /**
   * We maitain some send buffers in the thread local, in order to avoid allocating one sendbuf for each socket.
   */
  private final static ThreadLocal<ByteBuffer> _threadLocalSendBuffer = new ThreadLocal<ByteBuffer>() {
    protected ByteBuffer initialValue() {
      return ByteBuffer.allocateDirect(Helpers.getSndBufSize());
    }
  };
  
  /**
   * Remaining bytes which could not be written during last write attempt.
   */
  private byte[] _writeFailedSendBuf;

  /**
   * Our Reactor Metrics.
   */
  private final Meters.ReactorMeters.TcpMeters _tcpMeters;
  
  /**
   * Constructor.
   * 
   * @param channel The nio socket channel
   * @param size the buffer size (we'll poll/write if the buffer is not full)
   * @param local the socket local address
   * @param remote the socket remote address
   * @param reactor the reactor managing this socket
   * @param logger 
   */
  public BufferedSocketChannel(SocketChannel channel, InetSocketAddress local, InetSocketAddress remote,
                               ReactorImpl reactor, Logger logger, Meters.ReactorMeters.TcpMeters tcpMeters) {
    _local = local;
    _remote = remote;
    _channel = channel;
    _logger = logger;
    _tcpMeters = tcpMeters;
  }
  
  /**
   * Returns false if write interest must be activated on the output socket.
   * @param data 
   * @param off 
   * @param len 
   * @param copy 
   */
  public int write(byte[] data, int off, int len, boolean copy) {
    return write(ByteBuffer.wrap(data, off, len), copy);
  }
  
  /**
   * Returns true if message has been sent, false if socket can't be written or if msg has been buffered
   * @param buf 
   * @param copy 
   */
  public int write(ByteBuffer buf, boolean copy) {
    return enqueue(buf, copy);
  }
  
  /**
   * Returns true if bufs have been sent, false if socket is full or if bugs have been buffered.
   * @param bufs 
   * @param copy 
   */
  public int write(ByteBuffer[] bufs, boolean copy) {
    return enqueue(bufs, copy);
  }
  
  public boolean flush() throws IOException {
    // Fill our sndbuf with some bytes found from our concurrent send queue.
    ByteBuffer sendBuf = getSendBuffer();
    
    try {
      Object o;
      while ((o = _queue.peek()) != null) {
        // Fill our sndbuf with some buffers found from our concurrent send queue.
        if (sendBuf.hasRemaining()) {
          if (o instanceof ByteBuffer) {
            ByteBuffer bb = (ByteBuffer) o;
            int n = Math.min(sendBuf.remaining(), bb.remaining());
            Helpers.copyTo(bb, sendBuf, n);
            if (!bb.hasRemaining()) {
              // the buffer has been fully copied into our sndbuf: remove it from sendqueue
              _queue.poll();
              // keep filling our sndbuf with more buffers from the sendqueue
              continue;
            }
          } else {
            ByteBuffer[] buffers = (ByteBuffer[]) o;
            for (int i = 0; i < buffers.length && sendBuf.hasRemaining(); i++) {
              ByteBuffer bb = buffers[i];
              int n = Math.min(sendBuf.remaining(), bb.remaining());
              Helpers.copyTo(bb, sendBuf, n);
            }
            if (!buffers[buffers.length - 1].hasRemaining()) {
              // all buffers in the array have been fully copied into our sndbuf: remove them from the queue
              _queue.poll();
              // keep filling our sndbuf with more buffers from the sendqueue
              continue;
            }
          }
        }
        
        // At this point, out sndbuf is full: flush it
        if (!flushSendBuf(false, sendBuf)) {
          return false; // socket full
        }
      }
      
      return flushSendBuf(true, sendBuf);
    } 
    
    finally {
      // We must clear the send buffer, which may be reused for another channel.
      sendBuf.clear();
    }
  }
  
  private boolean flushSendBuf(boolean flushFullBuffer, ByteBuffer sendBuf) throws IOException {
    sendBuf.flip();
    while (sendBuf.hasRemaining()) {
      ByteBuffer log = _logger.isInfoEnabled() ? sendBuf.duplicate() : null;
      int sent = _channel.write(sendBuf);
      if (sent > 0) {
        if (log != null) {
          Helpers.logPacketSent(_logger, true, log, sent, getAddress());
        }
        _tcpMeters.tcpWriteBytes(sent);
        _tcpMeters.tcpWrite();
        buffered(-sent);
        _lastBuffWarnTime = 0;
        
        if (!flushFullBuffer && sendBuf.hasRemaining()) {
          // socket not full, but we did not empty our send buffer. compact the sendbuf so we'll possibly
          // fill it with more application message bytes.
          sendBuf.compact();
          return true; 
        }
      } else if (sent == 0) {
        sendFailed(sendBuf); // We have to bufferize the remaining bytes which could not be sent
        if (_bufferedBytes.get() > ONE_MB) {
          long now = System.currentTimeMillis();
          if (_lastBuffWarnTime == 0) {
            _lastBuffWarnTime = now;
          } else if (now - _lastBuffWarnTime > 5000L) {
            _lastBuffWarnTime = now;
            _logger.warn("send buffer full on " + getAddress() + " (bytes=" + _bufferedBytes + ")");
          }
        }
        return false; // socket full;
      } else {
        throw new IOException("socket closed");
      }
    }
    sendBuf.clear();
    return true;
  }
  
  /**
   * Is their some remaining bytes to be written out ?
   * @return true if there are still some bytes to be sent out
   */
  public boolean hasRemaining() {
    return _bufferedBytes.get() > 0;
  }
  
  public int remaining() {
    return _bufferedBytes.get();
  }
  
  /**
   * Shutdown this buffered socket.
   */
  public void close(int lingerSecs) {
	  if (_closed.compareAndSet(false, true)) {
		  try {
			  if (_channel != null) {
				  if (lingerSecs != -1) {
					  _channel.socket().setSoLinger(true, lingerSecs);
				  }
				  _channel.close();
			  }
		  } catch (Throwable e) {
		  }
	  }
	  
	  // cleanup queues, and metering counters
	  _queue.clear();
	  int buffered = _bufferedBytes.getAndSet(0);
	  _tcpMeters.tcpBuffered(-buffered);
	  _writeFailedSendBuf = null;
  }
  
  /**
   * Append some bytes in our buffered socket. The bytes are not sent.
   */
  private int enqueue(ByteBuffer buf, boolean copy) {
    if (buf.hasRemaining()) {
      if (copy) {
        buf = Helpers.copy(buf);
      }
      // We must increase the queue size before enqueing (if not the writer thread may decrease the queue size before we increase it).
      int buffered = buffered(buf.remaining());
      _queue.add(buf);
      return buffered;      
    } else {
      return _bufferedBytes.get();
    }
  }
  
  /**
   * Append some bytes in our buffered socket. The bytes are not sent.
   */
  private int enqueue(ByteBuffer[] bufs, boolean copy) {
    int bufSize = 0;
    ByteBuffer[] array = copy ? new ByteBuffer[bufs.length] : bufs;
    for (int i = 0; i < bufs.length; i++) {
      if (copy) {
        array[i] = Helpers.copy(bufs[i]);
      }
      bufSize += array[i].remaining();
    }

    if (bufSize > 0) {
      // We must increase the queue size before enqueing (if not the writer thread may decrease the queue size before we increase it).
      int buffered = buffered(bufSize);
      _queue.add(array);
      return buffered;
    } else {
      return _bufferedBytes.get();
    }
  }
  
  private String getAddress() {
    StringBuilder sb = new StringBuilder();
    sb.append("TcpChannel [local=");
    sb.append(_local);
    sb.append(",remote=");
    sb.append(_remote);
    sb.append("]");
    return sb.toString();
  }
  
  private ByteBuffer getSendBuffer() {
    // Return a send buffer from thread local. If the previous send buffer could not be fully flushed,
    // we have to copy the previous remaining (unsent) bytes to our send buffer.
    ByteBuffer buf = _threadLocalSendBuffer.get();
    if (_writeFailedSendBuf != null) {
      buf.put(_writeFailedSendBuf);
      _writeFailedSendBuf = null;
    }
    return buf;
  }
  
  private void sendFailed(ByteBuffer buf) {
    // The messages buffer could not be fully sent: we have to backup the remaining unsent bytes, which we'll send
    // later.
    _writeFailedSendBuf = new byte[buf.remaining()];
    buf.get(_writeFailedSendBuf);
  }
  
  private int buffered(int bytes) {
    _tcpMeters.tcpBuffered(bytes);
    return _bufferedBytes.addAndGet(bytes);
  }  
}
