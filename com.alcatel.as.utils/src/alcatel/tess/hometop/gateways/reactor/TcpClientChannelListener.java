// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor;

// Jdk

/**
 * A Tcp Connection listener.
 */
public interface TcpClientChannelListener extends TcpChannelListener {
  /**
   * A Client tcp connection has been established.
   * @param cnx the tcp channel
   */
  void connectionEstablished(TcpChannel cnx);
  
  /**
   * Invoked when a client tcp connection could not be established.
   * @param cnx The connection that could not be opened
   * @param error This exception may have the following values:<ul>
   *	<li>java.util.concurrent.TimeoutException if the connection could not be opened timely.
   *	<li>java.net.SocketExcepion (ConnectException,NoRouteToHostException, or PortUnreachableException) if we could not connect to the peer.
   *	<li>Any Throwables on any other uncheked/unexpected error. The application is in charge of
   *	    handling these unexpected exception: It should be at least log the error, or send an snmp alarms, etc ...
   *    </li>
   */
  void connectionFailed(TcpChannel cnx, Throwable error);
}
