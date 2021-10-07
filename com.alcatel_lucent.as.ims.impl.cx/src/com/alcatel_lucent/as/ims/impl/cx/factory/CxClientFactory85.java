package com.alcatel_lucent.as.ims.impl.cx.factory;

import org.osgi.service.component.annotations.Component;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.cx.CxClientFactory;

/**
 * The client factory for TS 29.229 v8.5.0
 */
@Component(name = "CxClientFactory85", service = { CxClientFactory.class }, property = { "version3gpp=8.5.0" })
public class CxClientFactory85
		extends CxClientFactoryImpl {

	public CxClientFactory85() {
		super(new Version(8, 5));
	}

}
