// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.k8s.sless.fwk.runtime.impl;

import static com.nokia.as.k8s.sless.fwk.runtime.impl.FunctionEngineService.LOGGER;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.metering2.MeteringService;
import com.nokia.as.k8s.sless.Function;
import com.nokia.as.k8s.sless.fwk.FunctionResource;
import com.nokia.as.k8s.sless.fwk.RouteResource;

public class FunctionEngine {
    
    private List<FunctionExec> _pending = new ArrayList<> ();
    private FunctionEngineService _service;
    private Function _function;
    private FunctionMeters _meters;
    private Object _tracker;
    private List<Bundle> _deployedBundles;
    private PlatformExecutor _exec;
    private long _timeout;

    private String _name;
    private List<String> _location;
    
    public FunctionEngine (FunctionEngineService service, FunctionResource function, PlatformExecutor exec){
	_service = service;
	_name = function.name;
	_location = function.function().locations();
	_exec = exec;
	_timeout = function.function().timeout ().orElse (10) * 1000L; // sec to ms
    }
    public String toString (){
	return new StringBuilder ().append ("Function[name=").append (_name).append (" / state=").append (_state).append (']').toString ();
    }
    // called in FunctionEngineService exec
    public FunctionEngine init (FunctionResource function, BundleContext osgi, MeteringService ms){
	_meters = new FunctionMeters (_name);
	_meters.init (ms).start (osgi);
	if (!function.function ().lazy ()){
		_timeout = 0L; // cancel unload unless explicitly set
	    _exec.execute (() -> {STATE_LOADING.enter (); load ();});
	}
	return this;
    }

    public String name (){ return _name;}
    public void execute (Runnable r){ _exec.execute (r);}
    public PlatformExecutor executor (){ return _exec;}

    // called in FunctionEngineService exec
    public void join (FunctionEngineService fes, FunctionResource function, RouteResource route, BiConsumer<FunctionEngine, FunctionMeters.RouteMeters> cb){
	_exec.execute (() -> {
		if (_state.active () == false){
		    // destroyed but leave not yet seen by fes --> re-try
		    fes.join (function, route, cb);
		    return;
		}
		if (LOGGER.isInfoEnabled ())
		    LOGGER.info (FunctionEngine.this+" : join : "+cb);
		FunctionMeters.RouteMeters meters = _meters.addRoute (route.name ());
		cb.accept (FunctionEngine.this, meters);
	    });
    }
    // called from anywhere
    public void leave (RouteResource route){
	_exec.execute (() -> {
		if (LOGGER.isInfoEnabled ())
		    LOGGER.info (FunctionEngine.this+" : leave : "+route);
		if (_meters.removeRoute (route.name ()) == 0L){
		    if (LOGGER.isInfoEnabled ())
			LOGGER.info (FunctionEngine.this+" : destroy");
		    _state.destroy ();
		}
	    });
    }

    /********** callbacks from FunctionContextImpl **********/

    public void exec_me (FunctionExec exec){
	exec.setTimeoutFuture (_exec.schedule (() -> {_state.timeout (exec);}, exec.config ().ttl (), java.util.concurrent.TimeUnit.MILLISECONDS));
	_exec.execute (() -> _state.exec (exec));
    }

    public void interrupt_me (FunctionExec exec, String cause){
	_exec.execute (() -> _state.interrupt (exec, cause));
    }

    /************** private behavior *******/

    // called in _exec
    private void load (){
	if (LOGGER.isInfoEnabled ()) LOGGER.info (this+" : load");
	_meters._f_load.inc (1);
	List<BundleDeployer.Module> modules = new ArrayList<> ();
	InputStream[] ins = new InputStream[_location.size ()];
	int i = 0;
	try{
	    for (int k=0; k<ins.length; k++){
		if (LOGGER.isInfoEnabled ()) LOGGER.info (this+": loading URL : "+_location.get (k));
		ins[i++] = new URL (_location.get (k)).openStream ();
		modules.add (new BundleDeployer.Module(_location.get (k), ins[k]));
	    }
	    _deployedBundles = _service.deployer ().deploy(modules).get();
	    _tracker = _service.deployer ().track (this::loaded, this::unloaded,
					"(name="+_name+")");
	}catch (Exception e){
	    LOGGER.warn (this+": failed to load", e);
	    try{
		for (int k=0; k<i; k++) ins[k].close ();
	    }catch(Exception ee){
		LOGGER.warn (this+": failed to close FileInputStream", ee);
	    }
	}
    }

    // called in _exec
    private void unload (){
	if (LOGGER.isInfoEnabled ()) LOGGER.info (this+" : unload");
	_meters._f_unload.inc (1);
	_service.deployer ().untrack (_tracker);
	_service.deployer ().undeploy (_deployedBundles);
	_deployedBundles = null;
    }

    public void loaded (Function f){
	execute (() -> {
		LOGGER.warn (FunctionEngine.this+" : loaded : "+f);
		_function = f;
		_meters._f_loaded.inc (1);
		_meters._s_loaded.set (1);
		_state.loaded ();
	    });
    }
    public void unloaded (Function f){
	execute (() -> {
		LOGGER.warn (FunctionEngine.this+" : unloaded : "+f);
		_function = null;
		_meters._f_unloaded.inc (1);
		_meters._s_loaded.set (0);
		_state.unloaded ();
	    });
    }
    public void timeout (){
	// scheduled in _exec
	// no exec for a long time : unload
	_state.timeout ();
    }

    protected abstract class State {
	protected String _toString;
	protected State (String toString){
	    _toString = toString;
	}
	public String toString (){ return _toString;}
	public void execPendings (){
	    List<FunctionExec> pending = _pending;
	    _pending = new ArrayList<> ();	    
	    for (FunctionExec exec : pending){
		_state.exec (exec);
	    }
	}
	public void destroyPendings (){
	    List<FunctionExec> pending = _pending;
	    _pending = new ArrayList<> ();	    
	    for (FunctionExec exec : pending)
		destroy (exec);
	}
	public void destroy (FunctionExec exec){
	    exec.exec_error (new RuntimeException ("Function destroyed"));
	}
	// state events to override
	protected void enter (){
	    if (LOGGER.isDebugEnabled ()) LOGGER.debug (FunctionEngine.this+" : change state to : "+this);
	    _state = this;
	}
	protected boolean active (){ return true;}
	protected abstract void exec (FunctionExec exec);
	protected void timeout (FunctionExec exec){ exec.exec_timeout (); }
	protected void interrupt (FunctionExec exec, String cause){ exec.exec_interrupted (cause);}
	protected void loaded (){ throw new IllegalStateException (this+": function loaded");}
	protected void unloaded (){ throw new IllegalStateException (this+": function unloaded");}
	protected void timeout (){ throw new IllegalStateException (this+": function timeout");}
	protected abstract void destroy ();
    }

    protected State STATE_INIT = new State ("STATE_INIT"){
	    @Override
	    protected void exec (FunctionExec exec){
		_pending.add (exec);
		STATE_LOADING.enter ();
		load ();
	    }
	    @Override
	    protected void destroy (){ STATE_DESTROYED.enter (); }
	};
    
    protected State STATE_LOADING = new State ("STATE_LOADING"){
	    @Override
	    protected void exec (FunctionExec exec){
		_pending.add (exec);
	    }
	    @Override
	    protected void timeout (FunctionExec exec){
		_pending.remove (exec);
		super.timeout (exec);
	    }
	    @Override
	    protected void interrupt (FunctionExec exec, String cause){
		_pending.remove (exec);
		super.interrupt (exec, cause);
	    }
	    @Override
	    protected void loaded (){ STATE_LOADED.enter (); }
	    @Override
	    protected void destroy (){
		destroyPendings ();
		STATE_DESTROYED.enter ();
		unload (); // TODO check behavior
	    }
	};

    protected State STATE_LOADED = new State ("STATE_LOADED"){
	    private Future _timeoutF;
	    private long _lastRun;
	    @Override
	    protected void enter (){
		super.enter ();
		execPendings ();
		_lastRun = System.currentTimeMillis ();
		if (_timeout > 0L)
		    _timeoutF = _exec.schedule ((Runnable) FunctionEngine.this::timeout, _timeout, java.util.concurrent.TimeUnit.MILLISECONDS);
	    }
	    private void cancelTimeout (){
		if (_timeoutF != null){
		    _timeoutF.cancel (true);
		    _timeoutF = null;
		}
	    }
	    @Override
	    protected void exec (FunctionExec exec){
		_lastRun = System.currentTimeMillis ();
		exec.exec_run (_function);
	    }
	    @Override
	    protected void unloaded (){
		cancelTimeout ();
		STATE_LOADING.enter ();
	    }
	    @Override
	    protected void timeout (){
		_timeoutF = null;
		long delay = _lastRun + _timeout - System.currentTimeMillis ();
		if (delay < 50){ // planned in less than 50ms --> now !
		    STATE_UNLOADING.enter ();
		    unload ();
		} else {		    
		    _timeoutF = _exec.schedule ((Runnable) FunctionEngine.this::timeout, delay, java.util.concurrent.TimeUnit.MILLISECONDS);
		}
	    }
	    @Override
	    protected void destroy (){
		cancelTimeout ();
		STATE_UNLOADING_FINAL.enter ();
		unload ();
	    }
	};
    protected State STATE_UNLOADING = new State ("STATE_UNLOADING"){
	    @Override
	    protected void exec (FunctionExec exec){
		_pending.add (exec);
	    }
	    @Override
	    protected void timeout (FunctionExec exec){
		_pending.remove (exec);
		super.timeout (exec);
	    }
	    @Override
	    protected void interrupt (FunctionExec exec, String cause){
		_pending.remove (exec);
		super.interrupt (exec, cause);
	    }
	    @Override
	    protected void unloaded (){
		if (_pending.size () > 0){
		    STATE_LOADING.enter ();
		    load ();
		} else {
		    STATE_INIT.enter ();
		}
	    }
	    @Override
	    protected void destroy (){
		destroyPendings ();
		STATE_DESTROYED.enter ();
	    }
	};
    protected State STATE_UNLOADING_FINAL = new State ("STATE_UNLOADING_FINAL"){
	    @Override
	    protected boolean active (){ return false;}
	    @Override
	    protected void exec (FunctionExec exec){
		destroy (exec);
	    }
	    @Override
	    protected void unloaded (){ STATE_DESTROYED.enter (); }
	    @Override
	    protected void destroy (){}
	};
    protected State STATE_DESTROYED = new State ("STATE_DESTROYED"){
	    @Override
	    protected void enter (){
		super.enter ();
		_meters.stop ();
		_service.destroyed (FunctionEngine.this);
	    }
	    @Override
	    protected boolean active (){ return false;}
	    @Override
	    protected void exec (FunctionExec exec){
		exec.exec_error (new RuntimeException ("Function destroyed"));
	    }
	    @Override
	    protected void loaded (){ // coming from STATE_LOADING
		unload ();
	    }
	    @Override
	    protected void unloaded (){ // coming from STATE_LOADED
	    }
	    @Override
	    protected void destroy (){}
	};

    private State _state = STATE_INIT;


}
