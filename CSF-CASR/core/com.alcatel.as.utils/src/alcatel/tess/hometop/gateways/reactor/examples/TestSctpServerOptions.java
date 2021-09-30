package alcatel.tess.hometop.gateways.reactor.examples;

// Jdk
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.alcatel.as.util.sctp.*;
import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.SctpClientOption;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.SctpServerOption;
import alcatel.tess.hometop.gateways.reactor.SctpChannel;
import alcatel.tess.hometop.gateways.reactor.SctpServerChannel;
import alcatel.tess.hometop.gateways.reactor.SctpServerChannelListener;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;

/**
 * Listen to incoming tcp connection request on port 9999.
 */
public class TestSctpServerOptions implements SctpServerChannelListener {
  static Logger _logger = Logger.getLogger("test");
  static SctpServerChannel _chanel;
  
  public static void main(String args[]) throws Exception {
    ReactorProvider factory = ReactorProvider.provider();
    Reactor reactor = factory.newReactor("reactor", true, null);
    InetSocketAddress local = new InetSocketAddress(args[0], 5500);
    
    TestSctpServerOptions me = new TestSctpServerOptions();
    //_schanel = factory.newSctpServerChannel(addr, new InetAddress[] {InetAddress.getByName("139.54.130.12")}, 5, 5, 0, null, me, null, reactor);
    Map<SctpServerOption, Object> opts = new HashMap<SctpServerOption, Object>();
    opts.put(SctpServerOption.SO_RCVBUF, new Integer(100000));
    opts.put(SctpServerOption.SO_SNDBUF, new Integer(150000));
/*
    opts.put(SctpServerOption.SECONDARY_LOCAL_ADDRS,
             new InetAddress[] { InetAddress.getByName(args[1]) });
*/

  sctp_spp_flags flags = new sctp_spp_flags(true, false, true, true, false);
    sctp_paddrparams params = new sctp_paddrparams(new InetSocketAddress(0), 10000, 0, 0, 0, flags);
    Map<SctpSocketOption, SctpSocketParam> sockopts = new HashMap<>();
    sockopts.put(SctpSocketOption.SCTP_PEER_ADDR_PARAMS, params);

    opts.put(SctpServerOption.SOCKET_OPTIONS, sockopts);
    _chanel = factory.sctpAccept(reactor, local, me, opts);
    Thread.sleep(Integer.MAX_VALUE);
  }
  
  // --- TcpServerChannelListener interface --
  
  public void connectionAccepted(SctpServerChannel ssc, SctpChannel cnx) {
    if (false) {
      _logger.warn("refusing cnx");
      cnx.close();
      return;
    }
    _logger.warn("connection accepted on " + ssc + ":" + cnx);
    try {
      _logger.warn("association=" + cnx.getAssociation());
    } catch (IOException e) {
      _logger.warn("can't get association", e);
    }
    //cnx.send(false, null, 0, ByteBuffer.wrap("hi".getBytes()));
  }
  
  public void connectionFailed(SctpServerChannel serverChannel, Throwable err) {
    _logger.warn("connection failed on " + serverChannel, err);
  }
  
  public void serverConnectionClosed(SctpServerChannel ssc, Throwable err) {
    _logger.warn("server connection closed: " + ssc, err);
  }
  
  // Invoked when the connection is closed. 
  public void connectionClosed(SctpChannel cnx, Throwable err) {
    _logger.warn("TcpChannel closed: " + cnx, err);
    _chanel.close();
  }
  
  // Handle an incoming message. 
  public void messageReceived(SctpChannel cnx, ByteBuffer msg, SocketAddress addr, int bytes,
                              boolean isComplete, boolean isUnordered, int ploadPID, int streamNumber) {
    _logger.warn("message received: remaining=" + msg.remaining());
    byte[] b = new byte[msg.remaining()];
    msg.get(b);
    _logger.warn(new String(b));

    try {
      sctp_paddrparams v = cnx.getSocketOption(SctpSocketOption.SCTP_PEER_ADDR_PARAMS, null);
      System.out.println(v);
    } catch(IOException e) { _logger.warn(e); } 
    
    //_logger.warn("gracefully closing");
    //cnx.close();
  }
  
  // Called if we are using timers. 
  public void receiveTimeout(TcpChannel cnx) {
    _logger.warn("Message timeout");
  }
  
  public void writeBlocked(TcpChannel cnx) {
    _logger.warn("Write blocked");
  }
  
  public void writeUnblocked(TcpChannel cnx) {
    _logger.warn("Write unblocked");
  }
  
  public void sendFailed(SctpChannel cnx, SocketAddress addr, ByteBuffer buf, int errcode, int streamNumber) {
    _logger.warn("sendFailed: addr=" + addr + ", buf=" + buf + ", errocde=" + errcode + ", stream="
        + streamNumber);
  }
  
  public void shutdown(SctpChannel cnx) {
    _logger.warn("shutdown");
    cnx.close();
  }
  
  @Override
  public void writeBlocked(SctpChannel cnx) {
    _logger.warn("write blocked");
  }
  
  @Override
  public void writeUnblocked(SctpChannel cnx) {
    _logger.warn("write unblocked");
  }
  
  @Override
  public void receiveTimeout(SctpChannel cnx) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void peerAddressChanged(SctpChannel cnx, SocketAddress addr, AddressEvent event) {
    _logger.warn("Peer address change event: addr="  + addr + ", event=" + event);
  }
}
