package com.nokia.as.k8s.sless.fwk.runtime.impl;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.SerialExecutor;
import com.nokia.as.k8s.sless.fwk.FunctionResource;
import com.nokia.as.k8s.sless.fwk.RouteResource;
import com.nokia.as.k8s.sless.fwk.runtime.ExecConfig;
import com.nokia.as.k8s.sless.fwk.runtime.ExecContext;
import com.nokia.as.k8s.sless.fwk.runtime.FunctionContext;

import io.cloudevents.CloudEvent;


public class FunctionContextImpl implements FunctionContext {

    public final static Logger LOGGER = Logger.getLogger("sless.runtime.function");
    public final static Logger LOGGER_EXEC = Logger.getLogger("sless.runtime.function.exec");

    private FunctionResource _functionR;
    private RouteResource _routeR;
    private PlatformExecutors _execs;
    private BundleContext _osgi;
    private long _defTTL;
    private FunctionEngine _engine;
    private ServiceRegistration _reg;
    private boolean _stopped;
    private SerialExecutor _exec = new SerialExecutor ();
    private Map<String, Object> _functionParams;
    private FunctionMeters.RouteMeters _meters;
    
    public FunctionContextImpl (RouteResource route, FunctionResource function){
	_routeR = route;
	_functionR = function;
	_defTTL = route.exec ().isPresent () ? route.exec ().get ().ttl ().orElse (5000): 5000L;
	if(_defTTL <= 0) _defTTL = 5000L;
	_functionParams = new HashMap<> ();
	_functionParams.putAll (function.paramsAsMap ());
	_functionParams.putAll (route.function.paramsAsMap ());
	_functionParams = Collections.unmodifiableMap (_functionParams);
    }
    public String toString (){
	return new StringBuilder ().append ("FunctionContext[ Route[name=").append (_routeR.name).append (", type=").append (_routeR.route.type).append (", path=").append (_routeR.route.path).append ("] / ").append (_engine).append (']').toString ();
    }
    public FunctionContextImpl start (BundleContext osgi, PlatformExecutors execs, FunctionEngineService fes){
	LOGGER.info (this+" : start");
	_osgi = osgi;
	_execs = execs;
	fes.join (_functionR, _routeR, this::joined);
	return this;
    }
    // called in engine _exec
    public void joined (FunctionEngine engine, FunctionMeters.RouteMeters meters){
	_exec.execute (() -> {
		_engine = engine;
		_meters = meters;
		LOGGER.info (FunctionContextImpl.this+" : joined");
		if (_stopped){
		    _engine.leave (_routeR);
		    return;
		}
		Dictionary props = new Hashtable ();
		props.put ("type", _routeR.route.type);
		_reg = _osgi.registerService (FunctionContext.class.getName (), FunctionContextImpl.this, props);
	    });	   
    }
    public void stop (){
	_exec.execute (() -> {
		LOGGER.info (FunctionContextImpl.this+" : stop");
		_stopped = true;
		if (_engine != null){
		    _reg.unregister ();
		    _engine.leave (_routeR);
		}
	    });
    }
    
    /************ public api ***********/

    public FunctionResource function (){ return _functionR;}
    public RouteResource route (){ return _routeR;}

    public ExecContext exec (CloudEvent event, ExecConfig conf){
	if (conf.executor () == null) conf.executor (_execs.createQueueExecutor (_execs.getProcessingThreadPoolExecutor ()));
	Map<String, Object> map = conf.functionParameters ();
	if (map == null) conf.functionParameters (_functionParams);
	else map.putAll (_functionParams);
	long ttl = conf.ttl ();
	if (ttl <= 0){
	    ttl = conf.maxttl ();
	    if (ttl <= 0) ttl = _defTTL;
	    else ttl = Math.min (_defTTL, ttl);
	    conf.ttl (ttl);
	}
	FunctionExec exec = new FunctionExec (event, this, conf);
	return exec.exec ();
    }

    /********** callbacks from FunctionExec **********/

    public void log_me (FunctionExec exec, String log){
	if (LOGGER_EXEC.isInfoEnabled ())
	    LOGGER_EXEC.info (this+" : "+exec+" : "+log);
    }

    public void exec_me (FunctionExec exec){
	_engine.exec_me (exec);
    }

    public void interrupt_me (FunctionExec exec, String cause){
	_engine.interrupt_me (exec, cause);
    }

    public void start (FunctionExec exec){
	_meters.exec ();
    }
    
    // the outputs of the function exec - can be done in any thread    
    public void done_interrupted (FunctionExec exec){
	_meters.interrupt (Math.max (1L, exec.execDelay ()));
    }
    public void done_timeout (FunctionExec exec){
	// TODO : check if the exec was executed at all (or timeout in load)
	_meters.timeout (Math.max (1L, exec.execDelay ()));
    }
    public void done_error (FunctionExec exec){
	_meters.error (Math.max (1L, exec.execDelay ()));
    }
    public void done_success (FunctionExec exec){
	_meters.success (Math.max (1L, exec.execDelay ()));
    }
}
