package com.alcatel_lucent.as.service.jetty.common.webapp;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;

public class ConvergentEnvConfiguration extends EnvConfiguration {
  
  private transient com.alcatel_lucent.as.service.jetty.common.webapp.AbstractWebAppContext _ctx;

  @Override
  public void preConfigure(WebAppContext context) throws Exception {
    super.preConfigure(context);
    // If the application is convergent, put the SipFactory in the java:comp/env (see JSR289)
    if(((com.alcatel_lucent.as.service.jetty.common.webapp.AbstractWebAppContext) context).isConvergent()) {
      Context iCtx = new InitialContext();
      Context local = (Context)iCtx.lookup("java:comp/env");
      Context localsip = local.createSubcontext("sip");
      Context localconv = localsip.createSubcontext(_ctx.getConvergentAppName());
      localconv.bind("SipFactory", _ctx.getSipFactory());
      localconv.bind("SipSessionsUtil", _ctx.getSipSessionsUtil());
    }
  }  
  
}
