package com.alcatel_lucent.as.ims.impl.sh.factory;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.sh.ShClientFactory;

import org.osgi.service.component.annotations.Component;

/**
 * The client factory for TS 29.329 v8.2.0
 */
@Component(name= "ShClientFactory82", service={ShClientFactory.class}, property={"version3gpp=8.2.0"})
public class ShClientFactory82
		extends ShClientFactoryImpl {

	public ShClientFactory82() {
		super(new Version(8,2));
	}

}
