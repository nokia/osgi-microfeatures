package com.nokia.casr.sample.diameter.client;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterPeerListener;
import com.nextenso.proxylet.diameter.DiameterPeerTable;
import com.nextenso.proxylet.diameter.client.DiameterClient;
import com.nextenso.proxylet.diameter.client.DiameterClientFactory;
import com.nextenso.proxylet.diameter.client.DiameterClientListener;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;

@Component(service = {})
public class ClientDemo implements Runnable, DiameterPeerListener {

	DiameterClient _client;
	ScheduledThreadPoolExecutor _thPool = new ScheduledThreadPoolExecutor(2);

	@Reference
	DiameterClientFactory _clientF;

	@Reference
	DiameterPeerTable _peerTable;

	@Activate
	public void start() throws Exception {
		DiameterPeer peer = _peerTable.newDiameterPeer(_peerTable.getLocalDiameterPeer(), // default
				"testserver.nokia.com", "127.0.0.1", 3868, false, DiameterPeer.Protocol.TCP);
		peer.addListener(ClientDemo.this);
		peer.connect();
	}

	// the connection is established
	public void connected(DiameterPeer peer) {
		System.out.println("TestClient : connected");
		try {
			_client = _clientF.newDiameterClient("testserver.nokia.com", "nokia.com", 10415, // lets use 3gpp vendor id
					123, // lets use a random application id for the test
					true, // lets use a session-id
					0 // yet session-lifetime=0 (3gpp likes that : a session for single transaction)
			);
		} catch (java.net.NoRouteToHostException e) {
			System.out.println("Failed to instanciate the client : " + e);
			return;
		}

		// schedule a request every second
		_thPool.scheduleAtFixedRate(this, 1000L, 1000L, TimeUnit.MILLISECONDS);
	}

	// we send a request every second
	public void run() {
		// code = 1 this is a random value for the test
		DiameterClientRequest request = _client.newAcctRequest(1, true /* proxyable (P flag will be set */);

		final DiameterClientListener callback = new DiameterClientListener() {
			public void handleResponse(DiameterClientRequest request, DiameterClientResponse response) {
				System.out.println("handleResponse : response = " + response);
			}

			public void handleException(DiameterClientRequest request, java.io.IOException ioe) {
				System.out.println("handleException : " + ioe);
			}
		};

		request.execute(callback);
	}

	/*************
	 * remaining PeerListener callbacks : not used in the test
	 ****************/

	public void connectionFailed(DiameterPeer peer, java.lang.String msg) {
		System.out.println("TestClient : connectionFailed");
		reconnect(peer);
	}

	public void disconnected(DiameterPeer peer, int disconnectCause) {
		System.out.println("TestClient : disconnected");
		reconnect(peer);
	}

	public void sctpAddressChanged(DiameterPeer peer, java.lang.String addr, int port,
			DiameterPeerListener.SctpAddressEvent event) {
	}

	private void reconnect(final DiameterPeer peer) {
		Runnable r = new Runnable() {
			public void run() {
				peer.connect();
			}
		};
		_thPool.schedule(r, 1000L, TimeUnit.MILLISECONDS);
	}

}
