package com.alcatel.as.http2.client;

import org.osgi.annotation.versioning.ProviderType;
import java.util.concurrent.Executor;
import java.util.Map;

@ProviderType
public interface Http2Client {

    public static final String PROP_TCP_CONNECT_TIMEOUT = "tcp.connect.timeout";
    public static final String PROP_TCP_CONNECT_SRC = "tcp.connect.src";

    public static final String PROP_CLIENT_INIT_DELAY = "client.init.delay";
    public static final String PROP_CLIENT_LOGGER = "client.logger";

    public static final String PROP_TCP_NO_DELAY = "tcp.nodelay";
    public static final String PROP_TCP_SECURE = "tcp.secure";
    public static final String PROP_TCP_SECURE_CIPHER = "tcp.secure.cipher";
    public static final String PROP_TCP_SECURE_PROTOCOL = "tcp.secure.protocol";
    public static final String PROP_TCP_SECURE_KEYSTORE_FILE = "tcp.secure.keystore.file";
    public static final String PROP_TCP_SECURE_KEYSTORE_PWD = "tcp.secure.keystore.pwd";
    public static final String PROP_TCP_SECURE_KEYSTORE_TYPE = "tcp.secure.keystore.type";
    public static final String PROP_TCP_SECURE_KEYSTORE_ALGO = "tcp.secure.keystore.algo";
    public static final String PROP_TCP_SECURE_ENDPOINT_IDENTITY_ALGO = "tcp.secure.endpoint.identity.algo";
    public static final String PROP_TCP_SECURE_SNI = "tcp.secure.sni";

    public static final String PROP_EXECUTOR_READ = "executor.read";
    public static final String PROP_EXECUTOR_WRITE = "executor.write";

    public static final String PROP_HTTP2_CONFIG = "http2.config";

    public static final String PROP_REACTOR_MUX = "reactor.mux";

    public static final String PROP_TCP_SECURE_KEYEXPORT_LABEL = "tcp.secure.keyexport.label";
    public static final String PROP_TCP_SECURE_KEYEXPORT_LENGTH = "tcp.secure.keyexport.length";
    public static final String PROP_TCP_SECURE_KEYEXPORT_CONTEXT = "tcp.secure.keyexport.context";
    
    /**
     * The proxy ip as a String or as an InetAddress
     */
    public static final String PROP_PROXY_IP = "proxy.ip";
    /**
     * The proxy port as a String or as an Integer.
     */
    public static final String PROP_PROXY_PORT = "proxy.port";
    /**
     * The proxy address as an InetSocketAddress
     */
    public static final String PROP_PROXY_ADDRESS = "proxy.address";

    public static final String PROP_PROXY_SECURE = "proxy.secure";
    public static final String PROP_PROXY_SECURE_CIPHER = "proxy.secure.cipher";
    public static final String PROP_PROXY_SECURE_PROTOCOL = "proxy.secure.protocol";
    public static final String PROP_PROXY_SECURE_KEYSTORE_FILE = "proxy.secure.keystore.file";
    public static final String PROP_PROXY_SECURE_KEYSTORE_PWD = "proxy.secure.keystore.pwd";
    public static final String PROP_PROXY_SECURE_KEYSTORE_TYPE = "proxy.secure.keystore.type";
    public static final String PROP_PROXY_SECURE_KEYSTORE_ALGO = "proxy.secure.keystore.algo";
    public static final String PROP_PROXY_SECURE_ENDPOINT_IDENTITY_ALGO = "proxy.secure.endpoint.identity.algo";
    public static final String PROP_PROXY_SECURE_ENDPOINT_IDENTITY_NAME = "proxy.secure.endpoint.identity.name";
    
    public void newHttp2Connection (java.net.InetSocketAddress dest,
				    java.util.function.Consumer<Http2Connection> onSuccess,
				    Runnable onFailure,
				    Runnable onClose,
				    Map<String, Object> props);
}
