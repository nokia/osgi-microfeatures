package com.alcatel.as.ioh;

import java.util.*;
import java.net.*;
import java.nio.*;

import alcatel.tess.hometop.gateways.reactor.*;

public class UdpMessageProcessor<T> {

    private MessageParser<T> _parser;

    public UdpMessageProcessor (MessageParser<T> parser){
	_parser = parser;
    }

    public void writeBlocked (UdpChannel server){
    }

    public void writeUnblocked (UdpChannel server){
    }

    public void receiveTimeout (UdpChannel server){
    }
    
    public void messageReceived (UdpChannel server,
				 java.nio.ByteBuffer buffer,
				 InetSocketAddress from){
	MessageParser<T> parser = getMessageParser ();
	do{
	    T msg = parser.parseMessage (buffer);
	    if (msg != null)
		messageReceived (server, msg, from);
	    else
		break;
	} while (buffer.remaining () > 0);
    }
    
    public MessageParser<T> getMessageParser (){
	return _parser;
    }

    public void messageReceived (UdpChannel server, T msg, InetSocketAddress from){
    }
}