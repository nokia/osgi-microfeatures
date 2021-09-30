package com.alcatel_lucent.as.ims.impl.sh.factory;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.sh.ShClientFactory;

import org.osgi.service.component.annotations.Component;

/**
 * The client factory for TS 29.329 v6.3.0
 */
@Component(name= "ShClientFactory63", service={ShClientFactory.class}, property={"version3gpp=6.3.0"})
public class ShClientFactory63
		extends ShClientFactoryImpl {

	public ShClientFactory63() {
		super(new Version(6,3));
	}

}
