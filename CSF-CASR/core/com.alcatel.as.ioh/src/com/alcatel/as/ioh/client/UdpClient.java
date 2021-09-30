package com.alcatel.as.ioh.client;

import java.util.*;
import java.net.*;
import java.nio.*;

import alcatel.tess.hometop.gateways.reactor.*;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface UdpClient extends Client {

    public static final String PROP_HEALTHCHECK_DATA = "healthcheck.data";
    public static final String PROP_HEALTHCHECK_DATA_SUCCESS = "healthcheck.data.success";
    public static final String PROP_HEALTHCHECK_DELAY = "healthcheck.delay";
    public static final String PROP_HEALTHCHECK_TIMEOUT = "healthcheck.timeout";
    public static final String PROP_HEALTHCHECK_PORT = "healthcheck.port";
    public static final String PROP_HEALTHCHECK_PORT_DIFF = "healthcheck.port.diff";
    
    public static final String PROP_BIND_IP = "bind.ip";
    public static final String PROP_BIND_PORT = "bind.port";

    public static final String PROP_WRITE_BUFFER = "write.buffer";
    public static final String PROP_READ_BUFFER = "read.buffer";
    
    public UdpClient addDestinations (List<InetSocketAddress> destAddresses);

    public Destination addDestination (InetSocketAddress destAddress, Object attachment, Map<String, Object> props);

    public Destination addDestination (Destination destination);

    public UdpClient removeDestination (Destination destination);

    public Endpoint newEndpoint (UdpChannelListener listener, InetSocketAddress addr, Object attachment, Map<String, Object> props);

    //public UdpChannel getChannel ();
    
    public List<Destination> getDestinations ();

    //public int sendAll (byte[] data, boolean availableOnly);

    //public void disableHealthcheck ();

    //public void enableHealthcheck ();

    //public void execute (Runnable r);

    public interface Endpoint {

	public UdpClient getUdpClient ();
	
	public UdpChannel getChannel ();

	public UdpChannelListener getListener ();

	public Map<String, Object> getProperties ();
	
	public <T> T attach (Object attachment);
	
	public <T> T attachment ();

	public boolean send (Destination dest, ByteBuffer data, boolean copy);
	
	public void close ();

	public void execute (Runnable r);
    }
    
    public interface Destination {

	public UdpClient getUdpClient ();

	public InetSocketAddress getRemoteAddress ();

	public void close ();
	
	public Map<String, Object> getProperties ();

	public <T> T attach (Object attachment);
	
	public <T> T attachment ();
	
	public boolean send (Endpoint endpoint, ByteBuffer data, boolean copy);
    }

}
