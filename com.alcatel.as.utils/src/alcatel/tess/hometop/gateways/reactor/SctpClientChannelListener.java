package alcatel.tess.hometop.gateways.reactor;

public interface SctpClientChannelListener extends SctpChannelListener {
  void connectionEstablished(SctpChannel cnx);
  
  void connectionFailed(SctpChannel cnx, Throwable error);
}
