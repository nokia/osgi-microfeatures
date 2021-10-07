package com.alcatel.as.diameter.lb;

import java.util.*;
import java.util.concurrent.atomic.*;
import org.apache.log4j.Logger;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.tools.*;
import com.alcatel.as.ioh.client.TcpClient.Destination;

import alcatel.tess.hometop.gateways.reactor.*;

import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.service.metering2.*;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface DiameterClient {

    public final static String APP_NAME = "DiameterLB";

    public String getDiameterId ();

    public boolean isOpen ();

    public Logger getLogger ();
    
    public PlatformExecutor getExecutor ();

    public SimpleMonitorable getMonitorable ();

    public SimpleMonitorable getMonitorable (Destination server);

    public AsyncChannel getClientChannel ();

    public Map<String, Object> getProperties ();

    public void attach (Object[] attachment);
    
    public Object[] attachment ();

    public <T> T attachment (int index);

    public DestinationManager getDestinationManager ();
    public DestinationManager getDestinationManager (String group);
    public DestinationManager getDestinationManager (int hashcode);
    
    public void sendToClient (DiameterMessage msg);

    public void sendToServer (Destination toServer, DiameterMessage msg);

}
