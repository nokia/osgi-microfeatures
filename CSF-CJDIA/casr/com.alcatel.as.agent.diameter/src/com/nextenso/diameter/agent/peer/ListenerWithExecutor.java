package com.nextenso.diameter.agent.peer;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.nextenso.diameter.agent.Utils;
import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterPeerListener;

public class ListenerWithExecutor
		implements DiameterPeerListener {

	private static final Logger LOGGER = Logger.getLogger("agent.diameter.listenerWithExecutor");

	private static class ConnectedTask
			implements Runnable {

		private final DiameterPeerListener _listener;
		private final DiameterPeer _peer;

		public ConnectedTask(DiameterPeerListener listener, DiameterPeer peer) {
			_listener = listener;
			_peer = peer;
		}

		public void run() {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("connected: listener=" + _listener);
			}

			_listener.connected(_peer);
		}

	}

	private static class DisconnectedTask
			implements Runnable {

		private final DiameterPeerListener _listener;
		private final DiameterPeer _peer;
		private final int _reason;

		public DisconnectedTask(DiameterPeerListener listener, DiameterPeer peer, int disconnectReason) {
			_listener = listener;
			_peer = peer;
			_reason = disconnectReason;
		}

		public void run() {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("disconnected: listener=" + _listener);
			}

			_listener.disconnected(_peer, _reason);
		}

	}

	private static class ConnectionFailedTask
			implements Runnable {

		private final DiameterPeerListener _listener;
		private final DiameterPeer _peer;
		private final String _message;

		public ConnectionFailedTask(DiameterPeerListener listener, DiameterPeer peer, String message) {
			_listener = listener;
			_peer = peer;
			_message = message;
		}

		public void run() {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("connectionFailed: listener=" + _listener);
			}
			_listener.connectionFailed(_peer, _message);
		}

	}

	private final DiameterPeerListener _listener;
	private final PlatformExecutor _executor;

	public ListenerWithExecutor(DiameterPeerListener listener) {
		_listener = listener;
		PlatformExecutor executor = Utils.getCallbackExecutor();
		if (executor == Utils.getPlatformExecutors ().getIOThreadPoolExecutor ()){
		    // this is the default returned by getCallbackExecutor --> not good : needs at least a Q
		    executor = Utils.getPlatformExecutors ().createQueueExecutor (Utils.getPlatformExecutors ().getIOThreadPoolExecutor (), listener.toString ());
		}
		_executor = executor; // final
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("new object with listener=" + _listener + ", executor=" + _executor);
		}
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeerListener#connected(com.nextenso.proxylet.diameter.DiameterPeer)
	 */
	public void connected(DiameterPeer peer) {
		Runnable task = new ConnectedTask(_listener, peer);
		_executor.execute(task, ExecutorPolicy.SCHEDULE);
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeerListener#connectionFailed(com.nextenso.proxylet.diameter.DiameterPeer,
	 *      java.lang.String)
	 */
	public void connectionFailed(DiameterPeer peer, String message) {
		Runnable callbackTask = new ConnectionFailedTask(_listener, peer, message);
		_executor.execute(callbackTask, ExecutorPolicy.SCHEDULE);
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeerListener#disconnected(com.nextenso.proxylet.diameter.DiameterPeer,
	 *      int)
	 */
	public void disconnected(DiameterPeer peer, int disconnectCause) {
		Runnable callbackTask = new DisconnectedTask(_listener, peer, disconnectCause);
		_executor.execute(callbackTask, ExecutorPolicy.SCHEDULE);

	}

	public void sctpAddressChanged(final DiameterPeer peer, final String addr, final int port, final DiameterPeerListener.SctpAddressEvent event){
		Runnable callbackTask = new Runnable (){
				public void run (){
					_listener.sctpAddressChanged (peer, addr, port, event);
				}
			};
		_executor.execute(callbackTask, ExecutorPolicy.SCHEDULE);

	}

	public DiameterPeerListener getListener() {
		return _listener;
	}

}
