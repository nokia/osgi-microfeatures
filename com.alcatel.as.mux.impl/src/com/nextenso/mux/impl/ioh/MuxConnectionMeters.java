package com.nextenso.mux.impl.ioh;

public interface MuxConnectionMeters {

	void stop();

	void tcpSocketConnected(int sockId, boolean clientSocket);

	void tcpSocketClosed(int sockId);

	void tcpSocketAborted(int sockId);

	void sctpSocketConnected(int sockId, boolean fromClient);

	void sctpSocketClosed(int sockId);

	void tcpSocketListening();

	void tcpSocketFailedConnect();

	void sctpSocketFailedConnect();

	void sctpSocketSendFailed();

	void sctpPeerAddressChanged();

	void udpSocketBound();

	void udpSocketFailedBind();

	void udpSocketClosed();

	void tcpSocketData(int len);

	void sctpSocketData(int len);

	void udpSocketData(int len);

	void sendTcpSocketData(int remaining);
	
	void sendSctpSocketData(int remaining);

	void sendUdpSocketData(int remaining);

	void sctpSocketListening();
	
	default void muxOpened(boolean local) {
		//do nothing by default
	}
	
	default void muxClosed(boolean local) {
		//do nothing by default
	}
	
	void setMuxStarted(boolean local, boolean started);
	
	public static String makeConnectionMonitorableName(boolean local, String stackInstance, int hash, String proto) {
		return (local ? "ioh.local." : "ioh.remote.") + 
				(stackInstance == null ? "__unknown" : stackInstance) +
				"." + hash +
				(proto == null ? "" : "." + proto);
	}
}