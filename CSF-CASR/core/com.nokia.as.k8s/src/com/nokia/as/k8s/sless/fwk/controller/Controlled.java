package com.nokia.as.k8s.sless.fwk.controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.nokia.as.k8s.sless.fwk.FunctionResource;
import com.nokia.as.k8s.sless.fwk.RouteResource;

public abstract class Controlled {

    protected final static Logger LOGGER = Logger.getLogger("sless.controller.rt");
    protected final static AtomicLong GOGO_ID = new AtomicLong (0L);
    
    private java.util.Map<Long, CompletableFuture<String>> _gogoCommands = new ConcurrentHashMap<> ();
    private String _type;
    private String _id;

    public Controlled (String id, String protocol){
	_id = id;
	if (protocol.startsWith ("sless."))
	    _type = protocol.substring ("sless.".length ());
	else // not expected though
	    _type = protocol;
    }

    public String toString (){
	return "Controlled["+_id+"]";
    }

    public String id (){ return _id;}

    public String type (){ return _type;}
    
    public Controlled start (){
	return this;
    }

    public void stop (){
	for (CompletableFuture cf : _gogoCommands.values ())
	    cf.complete (null);
	_gogoCommands.clear ();
    }

    protected abstract void push (RouteResource route, FunctionResource function);
    protected abstract void unpush (RouteResource route);
    
    public CompletableFuture<String> gogoRequest (String request){
	long id = GOGO_ID.getAndIncrement ();
	if (LOGGER.isInfoEnabled ()) LOGGER.info (this+" : gogo-req : id="+id+" : "+request);
	CompletableFuture<String> cf = new CompletableFuture<> ();
	_gogoCommands.put (id, cf);
	return gogoRequest (id, request, cf);
    }
    // to be overridden
    protected abstract CompletableFuture<String> gogoRequest (long id, String request, CompletableFuture<String> cf);

    public void gogoResponse (long id, String resp){
	if (LOGGER.isInfoEnabled ()) LOGGER.info (this+" : gogo-resp : id="+id+" : "+resp);
	CompletableFuture<String> cf = _gogoCommands.remove (id);
	if (cf != null) cf.complete (resp);
	return;
    }

    
    public boolean match (RouteResource route){
	if (!type ().equals (route.route.type)) return false;
	List<String> rtNames = route.runtimes().stream().map(r -> r.name).collect(Collectors.toList());
	return rtNames.isEmpty() || rtNames.contains(id ());
    }

    
}
