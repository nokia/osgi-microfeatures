package com.alcatel.as.ioh.lb.plugins;

import com.alcatel.as.ioh.lb.*;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.net.*;
import java.nio.ByteBuffer;

import com.alcatel.as.ioh.client.TcpClient.Destination;
import com.alcatel.as.ioh.client.UdpClient;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ConfigurationPolicy;

@Component(immediate=true, configurationPolicy = ConfigurationPolicy.OPTIONAL, property={"router.id=neo-snmp"})
public class NeoSnmpRouterFactory implements RouterFactory {

    public static final String PROP_REMOTE_IP_VERSION = "router.add.remote.ip.version";
    public static final String PROP_REMOTE_PORT = "router.add.remote.port";

    @Activate
    public void start (){
    }

    public Object newRouterConfig (Map<String, Object> props){
	return new NeoSnmpConfig (props).init ();
    }

    public Router newRouter (Object config){
	return ((NeoSnmpConfig) config)._router;
    }

    public String toString (){ return "NeoSnmpRouterFactory[id=neo-snmp]";}

    protected static class NeoSnmpConfig {
	protected int _maxSendBuffer;
	protected NeoSnmpRouter _router;
	protected boolean _remotePort;
	protected boolean _remoteIPVersion;
	protected int _remoteExtra;

	protected NeoSnmpConfig (Map<String, Object> props){
	    String s = (String) props.get (LoadBalancer.PROP_DEST_WRITE_BUFFER_MAX);
	    _maxSendBuffer = s != null ? Integer.parseInt (s) : Integer.MAX_VALUE;
	    s = (String) props.get (PROP_REMOTE_IP_VERSION);
	    if (s != null) _remoteIPVersion = Boolean.parseBoolean (s);
	    s = (String) props.get (PROP_REMOTE_PORT);
	    if (s != null) _remotePort = Boolean.parseBoolean (s);
	    if (_remoteIPVersion) _remoteExtra++;
	    if (_remotePort) _remoteExtra+=2;
	}
	protected NeoSnmpConfig init (){
	    _router = new NeoSnmpRouter (this);
	    return this;
	}
    }

    protected static class NeoSnmpRouter extends RoundRobinRouterFactory.RoundRobinRouter {

	private NeoSnmpConfig _config;

	protected NeoSnmpRouter (NeoSnmpConfig config){
	    super ();
	    _maxSendBuffer = config._maxSendBuffer;
	    _config = config;
	}
	public int neededBuffer (){ return 0;}
	
	public void route (Client client, Chunk chunk){
	    throw new IllegalStateException ("Not designed for TCP");
	}

	public void route (UdpClientContext client, Chunk chunk){
	    InetSocketAddress remoteAddr = client.getRemoteAddress ();
	    byte[] remoteIP = remoteAddr.getAddress ().getAddress ();
	    int remotePort = remoteAddr.getPort ();
	    ByteBuffer origData = chunk.getData ();
	    ByteBuffer newData = ByteBuffer.allocate (remoteIP.length + origData.remaining () + _config._remoteExtra);
	    if (_config._remoteIPVersion){
		if (remoteAddr.getAddress () instanceof Inet4Address)
		    newData.put ((byte)4);
		else
		    newData.put ((byte)6);
	    }
	    newData.put (remoteIP);
	    if (_config._remotePort){
		newData.put ((byte) (remotePort >> 8));
		newData.put ((byte) remotePort);
	    }
	    newData.put (origData);
	    newData.flip ();
	    // need to handle the origData properly : sendToDestination(null) will compact it
	    client.sendToDestination (null, chunk);
	    
	    chunk.setData (newData, true);
	    super.route (client, chunk);
	}

    }
    
}
