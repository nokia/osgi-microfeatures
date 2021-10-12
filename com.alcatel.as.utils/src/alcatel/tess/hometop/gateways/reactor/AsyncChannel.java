// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

import org.osgi.annotation.versioning.ProviderType;

import alcatel.tess.hometop.gateways.reactor.util.DataBuffer;

/**
 * Base interface for TCP/UDP connections.
 */
@ProviderType
public interface AsyncChannel extends Channel {
  /**
   * Policy used to decice which action to be done when a socket becomes full.
   */
  enum WriteBlockedPolicy {
    /**
     * The socket read interest is automatically disabled when it becomes full.
     * When using this mode, the {@link AsyncChannel#enableReading()}/{@link AsyncChannel#disableeReading()}
     * method must not be invoked by the application.
     */
    DISABLE_READ,
    
    /**
     * The channel listener is notified using writeBlocked/writeUnblocked callbacks.
     * In this case, the listener may itself decide to block socket read mode, using
     * enableRead/disableRead channel's methods.
     */
    NOTIFY,
    
    /**
     * Nothing is done when a socket can't be written (because it's full).
     */
    IGNORE
  }
  
  /**
   * Socket priorities: high priority sockets are selected before low priority
   * sockets. By default, channels are created using priority = MAX_PRIORITY.
   */
  int MAX_PRIORITY = 0;
  int MIN_PRIORITY = 1;
  
  /**
   * Gets the socket priority. <BR>
   * Sockets with high priority are selected BEFORE socket with low priority.
   * Used when "Response" socket needs to be scheduled before "Request" sockets.
   * #see {@link #MAX_PRIORITY}
   * #see {@link #MIN_PRIORITY}
   * @return The socket priority.
   */
  int getPriority();
  
  /**
   * Sets the socket priority. <BR>
   * Sockets with high priority are selected BEFORE socket with low priority.
   * Used when "Response" socket needs to be scheduled before "Request" sockets.
   * #see {@link #MAX_PRIORITY}
   * #see {@link #MIN_PRIORITY}
   */
  void setPriority(int priority);
  
  /**
   * Sets the <code>WriteBlockedPolicy</code> policy to be used when the socket becomes full.
   * @see {@link WriteBlockedPolicy#NOTIFY}
   */
  void setWriteBlockedPolicy(WriteBlockedPolicy writeBlockedPolicy);
  
  /**
   * Gets the local address (IP address/port number) of this connection.
   * 
   * @return The local address.
   */
  InetSocketAddress getLocalAddress();
  
  /**
   * Enables/disables SO_TIMEOUT with the specified timeout, in milliseconds. <BR>
   * If no data arrives from the socket, then the listener will be called in its
   * receiveTimeout() method.
   * 
   * @param soTimeout the specified timeout, in milliseconds, or 0 for no timeout.
   */
  void setSoTimeout(long soTimeout);
    
  /**
   * Enables/disables SO_TIMEOUT with the specified timeout, in milliseconds. <BR>
   * If no data arrives from the socket, then the listener will be called in its
   * receiveTimeout() method. Optionally, you can specify if you also need to check
   * if the timer must be rearmed in case the socket is flushed.
   * 
   * @param soTimeout the specified timeout, in milliseconds, or 0 for no timeout.
   * @param readOnly true if read timeouts must be tracked, false if both read/write 
   *        events must be tracked.
   */
  void setSoTimeout(long soTimeout, boolean readOnly);

  /**
   * Tells if this connection is closed.
   * @return true if this connection is closed.
   */
  boolean isClosed();
  
  /**
   * Disables this connection for read operations.
   */
  void disableReading();
  
  /**
   * Enables this connection for read operations.
   */
  void enableReading();
  
  /**
   * Configure an executor for this channel: The channel listener
   * methods will be invoked using this executor. By default,
   * the channel listener will be invoked with a single reactor thread.
   * @param executor the executor used to invoke callbacks of this channel.
   */
  void setInputExecutor(Executor executor);
    
  /**
   * Returns the executor used to handle channel events
   * @param executor the executor used to handle channel events
   */
  Executor getInputExecutor();
    
  /**
   * Writes some data into this connection. 
   * 
   * @param msg The message to be sent.
   * @param copy true if the buffer must be copied when it is buffered (because
   *          the socket is full). You are strongly encouraged to "give" the
   *          buffer to this method (that is: provide copy=false).
   */
  void send(ByteBuffer msg, boolean copy);
  
  /**
   * Writes some data into this connection. 
   * 
   * @param msg The message to be sent.
   * @param copy true if the buffer must be copied when it is buffered (because
   *          the socket is full). You are strongly encouraged to "give" the
   *          buffer to this method (that is: provide copy=false).
   */
  void send(ByteBuffer[] msg, boolean copy);
  
  /**
   * Writes some data into this connection. 
   * 
   * @param msg The message to be sent. Will be copied if the socket is full.
   * @deprecated use {@link #send(ByteBuffer, boolean)} or
   *             {@link #send(ByteBuffer[], boolean)}
   */
  @Deprecated
  void send(DataBuffer msg);
  
  /**
   * Writes some data into this connection. 
   * 
   * @param msg The message to be sent. Will be copied if the socket is full.
   * @deprecated use {@link #send(ByteBuffer, boolean)} or
   *             {@link #send(ByteBuffer[], boolean)}
   */
  @Deprecated
  void send(ByteBuffer msg);
  
  /**
   * Writes some data into this connection. 
   * 
   * @param msg The message to be sent. Will be copied if the socket is full.
   * @deprecated use {@link #send(ByteBuffer, boolean)} or
   *             {@link #send(ByteBuffer[], boolean)}
   */
  @Deprecated
  void send(ByteBuffer[] msg);
  
  /**
   * Writes some data into this connection. 
   * 
   * @param msg The message to be sent. Will be copied if the socket is full.
   * @deprecated use {@link #send(byte[], boolean)}
   */
  @Deprecated
  void send(byte[] msg);
  
  /**
   * Writes some data into this connection. 
   * 
   * @param msg The message to be sent.
   * @param copy true if the buffer must be copied when it is buffered (because
   *          the socket is full) You are strongly encouraged to "give" the
   *          buffer to this method (that is: provide copy=false).
   * @deprecated use {@link #send(ByteBuffer, boolean)} or
   *             {@link #send(ByteBuffer[], boolean)}
   */
  @Deprecated
  void send(byte[] msg, boolean copy);
  
  /**
   * Writes some data into this connection. 
   * 
   * @param msg The message to be sent. Will be copied if the socket is full.
   * @param off thhe message offset
   * @param len the message length
   * @deprecated use {@link #send(byte[], int, int, boolean)}
   */
  @Deprecated
  void send(byte[] msg, int off, int len);
  
  /**
   * Writes some data into this connection. 
   * 
   * @param msg The message to be sent.
   * @param off the message offset
   * @param len  the message length
   * @param copy true if the buffer must be copied when it is buffered (because
   *          the socket is full) You are strongly encouraged to "give" the
   *          buffer to this method (that is: provide copy=false).
   * @deprecated use {@link #send(ByteBuffer, boolean)} or
   *             {@link #send(ByteBuffer[], boolean)}
   */
  @Deprecated
  void send(byte[] msg, int off, int len, boolean copy);
  
  /**
   * Returns the number of bytes currently stored in the send queue associated to this channel.
   * @return the number of bytes currently stored in the channel send queue
   */
  int getSendBufferSize();
}
