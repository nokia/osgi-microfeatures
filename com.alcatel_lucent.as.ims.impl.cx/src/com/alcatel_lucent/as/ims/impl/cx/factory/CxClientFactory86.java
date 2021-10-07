package com.alcatel_lucent.as.ims.impl.cx.factory;

import org.osgi.service.component.annotations.*;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.cx.CxClientFactory;

/**
 * The client factory for TS 29.229 v8.6.0
 */
@Component(name = "CxClientFactory86", service = { CxClientFactory.class }, property = { "version3gpp=8.6.0" })
public class CxClientFactory86
		extends CxClientFactoryImpl {

	public CxClientFactory86() {
		super(new Version(8, 6));
	}

}
