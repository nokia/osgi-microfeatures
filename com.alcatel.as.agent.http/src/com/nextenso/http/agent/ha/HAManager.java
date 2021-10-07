package com.nextenso.http.agent.ha;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.alcatel.as.session.distributed.SessionManager;
import com.alcatel.as.session.distributed.SessionType;
import com.alcatel.as.session.distributed.event.SessionEventFilter;
import com.alcatel.as.session.distributed.event.SessionListener;
import com.alcatel.as.session.distributed.smartkey.SmartKeyService;
import com.alcatel.as.util.config.ConfigHelper;
import com.alcatel_lucent.ha.services.HAContext;
import com.alcatel_lucent.ha.services.RecoveryService;
import com.nextenso.http.agent.Agent;
import com.nextenso.http.agent.AgentProperties;
import com.nextenso.http.agent.Client;
import com.nextenso.http.agent.HttpChannel;
import com.nextenso.http.agent.impl.HttpSessionFacade;

@Component
public class HAManager {
  private final static Logger logger = Logger.getLogger("agent.http.ha.HAManager");
  private volatile SessionManager _mgr; // injected
  private SessionType type;
  private static volatile HAManager _instance;
  private SessionListener activationSessionListener;
  private volatile Dictionary<String, ?> _sipConf;
  private volatile BundleContext _bctx;
  private volatile Dictionary<String, ?> _agentConf;
  
  public final static String K_PREFIX = "@";
  protected final static String K_INTERNAL_PREFIX = "#";
  public final static int K_PREFIX_LEN = 1;
  protected final static String K_JSESSIONID = "#ID";
  public final static String K_MAX_INACTIVE_INTERVAL = "#MII";
  protected final static String K_CREATION_TIME = "#CT";
  protected final static String K_CONTEXT_PATH = "#CP";
  protected final static int DS_OFFSET = 900; // 15 mn
  private final static String SUB_SESSION_SEPARATOR = "_";
  private volatile boolean httpHa;
  private volatile boolean convergentHa;
  private ConcurrentHashMap<String, Agent> _agents = new ConcurrentHashMap<String, Agent>();
  private SmartKeyService smartKeyService;

  private static final String SIP_NEWHA = "sipagent.newha";
  private static final String SIP_HA = "sipagent.ha";
  
  //	private static boolean newHA = false;
  private static volatile RecoveryService<HttpSessionFacade> _rs = null;
  
  @Reference
  protected void bindSessionManager(SessionManager sm) {
    _mgr = sm;
  }
  
  @Reference
  protected void setSmartKeyService(SmartKeyService service) {
    this.smartKeyService = service;
  }

  @Reference(target = "(service.pid=httpagent)", cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
  void bindAgentConf(Dictionary<String, ?> agentConf) {
    _agentConf = agentConf;
  }
  void unbindAgentConf(Dictionary<String, ?> agentConf) {
  }
  
  @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, target = "(service.pid=sipagent)")
  protected void bindSipConf(Dictionary<String, ?> sipConf) {
    _sipConf = sipConf;
    _start();
  }

  protected void unbindSipConf(Dictionary<String, ?> sipConf) {
  }
    
  @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
  protected void bindRecoveryService(RecoveryService<HttpSessionFacade> rs) {
    _rs = rs;
    _start();
  }

  protected void unbindRecoveryService(RecoveryService<HttpSessionFacade> rs) {
  }
  
  public void addAgent(String containerIndex, Agent agent) {
    _agents.put(containerIndex, agent);
  }
  
  @Activate
  protected void start(BundleContext bctx) {
    _bctx = bctx;
    _start();
  }
  
  protected synchronized void _start() {
    if (_instance != null) {
      return; // already started and registered.
    }
    
    if (_bctx == null) {
      return; // not yet active
    }

    // HTTP configuration
    httpHa = ConfigHelper.getBoolean(_agentConf, AgentProperties.SESSION_HA, false);
    boolean sipConv = ConfigHelper.getBoolean(_agentConf, AgentProperties.SIP_CONVERGENCE, false);
 
    if (sipConv) {
      convergentHa = false; 
      if (_sipConf == null) {
        return; // we must wait for SIP configuration
      }      
      else {
        convergentHa = ConfigHelper.getBoolean(_sipConf, SIP_NEWHA, false) && 
            ConfigHelper.getBoolean(_sipConf, SIP_HA, false);
      }
      
      if (convergentHa && _rs == null)  {
        return; // we must wait for recovery service
      }
    }
    
    // all required dependencies are there: initialize and register our service
    enableAdvancedHA(convergentHa);
    
    logger.info("HA enabled: " + isHaEnabled());
    
    if (! startManager(isHaEnabled())) {
      logger.error("Could not start session HA");
    }
    _instance = this;
    _bctx.registerService(HAManager.class.getName(), this, null);
  }
  
  protected SessionManager getSessionManager() {
    return _mgr;
  }

  public static HAManager getInstance() {
    return _instance;
  }

  public static void setInstance() { // just for tests
    _instance = new HAManager();
  }
  
  public boolean isHaEnabled() {
    return (httpHa | convergentHa);
  }
  
  public boolean startManager(boolean ha) {
    Hashtable<String, String> props = new Hashtable<String, String>();
    props.put(SessionType.TYPE_NAME, "httpagent");
    try {
      if (ha) {
        activationSessionListener = new HttpSessionRecoveryListener(_agents);
        this.type = _mgr.addSessionType(props, activationSessionListener, new SessionEventFilter(
            SessionEventFilter.EVENT_SESSION_ACTIVATED));
      } else {
        props.put(SessionType.TYPE_HA, Boolean.FALSE.toString());
        this.type = _mgr.addSessionType(props);
        
      }
      return true;
    } catch (IllegalArgumentException e) {
      logger.warn("startHA", e);
      return false;
    }
  }
  
  public void destroyActivationListener() {
    if ((activationSessionListener) != null && (type != null)) {
      _mgr.removeSessionTypeListener(type, activationSessionListener);
    }
  }
  
  public void createOrUpdateSession(Client client) {
    // Create/update asynchronously the distributed session
    if (client.getSession().getHAContext() == null) {
      new HttpSessionTransaction(client).createOrUpdate();
    } else {
      HAContext ha = client.getSession().getHAContext();
      try {
        _rs.passivate(ha, null);
      } catch (Exception e) {
        if (logger.isDebugEnabled()) {
          logger.debug(e.getMessage(), e);
        }
      }
    }
  }
  
  public void destroySession(Client client) {
    // Destroy asynchronously the distributed session
    new HttpSessionTransaction(client).destroy();
  }
  
  public void recoverSession(final Client client, final HttpChannel channel) {
    
    String sessionId = client.getSession().getRemoteId();
    
    if (sessionId != null) {
      if (sessionId.indexOf(SUB_SESSION_SEPARATOR) > 0) {
        // New HA
        final HttpSessionFacade sessiontmp = client.getSession();
        final HAContext ha = sessiontmp.getHAContext();
        if (ha == null) {
          channel.handleRequest();
          return;
        }
        _rs.activate(ha, new RecoveryService.ActivationCallback() {
          
          public void activated(boolean arg0) {
            HttpSessionFacade newSession = null;
            if (ha.content().size() != 0) {
              newSession = (HttpSessionFacade) ha.content().get(0);
              newSession.setId(sessiontmp.getId());
              client.setSession(newSession);
              newSession.updateMaxInactiveInterval();
            }
            
            if (client.isDsCreated()) {
              // Session has been recovered successfully
              if (client.sessionRecovered()) {
                channel.handleRequest();
              }
            } else {
              // Unregistered clid
              channel.handleRequest();
            }
          }
          
        });
        //}
      } else {
        new HttpSessionTransaction(client).recover(new RequestRecoverTransactionListener(channel));
      }
    } else {
      new HttpSessionTransaction(client).recover(new RequestRecoverTransactionListener(channel));
      
    }
  }
  
  public String createSessionId(long id) {
    return getSessionType().createSmartKey(String.valueOf(id));
  }
  
  public String getSubSessionSeparator() {
    return SUB_SESSION_SEPARATOR;
  }
  
  @SuppressWarnings("rawtypes")
  public static RecoveryService getRS() {
    return _rs;
  }
  
  public void enableAdvancedHA(boolean enabled) {
    HttpSessionRecoveryListener.newHA = enabled;
    if (logger.isDebugEnabled()) {
      logger.debug("new HA=" + enabled);
    }
  }
  
  public SessionType getSessionType() {
    return type;
  }

  public void removeAgent(String containerIndex) {
    _agents.remove(containerIndex);
  }

  public boolean isSmartKey(String key) {
    return smartKeyService.isSmartKey(key);
  }

}
