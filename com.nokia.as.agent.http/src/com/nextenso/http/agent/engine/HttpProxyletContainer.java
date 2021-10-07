package com.nextenso.http.agent.engine;

import static com.nextenso.http.agent.Utils.logger;

import java.util.Dictionary;

import com.nextenso.http.agent.impl.HttpSessionFacade;
import com.nextenso.proxylet.engine.ProxyletApplication;
import com.nextenso.proxylet.engine.ProxyletContainer;
import com.nextenso.proxylet.engine.ProxyletEnv;
import com.nextenso.proxylet.engine.criterion.Criterion;
import com.nextenso.proxylet.engine.criterion.FalseCriterion;
import com.nextenso.proxylet.engine.criterion.LogicalORCriterion;
import com.nextenso.proxylet.http.HttpRequest;
import com.nextenso.proxylet.http.HttpSession;

/**
 * The HttpProxyletContainer manages all the deployed proxylets of a given
 * group. It is at the top of the whole architecture.
 */
@SuppressWarnings("rawtypes")
public class HttpProxyletContainer extends ProxyletContainer {
  // public final static String WAP_AGENT = "WspGw";
  private String _appName;
  private Dictionary _systemconf; //injected by DM
  
  public void bindSystemConfig(Dictionary systemConfig) {
    _systemconf = systemConfig;
  }
  
  /**
   * Constructor
   */
  public HttpProxyletContainer() {
    super(logger);
    _appName = "Http";
    setProxyletEngine(new HttpProxyletEngine(this));
  }
  
  /**
   * Returns the app Name
   */
  public String getAppName() {
    return _appName;
  }
  
  /**
   * Returns the HttpProxyletContext for a given request or response
   */
  public HttpProxyletContext getHttpContext() {
    return (HttpProxyletContext) getContext();
  }
  
  /*******************************************************
   * External calls
   *******************************************************/  
  public void init(ProxyletApplication app) throws Exception {
    HttpProxyletContext ctx = new HttpProxyletContext(this, app);
    if (logger.isInfoEnabled()) {
      logger.info("Checking license for http proxylets");
    }
    if (logger.isInfoEnabled()) {
      logger.info("Http proxylets license check ok");
    }
    ctx.setSystemProperties(_systemconf);
    super.setContext(ctx);
    super.init(app);
  }
  
  public boolean init(HttpRequest req) {
    HttpProxyletContext context = getHttpContext();
    if (context == null)
      return false;
    context.init(req);
    return true;
  }
  
  public boolean init(HttpSession session, boolean created) {
    HttpProxyletContext context = getHttpContext();
    if (context == null)
      return false;
    HttpSessionFacade sessionFacade = (HttpSessionFacade) session;
    sessionFacade.setProxyletContext(context);
    if (created) {
      context.sessionCreated(session);
    } else {
      context.sessionDidActivate(session);
    }
    return true;
  }
  
  /**
   * Specifies the number of request proxylets deployed
   */
  public int requestProxyletsSize() {
    if (getHttpContext() == null)
      return 0;
    return getHttpContext().getRequestChain().getSize();
  }
  
  /**
   * Specifies the number of response proxylets deployed
   */
  public int responseProxyletsSize() {
    if (getHttpContext() == null)
      return 0;
    return getHttpContext().getResponseChain().getSize();
  }
  
  public Criterion getResponseCriterion() {
    Criterion c = null;
    if (getHttpContext() != null) {
      Criterion tmp_ctx = null;
      ProxyletEnv[] envs = getHttpContext().getResponseChain().getValue();
      for (int i = 0; i < envs.length; i++) {
        Criterion tmp_env = envs[i].getCriterion();
        tmp_ctx = (tmp_ctx == null) ? tmp_env : LogicalORCriterion.getInstance(tmp_ctx, tmp_env);
      }
      if (tmp_ctx != null)
        c = (c == null) ? tmp_ctx : LogicalORCriterion.getInstance(c, tmp_ctx);
    }
    return (c != null) ? c : FalseCriterion.getInstance();
  }
}
