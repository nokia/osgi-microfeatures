// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.cjdi.stest.server;

import java.util.List;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.log4j.Logger;

import com.nextenso.proxylet.ProxyletConfig;
import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterPeerTable;
import com.nextenso.proxylet.diameter.DiameterRequest;
import com.nextenso.proxylet.diameter.DiameterRequestProxylet;
import com.nextenso.proxylet.diameter.client.DiameterClientFactory;
import com.nextenso.proxylet.diameter.dictionary.DiameterAVPDictionary;
import com.nextenso.proxylet.diameter.dictionary.DiameterCommandBuilder;
import com.nextenso.proxylet.diameter.dictionary.DiameterCommandBuilder.GenerationException;
import com.nextenso.proxylet.diameter.dictionary.DiameterCommandDictionary;
import com.nextenso.proxylet.diameter.dictionary.DiameterDictionaryFactory;
import com.nextenso.proxylet.diameter.dictionary.DiameterDictionaryFactory.LoadingException;
import com.nextenso.proxylet.diameter.dictionary.annotations.DiameterAVP;
import com.nextenso.proxylet.diameter.dictionary.annotations.DiameterCommand;
import com.nextenso.proxylet.mgmt.CommandEvent;
import com.nextenso.proxylet.mgmt.Monitorable;
import com.nokia.as.cjdi.stest.common.Constants;
import com.nokia.as.cjdi.stest.common.DiameterDictionaryConfig;

@Component(provides=DiameterRequestProxylet.class)
public class RequestServerDictionaryProxylet implements DiameterRequestProxylet, Monitorable {
	
	@ServiceDependency
	private DiameterDictionaryFactory dicFactory;

	@DiameterCommand(name = "My-Diameter-Answer")
	static class MyDiameterBeanAnswer {

		@DiameterAVP(name = "Session-Id")
		private String sessionId;
		
		@DiameterAVP(name = "Auth-Application-Id")
		private Long authAppId;
		
		@DiameterAVP(name = "Origin-Host")
		private String originHost;
		
		@DiameterAVP(name = "Origin-Realm")
		private String originFormat;
		
		@DiameterAVP(name = "Destination-Realm")
		private String destinationRealm;
		
		@DiameterAVP(name = "Auth-Request-Type")
		private Integer authReqType;
		
		@DiameterAVP(name = "Destination-Host")
		private String destinationHost;
		
		@DiameterAVP(name = "NAS-Identifier")
		private String nasIdentifier;
		
		@DiameterAVP(name = "My-Custom-AVP")
		private byte[] customAVP;
		
		public MyDiameterBeanAnswer() {
		}

		public String getSessionId() {
			return sessionId;
		}

		public void setSessionId(String sessionId) {
			this.sessionId = sessionId;
		}

		public Long getAuthAppId() {
			return authAppId;
		}

		public void setAuthAppId(Long authAppId) {
			this.authAppId = authAppId;
		}

		public String getOriginHost() {
			return originHost;
		}

		public void setOriginHost(String originHost) {
			this.originHost = originHost;
		}

		public String getOriginFormat() {
			return originFormat;
		}

		public void setOriginFormat(String originFormat) {
			this.originFormat = originFormat;
		}

		public String getDestinationRealm() {
			return destinationRealm;
		}

		public void setDestinationRealm(String destinationRealm) {
			this.destinationRealm = destinationRealm;
		}

		public Integer getAuthReqType() {
			return authReqType;
		}

		public void setAuthReqType(Integer authReqType) {
			this.authReqType = authReqType;
		}

		public String getDestinationHost() {
			return destinationHost;
		}

		public void setDestinationHost(String destinationHost) {
			this.destinationHost = destinationHost;
		}

		public String getNasIdentifier() {
			return nasIdentifier;
		}

		public void setNasIdentifier(String nasIdentifier) {
			this.nasIdentifier = nasIdentifier;
		}

		public byte[] getCustomAVP() {
			return customAVP;
		}

		public void setCustomAVP(byte[] customAVP) {
			this.customAVP = customAVP;
		}
	}

	
	private static Logger LOGGER = Logger.getLogger("jdiameter.server.request");
	private DiameterDictionaryConfig conf;
	private DiameterCommandBuilder cmdBuilder;

	public RequestServerDictionaryProxylet() {}

	public void destroy() {}

	/**
	 * @see com.nextenso.proxylet.Proxylet#init(com.nextenso.proxylet.ProxyletConfig)
	 */
	public void init(ProxyletConfig config)
		throws ProxyletException {
		LOGGER.debug("init: private AVP code=" + Constants.MY_AVP.getAVPCode());
		
		DiameterAVPDictionary avpDic;
		try {
			avpDic = dicFactory.loadAVPDictionaryFromJSON(conf.getAVPDictionary());
			DiameterCommandDictionary cmdDic = dicFactory.loadCommandDictionaryFromJSON(conf.getCommandDictionary(),
					avpDic);
			
			cmdBuilder = new DiameterCommandBuilder(cmdDic, avpDic);
		} catch (LoadingException e) {
			throw new ProxyletException(e);
		}
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

	@ConfigurationDependency
	public void injectConfig(DiameterDictionaryConfig conf) {
		this.conf = conf;
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

		
		MyDiameterBeanAnswer answer = new MyDiameterBeanAnswer();
		answer.setCustomAVP("despacito".getBytes());
		answer.setNasIdentifier("test2");

		try {
			cmdBuilder.buildResponse(request, answer);
		} catch (GenerationException e) {
			LOGGER.error("failed to generate response", e);
		}
		
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
