package com.nextenso.proxylet.diameter;

import java.util.List;

/**
 * 
 * The Diameter Route Table.
 */
public abstract class DiameterRouteTable {
	
	/**
	 * Unique DiameterRouteTable (injected by diameter container implementation).
	 */
	private volatile static DiameterRouteTable _instance;

	/**
	 * Gets the instance.
	 * 
	 * @return The table or null if not found.
	 */
	public static DiameterRouteTable getInstance() {
		if (_instance == null) {
			throw new RuntimeException("Diameter route table not yet initialized.");
		}
		return _instance;
	}
	
	/**
	 * Sets the unique DiameterRouteTable instance. This method is only meant to be called from the diameter agent implementation.
	 * @param instance The DiameterRouteTable instance
	 */
	public static void setDiameterRouteTable(DiameterRouteTable instance) {
		_instance = instance;
	}

	/**
	 * Gets the list of matching routes for these arguments.
	 * 
	 * @param destinationRealm The destination realm.
	 * @param applicationId The application identifier.
	 * @param applicationType The application identifier:
	 *          DiameterClient.TYPE_ACCT, DiameterClient.TYPE_AUTH,
	 *          DiameterClient.TYPE_ALL
	 * @return The list of matching routes. It may be empty if no route matches.
	 */
	public abstract List<DiameterRoute> getDiameterRoutes(String destinationRealm, long applicationId, int applicationType);

	/**
	 * Gets the list of matching routes for these arguments.
	 * 
	 * @param localPeer The local peer.
	 * @param destinationRealm The destination realm.
	 * @param applicationId The application identifier.
	 * @param applicationType The application identifier:
	 *          DiameterClient.TYPE_ACCT, DiameterClient.TYPE_AUTH,
	 *          DiameterClient.TYPE_ALL
	 * @return The list of matching routes. It may be empty if no route matches.
	 */
	public abstract List<DiameterRoute> getDiameterRoutes(DiameterPeer localPeer, String destinationRealm, long applicationId, int applicationType);

	/**
	 * Gets the routing peers for a destination host.
	 * 
	 * @param destinationHost The destination host.
	 * @return The routing peers. It may be empty if no routing peer matches.
	 */
	public abstract List<DiameterPeer> getRoutingPeers(String destinationHost);

	/**
	 * Gets the routing peers for a destination host for a local peer.
	 * 
	 * @param localPeer The local peer.
	 * @param destinationHost The destination host.
	 * @return The routing peers. It may be empty if no routing peer matches.
	 */
	public abstract List<DiameterPeer> getRoutingPeers(DiameterPeer localPeer, String destinationHost);

	/**
	 * Adds a new route.
	 * 
	 * @param routingPeer The routing peer (it cannot be null).
	 * @param destRealm The destination realm.
	 * @param applicationId The application identifier.
	 * @param applicationType DiameterClient.TYPE_ACCT, DiameterClient.TYPE_AUTH,
	 *          DiameterClient.TYPE_ALL
	 */
	public abstract DiameterRoute addDiameterRoute(DiameterPeer routingPeer, String destRealm, long applicationId, int applicationType);

	/**
	 * Adds a new route.
	 * 
	 * @param routingPeer The routing peer (it cannot be null).
	 * @param destRealm The destination realm.
	 * @param applicationId The application identifier.
	 * @param applicationType DiameterClient.TYPE_ACCT, DiameterClient.TYPE_AUTH,
	 *          DiameterClient.TYPE_ALL
	 * @param metrics The route metrics.
	 */
	public abstract DiameterRoute addDiameterRoute(DiameterPeer routingPeer, String destRealm, long applicationId, int applicationType, int metrics);

	/**
	 * Removes the route.
	 * 
	 * @param route The route to be removed.
	 */
	public abstract void removeDiameterRoute(DiameterRoute route);

	/**
	 * Adds a routing peer associated to a destination host. <BR>
	 * This method allows applications to creation "alias" route for a destination
	 * host.
	 * 
	 * @param routingPeer The routing peer.
	 * @param destHost The destination host.
	 */
	public abstract void addRoutingPeer(DiameterPeer routingPeer, String destHost);

	/**
	 * Removes a routing peer associated to a destination host.
	 * 
	 * @param routingPeer The routing peer.
	 * @param destHost The destination host.
	 */
	public abstract void removeRoutingPeer(DiameterPeer routingPeer, String destHost);

}
