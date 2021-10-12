// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * An SCTP asynchronous channel.
 */
public interface SctpChannelListener {
  /**
   * Events delivered when a peer address changed.
   */
  public enum AddressEvent
  {
      /**
       * The address is now part of the association.
       */
      ADDR_ADDED,

      /**
       * This address is now reachable.
       */
      ADDR_AVAILABLE,

      /**
       * This address has now been confirmed as a valid address.
       */
      ADDR_CONFIRMED,

      /**
       * This address has now been made to be the primary destination address.
       */
      ADDR_MADE_PRIMARY,

      /**
       * The address is no longer part of the association.
       */
      ADDR_REMOVED,

      /**
       * The address specified can no longer be reached.
       */
      ADDR_UNREACHABLE
  };

  /**
   * Handle an incoming SCTP message.
   * @param cnx The channel where the message is coming from
   * @param buf the received buf
   * @param addr The source address of the received message
   * @param bytes The number of bytes in the received message
   * @param isComplete False if the message is not fully available in the buffer 
   * @param isUnordered Tells whether or not the message is unordered
   * @param ploadPID The payload protocol Identifier.
   * @param streamNumber The stream number that the message was received on,
   */
  void messageReceived(SctpChannel cnx, ByteBuffer buf, SocketAddress addr, int bytes, boolean isComplete,
                       boolean isUnordered, int ploadPID, int streamNumber);
  
  /**
   * Invoked when data did not arrive timely on the sctp connection.
   * Notice that the connection is not closed. It is up to you to implements the
   * required logic, when a message does not arrive timely on this connection.
   * @param cnx the sctp channel where the receive timeout occurs
   */
  void receiveTimeout(SctpChannel cnx);
  
  /**
   * Sctp channel closed (possibly gracefuly).
   * @param cnx the channel concerned by this event
   * @param err if the close is caused by an unexpected error
   */
  void connectionClosed(SctpChannel cnx, Throwable err);
  
  /**
   * Socket output buffer is full
   * @param cnx the channel which can't bet written anymore
   */
  void writeBlocked(SctpChannel cnx);
  
  /**
   * Socket output buffer has been flushed and data can be written again
   * @param cnx the channel which can be written to again
   */
  void writeUnblocked(SctpChannel cnx);
  
  /**
   * Notification emitted when a send failed notification has been received
   * @param cnx the channel concerned by the event
   * @param addr The peer primary address of the association or the address that the message was sent to
   * @param buf the data that was to be sent
   * @param errcode the error code. The errorCode gives the reason why the send failed, and if set, will 
   *        be a SCTP protocol error code as defined in RFC2960 section 3.3.10 
   * @param streamNumber the stream number that the messge was to be sent on.
   */
  void sendFailed(SctpChannel cnx, SocketAddress addr, ByteBuffer buf, int errcode, int streamNumber);
  
  /**
   * Notification emmitted when an address change event occurred to the destination address on a multi-homed peer.
   * @param cnx the channel concerned by the event
   * @param addr the peer address concerned by the event
   * @param event the type of peer address change event
   */
  void peerAddressChanged(SctpChannel cnx, SocketAddress addr, AddressEvent event);
}
