package com.alcatel_lucent.as.ims.impl.cx.factory;

import org.osgi.service.component.annotations.Component;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.cx.CxClientFactory;

/**
 * The client factory for TS 29.229 v6.6.0
 */
@Component(name = "CxClientFactory66", service = { CxClientFactory.class }, property = { "version3gpp=6.6.0" })
public class CxClientFactory66
		extends CxClientFactoryImpl {

	public CxClientFactory66() {
		super(new Version(6, 6));
	}

}
