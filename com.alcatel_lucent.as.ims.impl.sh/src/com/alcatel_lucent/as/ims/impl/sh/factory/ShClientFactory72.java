package com.alcatel_lucent.as.ims.impl.sh.factory;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.sh.ShClientFactory;

import org.osgi.service.component.annotations.Component;

/**
 * The client factory for TS 29.329 v7.2.0
 */
@Component(name= "ShClientFactory72", service={ShClientFactory.class}, property={"version3gpp=7.2.0"})
public class ShClientFactory72
		extends ShClientFactoryImpl {

	public ShClientFactory72() {
		super(new Version(7,2));
	}

}
