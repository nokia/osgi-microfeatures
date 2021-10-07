package com.alcatel_lucent.as.ims.impl.cx.factory;

import org.osgi.service.component.annotations.Component;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.cx.CxClientFactory;

/**
 * The client factory for TS 29.229 v6.7.0
 */
@Component(name = "CxClientFactory67", service = { CxClientFactory.class }, property = { "version3gpp=6.7.0" })
public class CxClientFactory67
		extends CxClientFactoryImpl {

	public CxClientFactory67() {
		super(new Version(6, 7));
	}

}