package com.alcatel.as.diameter.lb.impl.test;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.net.*;

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

@Component(service={TcpServerProcessor.class}, property={"processor.id=diameter.test.server"}, immediate=true)
public class DiameterTestServer implements TcpServerProcessor {

    public static final Logger LOGGER = Logger.getLogger ("as.diameter.test.server");
    
    public DiameterTestServer (){
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
	server.getProperties ().put ("SERVER_IN_REQUESTS", new AtomicLong (0));
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
    public String[][] getInfo (TcpServer server, String key){
	boolean reset = (key != null && key.indexOf ("reset") > -1);
	AtomicLong SERVER_IN_REQUESTS = (AtomicLong) server.getProperties ().get ("SERVER_IN_REQUESTS");
	String[][] info = new String[2][];
	info[0] = new String[]{"SERVER_IN_REQUESTS"};
	info[1] = new String[]{String.valueOf (SERVER_IN_REQUESTS.get ())};
	if (reset) SERVER_IN_REQUESTS.set (0L);
	return info;
    }
    
    /**********************************************
     *           connection mgmt                  *
     **********************************************/
    
    public void connectionAccepted(TcpServer server,
				   TcpChannel acceptedChannel,
				   Map<String, Object> props){
	acceptedChannel.attach (new TestContext (props));
	acceptedChannel.enableReading ();
    }

    public TcpChannelListener getChannelListener (TcpChannel cnx){
	return (TcpChannelListener) cnx.attachment ();
    }
}
