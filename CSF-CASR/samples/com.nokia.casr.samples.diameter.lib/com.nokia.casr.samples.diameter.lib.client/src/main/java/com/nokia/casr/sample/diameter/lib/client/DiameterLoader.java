package com.nokia.casr.sample.diameter.lib.client;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterPeerListener;
import com.nextenso.proxylet.diameter.DiameterPeerTable;
import com.nextenso.proxylet.diameter.client.DiameterClient;
import com.nextenso.proxylet.diameter.client.DiameterClientFactory;
import com.nextenso.proxylet.diameter.client.DiameterClientListener;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;
import com.nokia.as.osgi.launcher.OsgiLauncher;

/**
 * This class sends some diameter requests to a remote diameter server.
 */
public class DiameterLoader implements Runnable, DiameterPeerListener {

	/**
	 * This service is the bridge between classpath world and CASR osgi world. Using
	 * this service, you can obtain CASR services, or register your classpath
	 * classes as osgi services in order to expose them to OSGI.
	 */
	private final OsgiLauncher _launcher;
	private volatile DiameterClient _client;
	private final ScheduledThreadPoolExecutor _thPool = new ScheduledThreadPoolExecutor(2);
	private final DiameterClientFactory _clientF;
	private final DiameterPeerTable _peerTable;
	private final DiameterPeer _peer;
	private volatile int _messageReceived;
	private volatile ScheduledFuture<?> _scheduler;

	DiameterLoader(OsgiLauncher launcher) throws InterruptedException, ExecutionException, TimeoutException {
		_launcher = launcher;
		_clientF = _launcher.getService(DiameterClientFactory.class).get(5, TimeUnit.SECONDS);
		_peerTable = _launcher.getService(DiameterPeerTable.class).get(5, TimeUnit.SECONDS);

		_peer = _peerTable.newDiameterPeer(_peerTable.getLocalDiameterPeer(), // default
				"testserver.nokia.com", "127.0.0.1", 3868, false, DiameterPeer.Protocol.TCP);
		_peer.addListener(this);
	}

	/**
	 * Starts load
	 */
	public void start() {
		try {
			System.out.println("DiameterLoader: starting");
			_peer.connect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Stops load
	 */
	public void stop() {
		if (_scheduler != null) {
			_scheduler.cancel(false);
		}
	}

	/**
	 * Gets the number of received responses
	 */
	public int getMessageReceived() {
		return _messageReceived;
	}

	/**
	 * We are connected to the server
	 */
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
		_scheduler = _thPool.scheduleAtFixedRate(this, 1000L, 1000L, TimeUnit.MILLISECONDS);
	}

	/**
	 * Method called each seconds, to send a request
	 */
	public void run() {
		// code = 1 this is a random value for the test
		DiameterClientRequest request = _client.newAcctRequest(1, true /* proxyable (P flag will be set */);

		final DiameterClientListener callback = new DiameterClientListener() {
			public void handleResponse(DiameterClientRequest request, DiameterClientResponse response) {
				System.out.println("handleResponse : response = " + response);
				_messageReceived++;
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
