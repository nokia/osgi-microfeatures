// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLEngine;

import org.osgi.annotation.versioning.ProviderType;

/**
 * An asynchronous tcp connection.
 * <p> You can have a look at some full fledged examples in 
 * <b>alcatel.tess.hometop.gateways.reactor.examples</b> package.
 */
@ProviderType
public interface TcpChannel extends AsyncChannel {
  /*
   * Get the remote address (ip address/port number) of this connection.
   * May be null when the connection is actually a datagram socket.
   */
  InetSocketAddress getRemoteAddress();
  
  /**
   * Forces any buffered data to be written out to the network.
   */
  void flush();
  
  /**
   * Indicates if this channel is secure or not.
   * @return true if the channel is in secured mode.
   */
  boolean isSecure();
  
  /**
   * Returns the secured client requested server names. Only used in server mode, and when security is enabled.
   * @returns the secured client requested server names (an empty list is returned in case the client did not indicate any SNI).
   */
  List<SNIHostName> getClientRequestedServerNames();
  
  void upgradeToSecure();

  /**
   * Get the security object of the channel
   * May return null is the channel is not secure
   */
  Security getSecurity();

  /**
   * Get the SSLEngine instance of the channel
   * May return null is the channel is not secure
   */
  SSLEngine getSSLEngine();  
  
  /**
   * Controls the action taken when unsent data is queued on the socket and a method to close the socket is invoked. 
   * it represents a Long timeout value, in milliseconds, known as the linger interval. The linger interval is the timeout for the close method to complete 
   * while the operating system attempts to transmit the unsent data. Socket will be forcibly closed after the timeout expires or when all unsent data is flushed.
   * 0 means no linger interval is used. By default, the linger option is set to 5000 milliseconds.
   */
  void setSoLinger(long linger);

  /**
   * Returns the TLS keys. TODO fix this comment.
   */
  Map<String, Object> exportTlsKey(String asciiLabel, byte[] context_value, int length);
}
