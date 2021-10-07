package com.nextenso.diameter.agent.impl;

import java.net.NoRouteToHostException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.alcatel.as.util.config.ConfigHelper;
import com.nextenso.diameter.agent.PropertiesDeclaration;
import com.nextenso.diameter.agent.Utils;
import com.nextenso.diameter.agent.peer.Peer;
import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterRequest;
import com.nextenso.proxylet.diameter.DiameterSession;
import com.nextenso.proxylet.diameter.client.DiameterClient;
import com.nextenso.proxylet.diameter.client.DiameterClientFactory;

/**
 * The DiameterClientFactory Implementation.
 */
@Component(provides={}, factoryMethod="getInstance")
public class DiameterClientFactoryFacade extends DiameterClientFactory {

	private static final Logger LOGGER = Logger.getLogger("agent.diameter.client");

	@Inject
	private BundleContext _bctx;
	
	/**
	 * Our single instance.
	 */
	private final static DiameterClientFactoryFacade _singleton = new DiameterClientFactoryFacade();

	/**
	 * Flag used to make sure we don't register our service twice.
	 */
	private final AtomicBoolean _registering = new AtomicBoolean(false);

	/**
	 * Our service registration
	 */
	private ServiceRegistration<?> _registration;
	
	/**
	 * Call this runnable once the service is registered
	 */
	Runnable _registrationListener;
	
	public static DiameterClientFactoryFacade getInstance() {
		return _singleton;
	}

	@ConfigurationDependency(pid="diameteragent")
	public void setAgentConfig(Dictionary<String, String> cnf) {
		try {
			if (ConfigHelper.getBoolean (cnf, PropertiesDeclaration.CLIENT_FACTORY_IMMEDIATE, false)){
				registerService(_bctx);
			}
		} catch (Exception e) {
			LOGGER.error("Can't register DiameterClientFactory service", e);
		}
	}
	
	/**
	 * Register the DiameterClientFactory in the OSGI registry and also set the implementation in the API
	 */
	public void registerService(BundleContext bctx) {
		if (_registering.compareAndSet(false, true)) {
			LOGGER.debug("registering diameter client factory");
			Hashtable<String, Object> props = new Hashtable<>();
			props.put("client.id", "diameter");
			ServiceRegistration<?> registration = bctx.registerService(DiameterClientFactory.class.getName(), getInstance(), props);
			DiameterClientFactory.setDiameterClientFactory(getInstance());
			Runnable registrationListener;
			synchronized (this) {
				_registration = registration;
				registrationListener = _registrationListener;
			}
			if (registrationListener != null) {
				registrationListener.run();
			}
		}
	}
	
	public void setRegistrationListener(Runnable registrationListener) {
		boolean registered = false;
		synchronized (this) {
			if (_registration != null) {
				registered = true;
			} else {
				_registrationListener = registrationListener;
			}
		}
		if (registered) {
			registrationListener.run();
		}
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClientFactory#newDiameterClient(java.lang.String,
	 *      java.lang.String, long, long, boolean, int)
	 */
	@Override
	public DiameterClient newDiameterClient(String destinationHost, String destinationRealm, long vendorId, long applicationId, boolean stateful,
			int sessionLifetime)
		throws java.net.NoRouteToHostException {
		return newDiameterClient(destinationHost, destinationRealm, vendorId, applicationId, DiameterClient.TYPE_ALL, stateful, sessionLifetime);
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClientFactory#newDiameterClient(com.nextenso.proxylet.diameter.DiameterPeer,
	 *      java.lang.String, java.lang.String, long, long, boolean, int)
	 */
	@Override
	public DiameterClient newDiameterClient(DiameterPeer localPeer, String destinationHost, String destinationRealm, long vendorId, long applicationId,
			boolean stateful, int sessionLifetime)
		throws NoRouteToHostException {
		return newDiameterClient(localPeer, destinationHost, destinationRealm, vendorId, applicationId, DiameterClient.TYPE_ALL, stateful, sessionLifetime);
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClientFactory#newDiameterClient(java.lang.String,
	 *      java.lang.String, long, long, int, boolean, int)
	 */
	@Override
	public DiameterClient newDiameterClient(String destinationHost, String destinationRealm, long vendorId, long applicationId, int type,
			boolean stateful, int sessionLifetime)
		throws java.net.NoRouteToHostException {
		String handlerName = Utils.getNextHandlerName();
		return new DiameterClientFacade(handlerName, destinationHost, destinationRealm, vendorId, applicationId, type, stateful, sessionLifetime * 1000L);
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClientFactory#newDiameterClient(com.nextenso.proxylet.diameter.DiameterPeer,
	 *      java.lang.String, java.lang.String, long, long, int, boolean, int)
	 */
	@Override
	public DiameterClient newDiameterClient(DiameterPeer localPeer, String destinationHost, String destinationRealm, long vendorId, long applicationId,
			int clientType, boolean stateful, int sessionLifetime)
		throws NoRouteToHostException {
		if (localPeer == null) {
			throw new IllegalArgumentException("null local peer not supported");
		}
		String handlerName = ((Peer) localPeer).getHandlerName();
		return new DiameterClientFacade(handlerName, destinationHost, destinationRealm, vendorId, applicationId, clientType, stateful, sessionLifetime * 1000L);
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClientFactory#newDiameterClient(java.lang.String,
	 *      java.lang.String, com.nextenso.proxylet.diameter.DiameterSession)
	 */
	@Override
	public DiameterClient newDiameterClient(String destinationHost, String destinationRealm, DiameterSession session)
		throws java.net.NoRouteToHostException {
		String handlerName = Utils.getNextHandlerName();

		return new DiameterClientFacade(handlerName, destinationHost, destinationRealm, session);
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClientFactory#newDiameterClient(java.lang.String,
	 *      java.lang.String, long, long, int, java.lang.String, int)
	 */
	@Override
	public DiameterClient newDiameterClient(String destinationHost, String destinationRealm, long vendorId, long applicationId, int type,
			String sessionId, int sessionLifetime)
		throws NoRouteToHostException {
		String handlerName = Utils.getNextHandlerName();
		return new DiameterClientFacade(handlerName, destinationHost, destinationRealm, vendorId, applicationId, type, sessionId, sessionLifetime * 1000L);
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClientFactory#newDiameterClient(com.nextenso.proxylet.diameter.DiameterRequest)
	 */
	@Override
	public DiameterClient newDiameterClient(DiameterRequest request)
		throws NoRouteToHostException {
		return new DiameterClientFacade(request);
	}

}
