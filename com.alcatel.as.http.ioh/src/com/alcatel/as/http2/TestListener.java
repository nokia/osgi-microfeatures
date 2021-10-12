// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2;

import java.nio.ByteBuffer;
import org.apache.log4j.Logger;
import java.util.concurrent.Executor;
import org.osgi.annotation.versioning.ProviderType;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import org.osgi.service.component.annotations.*;
import java.io.*;
import java.util.*;

// do not finish the response data before endRequest
// do not abort the request before recvReqHeaders

@Component(immediate=true, configurationPolicy = ConfigurationPolicy.OPTIONAL, property={"id=test"})
public class TestListener implements Http2RequestListener {

    @Override
    public String toString (){ return "TestListener";}

    public void newRequest (RequestContext cb){
	cb.attach (new TestContext ());
    }

    public void recvReqMethod (RequestContext cb, String method){
	TestContext tc = cb.attachment ();
	tc._headers.put ("x-method", method);
    }

    public void recvReqPath (RequestContext cb, String path){
	TestContext tc = cb.attachment ();
	tc._headers.put ("x-path", path);
    }

    public void recvReqScheme (RequestContext cb, String scheme){
	TestContext tc = cb.attachment ();
	tc._headers.put ("x-scheme", scheme);
    }

    public void recvReqAuthority (RequestContext cb, String auth){
	TestContext tc = cb.attachment ();
	tc._headers.put ("x-auth", auth);
    }

    public void recvReqHeader (RequestContext cb, String name, String value){
	TestContext tc = cb.attachment ();
	tc._headers.put ("x-"+name, value);
    }

    public void recvReqHeaders (RequestContext cb, boolean done){}

    public void recvReqData (RequestContext cb, ByteBuffer data, boolean done){
	TestContext tc = cb.attachment ();
	try{
	    while (data.remaining () > 0) tc._baos.write (data.get ());
	}catch(Exception e){}
    }
    
    public void endRequest (RequestContext cb){
	TestContext tc = cb.attachment ();
	cb.responseExecutor ().execute ( () -> {
		if (cb.isClosed ()) return;
		cb.setRespStatus (200);
		for (String name : tc._headers.keySet ())
		    cb.setRespHeader (name, tc._headers.get (name));
		byte[] data = tc._baos.toByteArray ();
		if (data.length == 0) data = "01234567989".getBytes ();
		cb.sendRespHeaders (false);
		cb.sendRespData (ByteBuffer.wrap (data), false, true);
	    });
    }

    public void abortRequest (RequestContext cb){};

    private static class TestContext {
	Map<String, String> _headers = new HashMap<> ();
	ByteArrayOutputStream _baos = new ByteArrayOutputStream ();
	
    }

}
