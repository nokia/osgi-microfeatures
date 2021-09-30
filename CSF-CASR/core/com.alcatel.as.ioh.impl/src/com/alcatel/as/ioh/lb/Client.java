package com.alcatel.as.ioh.lb;

import org.apache.log4j.Logger;
import java.util.List;
import java.nio.ByteBuffer;
import com.alcatel.as.ioh.client.TcpClient;
import com.alcatel.as.ioh.client.TcpClient.Destination;
import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.service.metering2.*;

public interface Client {

    public long getId ();

    public Logger getLogger ();
    
    public PlatformExecutor getExecutor ();

    public Meters getMeters ();

    public AsyncChannel getChannel ();

    public java.util.Map<String, Object> getProperties ();

    public void attach (Object[] attachment);
    
    public Object[] attachment ();

    public <T> T attachment (int index);

    public TcpClient getTcpClient ();
    public List<Destination> getOpenDestinations ();
    
    public void sendToClient (boolean copy, java.nio.ByteBuffer data);
    public void sendToClient (Destination fromServer, Chunk chunk);

    public void sendToDestination (Destination toServer, Chunk chunk);
    public void sendToDestination (Destination toServer, boolean copy, ByteBuffer data);
}
