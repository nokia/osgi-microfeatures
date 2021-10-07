package com.nokia.as.cjdi.stest.client;

import java.util.List;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.alcatel.as.service.coordinator.Coordination;
import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterPeerTable;
import com.nextenso.proxylet.diameter.DiameterRoute;
import com.nextenso.proxylet.diameter.DiameterRouteTable;
import com.nextenso.proxylet.diameter.client.DiameterClient;
import com.nextenso.proxylet.diameter.client.DiameterClientFactory;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.IdentityFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nokia.as.cjdi.stest.common.Constants;
import com.nokia.as.util.junit4osgi.OsgiJunitRunner;

/**
 * Sends an http request to the "MyProxylet" through the http io handler.
 */
//@Component(provides = Object.class)
//@Property(name = OsgiJunitRunner.JUNIT, value = "true")
//@RunWith(OsgiJunitRunner.class)
public class BasicClientTest extends TestBase {
	
	static final int APP_ID = 10;
	static final int VENDOR_ID = 10;
	static final int COMMAND_CODE = 1;
	
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

//	@Before
//	public void before() {
//		_log = _logFactory.getLogger(BasicClientTest.class);
//	}

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
		DiameterClientRequest diameterClientRequest = diameterclient.newAcctRequest(COMMAND_CODE, true);
		DiameterAVP avp;
		avp = new DiameterAVP(Constants.MY_AVP);
		avp.setValue(UTF8StringFormat.toUtf8String("test"), false);
		diameterClientRequest.addDiameterAVP(avp);

		avp = diameterClientRequest.getDiameterAVP(DiameterBaseConstants.AVP_ORIGIN_HOST);
		byte[] avpValue = IdentityFormat.toIdentity("fake.originhost");
		avp.setValue(avpValue, false);

		avp = new DiameterAVP(DiameterBaseConstants.AVP_EXPERIMENTAL_RESULT);
		diameterClientRequest.addDiameterAVP(avp);

		avp = new DiameterAVP(DiameterBaseConstants.AVP_EXPERIMENTAL_RESULT_CODE);
		avp.addValue(new byte[0], true);
		diameterClientRequest.addDiameterAVP(avp);

		DiameterClientResponse resp = diameterClientRequest.execute();
		_log.warn("Response code =" + resp.getResultCode());
	}	
}
