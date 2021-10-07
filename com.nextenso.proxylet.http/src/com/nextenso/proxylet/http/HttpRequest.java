package com.nextenso.proxylet.http;

import java.net.InetSocketAddress;
import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This Class encapsulates a request.
 */
@ProviderType
public interface HttpRequest extends HttpMessage {

    /**
     * A possible value for the next hop: specifies that the connection is direct (no proxy).
     */
    public static final String NEXT_HOP_DIRECT = "next_hop_direct";
    
    /**
     * A possible value for the next hop: specifies that the default value should be used.
     */
    public static final String NEXT_HOP_DEFAULT = "next_hop_default";
    
    /**
     * overloaded timeout for this request
     */
    public static final String TIMEOUT_ATTR_KEY = "__timeout";
    
    /**
     * 
     */
    @Deprecated
    public static final String PROXY_TRANSPARENT_ATTR_KEY = "__proxy_transp";
        
    /**
     * Returns the request prolog.
     * @return the prolog.
     */
    public HttpRequestProlog getProlog ();

    /**
     * Sets the next hop to use for this specific request.
     * <br/>It must be set before the headers are sent over the network to be effective.
     * <br/>The format is <i>host:port</i>.
     * <br/>The special values NEXT_HOP_DIRECT and NEXT_HOP_DEFAULT can be used.
     * @param nextHop the next hop <i>host:port</i>.
     */
    public void setNextHop (String nextHop);

    /**
     * Returns the value of the next hop.
     * <br/>The special values NEXT_HOP_DIRECT and NEXT_HOP_DEFAULT can be returned.
     * @return the next hop.
     */
    public String getNextHop ();
    
    
    /**
     * Set the next proxy server to be used for this request.
     * <br/> Overrides setNextHop and setNextServer
     * <br/> It must be set before the headers are sent over the network to be effective.
     * <br/> pass null to clear the value
     * @param addr the address for the next proxy to use
     */
    public void setNextProxy(InetSocketAddress addr);
    
    /**
     * Force the the request to be sent to this server, regardless of the request
     * URI.
     * <br/>It must be set before the headers are sent over the network to be effective.
     * <br/> pass null to clear the value
     * @param addr the server to connect to
     */
    public void setNextServer(InetSocketAddress addr);
    
    /**
     * Get the value for the nextProxy set using setNextProxy 
     * @return the next proxy server address or Optional.EMPTY if not set
     */
    public Optional<InetSocketAddress> getNextProxy();
    
    /**
     * Get the value for the next server address set using setNextServer 
     * @return the next server address or Optional.EMPTY if not set
     */
    public Optional<InetSocketAddress> getNextServer();
    
}
