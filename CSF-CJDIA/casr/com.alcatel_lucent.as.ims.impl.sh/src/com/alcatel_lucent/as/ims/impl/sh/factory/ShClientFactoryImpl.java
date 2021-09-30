package com.alcatel_lucent.as.ims.impl.sh.factory;

import java.net.NoRouteToHostException;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.common.AbstractFactory;
import com.alcatel_lucent.as.ims.diameter.sh.PushNotificationRequest;
import com.alcatel_lucent.as.ims.diameter.sh.ShClient;
import com.alcatel_lucent.as.ims.diameter.sh.ShClientFactory;
import com.alcatel_lucent.as.ims.impl.sh.PnrImpl;
import com.alcatel_lucent.as.ims.impl.sh.ShClientImpl;
import com.nextenso.proxylet.diameter.DiameterRequest;

/**
 * The Sh Client Factory Implementation.
 */
public class ShClientFactoryImpl
		extends AbstractFactory
		implements ShClientFactory {

	protected ShClientFactoryImpl(Version version) {
		super(version);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.ShClientFactory#createShClient(java.lang.String, java.lang.String)
	 */
	public ShClient createShClient(String destinationHost, String destinationRealm)
		throws NoRouteToHostException {
		return new ShClientImpl(destinationHost, destinationRealm, getVersion());
	}

	public PushNotificationRequest createPNR(DiameterRequest request) {
		return new PnrImpl(request, getVersion());
	}

}
