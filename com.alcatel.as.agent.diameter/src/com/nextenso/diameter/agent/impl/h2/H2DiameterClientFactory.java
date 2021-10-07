package com.nextenso.diameter.agent.impl.h2;

import java.net.NoRouteToHostException;
import java.util.Hashtable;
import java.util.Map;
import java.net.URI;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.*;
import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.StringProperty;
import com.alcatel_lucent.as.management.annotation.config.FileDataProperty;
import com.nextenso.diameter.agent.impl.DiameterClientFactoryFacade;
import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterRequest;
import com.nextenso.proxylet.diameter.DiameterSession;
import com.nextenso.proxylet.diameter.client.DiameterClient;
import com.nextenso.proxylet.diameter.client.DiameterClientFactory;

import com.alcatel.as.http2.client.api.*;

/**
 * The H2DiameterClientFactory Implementation.
 */
@Component(service={}, configurationPolicy = ConfigurationPolicy.REQUIRE, immediate=true)
@Config
public class H2DiameterClientFactory extends DiameterClientFactory {

    public static final Logger LOGGER = Logger.getLogger("agent.diameter.client");

    @StringProperty(title = "Target URL", help = "The target URL of the Http request", section="General", required = true, dynamic = false, defval = "http://127.0.0.1:8080/services/diameter")
    public final static String CONF_H2_URI = "diameter.ioh.h2.uri";

    @FileDataProperty(title="Http2 Request Headers",
		      fileData="h2clientHeaders.txt",
		      required=true,
		      dynamic=false,
		      section="General",
		      help="Describes the http headers to add to the http2 request.")
    public final static String CONF_HEADERS = "h2client.headers";

    protected HttpClientFactory _h2clientF;
    protected HttpClient _h2client;
    protected URI _uri;
    protected Headers _headers;

    @Reference
    public void setH2ClientFactory (HttpClientFactory f){
	_h2clientF = f;
    }
    public HttpClient getH2Client (){ return _h2client;}
    public HttpClientFactory getH2ClientFactory (){ return _h2clientF;}
    public URI getH2URI (){ return _uri;}
    public Headers getHeaders (){ return _headers;}

    @Activate
    public void activate (BundleContext bc, Map<String, String> conf){
	String s = conf.get (CONF_H2_URI);
	if (s == null) s = "http://127.0.0.1:8080/services/diameter";
	try{
	    _uri = new URI (s);
	    LOGGER.warn ("H2DiameterClientFactory : CONFIGURATION : "+CONF_H2_URI+" : "+s);
	}	
	catch(Exception e){
	    LOGGER.error ("H2DiameterClientFactory : INVALID CONFIGURATION : "+CONF_H2_URI+" : invalid uri : "+s, new Exception ("Failed to initialize"));
	}
	_h2client = _h2clientF.newHttpClient ();
	try{
	    _headers = new Headers ().init ((String) conf.get (CONF_HEADERS));
	}catch(Exception e){
	    LOGGER.error (this+" : invalid configuration", e);
	}
		// ensure our service is registered AFTER the real diameter client.
		DiameterClientFactoryFacade.getInstance().setRegistrationListener(() -> {
			LOGGER.debug("registering H2 diameter client factory");
			Hashtable<String, Object> props = new Hashtable<>();
			props.put("client.id", "h2");
			props.put("service.ranking", new Integer(-10));
			bc.registerService(DiameterClientFactory.class, H2DiameterClientFactory.this, props);
		});
    }
    @Modified
    public void modified (Map<String, String> conf){
    }

    /**
     * @see com.nextenso.proxylet.diameter.client.DiameterClientFactory#newDiameterClient(java.lang.String,
     *      java.lang.String, long, long, boolean, int)
     */
    @Override
    public DiameterClient newDiameterClient(String destinationHost, String destinationRealm, long vendorId, long applicationId, boolean stateful,
					    int sessionLifetime)
	throws java.net.NoRouteToHostException {
	return newDiameterClient(destinationHost, destinationRealm, vendorId, applicationId, DiameterClient.TYPE_ALL, stateful, sessionLifetime);
    }

    /**
     * @see com.nextenso.proxylet.diameter.client.DiameterClientFactory#newDiameterClient(com.nextenso.proxylet.diameter.DiameterPeer,
     *      java.lang.String, java.lang.String, long, long, boolean, int)
     */
    @Override
    public DiameterClient newDiameterClient(DiameterPeer localPeer, String destinationHost, String destinationRealm, long vendorId, long applicationId,
					    boolean stateful, int sessionLifetime)
	throws NoRouteToHostException {
	return newDiameterClient(localPeer, destinationHost, destinationRealm, vendorId, applicationId, DiameterClient.TYPE_ALL, stateful, sessionLifetime);
    }

    /**
     * @see com.nextenso.proxylet.diameter.client.DiameterClientFactory#newDiameterClient(java.lang.String,
     *      java.lang.String, long, long, int, boolean, int)
     */
    @Override
    public DiameterClient newDiameterClient(String destinationHost, String destinationRealm, long vendorId, long applicationId, int type,
					    boolean stateful, int sessionLifetime)
	throws java.net.NoRouteToHostException {
	String handlerName = "";//Utils.getNextHandlerName();
	return new H2DiameterClient(this, handlerName, destinationHost, destinationRealm, vendorId, applicationId, type, stateful, sessionLifetime * 1000L);
    }

    /**
     * @see com.nextenso.proxylet.diameter.client.DiameterClientFactory#newDiameterClient(com.nextenso.proxylet.diameter.DiameterPeer,
     *      java.lang.String, java.lang.String, long, long, int, boolean, int)
     */
    @Override
    public DiameterClient newDiameterClient(DiameterPeer localPeer, String destinationHost, String destinationRealm, long vendorId, long applicationId,
					    int clientType, boolean stateful, int sessionLifetime)
	throws NoRouteToHostException {
	if (localPeer == null) {
	    throw new IllegalArgumentException("null local peer not supported");
	}
	String handlerName = "";//((Peer) localPeer).getHandlerName();
	return new H2DiameterClient(this, handlerName, destinationHost, destinationRealm, vendorId, applicationId, clientType, stateful, sessionLifetime * 1000L);
    }

    /**
     * @see com.nextenso.proxylet.diameter.client.DiameterClientFactory#newDiameterClient(java.lang.String,
     *      java.lang.String, com.nextenso.proxylet.diameter.DiameterSession)
     */
    @Override
    public DiameterClient newDiameterClient(String destinationHost, String destinationRealm, DiameterSession session)
	throws java.net.NoRouteToHostException {
	String handlerName = "";//Utils.getNextHandlerName();

	return new H2DiameterClient(this, handlerName, destinationHost, destinationRealm, session);
    }

    /**
     * @see com.nextenso.proxylet.diameter.client.DiameterClientFactory#newDiameterClient(java.lang.String,
     *      java.lang.String, long, long, int, java.lang.String, int)
     */
    @Override
    public DiameterClient newDiameterClient(String destinationHost, String destinationRealm, long vendorId, long applicationId, int type,
					    String sessionId, int sessionLifetime)
	throws NoRouteToHostException {
	String handlerName = "";//Utils.getNextHandlerName();
	return new H2DiameterClient(this, handlerName, destinationHost, destinationRealm, vendorId, applicationId, type, sessionId, sessionLifetime * 1000L);
    }

    /**
     * @see com.nextenso.proxylet.diameter.client.DiameterClientFactory#newDiameterClient(com.nextenso.proxylet.diameter.DiameterRequest)
     */
    @Override
    public DiameterClient newDiameterClient(DiameterRequest request)
	throws NoRouteToHostException {
	throw new RuntimeException ("Not implemented");
    }

}
