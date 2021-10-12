// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.diameter.lb.impl.test;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.net.*;
import java.nio.*;

import com.alcatel.as.diameter.lb.*;
import com.alcatel.as.diameter.lb.impl.*;

import org.apache.log4j.Logger;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;

@Component(service={TcpServerProcessor.class}, property={"processor.id=diameter.test.gload"}, immediate=true)
public class GloadTestServer implements TcpServerProcessor {

    public static final Logger LOGGER = Logger.getLogger ("as.diameter.test.gload");
    
    public GloadTestServer (){
    }

    @Activate
    public void init (){
    }

    /**********************************************
     *           Server open/update/close         *
     **********************************************/
    public void serverCreated (TcpServer server){}
    public void serverDestroyed (TcpServer server){}
    public void serverOpened (TcpServer server){
	LOGGER.info ("serverStarted : "+server);
	if (checkServer (server) == false)
	    server.close ();
    }    
    public void serverFailed (TcpServer server, Object cause){
	LOGGER.debug ("serverFailed : "+server);
    }
    public void serverUpdated (TcpServer server){
	LOGGER.info ("serverUpdated : "+server);
	if (checkServer (server) == false)
	    server.close ();
    }
    public void serverClosed (TcpServer server){
	LOGGER.info ("serverClosed : "+server);
    }
    private boolean checkServer (Server server){
	return true;
    }
    
    /**********************************************
     *           connection mgmt                  *
     **********************************************/
    
    public void connectionAccepted(TcpServer server,
				   TcpChannel acceptedChannel,
				   Map<String, Object> props){
	LOGGER.info ("GloadServer : accepted : "+acceptedChannel);
	acceptedChannel.attach (new GloadConnection (acceptedChannel));
	acceptedChannel.enableReading ();
    }

    public TcpChannelListener getChannelListener (TcpChannel cnx){
	return (TcpChannelListener) cnx.attachment ();
    }

    private class GloadConnection implements TcpChannelListener {
	private TcpChannel _channel;
	private byte[] _header = new byte[8];
	private byte[] _data = _header;
	private int _read = 0;
	private GloadConnection (TcpChannel channel){
	    _channel = channel;
	}
	public void receiveTimeout(TcpChannel cnx){
	    Gload.PingMessage.write (cnx);
	}
	public int messageReceived(TcpChannel cnx,
				   java.nio.ByteBuffer buffer){
	    while (true){
		try{
		    Gload.Message msg = parse (buffer);
		    if (msg != null){
			if (LOGGER.isInfoEnabled ())
			    LOGGER.info ("GloadServer : received : "+msg);
			msg.received (cnx);
		    } else {
			break;
		    }
		}catch(Exception e){
		    LOGGER.warn ("GloadServer : Exception while reading message from Gload", e);
		    buffer.position (buffer.limit ());
		    cnx.shutdown ();
		}
	    }
	    return 0;
	}
	public void connectionClosed(TcpChannel cnx){
	}
	public void writeBlocked(TcpChannel cnx){
	    cnx.shutdown ();
	}
	public void writeUnblocked(TcpChannel cnx){
	}
	private Gload.Message parse (ByteBuffer buffer) throws Exception {
	    int available = buffer.remaining ();
	    int needed = _data.length - _read;
	    int willUse = Math.min (needed, available);
	    for (int i=0; i<willUse; i++){
		_data[_read + i] = buffer.get ();
	    }
	    _read += willUse;
	    if (_read == _data.length){
		if (_data == _header){
		    int len = (_header[6] & 0xFF) << 8;
		    len |= _header[7] & 0xFF;
		    if (len == 8){
			_read = 0;
			return Gload.messageRead (_data, true);
		    }
		    _data = new byte[len];
		    System.arraycopy (_header, 0, _data, 0, 8);
		    return parse (buffer);
		}
		try{
		    return Gload.messageRead (_data, true);
		}finally{
		    _read = 0;
		    _data = _header;
		}
	    }
	    return null;
	}
    }
}
