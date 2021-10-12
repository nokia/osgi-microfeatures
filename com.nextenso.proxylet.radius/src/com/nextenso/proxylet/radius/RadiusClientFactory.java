// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.radius;

/**
 * This class should be used to create a new instance of RadiusClient.
 * This class is an OSGi service (use DS or DM to get an instance on it):
 * <pre>
 * @Reference
 * void bindRadiusClientFactory(RadiusClientFactory clientFactory) { ... }
 * </pre>
 */
public abstract class RadiusClientFactory {
	
	/**
	 * The RadiusClientFactory instance singleton.
	 */
	private static volatile RadiusClientFactory _instance;
		
	/**
	 * Sets the unique RadiusClientFactory instance. This method is only meant to be called from the radius agent implementation.
	 * @param instance The RadiusClientFactory instance
	 */
	public static void setRadiusClientFactory(RadiusClientFactory factory) {
		_instance = factory;
	}

	/**
	 * The System property name indicating the RadiusClient implementation
	 * 
	 * @deprecated this property is not used anymore (SPI META-INF/services are
	 *             used instead)
	 */
	@Deprecated
	public static final String RADIUS_CLIENT_CLASS = "com.nextenso.proxylet.radius.RadiusClient.class";

	/**
	 * Returns a new instance of the current RadiusClient implementation. <br/>
	 * The server parameter indicates the Radius server to connect to. This
	 * parameter should be in the format "host:port" or "host". If the port is not
	 * specified, the default radius ports are assumed (1812 and 1813). <br/>
	 * The secret parameter may be null if the Agent configuration specifies the
	 * secret for that host.
	 * 
	 * @param server The radius server to connect to, in the format host[:port].
	 * @param secret The radius secret to use, may be null (see above).
	 * @return A RadiusClient for the specified host.
	 */
	public static RadiusClient newRadiusClient(String server, byte[] secret) {
		return getInstance().create(server, secret);
	}

	/**
	 * Returns a new instance of the current RadiusClient implementation. <br/>
	 * 
	 * The server parameter indicates the Radius server to connect to. This
	 * parameter should be in the format "host:port" or "host". If the port is not
	 * specified, the default radius ports are assumed (1812 and 1813). <br/>
	 * 
	 * The secret parameter may be null if the Agent configuration specifies the
	 * secret for that host. The returned client checks license validity on output
	 * flow. That is, a proxylet using the returned client must have a valid
	 * license to be able to send request.
	 * 
	 * @param server The radius server to connect to, in the format host[:port].
	 * @param secret The radius secret to use, may be null (see above).
	 * @param callerId The id of the component using the RadiusClient.
	 * @return A RadiusClient for the specified host.
	 * @internal
	 */
	public static RadiusClient newRadiusClient(String server, byte[] secret, String callerId) {
		return getInstance().create(server, secret, callerId);
	}

	/**
	 * Gets an instance of this factory.
	 * 
	 * @return The factory instance.
	 * @exception RuntimeException if the service is not available.
	 */
	protected static RadiusClientFactory getInstance() {
		RadiusClientFactory factory = _instance;

		if (factory == null) {
			throw new RuntimeException("Radius Client Factory service not available.");
		}

		return factory;
	}

	/**
	 * Creates a new client.
	 * 
	 * @param server The radius server to connect to, in the format host[:port].
	 * @param secret The radius secret to use, may be null (see above).
	 * @param callerId The id of the component using the RadiusClient.
	 * @return The client.
	 */
	protected abstract RadiusClient create(String server, byte[] secret, String callerId);

	/**
	 * Creates a new client.
	 * 
	 * @param server The radius server to connect to, in the format host[:port].
	 * @param secret The radius secret to use, may be null (see above).
	 * @return The client.
	 */
	protected abstract RadiusClient create(String server, byte[] secret);
}
