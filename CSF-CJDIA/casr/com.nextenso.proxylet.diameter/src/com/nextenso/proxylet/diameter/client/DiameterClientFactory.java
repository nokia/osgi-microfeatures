package com.nextenso.proxylet.diameter.client;

import java.net.NoRouteToHostException;

import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterRequest;
import com.nextenso.proxylet.diameter.DiameterSession;

/**
 * The factory in charge of instantiating DiameterClients.
 */
public abstract class DiameterClientFactory {

	/**
	 * The System property name indicating the DiameterClientFactory
	 * implementation class.
	 * 
	 * @deprecated
	 */
	@Deprecated
	public static final String DIAMETER_CLIENT_FACTORY_CLASS = "com.nextenso.proxylet.diameter.client.DiameterClientFactory.class";

	/**
	 * The DiameterClientFactory instance singleton.
	 */
	private static volatile DiameterClientFactory _instance;;
	
	public DiameterClientFactory() {}

	/**
	 * Gets the factory instance.
	 * 
	 * @return The unique factory instance.
	 */
	public static synchronized DiameterClientFactory getDiameterClientFactory() {
		DiameterClientFactory factory = _instance;

		if (factory == null) {
			throw new RuntimeException("Diameter Client not yet initialized.");
		}

		return factory;
	}
	
	/**
	 * Sets the unique DiameterClientFactory instance. This method is only meant to be called from the diameter agent implementation.
	 * @param instance The DiameterClientFactory instance
	 */
	public static void setDiameterClientFactory(DiameterClientFactory factory) {
		_instance = factory;
	}

	/**
	 * Creates a new DiameterClient. <br/>
	 * The type is assumed to be DiameterClient.TYPE_ALL.
	 * 
	 * @param destinationHost The destination host, may be <code>null</code> if
	 *          unknown.
	 * @param destinationRealm The destination realm.
	 * @param vendorId The application vendorId.
	 * @param applicationId The application identifier.
	 * @param stateful true if the client is stateful, false if stateless - if the
	 *          client is stateless, no DiameterSession is associated to the
	 *          DiameterClient.
	 * @param sessionLifetime The session lifetime in seconds, ignored if the
	 *          client is stateless.
	 * @exception NoRouteToHostException if no route is defined for the specified
	 *              (destinationHost, destinationRealm, applicationId).
	 */
	public abstract DiameterClient newDiameterClient(String destinationHost, String destinationRealm, long vendorId, long applicationId,
			boolean stateful, int sessionLifetime)
		throws NoRouteToHostException;

	/**
	 * Creates a new DiameterClient for a given local peer. <br/>
	 * The type is assumed to be DiameterClient.TYPE_ALL.
	 * 
	 * @param localPeer The local peer.
	 * @param destinationHost The destination host, may be <code>null</code> if
	 *          unknown
	 * @param destinationRealm The destination realm.
	 * @param vendorId The application vendorId.
	 * @param applicationId The application identifier.
	 * @param stateful true if the client is stateful, false if stateless - if the
	 *          client is stateless, no DiameterSession is associated to the
	 *          DiameterClient.
	 * @param sessionLifetime The session lifetime in seconds, ignored if the
	 *          client is stateless.
	 * @exception NoRouteToHostException if no route is defined for the specified
	 *              (destinationHost, destinationRealm, applicationId).
	 */
	public abstract DiameterClient newDiameterClient(DiameterPeer localPeer, String destinationHost, String destinationRealm, long vendorId,
			long applicationId, boolean stateful, int sessionLifetime)
		throws NoRouteToHostException;

	public abstract DiameterClient newDiameterClient(String destinationHost, String destinationRealm, long vendorId, long applicationId,
			int clientType, boolean stateful, int sessionLifetime)
		throws NoRouteToHostException;

	/**
	 * Creates a new DiameterClient.
	 * 
	 * @param destinationHost The destination host, may be <code>null</code> if
	 *          unknown.
	 * @param destinationRealm The destination realm.
	 * @param vendorId The application vendorId.
	 * @param applicationId The application identifier.
	 * @param clientType The type of the client (DiameterClient.TYPE_ACCT,
	 *          DiameterClient.TYPE_AUTH, DiameterClient.TYPE_ALL).
	 * @param sessionId The session identifier.
	 * @param sessionLifetime The session lifetime in seconds, ignored if the
	 *          client is no session identifier.
	 * @exception NoRouteToHostException if no route is defined for the specified
	 *              (destinationHost, destinationRealm, applicationId).
	 */
	public abstract DiameterClient newDiameterClient(String destinationHost, String destinationRealm, long vendorId, long applicationId,
			int clientType, String sessionId, int sessionLifetime)
		throws NoRouteToHostException;

	/**
	 * Creates a new DiameterClient for a given local peer.
	 * 
	 * @param localPeer The local peer.
	 * @param destinationHost The destination host, may be <code>null</code> if
	 *          unknown.
	 * @param destinationRealm The destination realm.
	 * @param vendorId The application vendorId.
	 * @param applicationId the application identifier.
	 * @param clientType The type of the client (DiameterClient.TYPE_ACCT,
	 *          DiameterClient.TYPE_AUTH, DiameterClient.TYPE_ALL).
	 * @param stateful true if the client is stateful, false if stateless - if the
	 *          client is stateless, no DiameterSession is associated to the
	 *          DiameterClient.
	 * @param sessionLifetime The session lifetime in seconds, ignored if the
	 *          client is stateless.
	 * @exception NoRouteToHostException if no route is defined for the specified
	 *              (destinationHost, destinationRealm, applicationId).
	 */
	public abstract DiameterClient newDiameterClient(DiameterPeer localPeer, String destinationHost, String destinationRealm, long vendorId,
			long applicationId, int clientType, boolean stateful, int sessionLifetime)
		throws NoRouteToHostException;

	/**
	 * Creates a new DiameterClient. <br/>
	 * The DiameterSession is provided and the destination should be the client or
	 * the server associated to the Session. <br/>
	 * This is the constructor to use when acting as a server sending a request to
	 * a client.
	 * 
	 * @param destinationHost The destination host, may be <code>null</code> if
	 *          unknown.
	 * @param destinationRealm The destination realm.
	 * @param session The Diameter session.
	 * @exception NoRouteToHostException if no route is defined for the specified
	 *              (destinationHost, destinationRealm, applicationId).
	 */
	public abstract DiameterClient newDiameterClient(String destinationHost, String destinationRealm, DiameterSession session)
		throws NoRouteToHostException;

	/**
	 * Creates a new DiameterClient with a request. <br/>
	 * 
	 * It uses the session, the origin-host and origin-realm of the request to
	 * create a client. If the request is not statefull, a client cannot be
	 * created and then, this method return null.
	 * 
	 * @param request The request.
	 * @return the client or null if the request does not have any session.
	 * @throws NoRouteToHostException
	 */
	public abstract DiameterClient newDiameterClient(DiameterRequest request)
		throws NoRouteToHostException;

}
