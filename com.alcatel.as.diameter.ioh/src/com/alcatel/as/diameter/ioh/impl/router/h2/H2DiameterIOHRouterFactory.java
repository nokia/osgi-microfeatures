// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.diameter.ioh.impl.router.h2;

import org.apache.log4j.Logger;
import org.osgi.service.component.annotations.*;

import java.util.*;

import com.alcatel.as.diameter.ioh.*;

import com.alcatel_lucent.as.management.annotation.config.FileDataProperty;

import com.alcatel.as.http2.client.api.*;

@Component(service={DiameterIOHRouterFactory.class}, property={"router.id=h2"}, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class H2DiameterIOHRouterFactory extends DiameterIOHRouterFactory {

    public static Logger LOGGER = Logger.getLogger ("as.ioh.diameter.router.h2");

    public final static String CONF_H2_URI = "diameter.ioh.h2.uri";
    public final static String CONF_REDIRECT_MAX = "diameter.ioh.h2.redirect.max";
    
    @FileDataProperty(title="Http Headers",
		      fileData="h2routerHeaders.txt",
		      required=true,
		      dynamic=false,
		      section="General",
		      help="Describes the http headers to add to the h2 request.")
    public final static String CONF_H2_HEADERS = "diameter.ioh.h2.headers";
    

    protected HttpClientFactory _h2F;
    protected Headers _headers;
    
    @Reference
    public void setH2ClientFactory (HttpClientFactory f){
	_h2F = f;
    }
    public HttpClientFactory getH2ClientFactory (){ return _h2F;}

    @Activate
    public void activate(Map<String, String> conf) {
	setConf (conf);
	try{
	    _headers = new Headers ().init ((String) conf.get (CONF_H2_HEADERS));
	}catch(Exception e){
	    LOGGER.error (this+" : invalid configuration", e);
	}
    }
    
    @Deactivate
    public void stop() {
    }

    @Override
    public String toString (){
	return "H2DiameterIOHRouterFactory";
    }

    public DiameterIOHRouter newDiameterIOHRouter (){
	return new H2DiameterIOHRouter (this, LOGGER);
    }

    public Headers getHeaders (){ return _headers;}

}
