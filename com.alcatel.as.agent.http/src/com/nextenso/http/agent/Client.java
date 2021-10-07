package com.nextenso.http.agent;

import static com.nextenso.http.agent.Utils.logger;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

import com.nextenso.agent.AgentConstants;
import com.nextenso.http.agent.impl.HttpMessageManager;
import com.nextenso.http.agent.impl.HttpRequestFacade;
import com.nextenso.http.agent.impl.HttpSessionFacade;
import com.nextenso.mux.MuxConnection;

public class Client implements HttpSessionFacade.SessionManager {
  /**
   * The default session timeout in milliseconds
   */
  private static long defaultSessionTimeout;
  /**
   * CRC used to create session ids.
   */
  private static CRC32 CRC = new CRC32();
  /**
   * Counter used to create session ids.
   */
  private static long uidCounter = 0L;
  
  /**
   * The session timeout in seconds for this client.
   */
  private long sessionTimeout = defaultSessionTimeout;
  private HttpSessionFacade session;
  private long clid;
  private boolean keepAlive;
  private boolean initialized;
  private boolean closed;
  private int reqId;
  private boolean invalidated;
  private int pendingRequests;
  private Future<?> timeoutTask;
  private boolean dsCreated;
  private boolean tempClid;
  private final Utils _utils;
  private boolean waitingForSwitchId;
  
  public static void setSessionTimeout(long value) {
    if (value == 0) {
      value = -1; // infinite
    }
    defaultSessionTimeout = value;
  }
  
  public static long getSessionTimeout(long minimum) {
    // returns the minimum if the timeout is lower
    return (defaultSessionTimeout >= minimum) ? defaultSessionTimeout : minimum;
  }
  
  private boolean isTempClid(long clid) {
    long masked = clid & 0xFFFFFFFF00000000L;
    return (masked == 0xDDDDDDDD00000000L);
  }
  
  public Client(long clid, Utils utils) {
    this(clid, true, utils);
  }
  
  public Client(long clid, boolean keepAlive, Utils utils) {
    _utils = utils;
    init(clid, keepAlive);
    if (logger.isDebugEnabled()) {
      logger.debug(this + " created");
    }
  }
  
  public Utils getUtils() {
    return _utils;
  }
  
  public void switchId(long newClid) {
    if (logger.isDebugEnabled()) {
      logger.debug("switch clid; old=" + Long.toHexString(this.clid) + ", new=" + Long.toHexString(newClid));
    }
    this.clid = newClid;
    tempClid = false;
    waitingForSwitchId = false;
  }
  
  public void init(long clid, boolean keepAlive) {
    this.initialized = false;
    this.closed = false;
    this.keepAlive = keepAlive;
    this.tempClid = isTempClid(clid);
    this.session = new HttpSessionFacade(this, false, _utils);
    this.clid = clid;
    this.reqId = System.identityHashCode(this);
    
    // Generate a unique id across all platforms for our agent.
    session.setId(generateSessionId());
    // we need lastAccessTime for launchTimoutTask()
    session.updateAccessedTime();
    if (keepAlive) {
      launchTimoutTask();
    }
  }
  
  public static void shutdown() {
    if (logger.isDebugEnabled())
      logger.debug("Stopping Client Timer task");
    defaultSessionTimeout = -1; // do not try to use the timer anymore
  }
  
  public boolean isDsCreated() {
    return dsCreated;
  }
  
  public boolean isNew() {
    return !initialized;
  }
  
  public boolean isKeepAlive() {
    return keepAlive;
  }
  
  public boolean isTempClid() {
    return this.tempClid;
  }
  
  public boolean isWaitingForSwitchId() {
    return waitingForSwitchId;
  }
  
  public void setDsCreated(boolean created) {
    dsCreated = created;
  }
  
  public synchronized void accessed() {
    session.updateAccessedTime();
  }
  
  public synchronized void accessedOnReq() {
    session.updateAccessedTimeOnReq();
  }
  
  public long getId() {
    return clid;
  }
  
  public HttpSessionFacade getSession() {
    return session;
  }
  
  public String toString() {
    return "Client [clid=" + Long.toHexString(clid) + ",clip=" + session.getRemoteAddr() + "]";
  }
  
  /*******************************************
   * HttpSessionFacade.SessionManager interface
   *******************************************/
  
  /**
   * used when invalidating a session already synchronized on session
   */
  public synchronized void invalidateSession(HttpSessionFacade theSession) {
    if (invalidated || closed) {
      return;
    }
    
    if (logger.isDebugEnabled())
      logger.debug("Invalidating Session "+this);
    
    invalidated = true;
    
    // Unregister our Client.
    removeClient();
    
    if (pendingRequests <= 0 && keepAlive) {
      // Send close session to the stacks
      synchronized (_utils.getConnectionManager()) {
        Enumeration<?> enumer = _utils.getConnectionManager().getMuxConnections();
        while (enumer.hasMoreElements()) {
          MuxConnection connection = (MuxConnection) enumer.nextElement();
          HttpChannel.closeSession(connection, this);
        }
      }
      // close ourselves
      close(false);
    } else {
      if (timeoutTask != null) {
        timeoutTask.cancel(false);
      }
    }
  }
  
  public synchronized int getMaxInactiveInterval() {
    if (sessionTimeout <= 0) {
      return -1;
    } else {
      return (int) (sessionTimeout / 1000L);
    }
  }
  
  public synchronized void setMaxInactiveInterval(int interval) {
    sessionTimeout = (interval <= 0) ? -1 : (long) (interval * 1000);
    if (timeoutTask != null) {
      timeoutTask.cancel(false);
      launchTimoutTask();
    }
  }
  
  public synchronized void complete() {
  }
  
  public synchronized String newSession() {
    if (logger.isDebugEnabled()) {
      logger.debug("newSession clid=" + Long.toHexString(this.clid));
    }
    // re-init of the client
    this.invalidated = false;
    this.dsCreated = false;
    
    // re-insert the client into the list
    Hashtable<Long, Client> clients = _utils.getClients();
    synchronized (clients) {
      clients.put(this.clid, this);
    }
    
    // Save some attributes
    String remoteAddr = session.getRemoteAddr();
    
    // Create a new session
    this.session = new HttpSessionFacade(this, false, _utils);
    // Generate a unique id across all platforms for our agent.
    session.setId(generateSessionId());
    // we need lastAccessTime for launchTimoutTask()
    session.updateAccessedTime();
    // New remote id
    String remoteId = Long.toString(session.getId());
    session.setRemoteId(remoteId);
    // Remote address
    session.setRemoteAddr(remoteAddr);
    if (keepAlive) {
      launchTimoutTask();
    }
    waitingForSwitchId = true;
    _utils.getContainer().init(session, true);
    return remoteId;
  }
  
  @Override
  public String changeSessionId() {
    if (logger.isDebugEnabled()) {
      logger.debug("changeSessionId clid=" + Long.toHexString(this.clid));
    }
    // Generate a unique id across all platforms for our agent.
    session.setId(generateSessionId());
    // New remote id
    String remoteId = Long.toString(session.getId());
    session.setRemoteId(remoteId);
    // To save all the attributes
    session.setAllAttributesModified();
    // clients map will be updated with the session ID switch (see Agent.muxData)
    return session.getRemoteId();
  }

  /*******************************************
   * Timeout & close
   *******************************************/
  
  private void launchTimoutTask() {
    if (sessionTimeout <= 0) {
      return;
    }
    
    // We put an offset of 100 milliseconds to compensate for the task triggering
    long delay = 100 + sessionTimeout - (System.currentTimeMillis() - session.getAccessedTime());
    if (delay < 0)
      delay = 100;
    if (logger.isDebugEnabled())
      logger.debug("launchTimeoutTask : sessionTimeout=" + sessionTimeout + ", delay=" + delay);
    
    timeoutTask = _utils.getTimerService().schedule(_utils.getHttpExecutor(), new SessionTimeoutTask(),
                                                    delay, TimeUnit.MILLISECONDS);
  }
  
  private boolean hasTimedOut() {
    return (sessionTimeout <= 0) ? false
        : ((System.currentTimeMillis() - session.getAccessedTime()) > sessionTimeout);
  }
  
  public void close(boolean keepDistributedSession) {
    synchronized (this) {
      if (closed) {
        removeClient();
        return;
      }
      
      if (logger.isDebugEnabled())
        logger.debug("Closing " + this);
      
      closed = true;
      
      if (timeoutTask != null) {
        timeoutTask.cancel(false);
      }
      
      // Keep or close the session
      if (dsCreated) {
        if (!keepDistributedSession) {
          dsCreated = false;
          session.close();
        }
      } else {
        session.close();
      }
    }
    
    removeClient();
  }
  
  /*******************************************
   * Utility Methods called by Command
   *******************************************/
  
  public synchronized HttpRequestFacade makeRequest() {
    pendingRequests++;
    return HttpMessageManager.makeRequest(session, reqId++);
  }
  
  public boolean init(HttpRequestFacade req, String remoteIp) {
    String headerName = _utils.getClipHeaderName();
    String remoteAddr = (headerName != null) ? req.getHeader(headerName) : null;
    if (remoteAddr != null) 
      session.setRemoteAddr(remoteAddr); // From header 
    else
      session.setRemoteAddr(remoteIp); // From tcpSocketConnect
    if (!initialized) {
      // the clid is still unknown
      headerName = _utils.getClidHeaderName();
      String remoteId = (headerName != null) ? req.getHeader(headerName) : null;
      session.setRemoteId(remoteId);
      if (!_utils.getContainer().init(session, true))
        return false;
      initialized = true;
    }
    _utils.getContainer().init(req);
    return true;
  }
  
  public boolean sessionRecovered() {
    if (!_utils.getContainer().init(session, false))
      return false;
    initialized = true;
    return true;
  }
  
  public void sessionPassivate() {
    _utils.getContainer().getHttpContext().sessionWillPassivate(session);
  }
  
  public synchronized int getPendingRequests() {
    return pendingRequests;
  }
  
  public synchronized void decPendingRequests() {
    pendingRequests--;
  }
  
  
  // Called when a channel request has been replied.
  protected boolean requestDone(boolean channelRemoved) {
    boolean closeSession = false;
    if (logger.isDebugEnabled()) {
      logger.debug("Request done "+this);
    }
    
    if (!keepAlive) {
      // Discard our Client once the http response has been sent.
      close(false);
    } else {
      synchronized (this) {
        if (channelRemoved) {
          if ((--pendingRequests) < 0) {
            pendingRequests = 0;
          }
          if (invalidated && pendingRequests == 0) {
            /* send sessionClose to the stack */
            closeSession = true;
            close(false);
          }
        }
      }
      // If it is temporary CLID and there is no session cookie or session attributes, destroy
      // this client
      if (tempClid && !session.isAccessed()) {
        close(false);
      }
    }
    return closeSession;
  }
  
  /*******************************************
   * Private methods
   *******************************************/
  
  public void removeClient() {
    Hashtable<Long, Client> clients = _utils.getClients();
    if (clients != null) {
      synchronized (clients) {
        clients.remove(clid);
      }
    }
  }
  
  private long generateSessionId() {
    StringBuilder sb = new StringBuilder(AgentConstants.PLATFORM_UID);
    synchronized (CRC) {
      sb.append(uidCounter++);
      CRC.reset();
      CRC.update(sb.toString().getBytes());
      return CRC.getValue();
    }
  }
  
  /***************************************
   * Inner class that checks Timeout
   ***************************************/
  
  private class SessionTimeoutTask implements Runnable {
    public void run() {
      if (timeoutTask.isCancelled())
        return;
      try {
        Client client = Client.this;
        synchronized (client) {
          if (client.closed) {
            // the user was killed
          } else if (client.hasTimedOut()) {
            if (logger.isDebugEnabled())
              logger.debug("Session : Timeout : " + client);
            invalidated = true;
            // The session is likely to timeout itself in the stack: don't send sessionClose to
            // the stack.
            client.close(false);
          } else {
            client.launchTimoutTask();
          }
        }
      } catch (Throwable t) {
        logger.error("Exception while executing Session Timeout Task", t);
      }
    }
  }
  
  // FIXME GVQ: Test for new HA
  public void setSession(HttpSessionFacade newSession) {
    session = newSession;
    session.setSessionMngr(this);
    session.setSecure(false);
  }
  
}
