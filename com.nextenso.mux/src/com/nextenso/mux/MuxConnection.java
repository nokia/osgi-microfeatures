// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

import com.nextenso.mux.socket.SocketManager;
import com.nextenso.mux.util.MuxIdentification;
import com.nextenso.mux.util.TimeoutManager;

import com.alcatel.as.util.sctp.*;

/**
 * This interface encapsulates a Mux Connection. 
 * <p>
 * Historically, MUX connections were used first between IO handlers (also called 'stacks') and agents. That's why the MUX api
 * provides method to deal with Stacks. If you use them for some other purpose (for example
 * for agent to agent communications), the stack parameters and method will be useless to you.  
 * <p/>
 * A MuxConnection is essentially a wrapper of a TCP/UDP or SCTP socket. When you send data on that socket, it is preceded by a small header (the so-called MUX header) that takes care
 * of indicating the data length, plus a few additional flags. Also, a magic header and trailer is put before/after you data.
 * That brings to you an easy way to send any piece of data to the peer without the burden of reassembling it to the other side.
 * Having the magic protects your multi-threaded applications. As a MUX server, you will never deal with corrupted data, even though a malicious/bugged
 * multi-threaded client messed its data sending. 
 * 
 * When used to connect agents to stacks, each MuxConnection is associated to exactly one MuxHandler: when data arrives from the stack, it calls 
 * back the MuxHandler; when a MuxHandler wants to send data to the stack, it uses a MuxConnection.
 * <p/>The main features are:
 * <p/>
 * <b>Mux Management</b>
 * <br/>All the methods used to define the connection (stack id).
 * <ul>
 * <li><code>getId()</code>
 * <li><code>getStackAppId()</code>
 * <li><code>getStackAppName()</code>
 * <li><code>getStackInstance()</code>
 * <li><code>getStackHost()</code>
 * <li><code>getStackAddress()</code>
 * <li><code>getStackPort()</code>
 * <li><code>sendMuxData(MuxHeader header, byte[] data, int off, int len, boolean copy)</code>
 * </ul>
 * <b>TCP Management</b>
 * <br/>All the methods used to handle TCP (socket open, socket close, tcp data).
 * <ul>
 * <li><code>sendTcpSocketListen(long listenId, int localIP, int localPort, boolean secure)</code>
 * <li><code>sendTcpSocketListen(long listenId, String localIP, int localPort, boolean secure)</code>
 * <li><code>sendTcpSocketConnect(long connectionId, String remoteHost, int remotePort, int localIP, int localPort, boolean secure)</code>
 * <li><code>sendTcpSocketConnect(long connectionId, String remoteHost, int remotePort, String localIP, int localPort, boolean secure)</code>
 * <li><code>sendTcpSocketClose(int sockId)</code>
 * <li><code>sendTcpSocketReset(int sockId)</code>
 * <li><code>sendTcpSocketAbort(int sockId)</code>
 * <li><code>sendTcpSocketData(int sockId, byte[] data, int off, int len, boolean copy)</code>
 * </ul>
 * <b>SCTP Management</b>
 * <br/>All the methods used to handle SCTP (socket open, socket close, stcp data).
 * <ul>
 * <li><code>sendSctpSocketListen (long listenId, String[] localAddrs, int localPort, int maxOutStreams, int maxInStreams, boolean secure)</code>
 * <li><code>sendSctpSocketConnect (long connectionId, String remoteHost, int remotePort, String[] localAddrs, int localPort, int maxOutSreams, int maxInStreams, boolean secure)</code>
 * <li><code>sendSctpSocketClose (int sockId)</code>
 * <li><code>sendSctpSocketReset (int sockId)</code>
 * <li><code>sendSctpSocketData (int sockId, String addr, boolean complete, int ploadPID, int streamNumber, long timeToLive, boolean unordered, boolean copy, ByteBuffer... data)</code>
 * </ul>
 * <b>UDP Management</b>
 * <br/>All the methods used to handle UDP (socket bind, socket close, udp data).
 * <ul>
 * <li><code>sendUdpSocketBind(long bindId, int localIP, int localPort, boolean shared)</code>
 * <li><code>sendUdpSocketClose(int sockId)</code>
 * <li><code>sendUdpSocketData(int sockId, int remoteIP, int remotePort, int virtualIP, int virtualPort, byte[] data, int off, int len, boolean copy)</code>
 * </ul>
 */
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface MuxConnection
{
    /**
     * Configure an InputExecutor which will be used to read/handle the socket
     */
    public void setInputExecutor(Executor inputExecutor);
        
    /**
     * Sets the source address. Use this before connecting.
     */
    public InetSocketAddress getLocalAddress();

    /**
     * Sets the source address. Use this before connecting.
     */
    public InetSocketAddress getRemoteAddress();

    /**
     * Activates or deactivates the keep alive mechanism
     * This will fire the keep alive on this connection when both interval and idle factor are greater than zero.
     * This will stop the keep alive on this connection when interval or idle factor is lower or equal to zero.
     * @param interval keep alive interval (in seconds)
     * @param idleFactor MUX idle factor
     * @return false if the connection is not opened
     */
    public boolean setKeepAlive(int interval, int idleFactor);

    /**
     * Indicates if the keep alive mechanism is activated
     * @return true if keep alive mode is active on this connection, false if not.
     */
    public boolean useKeepAlive();

    /**
     * Specifies if the MuxConnection is opened.
     * <br/>A MuxConnection is opened until muxClosed() is called on the MuxHandler.
     */
    public boolean isOpened();

    /**
     * Closes this mux connection.
     * @deprecated use {@link #close(Enum, String, Throwable)}
     */
    public void close();

    /**
     * Closes a mux connection and provides some root cause informations describing why the connection is closed
     * @param reason the reason why the mux connection is closed
     * @param info the info about the mux close event.
     * @param err an optional throwable root cause
     * @param cnx the cnx to close
     */
    public void close(Enum<?> reason, String info, Throwable err);

    /**
    * Closes abruptly this mux connection.
    * @deprecated use {@link #shutdown(Enum, String, Throwable)}
    */
    public void shutdown();

    /**
     * Aborts a mux connection and provides some root cause informations describing why the connection is closed
     * @param reason the reason why the mux connection is closed
     * @param info the info about the mux close event.
     * @param err an optional throwable root cause
     * @param cnx the cnx to close
     */
    public void shutdown(Enum<?> reason, String info, Throwable err); 

    /**
     * Returns this MuxConnection Id assigned by the CalloutAgent.
     * @return the MuxConnection Id.
     */
    public int getId();

    /**
     * Gets the input channel from which the last passed data by the MuxConnection is originated.
     * This value has a meaning when the double socket mux function is activated.
     * It returns always 0, when the double socket is not activated.
     * You can have the following returned values:
     * 0 - the data comes from the channel/socket with lowest priority,
     * 1 - the data comes from the channel/socket with higher priority,
     * Note: This interface can evolve, to offer more distinguish values in the future.
     */
    public int getInputChannel();

    /**
     * Gets the stack application Id.
     * @return the stack application Id.
     */
    public int getStackAppId();

    /**
     * Gets the stack application name.
     * @return the stack application name.
     */
    public String getStackAppName();

    /**
     * Gets the stack instance name.
     * @return the stack instance name.
     */
    public String getStackInstance();

    /**
     * Gets the stack host name.
     * @return the stack host name.
     */
    public String getStackHost();

    /**
     * Gets the stack address.
     * @return the stack address.
     */
    public String getStackAddress();

    /**
     * Gets the stack port number.
     * @return the stack port number.
     */
    public int getStackPort();
    
    /**
     * Get the Monitorable object associated with this connection
     * @return the Monitorable object. Can be null
     */
    public default Object getMonitorable() {
       return null;
    }

    /************************* attributes *************************/

    /**
     * Gets the SocketManager.
     */
    public SocketManager getSocketManager();

    /**
     * Sets the TimeoutManager.
     */
    public void setTimeoutManager(TimeoutManager manager);

    /**
     * Gets the TimeoutManager.
     */
    public TimeoutManager getTimeoutManager();

    /**
     * Gets the associated MuxHandler.
     */
    public MuxHandler getMuxHandler();

    /**
     * Sets the attributes.
     * @deprecated Use the attach method
     */
    @Deprecated
    public void setAttributes(Object[] attributes);

    /**
     * Gets the attributes.
     * @deprecated Use the attachment method
     */
    @Deprecated
    public Object[] getAttributes();

    /**
     * Attaches a context to this mux connection. This context can then be retrieved using the attachment method.
     * @param attachment the context to be attached to this mux connection.
     */
    public void attach(Object attachment);

    /**
     * Gets a context attached to this mux connection.
     * @return the context attached to this mux connection.
     */
    public <T> T attachment();

    /************************* mux *************************/

    /**
     * Notifies the Stack that the MuxHandler is ready.
     * <br/>The Stack behavior is protocol-specific.
     */
    public boolean sendMuxStart();

    /**
     * Notifies the Stack that the MuxHandler is shutting down.
     * <br/>The Stack behavior is protocol-specific.
     */
    public boolean sendMuxStop();

    /**
     * Sends opaque mux data.
     */
    public boolean sendMuxData(MuxHeader header, byte[] data, int off, int len, boolean copy);

    /**
     * Sends opaque mux data using a  byte buffer.
     * @param copy true if the buffer must be copied when the buffer is enqueued while the socket is full.
     */
    public boolean sendMuxData(MuxHeader header, boolean copy, ByteBuffer... buf);

    /**
     * Sends opaque mux data using a  byte buffer. The buffer will be copied if the mux socket is full.
     * @deprecated use {@link #sendMuxData(MuxHeader, boolean, ByteBuffer...)}
     */
    @Deprecated
    public boolean sendMuxData(MuxHeader header, ByteBuffer buf);

    /**
     * Sends Mux Identification. 
     */
    public boolean sendMuxIdentification(MuxIdentification id);

    /************************* TCP *************************/

    /**
     * Sends a TCP Socket Listen request.
     */
    public boolean sendTcpSocketListen(long listenId, int localIP, int localPort, boolean secure);

    /**
     * Sends a TCP Socket Listen request (IPv6 compatible).
     */
    public boolean sendTcpSocketListen(long listenId, String localIP, int localPort, boolean secure);

    /**
     * Sends a TCP Socket Connect request.
     */
    public boolean sendTcpSocketConnect(long connectionId, String remoteHost, int remotePort, int localIP,
                                        int localPort, boolean secure);

    /**
     * Sends a TCP Socket Connect request (IPv6 compatible).
     */
    public boolean sendTcpSocketConnect(long connectionId, String remoteHost, int remotePort, String localIP,
                                        int localPort, boolean secure);
    /**
     * Sends a TCP Socket Connect request (IPv6 compatible) with parameters (may be null).
     */
    public boolean sendTcpSocketConnect(long connectionId, String remoteHost, int remotePort, String localIP,
                                        int localPort, boolean secure, java.util.Map<String, String> params);

    /**
     * Sends a TCP Socket Close request.
     */
    public boolean sendTcpSocketClose(int sockId);
    /**
     * Sends a TCP Socket Reset request.
     */
    public boolean sendTcpSocketReset(int sockId);

    /**
     * Sends a Tcp Socket Abort request. The contract of this API depends on the stack this mux connection is using.
     * For stacks supporting shared sockets, aborting a shared socket will force the socket close, and the stack will then
     * advertise the close event to other agents, using {@link MuxHandler#tcpSocketAborted(MuxConnection, int)}. Else, 
     * the contract is the same as the {@link #sendTcpSocketClose(int)} method. In any case, the caller of this method 
     * is called back in {@link MuxHandler#tcpSocketAborted(MuxConnection, int)} method.
     * 
     * @param sockId The Mux Connection socket identifier
     * @return true if the socket abort message has been sent to the remote stack, false if not (if an error occured).
     */
    public boolean sendTcpSocketAbort(int sockId);

    /**
     * Sends Tcp data.
     */
    public boolean sendTcpSocketData(int sockId, byte[] data, int off, int len, boolean copy);

    /**
     * Sends TCP data using some  byte buffers.
     * 
     * @param copy true if the buffer must be copied when the buffer is enqueued while the socket is full.
     */
    public boolean sendTcpSocketData(int sockId, boolean copy, ByteBuffer... bufs);

    /**
     * Sets Tcp socket parameters.
     * @param sockId the id of the sctp socket.
     * @param params a map of parameters
     * @return true if the parameters were sent, false on any send errors.
     */
    public boolean sendTcpSocketParams(int sockId, java.util.Map<String, String> params);
    
    /************************* sctp *************************/

    /**
     * Sends a Sctp Socket Listen request for accepting sctp connections (IPv6 compatible).
     * The MuxHandler will be called backed in 
     * {@link MuxHandler#sctpSocketListening(MuxConnection, int, long, String[], int, boolean, int)}.
     * When a remote client connection is accepted, then the 
     * {@link MuxHandler#sctpSocketConnected(MuxConnection, int, long, String[], int, String[], int, int, int, boolean, boolean, int)}
     * callback is invoked with the <code>fromClient</code> parameter set to <code>true</code>.
     * 
     * @param listenId a unique long identifying the sctp socket listener.
     * @param localAddrs the local addresses to listen to.
     * @param localPort the local port to listen to. This localPort is applied to all provided local addresses.
     * @param maxOutStreams the maximum number of outbound streams
     * @param maxInStreams the maximum number of inbound streams
     * @param secure true for DTLS use
     * @return false on any errors, true if the operation succeeded. If false, the mux handler won't be called back.
     */
    public boolean sendSctpSocketListen(long listenId, String[] localAddrs, int localPort, int maxOutStreams,
                                        int maxInStreams, boolean secure);

    /**
     * Sends a Sctp Socket Connect request (IPv6 compatible).
     * The MuxHandler will be called backed in 
     * {@link MuxHandler#sctpSocketConnected(MuxConnection, int, long, String[], int, String[], int, int, int, boolean, boolean, int)}
     * with the parameter <code>fromClient</code> set to <code>false</code>.
     *
     * @param connectionId a unique long identifying the connection request.
     * @param remoteHost The remote peer address to which this channel is to be connected to.
     * @param remotePort The remote peer port number to which this channel is to be connected to.
     * @param localAddrs The bound addresses for the channel's socket (null to use a default local address).
     * @param localPort The bound local port number for the channel's socket (-1 to choose a default one).
     * @param maxOutStreams Must be non negative and no larger than 65536. 0 to use the end points default value.
     * @param maxInStreams Must be non negative and no larger than 65536. 0 to use the end points default value.
     * @param secure true for DTLS use
     * @return true if the operation succeeded, false if not.
     */
    public boolean sendSctpSocketConnect(long connectionId, String remoteHost, int remotePort,
                                         String[] localAddrs, int localPort, int maxOutStreams,
                                         int maxInStreams, boolean secure);
    /**
     * Sends a Sctp Socket Connect request (IPv6 compatible).
     * The MuxHandler will be called backed in 
     * {@link MuxHandler#sctpSocketConnected(MuxConnection, int, long, String[], int, String[], int, int, int, boolean, boolean, int)}
     * with the parameter <code>fromClient</code> set to <code>false</code>.
     *
     * @param connectionId a unique long identifying the connection request.
     * @param remoteHost The remote peer address to which this channel is to be connected to.
     * @param remotePort The remote peer port number to which this channel is to be connected to.
     * @param localAddrs The bound addresses for the channel's socket (null to use a default local address).
     * @param localPort The bound local port number for the channel's socket (-1 to choose a default one).
     * @param maxOutStreams Must be non negative and no larger than 65536. 0 to use the end points default value.
     * @param maxInStreams Must be non negative and no larger than 65536. 0 to use the end points default value.
     * @param secure true for DTLS use
     * @param sctpSocketOptions a map of SctpSocketOption / SctpSocketParam
     * @param params a map of sctp channel parameters (may be null)
     * @return true if the operation succeeded, false if not.
     */
    public boolean sendSctpSocketConnect(long connectionId, String remoteHost, int remotePort,
                                         String[] localAddrs, int localPort, int maxOutStreams,
                                         int maxInStreams, boolean secure, java.util.Map<SctpSocketOption, SctpSocketParam> sctpSocketOptions, java.util.Map<String, String> params);
    
    /**
     * Gracefully closes an sctp socket. The MuxHandler will be called back in {@link MuxHandler#sctpSocketClosed(MuxConnection, int)} when
     * the connection has been shot down.
     *
     * @param sockId the id of the socket to be gracefully closed.
     * @return true if the operation succeeded, false if not.
     */
    public boolean sendSctpSocketClose(int sockId);

    /**
     * Resets an sctp socket. The MuxHandler will be called back in {@link MuxHandler#sctpSocketClosed(MuxConnection, int)} when
     * the connection has been shot down.
     *
     * @param sockId the id of the socket to reset
     * @return true if the operation succeeded, false if not.
     */
    public boolean sendSctpSocketReset(int sockId);

    /**
     * Sends Sctp data.
     * 
     * @param sockId the id of the sctp socket to use when sending the data
     * @param addr the preferred destination of the message to be sent, or null if the primary address must be used
     * @param unordered Sets whether or not the message is unordered
     * @param complete Sets whether or not the message is complete
     * @param ploadPID the pay load protocol Identifier, or 0 indicate an unspecified pay load protocol 
     *        identifier.
     * @param streamNumber the stream number that the message is to be sent on.
     * @param timeToLive The time period that the sending side may expire the message if it 
     *        has not been sent, or 0 to indicate that no timeout should occur.
     * @param copy true if the buffer must be copied when the buffer is enqueued while the socket is full.
     * @param data the data message
     * @return true if the data was sent, false on any send errors.
     */
	public boolean sendSctpSocketData(int sockId, String addr, boolean unordered, boolean complete, int ploadPID, int streamNumber, long timeToLive,
			boolean copy, ByteBuffer... data);

    /**
     * Sets Sctp socket options.
     * 
     * @param sockId the id of the sctp socket.
     * @param sctpSocketOptions a map of SctpSocketOption / SctpSocketParam
     * @return true if the options were sent, false on any send errors.
     */
    public boolean sendSctpSocketOptions(int sockId, java.util.Map<SctpSocketOption, SctpSocketParam> sctpSocketOptions);
    /**
     * Sets Sctp socket parameters.
     * @param sockId the id of the sctp socket.
     * @param params a map of parameters
     * @return true if the parameters were sent, false on any send errors.
     */
    public boolean sendSctpSocketParams(int sockId, java.util.Map<String, String> params);

    /************************* UDP *************************/

    /**
     * Sends an UDP Socket Bind request.
     */
    public boolean sendUdpSocketBind(long bindId, int localIP, int localPort, boolean shared);

    /**
     * Sends an UDP Socket Bind request.
     */
    public boolean sendUdpSocketBind(long bindId, String localIP, int localPort, boolean shared);

    /**
     * Sends an UDP Socket Close request.
     */
    public boolean sendUdpSocketClose(int sockId);

    /**
     * Sends UDP data.
     */
    public boolean sendUdpSocketData(int sockId, int remoteIP, int remotePort, int virtualIP,
                                     int virtualPort, byte[] data, int off, int len, boolean copy);

    /**
     * Sends UDP data.
     */
    public boolean sendUdpSocketData(int sockId, int remoteIP, int remotePort, int virtualIP,
                                     int virtualPort, boolean copy, ByteBuffer... bufs);

    /**
     * Sends UDP data.
     */
    public boolean sendUdpSocketData(int sockId, String remoteIP, int remotePort, String virtualIP,
                                     int virtualPort, byte[] data, int off, int len, boolean copy);

    /**
     * Sends UDP data.
     */
    public boolean sendUdpSocketData(int sockId, String remoteIP, int remotePort, String virtualIP,
                                     int virtualPort, boolean copy, ByteBuffer... bufs);

    /************************* DNS *************************/

    /**
     * Sends a DNS getByAddr request.
     */
    public boolean sendDnsGetByAddr(long reqId, String addr);

    /**
     * Sends a DNS getByName request.
     */
    public boolean sendDnsGetByName(long reqId, String name);

    /************************* release *************************/

    /**
     * Sends a Session release request.
     */
    public boolean sendRelease(long sessionId);

    /**
     * Sends a Session release confirm/cancel.
     */
    public boolean sendReleaseAck(long sessionId, boolean confirm);

    /**
     * Disable socket read on the mux connection. This method can be used in order to regulate
     * flow control: when called, the socket won't be read anymore, until you invoke the 
     * {@link #enableRead(int)} method.
     * 
     * @param sockId the mux connection id
     */
    public void disableRead(int sockId);

    /**
     * Enable socket read on the mux connection. This method can be used in order to regulate
     * flow control: when called, the socket won't be read anymore, until you invoke the 
     * {@link #disableRead(int)} method.
     * 
     * @param sockId the mux connection id
     */
    public void enableRead(int sockId);
}
