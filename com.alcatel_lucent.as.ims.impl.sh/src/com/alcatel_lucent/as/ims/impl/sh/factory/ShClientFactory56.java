package com.alcatel_lucent.as.ims.impl.sh.factory;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.sh.ShClientFactory;

import org.osgi.service.component.annotations.Component;

/**
 * The client factory for TS 29.329 v5.6.0
 */
@Component(name= "ShClientFactory56", service={ShClientFactory.class}, property={"version3gpp=5.6.0"})
public class ShClientFactory56
		extends ShClientFactoryImpl {

	public ShClientFactory56() {
		super(new Version(5,6));
	}

}
