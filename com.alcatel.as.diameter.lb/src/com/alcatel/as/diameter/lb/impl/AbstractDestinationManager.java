package com.alcatel.as.diameter.lb.impl;

import com.alcatel.as.ioh.client.TcpClient.Destination;
import com.alcatel.as.diameter.lb.DestinationManager;

import java.util.*;
import com.alcatel.as.ioh.tools.KeyMap;

public abstract class AbstractDestinationManager implements DestinationManager {

    private static final Comparator<Destination> COMPARATOR = new Comparator<Destination> (){
	    public int compare (Destination d1, Destination d2){
		String name1 = d1.getProperties().get (com.alcatel.as.ioh.server.TcpServer.PROP_SERVER_NAME).toString ();
		String name2 = d2.getProperties().get (com.alcatel.as.ioh.server.TcpServer.PROP_SERVER_NAME).toString ();
		return name1.compareTo (name2);
	    }
	};

    //protected Map<Integer, Destination> _destinationsMap = new HashMap<> ();
    //protected KeyMap _map = new KeyMap (1023);
    protected String _group;

    protected ArrayList<Destination> _destinations = new ArrayList<> ();

    protected AbstractDestinationManager (String group){ _group = group;}
    
    public Destination get (int hash){
	/**
	return _destinationsMap.get (_map.getKey (hash));
	*/
	return _destinations.get (DestinationManagerMap.rehash (hash * 2) % _destinations.size ()); // *2 is needed else there is a congruence issue if we re-use the same hash than DestinationManagerMap
    }

    public void add (Destination dest, int weight){
	/**
	int key = makeKey (dest);
	_destinationsMap.put (key, dest);
	_map.addKey (key);
	*/
	_destinations.add (dest);
	_destinations.sort (COMPARATOR);
    }

    public abstract void update (Destination dest, int weight);

    public boolean remove (Destination dest){
	/**
	int key = makeKey (dest);
	_map.removeKey (key);
	return _destinationsMap.remove (key) != null;
	*/
	return _destinations.remove (dest);
    }

    public abstract void available (Destination dest, int weight);
    
    public abstract void unavailable (Destination dest);

    public void clear (){
	/**
	_destinationsMap.clear ();
	_map.reset ();
	*/
	_destinations.clear ();
    }

    protected int makeKey (Destination destination){
	return destination.hashCode () & 0x7FFFFFFF; // avoid -1;
    }
}
