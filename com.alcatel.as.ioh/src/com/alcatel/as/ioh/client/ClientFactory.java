package com.alcatel.as.ioh.client;

import java.util.*;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface ClientFactory {

    TcpClient newTcpClient (String id, Map<String, Object> props);

    void newTcpClientConfig (String xml);
    
    UdpClient newUdpClient (String id, Map<String, Object> props);

    void newUdpClientConfig (String xml);

}
