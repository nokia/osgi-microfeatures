// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.TimerService;
import com.alcatel.as.service.management.RuntimeStatistics;
import com.alcatel.as.util.sctp.SctpSocketOption;
import com.alcatel.as.util.sctp.SctpSocketParam;
import com.nextenso.mux.DNSParser;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxFactory;
import com.nextenso.mux.MuxFactory.ConnectionListener;
import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.MuxHeader;
import com.nextenso.mux.MuxHeaderV0;
import com.nextenso.mux.MuxHeaderV2;
import com.nextenso.mux.MuxHeaderV3;
import com.nextenso.mux.MuxHeaderV4;
import com.nextenso.mux.MuxHeaderV5;
import com.nextenso.mux.MuxHeaderV6;
import com.nextenso.mux.MuxProtocol;
import com.nextenso.mux.socket.SocketManager;
import com.nextenso.mux.util.MuxIdentification;
import com.nextenso.mux.util.MuxUtils;
import com.nextenso.mux.util.SocketManagerImpl;
import com.nextenso.mux.util.TimeoutManager;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.utils.IntHashtable;
import alcatel.tess.hometop.gateways.utils.Utils;

/**
 * Mux Connection nio implementation.
 */
@SuppressWarnings({ "serial", "unchecked" })
public class MuxConnectionImpl implements MuxConnection, MuxProtocol {
    
  /**
   * Mux Close root causes
   */
  public enum CloseReason {
    KeepAlive,
    InternalError
  }

  private final static String ASYNC_PING = "mux.ping.useThreadPool";
  private final static String LOAD_NORMAL = "com/alcatel_lucent/as/service/agent/overload/LOAD_NORMAL";
  private final static String LOAD_HIGH = "com/alcatel_lucent/as/service/agent/overload/LOAD_HIGH";
  private final static Logger _klvLogger = Logger.getLogger("as.service.mux.KeepAlive");
  private final static Logger _defaultLogger = Logger.getLogger("as.service.mux.MuxConnectionImpl");
  private final static AtomicInteger RND = new AtomicInteger(0);
  
  private final static byte[] MAGIC_TRAILER = new byte[4];
  private final static int DEFAULT_KEEP_ALIVE = 4000;
  private final static ByteBuffer MAGIC_TRAILER_BUFFER = ByteBuffer.allocate(4);
  private static EventAdmin _eventAdmin;
  
  static {
    MuxUtils.put_32(MAGIC_TRAILER, 0, MAGIC_TRAILER_VAL, false);
    MAGIC_TRAILER_BUFFER.order(ByteOrder.LITTLE_ENDIAN);
    MAGIC_TRAILER_BUFFER.putInt(MAGIC_TRAILER_VAL);
    MuxUtils.MUX_DATA_FLAGS_MIN_VALUE = PROTOCOL_OPAQUE;
    MuxUtils.MUX_DATA_FLAGS_MAX_VALUE = PROTOCOL_OPAQUE | ACTION_MASK;
  }
  
  private final RuntimeStatistics _runtimeStats;
  private Logger _logger = _defaultLogger;
  private Reactor _reactor;
  private final SocketManager _socketManager = new SocketManagerImpl();
  private TimeoutManager _timeoutManager;
  private Object[] _attrs;
  private Object _attachment;
  private long _connectionTimeout;
  private MuxFactory.ConnectionListener _connectionListener;
  private MuxHandler _handler;
  private MuxSocket _lowPrioritySocket = null;
  private MuxSocket _highPrioritySocket = null;
  private boolean _ignoreMux;
  private boolean _byteBufferMode;
  private boolean _ipv6Support;
  private volatile MuxKeepAlive _keepAlive;
  private int _stackId, _stackPort;
  private InetSocketAddress _localAddr;
  private InetSocketAddress _remoteAddr;
  private String _stackName, _stackHost, _stackInstance, _stackAddr;
  private IntHashtable _flagsTable;
  private int _id;
  private volatile Map<?,?> _muxOpts;
  private final TimerService _timer;
  private final PlatformExecutors _execs;
  private volatile boolean _muxOpened;
  
  // temporary fix: we have to mark the fact we a close is ongoing, because for SimpleMuxHandlers, we don't want yet to callback them in muxClosed
  // if the socket is being closed by the application.
  private volatile boolean _userClosing; 
  
  // List of Garbage Collectors MBeans (used to monitor the gc time).
  private final static List<GarbageCollectorMXBean> _oldMemCollectors = new ArrayList<>();    
  
  static {      
      // Find the available Old Mem garbage collectors, used to monitor old mem GC duration time.
      List<GarbageCollectorMXBean> garbageCollectors = ManagementFactory.getGarbageCollectorMXBeans();
      for (GarbageCollectorMXBean gc : garbageCollectors)
      {
          if ("PS MarkSweep".equals(gc.getName()) || "ConcurrentMarkSweep".equals(gc.getName()) || "G1 Old Generation".equals(gc.getName()))
          {
              _klvLogger.info("Found Old Memory Garbage Collector: " + gc.getName());
              _oldMemCollectors.add(gc);
          } else {
              _klvLogger.info("Ignoring Garbage Collector: " + gc.getName());
          }
      }
    }    
  
  /**
   * Log a mux connection close message
   * 
   * @param cnx the closed mux connection
   * @param logger the logger used to log the message
   * @param reason the reason why the mux connection is closed
   * @param info the info about the mux close event.
   * @param err an optional throwable root cause
   */
  public static void logClosingMux(MuxConnection cnx, Enum<?> reason, String info, Throwable err, Logger logger) {
    StringBuilder sb = new StringBuilder();
    sb.append("Closing mux cnx. Reason: ").append(reason);
    if (info != null) {
      sb.append(" (").append(info).append(")");
    }
    sb.append(". ");
    sb.append(cnx);          
    logger.warn(sb.toString(), err);
  }

  /**
   * Makes a new Mux Connection instance
   * 
   * @param reactor The Reactor used to manage this mux connection
   * @param mh the mux handler that will be called back with mux connection
   *          events
   * @param listener the listener used to track mux connection open/close
   * @param from the local address to bind
   * @param to the remote address to connect to
   * @param stackId the stack id of the stack to connect to
   * @param stackName the stack name of the stack to connect to
   * @param stackHost the host name of the stack to connect to
   * @param stackInstance the instance name of the stack to connect to
   * @param attachment a context to attach to this mux connection
   * @param flags the flag symbols used to log mux messages
   * @param logger the logger to be used by this mux connection
   * @param connectionTimeout the max duration in milliseconds until we wait for
   *          the connection to be opened, or 0L if no timeout has to be used.
   * @param mgmtRtStats the service used to retrieve informations (stats) on the
   *          runtime
   * @param opts 
   * @param inExec TODO
   * @param outExec TODO
   * @param ouExec TODO
   */
  public MuxConnectionImpl(Reactor reactor, MuxHandler mh, ConnectionListener listener,
                           InetSocketAddress from, InetSocketAddress to, int stackId, String stackName,
                           String stackHost, String stackInstance, Object attachment, IntHashtable flags,
                           Logger logger, long connectionTimeout, RuntimeStatistics runtimeStats, TimerService timer, 
                           Map opts, PlatformExecutors execs) {
    _muxOpts = opts;
    _connectionTimeout = connectionTimeout;
    _reactor = reactor;
    _handler = mh;
    _connectionListener = listener;
    _localAddr = from;
    _remoteAddr = to;
    _stackAddr = _remoteAddr != null ? _remoteAddr.getAddress().getHostAddress() : null;
    _stackPort = _remoteAddr != null ? _remoteAddr.getPort() : 0;
    _stackId = stackId;
    _stackName = stackName;
    _stackHost = stackHost;
    _stackInstance = stackInstance;
    _flagsTable = flags;
    _logger = logger == null ? _defaultLogger : logger;
    _attachment = attachment;
    _runtimeStats = runtimeStats;
    _timer = timer;
    _execs = execs;
    
    _id = getNextRnd();
    if (_handler != null) {
      Hashtable<Object, Object> muxConf = _handler.getMuxConfiguration();
      _ignoreMux = (!((Boolean) muxConf.get(MuxHandler.CONF_DEMUX)).booleanValue());
      _byteBufferMode = ((Boolean) muxConf.get(MuxHandler.CONF_USE_NIO)).booleanValue();
      _ipv6Support = ((Boolean) muxConf.get(MuxHandler.CONF_IPV6_SUPPORT)).booleanValue();
      
      int initialKeepAliveInterval = (int) ((Long) muxConf.get(MuxHandler.CONF_KEEP_ALIVE)).longValue();
      int initialIdleFactor = (int) ((Long) muxConf.get(MuxHandler.CONF_ALIVE_IDLE_FACTOR)).longValue();
      if (initialKeepAliveInterval > 0) {
        _keepAlive = new MuxKeepAlive(initialKeepAliveInterval, initialIdleFactor);
        // we'll schedule it once we are connected or when we'll accept.
      }
    }
  }
  
  public Map getMuxOptions() {
    return _muxOpts;
  }
    
  static synchronized void setEventAdmin(EventAdmin eventAdmin) {
    _eventAdmin = eventAdmin;
  }
  
  @Override
  public String toString() {
    StringBuilder buff = new StringBuilder();
    buff.append("MuxConnection [id=");
    buff.append(_id);
    if (_lowPrioritySocket != null) {
      buff.append(", local=");
      buff.append(_lowPrioritySocket.toString());
    }
    buff.append(", stackId=");
    buff.append(_stackId);
    buff.append(", stackName=");
    buff.append(_stackName);
    buff.append(", stackInstance=");
    buff.append(_stackInstance);
    buff.append(", stackHost=");
    buff.append(_stackHost);
    buff.append(", stackAddr=");
    buff.append(_stackAddr);
    buff.append(", stackPort=");
    buff.append(_stackPort);
    buff.append(']');
    return buff.toString();
  }
  
  String toLogString() {
    StringBuilder buff = new StringBuilder();
    buff.append("['").append(_stackInstance);
    buff.append("'");
    if (_lowPrioritySocket != null) {
      buff.append(",");
      buff.append(_lowPrioritySocket.toString());
    }
    buff.append(']');
    return buff.toString();
  }
  
  /************************************* MuxConnection interface *****************************/
  
  @Override
  public boolean isOpened() {
    return _muxOpened;
  }
  
  @Override
  public void close() {
    close(null, null, null);
  }
  
  public void close(final Enum<?> reason, final String info, final Throwable err) {
    _reactor.scheduleNow(new Runnable() { public void run() { close(reason, info, err, false /* don't shutdown */, false); }});
  }
  
  @Override
  public void shutdown() {
    shutdown(null, null, null);
  }
  
  public void shutdown(final Enum<?> reason, final String info, final Throwable err) {
    _reactor.scheduleNow(new Runnable() { public void run() {  close(reason, info, err, true /* abort the socket */, false); }});
  }
  
  @Override
  public int getId() {
    return _id;
  }
  
  @Override
  public int getInputChannel() {
    throw new RuntimeException("Method not implemented");
  }
  
  @Override
  public boolean setKeepAlive(final int interval, final int idleFactor) {
    if (_muxOpened) {
      _reactor.scheduleNow(new Runnable() { public void run() { 
        if (_keepAlive != null) {
          _keepAlive.cancel();
          _keepAlive = null;
        }
        if (interval > 0 && idleFactor > 0) {
          _keepAlive = new MuxKeepAlive(interval * 1000, idleFactor);
          _keepAlive.schedule();
        }
      }});
      return true;
    }
    
    return false;
  }
  
  @Override
  public boolean useKeepAlive() {
    return _keepAlive != null;
  }
  
  @Override
  public int getStackAppId() {
    return _stackId;
  }
  
  @Override
  public String getStackAppName() {
    return _stackName;
  }
  
  @Override
  public String getStackInstance() {
    return _stackInstance;
  }
  
  @Override
  public String getStackHost() {
    return _stackHost;
  }
  
  @Override
  public String getStackAddress() {
    return _stackAddr;
  }
  
  @Override
  public int getStackPort() {
    return _stackPort;
  }
  
  @Override
  public InetSocketAddress getLocalAddress() {
    return _localAddr;
  }
  
  @Override
  public InetSocketAddress getRemoteAddress() {
    return _remoteAddr;
  }
  
  /************************* attributes *************************/
  
  @Override
  public SocketManager getSocketManager() {
    return _socketManager;
  }
  
  @Override
  public void setTimeoutManager(TimeoutManager manager) {
    _timeoutManager = manager;
  }
  
  @Override
  public TimeoutManager getTimeoutManager() {
    return _timeoutManager;
  }
  
  @Override
  public MuxHandler getMuxHandler() {
    return _handler;
  }
  
  @Deprecated
  @Override
  public void setAttributes(Object[] attrs) {
    _attrs = attrs;
  }
  
  @Deprecated
  @Override
  public Object[] getAttributes() {
    return _attrs;
  }
  
  @Override
  public void attach(Object attachment) {
    _attachment = attachment;
  }
  
  @Override
  public <T> T attachment() {
    return (T) _attachment;
  }
  
  /************************* mux *************************/
  
  @Override
  public boolean sendMuxStart() {
		boolean status;
		if (connectionListenerCallback(() -> _connectionListener.muxStarted(this))) {
			_logger.info("sendMuxStart: connectionListener acknoledged, sending mux start");
			status = sendV4(_lowPrioritySocket, 0L, 0, PROTOCOL_CTL | ACTION_START, false, (ByteBuffer[]) null);
		} else {
			_logger.info("sendMuxSart: connectionListener rejected, sending mux stop");
			status = sendV4(_lowPrioritySocket, 0L, 0, PROTOCOL_CTL | ACTION_STOP, false, (ByteBuffer[]) null);
		}
		return status;
  }
  
  @Override
  public boolean sendMuxStop() {
		boolean status;
		if (connectionListenerCallback(() -> _connectionListener.muxStopped(this))) {
			_logger.info("sendMuxStop: connectionListener acknoledged, sending mux stop");
			status = sendV4(_lowPrioritySocket, 0L, 0, PROTOCOL_CTL | ACTION_STOP, false, (ByteBuffer[]) null);
		} else {
			_logger.info("sendMuxStop: connectionListener rejected, sending mux start");
			status = sendV4(_lowPrioritySocket, 0L, 0, PROTOCOL_CTL | ACTION_START, false, (ByteBuffer[]) null);
		}
		return status;
  }
  
  @Override
  public boolean sendMuxData(MuxHeader header, byte[] data, int off, int len, boolean copy) {
    if (data == null || len == 0) {
      return sendMuxData(header, false, (ByteBuffer[]) null);
    }
    ByteBuffer buf;
    buf = ByteBuffer.wrap(data, off, len);
    return sendMuxData(header, copy, buf);
  }
  
  @Deprecated
  @Override
  public boolean sendMuxData(MuxHeader header, ByteBuffer buf) {
    return sendMuxData(header, true, buf);
  }
  
  @Override
  public boolean sendMuxData(MuxHeader header, boolean copy, ByteBuffer ... bufs) {
    if (! _muxOpened) {
      return false;
    }
    
    int remaining = getRemaining(bufs);
    switch (header.getVersion()) {
    case 0:
      if (remaining > V0_CONTENT_SIZE_MAX) {
        return sendBigMessage(_lowPrioritySocket, header, V0_CONTENT_SIZE_MAX, copy, bufs);
      }
      return sendV0(_lowPrioritySocket, header.getSessionId(), header.getChannelId(), header.getFlags(),
                    copy, bufs);
      
    case 6:
      return sendV6(_lowPrioritySocket, header.getSessionId(), header.getChannelId(), header.getFlags(),
                    copy, bufs);
      
    case 5:
      return sendV5(_lowPrioritySocket, header.getFlags(), copy, bufs);
      
    case 4:
      if (remaining > V4_CONTENT_SIZE_MAX) {
        return sendBigMessage(_lowPrioritySocket, header, V4_CONTENT_SIZE_MAX, copy, bufs);
      }
      return sendV4(_lowPrioritySocket, header.getSessionId(), header.getChannelId(), header.getFlags(),
                    copy, bufs);
    case 2:
      if (remaining > V2_CONTENT_SIZE_MAX) {
        return sendBigMessage(_lowPrioritySocket, header, V2_CONTENT_SIZE_MAX, copy, bufs);
      }
      MuxHeaderV2 h2 = (MuxHeaderV2) header;
      return sendV2(_lowPrioritySocket, h2.getSessionId(), h2.getChannelId(), h2.getFlags(),
                    h2.getRemoteIP(), h2.getRemotePort(), h2.getVirtualIP(), h2.getVirtualPort(), copy, bufs);
    case 3:
      return sendV3(_lowPrioritySocket, header.getSessionId(), header.getChannelId(), header.getFlags(),
                    copy, bufs);
      
    default:
      return false;
    }
  }
  
  private boolean sendBigMessage(MuxSocket muxSocket, MuxHeader header, int maxSize, boolean copy,
                                 ByteBuffer ... bufs) {
    for (ByteBuffer buffer : bufs) {
      int pos = buffer.position();
      int len = buffer.remaining();
      do {
        boolean lastChunk = (len <= maxSize);
        int chunkLength = (lastChunk) ? len : maxSize;
        buffer.position(pos);
        buffer.limit(pos + chunkLength);
        ByteBuffer chunk = buffer.slice();
        
        switch (header.getVersion()) {
        case 0:
          if (!sendV0(muxSocket, header.getSessionId(), header.getChannelId(), header.getFlags(), copy, chunk)) {
            return false;
          }
          break;
        
        case 2:
          MuxHeaderV2 h2 = (MuxHeaderV2) header;
          if (!sendV2(_lowPrioritySocket, h2.getSessionId(), h2.getChannelId(), h2.getFlags(),
                      h2.getRemoteIP(), h2.getRemotePort(), h2.getVirtualIP(), h2.getVirtualPort(), copy,
                      chunk)) {
            return false;
          }
          break;
        
        case 3:
          if (!sendV3(muxSocket, header.getSessionId(), header.getChannelId(), header.getFlags(), copy, chunk)) {
            return false;
          }
          break;
        
        case 4:
          if (!sendV4(muxSocket, header.getSessionId(), header.getChannelId(), header.getFlags(), copy, chunk)) {
            return false;
          }
          break;
        
        case 5:
          if (!sendV5(muxSocket, header.getFlags(), copy, chunk)) {
            return false;
          }
          break;
        
        case 6:
          if (!sendV6(muxSocket, header.getSessionId(), header.getChannelId(), header.getFlags(), copy, chunk)) {
            return false;
          }
          break;
        
        default:
          throw new IllegalArgumentException("Invalid header version: " + header.getVersion());
        }
        
        pos += chunkLength;
        len -= chunkLength;
      } while (len > 0);
    }
    
    return true;
  }
  
  private int getRemaining(ByteBuffer[] bufs) {
    if (bufs == null) {
      return 0;
    }
    
    int remaining = 0;
    for (int i = 0; i < bufs.length; i++) {
      remaining += bufs[i].remaining();
    }
    return remaining;
  }
  
  @Override
  public boolean sendMuxIdentification(MuxIdentification identification) {
    StringBuilder sb = new StringBuilder();
    
    String appName = identification.getAppName();
    if (appName != null) {
      sb.append("applicationName=").append(appName).append(";");
    }
    
    String instanceName = identification.getInstanceName();
    if (instanceName != null) {
      sb.append("instanceName=").append(instanceName).append(";");
    }
    
    long keepAlive = identification.getKeepAlive();
    if (keepAlive != -1) {
      sb.append("keepAliveInterval=").append(keepAlive).append(";");
    }
    
    long idleFactor = identification.getIdleFactor();
    if (idleFactor != -1) {
      sb.append("idleFactor=").append(idleFactor).append(";");
    }
    
    long agentID = identification.getAgentID();
    if (agentID != -1) {
      sb.append("agentUid=").append(agentID).append(";");
    }
    
    long groupID = identification.getGroupID();
    if (groupID != -1) {
      sb.append("groupUid=").append(groupID).append(";");
    }
    
    long ringID = identification.getRingID();
    if (ringID != -1) {
      sb.append("ringId=").append(ringID).append(";");
    }
    
    long containerIndex = identification.getContainerIndex();
    if (containerIndex != -1) {
      sb.append("containerIndex=").append(containerIndex).append(";");
    }
    
    try {
      byte[] bytes = sb.toString().getBytes("ASCII");
      return sendV4(_lowPrioritySocket, (agentID == -1) ? 0 : agentID, 1, PROTOCOL_CTL | ACTION_ID, false,
                    ByteBuffer.wrap(bytes));
    } catch (UnsupportedEncodingException e) {
      return false;
    }
  }
  
  /************************* tcp *************************/
  
  @Override
  public boolean sendTcpSocketListen(long listenId, int localIP, int localPort, boolean secure) {
    return sendTcpSocketListen(listenId, MuxUtils.getIPAsString(localIP), localPort, secure);
  }
  
  @Override
  public boolean sendTcpSocketListen(long listenId, String localIP, int localPort, boolean secure) {
    byte[] msg = new byte[3 /* secure + port */+ localIP.length() + 1];
    int off = put8(msg, 0, (secure) ? 1 : 0);
    off = put16(msg, off, localPort, true);
    putAscii0(msg, off, localIP);
    return sendV0(_lowPrioritySocket, listenId, 0, PROTOCOL_TCP | ACTION_LISTEN, false, ByteBuffer.wrap(msg));
  }
  
  @Override
  public boolean sendTcpSocketConnect(long cnxId, String rhost, int rport, int lip, int lport, boolean secure) {
    return sendTcpSocketConnect(cnxId, rhost, rport, MuxUtils.getIPAsString(lip), lport, secure);
  }
  
  @Override
  public boolean sendTcpSocketConnect(long cnxId, String rhost, int rport, String lip, int lport,
                                      boolean secure) {
    int messageSize = 5 /* secure + ports */+ rhost.length() + 2;
    if (lip != null) {
      messageSize += 1 + lip.length();
    }
    byte[] msg = new byte[messageSize];
    int off = put8(msg, 0, (secure) ? 1 : 0);
    off = put16(msg, off, lport, true);
    off = putAscii0(msg, off, lip);
    off = put16(msg, off, rport, true);
    putAscii0(msg, off, rhost);
    return sendV0(_lowPrioritySocket, cnxId, 0, PROTOCOL_TCP | ACTION_OPEN_V4, false, ByteBuffer.wrap(msg));
  }
  @Override
  public boolean sendTcpSocketConnect(long connectionId, String remoteHost, int remotePort, String localIP,
				      int localPort, boolean secure, Map<String, String> params) {
    // params not implemented
    return sendTcpSocketConnect (connectionId, remoteHost, remotePort, localIP, localPort, secure);
  }
  @Override
  public boolean sendTcpSocketParams (int sockId, Map<String, String> param){
    // not implemented
    return true;
  }
  @Override
  public boolean sendTcpSocketClose(int sockId) {
    return sendV0(_lowPrioritySocket, 0L, sockId, PROTOCOL_TCP | ACTION_CLOSE, false, (ByteBuffer[]) null);
  }
  @Override
  public boolean sendTcpSocketReset(int sockId) {
    if (_defaultLogger.isDebugEnabled()) {
	_defaultLogger.debug("sendTcpSocketReset: sockId= " + sockId+" --> replaced by sendTcpSocketClose");
    }
    return sendTcpSocketClose (sockId);
  }
  
  @Override
  public boolean sendTcpSocketAbort(int sockId) {
    return sendV0(_lowPrioritySocket, 0L, sockId, PROTOCOL_TCP | ACTION_ABORT, false, (ByteBuffer[]) null);
  }
  
  @Override
  public boolean sendTcpSocketData(int sockId, byte[] data, int offset, int length, boolean copy) {
    return sendV3(_lowPrioritySocket, 0L, sockId, PROTOCOL_TCP | ACTION_DATA, copy,
                  ByteBuffer.wrap(data, offset, length));
  }
  
  @Override
  public boolean sendTcpSocketData(int sockId, boolean copy, ByteBuffer ... bufs) {
    return sendV3(_lowPrioritySocket, 0L, sockId, PROTOCOL_TCP | ACTION_DATA, copy, bufs);
  }
  
  /************************* sctp *************************/
  
  @Override
  public boolean sendSctpSocketClose(int sockId) {
    if (_defaultLogger.isDebugEnabled()) {
      _defaultLogger.debug("sendSctpSocketClose: sockId= " + sockId);
    }
    return sendV6(_lowPrioritySocket, 0L, sockId, PROTOCOL_TCP | ACTION_CLOSE, false, (ByteBuffer[]) null);
  }
  @Override
  public boolean sendSctpSocketReset(int sockId) {
    if (_defaultLogger.isDebugEnabled()) {
      _defaultLogger.debug("sendSctpSocketReset: sockId= " + sockId+" --> replaced by sendSctpSocketClose");
    }
    return sendSctpSocketClose (sockId);
  }
  
  /**
   * @see com.nextenso.mux.MuxConnection#sendSctpSocketConnect(long,
   *      java.lang.String, int, java.lang.String[], int, int, int)
   */
  @Override
  public boolean sendSctpSocketConnect(long connectionId, String remoteHost, int remotePort,
                                       String[] localAddrs, int localPort, int maxOutStreams, int maxInStreams, boolean secure) {
      //
      // secure is ignored in this implementation (no DTLS in C stacks)
      //
    if (_defaultLogger.isDebugEnabled()) {
      _defaultLogger.debug("sendSctpSocketConnect: sessionId= " + connectionId + ", remote host="
          + remoteHost + ", port=" + remotePort + ", local ips=" + Arrays.toString(localAddrs) + ", port="
          + localPort + ", maxOutStreams=" + maxOutStreams + ", maxInStreams=" + maxInStreams);
    }
    
    int messageLength = 1 /* nb of local addrs */+ 1 /* nb of remote addrs */+ 2 /* remote port */
        + 2 /* in streams */+ 2 /* out streams */
        + remoteHost.length() + 1 /*  \0 */;
    
    int localAddrsLength = 0;
    int nbLocalAddresses = 0;
    if (localAddrs != null) {
      for (String laddr : localAddrs) {
        if (laddr != null) {
          localAddrsLength += laddr.length();
          nbLocalAddresses++;
        }
      }
    }
    if (nbLocalAddresses > 0) {
      messageLength += 2 /*  local port */+ nbLocalAddresses + localAddrsLength;
    }
    
    byte[] msg = new byte[messageLength];
    int off = put8(msg, 0, nbLocalAddresses); // 1 byte
    if (localAddrs != null && nbLocalAddresses > 0) {
      off = put16(msg, off, localPort, true); // 2 bytes
      for (String laddr : localAddrs) {
        if (laddr != null) {
          off = putAscii0(msg, off, laddr);
        }
      }
    }
    
    off = put8(msg, off, 1); // 1 byte TODO we might have multi-homed remote addresses (API should be changed if needed)
    off = put16(msg, off, remotePort, true); // 2 bytes
    off = putAscii0(msg, off, remoteHost);
    off = put16(msg, off, maxOutStreams, true); // 2 bytes
    put16(msg, off, maxInStreams, true); // 2 bytes
    
    return sendV6(_lowPrioritySocket, connectionId, 0, PROTOCOL_TCP | ACTION_OPEN_V4, false,
                  ByteBuffer.wrap(msg));
  }
    public boolean sendSctpSocketConnect(long connectionId, String remoteHost, int remotePort,
					 String[] localAddrs, int localPort, int maxOutStreams, int maxInStreams, boolean secure, java.util.Map<SctpSocketOption, SctpSocketParam> sctpSocketOptions, Map<String, String> params){
	//
	// sctpSocketOptions & params are ignored with C stack
	//
	return sendSctpSocketConnect (connectionId, remoteHost, remotePort, localAddrs, localPort, maxOutStreams, maxInStreams, secure);
    }
    public boolean sendSctpSocketOptions(int sockId, java.util.Map<SctpSocketOption, SctpSocketParam> sctpSocketOptions){
	// not supported in C stack
	return true;
    }
    public boolean sendSctpSocketParams (int sockId, Map<String, String> param){
      // not implemented
      return true;
    }
  
  /**
   * @see com.nextenso.mux.MuxConnection#sendSctpSocketData(int,
   *      java.lang.String, boolean, boolean, int, int, long, boolean,
   *      java.nio.ByteBuffer[])
   */
  @Override
  public boolean sendSctpSocketData(int sockId, String addr, boolean unordered, boolean complete,
                                    int ploadPID, int streamNumber, long timeToLive, boolean copy,
                                    ByteBuffer ... data) {
    
    if (_defaultLogger.isDebugEnabled()) {
      _defaultLogger.debug("sendSctpSocketData: sockId= " + sockId + ", addr=" + addr + ", unordered="
          + unordered + ", complete=" + complete + ", ploadPID=" + ploadPID + ", streamNumber="
          + streamNumber + ", ttl=" + timeToLive);
    }
    
    // We provide the SCTP parameters  as part of the body.
    byte[] hdrbody = new byte[2 + addr.length() + 1 /* \0 */+ 1 /*unordered*/+ 1 /* complete */
        + 4 /* ploadPid */+ 2 /*streamNumber*/+ 4 /*timetoLive*/];
    int off = 0;
    off = put16(hdrbody, off, 0, true); // TODO port number when available in a future version
    off = putAscii0(hdrbody, off, addr);
    off = put8(hdrbody, off, (unordered) ? 1 : 0);
    off = put8(hdrbody, off, (complete) ? 1 : 0);
    off = put32(hdrbody, off, ploadPID, true);
    off = put16(hdrbody, off, streamNumber, true);
    off = put32(hdrbody, off, (int) timeToLive, true);
    
    ByteBuffer[] body = new ByteBuffer[data != null ? data.length + 1 : 1];
    body[0] = ByteBuffer.wrap(hdrbody);
    if (data != null) {
      System.arraycopy(data, 0, body, 1, data.length);
    }
    
    return sendV6(_lowPrioritySocket, 0L, sockId, PROTOCOL_TCP | ACTION_DATA, copy, body);
  }
  
  /**
   * @see com.nextenso.mux.MuxConnection#sendSctpSocketListen(long,
   *      java.lang.String[], int, int, int)
   */
  @Override
  public boolean sendSctpSocketListen(long listenId, String[] localAddrs, int localPort, int maxInStreams,
                                      int maxOutStreams, boolean secure) {
      //
      // secure is ignored in this implementation (no DTLS in C stacks)
      //
    if (_defaultLogger.isDebugEnabled()) {
      _defaultLogger.debug("sendSctpSocketListen: listenId= " + listenId + ", local ips="
          + Arrays.toString(localAddrs) + ", port=" + localPort + ", maxOutStreams=" + maxOutStreams
          + ", maxInStreams=" + maxInStreams);
    }
    
    int localAddrsLength = 0;
    for (String laddr : localAddrs) {
      localAddrsLength += laddr.length();
    }
    byte[] msg = new byte[1 /* no:of local addrs*/+ 2 /*localport*/+ 2 /*in streams*/+ 2/*out streams*/
        + localAddrsLength + localAddrs.length];
    int off = put8(msg, 0, localAddrs.length); // 1 byte
    off = put16(msg, off, localPort, true); // 2 bytes
    for (String laddr : localAddrs) {
      off = putAscii0(msg, off, laddr);
    }
    off = put16(msg, off, maxInStreams, true); // 2 bytes
    put16(msg, off, maxOutStreams, true); // 2 bytes
    
    return sendV6(_lowPrioritySocket, listenId, 0, PROTOCOL_TCP | ACTION_LISTEN, false, ByteBuffer.wrap(msg));
  }
  
  /************************* udp *************************/
  
  @Override
  public boolean sendUdpSocketBind(long bindId, int localIP, int localPort, boolean listen) {
    return sendUdpSocketBind(bindId, MuxUtils.getIPAsString(localIP), localPort, listen);
  }
  
  @Override
  public boolean sendUdpSocketBind(long bindId, String localIP, int localPort, boolean listen) {
    byte[] msg = new byte[2 + localIP.length() + 1];
    int off = put16(msg, 0, localPort, true);
    putAscii0(msg, off, localIP);
    int flags = (listen) ? PROTOCOL_UDP | ACTION_LISTEN : PROTOCOL_UDP | ACTION_OPEN_V4;
    return sendV0(_lowPrioritySocket, bindId, 0, flags, false, ByteBuffer.wrap(msg));
  }
  
  @Override
  public boolean sendUdpSocketClose(int sockId) {
    return sendV0(_lowPrioritySocket, 0, sockId, PROTOCOL_UDP | ACTION_CLOSE, false, (ByteBuffer[]) null);
  }
  
  @Override
  public boolean sendUdpSocketData(int sockId, int remoteIP, int remotePort, int virtualIP, int virtualPort,
                                   byte[] data, int off, int len, boolean copy) {
    return sendUdpSocketData(sockId, MuxUtils.getIPAsString(remoteIP), remotePort,
                             MuxUtils.getIPAsString(virtualIP), virtualPort, data, off, len, copy);
  }
  
  @Override
  public boolean sendUdpSocketData(int sockId, String remoteIP, int remotePort, String virtualIP,
                                   int virtualPort, byte[] data, int off, int len, boolean copy) {
    if (len > 0) {
      return sendUdpSocketData(sockId, remoteIP, remotePort, virtualIP, virtualPort, copy,
                               ByteBuffer.wrap(data, off, len));
    }
    return sendUdpSocketData(sockId, remoteIP, remotePort, virtualIP, virtualPort, false, (ByteBuffer[]) null);
  }
  
  @Override
  public boolean sendUdpSocketData(int sockId, int remoteIP, int remotePort, int virtualIP, int virtualPort,
                                   boolean copy, ByteBuffer ... bufs) {
    return sendUdpSocketData(sockId, MuxUtils.getIPAsString(remoteIP), remotePort,
                             MuxUtils.getIPAsString(virtualIP), virtualPort, copy, bufs);
  }
  
  @Override
  public boolean sendUdpSocketData(int sockId, String remoteIP, int remotePort, String virtualIP,
                                   int virtualPort, boolean copy, ByteBuffer ... bufs) {
    // We provide the udp address as part of the body.
    byte[] hdrbody = new byte[2 + remoteIP.length() + 1];
    MuxUtils.put_16(hdrbody, 0, remotePort, true);
    putAscii0(hdrbody, 2, remoteIP);
    
    ByteBuffer[] body = new ByteBuffer[bufs != null ? bufs.length + 1 : 1];
    body[0] = ByteBuffer.wrap(hdrbody);
    if (bufs != null) {
      System.arraycopy(bufs, 0, body, 1, bufs.length);
    }
    
    return sendV3(_lowPrioritySocket, 0L, sockId, PROTOCOL_UDP | ACTION_DATA, copy, body);
  }
  
  /************************* DNS *************************/
  
  @Override
  public boolean sendDnsGetByAddr(long reqId, String addr) {
    int msgLen = addr.length();
    byte[] msg = new byte[msgLen];
    MuxUtils.put_string(msg, 0, addr);
    return sendV0(_lowPrioritySocket, reqId, 0, PROTOCOL_DNS | ACTION_GET_BY_ADDR, false,
                  ByteBuffer.wrap(msg, 0, msgLen));
  }
  
  @Override
  public boolean sendDnsGetByName(long reqId, String name) {
    int msgLen = name.length();
    byte[] msg = new byte[msgLen];
    MuxUtils.put_string(msg, 0, name);
    return sendV0(_lowPrioritySocket, reqId, 0, PROTOCOL_DNS | ACTION_GET_BY_NAME, false,
                  ByteBuffer.wrap(msg, 0, msgLen));
  }
  
  /************************* release *************************/
  
  @Override
  public boolean sendRelease(long sessionId) {
    return sendV4(_lowPrioritySocket, sessionId, 0, PROTOCOL_CTL | ACTION_RELEASE, false, (ByteBuffer[]) null);
  }
  
  @Override
  public boolean sendReleaseAck(long sessionId, boolean confirm) {
    return sendV4(_lowPrioritySocket, sessionId, 0, (confirm) ? PROTOCOL_CTL | ACTION_RELEASE_CONFIRM
        : PROTOCOL_CTL | ACTION_RELEASE_CANCEL, false, (ByteBuffer[]) null);
  }
  
  /************************************* Package methods *******************************************/
  
  long getConnectionTimeout() {
    return _connectionTimeout;
  }
  
  Logger getLogger() {
    return _logger;
  }
  
  /**
   * Called by the MuxFactoryImpl, once a socket is connected/accepted.
   */
  void muxOpened(TcpChannel cnx, boolean client, Throwable connectError) {
    if (connectError != null) {
      // Could not connect or accept.
      if (_connectionListener != null) {
        if (client) {
          _connectionListener.muxConnected(this, connectError);
        } else {
          _connectionListener.muxAccepted(this, connectError);
        }
      }
      return;
    }
    
    MuxSocket ms = new MuxSocket(cnx);
    _lowPrioritySocket = ms;
    _localAddr = cnx.getLocalAddress();
    
    // schedule our keep alive, if our mux handler is configured with
    // keep alive (see constructor)
    if (_keepAlive != null) {
      _keepAlive.schedule();
    }
    
    _muxOpened = true;
    if (_connectionListener != null) {
      if (client) {
        try {
          _connectionListener.muxConnected(this, null);
        } catch (Throwable t) {
          _logger.error("Mux connection listener " + _connectionListener + " threw an exception on muxConnected callback ", t);
        }
      } else {
          try {
            _connectionListener.muxAccepted(this, null);
          } catch (Throwable t) {
            _logger.error("Mux connection listener " + _connectionListener + " threw an exception on muxAccepted callback ", t);
          }
        }
      }

      try {
          _handler.muxOpened(this);
      } catch (Throwable t) {
          _logger.error("Mux handler " + _handler + " threw an exception on open callback ", t);
      }
  }
  
  void muxClosed() {    
    if (_muxOpened) {
      _muxOpened = false;

      // reset some resources associated to the mux cnx.
      if (_keepAlive != null) {
        _keepAlive.cancel();
      }
      
      // For now, if the mux handler is a simple mux handler and if we the application has closed this cnx, then don't notify the simple mux handler.
      // TODO: refactor MonitorJava, DistSession, and DBCacheJava, and then invoke the muxClosed callback on the simple mux handler.
      if (_userClosing && _handler instanceof SimpleMuxFactoryImpl.MuxHandlerAdapter) {
        _logger.info("mux connection closed by simple mux handler: " + this);
        return;
      }
      
      if (_connectionListener != null) {
        try {
          _connectionListener.muxClosed(this);
        } catch (Throwable err) {
          _logger.warn("Exception while calling muxClosed method on connection listener " + _connectionListener, err);
        }
      }

      try {
        _handler.muxClosed(this);
      } catch (Throwable err) {
        _logger.error("Mux handler " + _handler + " threw an exception on close callback ", err);
      }
    }
  }
    
  /**
   * asynchronously close the mux cnx.
   * 
   * @param reason 
   * @param info
   * @param err
   * @param shutdown
   * @param internal true if we are closing from MuxConnectionImpl class, false if the user has requested a mux close.
   */
  private void close(Enum<?> reason, String info, Throwable err, boolean shutdown, boolean internal) {  
    if (! _muxOpened) {
      return;
    }
    
    if (reason != null) {
      logClosingMux(this, reason, info, err, _logger);
    }

    // Temporary fix: if the user is closingwe have to mark the fact user is closing, because for SimpleMuxHandlers, we don't want yet to callback them in muxClosed
    // if the socket is being closed by the application.
    if (! internal) {
      _userClosing = true;
    }
    
    
    if (_lowPrioritySocket != null) {
      if (shutdown) {
        _lowPrioritySocket.shutdown();
      } else {
        _lowPrioritySocket.close();
      }
    }
    
    if (_highPrioritySocket != null) {
      if (shutdown) {
        _highPrioritySocket.shutdown();
      } else {
        _highPrioritySocket.close();
      }
    }
  }
      
  int handleData(TcpChannel cnx, ByteBuffer msg) {
    try {
      return _lowPrioritySocket.handleData(msg);
    } catch (Throwable t) {
      close(CloseReason.InternalError, "Got exception while demultiplexing", t, true, true);
    }
    
    return 0;
  }
  
  InetSocketAddress getConnectAddr() {
    return _remoteAddr;
  }
  
  Reactor getReactor() {
    return _reactor;
  }
  
  private void sendMuxPing() {
    if (_runtimeStats != null) {            
      // schedule the ping response in the the io threadpool, in order to 
      // reflect current response time.
      
      final Runnable pingTask = new Runnable() {
          public void run() {
              sendV4(_lowPrioritySocket, 0L, 0, PROTOCOL_CTL | ACTION_PING, false, (ByteBuffer[]) null);
          }
      };
            
      if (_execs != null) {
          _execs.getIOThreadPoolExecutor().execute(pingTask);
      } else {
          pingTask.run();
      }           
    } else {        
        // No runtime statistic service available or rtt is zero: send ping reply right now.
        sendV4(_lowPrioritySocket, 0L, 0, PROTOCOL_CTL | ACTION_PING, false, (ByteBuffer[]) null);
    }
  }
  
  private void sendMuxAlive() {
    if (_klvLogger.isEnabledFor(Level.DEBUG)) {
      StringBuilder sb = new StringBuilder();
      sb.append(toLogString() + " : sent ");
      sb.append(logV4(PROTOCOL_CTL | ACTION_ALIVE, (ByteBuffer[]) null));
      _klvLogger.debug(sb.toString());
    }
    sendV4(_lowPrioritySocket, 0L, 0, PROTOCOL_CTL | ACTION_ALIVE, false, (ByteBuffer[]) null);
  }
  
  private void sendMuxAliveAck() {
    if (_klvLogger.isEnabledFor(Level.DEBUG)) {
      StringBuilder sb = new StringBuilder();
      sb.append(toLogString() + " : sent ");
      sb.append(logV4(ACK_MASK | PROTOCOL_CTL | ACTION_ALIVE, (ByteBuffer[]) null));
      _klvLogger.debug(sb.toString());
    }
    sendV4(_lowPrioritySocket, 0L, 0, ACK_MASK | PROTOCOL_CTL | ACTION_ALIVE, false, (ByteBuffer[]) null);
  }
  
  private boolean sendV0(MuxSocket muxCnx, long sessionId, int sockId, int flags, boolean copy,
                         ByteBuffer ... bufs) {
    if (_logger.isInfoEnabled()) {
      StringBuilder sb = new StringBuilder();
      sb.append("sending: ");
      sb.append(logV0(sessionId, sockId, flags, bufs));
      _logger.info(sb.toString());
    }
    
    try {
      TcpChannel cnx = muxCnx.getTcpChannel();
      if (! _muxOpened)
        return false;
      ByteBuffer[] buffers = new ByteBuffer[bufs != null ? bufs.length + 2 : 2];
      int index = 0;
      
      // setup mux header
      MuxHeaderV0 header = new MuxHeaderV0();
      header.set(sessionId, sockId, flags);
      ByteBuffer hdrV0 = ByteBuffer.allocate(V0_HEADER_SIZE);
      hdrV0.order(ByteOrder.LITTLE_ENDIAN);
      setupHeaderV0(header, getSize(bufs), hdrV0);
      buffers[index++] = hdrV0;
      
      // setup body.
      if (bufs != null) {
        System.arraycopy(bufs, 0, buffers, 1, bufs.length);
        index += bufs.length;
      }
      
      // setup magic trailer
      buffers[index++] = makeTrailer();
      
      // send header + body + trailer atomically (using NIO gathering writes).
      cnx.send(buffers, copy);
      return true;
    }
    
    catch (Throwable e) {
      _logger.warn("Could not send message on mux connection " + toString(), e);
      return false;
    }
  }
  
  private boolean sendV2(MuxSocket muxCnx, long sessionId, int sockId, int flags, int remoteIP,
                         int remotePort, int virtualIP, int virtualPort, boolean copy, ByteBuffer ... bufs) {
    if (_logger.isInfoEnabled()) {
      StringBuilder sb = new StringBuilder();
      sb.append("sending: ");
      sb.append(logV2(sessionId, sockId, flags, MuxUtils.getIPAsString(remoteIP), remotePort,
                      MuxUtils.getIPAsString(virtualIP), virtualPort, bufs));
      _logger.info(sb.toString());
    }
    
    try {
      TcpChannel cnx = muxCnx.getTcpChannel();
      if (! _muxOpened)
        return false;
      ByteBuffer[] buffers = new ByteBuffer[bufs != null ? bufs.length + 2 : 2];
      int index = 0;
      
      // setup mux header
      MuxHeaderV2 header = new MuxHeaderV2();
      header.set(sessionId, sockId, flags, remoteIP, remotePort, virtualIP, virtualPort);
      ByteBuffer hdrV2 = ByteBuffer.allocate(V2_HEADER_SIZE);
      hdrV2.order(ByteOrder.LITTLE_ENDIAN);
      setupHeaderV2(header, getSize(bufs), hdrV2);
      buffers[index++] = hdrV2;
      
      // setup body
      if (bufs != null) {
        System.arraycopy(bufs, 0, buffers, 1, bufs.length);
        index += bufs.length;
      }
      
      // setup magic trailer
      buffers[index++] = makeTrailer();
      
      // send header + body + trailer atomically (using NIO gathering writes).
      cnx.send(buffers, copy);
      return true;
    }
    
    catch (Throwable e) {
      _logger.warn("Could not send message on mux connection " + toString(), e);
      return false;
    }
  }
  
  private boolean sendV3(MuxSocket muxCnx, long sessionId, int sockId, int flags, boolean copy,
                         ByteBuffer ... bufs) {
    if (_logger.isInfoEnabled()) {
      StringBuilder sb = new StringBuilder();
      sb.append("sending: ");
      sb.append(logV3(sessionId, sockId, flags, bufs));
      _logger.info(sb.toString());
    }
    
    try {
      TcpChannel cnx = muxCnx.getTcpChannel();
      if (! _muxOpened)
        return false;
      ByteBuffer[] buffers = new ByteBuffer[bufs != null ? bufs.length + 2 : 2];
      int index = 0;
      
      // setup mux header
      MuxHeaderV3 v3 = new MuxHeaderV3();
      v3.set(sessionId, sockId, flags);
      ByteBuffer hdrV3 = ByteBuffer.allocate(V3_HEADER_SIZE);
      hdrV3.order(ByteOrder.LITTLE_ENDIAN);
      setupHeaderV3(v3, getSize(bufs), hdrV3);
      buffers[index++] = hdrV3;
      
      // setup body.
      if (bufs != null) {
        System.arraycopy(bufs, 0, buffers, 1, bufs.length);
        index += bufs.length;
      }
      
      // setup magic trailer
      buffers[index++] = makeTrailer();
      
      // send header + body + trailer atomically (using NIO gathering writes).
      cnx.send(buffers, copy);
      return true;
    }
    
    catch (Throwable e) {
      _logger.warn("Could not send message on mux connection " + toString(), e);
      return false;
    }
  }
  
  private boolean sendV4(MuxSocket muxCnx, long sessionId, int sockId, int flags, boolean copy,
                         ByteBuffer ... bufs) {
    if (_logger.isInfoEnabled()
        && (!((flags & (PROTOCOL_CTL | ACTION_UMEM)) == (PROTOCOL_CTL | ACTION_UMEM)))) {
      StringBuilder sb = new StringBuilder();
      sb.append("sending: ");
      sb.append(logV4(flags, bufs));
      _logger.info(sb.toString());
    }
    try {
      TcpChannel cnx = muxCnx.getTcpChannel();
      if (! _muxOpened)
        return false;
      ByteBuffer[] buffers = new ByteBuffer[bufs != null ? bufs.length + 2 : 2];
      int index = 0;
      
      // setup mux header
      MuxHeaderV4 v4 = new MuxHeaderV4();
      v4.set(sessionId, sockId, flags);
      ByteBuffer hdrV4 = ByteBuffer.allocate(V4_HEADER_SIZE);
      hdrV4.order(ByteOrder.LITTLE_ENDIAN);
      setupHeaderV4(v4, getSize(bufs), hdrV4);
      buffers[index++] = hdrV4;
      
      // setup body.
      if (bufs != null) {
        System.arraycopy(bufs, 0, buffers, 1, bufs.length);
        index += bufs.length;
      }
      
      // setup magic trailer
      buffers[index++] = makeTrailer();
      
      // send header + body + trailer atomically (using NIO gathering writes).
      cnx.send(buffers, copy);
      return true;
    }
    
    catch (Throwable e) {
      _logger.warn("Could not send message on mux connection " + toString(), e);
      return false;
    }
  }
  
  private boolean sendV5(MuxSocket muxCnx, int flags, boolean copy, ByteBuffer ... bufs) {
    if (_logger.isInfoEnabled()) {
      StringBuilder sb = new StringBuilder();
      sb.append("sending: ");
      sb.append(logV5(flags, bufs));
      _logger.info(sb.toString());
    }
    
    try {
      TcpChannel cnx = muxCnx.getTcpChannel();
      if (! _muxOpened)
        return false;
      
      ByteBuffer[] buffers = new ByteBuffer[bufs != null ? bufs.length + 2 : 2];
      int index = 0;
      
      // setup mux header
      MuxHeaderV5 v5 = new MuxHeaderV5();
      v5.set(flags);
      ByteBuffer hdrV5 = ByteBuffer.allocate(V5_HEADER_SIZE);
      hdrV5.order(ByteOrder.LITTLE_ENDIAN);
      setupHeaderV5(v5, getSize(bufs), hdrV5);
      buffers[index++] = hdrV5;
      
      // setup body.
      if (bufs != null) {
        System.arraycopy(bufs, 0, buffers, 1, bufs.length);
        index += bufs.length;
      }
      
      // setup magic trailer
      buffers[index++] = makeTrailer();
      
      // send header + body + trailer atomically (using NIO gathering writes).
      cnx.send(buffers, copy);
      return true;
    }
    
    catch (Throwable e) {
      _logger.warn("Could not send message on mux connection " + toString(), e);
      return false;
    }
  }
  
  private boolean sendV6(MuxSocket muxCnx, long sessionId, int sockId, int flags, boolean copy,
                         ByteBuffer ... bufs) {
    if (_logger.isInfoEnabled()) {
      StringBuilder sb = new StringBuilder();
      sb.append("sending: ");
      sb.append(logV6(sessionId, sockId, flags, bufs));
      _logger.info(sb.toString());
    }
    
    try {
      TcpChannel cnx = muxCnx.getTcpChannel();
      if (! _muxOpened) {
        return false;
      }
      ByteBuffer[] buffers = new ByteBuffer[bufs != null ? bufs.length + 2 : 2];
      int index = 0;
      
      // setup mux header
      MuxHeaderV6 v6 = new MuxHeaderV6();
      v6.set(sessionId, sockId, flags);
      ByteBuffer hdrV6 = ByteBuffer.allocate(V0_HEADER_SIZE);
      setupHeaderV6(v6, getSize(bufs), hdrV6);
      buffers[index++] = hdrV6;
      
      // setup body.
      if (bufs != null) {
        System.arraycopy(bufs, 0, buffers, 1, bufs.length);
        index += bufs.length;
      }
      
      // setup magic trailer
      buffers[index++] = makeTrailer();
      
      // send header + body + trailer atomically (using NIO gathering
      // writes).
      cnx.send(buffers, copy);
      return true;
    }
    
    catch (Throwable e) {
      _logger.warn("Could not send message on mux connection " + toString(), e);
      return false;
    }
  }
  
  private int getSize(ByteBuffer[] bufs) {
    if (bufs == null) {
      return 0;
    }
    int size = 0;
    for (ByteBuffer buf : bufs) {
      size += buf.remaining();
    }
    return size;
  }
  
  private String getFlagMessage(IntHashtable flagsTable, int flags) {
    if (flagsTable != null) {
      String name = (String) flagsTable.get(flags);
      if (name != null) {
        return name;
      }
    }
    String res = "";
    boolean needSlash = false;
    int protocol = flags & PROTOCOL_MASK;
    int ack = flags & ACK_MASK;
    int action = flags & ACTION_MASK;
    if (ack != 0) {
      res += "ACK";
      needSlash = true;
    }
    if (protocol == MuxProtocol.PROTOCOL_NONE) {
      if (needSlash) {
        res += "|";
      }
      res += "NONE";
      if (action == MuxProtocol.ACTION_DHT_GET) {
        res += "|DHT_GET";
      } else {
        res += action;
      }
      return res;
    }
    if (protocol == MuxProtocol.PROTOCOL_DNS) {
      if (needSlash) {
        res += "|";
      }
      res += "DNS";
      if (action == MuxProtocol.ACTION_GET_BY_NAME) {
        res += "|GET_BY_NAME";
      } else if (action == MuxProtocol.ACTION_GET_BY_ADDR) {
        res += "|GET_BY_ADDR";
      } else {
        res += "|WRONG DNS ACTION : " + action;
      }
      return res;
    }
    if (protocol == MuxProtocol.PROTOCOL_CTL) {
      if (needSlash) {
        res += "|";
      }
      res += "CTL";
      if (action == MuxProtocol.ACTION_CONFIG) {
        res += "|CONFIG";
      } else if (action == MuxProtocol.ACTION_CONFIG_CONNECTION) {
        res += "|CONFIG_CONNECTION";
      } else if (action == MuxProtocol.ACTION_STOP) {
        res += "|STOP";
      } else if (action == MuxProtocol.ACTION_RELEASE) {
        res += "|RELEASE";
      } else if (action == MuxProtocol.ACTION_RELEASE_CANCEL) {
        res += "|RELEASE_CANCEL";
      } else if (action == MuxProtocol.ACTION_START) {
        res += "|ACTION_START";
      } else if (action == MuxProtocol.ACTION_ID) {
        res += "|ID";
      } else if (action == MuxProtocol.ACTION_UMEM) {
        res += "|UMEM";
      } else if (action == MuxProtocol.ACTION_PING) {
        res += "|PING";
      } else if (action == MuxProtocol.ACTION_ALIVE) {
        res += "|ALIVE";
      } else {
        res += "|WRONG CTL ACTION : " + action;
      }
      return res;
    }
    if (protocol == MuxProtocol.PROTOCOL_TCP) {
      if (needSlash) {
        res += "|";
      }
      res += "TCP";
      if (action == MuxProtocol.ACTION_LISTEN) {
        res += "|LISTEN";
      } else if (action == MuxProtocol.ACTION_OPEN_V4) {
        res += "|OPEN_V4";
      } else if (action == MuxProtocol.ACTION_CLOSE) {
        res += "|CLOSE";
      } else if (action == MuxProtocol.ACTION_DATA) {
        res += "|DATA";
      } else if (action == MuxProtocol.ACTION_ABORT) {
        res += "|ABORT";
      } else {
        res += "|WRONG TCP ACTION : " + action;
      }
      return res;
    }
    if (protocol == MuxProtocol.PROTOCOL_UDP) {
      if (needSlash)
        res += "|";
      res += "UDP";
      if (action == MuxProtocol.ACTION_LISTEN)
        res += "|LISTEN";
      else if (action == MuxProtocol.ACTION_OPEN_V4)
        res += "|OPEN_V4";
      else if (action == MuxProtocol.ACTION_CLOSE)
        res += "|CLOSE";
      else if (action == MuxProtocol.ACTION_DATA)
        res += "|DATA";
      else
        res += "|WRONG UDP ACTION : " + action;
      return res;
    }
    return "" + flags;
  }
  
  private ByteBuffer putUnsigned(ByteBuffer buf, int b) {
    buf.put((byte) (b & 0xff));
    return buf;
  }
  
  private ByteBuffer putUnsignedShort(ByteBuffer buf, int val) {
    buf.putShort((short) (val & 0xffff));
    return buf;
  }
  
  private void setupHeaderV0(MuxHeaderV0 header, int size, ByteBuffer out /* always little endian */) {
    setupHeader(header, size, out);
  }
  
  private void setupHeaderV2(MuxHeaderV2 header, int size, ByteBuffer out) {
    out.clear();
    out.putInt(MAGIC_HEADER_VAL);
    putUnsigned(out, header.getVersion());
    putUnsigned(out, header.getFlags());
    putUnsignedShort(out, size);
    out.putLong(header.getSessionId());
    out.putInt(header.getChannelId());
    out.putInt(header.getRemoteIP());
    out.putInt(header.getVirtualIP());
    putUnsignedShort(out, header.getRemotePort());
    putUnsignedShort(out, header.getVirtualPort());
    out.flip();
  }
  
  private void setupHeaderV3(MuxHeaderV3 header, int size, ByteBuffer out) {
    out.clear();
    out.putInt(MAGIC_HEADER_VAL);
    putUnsigned(out, header.getVersion());
    putUnsigned(out, header.getFlags());
    putUnsignedShort(out, 0);
    out.putLong(header.getSessionId());
    out.putInt(header.getChannelId());
    out.putInt(size);
    out.flip();
  }
  
  private void setupHeaderV4(MuxHeaderV4 header, int size, ByteBuffer out) {
    setupHeader(header, size, out);
  }
  
  private void setupHeaderV5(MuxHeaderV5 header, int size, ByteBuffer out /* order = little indian */) {
    out.clear();
    out.putInt(MAGIC_HEADER_VAL);
    putUnsigned(out, header.getVersion());
    putUnsigned(out, header.getFlags());
    out.order(ByteOrder.BIG_ENDIAN);
    out.putInt(size);
    out.order(ByteOrder.LITTLE_ENDIAN);
    out.flip();
  }
  
  private void setupHeaderV6(MuxHeaderV6 header, int size, ByteBuffer out /* order = little indian */) {
    out.order(ByteOrder.LITTLE_ENDIAN);
    setupHeader(header, size, out);
  }
  
  private void setupHeader(MuxHeader header, int size, ByteBuffer out) {
    out.clear();
    out.putInt(MAGIC_HEADER_VAL);
    putUnsigned(out, header.getVersion());
    putUnsigned(out, header.getFlags());
    putUnsignedShort(out, size);
    out.putLong(header.getSessionId());
    out.putInt(header.getChannelId());
    out.flip();
  }
  
  private String logV0(long sid, int channel, int flags, ByteBuffer ... bufs) {
    return logV(0, sid, channel, flags, bufs);
  }
  
  private String logV(int version, long sid, int channel, int flags, byte[] data, int off, int len) {
    StringBuilder sb = new StringBuilder();
    sb.append("[v").append(version);
    if (version == 6) {
      sb.append("(SCTP)");
    }
    sb.append(":sessionId=").append(sid);
    sb.append(":channelId=").append(channel);
    sb.append(":flags=").append(getFlagMessage(_flagsTable, flags));
    if (len != 0)
      sb.append(",len=").append(len);
    sb.append("]");
    if (len > 0) {
      sb.append("\n");
      logData(sb, data, off, len);
    }
    return sb.toString();
  }
  
  private String logV0(long sid, int channel, int flags, byte[] data, int off, int len) {
    return logV(0, sid, channel, flags, data, off, len);
  }
  
  public Object logV6(long sessionId, int sockId, int flags, byte[] data, int off, int length) {
    return logV(6, sessionId, sockId, flags, data, off, length);
  }
  
  private String logV2(long sid, int channel, int flags, String rip, int rport, String vip, int vport,
                       ByteBuffer ... bufs) {
    StringBuilder sb = new StringBuilder("[v2");
    if (sid != 0L) {
      sb.append(":sessionId=").append(sid);
    }
    if (channel != 0) {
      sb.append(":channelId=").append(channel);
    }
    sb.append(",rip=").append(rip).append(",rport=").append(rport);
    sb.append(",vip=").append(vip).append(",vport=").append(vport);
    log(sb, flags, bufs);
    return sb.toString();
  }
  
  private String logV3(long sid, int channel, int flags, ByteBuffer ... bufs) {
    StringBuilder sb = new StringBuilder("[v3");
    if (sid != 0L) {
      sb.append(":sessionId=").append(sid);
    }
    if (channel != 0) {
      sb.append(":channelId=").append(channel);
    }
    log(sb, flags, bufs);
    return sb.toString();
  }
  
  private String logV3UdpSocketData(int channel, int flags, byte[] data, int off, int len, String remoteIP,
                                    int remotePort, IntHashtable flagsTable) {
    StringBuilder sb = new StringBuilder("[v3");
    if (channel != 0) {
      sb.append(":channelId=").append(channel);
    }
    sb.append(":remoteIP=").append(remoteIP);
    sb.append(":remotePort=").append(remotePort);
    sb.append(":flags=").append(getFlagMessage(flagsTable, flags));
    if (len > 0) {
      sb.append(":len=").append(len);
    }
    sb.append("]");
    if (_logger.isDebugEnabled() && len > 0) {
      sb.append("\n");
      logData(sb, data, off, len);
    }
    return sb.toString();
  }
  
  private String logV4(int flags, ByteBuffer ... bufs) {
    StringBuilder sb = new StringBuilder("[v4");
    log(sb, flags, bufs);
    return sb.toString();
  }
  
  private String logV5(int flags, ByteBuffer ... bufs) {
    StringBuilder sb = new StringBuilder("[v5");
    log(sb, flags, bufs);
    return sb.toString();
  }
  
  private String logV6(long sid, int channel, int flags, ByteBuffer ... bufs) {
    return logV(6, sid, channel, flags, bufs);
  }
  
  private String logV(int version, long sid, int channel, int flags, ByteBuffer ... bufs) {
    StringBuilder sb = new StringBuilder();
    sb.append("[v").append(version);
    if (version == 6) {
      sb.append("(SCTP)");
    }
    sb.append(":sessionId=").append(sid);
    sb.append(":channelId=").append(channel);
    log(sb, flags, bufs);
    return sb.toString();
  }
  
  private StringBuilder logData(StringBuilder sb, byte[] data, int off, int len) {
    if (len == 0) {
      return sb;
    }
    int max = off + len;
    for (int i = off; i < max; i++) {
      if (!Utils.isPrintable(data[i])) {
        sb.append(Utils.toString(data, off, len));
        return sb;
      }
    }
    
    sb.append(new String(data, off, len));
    return sb;
  }
  
  private void log(StringBuilder sb, int flags, ByteBuffer ... bufs) {
    sb.append(":flags=");
    sb.append(getFlagMessage(_flagsTable, flags));
    int len = getSize(bufs);
    if (len > 0) {
      sb.append(":len=").append(len);
    }
    sb.append("]");
    if (len > 0) {
      sb.append("\n");
      logData(sb, bufs);
    }
  }
  
  private StringBuilder logData(StringBuilder sb, ByteBuffer ... bufs) {
    if (bufs == null)
      return sb;
    
    loop: for (ByteBuffer buf : bufs) {
      buf = buf.duplicate();
      byte[] data = new byte[buf.remaining()];
      buf.get(data);
      for (int i = 0; i < data.length; i++) {
        if (!Utils.isPrintable(data[i])) {
          sb.append(Utils.toString(data, 0, data.length));
          continue loop;
        }
      }
      sb.append(new String(data, 0, data.length));
    }
    
    return sb;
  }
  
  private static synchronized void handleActionOverLoad(int flags) {
    if (flags == MuxProtocol.ACTION_OVERLOAD_NORMAL) {
      if (_eventAdmin != null) {
        _eventAdmin.postEvent(new Event(LOAD_NORMAL, (java.util.Dictionary) null));
      }
      if (_klvLogger.isInfoEnabled()) {
        _klvLogger.info("Normal load detected");
      }
      return;
    }
    
    final Boolean rtt = (flags & MuxProtocol.ACTION_OVERLOAD_CPU) == MuxProtocol.ACTION_OVERLOAD_CPU ? Boolean.TRUE
        : Boolean.FALSE;
    final Boolean mem = (flags & MuxProtocol.ACTION_OVERLOAD_MEM) == MuxProtocol.ACTION_OVERLOAD_MEM ? Boolean.TRUE
        : Boolean.FALSE;
    final Boolean cpu = (flags & MuxProtocol.ACTION_OVERLOAD_RTT) == MuxProtocol.ACTION_OVERLOAD_RTT ? Boolean.TRUE
        : Boolean.FALSE;
    
    if (_eventAdmin != null) {
      _eventAdmin.postEvent(new Event(LOAD_HIGH, (java.util.Dictionary) new Properties() {
        
        {
          put("RTT", rtt);
          put("CPU", cpu);
          put("MEM", mem);
        }
      }));
    }
    
    if (_klvLogger.isInfoEnabled()) {
      _klvLogger.info("Overload detected: CPU=" + cpu + ",RTT=" + rtt + ",MEM=" + mem);
    }
  }
  
  private static ByteBuffer makeTrailer() {
    ByteBuffer trailer = ByteBuffer.allocate(4);
    trailer.order(ByteOrder.LITTLE_ENDIAN);
    trailer.putInt(MAGIC_TRAILER_VAL);
    trailer.flip();
    return trailer;
  }
  
  private long getOldMemoryCollectionCount()
  {
      long total = 0;
      for (GarbageCollectorMXBean gc : _oldMemCollectors)
      {
          total += gc.getCollectionCount();
      }
      return total;
  }
    
  /************************************* Inner class ****************************************/
  private class MuxKeepAlive implements Runnable {
    
    private Future _future = null;
    private int _alivePending = 0;
    private long _aliveLastTime;
    private long _dataLastTime;
    private int _dataReceived = 0;
    private double _srtt = -1.0;
    private double _rttVar = -1.0;
    private double _rto = -1.0;
    private int _rate;
    private int _idleFactor = 2;
    private long _previousOldMemCollectionCount;
    
    public MuxKeepAlive(int rate, int idleFactor) {
      if (_klvLogger.isDebugEnabled()) {
        _klvLogger.debug("constr: new MuxKeepAlive rate=" + rate + ", idleFactor=" + idleFactor
            + ", keepAlive=" + this + " for connection " + MuxConnectionImpl.this);
      }
      
      /* Caution: rate is given in millisecond */
      _aliveLastTime = System.currentTimeMillis();
      _dataLastTime = System.currentTimeMillis();
      _rate = rate;
      if (idleFactor > 0) {
        _idleFactor = idleFactor;
      }
      if (_rate == -1) {
        _rate = DEFAULT_KEEP_ALIVE;
      }
      _rto = idleFactor * rate / 1000.0;
    }
    
    public void cancel() {
      if (_klvLogger.isDebugEnabled()) {
        _klvLogger.debug("cancel: keepAlive=" + this + ", future=" + _future);
      }
      if (_future != null) {
        _future.cancel(false);
        _future = null;
      }
    }
    
    public void schedule() {
      _future = _timer.scheduleWithFixedDelay(_reactor, this, 2 * 1L, 1L, TimeUnit.SECONDS);
      if (_klvLogger.isDebugEnabled()) {
        _klvLogger.debug("schedule: keepAlive=" + this);
      }
    }
    
    //		public int rate() {
    //			return _rate;
    //		}
    
    public void run() {
      long now = System.currentTimeMillis();
      if (_alivePending == 0) {
        /*
         * no alive is in the pipe. Check if we should send a new one what we do is to count the
         * time passed from the last data received.
         */
        if ((now - _dataLastTime) >= _rate) {
          /*
           * ok we have seen nothing from this peer for more than rate ms. Send a keep alive.
           */
          _alivePending = 1;
          _aliveLastTime = _dataLastTime = now;
          _previousOldMemCollectionCount = getOldMemoryCollectionCount();
          sendMuxAlive();
          return;
        }
        
        /*
         * we have received data from this peer since during the last 'rate' milliseconds time frame.
         * What we do is to call recAlive in order to refresh our RTO value which is the
         * adaptative value we use to decide to kill the peer socket.
         */
        _aliveLastTime = now;
        _previousOldMemCollectionCount = 0;
        recAlive();
      } else {
        /*
         * we are waiting for an ack|alive
         */
        if (_rto > 0.0) {
          // If we have received some data since previous alive message, *OR* if some Old Memory GC occured, then
          // don't take decision (if we just made a big full gc, we can't reasonably detect if a peer is sick or not ...
          
          long oldMemCollectionCount = getOldMemoryCollectionCount();
          long deltaGC = (oldMemCollectionCount - _previousOldMemCollectionCount);
          _previousOldMemCollectionCount = oldMemCollectionCount;
          
          if (_dataReceived != 0 || deltaGC > 0) {
            _dataReceived = 0;
            _aliveLastTime = now;
            if (deltaGC > 0 && _dataReceived == 0) {
                _logger.warn("Old Memory GC just occurred: deactivating ping timers temporarily (GC count=" + deltaGC + ")");
            }
            return;
          }
          
          if (_klvLogger.isDebugEnabled()) {
            _klvLogger.debug("run: problem in the peer with keepAlive=" + this);
          }
          
          if (((now - _aliveLastTime) / 1000.0) > (_rto * _idleFactor)) {
            /* brutal closing */
            close(CloseReason.KeepAlive, "keep alive timeout", null, true, true);
          } else {
            if (_klvLogger.isEnabledFor(Level.WARN)) {
              _klvLogger.warn(toLogString() + " : MUX keep alive detected slow peer. Did not respond for "
                  + ((now - _aliveLastTime) / 1000) + " sec.  (idle factor=" + _idleFactor + ",RTO=" + _rto
                  + ")");
            }
          }
        }
      }
    }
    
    public void recAlive() {
      double rtt;
      double alpha = 0.25;
      double beta = 0.125;
      double G = 1.0 * (_rate / 1000.0);
      long now = System.currentTimeMillis();
      rtt = 1.0 * (now - _aliveLastTime) / 1000;
      
      if (Math.abs(_srtt - 1.0) < .0000001) // (if (_srtt == -1.0)
      {
        _srtt = rtt;
        _rttVar = rtt / 2.0;
      } else {
        if (_srtt > rtt)
          _rttVar = (1 - beta) * _rttVar + beta * (_srtt - rtt);
        else
          _rttVar = (1 - beta) * _rttVar + beta * (rtt - _srtt);
        _srtt = (1 - alpha) * _srtt + alpha * rtt;
      }
      if (G > (4.0 * _rttVar)) {
        _rto = _srtt + (_rate / 1000.0);
      } else {
        _rto = _srtt + (4.0 * _rttVar);
      }
      _alivePending = 0;
      _dataReceived = 0;
      if (_klvLogger.isDebugEnabled()) {
        _klvLogger.debug(toLogString() + " : round-trip-time=" + rtt + " new calculated RTO=" + _rto
            + " rate=" + _rate);
      }
    }
    
    public void received() {
      _dataReceived = 1;
      _dataLastTime = System.currentTimeMillis();
    }
    
  }
  
  /**
   * This class represents a concrete socket connection. In mono-socket mode, a
   * MuxConnectionImpl instance uses only one MuxSocket instance. In
   * double-socket mode, a MuxConnectionImpl instance uses two MuxSocket
   * instance: the first one is used to handle requests, and the second one is
   * used to handle responses which has a higher priority.
   */
  class MuxSocket {
    
    private TcpChannel _connection;
    private MuxBuffer _muxBuffer;
    private MuxHeaderV0 _headerV0 = new MuxHeaderV0();
    private MuxHeaderV3 _headerV3 = new MuxHeaderV3();
    private MuxHeaderV5 _headerV5 = new MuxHeaderV5();
    private MuxHeaderV6 _headerV6 = new MuxHeaderV6();
    
    @Override
    public String toString() {
      if (_connection != null) {
        return _connection.toString();
      }
      return "empty";
    }
    
    public MuxSocket(TcpChannel cnx) {
      _connection = cnx;
      _muxBuffer = new MuxBuffer();
    }
    
    void close() {
      _connection.close();
    }
    
    void shutdown() {
      _connection.shutdown();
    }
    
    TcpChannel getTcpChannel() {
      return _connection;
    }
    
    int handleData(ByteBuffer msg) throws IOException {
      int missingBytes;
      _muxBuffer.reset(msg);
      
      while (msg.hasRemaining()) {
        msg.mark();
        
        if ((missingBytes = demultiplex(_muxBuffer)) > 0) {
          msg.reset();
          return missingBytes;
        }
        
        // Run scheduled tasks between each demultiplex messages: this will give more
        // priority to scheduled tasks again incoming mux messages.
        //reactor.yield();
      }
      
      return 0;
    }
    
    /*********************************** Private methods. ******************************************/
    
    /**
     * Decode one mux message.
     * 
     * @return 0 if the message has been fully decoded, or the missing bytes
     *         required to parse one message.
     */
    private int demultiplex(MuxBuffer buffer) throws IOException {
      int off = 0, magicHdr = 0, magicTrl = 0, version = 0, flags = 0, length = 0, sockId = 0;
      int virtualPort = 0, readSize = 0;
      long sessionId = 0L;
      StringBuilder destIP = new StringBuilder();
      int destPort = 0;
      
      if (_keepAlive != null) {
        _keepAlive.received();
      }
      
      /* all MUX versions have a 4-byte magic header and a 1 byte version */
      if (!buffer.read(5)) {
        return 5;
      }
      off = buffer.getConsumed() - 5;
      magicHdr = MuxUtils.get_32(buffer.getData(), off, false);
      if (magicHdr != MAGIC_HEADER_VAL) {
        throw new IOException("Received bad magic header (" + Integer.toHexString(magicHdr)
            + "), disconnecting ...");
      }
      
      version = MuxUtils.get_8(buffer.getData(), off + 4);
      if (version == 5) {
        /* we now expect the flags and the length */
        if (!buffer.read(5)) {
          return 5;
        }
        flags = MuxUtils.get_8(buffer.getData(), off + MUX_FLAGS_OFFSET);
        length = MuxUtils.get_32(buffer.getData(), off + V5_LEN_OFFSET, true);
      } else {
        if (!buffer.read(V0_HEADER_SIZE - 5)) {
          /* next time we come back we want a full V0 MUX header */
          return V0_HEADER_SIZE - 5;
        }
        flags = MuxUtils.get_8(buffer.getData(), off + V0_FLAGS_OFFSET);
        length = MuxUtils.get_16(buffer.getData(), off + V0_LEN_OFFSET, false);
        sessionId = MuxUtils.get_64(buffer.getData(), off + V0_SESSIONID_OFFSET, false);
        sockId = MuxUtils.get_32(buffer.getData(), off + V0_CHANNELID_OFFSET, false);
        if (version == 1) {
          version = 0;
          length += 8;
        }
      }
      switch (version) {
      case 4:
        /**
         * Version 4 is used only for the platform internal messages.
         */
        readSize = length + MAGIC_TRAILER_LEN;
        if (!buffer.read(readSize)) {
          return readSize;
        }
        magicTrl = MuxUtils.get_32(buffer.getData(), buffer.getConsumed() - MAGIC_TRAILER_LEN, false);
        if (magicTrl != MAGIC_TRAILER_VAL) {
          throw new IOException("Received bad magic V4 trailer. Disconnecting ..");
        }
        off = buffer.getConsumed() - readSize; // points to the data
        if (_klvLogger.isDebugEnabled()) {
          StringBuilder sb = new StringBuilder();
          sb.append(toLogString() + " : rcvd: ");
          sb.append(logV4(flags, ByteBuffer.wrap(buffer.getData(), off, length)));
          _klvLogger.debug(sb.toString());
        }
        switch (flags) {
        case (ACK_MASK | PROTOCOL_CTL | ACTION_ALIVE):
          if (_keepAlive != null) {
            _keepAlive.recAlive();
          }
          break;
        case (PROTOCOL_CTL | ACTION_ALIVE):
          sendMuxAliveAck();
          break;
        case (PROTOCOL_CTL | ACTION_ID):
          Properties props = parseHello(new String(buffer.getData(), off, length));
          int idleFactor = 0;
          int keepAliveInterval = 0;
          String p = props.getProperty("idleFactor");
          if (p != null) {
            idleFactor = Integer.parseInt(p);
          }
          p = props.getProperty("keepAliveInterval");
          if (p != null) {
            keepAliveInterval = Integer.parseInt(p);
          }
          p = props.getProperty("instanceName");
          if (p != null) {
            if (_stackInstance == null) {
              _stackInstance = p;
            } else {
              // Sanity check: are we really connecting the right instance name ?
              if (!_stackInstance.startsWith(p)) {
                throw new IOException("Detected invalid peer stack instance name: expected=" + _stackInstance
                    + ", but got connected to " + p);
              }
            }
          }
          if (keepAliveInterval != 0) {
            if (_keepAlive != null) {
              _keepAlive.cancel();
            }
            /* this is not printed out for the fastcache connection, but it works */
            _klvLogger.info("fire negotiated keep alive interval=" + keepAliveInterval + " idleFactor="
                + idleFactor + " for " + MuxConnectionImpl.this.toLogString());
            _keepAlive = new MuxKeepAlive(1000 * keepAliveInterval, idleFactor);
            _keepAlive.schedule();
          } else { // the peer told us to turn off keep alive, if any
            if (_keepAlive != null) {
              _keepAlive.cancel();
              _keepAlive = null;
            }
          }

          break;
        
        case (ACK_MASK | PROTOCOL_CTL | ACTION_RELEASE):
          try {
            _handler.releaseAck(MuxConnectionImpl.this, sessionId);
          } catch (Throwable t) {
            _logger.error("Exception in handler while calling releaseAck", t);
          }
          break;
        case (PROTOCOL_CTL | ACTION_PING):
          sendMuxPing();
          break;
        
        case (PROTOCOL_CTL | ACTION_OVERLOAD):
          handleActionOverLoad(sockId);
          break;
        
        default:
        }
        return 0;
        
      case 6:
        // read the length (maybe 0) and the magic trailer
        readSize = length + MAGIC_TRAILER_LEN;
        if (!buffer.read(readSize)) {
          return readSize;
        }
        
        magicTrl = MuxUtils.get_32(buffer.getData(), buffer.getConsumed() - MAGIC_TRAILER_LEN, false);
        if (magicTrl != MAGIC_TRAILER_VAL) {
          throw new IOException("Received bad magic V6 trailer. Disconnecting ..");
        }
        
        off = buffer.getConsumed() - readSize; // points to the data
        if (_defaultLogger.isDebugEnabled()) {
          _defaultLogger.debug("demultiplex: sctp - length=" + length + ", readSize=" + readSize
              + ", offset=" + off);
        }
        
        if (_logger.isInfoEnabled()) {
          StringBuilder sb = new StringBuilder();
          sb.append("rcvd: ");
          sb.append(logV6(sessionId, sockId, flags, buffer.getData(), off, length));
          _logger.info(sb.toString());
        }
        
        if (_ignoreMux) {
          _headerV6.set(sessionId, sockId, flags);
          try {
            if (_byteBufferMode) {
              handleMuxDataByteBuffer(buffer, _headerV6, off, length);
            } else {
              _handler.muxData(MuxConnectionImpl.this, _headerV6, buffer.getData(), off, length);
            }
          } catch (Throwable t) {
            _logger.error("Exception in handler while calling muxData", t);
          }
          return 0;
        }
        break;
      
      case 0:
        // read the length (maybe 0) and the magic trailer
        readSize = length + MAGIC_TRAILER_LEN;
        if (!buffer.read(readSize)) {
          return readSize;
        }
        
        magicTrl = MuxUtils.get_32(buffer.getData(), buffer.getConsumed() - MAGIC_TRAILER_LEN, false);
        if (magicTrl != MAGIC_TRAILER_VAL) {
          throw new IOException("Received bad magic V0 trailer. Disconnecting ..");
        }
        
        off = buffer.getConsumed() - readSize; // points to the data
        
        if (_logger.isInfoEnabled()) {
          StringBuilder sb = new StringBuilder();
          sb.append("rcvd: ");
          sb.append(logV0(sessionId, sockId, flags, buffer.getData(), off, length));
          _logger.info(sb.toString());
        }
        
        if (_ignoreMux) {
          _headerV0.set(sessionId, sockId, flags);
          try {
            if (_byteBufferMode) {
              handleMuxDataByteBuffer(buffer, _headerV0, off, length);
            } else {
              _handler.muxData(MuxConnectionImpl.this, _headerV0, buffer.getData(), off, length);
            }
          } catch (Throwable t) {
            _logger.error("Exception in handler while calling muxData", t);
          }
          return 0;
        }
        break;
      
      case 1:
        // cannot happen
        return 0;
        
      case 2: // not used anymore
        _logger.error("Received unexpected V2 message");
        return 0;
        
      case 3: {
        if (!buffer.read(4)) {
          return 4;
        }
        length = MuxUtils.get_32(buffer.getData(), buffer.getConsumed() - 4, false);
        byte[] data = null;
        readSize = length + MAGIC_TRAILER_LEN;
        if (!buffer.read(readSize)) {
          return readSize;
        }
        
        data = buffer.getData();
        off = buffer.getConsumed() - readSize;
        magicTrl = MuxUtils.get_32(buffer.getData(), buffer.getConsumed() - MAGIC_TRAILER_LEN, false);
        if (magicTrl != MAGIC_TRAILER_VAL) {
          throw new IOException("Received bad V3 magic trailer, disconnecting ...");
        }
        
        // Specific: in V3, UDP socket data always starts with remote port/IP ...
        if (!_ignoreMux && flags == (PROTOCOL_UDP | ACTION_DATA)) {
          // TODO: check with DIMI: port should be encoded in big Indian ...
          destPort = MuxUtils.get_16(buffer.getData(), off, true);
          off = getAscii0(buffer.getData(), off + 2, destIP);
          length -= (off - (buffer.getConsumed() - readSize));
        }
        
        if (_logger.isInfoEnabled()) {
          StringBuilder sb = new StringBuilder(110 + length);
          sb.append("rcvd: ");
          if (!_ignoreMux && flags == (PROTOCOL_UDP | ACTION_DATA)) {
            sb.append(logV3UdpSocketData(sockId, flags, data, off, length, destIP.toString(), destPort,
                                         _flagsTable));
          } else {
            sb.append(logV3(sessionId, sockId, flags, ByteBuffer.wrap(data, off, length)));
          }
          _logger.info(sb.toString());
        }
        
        if (_ignoreMux) {
          _headerV3.set(sessionId, sockId, flags);
          try {
            if (_byteBufferMode) {
              handleMuxDataByteBuffer(buffer, _headerV3, off, length);
            } else {
              _handler.muxData(MuxConnectionImpl.this, _headerV3, data, off, length);
            }
          } catch (Throwable t) {
            _logger.error("Exception in handler while calling muxData", t);
          }
          return 0;
        }
      }
        break;
      case 5: {
        byte[] data = null;
        readSize = length + MAGIC_TRAILER_LEN;
        if (!buffer.read(readSize)) {
          return readSize;
        }
        
        data = buffer.getData();
        off = buffer.getConsumed() - readSize;
        magicTrl = MuxUtils.get_32(buffer.getData(), buffer.getConsumed() - MAGIC_TRAILER_LEN, false);
        if (magicTrl != MAGIC_TRAILER_VAL) {
          throw new IOException("Received bad V5 magic trailer, disconnecting ...");
        }
        
        if (_logger.isInfoEnabled()) {
          StringBuilder sb = new StringBuilder(110 + length);
          sb.append("rcvd: ");
          sb.append(logV5(flags, ByteBuffer.wrap(data, off, length)));
          _logger.info(sb.toString());
        }
        switch (flags) {
        case (ACK_MASK | PROTOCOL_CTL | ACTION_ALIVE):
          if (_keepAlive != null) {
            _keepAlive.recAlive();
          }
          break;
        case (PROTOCOL_CTL | ACTION_ALIVE):
          sendMuxAliveAck();
          break;
        default:
          _headerV5.set(flags);
          try {
            if (_byteBufferMode) {
              handleMuxDataByteBuffer(buffer, _headerV5, off, length);
            } else {
              _handler.muxData(MuxConnectionImpl.this, _headerV5, data, off, length);
            }
          } catch (Throwable t) {
            _logger.error("Exception in handler while calling muxData", t);
          }
          return 0;
        }
      }
        break;
      }
      
      int socketId = sockId > 0 ? sockId : 0;
      int errno = sockId < 0 ? -sockId : 0;
      
      switch (flags) {
      
      /*************************** TCP / SCTP ***************************/
      
      case (PROTOCOL_TCP | ACTION_LISTEN):
      case (PROTOCOL_TCP | ACTION_LISTEN | ACK_MASK): {
        try {
          if (version == 6) {
            receiveSctpListen(buffer, off, socketId, sessionId, errno);
          } else {
            int secure = MuxUtils.get_8(buffer.getData(), off);
            boolean isSecure = (secure == 1);
            off++;
            int sourcePort = MuxUtils.get_16(buffer.getData(), off, true);
            off += 2;
            StringBuilder sourceIP = new StringBuilder();
            getAscii0(buffer.getData(), off, sourceIP);
            
            if (!_ipv6Support) {
              _handler.tcpSocketListening(MuxConnectionImpl.this, socketId,
                                          MuxUtils.getIPAsInt(sourceIP.toString()), sourcePort, isSecure,
                                          sessionId, errno);
            } else {
              _handler.tcpSocketListening(MuxConnectionImpl.this, socketId, sourceIP.toString(), sourcePort,
                                          isSecure, sessionId, errno);
            }
          }
        } catch (Throwable t) {
          _logger.error("Exception in handler while calling tcpSocketListening", t);
        }
      }
        break;
      
      case (PROTOCOL_TCP | ACTION_OPEN_V4):
      case (PROTOCOL_TCP | ACTION_OPEN_V4 | ACK_MASK): {
        boolean isClient = (flags == (PROTOCOL_TCP | ACTION_OPEN_V4));
        
        try {
          if (version == 6) {
            receiveSctpSocketOpened(buffer, off, sessionId, sockId, isClient, errno);
          } else {
            int secure = MuxUtils.get_8(buffer.getData(), off);
            boolean isSecure = (secure == 1);
            int sourcePort = MuxUtils.get_16(buffer.getData(), off + 1, true);
            StringBuilder sourceIP = new StringBuilder();
            int shift = getAscii0(buffer.getData(), off + 3, sourceIP);
            int destinationPort = MuxUtils.get_16(buffer.getData(), shift, true);
            StringBuilder destinationIP = new StringBuilder();
            getAscii0(buffer.getData(), shift + 2, destinationIP);
            String remoteIp = (isClient) ? sourceIP.toString() : destinationIP.toString();
            int remotePort = (isClient) ? sourcePort : destinationPort;
            String localIp = (!isClient) ? sourceIP.toString() : destinationIP.toString();
            int localPort = (!isClient) ? sourcePort : destinationPort;
            
            // virtual IP and virtual port are no more used -> 0
            if (!_ipv6Support) {
              _handler.tcpSocketConnected(MuxConnectionImpl.this, socketId, MuxUtils.getIPAsInt(remoteIp),
                                          remotePort, MuxUtils.getIPAsInt(localIp), localPort, 0, 0,
                                          isSecure, isClient, sessionId, errno);
            } else {
              _handler.tcpSocketConnected(MuxConnectionImpl.this, socketId, remoteIp, remotePort, localIp,
                                          localPort, "", 0, isSecure, isClient, sessionId, errno);
            }
          }
        } catch (Throwable t) {
          _logger.error("Exception in handler while calling tcpSocketConnected", t);
        }
      }
        break;
      
      case (PROTOCOL_TCP | ACTION_CLOSE):
        if (version == 6) {
          sendV6(this, sessionId, sockId, PROTOCOL_TCP | ACTION_CLOSE | ACK_MASK, false, (ByteBuffer[]) null);
        } else {
          sendV0(this, sessionId, sockId, PROTOCOL_TCP | ACTION_CLOSE | ACK_MASK, false, (ByteBuffer[]) null);
        }
        
        //$FALL-THROUGH$
      case (PROTOCOL_TCP | ACTION_CLOSE | ACK_MASK):
        try {
          if (version == 6) {
            _handler.sctpSocketClosed(MuxConnectionImpl.this, sockId);
          } else {
            _handler.tcpSocketClosed(MuxConnectionImpl.this, sockId);
          }
        } catch (Throwable t) {
          _logger.error("Exception in handler while calling tcpSocketClosed", t);
        }
        break;
      
      case (PROTOCOL_TCP | ACTION_ABORT | ACK_MASK):
        try {
          _handler.tcpSocketAborted(MuxConnectionImpl.this, sockId);
        } catch (Throwable t) {
          _logger.error("Exception in handler while calling tcpSocketAborted", t);
        }
        break;
      
      case (PROTOCOL_TCP | ACTION_DATA):
        if (version == 6) { // SCTP
          try {
            receiveSctpData(buffer, off, length, sockId, sessionId);
          } catch (Throwable t) {
            _logger.error("Exception in handler while calling processing data for SCTP", t);
          }
        } else { // TCP
          try {
            if (_byteBufferMode) {
              handleTcpDataByteBuffer(buffer, off, length, sockId, sessionId);
            } else {
              _handler
                  .tcpSocketData(MuxConnectionImpl.this, sockId, sessionId, buffer.getData(), off, length);
            }
          } catch (Throwable t) {
            _logger.error("Exception in handler while calling tcpSocketData", t);
          }
        }
        break;
      
      /*************************** UDP ***************************/
      
      case (PROTOCOL_UDP | ACTION_LISTEN):
      case (PROTOCOL_UDP | ACTION_LISTEN | ACK_MASK):
      case (PROTOCOL_UDP | ACTION_OPEN_V4 | ACK_MASK): {
        int sourcePort = MuxUtils.get_16(buffer.getData(), off, true);
        StringBuilder sourceIP = new StringBuilder();
        getAscii0(buffer.getData(), off + 2, sourceIP);
        
        try {
          if (!_ipv6Support) {
            _handler.udpSocketBound(MuxConnectionImpl.this, (sockId > 0) ? sockId : 0,
                                    MuxUtils.getIPAsInt(sourceIP.toString()), sourcePort,
                                    (flags != (PROTOCOL_UDP | ACTION_OPEN_V4 | ACK_MASK)), sessionId,
                                    (sockId < 0) ? -sockId : 0);
          } else {
            _handler.udpSocketBound(MuxConnectionImpl.this, (sockId > 0) ? sockId : 0, sourceIP.toString(),
                                    sourcePort, (flags != (PROTOCOL_UDP | ACTION_OPEN_V4 | ACK_MASK)),
                                    sessionId, (sockId < 0) ? -sockId : 0);
          }
        } catch (Throwable t) {
          _logger.error("Exception in handler while calling udpSocketBound", t);
        }
      }
        break;
      
      case (PROTOCOL_UDP | ACTION_CLOSE):
        sendV0(this, sessionId, sockId, PROTOCOL_UDP | ACTION_CLOSE | ACK_MASK, false, (ByteBuffer[]) null);
        
        //$FALL-THROUGH$
      case (PROTOCOL_UDP | ACTION_CLOSE | ACK_MASK):
        try {
          _handler.udpSocketClosed(MuxConnectionImpl.this, sockId);
        } catch (Throwable t) {
          _logger.error("Exception in handler while calling udpSocketClosed", t);
        }
        break;
      
      case (PROTOCOL_UDP | ACTION_DATA):
        try {
          if (!_ipv6Support) {
            if (_byteBufferMode) {
              handleUdpDataByteBuffer(buffer, off, length, sockId, sessionId,
                                      MuxUtils.getIPAsInt(destIP.toString()), destPort, 0, // virtual IP not used anymore
                                      virtualPort); // virtual port not used anymore
            } else {
              _handler.udpSocketData(MuxConnectionImpl.this, sockId, sessionId,
                                     MuxUtils.getIPAsInt(destIP.toString()), destPort, 0, // virtual IP not used anymore
                                     0, // virtual port not used anymore
                                     buffer.getData(), off, length);
            }
          } else {
            if (_byteBufferMode) {
              handleUdpDataByteBuffer(buffer, off, length, sockId, sessionId, destIP.toString(), destPort,
                                      "" /* virtual IP */, 0 /* virtual Port */);
            } else {
              _handler.udpSocketData(MuxConnectionImpl.this, sockId, sessionId, destIP.toString(), destPort,
                                     "", // virtual IP
                                     0, // virtual Port
                                     buffer.getData(), off, length);
            }
          }
        } catch (Throwable t) {
          _logger.error("Exception in handler while calling udpSocketData", t);
        }
        break;
      
      /*************************** DNS ***************************/
      
      case (PROTOCOL_DNS | ACTION_GET_BY_ADDR):
      case (PROTOCOL_DNS | ACTION_GET_BY_NAME):
        String[] response = DNSParser.parseDNSResponse(sockId, buffer.getData(), off);
        try {
          if (flags == (PROTOCOL_DNS | ACTION_GET_BY_ADDR))
            _handler.dnsGetByAddr(sessionId, response, -sockId);
          else
            _handler.dnsGetByName(sessionId, response, -sockId);
        } catch (Throwable t) {
          _logger.error("Exception in handler while calling dnsGetByAddr", t);
        }
        break;
      
      /*************************** NONE ***************************/
      case (PROTOCOL_NONE | ACTION_DHT_GET):
      case (PROTOCOL_NONE | ACTION_TOPOLOGY_VIEW_REQUEST): {
        MuxHeader header = null;
        if (version != 3) {
          _logger.error("Received unexpected message version for protocol NONE: v=" + version);
          return 0;
        }
        _headerV3.set(sessionId, sockId, flags);
        header = _headerV3;
        try {
          if (_byteBufferMode) {
            handleMuxDataByteBuffer(buffer, header, off, length);
          } else {
            _handler.muxData(MuxConnectionImpl.this, header, buffer.getData(), off, length);
          }
        } catch (Throwable t) {
          _logger.error("Exception in handler while calling muxData", t);
        }
      }
        break;
      
      default:
        /*************************** OPAQUE ***************************/
        if ((flags & PROTOCOL_MASK) == PROTOCOL_OPAQUE) {
          MuxHeader header = null;
          if (version == 0) {
            _headerV0.set(sessionId, sockId, flags);
            header = _headerV0;
          } else {
            if (version == 2) {
              _logger.error("Received unexpected V2 message");
              return 0;
            }
          }
          try {
            if (_byteBufferMode) {
              handleMuxDataByteBuffer(buffer, header, off, length);
            } else {
              _handler.muxData(MuxConnectionImpl.this, header, buffer.getData(), off, length);
            }
          } catch (Throwable t) {
            _logger.error("Exception in handler while calling muxData", t);
          }
          break;
        }
        
        _logger.error("Stack sent unknown flags: " + flags);
        break;
      }
      
      return 0;
    }// demultiplex !
    
    private void handleMuxDataByteBuffer(MuxBuffer buffer, MuxHeader header, int off, int length) {
      ByteBuffer bb = buffer.getByteBuffer();
      int currPos = bb.position();
      int currLimit = bb.limit();
      
      try {
        bb.position(off);
        bb.limit(off + length);
        _handler.muxData(MuxConnectionImpl.this, header, bb);
      } finally {
        bb.limit(currLimit);
        bb.position(currPos);
      }
    }
    
    private void handleTcpDataByteBuffer(MuxBuffer buffer, int off, int length, int sockId, long sessionId) {
      ByteBuffer bb = buffer.getByteBuffer();
      int currPos = bb.position();
      int currLimit = bb.limit();
      
      try {
        bb.position(off);
        bb.limit(off + length);
        _handler.tcpSocketData(MuxConnectionImpl.this, sockId, sessionId, bb);
      } finally {
        bb.limit(currLimit);
        bb.position(currPos);
      }
    }
    
    private void handleUdpDataByteBuffer(MuxBuffer buffer, int off, int length, int sockId, long sessionId,
                                         int remoteIP, int remotePort, int virtualIP, int virtualPort) {
      ByteBuffer bb = buffer.getByteBuffer();
      int currPos = bb.position();
      int currLimit = bb.limit();
      
      try {
        bb.position(off);
        bb.limit(off + length);
        _handler.udpSocketData(MuxConnectionImpl.this, sockId, sessionId, remoteIP, remotePort, virtualIP,
                               virtualPort, bb);
      } finally {
        bb.limit(currLimit);
        bb.position(currPos);
      }
    }
    
    private void handleUdpDataByteBuffer(MuxBuffer buffer, int off, int length, int sockId, long sessionId,
                                         String remoteIP, int remotePort, String virtualIP, int virtualPort) {
      ByteBuffer bb = buffer.getByteBuffer();
      int currPos = bb.position();
      int currLimit = bb.limit();
      
      try {
        bb.position(off);
        bb.limit(off + length);
        _handler.udpSocketData(MuxConnectionImpl.this, sockId, sessionId, remoteIP, remotePort, virtualIP,
                               virtualPort, bb);
      } finally {
        bb.limit(currLimit);
        bb.position(currPos);
      }
    }
    
    /**
     * Parse a string of properties key1=value1;key2=value2;key3=value3
     * 
     * @param data the string to parse
     * @return a map of properties
     */
    private Properties parseHello(String data) {
      Properties props = new Properties();
      String[] pairs = data.split(";");
      for (String pair : pairs) {
        String[] keyValue = pair.split("=");
        if (keyValue.length == 2) {
          props.put(keyValue[0], keyValue[1]);
        } else {
          // TODO log ignored keyValue 
        }
      }
      return props;
    }
    
  }
  
  /**
   * @see com.nextenso.mux.MuxConnection#disableRead(int)
   */
  @Override
  public void disableRead(int sockId) {
    _lowPrioritySocket.getTcpChannel().disableReading();
  }
  
  public void receiveSctpListen(MuxBuffer buffer, int offset, int socketId, long sessionId, int errno) {
    int off = offset;
    int nbAddresses = MuxUtils.get_8(buffer.getData(), off);
    off++;
    int sourcePort = MuxUtils.get_16(buffer.getData(), off, true);
    off += 2;
    String[] sourceIps = new String[nbAddresses];
    StringBuilder sBuffer = new StringBuilder();
    for (int i = 0; i < nbAddresses; i++) {
      sBuffer.setLength(0);
      off = getAscii0(buffer.getData(), off, sBuffer);
      sourceIps[i] = sBuffer.toString();
    }
    int maxOutStreams = MuxUtils.get_16(buffer.getData(), off, true);
    off += 2;
    int maxInStreams = MuxUtils.get_16(buffer.getData(), off, true);
    off += 2;
    
    if (_defaultLogger.isDebugEnabled()) {
      _defaultLogger.debug("receiveSctpListen: nbAddresses=" + nbAddresses + ", source port=" + sourcePort
          + ", ips=" + Arrays.toString(sourceIps) + ", maxOutStreams=" + maxOutStreams + ", maxInStreams="
          + maxInStreams);
    }
    _handler.sctpSocketListening(MuxConnectionImpl.this, socketId, sessionId, sourceIps, sourcePort, false, errno);
  }
  
  public void receiveSctpSocketOpened(MuxBuffer buffer, int offset, long sessionId, int sockId,
                                      boolean isClient, int errno) {
    int off = offset;
    int nbSource = MuxUtils.get_8(buffer.getData(), off);
    off++;
    int sourcePort = 0;
    String[] sourceIps = new String[nbSource];
    if (nbSource > 0) {
      sourcePort = MuxUtils.get_16(buffer.getData(), off, true);
      off += 2;
      for (int i = nbSource; i > 0; i--) {
        StringBuilder ip = new StringBuilder();
        off = getAscii0(buffer.getData(), off, ip);
        sourceIps[nbSource - i] = ip.toString();
      }
    }
    
    int nbDestination = MuxUtils.get_8(buffer.getData(), off);
    off++;
    String[] destinationIps = new String[nbDestination];
    int destinationPort = 0;
    if (nbDestination > 0) {
      destinationPort = MuxUtils.get_16(buffer.getData(), off, true);
      off += 2;
      for (int i = nbDestination; i > 0; i--) {
        StringBuilder ip = new StringBuilder();
        off = getAscii0(buffer.getData(), off, ip);
        destinationIps[nbDestination - i] = ip.toString();
      }
    }
    int maxOutStreams = MuxUtils.get_16(buffer.getData(), off, true);
    off += 2;
    int maxInStreams = MuxUtils.get_16(buffer.getData(), off, true);
    off += 2;
    
    String[] remoteIps = (isClient) ? sourceIps : destinationIps;
    int remotePort = (isClient) ? sourcePort : destinationPort;
    String[] localIps = (!isClient) ? sourceIps : destinationIps;
    int localPort = (!isClient) ? sourcePort : destinationPort;
    
    if (_defaultLogger.isDebugEnabled()) {
      _defaultLogger.debug("receiveSctpSocketOpened: nbSource=" + nbSource + ", source port=" + sourcePort
          + ", ips=" + Arrays.toString(sourceIps) + ", nb Destination=" + nbDestination
          + ", destination port=" + destinationPort + ", ips" + Arrays.toString(destinationIps)
          + ", maxOutStreams=" + maxOutStreams + ", maxInStreams=" + maxInStreams);
    }
    
    _handler.sctpSocketConnected(MuxConnectionImpl.this, sockId, sessionId, remoteIps, remotePort, localIps,
                                 localPort, maxOutStreams, maxInStreams, isClient, false, errno);
  }
  
  public void receiveSctpData(MuxBuffer buffer, int offset, int length, int sockId, long sessionId) {
    int off = offset;
    int remotePort = MuxUtils.get_16(buffer.getData(), off, true);
    off += 2;
    StringBuilder ip = new StringBuilder();
    off = getAscii0(buffer.getData(), off, ip);
    String remoteAddress = ip.toString();
    boolean isUnordered = (MuxUtils.get_8(buffer.getData(), off) == 1);
    off++;
    boolean isComplete = (MuxUtils.get_8(buffer.getData(), off) == 1);
    off++;
    int ploadPID = MuxUtils.get_32(buffer.getData(), off, true);
    off += 4;
    int streamNumber = MuxUtils.get_16(buffer.getData(), off, true);
    off += 2;
    int ttl = MuxUtils.get_32(buffer.getData(), off, true);
    off += 4;
    
    if (_defaultLogger.isDebugEnabled()) {
      _defaultLogger.debug("receiveSctpData: remote  port=" + remotePort + ", address=" + remoteAddress
          + ", isUnordered=" + isUnordered + ", isComplete=" + isComplete + ", ploadPID=" + ploadPID
          + ", streamNumber=" + streamNumber + ", ttl=" + ttl);
      _defaultLogger.debug("receiveSctpData: byteBufferMode=" + _byteBufferMode + ", offset=" + off);
    }
    
    if (_byteBufferMode) {
      handleSctpDataByteBuffer(buffer, off, length, sockId, sessionId, remoteAddress, isUnordered,
                               isComplete, ploadPID, streamNumber);
    } else {
      ByteBuffer buf = buffer.getByteBuffer();
      int currentPosition = buf.position();
      buf.position(off);
      if (_defaultLogger.isDebugEnabled()) {
        _defaultLogger.debug("receiveSctpData: buffer=\n" + buf);
        StringBuilder sb = new StringBuilder();
        logData(sb, buf);
        _defaultLogger.debug("receiveSctpData: buffer=\n" + sb);
      }
      _handler.sctpSocketData(MuxConnectionImpl.this, sockId, sessionId, buf, remoteAddress, isUnordered,
                              isComplete, ploadPID, streamNumber);
      buf.position(currentPosition);
    }
  }
  
  private void handleSctpDataByteBuffer(MuxBuffer buffer, int off, int length, int sockId, long sessionId,
                                        String addr, boolean isUnordered, boolean isComplete, int ploadPID,
                                        int streamNumber) {
    ByteBuffer bb = buffer.getByteBuffer();
    int currPos = bb.position();
    int currLimit = bb.limit();
    
    try {
      bb.position(off);
      bb.limit(off + length);
      _handler.sctpSocketData(MuxConnectionImpl.this, sockId, sessionId, bb, addr, isUnordered, isComplete,
                              ploadPID, streamNumber);
    } finally {
      bb.limit(currLimit);
      bb.position(currPos);
    }
  }
  
  /**
   * @see com.nextenso.mux.MuxConnection#enableRead(int)
   */
  @Override
  public void enableRead(int sockId) {
    _lowPrioritySocket.getTcpChannel().enableReading();
  }
  
  private static final int put8(byte[] dest, int off, int val) {
    MuxUtils.put_8(dest, off, val);
    return off + 1;
  }
  
  private static final int put16(byte[] dest, int off, int val, boolean networkByteOrder) {
    MuxUtils.put_16(dest, off, val, networkByteOrder);
    return off + 2;
  }
  
  private static final int put32(byte[] dest, int off, int val, boolean networkByteOrder) {
    MuxUtils.put_32(dest, off, val, networkByteOrder);
    return off + 4;
  }
  
  //	private static final int put64(byte[] dest, int off, long val, boolean networkByteOrder) {
  //		MuxUtils.put_64(dest, off, val, networkByteOrder);
  //		return off + 8;
  //	}
  
  private static final int putAscii0(byte[] dest, int off, String data) {
    String ascii = data;
    if (ascii == null) {
      ascii = "";
    }
    for (int i = 0; i < ascii.length(); i++) {
      dest[off + i] = (byte) (ascii.charAt(i) & 0xff);
    }
    dest[off + ascii.length()] = '\0';
    return off + ascii.length() + 1;
  }
  
  private int getAscii0(byte[] b, int offset, StringBuilder sb) {
    int off = offset;
    int start = off;
    sb.setLength(0);
    for (; off < b.length && b[off] != '\0'; off++) {
      sb.append((char) (b[off] & 0xff));
    }
    return start + sb.length() + 1;
  }
  
  private static int getNextRnd() {
    return RND.getAndIncrement();
  }
  
  @Override
  public void setInputExecutor(Executor inputExecutor) {
    _lowPrioritySocket.getTcpChannel().setInputExecutor(inputExecutor);
  }
  
  private boolean connectionListenerCallback(Supplier<Boolean> cb) {
  	try {
  		if (_connectionListener != null) {
  			return cb.get();
  		}
  	} catch (Throwable err) {
  		_logger.error("connection listener exception", err);
  	}
  	return false;
  }
}
