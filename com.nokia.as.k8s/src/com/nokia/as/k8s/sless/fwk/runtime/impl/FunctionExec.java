// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.k8s.sless.fwk.runtime.impl;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.Logger;
import com.nokia.as.k8s.sless.*;
import io.cloudevents.CloudEvent;
import com.nokia.as.k8s.sless.fwk.runtime.*;

public class FunctionExec implements ExecContext, EventContext {

    private static final AtomicLong SEED = new AtomicLong (1);
    private static long new_uid (){
	long l = SEED.getAndIncrement ();
	return l;
    }

    protected FunctionContextImpl _fc;
    protected ExecConfig _execConfig;
    protected long _limitDate;
    protected long _uid = new_uid ();
    
    protected volatile boolean _interrupted;

    public FunctionExec (CloudEvent event, FunctionContextImpl fc, ExecConfig cfg){
	_event = event;
	_fc = fc;
	_execConfig = cfg;
	_limitDate = System.currentTimeMillis () + _execConfig.ttl ();
	log ("created");
    }

    public String toString (){
	return new StringBuilder ().append ("Exec[id=").append (_uid).append ("/evt=").append (_event.getId ()).append (_interrupted ? "/done" : "/running").append (']').toString ();
    }

    public long id (){ return _uid;}

    /*********** EventContext ********/

    public java.util.Map<String, Object> eventParameters (){
	return _execConfig.eventParameters ();
    }

    public java.util.Map<String, Object> functionParameters (){
	return _execConfig.functionParameters ();
    }
    
    public void log (String log){
	_fc.log_me (this, log);
    }

    public boolean interrupted (){
	return _interrupted;
    }

    public long ttl (){
	if (interrupted ()) return 0L;
	long left = _limitDate - System.currentTimeMillis ();
	return Math.max (0L, left);
    }

    public java.util.concurrent.Executor executor (){
	return _execConfig.executor ();
    }

    /************ ExecContext *******/
    
    public void interrupt (String cause){
	_fc.interrupt_me (this, cause);
    }

    public ExecConfig config (){
	return _execConfig;
    }

    public EventContext eventContext (){ return this;}

    public CloudEvent event (){ return _event;}

    /**************************/
    
    // called in executor
    private boolean error (Throwable t){
	if (_interrupted) return false;
	_interrupted = true;
	if (_startExec != 0L) _stopExec = System.currentTimeMillis ();
	cancelTimeoutFuture ();
	log ("error : "+t);
	_execConfig.onError().accept (FunctionExec.this, t);
	return true;
    }
    private boolean success (CloudEvent result){
	if (_interrupted) return false;
	_interrupted = true;
	_stopExec = System.currentTimeMillis ();
	cancelTimeoutFuture ();
	log ("success");
	_execConfig.onSuccess().accept (FunctionExec.this, result);
	_fc.done_success (this);
	return true;
    }
    private void cancelTimeoutFuture (){
	if (_timeoutF != null){
	    _timeoutF.cancel (true);
	    _timeoutF = null;
	}
    }
    
    /************ Called by FunctionContextImpl ***********/

    private CloudEvent _event;
    private Future _timeoutF;
    private long _startExec, _stopExec;

    public long execDelay (){ return _stopExec - _startExec; }
    
    public FunctionExec exec (){
	_fc.exec_me (this);
	return this;
    }
    public void setTimeoutFuture (Future f){
	_timeoutF = f;
    }
    
    public void exec_interrupted (String cause){
	executor ().execute ( () -> {
		if (error (new InterruptedException (cause)))
		    _fc.done_interrupted (FunctionExec.this);
	    });
    }
    
    public void exec_timeout (){
	executor ().execute ( () -> {
		if (error (new java.util.concurrent.TimeoutException ()))
		    _fc.done_timeout (FunctionExec.this);
	    });
    }

    public void exec_error (Throwable t){
	executor ().execute ( () -> {
		if (error (t))
		    _fc.done_error (FunctionExec.this);
	    });
    }
    
    public void exec_run (Function f){
	executor ().execute (() -> {
		log ("exec");
		_fc.start (FunctionExec.this);
		CompletableFuture<CloudEvent> cf;
		_startExec = System.currentTimeMillis ();
		try{
		    cf = f.apply (_event, FunctionExec.this);
		}catch(Throwable t){
		    error (t);
		    _fc.done_error (FunctionExec.this);
		    return;
		}
		cf.whenComplete ((CloudEvent result, Throwable exception) -> {
			if (exception != null){
			    exec_error (exception);
			} else {
			    executor ().execute ( () -> {
				    success (result);
				});
			}
		    });
	    });
    }
}
