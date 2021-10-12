// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.cjdi.stest.server;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.log4j.Logger;

import com.nextenso.proxylet.ProxyletConfig;
import com.nextenso.proxylet.ProxyletContext;
import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterResponse;
import com.nextenso.proxylet.diameter.DiameterResponseProxylet;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.IdentityFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.mgmt.CommandEvent;
import com.nextenso.proxylet.mgmt.Monitorable;
import com.nokia.as.cjdi.stest.common.Constants;

@Component(provides=DiameterResponseProxylet.class)
public class ResponseServerProxylet implements  DiameterResponseProxylet, Monitorable {
		
	private static Logger LOGGER = Logger.getLogger("jdiameter.server.response");

	public ResponseServerProxylet() {
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterResponseProxylet#doResponse(com.nextenso.proxylet.diameter.DiameterResponse)
	 */
	public int doResponse(DiameterResponse response) {
		LOGGER.debug("doResponse: adding the application AVP");
		DiameterAVP avp = new DiameterAVP(Constants.MY_AVP);
		avp.setValue(UTF8StringFormat.toUtf8String("test"), false);
		response.addDiameterAVP(avp);
		
		avp = response.getDiameterAVP(DiameterBaseConstants.AVP_ORIGIN_HOST);
		byte[] avpValue = IdentityFormat.toIdentity("fake.server.originhost");
		avp.setValue(avpValue, false);

		return NEXT_PROXYLET;
	}

	public void destroy() {}

	public String getProxyletInfo() {
		return "JDiameter Server Request Test Pxlet";
	}

	public void init(ProxyletConfig config)
		throws ProxyletException {
		ProxyletContext context = config.getProxyletContext();
		LOGGER.debug("init: Param = "+ context.getAttribute("launcher.arg"));
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterResponseProxylet#accept(com.nextenso.proxylet.diameter.DiameterResponse)
	 */
	public int accept(DiameterResponse response) {
		return ACCEPT;
	}


	/**
	 * @see com.nextenso.proxylet.mgmt.Monitorable#commandEvent(com.nextenso.proxylet.mgmt.CommandEvent)
	 */
	public void commandEvent(CommandEvent event) {
	}

	/**
	 * @see com.nextenso.proxylet.mgmt.Monitorable#getCounters()
	 */
	public int[] getCounters() {
		return new int[0];
	}

	/**
	 * @see com.nextenso.proxylet.mgmt.Monitorable#getMajorVersion()
	 */
	public int getMajorVersion() {
		return 1;
	}

	/**
	 * @see com.nextenso.proxylet.mgmt.Monitorable#getMinorVersion()
	 */
	public int getMinorVersion() {
		return 0;
	}


}
