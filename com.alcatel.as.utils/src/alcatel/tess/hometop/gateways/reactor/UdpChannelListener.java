// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor;

// Jdk
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * A Datagram Connection listener.
 */
public interface UdpChannelListener {
  /**
   * This udp channel is ready to send/receive data.
   * @param cnx the bound udp channel
   * @deprecated this method is not used anymore when the channel has been created
   * using {@link ReactorProvider#udpBind(Reactor, InetSocketAddress, UdpChannelListener, java.util.Map)}. 
   */
  @Deprecated
  void connectionOpened(UdpChannel cnx);
  
  /**
   * This udp channel could not be setup, mainly because of an
   * already bound port number.
   * @param cnx the udp channel which could not be bound to
   * @param err the error cause
   * @deprecated this method is not used anymore when the channel has been created
   * using {@link ReactorProvider#udpBind(Reactor, InetSocketAddress, UdpChannelListener, java.util.Map)}. 
   */
  @Deprecated
  void connectionFailed(UdpChannel cnx, Throwable err);
  
  /**
   * Called when this channel is closed.
   * @param cnx the closed channel
   */
  void connectionClosed(UdpChannel cnx);
  
  /**
   * Invoked when the reactor detects that a message is ready to be
   * read on this connection. You must read the message fully.
   * @param cnx the udp channel
   * @param msg the received message
   * @param addr the originator address
   */
  void messageReceived(UdpChannel cnx, ByteBuffer msg, InetSocketAddress addr);
  
  /**
   * Invoked when data did not arrive timely on a udp connection.
   * Notice that the connection is not closed. It is up to you to implements the
   * required logic, when a message does not arrive timely on this connection.
   * @param cnx the udp channel
   */
  void receiveTimeout(UdpChannel cnx);
  
  /**
   * When invoked, this method tells that the socket is blocked on writes.
   * Actually, this method may be usefull for flow control.
   * @param cnx the udp channel
   */
  void writeBlocked(UdpChannel cnx);
  
  /**
   * When invoked, this method tells that all pending data has been sent out.
   * Actually, this method may be usefull for flow control.
   * @param cnx the udp channel
   */
  void writeUnblocked(UdpChannel cnx);
}
