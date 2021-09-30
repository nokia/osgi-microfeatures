package com.nokia.casr.samples.diameter.lib.server;

import java.util.concurrent.atomic.AtomicInteger;

import com.nextenso.proxylet.ProxyletConfig;
import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterRequest;
import com.nextenso.proxylet.diameter.DiameterRequestProxylet;
import com.nextenso.proxylet.diameter.DiameterResponse;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * This components sends some diameter requests to a remote diameter server.
 */

public class TestServer implements DiameterRequestProxylet {

	private AtomicInteger _count = new AtomicInteger(0);

	/***************************************/
	/********** Lifecycle mgmt *************/
	/***************************************/

	// called before any request comes in
	public void init(ProxyletConfig config) throws ProxyletException {
		// retrieve a parameter for illustration purpose
		String value = config.getStringParameter("myparam");
		System.out.println("***************** TestServer : init : myparam=" + value);
	}

	// not used in this sample
	public void destroy() {
	}

	// only informational
	public String getProxyletInfo() {
		return "TestServer";
	}

	/****************************************/
	/********** Call processing *************/
	/****************************************/

	// this method should be idempotent : do not modify the request or handle it :
	// just express your interest in it / or not.
	// do not block in its processing
	public int accept(DiameterRequest request) {
		// we accept all incoming requests
		return DiameterRequestProxylet.ACCEPT;
	}

	// This is where the request is handled may be called by many threads in
	// parallel, but not for the same request
	// (obviously) do not block in its processing
	public int doRequest(DiameterRequest request) {
		int count = _count.incrementAndGet();

		System.out.println(
				"***************** TestServer : doRequest : received request=" + request + ", count=" + _count);

		DiameterResponse response = request.getResponse();

		// indicate success with 2001 (there is constant, yet the int value 2001 would
		// be the same)
		// setResultCode is a shortcut (rather than setting explicitely the AVP)
		response.setResultCode(DiameterBaseConstants.RESULT_CODE_DIAMETER_SUCCESS);

		// for illustration purpose (no specific meaning) : insert a username AVP.
		// the AVP definition is part of existing constants.
		DiameterAVP usernameAVP = new DiameterAVP(DiameterBaseConstants.AVP_USER_NAME);

		usernameAVP.setValue(UTF8StringFormat.toUtf8String("HelloWorld " + count), false);
		response.addDiameterAVP(usernameAVP);

		// for illustration purpose (no specific meaning) : insert an exotic
		// vendor specific AVP / no part of diameter base protocol
		DiameterAVP applicationAVP = new DiameterAVP(99, 10415, DiameterAVP.V_FLAG); // code=99, vendorId=10415, flags=V
		// lets make it multi-valued for fun : 1 and 2
		applicationAVP.addValue(Unsigned32Format.toUnsigned32(1), false);
		applicationAVP.addValue(Unsigned32Format.toUnsigned32(2), false);
		response.addDiameterAVP(applicationAVP);

		// yet we dont plan to have response proxylet in the response chain
		return DiameterRequestProxylet.RESPOND_FIRST_PROXYLET;
	}
}
