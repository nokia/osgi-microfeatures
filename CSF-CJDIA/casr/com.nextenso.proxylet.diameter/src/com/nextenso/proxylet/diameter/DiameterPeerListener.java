package com.nextenso.proxylet.diameter;

/**
 * The Diameter Peer Listener.
 */
public interface DiameterPeerListener {

	public static final int VALUE_DISCONNECT_CAUSE_WATCHDOG_FAILURE = 0x10;

	/**
	 * Events delivered when a peer address changed
	 */
	public enum SctpAddressEvent
	{
		/**
		 * The address is now part of the association.
		 */
		ADDR_ADDED,

			/**
			 * This address is now reachable.
			 */
			ADDR_AVAILABLE,

			/**
			 * This address has now been confirmed as a valid address.
			 */
			ADDR_CONFIRMED,

			/**
			 * This address has now been made to be the primary destination address.
			 */
			ADDR_MADE_PRIMARY,

			/**
			 * The address is no longer part of the association.
			 */
			ADDR_REMOVED,

			/**
			 * The address specified can no longer be reached.
			 */
			ADDR_UNREACHABLE
			}

	/**
	 * Called when a peer is connected.
	 * 
	 * @param peer The connected peer.
	 */
	public void connected(DiameterPeer peer);

	/**
	 * Called when the peer connection fails.
	 * 
	 * @param peer The peer.
	 * @param msg The error message.
	 */
	public void connectionFailed(DiameterPeer peer, String msg);

	/**
	 * Called when a peer is disconnected.
	 * 
	 * @param peer The disconnected peer.
	 * @param disconnectCause The disconnect cause (a negative value indicates the
	 *          cause is unknown).
	 */
	public void disconnected(DiameterPeer peer, int disconnectCause);

	/**
	 * Called when there is an sctp event affecting the connection addresses.
	 * 
	 * @param peer The peer.
	 * @param addr the address affected.
	 * @param port the connection port
	 * @param event the sctp peer address change event : see sctp java API for a description of events.
	 */
	public void sctpAddressChanged(DiameterPeer peer, String addr, int port, SctpAddressEvent event);
}
