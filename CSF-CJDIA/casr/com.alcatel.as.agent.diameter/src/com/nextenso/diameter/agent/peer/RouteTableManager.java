package com.nextenso.diameter.agent.peer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.log4j.Logger;

import com.nextenso.diameter.agent.DiameterProperties;
import com.nextenso.diameter.agent.Utils;
import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterRoute;
import com.nextenso.proxylet.diameter.DiameterRouteTable;

import alcatel.tess.hometop.gateways.utils.ConfigException;

@Component(provides=DiameterRouteTable.class)
public class RouteTableManager
    extends DiameterRouteTable {

    private static final Logger LOGGER = Logger.getLogger("agent.diameter.routetablemanager");
    private static Map<String, RouteTable> ROUTE_TABLES = Utils.newConcurrentHashMap();
    private static Map<String, List<DiameterPeer>> ALIASES = Utils.newConcurrentHashMap();

    private ReentrantReadWriteLock _lock = new ReentrantReadWriteLock ();
    private Lock _readLock = _lock.readLock ();
    private Lock _writeLock = _lock.writeLock ();
    
    public RouteTableManager() {}
    
    @Start
    public void start() {
    	RouteTableManager.setDiameterRouteTable(this);
    }

    public static RouteTableManager getInstance() {
	return (RouteTableManager) DiameterRouteTable.getInstance();
    }

    /**
     * Initializes an handler connection.
     * 
     * @param handlerName The handler connection
     * @throws ConfigException if an error occurs.
     */
    public void muxOpened(String handlerName)
	throws ConfigException {
	RouteTable routes = new RouteTable(handlerName);
	try{
	    _writeLock.lock ();
	    ROUTE_TABLES.put(handlerName, routes);
	    routes.init();
	}finally{
	    _writeLock.unlock ();
	}
    }

    public void destroy(String handlerName) {
	try{
	    _writeLock.lock ();
	    RouteTable table = ROUTE_TABLES.remove(handlerName);
	    if (table != null) {
		table.clear();
	    }
	}finally{
	    _writeLock.unlock ();
	}
    }

    public void updateRoutes(String xml) {
	try{
	    _writeLock.lock ();
	    for (RouteTable routeTable : ROUTE_TABLES.values()) {
		try {
		    routeTable.init(xml);
		}
		catch (ConfigException e) {
		    LOGGER.error("Cannot read the route xml content", e);
		}
	    }
	}finally{
	    _writeLock.unlock ();
	}
    }

    /**
     * Gets the route table associated to an handler.
     * 
     * @param handlerName The handler name.
     * @return The route table.
     */
    /** IT IS NOT thread safe: must be called from a locked section */
    private RouteTable getRouteTable(String handlerName) {
	return handlerName != null ? ROUTE_TABLES.get(handlerName) : null;
    }

    /**
     * @see com.nextenso.proxylet.diameter.DiameterRouteTable#addDiameterRoute(com.nextenso.proxylet.diameter.DiameterPeer,
     *      java.lang.String, long, int)
     */
    @Override
    public DiameterRoute addDiameterRoute(DiameterPeer routingPeer, String destRealm, long applicationId, int applicationType) {
	return addDiameterRoute(routingPeer, destRealm, applicationId, applicationType, -1);
    }

    /**
     * @see com.nextenso.proxylet.diameter.DiameterRouteTable#addDiameterRoute(com.nextenso.proxylet.diameter.DiameterPeer,
     *      java.lang.String, long, int, int)
     */
    @Override
    public DiameterRoute addDiameterRoute(DiameterPeer routingPeer, String destRealm, long applicationId, int applicationType, int metrics) {
	if (routingPeer == null) {
	    throw new IllegalArgumentException("routingPeer is null");
	}
	DiameterPeer localPeer = routingPeer.getLocalDiameterPeer();
	if (localPeer == null) {
	    throw new IllegalArgumentException("routingPeer does not have local peer");
	}

	Route route = new Route(routingPeer, destRealm, applicationId, applicationType, metrics);
	String handlerName = getHandlerName(localPeer);
	try{
	    _writeLock.lock ();
	    RouteTable table = getRouteTable(handlerName);
	    table.addRoute(route);
	    return route;
	}finally{
	    _writeLock.unlock ();
	}
    }

    /**
     * @see com.nextenso.proxylet.diameter.DiameterRouteTable#addRoutingPeer(com.nextenso.proxylet.diameter.DiameterPeer,
     *      java.lang.String)
     */
    @Override
    public void addRoutingPeer(DiameterPeer routingPeer, String destHost) {
	try{
	    _writeLock.lock ();
	    List<DiameterPeer> list = ALIASES.get(destHost);
	    if (list == null) {
		list = new CopyOnWriteArrayList<DiameterPeer>();
		ALIASES.put(destHost, list);
	    }
	    list.add(routingPeer);
	}finally{
	    _writeLock.unlock ();
	}
    }

    /**
     * @see com.nextenso.proxylet.diameter.DiameterRouteTable#getDiameterRoutes(com.nextenso.proxylet.diameter.DiameterPeer,
     *      java.lang.String, long, int)
     */
    @Override
    public List<DiameterRoute> getDiameterRoutes(DiameterPeer localPeer, String destinationRealm, long applicationId, int applicationType) {
	if (localPeer != null) {
	    String handlerName = getHandlerName(localPeer);
	    try{
		_readLock.lock ();
		RouteTable table = getRouteTable(handlerName);
		return table.getRoutes(destinationRealm, applicationId, applicationType);
	    }finally{
		_readLock.unlock ();
	    }
	} else {
	    return getDiameterRoutes(destinationRealm, applicationId, applicationType);
	}
    }

    /**
     * @see com.nextenso.proxylet.diameter.DiameterRouteTable#getDiameterRoutes(java.lang.String,
     *      long, int)
     */
    @Override
    public List<DiameterRoute> getDiameterRoutes(String destinationRealm, long applicationId, int applicationType) {
	List<DiameterRoute> res = new ArrayList<DiameterRoute>();
	try{
	    _readLock.lock ();
	    for (RouteTable table : ROUTE_TABLES.values()) {
		res.addAll(table.getRoutes(destinationRealm, applicationId, applicationType));
	    }
	    return res;
	}finally{
	    _readLock.unlock ();
	}
    }

    /**
     * @see com.nextenso.proxylet.diameter.DiameterRouteTable#getRoutingPeers(com.nextenso.proxylet.diameter.DiameterPeer,
     *      java.lang.String)
     */
    @Override
    public List<DiameterPeer> getRoutingPeers(DiameterPeer localPeer, String destHost) {
	List<DiameterPeer> res = getRoutingPeers(destHost);
	for (Iterator<DiameterPeer> it = res.iterator(); it.hasNext();) {
	    DiameterPeer peer = it.next();
	    if (peer.getLocalDiameterPeer() != localPeer) {
		it.remove();
	    }
	}

	return res;
    }

    /**
     * @see com.nextenso.proxylet.diameter.DiameterRouteTable#getRoutingPeers(java.lang.String)
     */
    @Override
    public List<DiameterPeer> getRoutingPeers(String destinationHost) {
	List<DiameterPeer> res = new ArrayList<DiameterPeer>();
	try{
	    _readLock.lock ();
	    List<DiameterPeer> list = ALIASES.get(destinationHost);
	    if (list != null) {
		res.addAll(list);
	    }
	    return res;
	}finally{
	    _readLock.unlock ();
	}
    }

    /**
     * @see com.nextenso.proxylet.diameter.DiameterRouteTable#removeDiameterRoute(com.nextenso.proxylet.diameter.DiameterRoute)
     */
    @Override
    public void removeDiameterRoute(DiameterRoute route) {
	if (route == null) {
	    return;
	}
	DiameterPeer localPeer = route.getRoutingPeer().getLocalDiameterPeer();
	String handlerName = getHandlerName(localPeer);
	try{
	    _writeLock.lock ();
	    RouteTable table = getRouteTable(handlerName);
	    table.removeRoute(route);
	}finally{
	    _writeLock.unlock ();
	}
    }

    /**
     * @see com.nextenso.proxylet.diameter.DiameterRouteTable#removeRoutingPeer(com.nextenso.proxylet.diameter.DiameterPeer,
     *      java.lang.String)
     */
    @Override
    public void removeRoutingPeer(DiameterPeer routingPeer, String destHost) {
	try{
	    _writeLock.lock ();
	    List<DiameterPeer> list = ALIASES.get(destHost);
	    if (list != null) {
		list.remove(routingPeer);
		if (list.size () == 0) ALIASES.remove (destHost);
	    }
	}finally{
	    _writeLock.unlock ();
	}
    }

    private String getHandlerName(DiameterPeer localPeer) {
	return ((LocalPeer) localPeer).getHandlerName();
    }

    // USED BY DDE application directly !!! via java reflection --> must keep it for now
    public DiameterPeer getDestinationPeer(DiameterPeer localPeer, String destHost, String destRealm, long appId, int type) {
	return getDestinationPeer (localPeer, destHost, destRealm, appId, type,
				   // we rely on both props
				   DiameterProperties.doAbsoluteProxyRouting () && DiameterProperties.doAbsoluteClientRouting ());
    }
    public DiameterPeer getDestinationPeer(DiameterPeer localPeer, String destHost, String destRealm, long appId, int type, boolean absolute) {
	try{
	    _readLock.lock ();
	    if (destHost != null) {
		if (LOGGER.isDebugEnabled()) {
		    LOGGER.debug("getDestinationPeer: trying to find a Static peer with this defined Destination-Host=" + destHost);
		}
		DiameterPeer dest = Utils.getTableManager().getDiameterPeer(localPeer, destHost);
		if (dest != null && dest.isConnected() && !dest.isQuarantined()) {
		    return dest;
		}
		if (LOGGER.isDebugEnabled()) {
		    LOGGER.debug("getDestinationPeer: no connected peer with defined Destination-Host, searching in aliases...");
		}

		List<DiameterPeer> peers = getRoutingPeers(localPeer, destHost);
		if (!peers.isEmpty()) {
		    for (DiameterPeer peer : peers) {
			if (peer.isConnected() && !peer.isQuarantined()) {
			    return peer;
			}
		    }
		}
	    }

	    if (LOGGER.isDebugEnabled()) {
		LOGGER.debug("getDestinationPeer: no Destination-Host or no peer is found -> use routes");
	    }

	    String handlerName = getHandlerName(localPeer);
	    RouteTable localTable = getRouteTable(handlerName);
	    DiameterPeer peer = null;
	    int bestScore = -1;
	    int metrics = Integer.MAX_VALUE;
	    if (absolute){
		// new method : check all local peers for best absolute
		for (RouteTable table : ROUTE_TABLES.values ()){
		    Route route = table.getRoute (destRealm, appId, type);
		    if (route != null){
			int score = route.score (destRealm, appId, type);
			if (score > bestScore){
			    bestScore = score;
			    metrics = route.getMetrics ();
			    peer = route.getRoutingPeer ();
			} else if (score == bestScore){
			    int thisMetrics = route.getMetrics ();
			    if (thisMetrics < metrics){
				metrics = thisMetrics;
				peer = route.getRoutingPeer ();
			    } else if (thisMetrics == metrics){
				// privilege localPeer
				if (table == localTable){
				    peer = route.getRoutingPeer ();
				}
			    }
			}
		    }
		}
	    } else {
		// legacy method : use only the local peer
		Route route = localTable.getRoute (destRealm, appId, type);
		peer = route != null ? route.getRoutingPeer () : null;
	    }
	    return peer;
	}finally{
	    _readLock.unlock ();
	}
    }

    public void removePeer(RemotePeer sPeer) {
	String handlerName = sPeer.getHandlerName();
	try{
	    _writeLock.lock ();
	    // remove the tables
	    RouteTable routeTable = ROUTE_TABLES.get(handlerName);
	    if (routeTable != null) routeTable.removePeer(sPeer);

	    // remove the aliased peers
	    List<String> aliasesToRemove = new ArrayList<String>();
	    for (Entry<String, List<DiameterPeer>> entry : ALIASES.entrySet()) {
		List<DiameterPeer> list = entry.getValue();
		list.remove(sPeer);
		if (list.isEmpty()) {
		    aliasesToRemove.add(entry.getKey());
		}
	    }

	    // remove the empty lists
	    for (String alias : aliasesToRemove) {
		ALIASES.remove(alias);
	    }
	}finally{
	    _writeLock.unlock ();
	}
    }
}
