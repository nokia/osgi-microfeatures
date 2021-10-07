package com.alcatel_lucent.as.ims.impl.charging.rf.factory;

import java.net.NoRouteToHostException;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.rf.InterimListener;
import com.alcatel_lucent.as.ims.diameter.charging.rf.RfClientFactory;
import com.alcatel_lucent.as.ims.diameter.charging.rf.RfEventClient;
import com.alcatel_lucent.as.ims.diameter.charging.rf.RfSessionClient;
import com.alcatel_lucent.as.ims.diameter.common.AbstractFactory;
import com.alcatel_lucent.as.ims.impl.charging.rf.EventClient;
import com.alcatel_lucent.as.ims.impl.charging.rf.SessionClient;

/**
 * The Rf Client Factory Implementation.
 */
public class RfClientFactoryImpl
		extends AbstractFactory
		implements RfClientFactory {

	protected RfClientFactoryImpl(Version version) {
		super(version);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.rf.RfClientFactory#createEventClient(java.lang.Iterable,
	 *      java.lang.String)
	 */
	public RfEventClient createEventClient(Iterable<String> servers, String realm)
		throws NoRouteToHostException {
		return new EventClient(servers, realm, getVersion());
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.rf.RfClientFactory#createSessionClient(java.lang.Iterable,
	 *      java.lang.String,
	 *      com.alcatel_lucent.as.ims.diameter.charging.rf.InterimListener)
	 */
	public RfSessionClient createSessionClient(Iterable<String> servers, String realm, InterimListener listener)
		throws NoRouteToHostException {
		return new SessionClient(servers, realm, listener, getVersion());
	}

}
