package com.nextenso.agent;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.alcatel.as.service.appmbeans.ApplicationMBeanFactory;
import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.Scheduler;
import com.alcatel.as.service.coordinator.Callback;
import com.alcatel.as.service.coordinator.Coordination;
import com.alcatel.as.service.coordinator.Coordinator;
import com.alcatel.as.service.coordinator.Participant;
import com.alcatel.as.service.discovery.Advertisement;
import com.alcatel.as.service.ioh.IohEndpoint;
import com.alcatel.as.service.management.RuntimeStatistics;
import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeteringConstants;
import com.alcatel.as.service.metering2.Monitorable;
import com.alcatel.as.service.recorder.Record;
import com.alcatel.as.service.recorder.RecorderService;
import com.alcatel.as.service.reporter.api.CommandScopes;
import com.alcatel.as.service.reporter.api.ReporterSession;
import com.alcatel.as.service.shutdown.ShutdownService;
import com.alcatel.as.session.distributed.SessionManager;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel.as.util.config.ConfigHelper;
import com.alcatel.as.util.osgi.DictionaryToMap;
import com.alcatel.as.util.serviceloader.ServiceLoader;
import com.alcatel_lucent.as.management.annotation.command.Command;
import com.alcatel_lucent.as.management.annotation.command.Commands;
import com.alcatel_lucent.as.management.annotation.stat.Counter;
import com.alcatel_lucent.as.management.annotation.stat.Gauge;
import com.alcatel_lucent.as.management.annotation.stat.Stat;
import com.nextenso.agent.event.AsynchronousEventScheduler;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxContext;
import com.nextenso.mux.MuxFactory;
import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.util.DNSManager;
import com.nextenso.mux.util.MuxConnectionManager;
import com.nextenso.mux.util.MuxIdentification;
import com.nextenso.proxylet.dns.DNSClientFactory;
import com.nextenso.proxylet.engine.ProxyletUtils;
import com.nextenso.proxylet.mgmt.impl.ProxyletMBeansManager;

import alcatel.tess.hometop.gateways.concurrent.ThreadPool;
import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.utils.Config;
import alcatel.tess.hometop.gateways.utils.IntHashtable;
import alcatel.tess.hometop.gateways.utils.JndiConfig;
import alcatel.tess.hometop.gateways.utils.Log;
import alcatel.tess.hometop.gateways.utils.Utils;

/**
 * This component is in charge of connecting mux handlers to their io handlers.
 * Exposes statistics/commands to webadmin.
 * 
 * This is a concurrent DependencyManager component. All lifecycle and all dependency callbacks are scheduled in a dedicated PlatformExecutor 
 * Queue which is created for the CalloutServer component .
 */
@SuppressWarnings("deprecation")
@Stat(rootSnmpName = "alcatel.srd.a5350.CalloutAgent", rootOid = { 637, 71, 6, 110 })
@Commands(rootSnmpName = "alcatel.srd.a5350.CalloutAgent", rootOid = { 637, 71, 6, 110 })
public class CalloutServer implements MuxFactory.ConnectionListener, AgentProperties, Participant {
    /**
     * Our general callout agent Logger.
     */
    private static final Log _log = Log.getLogger("callout");
    
    /**
     * optional protocol name found from adverts. If present, we'll only connect mux handler if its protocol is matching the ones specified in the given advert protocol.name
     * (comma separated list).
     */
    private final static String ADVERT_PROTOCOL_NAME = "protocol.name";

	/**
     * Wait for the ServiceLoader, in case some proxylets need to use it. Waiting for it ensures that it will be started before pxlet can use it.
     */
    @SuppressWarnings("unused")
	private ServiceLoader _serviceLoader;

    /**
     * Shutdown hook used to dump all threads when jvm exits unexpectedly
     */
    private ThreadDumpOnExit _shutdownHook;

   /**
     * Used to register our counter/command objects. Injected by DM.
     */
    private BundleContext _bctx;

    /**
     * Legacy global configuration. Injected by DM.
     */
    private Config _config;

    /**
     * Platform Executor. Injected by DM.
     */
    private PlatformExecutors _pfExecs;

    /**
     * Service used to retrieve Runtime statistics (CPU/IO, etc ...). Injected by DM.
     */
    private RuntimeStatistics _mgmtRtService;

    /**
     * Shutdown Service use to gracefully shutdown the JVM. Injected by DM.
     */
    private ShutdownService _shutdown;

    /**
     * The default platform thread pool, which is yet exposed through the MuxContext API. Injected by DM.
     */
    private ThreadPool _threadPool;
    
    /**
     * Application MBean Factory. Injected by DM.
     */
    private ApplicationMBeanFactory _appMBeanFactory;

    /**
     * Event Admin OSGi service, used to notify about a stale advertisement. Injected by DM.
     */
    private EventAdmin _eventAdmin;
    
    /**
     * Reactor Provider service. Injected by DM.
     */
    private ReactorProvider _reactorProvider;

    /**
     * Service used to report our status. Injected by DM.
     */
    private ReporterSession _reporterSession;
    
    /**
     * Service used to record exceptional events.
     */
    private volatile RecorderService _logRecorder;
        
    /**
     * Mux connections container. 
     */
    private final MuxConnectionManager _connectionManager = new MuxConnectionManager();

    /**
     * List of injected mux factories. Key = "type" service property of each mux factory.
     */
    private final Map<String, MuxFactory> _muxFactories = new HashMap<>();

    /**
     * Map holding all MuxHandlerEnv instances. We use a concurrent hashmap in order to be able to
     * remove some entries while being iterating over the map.
     */
    private final Map<MuxHandler, MuxHandlerEnv> _muhHandlerEnvs = new ConcurrentHashMap<MuxHandler, MuxHandlerEnv>();
    
    /**
     * Map holding all MuxWire osgi services. We register in the osgi registry a MuxWire for each started mux connection.
     * (a MuxWire simply allows to expose the MuxConnection to other using a simpler interface).
     */
    private final Map<MuxConnection, ServiceRegistration<IohEndpoint>> _iohEndpoints = new ConcurrentHashMap<>();

    /**
     * Map that holds all known advertisements. 
     */
    private final Map<Advertisement, Map<String, String>> _adverts = new ConcurrentHashMap<>();

    /**
     * Map between MuxConnections and their corresponding advertisements.
     */
    private final Map<MuxConnection, Advertisement> _advertByCnx = new HashMap<MuxConnection, Advertisement>();

    /**
     * Class for protocol reactor/executor management.
     */
    private ReactorsManager _reactorsManager;

    /**
     * Payload sent when connecting to stack.
     */
    private volatile MuxIdentityPayload _muxIdentPayload;

    /**
     * System configuration.
     */
    private Dictionary<String, String> _systemConfig;

    /**
     * Super Agent configuration.
     */
    private Dictionary<String, String> _agentConfig;

    /**
     * Enabled protocols.
     */
    private final Set<String> _enabledProtocols = new HashSet<String>();

    /**
     * Task to call when we know we have fully initialized.
     */
    private Callback _onActive;

    /**
     * Mux Handler Descriptors container.
     */
    private final MuxHandlerDescriptors _descriptors = new MuxHandlerDescriptors();

    /**
     * Our current component queue.
     */
    private volatile PlatformExecutor _queue;
    
    /**
     * Number of expected mux handlers
     */
    private int _expectedMuxHandlers;
    
    /**
     * Map of connection attempts. key = stack instance, value = connection attempt count.
     * When connected, the key is removed, when connection fails, the value is incremented.
     * This is used to avoid logging every three seconds that we can't connect to a stack which
     * is advertized but not connectable. (we try to reconnect every three seconds).
     */
    private final Map<String, Integer> _connectionAttempts = new HashMap<>();
    
    /**
     * System Monitoring service (used to report used memory). Injected by DM.
     */
    private volatile Monitorable _systemMonitoring; 
    
    /**
     * total memory we got during previous call to our getTotalMemory() method.
     */
	volatile int _previousTotalMemory;
	
	/**
	 * free memory we got during previous call to our getFreeMemory() method.
	 */
	volatile int _previousFreeMemory;

	/**
	 * Log Recorder used to log mux events
	 */
	private Record _muxRecord;

    /**
     * Flag used to check if a mux connection is stale or not.
     */
    private final static boolean _checkStaleMux = Boolean.getBoolean("callout.check.stalemux");
    
    /**
     * Mux connection close root causes
     */
    private enum CloseReason {
      StaleConnection,
    }

    /**
     * Displays this callout server name.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (_systemConfig != null) {
            sb.append(ConfigHelper.getString(_systemConfig, ConfigConstants.GROUP_NAME));
            sb.append("/");
            sb.append(ConfigHelper.getString(_systemConfig, ConfigConstants.INSTANCE_NAME));
            sb.append("/");
            sb.append(ConfigHelper.getString(_systemConfig, ConfigConstants.INSTANCE_ID));
        }
        return sb.toString();
    }
  
    /**
     * Either the agent or the system configuration. Use "service.pid" property to detect 
     * the kind of configuration (either "agent", or "system" pid).
     */
    void updated(Dictionary<String, String> config) {
        String pid = config.get("service.pid");
        if ("system".equals(pid)) {
            _systemConfig = config;
        } else if ("agent".equals(pid)) {
            _agentConfig = config;
        }
    }

    /**
     * DM init callback. All required dependencies defined in the Activator are available. 
     * We now add some extra dependencies.
     * Thread safe. Current thread = PlatformExecutor queue created specifically for this component.
     */
    void init(Component component) {
        // Prepare list of extra dependencies we'll add in this method.
        DependencyManager dm = component.getDependencyManager();
        List<Dependency> dependencies = new ArrayList<>();

        // Load mux handler descriptors from class path and from bundles.
        _descriptors.scanBundles(_bctx);
        
        // Add dependencies on all hidden mux handlers which provides a .desc file in their META-INF bundle.
        // the desc infos must not be provided in the mux handler service properties
        for (Map.Entry<String, MuxHandlerDesc> e : _descriptors.getMuxHandlerDescriptors().entrySet()) {
            String protocol = e.getKey();
            MuxHandlerDesc desc = e.getValue();
            
            if (desc.isHidden()) {
                _log.debug("Add dependency on mux handler with protocol=%s", protocol);
                String filter = String.format("(&(%s~=%s)(!(%s=true)))", MuxHandlerDesc.PROTOCOL, protocol, MuxHandlerDesc.HIDDEN);                 		
                Dependency dep = dm.createServiceDependency().setService(MuxHandler.class, filter).setRequired(false).setCallbacks("bind", "unbind");
                dependencies.add(dep);
            }
        }
        
        // Add dependency on hidden MuxHandlers which provides desc infos as part of the service properties.
        _log.debug("Add dependency on all hidden mux handlers which provide their descriptors dynamically");
        String filter = String.format("(&(%s=*)(%s=true))", MuxHandlerDesc.PROTOCOL, MuxHandlerDesc.HIDDEN);         		        		
        Dependency dep = dm.createServiceDependency().setService(MuxHandler.class, filter).setRequired(false).setCallbacks("bind", "unbind");
        dependencies.add(dep);
        
        // If running in HA mode, depends on SessionManager to get our ring id.
        if (ConfigHelper.getBoolean(_agentConfig, AGENT_HA, false)) {
            _log.debug("Add dependency on session manager");
            dep = dm.createServiceDependency().setService(SessionManager.class).setRequired(true).setCallbacks("bind", null);
            dependencies.add(dep);
        }

        // Now, depends on MuxHandlers only for enabled protocols. We define optional dependencies
        // because we need DistributedSession service to be injected at the time the bind(MuxHandler) 
        // method is invoked. 
        String[] enabledProtocols = getEnabledProtocols();
        if (enabledProtocols.length > 0) {
            for (String protocol : enabledProtocols) {
                _log.debug("Add dependency on mux handler with protocol=%s", protocol);
                filter = String.format("(&(%s~=%s)(!(%s=true)))", MuxHandlerDesc.PROTOCOL, protocol, MuxHandlerDesc.HIDDEN);
                dep = dm.createServiceDependency().setService(MuxHandler.class, filter).setRequired(false).setCallbacks("bind", "unbind");
                dependencies.add(dep);
            }
        }

        // Now, atomically add our extra dependencies.
        component.add(dependencies.toArray(new Dependency[dependencies.size()]));
    }

    /**
     * Injected by DM, if we have defined a dependency from init() method.
     * Thread safe. Current thread = PlatformExecutor queue created specifically for this component.
     */
    void bind(SessionManager mngr, Map<String, Object> properties) {
        MuxIdentityPayload newPayload = new MuxIdentityPayload((String) properties.get("as.service.ds.ring.id"),
            (Integer) properties.get("as.service.ds.view.id"), (byte[]) properties.get("as.service.ds.view.payload"));
        _log.debug("update SessionManager %s %s %s", newPayload.sessionMngrRingId, newPayload.viewId,
            newPayload.viewPayload);
        _muxIdentPayload = newPayload;
    }

    /**
     * DM start callback. All required dependencies injected, initialize the container.
     * Notice that all other optional dependencies (bindMuxHandler, etc ...) will be called after the start() method.
     * This significantly eases the management of mux handlers: we'll be injected with them after we are initialized.
     * (with DS, things are more complex: we would have to maintain some temporary lists and handle them after our @Activate methods ...)
     * Thread safe. Current thread = PlatformExecutor queue created specifically for this component.
     */
    void start() {
        String groupInstName = groupInstName(new DictionaryToMap<String, String>(_systemConfig));
        _log.warn("Starting agent %s", this);
        _shutdownHook = new ThreadDumpOnExit();
        Runtime.getRuntime().addShutdownHook(_shutdownHook);
      
        // Store our current component queue.
        _queue = _pfExecs.getCurrentThreadContext().getCurrentExecutor();
        
        // Initialize ProxyeltMBeansMananager
        ProxyletMBeansManager.getInstance().setProxyletMBeansManager(_appMBeanFactory);
                
        if (ConfigHelper.getBoolean(_agentConfig, JNDI_CONFIG, true)) {
        	// Normally, Jndi context service should be provided by the configuration service.
        	// But we have some issues, so for now InitialContext is provided from callout server.        	
        	try {
        		// Map legacy config to Jndi.
        		JndiConfig.setConfig(_config);
        		_bctx.registerService(InitialContext.class.getName(), new InitialContext(), null);
        	} catch (NamingException t) {
        		_log.warn("could not setup JNDI", t);
        	}
        }

        // Sets the bundle context to this class, which needs it.
        MuxMonitorImpl.setBundleContext(_bctx);

        ClassLoader currCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            // Initialize the legacy logger (tracer)
            MuxHandlerLogger.init();

            // Thread Pool have already been initialized from our updated method.

            // Initialize the AgentConstants and the MuxContextImpl
            AgentConstants.AGENT_HOSTNAME = ConfigHelper.getString(_systemConfig, ConfigConstants.HOST_NAME);
            AgentConstants.AGENT_PID = Integer.parseInt(
                ConfigHelper.getString(_systemConfig, ConfigConstants.INSTANCE_PID));
            AgentConstants.AGENT_APP_NAME = ConfigHelper.getString(_systemConfig, ConfigConstants.COMPONENT_NAME);
            AgentConstants.AGENT_APP_ID = Long.decode(
                ConfigHelper.getString(_systemConfig, ConfigConstants.COMPONENT_ID)).intValue();//FIXME blueprint
            AgentConstants.AGENT_INSTANCE = groupInstName;
            AgentConstants.AGENT_GROUP = ConfigHelper.getString(_systemConfig, ConfigConstants.GROUP_NAME);
            AgentConstants.AGENT_UID = ConfigHelper.getLong(_systemConfig, ConfigConstants.INSTANCE_ID);
            AgentConstants.PLATFORM_UID = ConfigHelper.getString(_systemConfig, ConfigConstants.PLATFORM_ID);
            AgentConstants.GROUP_UID = ConfigHelper.getLong(_systemConfig, ConfigConstants.GROUP_ID);

            _log.debug("Initializing CalloutAgent [%s]", groupInstName);

            // MUX Context initialization.
            MuxContextImpl.init(_threadPool);

            // MUX asynchronous scheduler
            AsynchronousEventScheduler.init(_agentConfig, _pfExecs);

            // Initialize DNS
            DNSManager.setMuxConnectionManager(_connectionManager);
            System.setProperty(DNSClientFactory.DNS_CLIENT_CLASS, "com.nextenso.agent.DNSClient");

            // Init enabled protocols
            AgentConstants.PROTOCOLS = new String[getEnabledProtocols().length];
            int index = 0;
            for (String protocol : getEnabledProtocols()) {
                _enabledProtocols.add(protocol.toLowerCase());
                if (protocol.length() > 0) {
                    AgentConstants.PROTOCOLS[index] = String.valueOf(Character.toUpperCase(protocol.charAt(0)));
                    if (protocol.length() > 1) {
                        AgentConstants.PROTOCOLS[index] = AgentConstants.PROTOCOLS[index]
                            + protocol.substring(1).toLowerCase();
                    }
                    index++;
                }
                
                // Update expected number of mux handlers for that protocol.
                int elasticity = getElasticity(_agentConfig);
                MuxHandlerDesc desc = _descriptors.getFromProtocol(protocol);
                int expected = 0;
                if (desc != null && desc.isElastic()) {
                    expected = elasticity;
                } else {
                    expected = 1;
                }
                _expectedMuxHandlers += expected;
                _log.info("Will expect %d mux handlers for protocol %s", expected, protocol);
            }
        } catch (Throwable t) {
            handleError("initialization error", t);
        } finally {
            Thread.currentThread().setContextClassLoader(currCL);
        }

        // Register superagent commands handler
        _log.debug("Registering superagent counters, and commands handler.");
        Hashtable<String, Object> props = new Hashtable<>();
        props.put(CommandScopes.COMMAND_SCOPE, CommandScopes.APP_COUNTER_SCOPE);
        _bctx.registerService(Object.class.getName(), this, props);

        props = new Hashtable<>();
        props.put(CommandScopes.COMMAND_SCOPE, CommandScopes.APP_COMMAND_SCOPE);
        _bctx.registerService(Object.class.getName(), this, props);

        // If no mux handlers are expected, we can enter into the standby state now.
        if (_expectedMuxHandlers == 0) {
          allMuxHandlersInitialized();
        }        
        
        _log.info("Callout Server %s initialized, registering mux handler executors.", this);

        // Register mux handler reactors. This might will trigger activation of other mux handlers if 
        // they depend on their platform executors.
        _reactorsManager = new ReactorsManager(_agentConfig, _descriptors, _bctx, _pfExecs, _reactorProvider);
        _reactorsManager.registerExecutors();
        
        // Create a log recorder for mux connection events
        _muxRecord = _logRecorder.newRecord("agent.mux", null, false);
    }

    /**
     * DM calls this method when the bundle is stopped.
     * Thread safe. Current thread = PlatformExecutor queue created specifically for this component.
     */
    void stop() {
        // Service Registered in our start() method are automatically unregistered.
        // Nothing to do here. Our optional dependencies have already been unbound (including mux handlers). 
    	_log.warn("Stopping Agent component: %s", this);
    }
    
    /**
     * DM injects this optional dependency when the coordinator service has been injected with all expected
     * coordination participants (including ourself, that's why the dependency must be optional).
     * So, here we'll start an "ACTIVATION" coordination. And when done, we'll update our status as "fully active".
     */
    void bind(Coordinator coordinator) {
        _log.debug("Starting \"ACTIVATION\" coordination");
        Coordination coordination = coordinator.newCoordination("ACTIVATION", null);
        coordinator.begin(coordination, new Callback() {
            public void joined(Throwable error) {
                if (error != null) {
                    _log.warn("\"ACTIVATION\" coordination failed", error);
                    _reporterSession.setMessage("Activation failed: " + error.toString());
                    _reporterSession.setState(ReporterSession.FAILED);
                } else {
                    _log.warn("Agent %s initialized.", CalloutServer.this);
                    _reporterSession.setMessage("Active");
                    _reporterSession.setState(ReporterSession.ACTIVE);
                }
            }
        }, null);
    }

    /**
     * An "ACTIVATION" coordination asks us if all muxhandlers are initialized.
     * We'll call onActive once all mux handlers have been initialized.
     */
    @Override
    public void join(Coordination activation, final Callback onActive) {
        _queue.execute(new Runnable() {
            public void run() {
                _onActive = onActive;
                if (_expectedMuxHandlers <= 0) {
                    _onActive.joined(null);
                }
            }
        }, ExecutorPolicy.INLINE);
    }
        
    /**
     * Handle a Mux Factory service.
     */
    void bind(MuxFactory muxFactory, final Map<String, String> props) {
        String type = props.get("type");
        if (type == null) {
            _log.warn("Ignored invalid mux factory with missing required \"type\" service property: factory=%s, properties=%s", muxFactory, props);
            return;
        }
        _log.info("Bound mux factory (type=%s) %s; properties=%s", type, muxFactory, props);
        _muxFactories.put(type, muxFactory);
        // Try to connect pending mux handlers
        tryConnectMuxHandlers();
    }

    /**
     * Handles an advertisement.
     */
    void bind(Advertisement advert, final Map<String, String> props) {
        bind(advert, props, true);
    }

    /**
     * Unbinds an advertisement.
     */
    void unbind(Advertisement advert, final Map<String, String> props) {
        bind(advert, props, false);
    }

    /**
     * Handles a mux handler that is registering into the OSGi service registry.
     */
    void bind(MuxHandler muxHandler, Dictionary<String, Object> properties) {
        final String protocol = (String) properties.get(MuxHandlerDesc.PROTOCOL);
        String scope = null; // FIXME
        
        if (protocol == null) {
            _log.info("Ignoring mux handler %s without protocol: %s", muxHandler, muxHandler);
            return;
        }
        
        MuxHandlerDesc desc = _descriptors.getFromServiceProperties(protocol, properties);
        
        if (desc == null) {
            _log.debug("Ignoring MuxHandler for unknown protocol %s (no mux handler desc found).", protocol);
            return;
        }

        if (!_enabledProtocols.contains(protocol.toLowerCase()) && ! desc.isHidden()) {
            _log.debug("Ignoring MuxHandler for disabled protocol %s", protocol);
            return;
        }
        
        if (scope != null) {
            _log.info("Bound mux handler=%s with protocol=%s, scope=%s.", muxHandler, protocol, scope);
        } else {
            _log.info("Bound mux handler=%s with protocol=%s.", muxHandler, protocol);
        }            

        // Register Mux Handler.
        Reactor protocolReactor = _reactorsManager.getProtocolReactor(protocol);
        String instanceName = Utils.removeSpaces(AgentConstants.AGENT_INSTANCE + "-" + protocol);
        // Descriptor already parsed in start() when we looped over all descriptors
        boolean invokeInit = true;
        MuxContext muxCtx = new MuxContextImpl();
        initMuxHandler(muxHandler, desc, invokeInit, instanceName, muxCtx, protocolReactor, scope);
    }

    /**
     * Handles a mux handler that is unregistering from the OSGi service registry.
     * Called by DM, after start() method because the dependency is optional, and has a callback.
     */
    void unbind(MuxHandler muxHandler, Map<String, Object> properties) {
        final String protocol = (String) properties.get(MuxHandlerDesc.PROTOCOL);
        _log.info("Unbinding mux handler (protocol=%s)", protocol);
        if (protocol == null) {
            _log.info("ignoring mux handler without a protocol service property.");
            return;
        }

        if (!_enabledProtocols.contains(protocol.toLowerCase())) {
            _log.info("ignoring mux handler that is not enabled by configuration.");
            return;
        }
        removeMuxHandler(muxHandler);
    }

    /**
     * Used to load legacy mux handler (non osgi). Called by DM, after start() method 
     */
    void bind(ClassLoader legacyCL) {
        loadLegacyMuxHandlers(legacyCL);
    }

    // ------------------------- Counters ----------------------

    @Gauge(index = 0, snmpName = "JvmMemSize", oid = 100, desc = "JVM Total Memory Size (Kb)")
    public int getTotalMemory() {
    	// Get max memory from metering, if available (should be !)
    	Meter maxMem = _systemMonitoring.getMeters().get(MeteringConstants.SYSTEM_MEM_MAX);
    	if (maxMem != null) {
    		double maxMemory = maxMem.getValue();
    		if (maxMemory > 0) { // -1 means the metering can't provide the value
    			return (_previousTotalMemory = (int) (maxMemory / 1024f));
    		}
    	}
    	
        return _previousTotalMemory;
    }

    @Gauge(index = 1, snmpName = "NumStacks", oid = 101, desc = "Connected Stacks")
    public int getNbConnections() {
        return _connectionManager.size();
    }

    @Gauge(index = 2, snmpName = "JvmFreeMem", oid = 102, desc = "JVM Free Memory Available (Kb)")
    public int getFreeMemory() {
    	// Get free memory from metering, if available (should be !).
    	// We use the percentage of used memory to get the actual available free memory.

    	Meter percentageUsed = _systemMonitoring.getMeters().get(MeteringConstants.SYSTEM_MEM_COLLECTION_USAGE);
    	Meter maxMem = _systemMonitoring.getMeters().get(MeteringConstants.SYSTEM_MEM_MAX);

    	if (percentageUsed != null && maxMem != null) {
    		double maxMemory = maxMem.getValue();
    		double percentageUsedMem = percentageUsed.getValue();
    		if (maxMemory > 0 && percentageUsedMem > 0) { // -1 means the metering can't provide the value
    			maxMemory /= 1024f;
    			return (_previousFreeMemory = (int) (maxMemory - ((maxMemory * percentageUsedMem) / 100f)));
    		}
    	}    	
    	
        return _previousFreeMemory;
    }

    @Gauge(index = 3, snmpName = "FlowCtrl", oid = 103, desc = "Flow Control")
    public int getConcurrentGC() {
        return _mgmtRtService.hasConcurrentGc() ? 1 : 0;
    }

    @Counter(index = 4, snmpName = "JvmNormalMemStateSec", oid = 104, desc = "JVM normalMemory state (sec)")
    public int getNormalMemoryLevelSec() {
        return _mgmtRtService.getNormalMemoryLevelSec();
    }

    @Counter(index = 5, snmpName = "JvmHighMemStateSec", oid = 105, desc = "JVM highMemory state (sec)")
    public int gethighMemoryLevelSec() {
        return _mgmtRtService.getHighMemoryLevelSec();
    }

    @Counter(index = 6, snmpName = "JvmEmergencyMemStateSec", oid = 106, desc = "JVM emergencyMemory state (sec)")
    public int getemergencyMemoryLevelSec() {
        return _mgmtRtService.getEmergencyMemoryLevelSec();
    }

    @Gauge(index = 7, snmpName = "JvmActualMemoryLevelState", oid = 107, desc = "JVM Actual Memory Level State (int) 0:Normal 1:High 2:Emergency")
    public int getmemoryLevel() {
        return _mgmtRtService.getMemoryLevel();
    }

    @Gauge(index = 8, desc = "Percentage Of Used Memory After Last GC")
    public int getUsedMemory() {
    	Meter usedMemoryAfterLastCollection = _systemMonitoring.getMeters().get(MeteringConstants.SYSTEM_MEM_COLLECTION_USAGE);
    	return usedMemoryAfterLastCollection != null ? (int) usedMemoryAfterLastCollection.getValue() : 0;
    }

    // ------------------------- Commands ----------------------

    // No annotation used for APP_SHUTDOWN scope ...
    public void exitRequest() {
        exit(false, "Exiting on management request", null);
    }

    @Command(code = 1, desc = "Execute a full garbage collection")
    public void fullGC() {
        _log.warn("Doing full garbage collection");
        System.gc();
    }

    @Command(code = 2, desc = "Dump a complete dump stack of all agent threads")
    public void dumpState() {
    	if (ConfigHelper.getBoolean(_agentConfig, DUMP_STACKTRACE, true)) {
			_log.warn("Dumping thread stacktraces");
			StringWriter sw = new StringWriter();
			sw.write("Agent: Dumping state\n");
			try {
				Map<Thread, StackTraceElement[]> mapStacks = Thread.getAllStackTraces();
				Iterator<Thread> threads = mapStacks.keySet().iterator();
				while (threads.hasNext()) {
					Thread thread = threads.next();
					StackTraceElement[] stes = mapStacks.get(thread);
					sw.write("\nThread [" + thread.getName() + " prio=" + thread.getPriority()
							+ "] --> StackTrace elements ...\n");
					for (StackTraceElement ste : stes) {
						sw.write("\t" + ste.toString() + "\n");
					}
				}

				_log.warn(sw.toString());
			} catch (Throwable t) {
				_log.error("Agent: Exception while dumping state", t);
			}
    	}
    }

    // ----------------------- MuxFactory.ConnectionListener interface ---------------------------

    private String getStackKey(MuxConnection cnx) {
    	return MuxHandlerEnv.getStackKey(cnx.getStackInstance(), cnx.getStackAddress(), cnx.getStackPort());
    }
    
    /**
     * A mux handler is connected to its IOH. Called from a mux handler reactor thread, or from a mux handler queue.
     * we must send mux ident, just before muxHandler.muxConnected is called ...
     */
    public void muxConnected(final MuxConnection connection, Throwable error) {
        if (error == null) {   
			_connectionAttempts.remove(getStackKey(connection));
			_log.warn("Connected agent %s with mux connection %s", this, connection);
			_muxRecord.record(new com.alcatel.as.service.recorder.Event("mux connected : " + connection.getStackAddress() + ":" + connection.getStackPort()));
			_queue.execute(new Runnable() {
				public void run() {
					_connectionManager.addMuxConnection(connection);
				}
			});

			// The following code must not be scheduled in our serial queue, because we must
			// send the payload before the mux handler is called
			// in its muxOpened callback, and we are currently running from the mux handler
			// reactor thread (or queue).

			MuxIdentification muxId = new MuxIdentification().load(_systemConfig).setDefaultKeepAlive();
			if (_muxIdentPayload != null) {
				long ringId = Long.parseLong(_muxIdentPayload.sessionMngrRingId);
				muxId.setRingID(ringId);
			}

			MuxHandlerEnv env = _muhHandlerEnvs.get(connection.getMuxHandler());
			if (env != null) {
				_log.info("Mux handler %s connected to %s with connection: %s", env.getMuxHandler(),
						connection.getStackInstance(), connection);

				int muxHandlerIndex = env.getScopeIndex();
				muxId.setContainerIndex(muxHandlerIndex);
				
				// append the protocol in the application name (the ioh needs this info)
				String appName = muxId.getAppName();
				StringBuilder sb = new StringBuilder(appName).append(";").append("agent.protocol=").append(env.getProtocol().toLowerCase());				
				String appParams = ConfigHelper.getString(_systemConfig, "application.parameters", null);
				if (appParams != null) {
					sb.append(";").append(appParams);
				}								
				muxId.setAppName(sb.toString());
				
				_log.info("Sending Mux Identification: %s", muxId);
				connection.sendMuxIdentification(muxId);
				registerIohEndpoint(connection, env);
			}
        } else {
			// connection failure.
			_queue.execute(() -> {
				Integer attempt = _connectionAttempts.get(getStackKey(connection));
				String info = String.format("could not connect agent %s to %s", CalloutServer.this,
						connection.getRemoteAddress());
				Throwable err = error;
				if (err instanceof IOException) {
					info += ": " + error.toString();
					err = null;
				}
				if (attempt == null) {
					// log the connect failure only one time.
					_log.warn("%s", err, info);
					_connectionAttempts.put(getStackKey(connection), 1);
				} else {
					_connectionAttempts.put(getStackKey(connection), attempt + 1);
					_log.info("%s", err, info);
				}
				removeMuxConnection(connection, true /* notify advert impl that the advert is unreachable */);
				scheduleReconnect(connection.getMuxHandler());
			});
        }
    }

    public void muxAccepted(MuxConnection cnx, Throwable error) {
        // not possible
    }
    
    // someone called cnx.sendMuxStart (from any thread)
    public boolean muxStarted(MuxConnection cnx) {
    	return activateIohEndpoint(cnx, true);
    }

    // someone called cnx.sendMuxStart (from any thread)
    public boolean muxStopped(MuxConnection cnx) {
    	return activateIohEndpoint(cnx, false);
    }
    
    public void muxClosed(final MuxConnection cnx) {
	_muxRecord.record(new com.alcatel.as.service.recorder.Event("mux closed : " + cnx.getStackAddress() + ":" + cnx.getStackPort()));
	unregisterIohEndpoint(cnx);
    	_queue.execute(() -> {
    		removeMuxConnection(cnx, true /* notify advert impl that this mux cnx is dead */);
    		scheduleReconnect(cnx.getMuxHandler());
	    });
    }

    // called from muxConnected (thread=mux handler queue)
    private void registerIohEndpoint(MuxConnection cnx, MuxHandlerEnv env) {
		ServiceRegistration<IohEndpoint> iohEndPoint = _iohEndpoints.get(cnx);
		if (iohEndPoint == null) {
			Advertisement advert = _advertByCnx.get(cnx);
			if (advert == null) {
				// Unexpected
				_log.error("Could not register IohEndpoint for mux connection %s (advert not found)", cnx);
				return;
			}
			Map<String, String> advertProps = _adverts.get(advert);
			if (advertProps == null) {
				// Unexpected
				_log.error("Could not register IohEndpoint for mux connection %s (advert properties not found)", cnx);
				return;
			}
			Hashtable<String, Object> props = new Hashtable<>();
			props.put(IohEndpoint.PROTOCOL, env.getProtocol().toLowerCase());
			props.put(IohEndpoint.GROUP, advertProps.get(ConfigConstants.GROUP_NAME));
			props.put(IohEndpoint.INSTANCE, advertProps.get(ConfigConstants.INSTANCE_NAME));
			props.put(IohEndpoint.ADDRESS, cnx.getStackAddress());
			props.put(IohEndpoint.PORT, new Integer(cnx.getStackPort()));
			
			IohEndpointImpl impl = new IohEndpointImpl(cnx);
			iohEndPoint = _bctx.registerService(IohEndpoint.class, impl, props);
			IohEndpointImpl._log.info("Registered IohEndpoint service for mux cnx %s protocol=%s, service id=%d", cnx,
					  env.getProtocol().toLowerCase(), iohEndPoint.getReference().getProperty("service.id"));
			_iohEndpoints.put(cnx, iohEndPoint);
		}
    }

    // called from muxStarted or muxStopped (thread=any)
    private boolean activateIohEndpoint(MuxConnection cnx, boolean registered) {
    	ServiceRegistration<IohEndpoint> reg = _iohEndpoints.get(cnx);
    	if (reg != null) {
    		IohEndpointImpl iohEndpoint = (IohEndpointImpl) _bctx.getService(reg.getReference());
    		if (iohEndpoint != null) {
    			return iohEndpoint.activate(registered);
    		}
    	}
    	return true; // unexpected, if no ioh endpoint is found, accept registration
    }
    
    // called from muxClosed (thread=mux handler queue)
    private void unregisterIohEndpoint(MuxConnection cnx) {
    	_log.warn("agent %s lost connection %s", CalloutServer.this, cnx);
    	ServiceRegistration<IohEndpoint> iohEndpoint = _iohEndpoints.remove(cnx);
        if (iohEndpoint != null) {
      	  _log.info("unregistering IohEndpoint service for closed mux cnx %s", cnx);
      	  try {
      		  iohEndpoint.unregister();
      	  } catch (Exception e) {}
        }
    }

    // ------------------------- Package methods -----------------------------------------------
    
    public static int getElasticity(Dictionary<String, String> agentConfig) {
        int elasticity = ConfigHelper.getInt(agentConfig, AgentProperties.AGENT_ELASTICITY, 1);
        if (elasticity == 0) {
            elasticity = Runtime.getRuntime().availableProcessors();
        }
        return elasticity;
    }   
    
    // ------------------------ Private methods ------------------------------------------------

    /**
     * Called from our component queue.
     */
    private void removeMuxConnection(final MuxConnection cnx, boolean notifyStaleAdvert) {
        MuxHandlerEnv env = _muhHandlerEnvs.get(cnx.getMuxHandler());
        if (env != null) {
            env.removeStack(cnx.getStackInstance(), cnx.getStackAddress(), cnx.getStackPort());
        }
        _connectionManager.removeMuxConnection(cnx);
        Advertisement advert = _advertByCnx.remove(cnx);
        if (advert != null && notifyStaleAdvert) {
            Map<String, Object> props = new HashMap<String, Object>();
            props.put(Advertisement.class.getName(), advert);
            _log.debug("Notifying about stale advert %s", advert);
            _eventAdmin.sendEvent(new Event(Advertisement.STALE_ADVERT, props));
        }
    }
    
    /**
     * @return the (possibly empty) list of configured (active) protocols, or null if all protocols are enabled.
     */
    private String[] getEnabledProtocols() {
        String s = ConfigHelper.getString(_agentConfig, AgentProperties.MUX_HANDLERS, "");
        return s.length() == 0 ? new String[0] : s.split(" ");
    }

    /**
     * Called from our component queue.
     */
    private void loadLegacyMuxHandlers(ClassLoader legacyCL) {
        _log.debug("Initializing legacy mux handlers ...");
        for (String protocol : getEnabledProtocols()) {
            MuxHandlerDesc description = _descriptors.getFromProtocol(protocol);

            String className = description.getClassName();
            String instanceName = Utils.removeSpaces(AgentConstants.AGENT_INSTANCE + "-" + description.getProtocol());

            try {
                if (className != null) {
                    Reactor protocolReactor = _reactorsManager.getProtocolReactor(description.getProtocol());
                    MuxContext muxCtx = new MuxContextImpl();

                    // old mode.. instantiate classname found in .desc
                    _log.info("Loading legacy MuxHandler: " + className);
                    Class<?>[] classes = { Integer.TYPE, String.class, String.class, MuxContext.class };
                    Class<?> clazz = null;
                    clazz = legacyCL.loadClass(className);
                    Constructor<?> c = clazz.getConstructor(classes);
                    Object[] arguments = new Object[4];
                    arguments[0] = Integer.valueOf(description.getAppId());
                    arguments[1] = description.getAppName();
                    arguments[2] = instanceName;
                    arguments[3] = muxCtx;
                    MuxHandler muxHandler = (MuxHandler) c.newInstance(arguments);
                    initMuxHandler(muxHandler, description, false, instanceName, muxCtx, protocolReactor, null);
                }
            } catch (Throwable e) {
                _log.error("Unable to load legacy mux handler [%s]", e, description.getClassName());
                System.exit(1);
            }
        }
    }

    private String buildAdvertFilter(MuxHandler mh, MuxHandlerDesc mhd) {
        /* Build initial filter based on eventType */
        int[] appIds = (int[]) mh.getMuxConfiguration().get(MuxHandler.CONF_STACK_ID);

        /* This one is mandatory */
        if (appIds.length == 0) {
            _log.error(
                "Filtering on application Id (MuxHandler.CONF_STACK_ID) is mandatory. Advertising for [%s] will be not activated",
                mhd.getProtocol());
            return null;
        }

        StringBuilder advertFilter = new StringBuilder("(&");
        advertFilter.append("(provider=*)");
        if (appIds.length > 1) {
            advertFilter.append("(|");
        }
        for (int appId : appIds) {
            advertFilter.append("(" + ConfigConstants.MODULE_ID + "=" + appId + ")");
        }
        if (appIds.length > 1) {
            advertFilter.append(")");
        }

        // Don't filter on the group, we'll do it in the addressingInformationReceived method.

        if (_systemConfig.get(ConfigConstants.PLATFORM_NAME) != null)
            advertFilter.append(
                "(" + ConfigConstants.PLATFORM_NAME + "=" + _systemConfig.get(ConfigConstants.PLATFORM_NAME) + ")");

        /* Check filtering on instance ? */
        Object o = mh.getMuxConfiguration().get(MuxHandler.CONF_STACK_INSTANCE);
        if (o instanceof String) {
            advertFilter.append("(" + ConfigConstants.INSTANCE_NAME + "=" + (String) o + ")");
        } else {
            String[] stackInstances = (String[]) o;
            if (stackInstances.length > 0) {
                if (stackInstances.length > 1) {
                    advertFilter.append("(|");
                }
                for (String stackInstance : stackInstances) {
                    advertFilter.append("(" + ConfigConstants.INSTANCE_NAME + "=" + stackInstance + ")");
                }
                if (stackInstances.length > 1) {
                    advertFilter.append(")");
                }
            }
        }
        advertFilter.append(")");

        _log.debug("Looking for an advert in the OSGi service registry using the filter %s for mux handler %s",
            advertFilter, mhd.getProtocol());
        return (advertFilter.toString());
    }

    private void initMuxHandler(final MuxHandler mh, final MuxHandlerDesc mhd, final boolean invokeInit,
        final String instanceName, final MuxContext muxCtx, Reactor protocolReactor, String scope)
    {
        IntHashtable flags = mhd.getFlags();
        final String protocol = mhd.getProtocol();

        // implementation issue, MuxContextImpl needs MuxHandler for monitoring purpose
        ((MuxContextImpl) muxCtx).setMuxHandler(mh);

        try {
            PlatformExecutor muxHandlerQueue = _reactorsManager.getProtocolQueue(protocol, scope);
            final MuxHandlerEnv muxHandlerEnv = new MuxHandlerEnv(_pfExecs, mh, mhd, flags, protocol, protocolReactor, scope, muxHandlerQueue);

            // We must initialize the mux handler in its executor thread, but we then want to proceed in our serial queue.
            // To do so, we use a helper Scheduler class which orchestrate executions using executors .
            
            _log.debug("Scheduling initialization for mux handler %s", mh);

            Scheduler scheduler = _pfExecs.createScheduler();
            Executor exec = muxHandlerEnv.getExecutor();
            scheduler.atFirst(exec, new Scheduler.F() {
                public void f(Scheduler scheduler) {
                    try {
                        _log.debug("Initializing mux handler %s", mh);
                        if (invokeInit) {
                            muxHandlerEnv.init(mhd.getAppId(), mhd.getAppName(), instanceName, muxCtx);
                        }
                        muxHandlerEnv.init(_config); // legacy init always called
                        muxHandlerEnv.initializeMaxStackCount();

                        // Detect if the mux handler is using the new management API. 
                        // If not, register the reporter on behalf of the old mux handler
                        if (muxHandlerEnv.getMuxHandlerDesc().autoReport()) {
                        	_log.debug("Creating reporter on behalf of mux handler %s", mh);
                            MuxMonitorImpl.startReporter(AgentConstants.AGENT_INSTANCE, mh.getAppId(), mh.getAppName(),
                                mh.getInstanceName(), mh);
                        }
                        scheduler.next();
                    } catch (Throwable t) {
                      handleError("Error while binding MuxHandler for protocol " + protocol, t);
                    }
                }
            }).andThen(_queue, new Scheduler.F() {
                public void f(Scheduler scheduler) {
                    try {
                        _log.info("mux handler %s with protocol %s initialized.", mh, protocol);
                        _muhHandlerEnvs.put(mh, muxHandlerEnv);
                        muxHandlerInitialized(muxHandlerEnv.getMuxHandlerDesc());

                        // Eventually schedule external stack connector (for ss7 stack, for instance)
                        registerExternalStackAdverts(muxHandlerEnv);

                        // Try to connect this mux handlers to any available IOH.
                        tryConnectMuxHandler(mh, muxHandlerEnv);                     
                    } catch (Throwable e) {
                      handleError("Error while binding MuxHandler for protocol " + protocol, e);
                    }
                }
            });
            
            // Start the scheduler. The first function is executed in the mux handler reactor (we must init mux handler in its reactor thread),
            // and the second one is rescheduled in our serial queue, where we'll proceed with the mux handler registration. 
            scheduler.next();
        }

        catch (Exception e) {
          handleError("Error while binding MuxHandler for protocol " + protocol, e);
        }
    }

    protected void handleError(String error, Throwable t) {
		if ("true".equals(System.getProperty("system.failstop", "true"))) {
			exit(true, "Unexpected exception", t);
		} else {
			_log.error("Agent %s: Error: %s", t, this, error);
		}
    }
    
    /**
     * One mux handler has been initialized. Thread safe.
     * @param muxHandlerDesc 
     */
    private void muxHandlerInitialized(MuxHandlerDesc muxHandlerDesc) {
    	if (muxHandlerDesc.isHidden()) {
    		return;
    	}
        // Now, we check if all mux handlers are there. If yes, then enter in standby state until the activation coordination is joined.
        if ((--_expectedMuxHandlers) == 0) {
        	allMuxHandlersInitialized();
        }
        else {
            _log.debug("Awaiting for more mux handlers before setting fully active state (await count=%d)",  _expectedMuxHandlers);
        }
    }
    
    private void allMuxHandlersInitialized() {
        // Mark our state as STANDBY until the ACTIVATION coordination is joined.
        _reporterSession.setState(ReporterSession.STANDBY);
        _reporterSession.setMessage("Standby");

        _log.info("Agent is fully initialized (all mux handlers are active). Joining \"ACTIVATION\" coordination");
        if (_onActive != null) {
            _onActive.joined(null); // will enter in ACTIVE state.
        }
    }

    private void scheduleReconnect(final MuxHandler muxHandler) {
        _queue.schedule(new Runnable() {
            public void run() {
                MuxHandlerEnv env = _muhHandlerEnvs.get(muxHandler);
                if (env != null) {
                    tryConnectMuxHandler(muxHandler, env);
                }
            }
        }, 3, TimeUnit.SECONDS);
    }

    private void removeMuxHandler(MuxHandler muxHandler) {
        MuxHandlerEnv env = _muhHandlerEnvs.remove(muxHandler);
        if (env != null) {
            _log.info("Agent %s: destroying mux handlers %s", this, env.getMuxHandler());
            // for now, we disable mux handler destroy call, because when mux handlers are now assumed to use the shutdown service.
            // env.destroy(); // scheduled in mux handler executor
        }
    }

    @SuppressWarnings("unchecked")
    private void mapMuxConnection(String stackInstance, Consumer<MuxConnection> consumer) {
        Enumeration<MuxConnection> openedMuxConnections = _connectionManager.getMuxConnections();
        while (openedMuxConnections.hasMoreElements()) {
            MuxConnection cnx = (MuxConnection) openedMuxConnections.nextElement();
            if (stackInstance == null || cnx.getStackInstance().equals(stackInstance)) {
              consumer.accept(cnx);
            }
        }
    }

    private String groupInstName(Map<String, String> props) {
        if (_systemConfig.get(ConfigConstants.PLATFORM_NAME) != null) //check blueprint mode
        {
            _log.debug("Blueprint mode detected. use new instance name");
            
            // temporary hack/fix: if advert provider is jmdns, strip the module name from the instance name.
            // (for now, I don't see why our jmdns provider appends the component name to the instance name)
            String instanceName = props.get(ConfigConstants.INSTANCE_NAME);
            String moduleName = props.get(ConfigConstants.MODULE_NAME);
            if (moduleName != null) {
            	String provider = props.get("provider");
            	String suffix = "__" + moduleName; 

            	if ("JmDNS".equals(provider) && instanceName.endsWith(suffix)) {
            		_log.debug("stripping component name from the end of jmdns advert instance name:%s", instanceName);
            		int index = instanceName.lastIndexOf(suffix);
            		instanceName = instanceName.substring(0, index);
            	}
            }
            
            return props.get(ConfigConstants.PLATFORM_NAME) + "." + props.get(ConfigConstants.GROUP_NAME) + "__"
                + props.get(ConfigConstants.COMPONENT_NAME) + "." + instanceName;
        } else {
            _log.debug("Legacy mode detected. use old instance name");
            return props.get(ConfigConstants.GROUP_NAME) //legacy mode
                + "__" + props.get(ConfigConstants.INSTANCE_NAME);
        }
    }
    
    private String getAdvertAddress(Advertisement advert, Map<String, String> advertProps) {
    	// if the remote stack listens on all:*, the advert.getIp() method returns "0.0.0.0" !
    	// In this case, we have to convert 0.0.0.0 to the real stack address.
    	String advertAddr = advert.getIp();
    	try {
			InetAddress iaddr = InetAddress.getByName(advertAddr);
			if (iaddr.isAnyLocalAddress()) {
				String stackHost = advertProps.get(ConfigConstants.HOST_NAME);
				if (stackHost == null) {
					_log.warn("Ignoring advert %s with properties %s (advert contains an \"any\" addr without a host.name property).", advert, advertProps);
					return null;
				}
				InetAddress hostInetAddr = (new InetSocketAddress(stackHost, advert.getPort())).getAddress();
				if (hostInetAddr == null) {
					_log.warn("Ignoring advert %s with properties %s (advert contains an \"any\" addr but host name %s is not resolvable).",
							advert, advertProps, stackHost);
					return null;					
				}

				advertAddr = hostInetAddr.getHostAddress();
			} else {
				advertAddr = iaddr.getHostAddress();
			}
    	} catch (UnknownHostException e) {
    		// the advertAddress is not resolvable, just log and return it, we'll fail to connect
    		// but hopefully later, we'll be able to reconnect.
    		_log.warn("advert %s with properties %s is not resolvable.", advert, advertProps);
		}
        return advertAddr;
    }

    private void bind(Advertisement advert, Map<String, String> advertProps, boolean isActive) {
        _log.debug("Bound advert: %s, active:%b, properties: %s", advert, isActive, advertProps);

        if (!acceptAdvert(advert, advertProps)) { // will log if not accepted.
            return;
        }

        String stackAddress = getAdvertAddress(advert, advertProps);
        if (stackAddress == null) { // will log if stackAddress can't be found
        	return;
        }
        
        String stackInstance = groupInstName(advertProps);
        int stackPort = advert.getPort();

        if (!isActive) {
            _log.warn("Unbound advert %s from stack %s (advert props=%s).", advert, stackInstance, advertProps);
            _adverts.remove(advert);
            
            // If we were already connected to the remote stack, we'll be disconnected soon if the advert tells the true.
            return;
        }

        _log.warn("Accepted advert: %s, stack instance: %s", advert, stackInstance);
            

        // Check if we currently have an opened mux connection to the same stack instance, but
        // with wrong addr/port. If so, we'll get rid off our old invalid mux connection.

	if (_checkStaleMux) {
	    mapMuxConnection(stackInstance, cnx -> {
		    _log.debug("Checking stale connection for new peer [%s/%s/%d]; currently opened cnx address is: %s:%d",
			       stackInstance, stackAddress, stackPort, cnx.getStackAddress(), cnx.getStackPort());

		    boolean conflict = ((!cnx.getStackAddress().equals(stackAddress)) || cnx.getStackPort() != stackPort);
		    if (conflict) {
			String reason = String.format(
			    "agent %s detected stale connection for peer [%s/%s:%d] whose address conflicts with current already opened connection: %s/%s:%d",
			    this, stackInstance, stackAddress, stackPort, cnx.getStackInstance(), cnx.getStackAddress(),
			    cnx.getStackPort());
			cnx.shutdown(CloseReason.StaleConnection, reason, null); // we'll be called back in muxClosed.
		    }
		});
	}
        
        // Store this advert.
        _adverts.put(advert, advertProps);

        // Try to see if we can connect some mux handlers to the advert's IOH.
        tryConnectMuxHandlers();
    }

    /**
     * Try to connect all initialized mux handlers to all available corresponding io handlers.
     */
    private void tryConnectMuxHandlers() {
        _log.debug("Checking if some mux handlers can be connected.");
        for (Map.Entry<MuxHandler, MuxHandlerEnv> e : _muhHandlerEnvs.entrySet()) {
            MuxHandler muxHandler = e.getKey();
            MuxHandlerEnv env = e.getValue();
            tryConnectMuxHandler(muxHandler, env);
        }
    }

    /**
     * Try to connect an initialized mux handler to all available corresponding io handlers.
     */
    private void tryConnectMuxHandler(MuxHandler muxHandler, MuxHandlerEnv muxHandlerEnv) {
        _log.debug("Checking if mux handler %s has to be connected.", muxHandler);

        // For each known advert, see if the muxHandler is interested to it and is not yet connected to the associated handler.
        MuxHandlerDesc desc = muxHandlerEnv.getMuxHandlerDesc();

        for (Map.Entry<Advertisement, Map<String, String>> e : _adverts.entrySet()) {
            Advertisement advert = e.getKey();
            Map<String, String> properties = e.getValue();
            String stackInstance = groupInstName(properties);
            Object moduleId = properties.get(ConfigConstants.MODULE_ID);
            int stackId = moduleId instanceof Integer ? (Integer) moduleId : Integer.parseInt(moduleId.toString());
            String stackAddress = getAdvertAddress(advert, e.getValue());
            String stackHost = properties.get(ConfigConstants.HOST_NAME);

            // Check if the advert matches the mux handler

            String filter = buildAdvertFilter(muxHandler, desc);
            try {
                Filter f = _bctx.createFilter(filter);
                if (!f.matches(properties)) {
                    _log.debug("ignoring advert %s with properties %s for mux handler %s (criteria don't match)",
                        advert, properties, muxHandler);
                    continue;
                }
                _log.debug("found advert %s for mux handler %s", advert, muxHandler);
            } catch (InvalidSyntaxException ex) {
                _log.error("invalid mux handler advert filter: " + filter, ex);
                continue;
            }

            // Check if the mux handler is already connected to the ioh represented by this advert.
            // temporary hack: we must do this check from our connectMuxHandlermethod
            // because for local mux connections, the local muxcnx impl is mangling the stack instance name
            // See connectMuxHandler
            /*
            if (muxHandlerEnv.isConnectedTo(stackInstance, stackAddress, advert.getPort())) {
                _log.debug("Ignoring stack %s for mux handler %s (already connected to it)", stackInstance,
                    muxHandlerEnv);
                continue;
            }

            // See if we can add more stacks to this mux handler.

            if (!muxHandlerEnv.mayAddStack()) {
                _log.debug("Ignoring stack %s. Max simultaneous mux connections exceeded", stackInstance);
                continue;
            }
            */
            
            // See if we have a mux factory available for the advert.
            
            MuxFactory muxFactory = getMuxFactory(muxHandlerEnv.getProtocol(), advert, properties);
            if (muxFactory == null) {
                _log.debug("Ignoring stack %s. mux factory not yet available", stackInstance);
                continue;
            }
            
            
            String stackName = properties.get(ConfigConstants.COMPONENT_NAME);
            
            // If the IOH app id is acceptable, connect the mux handler to the IOH.  
            boolean ok;            
            int[] appIds = (int[]) muxHandler.getMuxConfiguration().get(MuxHandler.CONF_STACK_ID);
    		ok = (appIds == null || appIds.length == 0);
            for (int j = 0; j < appIds.length; j++) {
            	if (ok = (appIds[j] == stackId)) {
                    break;
                }
            }
        	if (!ok) {
    			_log.debug("Stack instance %s not accepted by mux handler %s (stack id %d not compatible)", stackInstance, muxHandler.getAppName(), stackId);
        		continue;
    		}
            
    		// check the stackName
    		Object o = muxHandler.getMuxConfiguration ().get (MuxHandler.CONF_STACK_NAME);
    		if (o instanceof String){
    		    ok = (stackName.equalsIgnoreCase ((String)o));
    		} else {
    		    String[] stackNames = (String[]) o;
    		    ok = (stackNames == null || stackNames.length == 0);
    		    for (int j=0; stackNames != null && j<stackNames.length; j++){
    			if (ok = (stackNames[j].equalsIgnoreCase (stackName)))
    			    break;
    		    }
    		}
    		if (!ok) {
    			_log.debug("Stack instance %s not accepted by mux handler %s (stack name %s not compatible)", stackInstance, muxHandler.getAppName(), stackName);
    			continue;
    		}
    		
    		// check the stackInstance
    		o = muxHandler.getMuxConfiguration ().get (MuxHandler.CONF_STACK_INSTANCE);

    		if (o instanceof String){
    		    ok = (stackInstance.equalsIgnoreCase ((String)o));
    		} else {
    		    String[] stackInstances = (String[]) o;
    		    ok = (stackInstances == null || stackInstances.length == 0);
    		    for (int j=0; stackInstances != null && j<stackInstances.length; j++){
    			if (ok = (stackInstances[j].equalsIgnoreCase (stackInstance)))
    			    break;
    		    }
    		}
    		if (!ok) {
    			_log.debug("Stack instance %s not accepted by mux handler %s (stack instance not compatible)", stackInstance, muxHandler.getAppName());
    			continue;
    		}
    		
    		// check the stackHost
    		o = muxHandler.getMuxConfiguration ().get (MuxHandler.CONF_STACK_HOST);
    		if (o instanceof String){
    		    ok = (stackHost.equalsIgnoreCase ((String)o));
    		} else {
    		    String[] stackHosts = (String[]) o;
    		    ok = (stackHosts == null || stackHosts.length == 0);
    		    for (int j=0; stackHosts != null && j<stackHosts.length; j++){
    			if (ok = (stackHosts[j].equalsIgnoreCase (stackHost)))
    			    break;
    		    }
    		}    		
    		if (!ok) {
    			_log.debug("Stack instance %s not accepted by mux handler %s (stack host %s not compatible)", stackInstance, muxHandler.getAppName(), stackHost);
    			continue;
    		}    
    		
    		// check the advert protocol.name
    		String advertProtocolName = properties.get(ADVERT_PROTOCOL_NAME);
    		if (advertProtocolName != null) {
    			String[] protocols = advertProtocolName.split(",");
    			if (protocols != null) {
    				ok = false;
    				for (String p : protocols) {
    					if (p.equals("*") || p.equalsIgnoreCase(muxHandlerEnv.getProtocol())) {
    						ok = true;
    						break;
    					}
    				}
    			}
    		}
    		if (!ok) {
    			_log.debug("Stack instance %s not accepted by mux handler %s (protocol name %s not compatible)", 
    					stackInstance, muxHandler.getAppName(), muxHandlerEnv.getProtocol());
    			continue;
    		}

            connectMuxHandler(muxFactory, muxHandlerEnv, muxHandler, stackHost,
                    stackName, stackId, stackInstance, stackAddress,
                    advert.getPort(), advert, properties);
        }
    }
    
    private void connectMuxHandler(MuxFactory muxFactory, MuxHandlerEnv muxHandlerEnv, MuxHandler muxHandler, String stackHost,
        String stackName, int stackId, final String stackInstance, String stackAddress, int stackPort,
        Advertisement advert, Map<String, String> advertProps)
    {		
        if (muxHandler.accept(stackId, stackName, stackHost, stackInstance)) {
            StringBuilder sb = new StringBuilder();
            sb.append("MuxConnection [stackId=");
            sb.append(stackId);
            sb.append(", stackName=");
            sb.append(stackName);
            sb.append(", stackInstance=");
            sb.append(stackInstance);
            sb.append(", stackHost=");
            sb.append(stackHost);
            sb.append(", stackAddr=");
            sb.append(stackAddress);
            sb.append(", stackPort=");
            sb.append(stackPort);
            sb.append(']');

            // Now, open the mux connection, either using the main agent reactor, or a dedicated  reactor.
            InetSocketAddress inetAddress = new InetSocketAddress(stackAddress, stackPort);

            Map<Object, Object> muxConfig = new HashMap<Object, Object>();
            muxConfig.put(MuxFactory.OPT_LOGGER, MuxHandlerLogger.makeMuxConnectionTracer(stackInstance));
            muxConfig.put(MuxFactory.OPT_FLAGS, muxHandlerEnv.getFlags());
            muxConfig.put(MuxFactory.PROTOCOL, muxHandlerEnv.getProtocol().toLowerCase());

            PlatformExecutor queue = muxHandlerEnv.getQueue();
            if (queue != null) {
                _log.info("Using executor %s for mux handler %s with stack=%s)", queue, muxHandler, stackInstance);
                muxConfig.put(MuxFactory.OPT_INPUT_EXECUTOR, queue);
            } else {
                _log.info("Using executor %s for mux handler %s with stack=%s)", muxHandlerEnv.getReactor().getPlatformExecutor(), muxHandler, stackInstance);
            }

            //muxHandlerEnv.addStack(stackInstance, stackAddress, stackPort);
            
            MuxConnection c = muxFactory.newMuxConnection(muxHandlerEnv.getReactor(), this,
                muxHandlerEnv.getMuxHandler(), inetAddress, stackId, stackName, stackHost, stackInstance, muxConfig);
            
            // Register the added stack for our mux handler. Take care: the muxConnection.getStackInsatnce() may have mangled the
            // instancename, that's why we retrieve the instance name using muxConnection.getInstanceName() before registering it
            String mangledStackInstance = c.getStackInstance();
            
            // Check if the mux handler is already connected to the ioh represented by this advert.
            if (muxHandlerEnv.isConnectedTo(mangledStackInstance, stackAddress, advert.getPort())) {
                _log.debug("Ignoring stack %s for mux handler %s (already connected to it)", stackInstance,
                    muxHandlerEnv);
                return;
            }

            // See if we can add more stacks to this mux handler.
            if (!muxHandlerEnv.mayAddStack()) {
                _log.debug("Ignoring stack %s. Max simultaneous mux connections exceeded", stackInstance);
                return;
            }
            
            muxHandlerEnv.addStack(mangledStackInstance, stackAddress, stackPort);
            
            _advertByCnx.put(c, advert);
            _log.info("Connecting mux handler %s to stack instance %s with mux factory %s: connection=%s", muxHandler, stackInstance, muxFactory, c);
            muxFactory.connect(c);
        } else {
            _log.debug("Stack %s not accepted by mux handler %s", stackInstance, muxHandler.getAppName());
        }
    }

    private MuxFactory getMuxFactory(String protocol, Advertisement advert, Map<String, String> advertProps) {
        // check if the advert provides some informations about the mux handler to use.    	
    	boolean isLocalAdvert = isLocalAdvert(advert, advertProps);
    	
    	String preferredMuxFactory = null;
    	if (isLocalAdvert) {
    		preferredMuxFactory = advertProps.get("mux.factory.local");
            if (preferredMuxFactory == null) {
                _log.info("Ignoring local advert %s (no mux.factory.local property found)", advert);
            	return null;
            }
    	} else {
    		preferredMuxFactory = advertProps.get("mux.factory.remote");
    		if (preferredMuxFactory == null) {
                _log.info("no mux.factory.remote property found from remote advert: using legacy default mux factory.");
    			preferredMuxFactory = "default"; // consider legacy stack.
    		}
    	}
    	
        _log.debug("will use %s mux factory for advert %s", preferredMuxFactory, advert);
        MuxFactory muxFactory = _muxFactories.get(preferredMuxFactory);
        if (muxFactory == null) {
            _log.debug("Can't connect mux handler with protocol %s (no available mux factory with type %s).", protocol,
                preferredMuxFactory);
        }
        return muxFactory;
    }

    private boolean isLocalAdvert(Advertisement advert, Map<String, String> advertProps) {
        long advertId = ConfigHelper.getLong(advertProps, ConfigConstants.INSTANCE_ID);
        long myInstanceId = ConfigHelper.getLong(_systemConfig, ConfigConstants.INSTANCE_ID);
        boolean isLocalAdvert = (myInstanceId == advertId);
        _log.debug("advert instance id=%d, our instance id=%d -> advert %s is considered as a "
            + (isLocalAdvert ? "local" : "remote") + " advertisement.", advertId, myInstanceId, advert);
        return isLocalAdvert;
    }

    private void exit(boolean halt, String cause, Throwable error) {
	  Runtime.getRuntime().removeShutdownHook(_shutdownHook);
	  if (halt) {
		  _log.error("%s, halting agent %s", error, cause, this);
	      _shutdown.halt(1, true /* dump stack traces */);
	  } else {
		  _log.warn("%s, stopping agent %s", error, cause, this);
	      _shutdown.shutdown(this);
	  }
    }

    private static <T> T[] copy(T[] array) {
        @SuppressWarnings("unchecked")
        T[] copy = (T[]) java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), array.length);
        System.arraycopy(array, 0, copy, 0, array.length);
        return copy;
    }

    private static int[] copy(int[] array) {
        int[] copy = new int[array.length];
        System.arraycopy(array, 0, copy, 0, array.length);
        return copy;
    }

    @SuppressWarnings("unchecked")
    private void registerExternalStackAdverts(MuxHandlerEnv muxHandlerEnv) {
        _log.debug("checking if some external stack adverts must be registered with protocol %s", muxHandlerEnv.getProtocol());

        MuxHandler muxHandler = muxHandlerEnv.getMuxHandler();
        if (muxHandler == null) {
            _log.debug("No mux handler present for %s", muxHandlerEnv.getProtocol());
            return;
        }

        Map<Object, ?> map = (Map<Object, ?>) muxHandler.getMuxConfiguration().get(MuxHandler.CONF_EXTERNAL_STACKS);
        if (map == null) {
            _log.debug("No external stack connector found for mux handler %s", muxHandler.getAppName());
            return;
        }

        // extract stack addresses.
        String[] stackAddresses;
        Object o = map.get(MuxHandler.CONF_STACK_ADDRESS);
        if (o instanceof String) {
            stackAddresses = new String[] { (String) o };
        } else if (o instanceof String[]) {
            stackAddresses = copy((String[]) o);
        } else {
            _log.error("Ignoring invalid external stack CONF_STACK_ADDRESS: %s for mux handler %s", map, muxHandler);
            return;
        }

        // extract stack ports.
        Integer[] stackPorts;
        o = map.get(MuxHandler.CONF_STACK_PORT);
        if (o instanceof Integer) {
            stackPorts = new Integer[] { (Integer) o };
        } else if (o instanceof Integer[]) {
            stackPorts = copy((Integer[]) o);
        } else {
            _log.error("Ignoring invalid external stack CONF_STACK_PORT: %s for mux handler %s", map, muxHandler);
            return;
        }

        // extract stack host.
        String[] stackHosts;
        o = map.get(MuxHandler.CONF_STACK_HOST);
        if (o instanceof String) {
            stackHosts = new String[] { (String) o };
        } else if (o instanceof String[]) {
            stackHosts = copy(((String[]) o));
        } else {
            _log.error("Ignoring invalid external stack CONF_STACK_HOST: %s for mux handler %s", map, muxHandler);
            return;
        }

        // extract stack name.
        String[] stackNames;
        o = map.get(MuxHandler.CONF_STACK_NAME);
        if (o instanceof String) {
            stackNames = new String[] { (String) o };
        } else if (o instanceof String[]) {
            stackNames = copy((String[]) o);
        } else {
            _log.error("Ignoring invalid external stack CONF_STACK_NAME: %s for mux handler %s", map, muxHandler);
            return;
        }

        // extract stack id.
        int[] stackIds;
        o = map.get(MuxHandler.CONF_STACK_ID);
        if (o instanceof int[]) {
            stackIds = copy((int[]) o);
        } else {
            _log.error("Ignoring invalid external stack CONF_STACK_ID: %s for mux handler %s", map, muxHandler);
            return;
        }

        // extract stack instances. warning: the format is "group__instance" and we have to
        // strip the group name ...
        String[] stackInstances;
        o = map.get(MuxHandler.CONF_STACK_INSTANCE);
        if (o instanceof String) {
            stackInstances = new String[] { (String) o };
            // the format is group__instance: so we have to strip the group name.
            stackInstances[0] = stripGroup(stackInstances[0]);
        } else if (o instanceof String[]) {
            stackInstances = copy((String[]) o);
            // the format is group__instance: so we have to strip the group name.
            for (int i = 0; i < stackInstances.length; i++) {
                stackInstances[i] = stripGroup(stackInstances[i]);
            }
        } else {
            _log.error("Ignoring invalid external stack CONF_STACK_INSTANCE: %s for mux handler %s", map, muxHandler);
            return;
        }

        ExternalStackAdvert externalAdvert = new ExternalStackAdvert(stackHosts, stackAddresses, stackPorts, stackNames,
            stackIds, stackInstances);
        externalAdvert.register();
    }

    private String stripGroup(String groupInstance) {
        int separator = groupInstance.indexOf("__");
        return separator != -1 ? groupInstance.substring(separator + 2) : groupInstance;
    }

    private boolean acceptAdvert(Advertisement advert, Map<String, String> props) {
        // Check if the advert is matching our group
        String myGroup = _systemConfig.get(ConfigConstants.GROUP_NAME);
        String advertGroup = (String) props.get("group.name");
        String advertGroupTarget = (String) props.get("group.target");
        if (advertGroupTarget != null) {
            String[] targets = advertGroupTarget.split(",");
            for (String target : targets) {
            	target = target.trim();
                if ("*".equals(target) || myGroup.equals(target)) {
                    return true;
                }
            }
            _log.debug(
                "Ignoring advert %s (our group %s is not referenced in \"group.target\" advert property: %s.", advert,
                myGroup, advertGroupTarget);
        } else if (myGroup.equals(advertGroup)) {
            return true;
        }
        _log.debug("Ignoring advert %s (our group %s is not matching the advert group %s).", advert, myGroup,
            advertGroup);
        return false;
    }

    /**
     * External stack connector
     */
    private class ExternalStackAdvert {
        private String[] _stackHosts;
        private String[] _stackAddresses;
        private Integer[] _stackPorts;
        private String[] _stackNames;
        private int[] _stackIds;
        private String[] _stackInstances;

        ExternalStackAdvert(String[] stackHosts, String[] stackAddresses, Integer[] stackPorts, String[] stackNames, int[] stackIds, String[] stackInstances) {
            _stackHosts = stackHosts;
            _stackAddresses = stackAddresses;
            _stackPorts = stackPorts;
            _stackNames = stackNames;
            _stackIds = stackIds;
            _stackInstances = stackInstances;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ExternalStack:").append("hosts=").append(Arrays.toString(_stackHosts));
            sb.append(", addrs=").append(Arrays.toString(_stackAddresses));
            sb.append(", ports=").append(Arrays.toString(_stackPorts));
            sb.append(", names=").append(Arrays.toString(_stackNames));
            sb.append(", ids=").append(Arrays.toString(_stackIds));
            sb.append(", instances=").append(Arrays.toString(_stackInstances));
            return sb.toString();
        }

        public void register() {
            for (int i = 0; i < _stackHosts.length; i++) {
                String[] stackAddress = new String[] { _stackAddresses[i] };
                int[] stackPort = new int[] { _stackPorts[i].intValue() };
                String group = ConfigHelper.getString(_systemConfig, ConfigConstants.GROUP_NAME);

                final Advertisement advert = new Advertisement(stackAddress[0], stackPort[0]);
                final Hashtable<String, String> props = new Hashtable<>();
                props.put("provider", "external");
                props.put(ConfigConstants.HOST_NAME, _stackHosts[i]);
                props.put(ConfigConstants.GROUP_NAME, group);
                props.put(ConfigConstants.MODULE_ID, String.valueOf(_stackIds[i]));
                props.put(ConfigConstants.COMPONENT_NAME, _stackNames[i]);
                props.put(ConfigConstants.INSTANCE_NAME, _stackInstances[i]);

                _bctx.registerService(Advertisement.class.getName(), advert, props);
            }
        }
    }

    // We want to dump all current threads if we are exiting unexpectedly.
    class ThreadDumpOnExit extends Thread {
        public void run() {
            dumpState();
        }
    }
}
