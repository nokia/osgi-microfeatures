package com.alcatel.as.ioh.impl.tools;

import java.nio.*;
import java.util.*;

import com.alcatel.as.ioh.server.*;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import alcatel.tess.hometop.gateways.reactor.*;

@Component(service = { TcpServerProcessor.class, UdpServerProcessor.class, SctpServerProcessor.class }, property = { "processor.id=ioh.echo"}, immediate = true)
public class EchoProcessor implements TcpServerProcessor, UdpServerProcessor, SctpServerProcessor {

    public void serverCreated(TcpServer server) {
    }

    public void serverDestroyed(TcpServer server) {
    }

    public void serverOpened(TcpServer server) {
    }

    public void serverFailed(TcpServer server, java.lang.Object cause) {
    }

    public void serverUpdated(TcpServer server) {
    }

    public void serverClosed(TcpServer server) {
    }

    public void connectionAccepted(TcpServer server, TcpChannel client,
				   java.util.Map<java.lang.String, java.lang.Object> props) {
	TcpChannelListener l = new TcpChannelListener (){
		public int messageReceived (TcpChannel channel, ByteBuffer data){
		    ByteBuffer copy = ByteBuffer.allocate (data.remaining ());
		    copy.put (data);
		    copy.flip ();
		    channel.send (copy, false);
		    return 0;
		}
    
		public void receiveTimeout (TcpChannel channel){}
		public void writeBlocked (TcpChannel channel){}
		public void writeUnblocked (TcpChannel channel){}
		public void connectionClosed(TcpChannel cnx){}
	    };
	client.attach(l);
	client.enableReading ();
    }
    
    public TcpChannelListener getChannelListener(TcpChannel channel) {
	return (TcpChannelListener) channel.attachment();
    }

    public void serverCreated(SctpServer server){
    }
    public void serverDestroyed(SctpServer server){
    }
    public void serverOpened(SctpServer server){
    }
    public void serverFailed(SctpServer server,
		      java.lang.Object cause){
    }
    public void serverUpdated(SctpServer server){
    }
    public void serverClosed(SctpServer server){
    }
    public void connectionAccepted(SctpServer server,
				   SctpChannel client,
				   java.util.Map<java.lang.String,java.lang.Object> props){
	SctpChannelListener l = new SctpChannelListener (){
		public void messageReceived(SctpChannel cnx, java.nio.ByteBuffer data, java.net.SocketAddress addr, int bytes, boolean isComplete, boolean isUnordered, int ploadPID, int streamNumber){
		    ByteBuffer copy = ByteBuffer.allocate (data.remaining ());
		    copy.put (data);
		    copy.flip ();
		    cnx.send (false, null, 0, copy);
		}
		public void peerAddressChanged(SctpChannel cnx,
					       java.net.SocketAddress addr,
					       SctpChannelListener.AddressEvent event){
		}
		public void sendFailed(SctpChannel cnx,
				       java.net.SocketAddress addr,
				       java.nio.ByteBuffer buf,
				       int errcode,
				       int streamNumber){
		}
		public void receiveTimeout (SctpChannel channel){}
		public void writeBlocked (SctpChannel channel){}
		public void writeUnblocked (SctpChannel channel){}
		public void connectionClosed(SctpChannel cnx, Throwable t){}
	    };
	client.attach(l);
	client.enableReading ();
    }
    public SctpChannelListener getChannelListener(SctpChannel channel){
	return (SctpChannelListener) channel.attachment();
    }

    
    public void serverCreated(UdpServer server){}
    public void serverDestroyed(UdpServer server){}
    public void serverOpened(UdpServer server){
	server.getServerChannel ().enableReading ();
    }
    public void serverFailed(UdpServer server,
		      java.lang.Object cause){}
    public void serverUpdated(UdpServer server){}
    public void serverClosed(UdpServer server){}
    public UdpChannelListener getChannelListener(UdpChannel cnx){
	return new UdpChannelListener (){
	    public void connectionOpened(UdpChannel cnx){}
	    public void connectionFailed(UdpChannel cnx,
					 java.lang.Throwable err){}
	    public void connectionClosed(UdpChannel cnx){}
	    public void messageReceived(UdpChannel cnx,
					java.nio.ByteBuffer msg,
					java.net.InetSocketAddress addr){
		ByteBuffer copy = ByteBuffer.allocate (msg.remaining ());
		copy.put (msg);
		copy.flip ();
		cnx.send (addr, false, copy);
	    }
	    public void receiveTimeout(UdpChannel cnx){}
	    public void writeBlocked(UdpChannel cnx){}
	    public void writeUnblocked(UdpChannel cnx){}
	};
    }
    
}
