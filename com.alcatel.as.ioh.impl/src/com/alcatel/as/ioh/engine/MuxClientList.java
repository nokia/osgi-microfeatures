// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.engine;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.*;
import java.nio.*;
import java.net.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClientState;

public class MuxClientList {

    public static interface Iterator<T> {
	T next (MuxClient agent, T ctx);
    }

    protected static final ArrayList<MuxClient> VOID = new ArrayList<>(0);

    protected static AtomicInteger initSeed = new AtomicInteger (0);

    protected ArrayList<MuxClient> _remoteAgents;
    protected ArrayList<MuxClient> _localAgents;
    protected ArrayList<MuxClient> _remoteIOHs;
    protected ArrayList<MuxClient> _deactivated;
    protected int _rnd = initSeed.getAndIncrement ();
    protected boolean _hasRemoteAgent, _hasLocalAgent, _hasRemoteIOH;
    protected boolean _iterating;
    protected int _size;

    public MuxClientList (){
	_remoteAgents = new ArrayList<> ();
	_localAgents = new ArrayList<> (2);
	_remoteIOHs = new ArrayList<> (3);
	_deactivated = new ArrayList<> (3);
    }
    public MuxClientList (MuxClientList other, boolean useMux){
	_localAgents = (ArrayList) other._localAgents.clone ();
	_deactivated = (ArrayList) other._deactivated.clone ();
	if (useMux){
	    _remoteAgents = (ArrayList) other._remoteAgents.clone ();
	    _remoteIOHs = (ArrayList) other._remoteIOHs.clone ();
	} else {
	    _remoteAgents = VOID;
	    _remoteIOHs = VOID;
	}
	changed ();
    }
    public boolean iterating (){ return _iterating;}
    public String toString (){
	StringBuilder sb = new StringBuilder ();
	sb.append ("MuxClientList[");
	sb.append ("RemoteAgents=").append (_remoteAgents);
	if (_hasLocalAgent) sb.append (", LocalAgents=").append (_localAgents);
	if (_hasRemoteIOH) sb.append (", RemoteIOHs=").append (_remoteIOHs);
	if (_deactivated.size () > 0) sb.append (", deactivated=").append (_deactivated);
	sb.append (']');
	return sb.toString ();
    }

    protected void changed (){
	_hasRemoteAgent = _remoteAgents.size () > 0;
	_hasLocalAgent = _localAgents.size () > 0;
	_hasRemoteIOH = _remoteIOHs.size () > 0;
	_size = _remoteAgents.size () + _remoteIOHs.size () + _localAgents.size () + _deactivated.size ();
    }
    public int size (){ return _size;}
    public int sizeOfActive (){ return _remoteAgents.size () + _remoteIOHs.size () + _localAgents.size ();}
    public int sizeOfRemoteAgents (){ return sizeOfRemoteAgents (true); }
    public int sizeOfRemoteAgents (boolean activeOnly){
	if (activeOnly) return _remoteAgents.size ();
	int n = _remoteAgents.size ();
	for (MuxClient agent : _deactivated){
	    if (agent.isLocalAgent () || agent.isRemoteIOHEngine ()) continue;
	    n++;
	}
	return n;
    }
    
    public void clear (){
	if (_size == 0) return;
	_remoteAgents.clear ();
	_localAgents.clear ();
	_remoteIOHs.clear ();
	_deactivated.clear ();
    }
    
    public Object iterate (Iterator listener, Object ctx){
	_iterating = true;
	for (MuxClient agent : _remoteAgents)
	    ctx = listener.next (agent, ctx);
	for (MuxClient agent : _localAgents)
	    ctx = listener.next (agent, ctx);
	for (MuxClient agent : _remoteIOHs)
	    ctx = listener.next (agent, ctx);
	for (MuxClient agent : _deactivated)
	    ctx = listener.next (agent, ctx);
	_iterating = false;
	return ctx;
    }

    public void add (MuxClient agent, MuxClientState state){ add (agent, state.stopped ());}
    public void add (MuxClient agent, boolean stopped){
	if (stopped){
	    insert (agent, _deactivated);
	} else if (agent.isRemoteIOHEngine ()){
	    insert (agent, _remoteIOHs);
	} else if (agent.isLocalAgent ()){
	    insert (agent, _localAgents);
	} else {
	    insert (agent, _remoteAgents);
	}
    }
    protected void insert (MuxClient agent, List<MuxClient> list){
	try{
	    String agentId = agent.getAgentId ();
	    int size = list.size ();
	    for (int i = 0; i<size; i++){
		MuxClient other = list.get (i);
		if (other.getAgentId ().compareTo (agentId) < 0)
		    continue;
		list.add (i, agent);
		return;
	    }
	    list.add (size, agent);
	}finally{
	    changed ();
	}
    }

    public boolean remove (MuxClient agent){
	boolean removed = false;
	if (agent.isRemoteIOHEngine ()){
	    removed = _remoteIOHs.remove (agent) || _deactivated.remove (agent);
	} else if (agent.isLocalAgent ()){
	    removed = _localAgents.remove (agent) || _deactivated.remove (agent);
	} else {
	    removed = _remoteAgents.remove (agent) || _deactivated.remove (agent);
	}
	if (removed) changed ();
	return removed;
    }
    
    public boolean contains (MuxClient agent){
	if (agent.isRemoteIOHEngine ()){
	    return (_remoteIOHs.contains (agent) || _deactivated.contains (agent));
	} else if (agent.isLocalAgent ()){
	    return (_localAgents.contains (agent) || _deactivated.contains (agent));
	} else {
	    return (_remoteAgents.contains (agent) || _deactivated.contains (agent));
	}
    }
    public boolean isDeactivated (MuxClient agent){
	return _deactivated.contains (agent);
    }
    
    public boolean deactivate (MuxClient agent){
	// the new agent is present but cannot be picked
	boolean moved = false;
	if (agent.isRemoteIOHEngine ()){
	    moved = _remoteIOHs.remove (agent);
	} else if (agent.isLocalAgent ()){
	    moved = _localAgents.remove (agent);
	} else {
	    moved = _remoteAgents.remove (agent);
	}
	if (moved){
	    insert (agent, _deactivated);
	}
	return moved;
    }

    public boolean reactivate (MuxClient agent){
	boolean ok = _deactivated.remove (agent);
	if (!ok) return false;
	add (agent, false);
	return true;
    }

    public MuxClient pick (Object preferenceHint){
	if (_hasRemoteAgent)
	    return pickRemoteAgent (preferenceHint);
	if (_hasRemoteIOH)
	    return pickRemoteIOH (preferenceHint);
	if (_hasLocalAgent)
	    return pickLocalAgent (preferenceHint);
	return null;
    }
    public MuxClient pickRemoteAgent (){
	return pick (_remoteAgents);
    }
    public MuxClient pickRemoteAgent (Object preferenceHint){
	if (preferenceHint == null) return pickRemoteAgent ();
	return pick (preferenceHint.hashCode (), _remoteAgents);
    }
    public MuxClient pickLocalAgent (){
	return pick (_localAgents);
    }
    public MuxClient pickLocalAgent (Object preferenceHint){
	if (preferenceHint == null) return pickLocalAgent ();
	return pick (preferenceHint.hashCode (), _localAgents);
    }
    public MuxClient pickRemoteIOH (){
	return pick (_remoteIOHs);
    }
    public MuxClient pickRemoteIOH (Object preferenceHint){
	if (preferenceHint == null) return pickRemoteIOH ();
	return pick (preferenceHint.hashCode (), _remoteIOHs);
    }
    public MuxClient pick (int rnd, List<MuxClient> list){
	int size = list.size ();
	if (size == 0)
	    return null;
	if (size == 1)
	    return list.get (0);
	return list.get ((rnd & 0X7FFFFFFF)%size);
    }
    public MuxClient pick (List<MuxClient> list){
	int size = list.size ();
	if (size == 0)
	    return null;
	if (size == 1)
	    return list.get (0);
	int rnd = ThreadLocalRandom.current ().nextInt (size);
	return list.get (rnd);
    }
}
