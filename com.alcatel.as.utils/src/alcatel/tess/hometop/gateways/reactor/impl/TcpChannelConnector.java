// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.impl;

// Jdk
import java.io.IOException;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import alcatel.tess.hometop.gateways.reactor.Security;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpClientChannelListener;
import alcatel.tess.hometop.gateways.utils.Log;

public class TcpChannelConnector implements SelectHandler {
  private final Executor _queue;
  private final TcpClientChannelListener _listener;
  private final ReactorImpl _reactor;
  private final InetSocketAddress _from;
  private final InetSocketAddress _addr;
  private final Object _attached;
  private final static Log _logger = Log.getLogger("as.service.reactor.TcpChannelConnector");
  private final boolean _noDelay;
  private final NioSelector _selector;
  private final int _priority;

  private SelectionKey _key; // accessed from selector thread
  private SocketChannel _socket; // accessed from selector thread
  private Future<?> _timer; // accessed from selector thread
  private boolean _connectionFailedCalled; // accessed from selector thread
  private final boolean _directBuffer;
  private final int _autoFlushSize;
  private final boolean _ipTransparent;
  private final int _sndbuf;
  private final int _rcvbuf;
  private final Security _security;
  private TcpSocketOptionHelper _socketOptionHelper;
  private final long _linger;

  // Timer scheduled in selector thread
  public class ConnectionTimeout implements Runnable {
    public void run() {
      _logger.info("Could not connect to %s timely.", _addr);
      abort();
      connectionFailed(createFailedChannel(), new TimeoutException("Could not connect to " + _addr + " timely"));
    }
  }
  
  public TcpChannelConnector(ReactorImpl reactor, TcpClientChannelListener listener, InetSocketAddress from,
                             InetSocketAddress addr, Object attachment, int priority,
                             Executor queue, boolean noDelay, boolean directBuffer, int autoFlushSize,
                             boolean ipTransparent, int sndbuf, int rcvbuf, Security security, long linger) {
    _reactor = reactor;
    _listener = listener;
    _from = from;
    _addr = addr;
    _attached = attachment;
    _selector = _reactor.getSelector(Helpers._tcpSelectorCounter);
    _security = security;
    _queue = queue;
    _noDelay = noDelay;
    _priority = priority;
    _directBuffer = directBuffer;
    _autoFlushSize = autoFlushSize;
    _ipTransparent = ipTransparent;
    _sndbuf = sndbuf;
    _rcvbuf = rcvbuf;
    _socketOptionHelper = new TcpSocketOptionHelper(_logger);
    _linger = linger;
  }
  
  public InetSocketAddress getAddress() {
    return _addr;
  }
  
  public void connect() {
    connect(0 /* no timeout */);
  }
  
  public void connect(final long timeout) {
    Helpers.barrier(_queue);
    _selector.schedule(new Runnable() {
      public void run() {
        if (_timer != null) {
          _timer.cancel(false);
        }
        _logger.info("Connecting to %s using timer %d", _addr, timeout);
        if (timeout > 0) {
          _timer = _selector.schedule(new ConnectionTimeout(), timeout, TimeUnit.MILLISECONDS);
        }
        doConnect();
      }
    });
  }
  
  public int getPriority() {
    return 0;
  }
  
  public void selected(SelectionKey key) {
    try {
      SocketChannel sc = (SocketChannel) key.channel();
      if (!sc.finishConnect()) {
        throw new ConnectException("Can not connect to " + _addr);
      }
      connected(sc);
    }
    
    catch (Throwable t) {
      _logger.info("Could not connect to %s", t, _addr);
      abort();
      connectionFailed(createFailedChannel(), t);
    }
  }
  
  private TcpChannel createFailedChannel() {
    return new ClosedTcpChannelImpl(_reactor, _priority, _attached, _security != null, _from, _addr);
  }
  
  private TcpChannelImpl createChannel(SocketChannel socket) throws Exception {
    TcpChannelImpl cnx;

    if (_security != null) {
    	cnx = new TcpChannelSecureImpl(socket, _key, _reactor, _selector, _listener, _priority, 
    								   true, _queue, _directBuffer, _autoFlushSize, _security, _security.isDelayed(), _linger);
    } else {
      cnx = new TcpChannelImpl(socket, _key, _reactor, _selector, _listener, _priority, _queue, _directBuffer, _autoFlushSize, _linger);
    }
    cnx.attach(_attached);  
    return cnx;
  }
  
  private void abort() {
    if (_timer != null) {
      _timer.cancel(false);
    }
    
    try {
      if (_key != null) {
        _key.cancel();
      }
    } catch (Throwable ignored) {
    }
    
    try {
      if (_socket != null) {
        _socket.close();
      }
    } catch (IOException e) {
    }
  }
  
  private void doConnect() {
    try {
      _socket = SocketChannel.open();
      _socket.socket().setKeepAlive(true);
      _socket.socket().setTcpNoDelay(_noDelay);
      _socket.socket().setReuseAddress(true);
      
      if (_sndbuf > 0) {
        _socket.socket().setSendBufferSize(_sndbuf);
      }
      if (_rcvbuf > 0) {
        _socket.socket().setReceiveBufferSize(_rcvbuf);
      }
      if (_from != null) {
        _logger.info("Bind to local addr %s (tcpNoDelay=%b)", _from, _noDelay);
        if(_ipTransparent) {
            _socketOptionHelper.setIpTransparent(_socket, _from.getAddress() instanceof Inet4Address);
        }
        _socket.socket().bind(_from);
      }
      _socket.configureBlocking(false);
      if (!_socket.connect(_addr)) {
        _key = _selector.registerSelectHandler(_socket, SelectionKey.OP_CONNECT,
                                                      TcpChannelConnector.this);
      } else {
        connected(_socket);
      }
    }
    
    catch (Throwable t) {
      _logger.info("Failed to connect to %s", t, _addr);
      abort();
      connectionFailed(createFailedChannel(), t);
    }
  }
  
  private void connected(SocketChannel channel) throws Exception {
    _logger.info("Connection established to %s", _addr);

    if (_timer != null) {
      _timer.cancel(false);
    }
    
    final TcpChannelImpl cnx = createChannel(channel); // read interest disabled  
    _connectionFailedCalled = true;
    _queue.execute(new Runnable() {
      public void run() {
        try {
          _selector.getMeters().addTcpChannel(1, _security != null); 
          _listener.connectionEstablished(cnx);
          cnx.enableReadingInternal();
        } catch (Throwable t) {
          _logger.warn("got unexpected exception while invoking connectionEstablished callback", t);
          cnx.shutdown(); // abort
        }
      }
    });
  }
  
  private void connectionFailed(final TcpChannel channel, final Throwable t) {
    if (!_connectionFailedCalled) {
      _connectionFailedCalled = true;
      _queue.execute(new Runnable() {
        public void run() {
          try {
            _listener.connectionFailed(channel, t);
          } catch (Throwable t2) {
            _logger.warn("Got exception while invoking connectionFailed callback", t2);
          }
        }
      });
    }
  }
}
