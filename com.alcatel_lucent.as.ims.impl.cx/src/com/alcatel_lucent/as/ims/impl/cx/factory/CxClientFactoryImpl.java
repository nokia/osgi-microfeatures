package com.alcatel_lucent.as.ims.impl.cx.factory;

import java.net.NoRouteToHostException;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.common.AbstractFactory;
import com.alcatel_lucent.as.ims.diameter.cx.CxClient;
import com.alcatel_lucent.as.ims.diameter.cx.CxClientFactory;
import com.alcatel_lucent.as.ims.impl.cx.CxClientImpl;

/**
 * The Cx Client Factory abstractImplementation.
 */
public abstract class CxClientFactoryImpl
		extends AbstractFactory
		implements CxClientFactory {

	protected CxClientFactoryImpl(Version version) {
		super(version);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.cx.CxClientFactory#createCxClient(java.lang.String,
	 *      java.lang.String)
	 */
	public CxClient createCxClient(String destinationHost, String destinationRealm)
		throws NoRouteToHostException {
		return new CxClientImpl(destinationHost, destinationRealm, getVersion());
	}
	
	//		if (version.getMajor() == 8 && version.getMinor() >= 7) {
	//			throw new UnsupportedVersionException();
	//		}
	//		if (version.getMajor() == 9 && version.getMinor() >= 0) {
	//			throw new UnsupportedVersionException();
	//		}
	//		if (version.getMajor() > 9) {
	//			throw new UnsupportedVersionException();
	//		}

}
