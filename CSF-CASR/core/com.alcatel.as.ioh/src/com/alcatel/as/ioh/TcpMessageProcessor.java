package com.alcatel.as.ioh;

import java.util.*;
import java.net.*;
import java.nio.*;

import alcatel.tess.hometop.gateways.reactor.*;

public class TcpMessageProcessor<T> implements TcpChannelListener {

    private MessageParser<T> _parser;

    public TcpMessageProcessor (MessageParser<T> parser){
	_parser = parser;
    }

    public void writeBlocked(TcpChannel cnx){
    }

    public void writeUnblocked(TcpChannel cnx){
    }

    public void receiveTimeout (TcpChannel cnx){
    }
    
    public int messageReceived (TcpChannel cnx,
				java.nio.ByteBuffer buffer){
	MessageParser<T> parser = getMessageParser ();
	while (true){
	    T msg = parser.parseMessage (buffer);
	    if (msg != null)
		messageReceived (cnx, msg);
	    else
		break;
	}
	return 0;
    }
    
    public MessageParser<T> getMessageParser (){
	return _parser;
    }

    public void messageReceived (TcpChannel cnx, T msg){
    }

    public void connectionClosed (TcpChannel cnx){
    }
}