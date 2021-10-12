// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import org.apache.log4j.Logger;

public class OnAvailable {

    private List<Listener> _listeners;
    private boolean _closed;
    private BooleanSupplier _isAvailable;
    private BiFunction<Runnable, Long, Future> _scheduler;
    private Logger _logger;

    public OnAvailable (BooleanSupplier isAvailable, BiFunction<Runnable, Long, Future> scheduler, Logger logger){
	_isAvailable = isAvailable;
	_scheduler = scheduler;
	_logger = logger;
    }
    
    public void add (Runnable onSuccess, Runnable onFailure, long delay){
	if (_isAvailable.getAsBoolean ()){
	    onSuccess.run (); // inline ! to avoid spinning
	    return;
	}
	if (_closed){
	    onFailure.run (); // inline !
	    return;
	}
	if (_listeners == null) _listeners = new ArrayList<> (3);
	Listener listener = new Listener (onSuccess, onFailure);
	_listeners.add (listener);
	listener._timeoutF = _scheduler.apply (listener::timeout, delay);
	return;
    }

    public void available (){
	if (_listeners == null) return;
	while (_listeners.size () > 0 && _isAvailable.getAsBoolean ()){
	    _listeners.remove (0).available ();
	}
    }
    public void closed (){ // idempotent
	if (_closed) return;
	_closed = true;
	if (_listeners != null){
	    for (int i=0; i<_listeners.size (); i++){
		_listeners.get (i).closed ();
	    }
	    _listeners.clear ();
	}
    }
    private class Listener {
	private Runnable _onSuccess, _onFailure;
	private Future _timeoutF;
	private boolean _done;
	private Listener (Runnable onSuccess, Runnable onFailure){
	    _onSuccess = onSuccess;
	    _onFailure = onFailure;
	}
	private void available (){
	    _timeoutF.cancel (true);
	    _done = true;
	    try{_onSuccess.run ();}catch(Throwable t){_logger.warn ("Exception while calling onAvailable callback", t);}
	}
	private void closed (){
	    _timeoutF.cancel (true);
	    _done = true;
	    try{_onFailure.run ();}catch(Throwable t){_logger.warn ("Exception while calling onFailure callback", t);}
	}
	private void timeout (){
	    if (_done) return; // called in right exec, but may still be scheduled from timerService even though cancelled - so need a flag to be sure
	    _listeners.remove (this);
	    _onFailure.run ();
	}
    }
}
