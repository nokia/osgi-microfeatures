package com.nokia.as.k8s.sless.fwk.runtime.impl.agent;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.SerialExecutor;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxContext;
import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.MuxHeader;
import com.nokia.as.k8s.controller.CustomResource;
import com.nokia.as.k8s.sless.fwk.FunctionResource;
import com.nokia.as.k8s.sless.fwk.Mux;
import com.nokia.as.k8s.sless.fwk.RouteResource;
import com.nokia.as.k8s.sless.fwk.runtime.SlessRuntime;
import com.nokia.as.k8s.sless.fwk.runtime.impl.FunctionContextImpl;
import com.nokia.as.k8s.sless.fwk.runtime.impl.FunctionEngineService;

@Component(service={}, immediate=true, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class RuntimeAgent {

    static final Logger LOGGER = Logger.getLogger("sless.runtime.agent");

    private List<SlessRuntime> _runtimesPending = new ArrayList<> ();
    private BundleContext _osgi;
    private FunctionEngineService _engineS;
    private PlatformExecutors _execs;
    private Map<SlessRuntime, RuntimeMuxHandler> _runtimes = new HashMap<> ();

    public String toString (){ return "RuntimeAgent";}
    
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public synchronized void setRuntime (SlessRuntime rt, Map<String, String> properties){
	LOGGER.info (this+" : setRuntime : "+rt);
	_runtimesPending.add (rt);
	initMuxHandlers ();
    }
    public synchronized void unsetRuntime (SlessRuntime rt){
	LOGGER.info (this+" : unsetRuntime : "+rt);
	RuntimeMuxHandler handler = _runtimes.remove (rt);
	if (handler != null){
	    handler.unregister ();
	    return;
	}
	_runtimesPending.remove (rt);
    }

    @Reference
    public void setEngineService (FunctionEngineService fes){
	_engineS = fes;
    }

    @Reference
    public void setPlatformExecs (PlatformExecutors execs){
	_execs = execs;
    }
    
    @Activate
    public synchronized void activate (BundleContext ctx, Map<String, String> conf){
	LOGGER.info (this+" : activate");
	_osgi = ctx;
	initMuxHandlers ();
    }

    private void initMuxHandlers (){
	if (_osgi == null) return;
	for (SlessRuntime rt: _runtimesPending){
	    RuntimeMuxHandler handler = new RuntimeMuxHandler (this, rt);
	    handler.register ();
	    _runtimes.put (rt, handler);
	}
        _runtimesPending.clear ();
    }

    private static class RuntimeMuxHandler extends MuxHandler {

	private SlessRuntime _runtime;
	private RuntimeAgent _agent;
	private String _toString;
	private Map<String, FunctionContextImpl> _functions = new HashMap<> ();
	private ServiceRegistration _reg;
	private SerialExecutor _exec = new SerialExecutor ();
	
	private RuntimeMuxHandler (RuntimeAgent agent, SlessRuntime rt){
	    _agent = agent;
	    _runtime = rt;
	    _toString = "RuntimeMuxHandler["+_runtime.type ()+"]";
	}
	public String toString (){ return _toString;}
	protected void register (){
	    LOGGER.info (this+" : register");
	    // the following props will be set in the opened TcpServers, all the processor.advertize in particular will be used in advertizing
	    Dictionary props = new Hashtable ();
	    props.put ("protocol", "sless."+_runtime.type ());
	    props.put ("autoreporting", "false");
	    props.put ("hidden", "true");
	    _reg = _agent._osgi.registerService (MuxHandler.class.getName (), this, props);
	}
	protected void unregister (){
	    _reg.unregister (); // TODO check if Callout handles it OK.
	    // TODO clarify the behavior with destroy()
	}

	// ---------------- MuxHandler interface
	// -----------------------------------------------------------

	/** Called by the CalloutAgent when it has seen our MuxHandler */
	@SuppressWarnings("unchecked")
	@Override
	public void init(int appId, String appName, String appInstance, MuxContext muxContext) {
	    // Don't forget to call the super.init method !
	    super.init(appId, toString (), appInstance, muxContext);

	    getMuxConfiguration().put(CONF_STACK_ID, Mux.CONTROLLER_ID);
	    getMuxConfiguration().put(CONF_USE_NIO, true);
	    getMuxConfiguration().put(CONF_THREAD_SAFE, true);
	    getMuxConfiguration().put(CONF_IPV6_SUPPORT, true);
	    getMuxConfiguration().put(CONF_L4_PROTOCOLS, new String[0]);
	}
	
	@Override
	public void muxOpened(final MuxConnection connection) {
	    LOGGER.warn (this+" : muxOpened : "+connection);
	}

	@Override
	public void muxClosed(MuxConnection connection) {
	    LOGGER.warn (this+" : muxClosed : "+connection);
	}

	@Override
	public void muxData(MuxConnection connection, MuxHeader header, ByteBuffer buffer){
	    switch (header.getFlags ()){
	    case Mux.FLAG_GOGO_REQ:
		long id = header.getSessionId ();
		String req = Mux.getUTF8 (buffer);
		if (LOGGER.isInfoEnabled ()) LOGGER.info (this+" : gogo-req : id="+id+" : "+req);
		gogo (id, req);
		return;
	    case Mux.FLAG_PUSH:
		CustomResource route = Mux.readResource (buffer, RouteResource.CRD);
		CustomResource function = Mux.readResource (buffer, FunctionResource.CRD);
		RouteResource routeR = RouteResource.of(route);
		FunctionResource functionR = FunctionResource.of(function);
		if (LOGGER.isInfoEnabled ()) LOGGER.info (this+" : push : "+functionR+" / "+routeR+" from "+connection);
		_exec.execute (() -> {start (routeR, functionR);});
		return;
	    case Mux.FLAG_UNPUSH:
		String routeS = Mux.getUTF8 (buffer);
		if (LOGGER.isInfoEnabled ()) LOGGER.info (this+" : unpush : "+routeS+" from "+connection);
		_exec.execute (() -> {stop (routeS);});
		return;
	    }
	}
	
	public void destroy() {
	}

	@Override
	public int getMinorVersion() {
	    return 0;
	}

	@Override
	public int getMajorVersion() {
	    return 1;
	}

	@Override
	public int[] getCounters() {
	    throw new RuntimeException("deprecated method, should not be used anymore");
	}

	@Override
	public void commandEvent(int command, int[] intParams, String[] strParams) {
	}
	
	/********************************
	 * Utils *
	 *******************************/
	
	private void gogo (long id, String req){
	}

	private FunctionContextImpl start (RouteResource route, FunctionResource function){
	    FunctionContextImpl ctx = new FunctionContextImpl (route, function);
	    _functions.put (route.name, ctx);
	    ctx.start (_agent._osgi, _agent._execs, _agent._engineS);
	    return ctx;
	}

	private FunctionContextImpl stop (String route){
	    FunctionContextImpl ctx = _functions.remove (route);
	    ctx.stop ();
	    return ctx;
	}
    }
}
