package com.alcatel_lucent.as.ims.impl.cx.factory;

import org.osgi.service.component.annotations.Component;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.cx.CxClientFactory;

/**
 * The client factory for TS 29.229 v7.3.0
 */
@Component(name = "CxClientFactory73", service = { CxClientFactory.class }, property = { "version3gpp=7.3.0" })
public class CxClientFactory73
		extends CxClientFactoryImpl {

	public CxClientFactory73() {
		super(new Version(7, 3));
	}

}
