package alcatel.tess.hometop.gateways.reactor.impl;

// Jdk
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLEngine;

import org.apache.log4j.Level;

import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.metering2.StopWatch;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.Security;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpChannelListener;
import alcatel.tess.hometop.gateways.reactor.util.DataBuffer;
import alcatel.tess.hometop.gateways.utils.Log;

/**
 * Reactor Tcp Channel.
 */
public class TcpChannelImpl implements TcpChannel, SelectHandler {
  protected final TcpChannelListener _listener;
  protected final SelectionKey _key;
  protected final ReactorImpl _reactor;
  protected final SocketChannel _socket;
  private final AtomicBoolean _closed = new AtomicBoolean(false);
  private final static Log _logger = Log.getLogger("as.service.reactor.TcpChannelImpl");
  protected volatile long _lastIOTime;
  protected volatile Executor _queue; // executor used for input/output
  protected volatile Future<?> _inactivityTimer;
  protected final boolean _directBuffer;

  private final ReadInterestController _readInterestController;
  private final InetSocketAddress _localAddress;
  private final InetSocketAddress _remoteAddress;
  private final BufferedSocketChannel _bufferedSocket;
  private final AtomicBoolean _writeBlocked = new AtomicBoolean();
  private final AtomicBoolean _writeInterestScheduled = new AtomicBoolean();
  private final AtomicBoolean _aborted = new AtomicBoolean();
  private final NioSelector _selector;
  private volatile WriteBlockedPolicy _writeBlockedPolicy = WriteBlockedPolicy.NOTIFY;
  private volatile Object _attached;
  private volatile long _soTimeout;
  private volatile boolean _soTimeoutReadOnly = true;
  private volatile int _priority;
  private final TcpChannelListenerSupport _listenerSupport;
  private final int _autoFlushSize;
  private final Meters.ReactorMeters.TcpMeters _tcpMeters;
  private StopWatch _writeInterestWatch;
  private StopWatch _writeScheduleWatch;
  private volatile long _linger;
  private ScheduledFuture<?> _closeTimer;

  /**
   * Task scheduled in the selector thread for requesting write interest on socket.
   */
  private final Runnable _writeInterestTask = new Runnable() {
    public void run() {
      try {
        _key.interestOps(_key.interestOps() | SelectionKey.OP_WRITE);
      } catch (Throwable t) {
        abort(t, -1);
      }
    }
  };
  
  /**
   * Task scheduled in the input executor in order to handle input socket events.
   */
  private final Runnable _inputReadyTask = () -> {
    try {
      inputReadyInExecutor();
    }
    catch (Throwable t) {
      abort(t, -1);
    }
  };
  
  // we have flushed (some or all of buffered bytes, we can now update the last io time.
  private void updateWriteTime() {
      if (_inactivityTimer != null && ! _soTimeoutReadOnly) {
          _lastIOTime = System.currentTimeMillis();
      }	  
  }
  
  /**
   * Flush send-queue in channel output executor.
   */
  private final Runnable _flushTask = new Runnable() {
    public void run() {
      try {
        _writeScheduleWatch.close();

        // At this point, we know we can write some bytes. Try to flush pending bytes.
        if (!_bufferedSocket.flush()) {
          // Resume write interest (the _writeInterestScheduled flag is left to true).
          _selector.scheduleNow(_writeInterestTask);
          // Notify user that writing is blocked
          writeBlocked();
          return;
        }
                                
        // All sent: reset writeInterestScheduled to false, and re-enable the schedule of write interests.
        _writeInterestScheduled.set(false);
        
        // Notify listeners about the new unblocked state.
        writeUnblocked();
        
        // Check if some buffers have been added *after* we flushed all buffers, 
        // but *before* the _writeInterestScheduled flag have been reset to false.
        if (_bufferedSocket.remaining() > _autoFlushSize) {
          scheduleWriteInterest();
          return;
        }
        
        // All sent, check if we must close the socket.
        if (_closed.get()) {
        	// Re check in case someone added data to send. If true, it means a write interest is in progress.
        	if (! _bufferedSocket.hasRemaining()) {
        		abort(null, -1);
        	}
        }        
      } catch (Throwable t) {
        abort(t, -1);
      }
    }
  };
  
  /**
   * Inactivity timer scheduled in reactor or in input executor thread.
   */
  protected class InativityTimer implements Runnable {
    @Override
    public void run() {
      if (!_inactivityTimer.isCancelled() && !_closed.get()) {
        long now = System.currentTimeMillis();
        long nowPadded = now + 100;
        if (nowPadded - _lastIOTime > _soTimeout) {
        	_logger.info("ReceiveTimeout on %s", TcpChannelImpl.this);
          // We are running within our executor thread.
          _lastIOTime = now;
          _listener.receiveTimeout(TcpChannelImpl.this);
        }
      }
    }
  }
  
  public TcpChannelImpl(SocketChannel socket, SelectionKey key, ReactorImpl reactor, NioSelector selector,
                        TcpChannelListener listener, int priority, Executor queue,
                        boolean directBuffer, int autoFlushSize, long linger) throws IOException {
    _listenerSupport = new TcpChannelListenerSupport();
    _selector = selector;
    _reactor = reactor;
    _listener = listener;
    _priority = priority;
    _queue = queue;
    _socket = socket;
    if (key == null) {
      key = selector.registerSelectHandler(socket, 0, this);
    } else {
      key.interestOps(0);
      key.attach(this);
    }
    _key = key;
    _readInterestController = new ReadInterestController(_key, _logger.getLogger(), (read) -> {});
    _remoteAddress = new InetSocketAddress(socket.socket().getInetAddress(), socket.socket().getPort());
    _localAddress = new InetSocketAddress(socket.socket().getLocalAddress(), socket.socket().getLocalPort());
    _tcpMeters = _reactor.getMeters().newTcpMeters(_remoteAddress);
    _bufferedSocket = new BufferedSocketChannel(_socket, _localAddress, _remoteAddress, reactor,
        _logger.getLogger(), _tcpMeters);
    _autoFlushSize = autoFlushSize;
    _directBuffer = directBuffer;
    _linger = linger;
  }
  
  // --- Public methods -------------------------------------------------
  
  @Override
  public String toString() {
    return "TcpChannel: local=" + _localAddress + ",remote=" + _remoteAddress;
  }
  
  // --- TcpChannel interface--------------------------------------------
  
  public int getPriority() {
    return _priority;
  }
  
  public InetSocketAddress getLocalAddress() {
    return _localAddress;
  }
  
  public InetSocketAddress getRemoteAddress() {
    return _remoteAddress;
  }
  
  // called from reactor or input executor thread
  public void setSoTimeout(final long soTimeout) {
	  setSoTimeout(soTimeout, true);
  }
  
  // called from reactor or input executor thread
  public void setSoTimeout(final long soTimeout, boolean readOnly) {
    if (_inactivityTimer != null) {
      _inactivityTimer.cancel(false);
    }
    
    _soTimeout = soTimeout;
    _soTimeoutReadOnly = readOnly;
    
    if (soTimeout > 0) {
      // TODO activate the timer *after* the connected or accepted callback has been invoked.
      _lastIOTime = System.currentTimeMillis();
      _inactivityTimer = _reactor.getReactorProvider().getApproxTimerService()
          .scheduleWithFixedDelay(_queue, new InativityTimer(), 500, 500, TimeUnit.MILLISECONDS);
    }
  }
  
  @SuppressWarnings("unchecked")
  public <T> T attachment() {
    return (T) _attached;
  }
  
  public void attach(Object attached) {
    _attached = attached;
  }
  
  public void close() {
    if (_closed.compareAndSet(false, true)) {
		Helpers.channelClosing(this);
		_selector.schedule(() -> {
			if (_linger > 0L) {
				_closeTimer = _selector.schedule(() -> abort(null, 0), _linger, TimeUnit.MILLISECONDS);
			}
			scheduleWriteInterest();
		});
    }
  }
  
  public void shutdown() {
    Helpers.channelShuttingDown(this);
    _closed.set(true);
    _selector.schedule(() -> abort(null, 0));
  }
  
  public boolean isClosed() {
    return _closed.get();
  }
  
  public void disableReading() {
    _readInterestController.disableReading(_selector);
  }
  
  public void enableReading() {
    _readInterestController.enableReading(_selector);
  }
  
  public void disableReadingInternal() {
    _readInterestController.disableReadingInternal(_selector);
  }
  
  public void enableReadingInternal() {
    _readInterestController.enableReadingInternal(_selector);
  }
  
  public void send(DataBuffer data) {
    try {
      send(data.getInternalBuffer(), true);
    }
    
    finally {
      data.resetCapacity();
    }
  }
  
  public void send(byte[] data) {
    send(data, true);
  }
  
  public void send(byte[] data, boolean copy) {
    send(data, 0, data.length, copy);
  }
  
  public void send(byte[] data, int off, int len) {
    send(data, off, len, true);
  }
  
  public void send(ByteBuffer buf) {
    send(buf, true);
  }
  
  public void send(ByteBuffer[] bufs) {
    send(bufs, true);
  }
  
  public void send(byte[] data, int off, int len, boolean copy) {
    send(ByteBuffer.wrap(data, off, len), copy);
  }
  
  public void send(ByteBuffer buf, boolean copy) {
    doSend(buf, copy);
  }
  
  public void send(ByteBuffer[] bufs, boolean copy) {
    doSend(bufs, copy);
  }
  
  public void flush() {
    if (_autoFlushSize > 0 && _bufferedSocket.remaining() > 0) {
      scheduleWriteInterest();
    }
  }
  
  public boolean isSecure() {
    return false;
  }
  
  public Reactor getReactor() {
    return _reactor;
  }
  
  public int getSendBufferSize() {
    return _bufferedSocket.remaining();
  }
  
  /**
   * Sets the executor used to dispatch listener methods.
   */
  public void setInputExecutor(Executor executor) {
    if (executor == null) {
      throw new IllegalArgumentException("Input Executor can't bet set to null");
    }
    _queue = executor;
    if (_soTimeout > 0L) {
      setSoTimeout(_soTimeout, _soTimeoutReadOnly);
    }
  }
  
  /**
   * Returns the executor used to dispatch listener methods.
   */
  public Executor getInputExecutor() {
    return _queue;
  }
  
  public void setPriority(int priority) {
    _priority = priority;
  }
  
  
  @Override
  public void setSoLinger(long linger) {
	  _linger = linger;
  }

  public Map<String, Object> exportTlsKey(String asciiLabel, byte[] context_value, int length) {
      throw new IllegalStateException("TcpChannel is not in secure mode");
  }

  // --- Reactor.Listener methods ----------------------------------------------
  
  /**
   * Method called by the selector thread when read/write/open/close operations are ready to be
   * processed.
   */
  public void selected(SelectionKey key) {
    try {
      if (key.isValid()) {
        if (key.isWritable()) {
          outputReady();
        }
        if (key.isReadable()) {
          inputReady();
        }
      } else {
        // key probably cancelled.
        abort(new IOException("Invalid Selection Key (socket closed, or key has been cancelled)"), -1);
      }
    }
    
    catch (Throwable t) {
      abort(t, -1);
    }
  }
  
  // --- Protected methods -------------------------------------------------------
  
  protected void inputReady() {
    disableReadingInternal();
    _queue.execute(_inputReadyTask);
  }
  
  protected void inputReadyInExecutor() throws IOException {
    ByteBuffer rcvBuf = Helpers.getCurrentThreadReceiveBuffer(_directBuffer);

    try {    
      if (readData(rcvBuf) == -1) {
        abort(new IOException("EOF exception"), -1);
        return;
      }
      
      if (!rcvBuf.hasRemaining()) {
        return;
      }
      
      if (_logger.isInfoEnabled()) {
        Helpers.logPacketReceived(_logger.getLogger(), rcvBuf, this);
      }
      
      if (_closed.get()) {
        if (_logger.isInfoEnabled()) {
          _logger.info("Message not passed to channel listener (channel has been closed)");
        }
        return;
      }
      
      _listenerSupport.handleMessage(_listener, this, rcvBuf);
    } finally {
      rcvBuf.clear();
    }
  }
  
  /**
   * Reads incoming data from socket.
   * @param rcvBuf 
   * @return true if the socket is not closed, false if not
   * @throws IOException on any errors
   */
  protected int readData(ByteBuffer rcvBuf) throws IOException {
    // Just keep track of this current read time, just in case a timer has been
    // started.
    
    if (_inactivityTimer != null) {
      _lastIOTime = System.currentTimeMillis();
    }
        
    // Read this socket.
    _tcpMeters.tcpRead();
    int n = _socket.read(rcvBuf);
    if (n > 0) {
      _tcpMeters.tcpReadBytes(n);
    }
    rcvBuf.flip();
    return n;
  }
  
  private void outputReady() {
	// we are about to write something, update the last write time.
	updateWriteTime();

    _writeInterestWatch.close();
    _key.interestOps(_key.interestOps() & ~SelectionKey.OP_WRITE);
    if (Helpers.dispatchWrites()) {
    	// writes are rescheduled in the processing threadpool, 
    	Helpers.getDefaultOutputExecutor().execute(_flushTask);
    } else {
    	// schedule the write in our own queue.
    	Helpers.schedule(_queue, ExecutorPolicy.SCHEDULE_HIGH, _flushTask);
    }
  }
  
  protected void doSend(final Object msg, final boolean copy) {    
    try {
      int bufferedBytes = 0;
      if (msg instanceof ByteBuffer) {
        bufferedBytes = _bufferedSocket.write((ByteBuffer) msg, copy);
      } else {
        bufferedBytes = _bufferedSocket.write((ByteBuffer[]) msg, copy);
      }
      if (bufferedBytes > _autoFlushSize) {
        scheduleWriteInterest();
      }
    }
    
    catch (Throwable t) {
      _logger.error("got unexpected exception while sending data to %s",t, getRemoteAddress());
      shutdown();
    }
  }
  
  // --- Private methods -------------------------------------------------------
  
  private void scheduleWriteInterest() {
    if (_writeInterestScheduled.compareAndSet(false, true)) {
      _writeInterestWatch = _tcpMeters.startTcpWriteDelayReadyWatch();
      _writeScheduleWatch = _tcpMeters.startTcpWriteDelayScheduleWatch();
      _selector.scheduleNow(_writeInterestTask);
    }
  }
  
  /**
   * The socket being currently written becomes blocked (full). It means that some bytes
   * are buffered until the socket becomes unblocked. 
   */
  private void writeBlocked() {
    if (_writeBlocked.compareAndSet(false, true)) {
      _tcpMeters.tcpWriteBlocked();
      switch (_writeBlockedPolicy) {      
      case NOTIFY:
        _queue.execute(() -> {
          try {
            if (!_closed.get()) {
              _listener.writeBlocked(TcpChannelImpl.this);
            }
          } catch (Throwable t) {
            _logger.warn("got unexpected exception while invoking tcp listener writeBlocked callback", t);
          }
        });
        break;
      
      case DISABLE_READ:
        // Disable read on this socket which can't be written.
        disableReading();
        break;
      
      case IGNORE:
        break;
      }
    }
  }
  
  /**
   * The socket being currently written becomes unblocked (all buffered bytes have been flushed).
   * In this case, invoke the writeUnbloked callback if the listener is currently blocked.
   */
  private void writeUnblocked() {
    if (_writeBlocked.compareAndSet(true, false)) {
      _tcpMeters.tcpWriteUnblocked();
      switch (_writeBlockedPolicy) {
      case NOTIFY:
        _queue.execute(() -> {
          try {
            if (!_closed.get()) {
              _listener.writeUnblocked(TcpChannelImpl.this);
            }
          } catch (Throwable t) {
            _logger.warn("got unexpected exception while invoking tcp listener writeUnblocked callback", t);
          }
        });
        break;
      
      case DISABLE_READ:
        // Re-enable read mode on the socket (we disabled it from our writeBlocked method).
        enableReading();
        break;
      
      case IGNORE:
        break;
      }
    }
  }
  
  /**
   * Close the socket, if not already done, and notify the channel listener, if not already done.
   * This method can be called from the selector thread, from the input executor, or from the
   * output executor thread.
   * So, because selection key must be cancelled from a selector thread, we schedule this method in 
   * the selector thread.
   * @param t an exception to log (null if no exception to log)
   * @param lingerSecs -1 if no linger must be used, or >= 0 if linger must be used.
   */
  protected void abort(final Throwable t, final int lingerSecs) {
	  // Cancel inactivity time.
	  Future<?> inactivityTimer = _inactivityTimer;
	  if (inactivityTimer != null) {
		  inactivityTimer.cancel(false);
	  }
	  _selector.scheduleNow(() -> doAbort(t, lingerSecs));
  }
  
  private void doAbort(final Throwable t, final int lingerSecs) {
	  if (_aborted.compareAndSet(false, true)) {
		  if (t != null) {
			  Level level = Helpers.isSocketException(t) ? Level.INFO : Level.WARN;
			  _logger.log(level, "Got exception on channel: %s", t, this);
			  if (! _logger.isEnabledFor(level) && ! _closed.get()) {
				  Helpers.channelAborted(this, t);
			  }
		  } else if (! _closed.get()) {
			  Helpers.channelAborted(this, new Exception());
		  }

		  _logger.info("Connection closed: %s", toString());

		  // has been incremented in TcpserverChannelImpl.doAccept()
		  _selector.getMeters().addTcpChannel(-1, isSecure()); 

		  if (_closeTimer != null) {
			  _closeTimer.cancel(false);
		  }
			  
		  try {
			  if (_key != null) {
				  _key.cancel(); // must be done in the selector thread
			  }
		  } catch (Throwable e) {
		  }

		  // Before closing the socket, reenable write interest in order to make sure we'll reschedule any write interest 
		  // (in case the user sends more data after socket closed).
		  _writeInterestScheduled.set(false);

		  // now close socket and cleanup socket metering counters
		  if (_bufferedSocket != null) {
			  _bufferedSocket.close(lingerSecs);
		  }

		  if (_writeBlocked.compareAndSet(true, false)) {
			  _tcpMeters.tcpWriteUnblocked();
		  }
		  
		  _closed.set(true);
		  _queue.execute(() -> {
			  try {
				  _listenerSupport.close();
				  _tcpMeters.close();
				  _listener.connectionClosed(TcpChannelImpl.this);
			  } catch (Throwable err) {
				  _logger.error(
						  "Got unexpected exception while invoking connectionClosed callback on connection: %s",
						  err,
						  TcpChannelImpl.this);
			  }
		  });
	  }	else {
		  // send method has been called after the connection closed, we still need to call the following method, in order to cleanup metering counters
		  // First, reenable write interest, this will enable send method to schedule write interest, which will eventually be aborted
		  _writeInterestScheduled.set(false);
		  
		  // Now, we can safely cleanup our metering counters
		  if (_bufferedSocket != null) {
			  _bufferedSocket.close(lingerSecs);
		  }
	  }
  }
  
  @Override
  public void setWriteBlockedPolicy(WriteBlockedPolicy writeBlockedPolicy) {
    _writeBlockedPolicy = writeBlockedPolicy;
  }
  
  @Override
  public void upgradeToSecure() {
  }

  @Override
  public Security getSecurity() {
    return null;
  }

  @Override
  public SSLEngine getSSLEngine() {
    return null;
  }

  @Override
  public List<SNIHostName> getClientRequestedServerNames() {
	return Collections.emptyList();
  }
}
