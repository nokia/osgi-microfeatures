// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.diameter.lb.impl;

import java.util.*;

import com.alcatel.as.diameter.lb.DestinationManager;
import com.alcatel.as.ioh.client.TcpClient.Destination;
import alcatel.tess.hometop.gateways.reactor.*;

public class SimpleDestinationManager extends AbstractDestinationManager {

    private int _available = 0;
    private int _unavailable = 0;

    private DestinationWrapper _firstAvailable;
    private DestinationWrapper _firstUnavailable;
    private DestinationWrapper _anyAvailable;
    private DestinationWrapper _anyUnavailable;
    
    public SimpleDestinationManager (String group){
	super (group);
    }
    public String toString(){ return "SimpleDestinationManager["+_group+", "+connected()+"]";}

    public void clear (){
	_firstAvailable = null;
	_firstUnavailable = null;
	_anyAvailable = null;
	_anyUnavailable = null;
	_available = 0;
	_unavailable = 0;
    }

    public int connected (){
	return _available + _unavailable;
    }

    public int available (){
	return _available;
    }

    public int unavailable (){
	return _unavailable;
    }

    public void add (Destination dest, int weight){
	super.add (dest, weight);
	_firstAvailable = new DestinationWrapper (dest).insert (null, _firstAvailable);
	if (_anyAvailable == null) _anyAvailable = _firstAvailable;
	_available++;
    }

    public void update (Destination dest, int weight){}
    
    public boolean remove (Destination dest){
	super.remove (dest);
	DestinationWrapper wrapper = getWrapper (_firstAvailable, dest);
	if (wrapper != null){
	    _available--;
	    _firstAvailable = wrapper.remove (_firstAvailable);
	    _anyAvailable = _firstAvailable;
	    return true;
	}
	wrapper = getWrapper (_firstUnavailable, dest);
	if (wrapper != null){
	    _unavailable--;
	    _firstUnavailable = wrapper.remove (_firstUnavailable);
	    _anyUnavailable = _firstUnavailable;
	    return true;
	}
	return false;
    }

    public void available (Destination dest, int weight){
	DestinationWrapper wrapper = getWrapper (_firstUnavailable, dest);
	if (wrapper != null){
	    _unavailable--;
	    _available++;
	    _firstUnavailable = wrapper.remove (_firstUnavailable);
	    _firstAvailable = wrapper.insert (null, _firstAvailable);
	    _anyUnavailable = _firstUnavailable;
	    _anyAvailable = _firstAvailable;
	}
    }

    public void unavailable (Destination dest){
	DestinationWrapper wrapper = getWrapper (_firstAvailable, dest);
	if (wrapper != null){
	    _available--;
	    _unavailable++;
	    _firstAvailable = wrapper.remove (_firstAvailable);
	    _firstUnavailable = wrapper.insert (null, _firstUnavailable);
	    _anyUnavailable = _firstUnavailable;
	    _anyAvailable = _firstAvailable;
	}
    }
    
    public Destination getAny (){
	if (_anyAvailable != null){
	    try{ return _anyAvailable._destination; }
	    finally {
		if ((_anyAvailable = _anyAvailable._next) == null)
		    _anyAvailable = _firstAvailable;
	    }
	}
	if (_anyUnavailable != null){
	    try{ return _anyUnavailable._destination; }
	    finally {
		if ((_anyUnavailable = _anyUnavailable._next) == null)
		    _anyUnavailable = _firstUnavailable;
	    }
	}
	return null;
    }

    public int sendAll (byte[] bytes){
	return sendAll (bytes, _firstAvailable)+sendAll (bytes, _firstUnavailable);
    }
    private int sendAll (byte[] bytes, DestinationWrapper from){
	int ret = 0;
	DestinationWrapper wrapper = from;
	while (wrapper != null){
	    wrapper._destination.send (bytes, true);
	    ret++;
	    wrapper = wrapper._next;
	}
	return ret;
    }

    private DestinationWrapper getWrapper (DestinationWrapper from, Destination dest){
	DestinationWrapper tmp = from;
	while (tmp != null){
	    if (tmp._destination == dest) return tmp;
	    tmp = tmp._next;
	}
	return null;
    }

    private static class DestinationWrapper {
	private Destination _destination;
	private DestinationWrapper _prev, _next;
	private DestinationWrapper (Destination dest){
	    _destination = dest;
	}
	private DestinationWrapper insert (DestinationWrapper prev, DestinationWrapper next){
	    _prev = prev;
	    if (prev != null) prev._next = this;
	    _next = next;
	    if (next != null) next._prev = this;
	    return this;
	}
	private DestinationWrapper remove (DestinationWrapper first){
	    if (_prev != null){
		_prev._next = _next;
		if (_next != null)
		    _next._prev = _prev;
		return first;
	    }
	    if (_next != null){
		_next._prev = null;
		return _next;
	    }
	    return null;
	}
    }
}
