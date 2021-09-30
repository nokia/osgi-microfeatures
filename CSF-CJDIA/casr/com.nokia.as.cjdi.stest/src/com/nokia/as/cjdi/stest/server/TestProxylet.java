package com.nokia.as.cjdi.stest.server;

import java.util.function.Function;
import com.nextenso.proxylet.*;
import com.nextenso.proxylet.diameter.*;
import org.apache.felix.dm.annotation.api.Component;

/**
 * This proxylet is used for the new tests run by TestPlayer
 *
 *
 * triggered for application id = 1
 */
@Component
public class TestProxylet implements DiameterRequestProxylet, DiameterResponseProxylet {

    public String getProxyletInfo (){ return "TestRequestProxylet";}
    public void destroy(){}
    public void init(ProxyletConfig pc){}

    public int accept(DiameterRequest request) {
	if (request.getDiameterApplication () == 1)
	    return DiameterRequestProxylet.ACCEPT;
	if (request.getDiameterApplication () == 2)
	    return DiameterRequestProxylet.ACCEPT_MAY_BLOCK;
	return DiameterRequestProxylet.IGNORE;
    }
    
    public int doRequest(DiameterRequest request){

	switch (request.getDiameterCommand ()){
	case 1: return returnOK (request);
	case 2: return suspendOK (request);
	case 3: return redirect (request);
	case 4: return samePxlet (request);
	}

	return DiameterRequestProxylet.NEXT_PROXYLET;
    }

    // triggered with command code = 1 : respond ok
    private int returnOK (DiameterRequest request){
	request.getResponse ().setResultCode (2001);
	return DiameterRequestProxylet.RESPOND_FIRST_PROXYLET;
    }
    // triggered with command code = 2 : suspend and respond ok
    private int suspendOK (DiameterRequest request){
	new Thread (() -> {
		try{
		    Thread.sleep (1000);
		    request.getResponse ().setResultCode (2001);
		    request.resume (DiameterRequestProxylet.RESPOND_FIRST_PROXYLET);
		}catch(Exception e){
		}
	}).start ();
	return DiameterRequestProxylet.SUSPEND;
    }
    // triggered with command code = 3 : redirect response
    private int redirect (DiameterRequest request){
	Object o = request.getAttribute ("x");
	if (o == null){
	    return setResponse (request, 3006, (resp) -> {
		    resp.getRequest ().setAttribute ("x", Boolean.TRUE);
		    return DiameterResponseProxylet.REDIRECT_FIRST_PROXYLET;
		});
	}
	return setResponse (request, 2001, (resp) -> {
		return DiameterResponseProxylet.NEXT_PROXYLET;
	    });
    }
    // triggered with command code = 4 : same proxylet
    private int samePxlet (DiameterRequest request){
	Object o = request.getAttribute ("x");
	if (o == null){
	    request.setAttribute ("x", Boolean.TRUE);
	    return DiameterRequestProxylet.SAME_PROXYLET;
	}
	request.getResponse ().setResultCode (2001);
	return DiameterRequestProxylet.RESPOND_FIRST_PROXYLET;
    }



    private int setResponse (DiameterRequest req, int resultCode, Function<DiameterResponse, Integer> f){
	req.getResponse ().setResultCode (resultCode);
	req.setAttribute ("doResponse", f);
	return DiameterRequestProxylet.RESPOND_FIRST_PROXYLET;
    }


    /******************** Response Proxylet **************/

    public int accept(DiameterResponse response) {
	if (response.getDiameterApplication () == 1)
	    return DiameterResponseProxylet.ACCEPT;
	if (response.getDiameterApplication () == 2)
	    return DiameterResponseProxylet.ACCEPT_MAY_BLOCK;
	return DiameterResponseProxylet.IGNORE;
    }

    public int doResponse(DiameterResponse response){
	Function<DiameterResponse, Integer> f = (Function<DiameterResponse, Integer>) response.getRequest ().getAttribute ("doResponse");

	if (f != null) return f.apply (response);

	return DiameterResponseProxylet.NEXT_PROXYLET;
	
    }

}
