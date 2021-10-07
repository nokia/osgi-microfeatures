package com.alcatel_lucent.as.agent.web.muxhandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.eclipse.jetty.io.Connection;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel_lucent.as.agent.web.container.Container;
import com.alcatel_lucent.as.service.jetty.common.connector.AbstractBufferEndPoint;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxHeaderV0;
import com.nextenso.mux.socket.Socket;

public class WebAgentSocket implements Socket, WebAgentSocketInterface {
  
  private MuxConnection connection;
  private int id, remotePort;
  private String remoteIp;
  private boolean secure;
  private EndPoint endPoint;
  private InetSocketAddress localIp;

  public class EndPoint extends AbstractBufferEndPoint {

    public EndPoint(PlatformExecutor tpExecutor) {
      super(new InetSocketAddress(remoteIp, remotePort), WebAgentSocket.this, WebAgentSocket.this.secure, tpExecutor);
    }
    
    @Override
    public void upgrade(Connection newConnection) {
      // TODO this method is called when a 101 switching protocol has been called. however what we need is to detect
      // protocol switch before the 101 is actually sent to the ioh. So, here, we send a protocol switch mux message after 
      // the 101 has been sent but we have to find out another solution that allows to send the mux switch before the 101 
      // is sent out.
  	  prepareUpgrade();
  	  super.upgrade(newConnection);
    }
    
    @Override
    public Object getTransport() {
    	return connection;
    }
    
    @Override
    public void prepareUpgrade() {
      if (Agent.LOGGER.isDebugEnabled()) Agent.LOGGER.debug("prepareUpgrade on "+this); 
      WebAgentSocket.this.prepareUpgrade();
    }

    public void clientData(byte[] data, int off, int len) {
      try {
        byte[] copy = new byte[len];
        System.arraycopy(data, off, copy , 0, len);
        input.write(copy, 0, len);
        input.flush();
      }
      catch (IOException e) {
        if (Agent.LOGGER.isDebugEnabled()) Agent.LOGGER.debug("cannot handle client data", e);
      }
    }


	@Override
    public InetSocketAddress getLocalAddress() {
	   return localIp;
	}
	  

	@Override
    public InetSocketAddress getRemoteAddress() {
	    return new InetSocketAddress(remoteIp, remotePort);
	}
  }
  
  public WebAgentSocket(MuxConnection connection, int id, String remoteIp, int remotePort, 
      String localIp, int localPort, boolean secure, PlatformExecutor tpExecutor) {
    this.connection = connection;
    this.id = id;
    this.remoteIp = remoteIp;
    this.remotePort = remotePort;
    this.localIp = new InetSocketAddress(localIp, localPort);
    this.secure = secure;
    this.endPoint = new EndPoint(tpExecutor);
  }

  public EndPoint getEndPoint() {
    return endPoint;
  }
    
  public void serverData(ByteBuffer[] bufs) {
    connection.sendTcpSocketData(id, true, bufs);    
  }

  public void closedByServer() {
    connection.sendTcpSocketClose(id);
  }
  
  public void prepareUpgrade() {
    MuxHeaderV0 hdr = new MuxHeaderV0();
    hdr.set(-1, getSockId(), 0x4A /* SWITCH-PORTOCOL */);
    connection.sendMuxData(hdr, true);
  }

  /*-- WebAgentSocketInterface ----------------------*/

  @Override
  public void received(byte[] data, int off, int len) {
    endPoint.clientData(data, off, len);    
  }
  
  @Override
  public void closed(Container container) {
    container.removeClient(this);    
  }

  /*-- MUX Socket ----------------------*/
  
  @Override
  public boolean close() {
    return false;
  }

  @Override
  public int getLocalIP() {
    return localIp.getAddress().hashCode();
  }

  @Override
  public String getLocalIPString() {
    return localIp.getHostString();
  }

  @Override
  public int getLocalPort() {
    return localIp.getPort();
  }

  @Override
  public int getSockId() {
    return id;
  }

  @Override
  public int getType() {
    return Socket.TYPE_TCP;
  }
  
  @Override
  public String toString() {
    return "WebAgentSocket [id=" + id + ", remoteIp=" + remoteIp + ", secure=" + secure + "]";
  }

}
