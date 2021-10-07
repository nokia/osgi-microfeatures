package com.alcatel_lucent.as.ims.impl.sh.factory;

import org.osgi.service.component.annotations.Component;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.sh.ShClientFactory;

/**
 * The client factory for TS 29.329 v7.0.0
 */
@Component(name= "ShClientFactory70", service={ShClientFactory.class}, property={"version3gpp=7.0.0"})
public class ShClientFactory70
		extends ShClientFactoryImpl {

	public ShClientFactory70() {
		super(new Version(7,0));
	}

}
