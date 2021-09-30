package com.alcatel.as.diameter.lb;

import com.alcatel.as.ioh.client.TcpClient.Destination;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface DestinationManager {

    public Destination getAny ();

    public Destination get (int hash);
}