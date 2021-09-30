package alcatel.tess.hometop.gateways.reactor.examples;

// Jdk
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.ExecutorPolicy;

import com.alcatel.as.util.sctp.*;
import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.SctpChannel;
import alcatel.tess.hometop.gateways.reactor.SctpClientChannelListener;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.SctpClientOption;

/**
 * Open a tcp socket to the TestServer program (port 9999),
 * and send him "Hello".
 */
public class TestSctpClientOptions implements SctpClientChannelListener {
  static Logger _logger = Logger.getLogger("client");
  static volatile Reactor _reactor;
  
  public static void main(String args[]) throws IOException, InterruptedException {
    TestSctpClientOptions client = new TestSctpClientOptions();
    ReactorProvider factory = ReactorProvider.provider();
    
    _reactor = factory.create("Client");
    _reactor.start();
    SocketAddress local = new InetSocketAddress(args[0], 5000);
    //InetAddress[] secondaryLocals = new InetAddress[] { InetAddress.getByName(args[1]) };
    SocketAddress remote = new InetSocketAddress(args[2], 5500);
    
    Map<SctpClientOption, Object> opts = new HashMap<ReactorProvider.SctpClientOption, Object>();
    opts.put(SctpClientOption.SO_RCVBUF, new Integer(10000));
    opts.put(SctpClientOption.SO_SNDBUF, new Integer(20000));
    
    sctp_spp_flags flags = new sctp_spp_flags(true, false, true, true, false);
    sctp_paddrparams params = new sctp_paddrparams(new InetSocketAddress("127.0.0.1", 5500), 10000, 0, 0, 0, flags);
    Map<SctpSocketOption, SctpSocketParam> sockopts = new HashMap<>();
    sockopts.put(SctpSocketOption.SCTP_PEER_ADDR_PARAMS, params);

    opts.put(SctpClientOption.SOCKET_OPTIONS, sockopts);
    opts.put(SctpClientOption.LOCAL_ADDR, local);
    //opts.put(SctpClientOption.SECONDARY_LOCAL_ADDRS, secondaryLocals);    
    
    factory.sctpConnect(_reactor, remote, client, opts);
    Thread.sleep(Integer.MAX_VALUE);
  }
  
  @Override
  public void connectionEstablished(final SctpChannel cnx) {
    try {
      _logger.warn("connection established: remotePort=" + cnx.getRemotePort() + ", locals="
          + cnx.getLocalAddresses() + ", remotes=" + cnx.getRemoteAddresses());
      _logger.warn("new association=" + cnx.getAssociation());
      
      _reactor.scheduleAtFixedRate(new Runnable() {
        public void run() {
          System.out.println("sending ...");
          cnx.send(false, null, 0, ByteBuffer.wrap("hello".getBytes()));
          try {
      sctp_paddrparams v = cnx.getSocketOption(SctpSocketOption.SCTP_PEER_ADDR_PARAMS, null);
      System.out.println(v);
    } catch(IOException e) { _logger.warn(e); } 
        }
      }, 0L, 1000L, TimeUnit.MILLISECONDS);      
    } catch (IOException e) {
      _logger.warn(e);
    }
  }
  
  @Override
  public void connectionFailed(SctpChannel cnx, Throwable error) {
    _logger.warn("connection failed");
  }
  
  @Override
  public void messageReceived(SctpChannel cnx, ByteBuffer msg, SocketAddress addr, int bytes,
                              boolean isComplete, boolean isUnordered, int ploadPID, int streamNumber) {
    _logger.warn("message received: remaining=" + msg.remaining());
    byte[] b = new byte[msg.remaining()];
    msg.get(b);
    _logger.warn(new String(b));       
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
  public void connectionClosed(SctpChannel cnx, Throwable err) {
    _logger.warn("connection Closed", err);
  }
  
  public void sendFailed(SctpChannel cnx, SocketAddress addr, ByteBuffer buf, int errcode, int streamNumber) {
    _logger.warn("sendFailed: addr=" + addr + ", buf=" + buf + ", errocde=" + errcode + ", stream="
        + streamNumber);
  }
  
  public void shutdown(SctpChannel cnx) {
    _logger.warn("shutdown");
    //cnx.close();
  }
  
  @Override
  public void receiveTimeout(SctpChannel cnx) {
  }

  @Override
  public void peerAddressChanged(SctpChannel cnx, SocketAddress addr, AddressEvent event) {
    _logger.warn("Peer address change event: addr="  + addr + ", event=" + event);
  }
}
