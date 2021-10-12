// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.cjdi.stest.server;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.log4j.Logger;

import com.nextenso.proxylet.ProxyletConfig;
import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterPeerTable;
import com.nextenso.proxylet.diameter.DiameterRequest;
import com.nextenso.proxylet.diameter.DiameterRequestProxylet;
import com.nextenso.proxylet.diameter.client.DiameterClientFactory;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;
import com.nextenso.proxylet.mgmt.CommandEvent;
import com.nextenso.proxylet.mgmt.Monitorable;
import com.nokia.as.cjdi.stest.common.Constants;

@Component(provides=DiameterRequestProxylet.class)
public class RequestServerProxylet implements DiameterRequestProxylet, Monitorable {

	private static Logger LOGGER = Logger.getLogger("jdiameter.server.request");

	public RequestServerProxylet() {}

	public void destroy() {}

	/**
	 * @see com.nextenso.proxylet.Proxylet#init(com.nextenso.proxylet.ProxyletConfig)
	 */
	public void init(ProxyletConfig config)
		throws ProxyletException {
		LOGGER.debug("init: private AVP code=" + Constants.MY_AVP.getAVPCode());
	}

	/**
	 * @see com.nextenso.proxylet.Proxylet#getProxyletInfo()
	 */
	public String getProxyletInfo() {
		return "Diameter request proxylet for JDiameter";
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterRequestProxylet#accept(com.nextenso.proxylet.diameter.DiameterRequest)
	 */
	public int accept(DiameterRequest request) {
		return ACCEPT_MAY_BLOCK;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterRequestProxylet#doRequest(com.nextenso.proxylet.diameter.DiameterRequest)
	 */
	public int doRequest(DiameterRequest request) {
		DiameterPeerTable peerTable = DiameterPeerTable.getDiameterPeerTable();
		List<DiameterPeer> peers = peerTable.getLocalDiameterPeers();
		if (!peers.isEmpty()) {
			DiameterPeer peer = peers.get(0);
			String originHost = peer.getOriginHost();
			try {
				DiameterClientFactory.getDiameterClientFactory().newDiameterClient(originHost, null, 1, 1, false, 0);
				LOGGER.debug("doRequest: can use the local host to make a client");
			}
			catch (Exception e) {
				LOGGER.error("doRequest: cannot use the local host to make a client", e);
			}
		}

		
		LOGGER.debug("doRequest: request=" + request);
		
		if (request.getDiameterAVP(DiameterBaseConstants.AVP_EXPERIMENTAL_RESULT) != null &&
		    request.getDiameterAVP(DiameterBaseConstants.AVP_EXPERIMENTAL_RESULT).getValueSize () == 1
		    ) {
			LOGGER.debug("doRequest: I got an AVP_EXPERIMENTAL_RESULT in the request !!");
		}
		if (request.getDiameterAVP(DiameterBaseConstants.AVP_EXPERIMENTAL_RESULT_CODE) != null &&
		    request.getDiameterAVP(DiameterBaseConstants.AVP_EXPERIMENTAL_RESULT_CODE).getValueSize () == 1
		    ) {
			LOGGER.debug("doRequest: I got an AVP_EXPERIMENTAL_RESULT_CODE in the request !!");
		}
		
		//request.getResponse().setResultCode(DiameterBaseConstants.RESULT_CODE_DIAMETER_SUCCESS);
		DiameterAVP experimentalResult = new DiameterAVP(DiameterBaseConstants.AVP_EXPERIMENTAL_RESULT);
		List<DiameterAVP> avps = new ArrayList<DiameterAVP>();
		DiameterAVP avp = new DiameterAVP(DiameterBaseConstants.AVP_EXPERIMENTAL_RESULT_CODE);
		avp.setValue(Unsigned32Format.toUnsigned32(5065L), false);
		avps.add(avp);
		avp = new DiameterAVP(DiameterBaseConstants.AVP_VENDOR_ID);
		avp.setValue(Unsigned32Format.toUnsigned32(10415L), false);
		avps.add(avp);
		experimentalResult.setValue(GroupedFormat.toGroupedAVP(avps), false);
		request.getResponse().addDiameterAVP(experimentalResult);
		
		LOGGER.debug("doRequest: response=" + request.getResponse());
	
		return RESPOND_FIRST_PROXYLET;
	}

	/**
	 * @see com.nextenso.proxylet.mgmt.Monitorable#commandEvent(com.nextenso.proxylet.mgmt.CommandEvent)
	 */
	public void commandEvent(CommandEvent event) {}

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
