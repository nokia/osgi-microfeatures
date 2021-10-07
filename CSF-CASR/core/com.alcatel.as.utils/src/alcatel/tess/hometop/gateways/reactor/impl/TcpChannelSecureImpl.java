package alcatel.tess.hometop.gateways.reactor.impl;

// Jdk
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLEngine;

import com.alcatel.as.service.concurrent.ExecutorPolicy;

import alcatel.tess.hometop.gateways.reactor.Security;
import alcatel.tess.hometop.gateways.reactor.TcpChannelListener;
import alcatel.tess.hometop.gateways.reactor.util.DataBuffer;
import alcatel.tess.hometop.gateways.utils.Log;

/**
 * Secured tcp socket implementation.
 */
public class TcpChannelSecureImpl extends TcpChannelImpl {
  /**
   * This service does all the TLS encoding/decoding stuff.
   */
  private final TLSEngine _tlse;
  
  /**
   * Buffer used to store decoded message not fully handled by channel listener.
   * (the listener may not fully consume a received message, if it is not enough
   * big to contain a full application message).
   */
  private final DataBuffer _bufferedMessage;

  /**
   * Flag used to ensure that the secured socket closed method is called only once.
   */
  private final AtomicBoolean _closed = new AtomicBoolean(false);
  
  private AtomicBoolean tlsActivated;
  private Security security;
  private final static Log _logger = Log.getLogger("as.service.reactor.TcpChannelSecureImpl");

  /**
   * Makes a new secured TcpChannel.
   */
  public TcpChannelSecureImpl(SocketChannel socket, SelectionKey key, ReactorImpl reactor,
                              NioSelector selector, TcpChannelListener listener, int priority,
                              boolean isClient, Executor queue, boolean useDirectBuffer, int autoFlushSize,
                              Security security, boolean delayed, long linger) throws Exception 
  {	  
    super(socket, key, reactor, selector, listener, priority, queue, useDirectBuffer, autoFlushSize, linger);
    _bufferedMessage = new DataBuffer(Helpers.getRcvBufSize(), false);
    
    String remoteIp = socket.socket().getInetAddress().getHostAddress();
    int remotePort = socket.socket().getPort();
    tlsActivated = new AtomicBoolean(!delayed);
    this.security = security;
    _tlse = new TLSEngineImpl(security, isClient, "TLSv1.2", remoteIp, remotePort, "TCP");
  }
  
  @Override
  public String toString() {
    return "TcpChannelSecure [local=" + getLocalAddress() + ",remote=" + getRemoteAddress() + "]";
  }
  
  @Override
  public boolean isSecure() {
    return true;
  }
  
  @Override
  public List<SNIHostName> getClientRequestedServerNames() {
	return ((TLSEngineImpl) _tlse).getClientRequestedServerNames();
  }

  @Override
  protected void doSend(Object msg, final boolean copy) {
      if(tlsActivated.get()) {
          if (Helpers.isCurrentThreadInQueue(_queue)) {
        	  doSendSecureInExecutor(msg);
          } else {
        	  Object message = copy ? Helpers.copy(msg) : msg;
        	  Helpers.schedule(_queue, ExecutorPolicy.SCHEDULE_HIGH, () -> doSendSecureInExecutor(message));
          }
      } else {
    	  super.doSend(msg, copy);
      }
  }
  
  private void doSendSecureInExecutor(Object msg) {
    try {
      if (msg instanceof ByteBuffer) {
        ByteBuffer buf = (ByteBuffer) msg;
        if (_logger.isInfoEnabled()) {
          Helpers.logPacketSent(_logger.getLogger(), false, buf.duplicate(), buf.remaining(), this);
        }        
        _tlse.fillsEncoder((ByteBuffer) msg);
      } else {
        for (ByteBuffer buf : (ByteBuffer[]) msg) {
          if (_logger.isInfoEnabled()) {
            Helpers.logPacketSent(_logger.getLogger(), false, buf.duplicate(), buf.remaining(), this);
          }
          _tlse.fillsEncoder(buf);         
        }
      }
      
      runTLSEngine();
    } catch (Throwable t) {
      abort(false /* don't schedule connectionClosed in channel executor */, t);
    }
  }
  
  @Override
  public void close() {
    if(tlsActivated.get()) {	  
		if (_closed.compareAndSet(false, true)) {
			Runnable close = () -> {
				_logger.debug("Closing TLS connection %s", this);
				TcpChannelSecureImpl.super.close(); // for now, don't perform TLS graceful shutdown
			};

			if (Helpers.isCurrentThreadInQueue(_queue)) {
				close.run();
			} else {
				Helpers.schedule(_queue, ExecutorPolicy.SCHEDULE_HIGH, close::run);
			}
		}
    } else {
    	super.close();
    }
  }
  
  public Map<String, Object> exportTlsKey(String asciiLabel, byte[] context_value, int length) {
      // return _reactor.getReactorProvider().getTlsExport().exportKey(((TLSEngineImpl) _tlse).sslEngine(), asciiLabel, context_value, length);
      return Collections.emptyMap();
  }

  /**
   * Handle tls input data (we are running in the channel executor thread).
   */
  @Override
  protected void inputReadyInExecutor() throws IOException {
    ByteBuffer rcvBuf = Helpers.getCurrentThreadReceiveBuffer(_directBuffer);
    try {
      // Read data from socket (the rcvBuf will be flipped).
      if (readData(rcvBuf) == -1) {
        abort(false /* don't schedule connectionClosed in channel executor*/, new IOException("EOF exception"));
        return;
      }
      
      // Decode this encrypted message, which might be a handshake message.
      if (rcvBuf.hasRemaining() && _logger.isInfoEnabled()) {
        Helpers.logPacketReceived(_logger.getLogger(), rcvBuf, this);
      }
      
      if(tlsActivated.get()) {
	      _tlse.fillsDecoder(rcvBuf);
	      
	      // Run tls engine.
	      runTLSEngine();
      } else {
    	  messageReceived(rcvBuf);
      }
      enableReadingInternal();
    } finally {
      rcvBuf.clear();
    }
  }
  
  protected void abort(boolean scheduleConnectionClosed, Throwable t) {
    _bufferedMessage.resetCapacity();
    super.abort(t, -1);
  }
  
  /**
   * Runs the tls engine. We'll encode some messages to be sent, or decode some
   * received encrypted messages.
   */
  @SuppressWarnings("incomplete-switch")
  private void runTLSEngine() {
    try {
      TLSEngine.Status status;
      
      loop: while ((status = _tlse.run()) != TLSEngine.Status.NEEDS_INPUT) {
        switch (status) {
        case DECODED:
          messageReceived(_tlse.getDecodedBuffer());
          break;
        
        case ENCODED:
          _logger.debug("Encoded tls message");
          // This message is either a handshake message, or our encoded message: send it
          super.doSend(_tlse.getEncodedBuffer(), true);
          break;
        
        case CLOSED:
          _logger.debug("tls engine returned CLOSED status");
          abort(false, new IOException("TLS close"));
          break loop;
        }
      }
    }
    
    catch (Throwable t) {
      abort(false /* don't schedule connectionClosed in channel executor */, t);
    }
  }
  
  private void messageReceived(ByteBuffer decodedBuf) {
    if (decodedBuf.hasRemaining() && _logger.isInfoEnabled()) {
      Helpers.logPacketReceived(_logger.getLogger(), decodedBuf, this);
    }
    
    // Check if we have buffered a previous response, and append this new one to it.
    if (_bufferedMessage.position() > 0) {
      _bufferedMessage.put(decodedBuf);
      _bufferedMessage.flip();
      int missingBytes = _listener.messageReceived(this, _bufferedMessage.getInternalBuffer());
      if (missingBytes > 0) {
        _bufferedMessage.compact();
        _bufferedMessage.ensureCapacity(missingBytes);
      } else {
        _bufferedMessage.resetCapacity();
      }
    } else {
      int missingBytes = _listener.messageReceived(this, decodedBuf);
      if (missingBytes > 0) {
        _bufferedMessage.put(decodedBuf);
      }
    }
  }
  
  @Override
  public void upgradeToSecure() {
	  Runnable upgrade = () -> tlsActivated.set(true);
      if (Helpers.isCurrentThreadInQueue(_queue)) {
    	  upgrade.run();
      } else {
    	  _logger.debug("Upgrading to TLS");
    	  Helpers.schedule(_queue, ExecutorPolicy.SCHEDULE_HIGH, upgrade::run);
      }
  }

  @Override
  public Security getSecurity() {
    return this.security;
  }

  @Override
  public SSLEngine getSSLEngine() {
    return ((TLSEngineImpl) _tlse).sslEngine();
  } 
}
