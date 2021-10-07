package com.nextenso.http.agent.ha;

import static com.nextenso.http.agent.Utils.logger;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.alcatel.as.session.distributed.Session;
import com.alcatel.as.session.distributed.Session.Attribute;
import com.alcatel.as.session.distributed.SessionException;
import com.alcatel.as.session.distributed.Transaction;
import com.alcatel.as.session.distributed.TransactionListener;
import com.nextenso.http.agent.Client;
import com.nextenso.http.agent.impl.HttpSessionFacade;

class HttpSessionTransaction {  
  private Client client;
  private String containerIndex;
  
  public HttpSessionTransaction(Client client) {
    this.client = client;
    this.containerIndex = client.getUtils().getContainerIndex();
  }
  
  public void createOrUpdate() {
    HAManager
        .getInstance()
        .getSessionManager()
        .execute(new Transaction(HAManager.getInstance().getSessionType(), Long.toHexString(client.getId()) + "-" + containerIndex,
                     Transaction.TX_CREATE_GET | Transaction.TX_SERIALIZED) {
                   private static final long serialVersionUID = 1L;
                   
                   public void execute(Session ds) throws SessionException {
                     if (ds != null) {
                       HttpSessionFacade session = client.getSession();
                       if (ds.created()) {
                         // New session
                         client.setDsCreated(true);
                         // Set session management parameters
                         ds.setAttribute(HAManager.K_JSESSIONID, session.getRemoteId());
                         ds.setAttribute(HAManager.K_CONTEXT_PATH, session.getContextPath());
                         ds.setAttribute(HAManager.K_CREATION_TIME, Long.valueOf(session.getCreationTime()));
                         int interval = session.getMaxInactiveInterval();
                         ds.setAttribute(HAManager.K_MAX_INACTIVE_INTERVAL, Integer.valueOf(interval));
                         if (interval > 0) {
                           ds.setAttribute("_duration", interval + HAManager.DS_OFFSET);
                         }
                         // set attributes
                         handleModifications(ds, session);
                         if (logger.isDebugEnabled()) {
                           logger.debug("Creating sid=" + ds.getSessionId());
                           logAttributes(ds);
                         }
                         ds.commit(null);
                       } else {
                         // Existing session
                         if (handleModifications(ds, session)) {
                           // Modified session
                           if (logger.isDebugEnabled()) {
                             logger.debug("Modifying sid=" + ds.getSessionId());
                             logAttributes(ds);
                           }
                           ds.commit(null);
                         } else {
                           // Unmodified session
                           ds.rollback(null);
                         }
                       }
                     }
                   }
                 }, new GetOrCreateTransactionListener());
  }
  
  public void recover(TransactionListener listener) {
    if (listener == null) {
      listener = new DefaultRecoverTransactionListener();
    }
    HAManager
        .getInstance()
        .getSessionManager()
        .execute(new Transaction(HAManager.getInstance().getSessionType(), Long.toHexString(client.getId()) + "-" + containerIndex,
                     Transaction.TX_GET | Transaction.TX_SERIALIZED) {
                   private static final long serialVersionUID = 1L;
                   
                   public void execute(Session ds) throws SessionException {
                     if (ds != null) {
                       if (logger.isDebugEnabled()) {
                         logger.debug("Recovering sid=" + ds.getSessionId());
                         logAttributes(ds);
                       }
                       // initialize session
                       client.setDsCreated(true);
                       HttpSessionFacade session = client.getSession();
                       session.sessionCookieSet();
                       session.updateAccessedTime();
                       session.updateAccessedTimeOnReq();
                       // Retrieve session management parameters
                       // - Retrieve sessionId
                       session.setRemoteId((String) ds.getAttribute(HAManager.K_JSESSIONID));
                       // - Retrieve context path
                       session.setContextPath((String) ds.getAttribute(HAManager.K_CONTEXT_PATH));
                       // - Retrieve creation time
                       session.setCreationTime((Long) ds.getAttribute(HAManager.K_CREATION_TIME));
                       // - Retrieve max inactive interval
                       session.setMaxInactiveInterval((Integer) ds
                           .getAttribute(HAManager.K_MAX_INACTIVE_INTERVAL));
                       // Retrieve attributes from DS
                       restoreAttributes(ds, session);
                       ds.commit(null);
                     }
                   }
                 }, listener);
  }
  
  public void destroy() {
    HAManager
        .getInstance()
        .getSessionManager()
        .execute(new Transaction(HAManager.getInstance().getSessionType(), Long.toHexString(client.getId()) + "-" + containerIndex,
                     Transaction.TX_GET | Transaction.TX_SERIALIZED) {
                   private static final long serialVersionUID = 1L;
                   
                   public void execute(Session ds) throws SessionException {
                     if (ds != null) {
                       if (logger.isDebugEnabled()) {
                         logger.debug("destroy DS sid=" + ds.getSessionId());
                       }
                       ds.destroy(null);
                     }
                   }
                 }, null);
    
  }
  
  /*--- Private methods ---*/
  
  @SuppressWarnings("rawtypes")
  protected boolean handleModifications(Session ds, HttpSessionFacade session) {
    Set<String> params = session.getModifiedParams().keySet();
    int nbParams = params.size();
    for (String param : params) {
      try {
        // Attributes
        if (param.startsWith(HAManager.K_PREFIX)) {
          Object value = session.getAttribute(param.substring(HAManager.K_PREFIX_LEN));
          if (value != null) {
            if (value instanceof Serializable) {
              if (logger.isDebugEnabled()) {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                if (loader != null) {
                  String className = null;
                  try {
                    Class cl = value.getClass();
                    while (cl != null) {
                      className = cl.getName();
                      if (!className.startsWith("java.")) {
                        loader.loadClass(className);
                      }
                      cl = cl.getSuperclass();
                    }
                  } catch (Exception e) {
                    logger.debug("ClassLoader " + loader.toString() + " cannot load class " + className);
                  }
                }
              }
              ds.setAttribute(param, (Serializable) value);
            }
          } else {
            ds.removeAttribute(param, false);
          }
        }
        // Session management parameters
        else if (param.startsWith(HAManager.K_INTERNAL_PREFIX)) {
          if (HAManager.K_MAX_INACTIVE_INTERVAL.compareTo(param) == 0) {
            int interval = session.getMaxInactiveInterval();
            ds.setAttribute(param, Integer.valueOf(interval));
            if (interval > 0) {
              ds.setAttribute("_duration", interval + HAManager.DS_OFFSET);
            } else {
              ds.setAttribute("_duration", -1);
            }
          }
        }
      } catch (SessionException e) {
        if (logger.isDebugEnabled()) {
          logger.debug("Cannot set or remove attribute " + param, e);
        }
      }
    }
    params.clear();
    return (nbParams != 0);
  }
  
  @SuppressWarnings("rawtypes")
  private void restoreAttributes(Session ds, HttpSessionFacade session) {
    try {
      List attrNames = ds.getAttributes();
      Iterator iter = attrNames.iterator();
      while (iter.hasNext()) {
        Attribute attribute = (Attribute) iter.next();
        String name = attribute.getName();
        Object value = attribute.getValue();
        if ((value != null) && name.startsWith(HAManager.K_PREFIX) && (name.length() > 1)) {
          session.restoreAttribute(name.substring(HAManager.K_PREFIX_LEN), attribute.getValue());
        }
      }
    } catch (SessionException e) {
      logger.error("Could not restore HA attributes", e);
    }
  }
  
  @SuppressWarnings("rawtypes")
  protected void logAttributes(Session ds) {
    try {
      List attrNames = ds.getAttributes();
      Iterator iter = attrNames.iterator();
      while (iter.hasNext()) {
        Attribute attribute = (Attribute) iter.next();
        logger.debug(attribute.getName() + "=" + attribute.getValue().toString());
      }
    } catch (SessionException e) {
      logger.error("could not log attributes", e);
    }
  }
  
  /*--- Transaction listeners ---*/
  
  private class GetOrCreateTransactionListener implements TransactionListener {
    
    public void transactionCompleted(Transaction tr, Serializable result) {
    }
    
    public void transactionFailed(Transaction tr, SessionException result) {
      logger.error(result.getMessage() + " sid=" + tr.getSessionId(), result);
    }
    
  }
  
  private class DefaultRecoverTransactionListener implements TransactionListener {
    
    public void transactionCompleted(Transaction tr, Serializable result) {
      Hashtable<Long, Client> clients = client.getUtils().getClients();
      synchronized (clients) {
        if (client.sessionRecovered()) {
          // Put the client in the public map
          clients.put(client.getId(), client);
        }
        if (logger.isDebugEnabled() && client.isDsCreated()) {
          logger.debug("HTTP session recovered sid=" + Long.toHexString(client.getId()));
        }
      }
    }
    
    public void transactionFailed(Transaction tr, SessionException result) {
      logger.error(result.getMessage() + " sid=" + tr.getSessionId(), result);
    }    
  }  
}
