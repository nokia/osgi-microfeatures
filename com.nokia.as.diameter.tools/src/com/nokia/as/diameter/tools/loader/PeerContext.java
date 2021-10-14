package com.nokia.as.diameter.tools.loader;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

public abstract class PeerContext {
  static Logger _logger = Logger.getLogger(PeerContext.class);
  static AtomicInteger _clientID = new AtomicInteger();
  volatile ByteBuffer _rcvBuf = ByteBuffer.allocate(64 * 1024);
  volatile LinkedList<ByteBuffer> _queue = new LinkedList<ByteBuffer>();
  volatile boolean _ceaReceived;
  final String _originHost;
  volatile byte[] _acr;
  final ByteArrayOutputStream _out;
  volatile ByteBuffer[] _preallocBuffers = new ByteBuffer[16]; // NIO iovec size is 16
  final String _destinationHost;
  Future<?> _tpsTimer;
  final Executor _executor;
  boolean _isBlocked;
  boolean _dprSent;
  protected boolean _dpaReceived;
  
  PeerContext(int bulkSize, byte[] acrUserName, int fillbackSize, Executor executor) {
    int id = _clientID.incrementAndGet();
    _originHost = "client" + id;
    _destinationHost = "server"; // the proxy will dispatch on every servers, see routes.xml
    _acr = DiameterUtils.makeACR(_originHost, _destinationHost, acrUserName, fillbackSize);
    _out = new ByteArrayOutputStream(bulkSize);
    _executor = executor;
  }
  
  public abstract boolean flush(SelectionKey sk) throws Exception;
  
  public void enqueue(ByteBuffer buf) {
    _queue.add(buf);
  }
  
  public boolean isBlocked() {
    return _isBlocked;
  }
  
  public void setBlocked(boolean isBlocked) {
    _isBlocked = isBlocked;
  }
}
