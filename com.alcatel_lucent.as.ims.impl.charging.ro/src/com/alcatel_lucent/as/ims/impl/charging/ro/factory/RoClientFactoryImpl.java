package com.alcatel_lucent.as.ims.impl.charging.ro.factory;

import java.net.NoRouteToHostException;

import com.alcatel_lucent.as.ims.Version;

import com.alcatel_lucent.as.ims.diameter.charging.ro.EcurClient;
import com.alcatel_lucent.as.ims.diameter.charging.ro.IecClient;
import com.alcatel_lucent.as.ims.diameter.charging.ro.RoClientFactory;
import com.alcatel_lucent.as.ims.diameter.charging.ro.ScurClient;
import com.alcatel_lucent.as.ims.diameter.common.AbstractFactory;
import com.alcatel_lucent.as.ims.impl.charging.ro.EcurClientImpl;
import com.alcatel_lucent.as.ims.impl.charging.ro.IecClientImpl;
import com.alcatel_lucent.as.ims.impl.charging.ro.ScurClientImpl;

/**
 * The Ro Client Factory Implementation.
 */
public class RoClientFactoryImpl
		extends AbstractFactory
		implements RoClientFactory {

	/**
	 * Constructor for this class.
	 * 
	 * @param version The 32.299 document version.
	 */
	protected RoClientFactoryImpl(Version version) {
		super(version);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.RoClientFactory#createScurClient(java.lang.Iterable,
	 *      String, java.lang.String)
	 */
	public ScurClient createScurClient(Iterable<String> servers, String realm, String serviceContextId)
		throws NoRouteToHostException {
		return new ScurClientImpl(servers, realm, serviceContextId, getVersion());
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.RoClientFactory#createIecClient(java.lang.Iterable,
	 *      String, java.lang.String)
	 */
	public IecClient createIecClient(Iterable<String> servers, String realm, String serviceContextId)
		throws NoRouteToHostException {
		return new IecClientImpl(servers, realm, serviceContextId, getVersion());
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.RoClientFactory#createEcurClient(java.lang.Iterable,
	 *      java.lang.String, java.lang.String)
	 */
	public EcurClient createEcurClient(Iterable<String> servers, String realm, String serviceContextId)
		throws NoRouteToHostException {
		return new EcurClientImpl(servers, realm, serviceContextId, getVersion());
	}

}
