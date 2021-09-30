package com.alcatel.as.diameter.lb.impl;

import java.util.function.*;
import java.util.*;

import com.alcatel.as.diameter.lb.UserLocator;
import com.alcatel.as.diameter.lb.DestinationManager;
import com.alcatel.as.ioh.client.TcpClient.Destination;
import alcatel.tess.hometop.gateways.reactor.*;

public class DestinationManagerMap {

    private Map<String, AbstractDestinationManager> _destManagers = new HashMap <> ();
    private Function<String, AbstractDestinationManager> _computeIfAbsentFunction = s -> new SimpleDestinationManager (s);
    private AbstractDestinationManager _defManager = null;
    private ArrayList<String> _groups = new ArrayList<String> ();

    public DestinationManagerMap (Function<String, AbstractDestinationManager> destinationManagerFactory){
	_computeIfAbsentFunction = destinationManagerFactory;
	_defManager = _computeIfAbsentFunction.apply (null);
	_destManagers.put (UserLocator.DEF_GROUP, _defManager);
    }

    public void clear (){
	_destManagers.forEach ((key, map) -> {map.clear ();});
	_destManagers.clear ();
	_groups.clear ();
    }

    public AbstractDestinationManager get (String group){
	if (group == null) return _defManager;
	return _destManagers.get (group);
    }
    public AbstractDestinationManager getCreate (String group){
	if (group == null) return _defManager;
	AbstractDestinationManager mgr =  _destManagers.computeIfAbsent (group, _computeIfAbsentFunction);
	if (_groups.indexOf (group) == -1){
	    _groups.add (group);
	    _groups.sort (null);
	}
	return mgr;
    }

    public AbstractDestinationManager get (int hash){
	if (hash == -1) return _defManager;
	int size = _groups.size ();
	if (size == 0) return _defManager;
	return get (_groups.get (rehash (hash) % size));
    }

    /**
     * Applies a supplemental hash function to a given msg identifier, which
     * defends against poor quality hash functions.  This is critical in order to
     * fairly distribute messages amongs agents, especially when msg identifiers only differs
     * by a small constant.
     */
    public static int rehash(int h) {
	// Ensures the number is unsigned.
	h = (h & 0X7FFFFFFF);
	//h += RND[h % RND.length];
	
	// This function ensures that hashCodes that differ only by
	// constant multiples at each bit position have a bounded
	// number of collisions (approximately 8 at default load factor).
	h ^= (h >>> 20) ^ (h >>> 12);
	return (h ^ (h >>> 7) ^ (h >>> 4)) & 0x7FFFFFFF;
    }
}
