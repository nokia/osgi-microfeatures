package com.alcatel_lucent.as.ims.impl.sh.factory;

import org.osgi.service.component.annotations.Component;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.sh.ShClientFactory;

/**
 * The client factory for TS 29.329 v8.5.0
 */
@Component(name= "ShClientFactory85", service={ShClientFactory.class}, property={"version3gpp=8.5.0"})
public class ShClientFactory85
		extends ShClientFactoryImpl {

	public ShClientFactory85() {
		super(new Version(8,5));
	}

}
