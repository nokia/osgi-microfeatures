package com.nextenso.proxylet.engine;

// ProxyletAPI
import org.apache.log4j.Logger;

import com.nextenso.proxylet.Proxylet;
import com.nextenso.proxylet.mgmt.impl.ProxyletMBeansManager;

/**
 * The ProxyletContainer manages all the deployed Contexts. It is at the top of the whole
 * architecture.
 */
public class ProxyletContainer {
  
  protected ProxyletEngine _engine;
  
  protected Context _context;
  
  protected Logger _logger = Logger.getLogger("callout"); // default value to be
  
  // overridden
  /**
   * @deprecated remove empty Constructor
   */
  protected ProxyletContainer() {
  }
  
  protected ProxyletContainer(Logger logger) {
    // the subclass must instanciate the engine
    _logger = logger;
  }
  
  /**
   * Returns our Context
   */
  public Context getContext() {
    return _context;
  }
  
  /**
   * Sets the default Context
   */
  public void setContext(Context context) {
    this._context = context;
  }
  
  /**
   * Sets the engine.
   * 
   * @param engine The engine.
   */
  protected void setProxyletEngine(ProxyletEngine engine) {
    this._engine = engine;
  }
  
  /**
   * Returns the ProxyletEngine
   */
  public ProxyletEngine getProxyletEngine() {
    return _engine;
  }
  
  /*******************************************************
   * External calls
   *******************************************************/
  
  public void init(ProxyletApplication app) throws Exception {
    if (_context != null) {
      ProxyletMBeansManager.getInstance().registerProxyletMBeans(_context, app.getScope());
      _context.init(app);
    }
  }
  
  public void destroy() {
    // we call destroy on all the contexts
    if (_logger.isInfoEnabled()) {
      _logger.info("Starting to destroy the proxylets");
    }
    if (_context != null) {
      if (ProxyletUtils.isInAgentMode()) {
        ProxyletMBeansManager.getInstance().unregisterProxyletMBeans(_context);
      }
      _context.destroy();
    }
    _logger.info("All proxylets destroyed");
  }
  
  /**
   * Returns an instance of Proxylet given its classname and context. Returns null if it cannot
   * be found. The chainType can be for instance ProxyletChain.REQUEST_CHAIN or
   * ProxyletChain.RESPONSE_CHAIN. If contextId==null, it looks in all contexts and returns the
   * first instance found.
   */
  public Proxylet getProxylet(String className, int chainType) {
    ProxyletChain chain = null;
    if (_context == null)
      return null;
    ProxyletChain[] chains = _context.getProxyletChains();
    loop: for (int i = 0; i < chains.length; i++) {
      if (chains[i].getType() == chainType) {
        chain = chains[i];
        break loop;
      }
    }
    if (chain == null)
      throw new IllegalArgumentException("Invalid chain type:" + chainType);
    ProxyletEnv[] envs = chain.getValue();
    for (int i = 0; i < envs.length; i++) {
      Proxylet p = envs[i].getProxylet();
      // Don't compare className with p.getClass().getName() because p is a dynamic proxy ...
      if (envs[i].getProxyletClass().getName().equals(className))
        return p;
    }
    return null;
  }
}
