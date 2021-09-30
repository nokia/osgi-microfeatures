package com.nextenso.http.agent.ha;

import java.util.Map;

import com.alcatel_lucent.ha.services.HAContext;
import com.alcatel_lucent.ha.services.RecoveryServiceSupport;
import com.nextenso.http.agent.impl.HttpSessionFacade;

// Who is using this class ?
public class HttpRecoveryService extends RecoveryServiceSupport<HttpSessionFacade> {
  
  public HttpRecoveryService() {
    setSessionType("httpagent");
  }
  
  public HttpSessionFacade createRoot(HAContext context) {
    return null; // return new HttpSessionFacade(); // FIXME constructor needs utils !
  }
  
  @SuppressWarnings("rawtypes")
  @Override
  protected ClassLoader switchToApplicationClassLoader(Map map, String name) {
    return null;
  }
  
  @Override
  protected String[] recoveryKeys(String o) {
    if (!(o instanceof String)) {
      return null;
    } else {
      return new String[] { o };
    }
  }
}
