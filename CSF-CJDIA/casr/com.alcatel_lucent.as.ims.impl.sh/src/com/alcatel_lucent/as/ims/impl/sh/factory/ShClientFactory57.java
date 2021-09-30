package com.alcatel_lucent.as.ims.impl.sh.factory;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.sh.ShClientFactory;

import org.osgi.service.component.annotations.Component;

/**
 * The client factory for TS 29.329 v5.7.0
 */
@Component(name= "ShClientFactory57", service={ShClientFactory.class}, property={"version3gpp=5.7.0"})
public class ShClientFactory57
		extends ShClientFactoryImpl {

	public ShClientFactory57() {
		super(new Version(5,7));
	}

}
