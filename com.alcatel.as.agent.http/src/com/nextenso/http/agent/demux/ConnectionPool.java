package com.nextenso.http.agent.demux;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import com.nextenso.http.agent.Utils;
import com.nextenso.http.agent.demux.client.HttpClientSocketUser;
import com.nextenso.http.agent.impl.HttpRequestFacade;
import com.nextenso.mux.MuxConnection;
import com.nextenso.proxylet.http.HttpRequest;

public class ConnectionPool {

  private ConcurrentHashMap<MuxConnection, ConcurrentHashMap<Long, EndPoint>> connections;
  private ConcurrentHashMap<Long, DemuxClientSocket> pendingSockets = new ConcurrentHashMap<Long, DemuxClientSocket>();
  private AtomicLong currentId = new AtomicLong();
  private final ReentrantLock lock = new ReentrantLock();
  private Utils utils;

  public ConnectionPool(Utils utils) {
    this.utils = utils;
    this.connections = new ConcurrentHashMap<MuxConnection, ConcurrentHashMap<Long, EndPoint>>();
  }

  public void addConnection(MuxConnection connection) {
    ConcurrentHashMap<Long, EndPoint> endPoints = new ConcurrentHashMap<Long, EndPoint>();
    endPoints.put(0L, new EndPoint(null, 0, false, connection, 0)); // End-point used by HTTP clients
    connections.put(connection, endPoints);
  }
  
  public String getNextProxy(String host) {
    return utils.getAgent().getNextHopEvaluator().getNextProxy(host);
  }

  public void removeConnection(MuxConnection connection) {
    connections.remove(connection);
  }

  public void addEndPoint(MuxConnection mux, String localIP, int localPort, boolean secure, int sockId) {
    long cnxId = ((localPort & 0xFFFFFFFFL) << 32) | (localIP.hashCode() & ((long) 0xFFFFFFFFL));
    ConcurrentHashMap<Long, EndPoint> endPoints = connections.get(mux);
    if (endPoints != null) {
      endPoints.put(cnxId, new EndPoint(localIP, localPort, secure, mux, sockId));
    }
  }
  

  public boolean hasEndPoint(MuxConnection mux, String localIP, int localPort) {
    long cnxId = ((localPort & 0xFFFFFFFFL) << 32) | (localIP.hashCode() & ((long) 0xFFFFFFFFL));
    ConcurrentHashMap<Long, EndPoint> endPoints = connections.get(mux);
    if (endPoints != null) {
      return endPoints.contains(cnxId);
    }
    
    return false;
  }
  
  public void removeEndPoint(MuxConnection mux, int sockId) {
    ConcurrentHashMap<Long, EndPoint> endPoints = connections.get(mux);
    if (endPoints != null) {
      for(Entry<Long, EndPoint> entry : endPoints.entrySet()) {
        long key = entry.getKey();
        if (key > 0) {
          if (entry.getValue().sockId == sockId) {
            endPoints.remove(key);
            break;
          }
        }
      }
    }
  }
  
  private String getLocalSourceIp(MuxConnection mux) {
    ConcurrentHashMap<Long, EndPoint> endPoints = connections.get(mux);
    String previous = null;
    for(EndPoint endPoint : endPoints.values()) {
      if (endPoint.getLocalPort() > 0) {
        String localIp = endPoint.getLocalIp();
        if (previous != null) {
          if (!previous.equals(localIp)) return null;
        }
        previous = localIp;          
      }
    }
    return previous;    
  }
  
  public boolean isSameConnection(MuxConnection mux, long cnxId, InetSocketAddress address) {
    ConcurrentHashMap<Long, EndPoint> endPoints = connections.get(mux);
    if (endPoints != null) {      
      EndPoint endPoint = endPoints.get(cnxId);
      if (endPoint != null) {
        return endPoint.address.equals(address);
      }
    }
    return false;
  }

  public boolean isSelfConnection(InetSocketAddress address) {
    if (utils.getAgent().selfConnectionProhibited()) {      
      for(ConcurrentHashMap<Long, EndPoint> endPoints : connections.values()) {      
        for(EndPoint endPoint : endPoints.values()) {
          if (endPoint.getLocalPort() > 0) {
            if (endPoint.address.equals(address)) return true;
          }
        }
      }
    }
    return false;
  }
  
  public boolean isHostName(String host) {
    char c = host.charAt(0);
    if (c >= '0' && c <='2') return false;
    if (host.indexOf(':') >= 0) return false;
    return true;
  }
  
  public String getViaContent(MuxConnection mux, long cnxId) {
    ConcurrentHashMap<Long, EndPoint> endPoints = connections.get(mux);
    if (endPoints != null) {      
      EndPoint endPoint = endPoints.get(cnxId);
      if (endPoint != null) {
        StringBuilder buf = new StringBuilder(64);
        buf.append("\t, 1.1 ");
        buf.append(endPoint.getLocalIp());
        buf.append(":");
        buf.append(endPoint.getLocalPort());
        return buf.toString();
      }
    }
    return null;
  }

  public DemuxClientSocket getProxySocket(MuxConnection mux, long cnxId, String host, int port,
                                    HttpRequestFacade request, HttpPipeline pipeline, ByteBuffer[] data, boolean retry) {
    ConcurrentHashMap<Long, EndPoint> endPoints = connections.get(mux);
    if (endPoints != null) {
      EndPoint endPoint = endPoints.get(cnxId);
      if (endPoint != null) {        
        if (endPoint.secure == request.isSecure()) {
          lock.lock();
          try {
            String destination = host.toLowerCase() + ":" + port;
            DemuxClientSocket socket = null;
            if (!retry) socket = endPoint.getIdleSocket(destination);
            if (socket != null) {
              if (Utils.logger.isDebugEnabled()) Utils.logger.debug("proxy: reuse "+socket);
              ProxySocketUser user = new ProxySocketUser(pipeline, request.getMethod());
              socket.access();
              // Send immediately: return "false" means "error while writing data"
              if (pipeline.sendProxyRequestData(socket.getSockId(), data)) {
                // Request has been written successfully
                socket.setUser(user, true);
                socket.setTimeout(getTimeout(request));
                return socket;              
              }
              else {
                if (Utils.logger.isInfoEnabled()) Utils.logger.info("proxy: cannot reuse "+socket);
                // anyway, close the socket
                mux.sendTcpSocketClose(socket.getSockId());
                // then, continue with new socket
              }
            }
            long id  = 0;
            while(id == 0) {
              id = currentId.incrementAndGet() & 0x7FFFFFFFFFFFFFFFL;
            }
            
            boolean success = mux.sendTcpSocketConnect(id, host, port, 0, 0, request.isSecure());
            if (success) {
              if (Utils.logger.isDebugEnabled()) Utils.logger.debug("proxy: new socket to "+destination);
              socket = new DemuxClientSocket(destination, this);
              ProxySocketUser user = new ProxySocketUser(pipeline, request.getMethod());
              socket.setUser(user, false);
              socket.setTimeout(getTimeout(request));
              user.putData(data);
              pendingSockets.put(id, socket);
              return socket;
            }            
          }
          finally {
            lock.unlock();
          }
        }
        else {
          // FIXME secure issue ???
        }
      }
    }
    return null;
  }
  
  public DemuxClientSocket getTunnelSocket(MuxConnection mux, String to, int port, HttpPipeline pipeline, HttpRequestFacade request) {
    lock.lock();
    try {
      long id  = 0;
      while(id == 0) {
        id = currentId.incrementAndGet() & 0x7FFFFFFFFFFFFFFFL;
      }
      if (mux.sendTcpSocketConnect(id, to, port, 0, 0, request.isSecure())) {
        String destination = to.toLowerCase() + ":" + port;
        if (Utils.logger.isDebugEnabled()) Utils.logger.debug("tunnel: new socket to "+destination);
        DemuxClientSocket socket = new DemuxClientSocket(destination, this);
        TunnelSocketUser user = new TunnelSocketUser(pipeline);
        socket.setUser(user, false);
        socket.setTimeout(getTimeout(request));
        pendingSockets.put(id, socket);
        return socket;
      }
    }
    finally {
      lock.unlock();
    }    
    return null;
  }
  
  public DemuxClientSocket getHttpClientSocket(MuxConnection mux, String host, String to, int port, 
                                               HttpRequestFacade request, long id, HttpClientSocketUser user, boolean retry) {
    lock.lock();
    try {  
      String destination = host.toLowerCase() + ":" + port;
      ConcurrentHashMap<Long, EndPoint> endPoints = connections.get(mux);
      DemuxClientSocket socket = null;
      EndPoint endPoint = endPoints.get(0L);
      if (!retry) socket = endPoint.getIdleSocket(destination);
      if (socket != null) {
        if (Utils.logger.isDebugEnabled()) Utils.logger.debug("client: reuse "+socket);
        socket.setUser(user, true);
        socket.setTimeout(getTimeout(request));
        socket.access();
        return socket;
      }
      else {
        while(id <= 0) {
          id = currentId.incrementAndGet() & 0x7FFFFFFFFFFFFFFFL;
        }
        boolean success = false;
        String localIp = getLocalSourceIp(mux);
        if (localIp != null) {
          success = mux.sendTcpSocketConnect(id, to, port, endPoint.getLocalIp(), 0, request.isSecure());
        }
        else {
          success = mux.sendTcpSocketConnect(id, to, port, 0, 0, request.isSecure());
        }
        if (success) {
          if (Utils.logger.isDebugEnabled()) Utils.logger.debug("client: new socket to "+destination);
          socket = new DemuxClientSocket(destination, this);
          socket.setUser(user, false);
          socket.setTimeout(getTimeout(request));
          pendingSockets.put(id, socket);
          return socket;
        }
      }
    }
    finally {
      lock.unlock();
    }
    return null;
  }

  public void socketConnected(MuxConnection mux, long id, int sockId, int errno) {
    lock.lock();
    try {          
      DemuxClientSocket socket = pendingSockets.remove(id);
      if (socket != null) {
        if (errno == 0) {
          socket.socketConnected(sockId);
          mux.getSocketManager().addSocket(socket);
        }
        else {
          socket.socketError(errno);
        }
      }
    }
    finally {
      lock.unlock();
    }
  }

  public void releaseClientSocket(MuxConnection mux, long cnxId, DemuxClientSocket socket) {
    ConcurrentHashMap<Long, EndPoint> endPoints = connections.get(mux);
    if (endPoints != null) {      
      EndPoint endPoint = endPoints.get(cnxId);
      if (endPoint != null) {
        endPoint.addIdleSocket(socket);
      }
    }
  }

  public void clientSocketClosed(MuxConnection mux, long cnxId, DemuxClientSocket socket) {
    ConcurrentHashMap<Long, EndPoint> endPoints = connections.get(mux);
    if (endPoints != null) {
      EndPoint endPoint = endPoints.get(cnxId);
      if (endPoint != null) {
        endPoint.removeSocket(socket);
      }      
    }
  }
  
  private long getTimeout(HttpRequest req) {
	  Object customTimeout = req.getAttribute(HttpRequest.TIMEOUT_ATTR_KEY);
	  
	  if(customTimeout == null) {
		  return HttpPipeline.SOCKET_TIMEOUT * 1000;
	  } else if(customTimeout instanceof Long) {
		  return (Long) customTimeout;
	  } else if(customTimeout instanceof Duration) {
		  return ((Duration) customTimeout).toMillis();
	  }
	  
	  return HttpPipeline.SOCKET_TIMEOUT * 1000;
  }
  
  public Future<?> scheduleTimeout(Runnable task, int seconds) {
    return utils.getTimerService().schedule(utils.getHttpExecutor(), task, seconds, TimeUnit.SECONDS);
  }
  
  public Future<?> scheduleTimeout(Runnable task, long millis) {
	    return utils.getTimerService().schedule(utils.getHttpExecutor(), task, millis, TimeUnit.MILLISECONDS);
  }
  
  private static class EndPoint implements SocketUser {

    InetSocketAddress address;
    boolean secure;
    private final ReentrantLock lock = new ReentrantLock();
    private HashMap<String, LinkedList<DemuxClientSocket>> pool;
    private MuxConnection mux;
    private int sockId;

    EndPoint(String localIP, int localPort, boolean secure, MuxConnection mux, int sockId) {
      if (localIP == null) {
        this.address = new InetSocketAddress(localPort);
      }
      else {        
        this.address = new InetSocketAddress(localIP, localPort);
      }
      this.secure = secure;
      this.mux = mux;
      this.sockId = sockId;
      this.pool = new HashMap<String, LinkedList<DemuxClientSocket>>();
    }

    private String getLocalIp() {
      return address.getHostString();
    }

    private int getLocalPort() {
      return address.getPort();
    }

    private void addIdleSocket(DemuxClientSocket socket) {
      lock.lock();
      if (Utils.logger.isDebugEnabled()) Utils.logger.debug("add idle " + socket);
      socket.setUser(this, false);
      try {
        String destination = socket.getDestination();
        LinkedList<DemuxClientSocket> queue = pool.get(destination);
        if (queue == null) {
          queue= new LinkedList<DemuxClientSocket>();
          pool.put(destination, queue);
        }
        queue.offer(socket);
      }
      finally {
        lock.unlock();
      }
    }

    private void removeSocket(DemuxClientSocket socket) {
      lock.lock();
      if (Utils.logger.isDebugEnabled()) Utils.logger.debug("remove idle " + socket);
      try {
        LinkedList<DemuxClientSocket> queue = pool.get(socket.getDestination());
        if (queue != null) {        
          queue.remove(socket);
          if (queue.isEmpty()) pool.remove(socket.getDestination());
        }
      }
      finally {
        lock.unlock();
      }
    }    

    private DemuxClientSocket getIdleSocket(String destination) {
      lock.lock();
      try {
        LinkedList<DemuxClientSocket> queue = pool.get(destination);
        if (queue != null) {
          return queue.pollLast(); // poll first or last inactive socket?
        }
        return null;
      }
      finally {
        lock.unlock();
      }
    }

    //--- SocketUser
    
    @Override
    public void dataReceived(byte[] data, int off, int len) { }

    @Override
    public void connected(int sockId) { }

    @Override
    public void closed(DemuxClientSocket socket, boolean aborted) { 
      removeSocket(socket);
    }

    @Override
    public void error(int errno) { }

    @Override
    public void timeout(DemuxClientSocket socket) {
      mux.sendTcpSocketClose(socket.getSockId());
    }

    @Override
    public boolean isTunneling() {
      return false;
    }

  }

}
