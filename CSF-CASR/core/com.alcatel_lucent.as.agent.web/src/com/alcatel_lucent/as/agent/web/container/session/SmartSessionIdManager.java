package com.alcatel_lucent.as.agent.web.container.session;

import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;

import com.alcatel.as.session.distributed.SessionManager;
import com.alcatel.as.session.distributed.SessionType;
import com.alcatel.as.session.distributed.smartkey.SmartKeyService;

public class SmartSessionIdManager extends DefaultSessionIdManager {
  
  private SmartKeyService keyService;
  private long instanceId;
  private SessionManager sm;
  private SessionType type;

  public SmartSessionIdManager(SmartKeyService keyService, long instanceId, SessionManager sm, Server _server) {
    super(_server);
    this.keyService = keyService;
    this.instanceId = instanceId;
    this.sm = sm;
    if (sm != null) {
      Hashtable<String, String> props = new Hashtable<String, String>();
      props.put(SessionType.TYPE_NAME, "webagent");
      this.type = sm.addSessionType(props);
    }
  }

  @Override
  public String newSessionId(long seedTerm) {
    String uid = super.newSessionId(seedTerm);
    if (sm == null) {
      StringBuilder buf = new StringBuilder(256);
      return buf.append(keyService.createSmartKey(instanceId)).append('-').append(uid).toString();
    }
    return type.createSmartKey(uid);
  }
   
}
