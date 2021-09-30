package com.alcatel.as.ioh;

import java.net.SocketAddress;

import alcatel.tess.hometop.gateways.reactor.SctpChannel;
import alcatel.tess.hometop.gateways.reactor.SctpChannelListener;

public class SctpMessageProcessor<T> implements SctpChannelListener {

    private MessageParser<T> _parser;

    public SctpMessageProcessor (MessageParser<T> parser){
	_parser = parser;
    }

    public void writeBlocked(SctpChannel cnx){
    }

    public void writeUnblocked(SctpChannel cnx){
    }

    public void receiveTimeout (SctpChannel cnx){
    }
    
    public void messageReceived(SctpChannel cnx,
				java.nio.ByteBuffer buffer,
				java.net.SocketAddress addr,
				int bytes,
				boolean isComplete,
				boolean isUnordered,
				int ploadPID,
				int streamNumber){
	MessageParser<T> parser = getMessageParser ();
	while (true){
	    T msg = parser.parseMessage (buffer);
	    if (msg != null)
		messageReceived (cnx, msg);
	    else
		break;
	}
    }
    
    public void connectionClosed(SctpChannel cnx,
				 java.lang.Throwable err){
    }

    public void sendFailed(SctpChannel cnx,
			   java.net.SocketAddress addr,
			   java.nio.ByteBuffer buf,
			   int errcode,
			   int streamNumber){
    }
    
    public MessageParser<T> getMessageParser (){
	return _parser;
    }

    public void messageReceived (SctpChannel cnx, T msg){
    }

    @Override
    public void peerAddressChanged(SctpChannel cnx, SocketAddress addr, AddressEvent event) {
    }
}
