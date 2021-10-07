package com.alcatel_lucent.as.ims.impl.sh.factory;

import org.osgi.service.component.annotations.Component;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.sh.ShClientFactory;

/**
 * The client factory for TS 29.329 v7.5.0
 */
@Component(name= "ShClientFactory75", service={ShClientFactory.class}, property={"version3gpp=7.5.0"})
public class ShClientFactory75
		extends ShClientFactoryImpl {

	public ShClientFactory75() {
		super(new Version(7,5));
	}

}
