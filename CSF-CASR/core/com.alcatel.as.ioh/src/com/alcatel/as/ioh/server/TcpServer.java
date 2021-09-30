package com.alcatel.as.ioh.server;

import java.util.*;
import java.net.*;
import java.nio.*;

import com.alcatel.as.ioh.*;

import alcatel.tess.hometop.gateways.reactor.*;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface TcpServer extends Server {
	
    public static final String PROP_TCP_NO_DELAY = "tcp.nodelay";

    public static final String PROP_TCP_SECURE_DELAYED = "tcp.secure.delayed";
    public static final String PROP_TCP_SECURE_CIPHER = "tcp.secure.cipher";
    public static final String PROP_TCP_SECURE_CIPHER_SUITES_ORDER = "tcp.secure.cipher.suites.order";
    public static final String PROP_TCP_SECURE_PROTOCOL = "tcp.secure.protocol";
    public static final String PROP_TCP_SECURE_KEYSTORE_FILE = "tcp.secure.keystore.file";
    public static final String PROP_TCP_SECURE_KEYSTORE_PWD = "tcp.secure.keystore.pwd";
    public static final String PROP_TCP_SECURE_KEYSTORE_TYPE = "tcp.secure.keystore.type";
    public static final String PROP_TCP_SECURE_KEYSTORE_ALGO = "tcp.secure.keystore.algo";
    public static final String PROP_TCP_SECURE_ENDPOINT_IDENTITY_ALGO = "tcp.secure.endpoint.identity.algo";
    public static final String PROP_TCP_SECURE_KEYSTORE_WATCH = "tcp.secure.keystore.watch";
    public static final String PROP_TCP_SECURE_CLIENT_AUTHENTICATE = "tcp.secure.client.authenticate";
    public static final String PROP_TCP_SECURE_ALPN_PROTOCOL = "tcp.secure.alpn.protocol";
    public static final String PROP_TCP_SECURE_KEYEXPORT_LABEL = "tcp.secure.keyexport.label";
    public static final String PROP_TCP_SECURE_KEYEXPORT_LENGTH = "tcp.secure.keyexport.length";
    public static final String PROP_TCP_SECURE_SNI = "tcp.secure.sni";
    
    public InetSocketAddress getAddress ();
    
    public TcpServerChannel getServerChannel ();
}
