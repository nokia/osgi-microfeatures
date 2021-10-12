// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.k8s.sless.fwk.runtime.impl;

import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.util.Meters;
import java.util.*;

public class FunctionMeters extends SimpleMonitorable {
    
    public Meter _f_loaded, _f_unloaded, _f_load, _f_unload;
    public Meter _r_open;
    public Meter _s_loaded;
    
    public String _fname;
    public RouteMeters _aggregatedRoutes;
    public Map<String, RouteMeters> _routeMeters = new HashMap<> ();
    private MeteringService _metering;
    
    public FunctionMeters (String name){
	super ("sless.function."+normalize (name), "Function "+name);
	_fname = normalize (name);
    }
    
    public FunctionMeters init (MeteringService ms){
	_metering = ms;
	addMeter (Meters.createUptimeMeter (ms));
	_s_loaded = createAbsoluteMeter (ms, _fname+".state.loaded");
	_f_load = createIncrementalMeter (ms, _fname+".binary.load", null);
	_f_unload = createIncrementalMeter (ms, _fname+".binary.unload", null);
	_f_loaded = createIncrementalMeter (ms, _fname+".binary.loaded", null);
	_f_unloaded = createIncrementalMeter (ms, _fname+".binary.unloaded", null);
	_r_open = createIncrementalMeter (ms, _fname+".route.open", null);
	_aggregatedRoutes = new RouteMeters (null, ms);
	return this;
    }

    public RouteMeters addRoute (String route){
	_r_open.inc (1);
	route = normalize (route);
	RouteMeters meters = new RouteMeters (route, _metering);
	_routeMeters.put (route, meters);
	updated ();
	return meters;
    }
    public long removeRoute (String route){
	_r_open.inc (-1);
	route = normalize (route);
	RouteMeters meters = _routeMeters.remove (route);
	meters.removed ();
	updated ();
	return _r_open.getValue ();
    }
    public void stop (){
	_aggregatedRoutes.stop ();
	for (RouteMeters meters : _routeMeters.values ())
	    meters.stop ();
	super.stop ();
    }
    
    public class RouteMeters {

	public Meter _x_run, _x_done, _x_success, _x_fail, _x_timeout, _x_interrupt, _x_error;
	public Meter _x_running;
	public Meter _x_run_rate;
	public Meter _x_elapsed, _x_elapsed_success, _x_elapsed_fail;
	
	private RouteMeters (String route, MeteringService ms){
	    String prefix = (route != null) ? route+":"+_fname+"." : _fname+".";
	    _x_run = createIncrementalMeter (ms, prefix+"exec.run", route != null ? _aggregatedRoutes._x_run : null);
	    _x_done = createIncrementalMeter (ms, prefix+"exec.done", route != null ? _aggregatedRoutes._x_done : null);
	    _x_success = createIncrementalMeter (ms, prefix+"exec.done.success", route != null ? _aggregatedRoutes._x_success : null);
	    _x_fail = createIncrementalMeter (ms, prefix+"exec.done.fail", route != null ? _aggregatedRoutes._x_fail : null);
	    _x_timeout = createIncrementalMeter (ms, prefix+"exec.done.fail.timeout", route != null ? _aggregatedRoutes._x_timeout : null);
	    _x_interrupt = createIncrementalMeter (ms, prefix+"exec.done.fail.interrupt", route != null ? _aggregatedRoutes._x_interrupt : null);
	    _x_error = createIncrementalMeter (ms, prefix+"exec.done.fail.error", route != null ? _aggregatedRoutes._x_error : null);
	    _x_running = createValueSuppliedMeter (ms, prefix+"exec.running", () -> {
		    return _x_run.getValue () - _x_done.getValue ();
		});
	
	    addMeter (_x_run_rate = Meters.createRateMeter (ms, _x_run, 1000L));
	    
	    _x_elapsed = createIncrementalMeter (ms, prefix+"exec.elapsed", route != null ? _aggregatedRoutes._x_elapsed : null);
	    _x_elapsed_success = createIncrementalMeter (ms, prefix+"exec.elapsed.success", route != null ? _aggregatedRoutes._x_elapsed_success : null);
	    _x_elapsed_fail = createIncrementalMeter (ms, prefix+"exec.elapsed.fail", route != null ? _aggregatedRoutes._x_elapsed_fail : null);
	}
	private void removed (){
	    removeMeter (_x_run, _x_done, _x_success, _x_fail, _x_timeout, _x_interrupt, _x_error, _x_running, _x_run_rate, _x_elapsed, _x_elapsed_success, _x_elapsed_fail);
	    stop ();
	}
	public void stop (){
	    Meters.stopRateMeter (_x_run_rate);
	}
	public void exec (){
	    _x_run.inc (1);
	}
	private void fail (long delay){ // private
	    _x_done.inc (1);
	    _x_fail.inc (1);
	    _x_elapsed_fail.inc (delay);	    
	}
	public void interrupt (long delay){
	    fail (delay);
	    _x_interrupt.inc (1);
	}
	public void timeout (long delay){
	    fail (delay);
	    _x_timeout.inc (1);
	}
	public void error (long delay){
	    fail (delay);
	    _x_error.inc (1);
	}
	public void success (long delay){
	    _x_done.inc (1);
	    _x_success.inc (1);
	    _x_elapsed_success.inc (delay);
	}
    }

    public static String normalize (String name){ // normalized name
	StringBuilder sb = new StringBuilder ();
	for (int i=0; i<name.length (); i++){
	    char c = name.charAt (i);
	    if (c >= 'a' && c <= 'z') sb.append (c);
	    else if (c >= 'A' && c <= 'Z') sb.append (c);
	    else if (c >= '0' && c <= '9') sb.append (c);
	    else sb.append ('_');
	}
	return sb.toString ();
    }
}

/************************************************

exportMeters -ms sless.function -mts {function}.state.loaded -alias sless_function_state_loaded
exportMeters -ms sless.function -mts {function}.binary.load -alias sless_function_binary_load
exportMeters -ms sless.function -mts {function}.binary.unload -alias sless_function_binary_unload
exportMeters -ms sless.function -mts {function}.binary.loaded -alias sless_function_binary_loaded
exportMeters -ms sless.function -mts {function}.binary.unloaded -alias sless_function_binary_unloaded
exportMeters -ms sless.function -mts {function}.route.open -alias sless_function_route_open

exportMeters -ms sless.function -mts {route}:{function}.exec.run -alias sless_function_exec_run
exportMeters -ms sless.function -mts {route}:{function}.exec.running -alias sless_function_exec_running
exportMeters -ms sless.function -mts {route}:{function}.exec.run.rate -alias sless_function_run_rate
exportMeters -ms sless.function -mts {route}:{function}.exec.done -alias sless_function_exec_done
exportMeters -ms sless.function -mts {route}:{function}.exec.done.success -alias sless_function_done_success
exportMeters -ms sless.function -mts {route}:{function}.exec.done.fail -alias sless_function_done_fail
exportMeters -ms sless.function -mts {route}:{function}.exec.done.fail.timeout -alias sless_function_fail_timeout
exportMeters -ms sless.function -mts {route}:{function}.exec.done.fail.interrupt -alias sless_function_fail_interrupt
exportMeters -ms sless.function -mts {route}:{function}.exec.done.fail.error -alias sless_function_fail_error
exportMeters -ms sless.function -mts {route}:{function}.exec.elsapsed -alias sless_function_exec_elsapsed
exportMeters -ms sless.function -mts {route}:{function}.exec.elsapsed.success -alias sless_function_elsapsed_success
exportMeters -ms sless.function -mts {route}:{function}.exec.elsapsed.fail -alias sless_function_elsapsed_fail

************************************************/
