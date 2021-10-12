// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.cjdi.stest.client;

import java.util.List;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.alcatel.as.service.coordinator.Coordination;
import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterPeerTable;
import com.nextenso.proxylet.diameter.DiameterRoute;
import com.nextenso.proxylet.diameter.DiameterRouteTable;
import com.nextenso.proxylet.diameter.client.DiameterClient;
import com.nextenso.proxylet.diameter.client.DiameterClientFactory;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;
import com.nextenso.proxylet.diameter.dictionary.DiameterAVPDictionary;
import com.nextenso.proxylet.diameter.dictionary.DiameterCommandBuilder;
import com.nextenso.proxylet.diameter.dictionary.DiameterCommandDictionary;
import com.nextenso.proxylet.diameter.dictionary.DiameterDictionaryFactory;
import com.nextenso.proxylet.diameter.dictionary.annotations.DiameterAVP;
import com.nextenso.proxylet.diameter.dictionary.annotations.DiameterCommand;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nokia.as.cjdi.stest.common.DiameterDictionaryConfig;
import com.nokia.as.util.junit4osgi.OsgiJunitRunner;

@Component(provides = Object.class)
@Property(name = OsgiJunitRunner.JUNIT, value = "true")
@RunWith(OsgiJunitRunner.class)
public class DictionaryClientTest extends TestBase {

	static final int APP_ID = 10;
	static final int VENDOR_ID = 10;
	static final int COMMAND_CODE = 1;

	@DiameterCommand(name = "My-Diameter-Request")
	static class MyDiameterBean {

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
		
		public MyDiameterBean() {
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
	
	@ServiceDependency
	protected DiameterDictionaryFactory dicFac;

	@ServiceDependency(filter = "(name=ACTIVATION)")
	protected Coordination _ready; // injected

	@ServiceDependency
	protected LogServiceFactory _logFactory; // injected

	@ServiceDependency
	protected DiameterRouteTable _routeTable; // injected

	@ServiceDependency
	protected DiameterPeerTable _peerTable; // injected

	@ServiceDependency
	protected DiameterClientFactory _clientFactory; // injected

	protected LogService _log;

	protected DiameterDictionaryConfig conf;

	@Before
	public void before() {
		_log = _logFactory.getLogger(DictionaryClientTest.class);
	}

	@ConfigurationDependency
	public void injectConfig(DiameterDictionaryConfig conf) {
		this.conf = conf;
	}

	@Test
	public void testBasic() throws Exception {
		_log.warn("routeTable=%s", _routeTable);
		_log.warn("peerTable=" + _peerTable);

		super.awaitPeerConnected(_peerTable); // not working all the times , I don't know why ...

		List<DiameterPeer> localPeers = _peerTable.getLocalDiameterPeers();

		if (localPeers != null && !localPeers.isEmpty()) {
			DiameterPeer localPeer = localPeers.get(0);
			_log.warn("local peer Firmware-Revision=" + localPeer.getFirmwareRevision());
		}

		List<DiameterRoute> routes = _routeTable.getDiameterRoutes(null, -1, 0);
		_log.warn("DiameterRoutes=" + routes);

		// No way to know when the peer table is connected. Sometimes ... poll
		// until peer table is connected.
		DiameterClient diameterclient = _clientFactory.newDiameterClient("server.alcatel.com", "alcatel.com", VENDOR_ID,
				APP_ID, false, -1);

		DiameterAVPDictionary avpDic = dicFac.loadAVPDictionaryFromJSON(conf.getAVPDictionary());
		DiameterCommandDictionary cmdDic = dicFac.loadCommandDictionaryFromJSON(conf.getCommandDictionary(),
				avpDic);
		
		DiameterCommandBuilder commandBuilder = new DiameterCommandBuilder(cmdDic, 
				avpDic);
		
		MyDiameterBean bean = new MyDiameterBean();
		bean.setCustomAVP("hello".getBytes());
		
		DiameterClientRequest req = commandBuilder.buildRequest(diameterclient, bean);
		_log.warn("========= Generated request=" + req);
		DiameterClientResponse response = req.execute();
		_log.warn("response=" + response);
		com.nextenso.proxylet.diameter.DiameterAVP avp = response.getDiameterAVP(avpDic.getAVPDefinitionByName("My-Custom-AVP"));
		Assert.assertNotNull(avp);
		Assert.assertArrayEquals("despacito".getBytes(), avp.getValue());
		
		
		com.nextenso.proxylet.diameter.DiameterAVP avp2 = response.getDiameterAVP(avpDic.getAVPDefinitionByName("NAS-Identifier"));
		Assert.assertArrayEquals(UTF8StringFormat.toUtf8String("test2"), avp2.getValue());
	}
}
