package com.alcatel_lucent.as.ims.impl.cx.factory;

import org.osgi.service.component.annotations.Component;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.cx.CxClientFactory;

/**
 * The client factory for TS 29.229 v7.7.0
 */
@Component(name = "CxClientFactory77", service = { CxClientFactory.class }, property = { "version3gpp=7.7.0" })
public class CxClientFactory77
		extends CxClientFactoryImpl {

	public CxClientFactory77() {
		super(new Version(7, 7));
	}

}
