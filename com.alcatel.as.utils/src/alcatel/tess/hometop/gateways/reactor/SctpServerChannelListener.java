package alcatel.tess.hometop.gateways.reactor;

public interface SctpServerChannelListener extends SctpChannelListener {
  void connectionAccepted(SctpServerChannel ssc, SctpChannel client);
  
  void serverConnectionClosed(SctpServerChannel ssc, Throwable err);
}
