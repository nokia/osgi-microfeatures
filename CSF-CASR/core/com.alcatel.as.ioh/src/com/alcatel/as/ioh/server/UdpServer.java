package com.alcatel.as.ioh.server;

import java.util.*;
import java.net.*;
import java.nio.*;

import com.alcatel.as.ioh.*;

import alcatel.tess.hometop.gateways.reactor.*;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface UdpServer extends Server {

    public static final String PROP_UDP_SECURE_DELAYED = "udp.secure.delayed";
    public static final String PROP_UDP_SECURE_CIPHER = "udp.secure.cipher";
    public static final String PROP_UDP_SECURE_CIPHER_SUITES_ORDER = "udp.secure.cipher.suites.order";
    public static final String PROP_UDP_SECURE_PROTOCOL = "udp.secure.protocol";
    public static final String PROP_UDP_SECURE_KEYSTORE_FILE = "udp.secure.keystore.file";
    public static final String PROP_UDP_SECURE_KEYSTORE_PWD = "udp.secure.keystore.pwd";
    public static final String PROP_UDP_SECURE_KEYSTORE_TYPE = "udp.secure.keystore.type";
    public static final String PROP_UDP_SECURE_KEYSTORE_ALGO = "udp.secure.keystore.algo";
    public static final String PROP_UDP_SECURE_ENDPOINT_IDENTITY_ALGO = "udp.secure.endpoint.identity.algo";
    public static final String PROP_UDP_SECURE_KEYSTORE_WATCH = "udp.secure.keystore.watch";
    public static final String PROP_UDP_SECURE_CLIENT_AUTHENTICATE = "udp.secure.client.authenticate";

    public InetSocketAddress getAddress ();
    
    public UdpChannel getServerChannel ();
}
