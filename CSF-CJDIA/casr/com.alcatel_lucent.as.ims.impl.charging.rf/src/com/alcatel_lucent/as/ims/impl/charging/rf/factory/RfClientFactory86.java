package com.alcatel_lucent.as.ims.impl.charging.rf.factory;

import org.osgi.service.component.annotations.Component;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.rf.RfClientFactory;

/**
 * The client factory for TS 32.299 v8.6.0
 */
@Component(name= "RfClientFactory86", service={RfClientFactory.class}, property={"version3gpp=8.6.0"})
public class RfClientFactory86
		extends RfClientFactoryImpl {

	public RfClientFactory86() {
		super(new Version(8,6));
	}

}
