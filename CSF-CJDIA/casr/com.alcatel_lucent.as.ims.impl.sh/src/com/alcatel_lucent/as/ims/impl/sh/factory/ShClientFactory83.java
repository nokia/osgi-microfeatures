package com.alcatel_lucent.as.ims.impl.sh.factory;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.sh.ShClientFactory;

import org.osgi.service.component.annotations.Component;

/**
 * The client factory for TS 29.329 v8.3.0
 */
@Component(name= "ShClientFactory83", service={ShClientFactory.class}, property={"version3gpp=8.3.0"})
public class ShClientFactory83
		extends ShClientFactoryImpl {

	public ShClientFactory83() {
		super(new Version(8,3));
	}

}
