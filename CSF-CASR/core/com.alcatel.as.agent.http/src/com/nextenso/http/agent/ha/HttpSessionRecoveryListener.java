package com.nextenso.http.agent.ha;

import java.math.BigInteger;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.alcatel.as.session.distributed.event.SessionEvent;
import com.alcatel.as.session.distributed.event.SessionListener;
import com.alcatel_lucent.ha.services.HAContext;
import com.nextenso.http.agent.Agent;
import com.nextenso.http.agent.Client;
import com.nextenso.http.agent.Utils;

class HttpSessionRecoveryListener implements SessionListener {
  protected static boolean newHA = false;
  private final Map<String, Agent> _agents;
  private final static Logger logger = Logger.getLogger("agent.http.ha.HttpSessionRecoveryListener");
  
  public HttpSessionRecoveryListener(Map<String, Agent> agents) {
    _agents = agents;
  }
  
  public void handleEvent(List<SessionEvent> events) {
    for (int i = 0; i < events.size(); i++) {
      SessionEvent event = events.get(i);
      try {
        if (logger.isInfoEnabled()) {
          logger.info("Trying to recover session " + event.getSessionId());
        }
        String containerIndex = "1";
        String sessionId = event.getSessionId();
        int dash = sessionId.indexOf("-");
        if (dash != -1) {
          sessionId = sessionId.substring(0, dash);
          containerIndex = event.getSessionId().substring(dash + 1);
        }
        final String containerIndex$ = containerIndex;
        final long clid = new BigInteger(sessionId, 16).longValue();
        
        // look for clid from all elastic agents.
        Agent agent = _agents.get(containerIndex);
        if (agent != null) {
          final Utils utils = agent.getUtils();
          if (containerIndex.equals(utils.getContainerIndex())) {
            if (logger.isInfoEnabled()) {
              logger.info("recovering session " + sessionId + " in container index " + containerIndex$);
            }
            utils.getHttpExecutor().execute(new Runnable() {
              public void run() {
                Hashtable<Long, Client> clients = utils.getClients();
                Client client;
                synchronized (clients) {
                  client = (Client) clients.get(clid);
                }
                if (client == null) {
                  // If the client has not been already recovered (by a request), recover the session
                  if (newHA) {
                    HAContext ha = HAManager.getRS()
                        .context(Long.toHexString(new Client(clid, utils).getId()) + "-" + containerIndex$);
                    HAManager.getRS().activate(ha, null);
                  } else {
                    new HttpSessionTransaction(new Client(clid, utils)).recover(null);
                  }
                }
              }
            });
          }
        } else {
          logger.warn("Did not find any container for session " + event.getSessionId());
        }
      } catch (Exception e) {
        if (logger.isDebugEnabled()) {
          logger.debug("HA: cannot recover sid=" + event.getSessionId(), e);
        }
      }
    }
  }
}
