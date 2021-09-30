package com.nextenso.mux;

import java.nio.ByteBuffer;
import java.util.Hashtable;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.utils.Config;
import alcatel.tess.hometop.gateways.utils.ConfigException;
import alcatel.tess.hometop.gateways.utils.Parameters.Type;

import com.nextenso.mux.event.MuxMonitorable;
import com.nextenso.mux.socket.TcpMessageParser;
import com.nextenso.mux.util.DNSManager;
import com.nextenso.mux.util.MuxHandlerMeters;

/**
 * The class to extend to implement an Agent.
 * <p/>
 * A MuxHandler is called back by one or more MuxConnections.
 * <p/>The main features are:
 * <p/>
 * <b>Agent Management</b>
 * <br/>All the methods used to integrate the agent into the super-agent.
 * <ul>
 * <li><code>init(Config cnf)</code>
 * <li><code>destroy()</code>
 * </ul>
 * <p/>
 * <b>Monitoring</b>
 * <br/>All the methods used to monitor the agent.
 * <ul>
 * <li><code>commandEvent(int command, int[] intParams, String[] strParams)</code>
 * <li><code>getCounters()</code>
 * <li><code>getMajorVersion()</code>
 * <li><code>getMinorVersion()</code>
 * <li><code>muxGlobalEvent(int identifierI, String identifierS, byte[] data, int off, int len)</code>
 * <li><code>muxLocalEvent(int identifierI, String identifierS, Object data, boolean asynchronous)</code>
 * </ul>
 * <p/>
 * <b>Mux Management</b>
 * <br/>All the methods used to manage the mux connection (open, close, opaque data).
 * <ul>
 * <li><code>muxOpened(MuxConnection connection)</code>
 * <li><code>muxClosed(MuxConnection connection)</code>
 * <li><code>muxData(MuxConnection connection, MuxHeader header, byte[] data, int off, int len)</code>
 * </ul>
 * <b>Tcp Management</b>
 * <br/>All the methods used to handle tcp (socket open, socket close, tcp data).
 * <ul>
 * <li><code>tcpSocketListening(MuxConnection connection, int sockId, int localIP, int localPort, boolean secure, long listenId, int errno)</code>
 * <li><code>tcpSocketListening(MuxConnection connection, int sockId, String localIP, int localPort, boolean secure, long listenId, int errno)</code>
 * <li><code>tcpSocketConnected(MuxConnection connection, int sockId, int remoteIP, int remotePort, int localIP, int localPort, int virtualIP, int virtualPort, boolean secure, boolean clientSocket, long connectionId, int errno)</code>
 * <li><code>tcpSocketConnected(MuxConnection connection, int sockId, String remoteIP, int remotePort, String localIP, int localPort, String virtualIP, int virtualPort, boolean secure, boolean clientSocket, long connectionId, int errno)</code>
 * <li><code>tcpSocketClosed(MuxConnection connection, int sockId)</code>
 * <li><code>tcpSocketAborted(MuxConnection connection, int sockId)</code>
 * <li><code>tcpSocketData(MuxConnection connection, int sockId, long sessionId, byte[] data, int off, int len)</code>
 * </ul>
 * <b>Sctp Management</b>
 * <br/>All the methods used to handle sctp (socket open, socket close, sctp data).
 * <ul>
 * <li><code>sctpSocketListening(MuxConnection cnx, int sockId, long listenerId, String[] localAddrs, int localPort, boolean secure, int errno)</code>
 * <li><code>sctpSocketConnected(MuxConnection cnx, int sockId, long listenerId, String[] remoteAddrs, int remotePort, String[] localAddrs, int localPort, int maxOutStreams, int maxInStreams, boolean fromClient, boolean secure, int errno)</code>
 * <li><code>sctpSocketData(MuxConnection cnx, int sockId, long sessionId, ByteBuffer data, String addr, boolean isUnordered, int ploadPID, int streamNumber)</code>
 * <li><code>sctpSocketClosed(MuxConnection cnx, int sockId)</code>
 * <li><code>sctpSocketSendFailed(MuxConnection cnx, int sockId, String addr, int streamNumber, ByteBuffer buf, int errcode)</code>
 * <li><code>sctpAssociationChanged(MuxConnection cnx, int sockId, SctpAssociationEvent event)</code>
 * <li><code>sctpPeerAddressChanged(MuxConnection cnx, int sockId, String addr, SctpAddressEvent event)</code>
 * </ul>
 * <b>Udp Management</b>
 * <br/>All the methods used to handle udp (socket bind, socket close, udp data).
 * <ul>
 * <li><code>udpSocketBound(MuxConnection connection, int sockId, int localIP, int localPort, boolean shared, long bindId, int errno)</code>
 * <li><code>udpSocketClosed(MuxConnection connection, int sockId)</code>
 * <li><code>udpSocketData(MuxConnection connection, int sockId, long sessionId, int remoteIP, int remotePort, int virtualIP, int virtualPort, byte[] data, int off, int len)</code>
 * </ul>
 * <p/>
 * <b>Accepting a MuxConnection</b>
 * <br/>A MuxConnection Object encapsulates a Mux Socket with a Stack. When a Stack starts up, a MuxConnection Object is instanciated and it is submitted to each MuxHandler. When a MuxHandler accepts the MuxConnection, the MuxConnection is bound to it, and all the Mux Socket trafic is forwarded to the MuxHandler. The steps to see if a MuxHandler accepts a MuxConnection are:
 * <ul>
 * <li><code>getMuxConfiguration()</code>
 * <li><code>accept(int stackAppId, String stackName, String stackHost, String stackInstance)</code>
 * </ul>
 */
@SuppressWarnings("unused")
public abstract class MuxHandler implements MuxMonitorable
{
    /**
     * The configuration key specifying the number of MuxConnections this MuxHandler can serve.
     * <br/>The value must be an Integer. -1 is the default value and stands for 'Unlimited'.
     */
    public static final Object CONF_CONNECTION_NUMBER = new Object();
    /**
     * The configuration key specifying if this MuxHandler is thread safe or not. A thread safe
     * mux handler may be invoked by multiple threads simultaneously. 
     * False is the default value.
     */
    public static final Object CONF_THREAD_SAFE = new Object();
    /**
     * The configuration key specifying if this MuxHandler wants the MuxConnection to demultiplex the Mux Socket.
     * <br/>The value must be a Boolean. True is the default value.
     */
    public static final Object CONF_DEMUX = new Object();
    /**
     * The configuration key specifying the Stack Application Ids this MuxHandler can serve.
     * <br/>The value must be an int[]. int[0] is the default value and stands for 'Any'.
     */
    public static final Object CONF_STACK_ID = new Object();
    /**
     * The configuration key specifying the Stack Instance Names this MuxHandler can serve.
     * <br/>The value must be a String or a String[]. String[0] is the default value and stands for 'Any'.
     */
    public static final Object CONF_STACK_INSTANCE = new Object();
    /**
     * The configuration key specifying the Stack Application Names this MuxHandler can serve.
     * <br/>The value must be a String or a String[]. String[0] is the default value and stands for 'Any'.
     */
    public static final Object CONF_STACK_NAME = new Object();
    /**
     * The configuration key specifying the Stack Host Names this MuxHandler can serve.
     * <br/>The value must be a String or a String[]. String[0] is the default value and stands for 'Any'.
     */
    public static final Object CONF_STACK_HOST = new Object();
    /**
     * The configuration key specifying the Stack IP address this MuxHandler can serve.
     * <br/>This property must be used in conjonction with CONF_EXTERNAL_STACKS.
     * <br/>The value must be a String or a String[]. String[0] is the default value and stands for 'Any'.
     */
    public static final Object CONF_STACK_ADDRESS = new Object();
    /**
     * The configuration key specifying the Stack IP port numbers this MuxHandler can serve.
     * <br/>This property must be used in conjonction with CONF_EXTERNAL_STACKS.
     * <br/>The value must be an Integer or an Integer array. Integer[0] is the default value and stands for 'Any'.
     */
    public static final Object CONF_STACK_PORT = new Object();
    /**
     * The configuration key specifying the external Stacks this mux handler will connect to.
     * <br/>The value must be Map which contains reuses the properties CONF_STACK_*. null is the default value and 
     * stands for 'No External Stack'.
     */
    public static final Object CONF_EXTERNAL_STACKS = new Object();
    /**
     * The configuration key specifying the delay between the shutdown order and the effective System exit.
     * <br/>The value must be a Long. '0' is the default value and means 'no delay'.
     */
    public static final Object CONF_EXIT_DELAY = new Object();
    /**
     * The configuration key specifying the keepAlive interval in milliseconds used to check the connection.
     * <br/>The value must be a Long.
     * <br/>'-1' is the default value and means 'default value (usually configurable)'.
     * <br/>'0' means 'no keepAlive'.
     * <br/>Any positive value requires the stack to send keepAlive messages at the same frequency.
     */
    public static final Object CONF_KEEP_ALIVE = new Object();
    /**
     * The configuration key specifying the keepAlive idle factor in milliseconds used to check the connection.
     * <br/>The value must be a Long.
     * <br/>0 or '-1' is the default value and means 'default value (usually configurable)'.
     * <br/>Any positive value indicates an idle factor as needed by the algorithm used to compute
     * the delay after wich one consider the connection death.
     */
    public static final Object CONF_ALIVE_IDLE_FACTOR = new Object();
    /**
     * The configuration key specifying if MuxHandler's nio related methods must called or not.
     * <br/>The value must be a Boolean.
     * <br/>'False' is the default value and means 'calls the byte array related method'
     * <br/>'True' means 'calls the nio related method'
     */
    public static final Object CONF_USE_NIO = new Object();
    /**
     * The configuration key specifying IPv6 support.
     * <br/>The value must be a Boolean.
     * <br/>'False' is the default value and means that tcpSocketXXX/udpSocketXXX method are always invoked with
     * an integer for ip addresse representation.
     * <br/>'True' means that tcpSocketXXX/udpSocketXXX method are always invoked with
     * a string for ip addresse representation.
     */
    public static final Object CONF_IPV6_SUPPORT = new Object();
    /**
     * The configuration key specifying a tcp parser. Such parser is used when running this mux handler 
     * in a standalone environment, without actual remote io handler. The parser will have to parse all 
     * incoming message. 
     * <br/>The value must be an instance of a {@link TcpMessageParser}.
     * <br/>null is the default value and means that no tcp parser is supported by this mux handler.
     */
    public static final Object CONF_TCP_PARSER = new Object();
    /**
     * The configuration key specifying if the MuxHandler wants to control the sending of MuxStart when a MuxConnection is opened.
     * This can introduce a delay : the stack will not send traffic until the MuxStart is sent.
     * <br/>The value must be a Boolean
     * <br/>default is False : the MuxHandler will not send a MuxStart (hence the MuxStart is implied and sent to the stack automatically when connected).
     */
    public static final Object CONF_MUX_START = new Object();    
    
    /**
     * The configuration key specifying what protocols the MuxHandler is interested in. At this time,
     * this simply create the metrics for the passed protocols in the Monitorable associated with the
     * MuxConnection.
     * <br/>The value must be a String[]. Valid values include : "tcp", "udp", "sctp". 
     * Default value is null which means the MuxHandler is assumed to use all protocols
     */
    public static final Object CONF_L4_PROTOCOLS = new Object();
    
    /**
     * The configuration key specifying the metrics object for this MuxHandler. It will be used by  each 
     * MuxConnection to aggregate metrics. If not present, no aggregations will be done.
     * <br/> The value must be of type {@link MuxHandlerMeters}
     */
    public static final Object CONF_HANDLER_METERS = new Object();
    

    /**
     * Events delivered when a peer address changed
     */
    public enum SctpAddressEvent
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
    }

    private int _appId;
    private String _appName, _appInstance;
    private MuxContext _muxContext;
    @SuppressWarnings("unchecked")
    private final Hashtable _muxConfiguration = new Hashtable();

    /** 
     * empty constructor
     * if used, setContextParameters() MUST be called before anything else
     */
    public MuxHandler()
    {
    }

    public MuxHandler(int appId, String appName, String appInstance, MuxContext muxContext)
    {
        init(appId, appName, appInstance, muxContext);
    }

    @SuppressWarnings("unchecked")
    public void init(int appId, String appName, String appInstance, MuxContext muxContext)
    {
        _appId = appId;
        _appName = appName;
        _appInstance = appInstance;
        _muxContext = muxContext;
        _muxConfiguration.put(CONF_CONNECTION_NUMBER, Integer.valueOf(-1));
        _muxConfiguration.put(CONF_THREAD_SAFE, Boolean.FALSE);
        _muxConfiguration.put(CONF_DEMUX, Boolean.TRUE);
        _muxConfiguration.put(CONF_STACK_ID, new int[0]);
        _muxConfiguration.put(CONF_STACK_INSTANCE, new String[0]);
        _muxConfiguration.put(CONF_STACK_NAME, new String[0]);
        _muxConfiguration.put(CONF_STACK_HOST, new String[0]);
        _muxConfiguration.put(CONF_STACK_ADDRESS, new String[0]);
        _muxConfiguration.put(CONF_STACK_PORT, new Integer[0]);
        _muxConfiguration.put(CONF_EXIT_DELAY, Long.valueOf(0));
        _muxConfiguration.put(CONF_KEEP_ALIVE, Long.valueOf(-1));
        _muxConfiguration.put(CONF_ALIVE_IDLE_FACTOR, Long.valueOf(2));
        _muxConfiguration.put(CONF_USE_NIO, Boolean.FALSE);
        _muxConfiguration.put(CONF_IPV6_SUPPORT, Boolean.FALSE);
    }

    @SuppressWarnings("unchecked")
    public Hashtable getMuxConfiguration()
    {
        return _muxConfiguration;
    }

    public boolean accept(int stackAppId, String stackName, String stackHost, String stackInstance)
    {
        return true;
    }

    public void init(Config cnf) throws ConfigException
    {
    }

    public void destroy()
    {
    }

    public final int getAppId()
    {
        return _appId;
    }

    public final String getAppName()
    {
        return _appName;
    }

    public final String getInstanceName()
    {
        return _appInstance;
    }

    public final MuxContext getMuxContext()
    {
        return _muxContext;
    }

    /************************* monitorable *************************/

    public void commandEvent(int command, int[] intParams, String[] strParams)
    {
    }

    public void muxGlobalEvent(int identifierI, String identifierS, byte[] data, int off, int len)
    {
    }

    public void muxLocalEvent(int identifierI, String identifierS, Object data)
    {
    }

    /************************* mux mgmt *************************/

    public void muxOpened(MuxConnection connection)
    {
    }

    public void muxClosed(MuxConnection connection)
    {
    }

    public void muxData(MuxConnection connection, MuxHeader header, byte[] data, int off, int len)
    {
    }

    public void muxData(MuxConnection connection, MuxHeader header, ByteBuffer data)
    {
    }

    /************************* tcp socket mgmt *************************/

    public void tcpSocketListening(MuxConnection connection, int sockId, int localIP, int localPort,
                                   boolean secure, long listenId, int errno)
    {
    }

    public void tcpSocketListening(MuxConnection connection, int sockId, String localIP, int localPort,
                                   boolean secure, long listenId, int errno)
    {
    }

    public void tcpSocketConnected(MuxConnection connection, int sockId, int remoteIP, int remotePort,
                                   int localIP, int localPort, int virtualIP, int virtualPort,
                                   boolean secure, boolean clientSocket, long connectionId, int errno)
    {
    }

    public void tcpSocketConnected(MuxConnection connection, int sockId, String remoteIP, int remotePort,
                                   String localIP, int localPort, String virtualIP, int virtualPort,
                                   boolean secure, boolean clientSocket, long connectionId, int errno)
    {
    }

    public void tcpSocketClosed(MuxConnection connection, int sockId)
    {
    }

    public void tcpSocketAborted(MuxConnection connection, int sockId)
    {
    }

    public void tcpSocketData(MuxConnection connection, int sockId, long sessionId, byte[] data, int off,
                              int len)
    {
    }

    public void tcpSocketData(MuxConnection connection, int sockId, long sessionId, ByteBuffer data)
    {
    }

    /************************* sctp socket mgmt ************************/

    /**
     * A sctp server socket is listening.
     * 
     * @param connexion The mux connection used to send the SCTP server socket listen command.
     * @param sockId The SCTP socket id allocated for this new server socket.
     * @param listenerId The listener id which was provided when sending the SCTP socket listen command.
     * @param localAddrs The socket addresses to which this channel's socket is bound.
     * @param localPort The socket port number to which this channel's socket is bound.
     * @param secure true for DTLS
     * @param errno A positive number on any errors (see MuxUtils.ERROR_XXX codes)
     */
    public void sctpSocketListening(MuxConnection connexion, int sockId, long listenerId, String[] localAddrs,
                                    int localPort, boolean secure, int errno)
    {
    }

    /**
     * A sctp client socket is connected (either on a client or a server socket).
     * 
     * @param connection the mux connection used to send the SCTP connect request command.
     * @param sockId the SCTP socket id allocated for this new connected socket.
     * @param connectionId the id which was provided when sending the sctp socket connect.
     * @param remoteAddrs the addresses of the remote peer
     * @param remotePort the remote peer port number
     * @param localAddrs The socket addresses to which this channel's socket is bound.
     * @param localPort the socket port number to which this channel's socket is bound.
     * @param maxOutStreams the negociated maximum number of outbound streams (
     * @param maxInStreams the negociated maximum number of inbound streams
     * @param fromClient true if a remote client connected has been initiated by a remote client, false if not. 
     * @param secure true for DTLS
     * @param errno a positive number on any errors (see MuxUtils.ERROR_XXX codes)
     */
    public void sctpSocketConnected(MuxConnection connection, int sockId, long connectionId, String[] remoteAddrs,
                                    int remotePort, String[] localAddrs, int localPort, int maxOutStreams,
                                    int maxInStreams, boolean fromClient, boolean secure, int errno)
    {
    }

    /**
     * Handles an incoming SCTP message.
     * 
     * @param connection The mux connection managing this SCTP connection.
     * @param sockId The identifier of the SCTP connection.
     * @param sessionId The sessionId allocated by the stack.
     * @param data The message.
     * @param addr The source address of the received message.
     * @param isUnordered Tells whether or not the message is unordered.
     * @param isComplete Indicates whether the message is complete.
     * @param ploadPID The payload protocol identifier.
     * @param streamNumber The stream number that the message was received on.
     */
    public void sctpSocketData(MuxConnection connection, int sockId, long sessionId, ByteBuffer data, String addr,
                               boolean isUnordered, boolean isComplete, int ploadPID, int streamNumber)
    {
    }

    /**
     * Notification emitted when a peers shutdowns an the association, or when the SCTP socket as been closed. 
     * When a peer sends a SHUTDOWN, the SCTP stack delivers this notification to inform the application that it should cease sending data
     * 
     * @param connection The channel concerned by this event.
     * @param sockId The identifier of the sctp connection.
     */
    public void sctpSocketClosed(MuxConnection connection, int sockId)
    {
    }

    /**
     * Notification emitted when a send failed notification has been received.
     * 
     * @param connection The mux connection concerned by the event.
     * @param sockId The identifier of the SCTP connection.
     * @param addr The peer primary address of the association or the address that the message was sent to
     * @param streamNumber The stream number that the messge was to be sent on.
     * @param buf The data that was to be sent.
     * @param errcode the error code. The errorCode gives the reason why the send failed, and if set, will 
     *        be a SCTP protocol error code as defined in RFC2960 section 3.3.10 
     */
    public void sctpSocketSendFailed(MuxConnection connection, int sockId, String addr, int streamNumber,
                                     ByteBuffer buf, int errcode)
    {
    }
    
    /**
     * Notification emmitted when an address change event occurred to the destination address on a multi-homed peer.
     * @param cnx the channel concerned by the event
     * @param addr the peer address concerned by the event
     * @param event the type of peer address change event
     */
    public void sctpPeerAddressChanged(MuxConnection cnx, int sockId, String addr, int port, SctpAddressEvent event) {      
    }

    /************************* udp socket mgmt *************************/

    public void udpSocketBound(MuxConnection connection, int sockId, int localIP, int localPort,
                               boolean shared, long bindId, int errno)
    {
    }

    public void udpSocketBound(MuxConnection connection, int sockId, String localIP, int localPort,
                               boolean shared, long bindId, int errno)
    {
    }

    public void udpSocketClosed(MuxConnection connection, int sockId)
    {
    }

    public void udpSocketData(MuxConnection connection, int sockId, long sessionId, int remoteIP,
                              int remotePort, int virtualIP, int virtualPort, byte[] data, int off, int len)
    {
    }

    public void udpSocketData(MuxConnection connection, int sockId, long sessionId, int remoteIP,
                              int remotePort, int virtualIP, int virtualPort, ByteBuffer data)
    {
    }

    public void udpSocketData(MuxConnection connection, int sockId, long sessionId, String remoteIP,
                              int remotePort, String virtualIP, int virtualPort, byte[] data, int off, int len)
    {
    }

    public void udpSocketData(MuxConnection connection, int sockId, long sessionId, String remoteIP,
                              int remotePort, String virtualIP, int virtualPort, ByteBuffer data)
    {
    }

    /************************* dns socket mgmt *************************/

    public void dnsGetByAddr(long reqId, String[] response, int errno)
    {
        DNSManager.notify(reqId, response, errno);
    }

    public void dnsGetByName(long reqId, String[] response, int errno)
    {
        DNSManager.notify(reqId, response, errno);
    }

    /************************* release mgmt *************************/

    public void releaseAck(MuxConnection connection, long sessionId)
    {
        connection.getTimeoutManager().ack(sessionId);
    }
}
