// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.examples;

// Jdk
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.UdpOption;
import alcatel.tess.hometop.gateways.reactor.UdpChannel;
import alcatel.tess.hometop.gateways.reactor.UdpChannelListener;

/**
 * Listen to udp message on port 9999.
 */
public class TestUdpServer implements UdpChannelListener {
  final static Logger tracer = Logger.getLogger("test");
  static UdpChannel _channel;
  static Reactor reactor;
  public static void main(String args[]) throws Exception {
    try {
      tracer.setLevel(Level.DEBUG);
      
      TestUdpServer me = new TestUdpServer();
      ReactorProvider factory = ReactorProvider.provider();
      reactor = factory.create("reactor");
      reactor.start();
      
      InetSocketAddress local = new InetSocketAddress(9999);
      Map<ReactorProvider.UdpOption, Object> o = new HashMap<> ();
      o.put(UdpOption.ENABLE_READ, false);
      _channel = factory.udpBind(reactor, local, me, o);
      //factory.newUdpChannel(local, me, reactor, null, tracer);

      System.out.println("Created udp server: " + _channel);
      
      reactor.schedule(new Runnable() {
        public void run() {
         System.out.println("LISTENING udp channel");
         _channel.enableReading();
        }}, 3000, TimeUnit.MILLISECONDS);
      
      Thread.sleep(Integer.MAX_VALUE);
    }
    
    catch (Throwable t) {
      tracer.error("got exception while binding udp server", t);
    }
  }
  
  // --- ChannelListener interface ---
  
  public void connectionOpened(UdpChannel cnx) {
	    tracer.warn("UdpConnection connectionOpened: " + cnx);
	    _channel = cnx;
	    //cnx.disableReading();
  }
  
  public void connectionClosed(UdpChannel c) {
    tracer.warn("UdpConnection closed: " + c);
  }
  
  public void connectionFailed(UdpChannel cnx, Throwable err) {
    tracer.warn("UdpConnection failed on local addr " + cnx.getLocalAddress() + "(attachment="
                    + cnx.attachment() + ")", err);
  }
  
  final static AtomicInteger _counter = new AtomicInteger();
  
  // Handle an incoming message. 
  public void messageReceived(UdpChannel cnx, ByteBuffer msg, InetSocketAddress from) {
    StringBuffer sb = new StringBuffer();
    while (msg.hasRemaining()) {
      sb.append((char) msg.get());
    }
    //if (_counter.get() % 1000 == 0) {
	tracer.warn("Received message from " + from);
	//}


    if (_counter.getAndIncrement() == 10000000) {
        tracer.warn("RECEIVED LAST MSG from " + from);
    }
  }
  
  // Invoked when an IO exception occurs. 
  public void exceptionCaught(UdpChannel cnx, Throwable cause) {
    tracer.warn("Got exception", cause);
  }
  
  // Called if we are using timers. (see Channel.setSoTimeout()). 
  public void receiveTimeout(UdpChannel cnx) {
    tracer.warn("Message timeout");
  }
  
  // When invoked, this method tells that the socket is blocked on writes.
  // Actually, this method may be usefull for flow control: for example,
  // You can stop reading a socket by calling Channel.disableReading()
  // method. Re-enabling read operations may be done by calling 
  // Channel.enableReading().
  public void writeBlocked(UdpChannel cnx) {
    tracer.warn("Write blocked");
  }
  
  // When invoked, this method tells that all pending data has been sent out.
  // Actually, this method may be usefull for flow control: for example,
  // You can stop reading a socket by calling Channel.disableReading()
  // method. Re-enabling read operations may be done by calling 
  // Channel.enableReading().
  public void writeUnblocked(UdpChannel cnx) {
    tracer.warn("Write unblocked");
  }
}
