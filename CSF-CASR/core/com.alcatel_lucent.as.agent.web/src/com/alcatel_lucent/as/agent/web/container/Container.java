package com.alcatel_lucent.as.agent.web.container;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.InitialContext;
import javax.servlet.Filter;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServlet;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.HouseKeeper;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.appmbeans.ApplicationMBeanFactory;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.TimerService;
import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.Monitorable;
import com.alcatel.as.service.metering2.SimpleMonitorable;
import com.alcatel.as.service.reporter.api.CommandScopes;
import com.alcatel.as.session.distributed.SessionManager;
import com.alcatel.as.session.distributed.smartkey.SmartKeyService;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel.as.util.config.ConfigHelper;
import com.alcatel_lucent.as.agent.web.container.deployer.ContextDeployer;
import com.alcatel_lucent.as.agent.web.container.metrics.MeteringHandler;
import com.alcatel_lucent.as.agent.web.container.session.SmartSessionIdManager;
import com.alcatel_lucent.as.agent.web.muxhandler.WebAgentSocket;
import com.alcatel_lucent.as.management.annotation.stat.Counter;
import com.alcatel_lucent.as.management.annotation.stat.Gauge;
import com.alcatel_lucent.as.management.annotation.stat.Stat;
import com.alcatel_lucent.as.service.jetty.common.connector.BufferConnector;
import com.alcatel_lucent.as.service.jetty.common.connector.EndPointManager;
import com.alcatel_lucent.as.service.jetty.common.deployer.WebApplication;
import com.alcatel_lucent.as.service.jetty.common.handler.HandlerHelper;
import com.alcatel_lucent.as.service.jetty.common.utils.JettyUtils;
import com.alcatel_lucent.as.service.jetty.common.webapp.WebAppClassLoader;
import com.nextenso.mux.MuxConnection;

/**
 * This is the Web container 
 * It is registered as an OSGi service for the MuxHandler.
 */

@Stat(rootSnmpName = "alcatel.srd.a5350.WebAgent", rootOid = { 637, 71, 6, 1130 })
@Component(provides=Container.class)
public class Container implements EndPointManager {
  
  public final static int SCAVENGE_PERIOD = 60; // in seconds

  private final static Logger LOGGER = Logger.getLogger("agent.web.container");

  private static final int APP_WEB_AGENT = 288;
  private static final String MODULE_WEB_AGENT = "WebAgent";

  private LogService logService;
  /** our protocol executor */
  private PlatformExecutor executor; // injected
  /** the ApplicationMBeanFactory service */
  private ApplicationMBeanFactory appMbeanFactory; //injected
  private TimerService timerService; //injected
  private SessionManager sm; //injected

  /** our protocol name */
  private final static String PROTOCOL = "web";
  private File tmpDirBase;
  private BufferConnector connector;
  private Server server;
  private StatisticsHandler statisticsHandler;
  private ContextDeployer contextDeployer;
  private Dictionary<?, ?> conf;
  private Properties _overrideDescriptors;
  private PlatformExecutor tpExecutor;
  private MeteringHandler meteringHandler;

  private AtomicInteger channels = new AtomicInteger();
  
  // ------------------ Declarative Service dependencies and life cycle ---------------------------------------------

  @Inject
  BundleContext bContext;

  @ServiceDependency
  void bind(LogServiceFactory logFactory)
  {
    logService = logFactory.getLogger(LOGGER.getName());
  }

  @ServiceDependency 
  public void setInitialContext(InitialContext ctx) { }

  @ServiceDependency(filter = "(service.pid=system)")
  private Dictionary<String, String> systemConfig;

  @ServiceDependency
  private PlatformExecutors execs;

  @ServiceDependency
  private SmartKeyService smartKeyService;
  
  @ServiceDependency
  private MeteringService meteringService;

  /** 
   * dependency on our protocol executor.
   */
  @ServiceDependency(filter="(id="+PROTOCOL+")")
  public void bindExecutor(PlatformExecutor exec) {
    executor = exec;
  }

  /** dependency on the ApplicationMBeanFactory service */
  @ServiceDependency 
  public void bindApplicationMBeanFactory(ApplicationMBeanFactory factory) {
    appMbeanFactory = factory;
  }

  @ServiceDependency(filter="(strict=false)")
  public void bindTimerService(TimerService timerService) {
    this.timerService = timerService;
  }
  
  public void registerCnxMonitorable(SimpleMonitorable mon, MuxConnection muxCnx) {
	  if(muxCnx != null && mon != null) {
		  meteringHandler.registerMuxCnxMonitorable(muxCnx, (SimpleMonitorable) mon);
	  }
  }
  
  public void unregisterCnxMonitorable(MuxConnection muxCnx) {
	  if(muxCnx != null) {
		  meteringHandler.unregisterMuxCnxMonitorable(muxCnx);
	  }
  }

  @ConfigurationDependency(pid="webagent") 
  void updated(Dictionary<?, ?> conf) { 
    if (conf != null) { 
    	if (LOGGER.isDebugEnabled()) LOGGER.debug("config updated: " + conf);
    	this.conf = conf;
    	_overrideDescriptors = new Properties();
    	String descriptors = (String) conf.get(AgentProperties.OVERRIDE_DESCRIPTOR);
    	if (descriptors != null) {
    		try (Reader r = new BufferedReader(new StringReader(descriptors))) {
    			_overrideDescriptors.load(r);
    		}  catch (Exception e) {
    			LOGGER.error("could not read web agent configuration " + AgentProperties.OVERRIDE_DESCRIPTOR + " property", e);
    		}
    	}
    }
  }

  /**
   * Inject a new web application
   * Notice that this dependency is required: this is because we are using a LifecycleController,
   * which blocks our service startup until we invoke our _registrationTrigger.run() method ...
   * So, optional dependencies can't be used here, since optional dependency callbacks are trigger
   * once the component is started. Notice that in a next version of DM, optional dependency callbacks
   * might be triggered before the component is started (but after all required dependencies have been
   * injected. In this case we'll be able to get rid of our {@link FakeWebApplication} service, which is 
   * only used to avoid being deactivated once we lose every deployed applications.
   * 
   * @param serviceProperties the web application service properties (optionally contain context-path)
   * @param webapp the web application registered by the web app deployer
   */
  @ServiceDependency(required=false, removed = "unbind")
  void bind(Map<String, String> serviceProperties, final WebApplication webapp) {
    // Get the context path, possibly stored in the web application "context-path" OSGi service property.
    final String contextPath = serviceProperties.get("context-path");
    final HashMap<String, HttpServlet> servlets = new HashMap<String, HttpServlet>(webapp.getServlets());
    final HashMap<String, Filter> filters = new HashMap<String, Filter>(webapp.getFilters());
    final HashMap<String, ServletContextListener> listeners = new HashMap<String, ServletContextListener>(webapp.getListeners());
    logService.warn("Deploying webapp " + webapp);
    executor.execute(new Runnable() { 
      public void run() {
        try {
        	contextDeployer.setOverrideDescriptor(null); // reset previous descriptors
        	String descriptorPath = _overrideDescriptors.getProperty(webapp.getBundle().getSymbolicName());
        	if(descriptorPath != null) {
        		File _overrideDescriptor = new File(descriptorPath);
        		if (_overrideDescriptor.exists()) {
        			  logService.warn(webapp+ " : descriptor overridden by "+ _overrideDescriptor);
        			contextDeployer.setOverrideDescriptor(_overrideDescriptor);
        		} else {
        	          logService.warn(webapp+ " : descriptor override failed : file "+ _overrideDescriptor + " doesn't exist");
        		}
        	}
          contextDeployer.deploy(webapp.getBundle(), 
                                 contextPath, 
                                 tmpDirBase, 
                                 servlets, 
                                 filters, 
                                 listeners);
          webapp.initDone();
        } catch (IOException e) {
          logService.error("Failed to deploy " + webapp, e);
        }
      }
    });
  }

  void unbind(Map<String, String> serviceProperties, final WebApplication webapp) {
    final String contextPath = serviceProperties.get("context-path");
    logService.warn("Deploying webapp " + webapp);
    executor.execute(new Runnable() { 
      public void run() {
        try {
          contextDeployer.undeploy(webapp.getBundle(), contextPath);
        } catch (IOException e) {
          logService.error("Failed to undeploy " + webapp, e);
        }
      }
    });
  }
  
  @SuppressWarnings("serial")
  @ServiceDependency(required=false, filter="(protocol=mux)")
  void bindHttpClientTransport(HttpClientTransport transport) {
    logService.debug("bindHttpClientTransport " + transport);
    HttpClient httpClient = new HttpClient(transport, null);
    try {
      httpClient.setExecutor(executor);
      httpClient.start();
      bContext.registerService(HttpClient.class.getName(), httpClient, new Hashtable<String, Object>() {
        {
          put("transport", "mux");
        }
      });      
    }
    catch (Exception e) {
      logService.error("Cannot register HttpClient", e);
    }
  }

  @Init
  public void init(org.apache.felix.dm.Component component) {
    boolean ha = ConfigHelper.getBoolean(conf, AgentProperties.HIGH_AVAILABILITY, false);
    if (ha) {
      DependencyManager dm = component.getDependencyManager();
      component.add(dm.createServiceDependency().setService(SessionManager.class).setAutoConfig("sm").setRequired(true));
    }
  }

  /** 
   * container activation: all required dependencies are available 
   */
  @Start
  public void activate() {
    logService.debug("container activated");
    executor.execute(new Runnable() {      
      @Override
      public void run() {
        start();       
      }
    });
  }

  @Stop
  public void deactivate() {
    logService.debug("container deactivated");
    if (server != null) {
      try {
        server.stop();
        server = null;
      }
      catch (Exception e) { }
    }
  }

  public void destroy() {
    if (logService.isInfoEnabled()) {
      logService.info("Destroying container");
    }
  }

  @SuppressWarnings("serial")
  private void start() {    
    // First of all, change class-loader (OSGI constraint for ClassLoader hierarchy)
    ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
    logService.info("starting ... CL="+Thread.currentThread().getContextClassLoader());

    JettyUtils.setLogger(LOGGER);
    
    // If some custom webapp shared resource classpath dirs are specified, initialize the webapp classloader.
    String resourceDirs = ConfigHelper.getString(conf, AgentProperties.WEBAPP_RESOURCE_DIRS, "");
    if (resourceDirs.length() > 0) {
    	try {
    		WebAppClassLoader.setSharedWebAppResourceClassPath(resourceDirs.split(","));
    	} catch (MalformedURLException e) {
    		logService.error("Invalid parameter for property " + AgentProperties.WEBAPP_RESOURCE_DIRS + " : " + resourceDirs, e);
    	}
    }

    // Get tmp dir for webapp deployments
    String relativePath = "webagent/";
    relativePath += ConfigHelper.getString(systemConfig, "group.name") + "/";
    relativePath += ConfigHelper.getString(systemConfig, "instance.name");
    tmpDirBase = JettyUtils.getTmpDir(relativePath);
    if (tmpDirBase == null) {
      logService.warn("Bad tmp path: " + relativePath);
    }

    // Read properties
    int timeout = ConfigHelper.getInt(conf, AgentProperties.SESSION_TIMEOUT, 1800);
    long requestBlockingTimeout = ConfigHelper.getInt(conf, AgentProperties.REQUEST_BLOCKING_TIMEOUT, -1);
    int reqBufSize = ConfigHelper.getInt(conf, AgentProperties.HEADER_REQBUFSIZE);
    int rspBufSize = ConfigHelper.getInt(conf, AgentProperties.HEADER_RSPBUFSIZE);
    int outBufSize = ConfigHelper.getInt(conf, AgentProperties.HEADER_OUTPUTBUF);
    String extraConf = (String) conf.get(AgentProperties.EXTRACONF);
    String contextDirName = (String) conf.get(AgentProperties.EXTERNAL_CONTEXTDIR);
    String restRootPath = (String) conf.get(AgentProperties.OSGI_HTTP_SERVICE_PATH);
    boolean useIoPool = AgentProperties.IO_TP.equals(ConfigHelper.getString(conf, AgentProperties.EXECUTOR, AgentProperties.IO_TP));
    long instanceId = ConfigHelper.getLong(systemConfig, ConfigConstants.INSTANCE_ID, 1L);
    boolean showServicePathsOn404 = ConfigHelper.getBoolean(conf, AgentProperties.SHOW_SERVICE_PATHS_ON_404);    

    try {
      meteringHandler = new MeteringHandler(meteringService);
      tpExecutor = useIoPool ? execs.getIOThreadPoolExecutor() : execs.getProcessingThreadPoolExecutor();
      server = JettyUtils.createServer(tpExecutor, timerService, this.getClass().getPackage().getName());
      // set connector      
      connector = JettyUtils.createConnector(server, this, outBufSize, reqBufSize, rspBufSize, requestBlockingTimeout);

      // extra configuration
      if (extraConf != null) {
        logService.info("Add extra configuration:\n" + extraConf);
        try {
          JettyUtils.addXmlConfiguration(server, extraConf);
        }
        catch (Exception e)
        {
          logService.warn("Wrong Jetty server extra configuration",e);
        }
      }
      // Set session id manager
      server.setSessionIdManager(new SmartSessionIdManager(smartKeyService, instanceId, sm, server));

      // Set handlers
      HandlerHelper handlerHelper = new HandlerHelper(server, showServicePathsOn404);
      ContextHandlerCollection chc = new ContextHandlerCollection();
      ServletContextHandler httpServiceContext = null;
      if (bContext != null && restRootPath != null && restRootPath.trim().length() > 0) {
        DefaultSessionIdManager sessionManager = new DefaultSessionIdManager(server);
        HouseKeeper houseKeeper = new HouseKeeper();
        houseKeeper.setIntervalSec(Container.SCAVENGE_PERIOD);
        sessionManager.setSessionHouseKeeper(houseKeeper);
        httpServiceContext = JettyUtils.createHttpService(server, bContext, handlerHelper, restRootPath, sessionManager, timeout);
      }      
      statisticsHandler = JettyUtils.buildHandlers(chc, LOGGER.getName()+".requests", httpServiceContext, handlerHelper);
      HandlerCollection handlers = new HandlerCollection(new Handler[] {statisticsHandler, meteringHandler});
      
      server.setHandler(handlers);

      // Set Context deployer
      contextDeployer = new ContextDeployer(appMbeanFactory, timeout, sm);
      contextDeployer.setContexts(chc);
      server.addBean(contextDeployer);

      // Set external context deployer
      JettyUtils.createExternalContextProvider(server, chc, contextDirName);

      server.start();
      
      bContext.registerService(Object.class.getName(), this, new Hashtable<String, Object>() {
        {
          put(ConfigConstants.MODULE_ID, APP_WEB_AGENT);
          put(ConfigConstants.MODULE_NAME, MODULE_WEB_AGENT);
          put(CommandScopes.COMMAND_SCOPE, CommandScopes.APP_COUNTER_SCOPE);
        }
      });
      
      logService.warn("JETTY-" + Server.getVersion() + " started");
    }
    catch (Exception e) {
      logService.error("cannot start web agent", e);
    }
    finally {
      Thread.currentThread().setContextClassLoader(oldCl);
    }
  }

  public void addClient(WebAgentSocket socket) {
    if (server != null) {
      channels.incrementAndGet();
      connector.addEndPoint(socket.getEndPoint());
    }
  }

  public void removeClient(WebAgentSocket socket) {
    if (server != null) {
      channels.decrementAndGet();
      socket.getEndPoint().close(false);
    }
  }
  
  public PlatformExecutor getTpExecutor(){
    return tpExecutor;
  }

  public PlatformExecutor getIOExecutor(){
    return execs.getIOThreadPoolExecutor();
  }

  // EndPointManager
  @Override
  public void messageReceived(Object socket, ByteBuffer... bufs) throws IOException {
    ((WebAgentSocket) socket).serverData(bufs);    
  }

  @Override
  public void connectionClosed(Object socket) {
    ((WebAgentSocket) socket).closedByServer();     
  }

  @Override
  public String newSession(Object socket) {
    // unused by WebAgent
    return null;
  }

  @Override
  public String changeSessionId(Object socket) {
    // unused by WebAgent
    return null;
  }
  
  /**
   * Used by the Agent to initialize the MuxHandler meters object. Must be started
   * by Agent.
   */
  public SimpleMonitorable getContainerMonitorable() {
	  return meteringHandler.getParentMonitorable();
  }

  // Statistics
  @Gauge(index = 0, desc = "Number of webapps currently deployed")
  public int getDeployedWebapps() {
    return contextDeployer.getDeployments();
  }

  @Gauge(index = 1, desc = "Number of opened channels")
  public int getOpenedChannels() {
    return channels.get();
  }

  @Gauge(index = 2, desc = "Number of sessions currently active")
  public int getSessionsActive() {
    return contextDeployer.getSessionsActive();
  }

  @Counter(index = 3, desc = "Number of handled requests")
  public int getRequests() {
    return statisticsHandler.getRequests();
  }

  @Gauge(index = 4, desc = "Number of requests currently active")
  public int getRequestsActive(){
    return statisticsHandler.getRequestsActive();
  }

  @Gauge(index = 5, desc = "Maximum number of active requests")
  public int getRequestsActiveMax(){
    return statisticsHandler.getRequestsActiveMax();
  }

  @Gauge(index = 6, desc = "Average duration of request handling in milliseconds")
  public int getRequestsDurationAve(){
    return (int) statisticsHandler.getRequestTimeMean();
  }

  @Gauge(index = 7, desc = "Maximum duration of request handling in milliseconds")
  public int getRequestsDurationMax(){
    return (int) statisticsHandler.getRequestTimeMax();
  }

  @Counter(index = 8, desc = "Number of dispatches, excluding active dispatches")
  public int getDispatched() {
    return statisticsHandler.getDispatched();
  }

  @Gauge(index = 9, desc = "Current number of dispatches, including resumed requests")
  public int getDispatchedActive() {
    if (statisticsHandler != null) return statisticsHandler.getDispatchedActive();
    return 0;
  }

  @Counter(index = 10, desc = "Number of suspended requests")
  public int getSuspends() {
    return statisticsHandler.getAsyncRequests();
  }

  @Gauge(index =11, desc = "Number of requests currently suspended")
  public int getSuspendsActive() {
    return statisticsHandler.getAsyncRequestsWaiting();
  }

  @Counter(index =12, desc = "Number of requests that have been resumed")
  public int getResumes() {
    return statisticsHandler.getAsyncDispatches();
  }

  @Counter(index =13, desc = "Number of requests that expired while suspended")
  public int getExpires() {
    return statisticsHandler.getExpires();
  }

  @Counter(index =14, desc = "Number of responses with a 1xx status returned")
  public int getResponses1xx() {
    if (statisticsHandler != null) return statisticsHandler.getResponses1xx();
    return 0;
  }

  @Counter(index =15, desc = "Number of responses with a 2xx status returned")
  public int getResponses2xx() {
    return statisticsHandler.getResponses2xx();
  }

  @Counter(index =16, desc = "Number of responses with a 3xx status returned")
  public int getResponses3xx() {
    return statisticsHandler.getResponses3xx();
  }

  @Counter(index =17, desc = "Number of responses with a 4xx status returned")
  public int getResponses4xx() {
    if (statisticsHandler != null) return statisticsHandler.getResponses4xx();
    return 0;
  }

  @Counter(index =18, desc = "Number of responses with a 5xx status returned")
  public int getResponses5xx() {
    return statisticsHandler.getResponses5xx();
  }

}
