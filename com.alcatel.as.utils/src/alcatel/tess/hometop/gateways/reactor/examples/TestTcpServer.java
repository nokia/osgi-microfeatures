// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.examples;

// Jdk
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.PlatformExecutors;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannel;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannelListener;
import alcatel.tess.hometop.gateways.reactor.util.SynchronousTimerTask;

/**
 * Listen to incoming tcp connection request on port 9999.
 */
public class TestTcpServer implements TcpServerChannelListener {
  final static Logger tracer = Logger.getLogger(TestTcpServer.class);
  static PlatformExecutors _execs;
  
  public static void main(String args[]) throws Exception {
    ReactorProvider factory = ReactorProvider.provider();
    Reactor reactor = factory.create("server");
    reactor.start();
    InetSocketAddress from = new InetSocketAddress(args[0], 5500);
    _execs = PlatformExecutors.getInstance();
    final boolean secure = (args.length) > 0 ? args[args.length - 1].equals("secure") : false;

    TestTcpServer me = new TestTcpServer();
    Map<ReactorProvider.TcpServerOption, Object> o = new HashMap<>();
    o.put(ReactorProvider.TcpServerOption.ENABLE_READ, true);
    o.put(ReactorProvider.TcpServerOption.SECURE, secure);
    final TcpServerChannel s = factory.tcpAccept(reactor, from, me, o);
    
    Thread.sleep(Integer.MAX_VALUE);
  }
  
  // --- TcpServerChannelListener interface ---
  
  static TcpServerChannel tsc = null;
  
  public void serverConnectionOpened(final TcpServerChannel server) {
    tracer.warn("serverSocket ready to accept from local addr=" + server.getLocalAddress());
    tsc = server;
  }
  
  public void serverConnectionClosed(TcpServerChannel server) {
    tracer.warn("ServerSocket closed");
  }
  
  public void serverConnectionFailed(TcpServerChannel cnx, Throwable err) {
    tracer.warn("Could not setup our server socket on addr " + cnx.getLocalAddress() + " (attachment="
                    + cnx.attachment() + ")", err);
  }
  
  public void connectionAccepted(TcpServerChannel tsc, TcpChannel cnx) {
    //tracer.warn("Accepted new client: " + cnx);
    //cnx.setInputExecutor(_execs.createQueueExecutor(_execs.getProcessingThreadPoolExecutor()));
    //cnx.close();
  }
  
  public void connectionFailed(TcpServerChannel tsc, Throwable err) {
    tracer.warn("Failed to accept a new tcp connection", err);
  }
  
  // Invoked when the connection is closed. 
  public void connectionClosed(TcpChannel cnx) {
    tracer.warn("TcpChannel closed: " + cnx);
  }
  
  // Handle an incoming message. 
  public int messageReceived(TcpChannel cnx, ByteBuffer msg) {
    try {
      StringBuffer sb = new StringBuffer();
      while (msg.hasRemaining()) {
        sb.append((char) msg.get());
      }
      tracer.warn("Received: " + sb.toString());
      for (int i = 0; i < 5000000; i ++) 
      	cnx.send(ByteBuffer.wrap(("hi-" +i).getBytes()), false);
      tracer.warn("gracefully closing");
      cnx.close();
    }
    
    catch (Throwable e) {
      tracer.warn("Error while sending", e);
    }
    return 0;
  }
  
  // Called if we are using timers. 
  public void receiveTimeout(TcpChannel cnx) {
    tracer.warn("Message timeout");
  }
  
  // When invoked, this method tells that the socket is blocked on writes.
  // Actually, this method may be usefull for flow control: for example,
  // You can stop reading a socket by calling TcpChannel.disableReading()
  // method. Re-enabling read operations may be done by calling 
  // TcpChannel.enableReading().
  public void writeBlocked(TcpChannel cnx) {
    tracer.warn("Write blocked");
  }
  
  // When invoked, this method tells that all pending data has been sent out.
  // Actually, this method may be usefull for flow control: for example,
  // You can stop reading a socket by calling TcpChannel.disableReading()
  // method. Re-enabling read operations may be done by calling 
  // TcpChannel.enableReading().
  public void writeUnblocked(TcpChannel cnx) {
    tracer.warn("Write unblocked");
  }
}
