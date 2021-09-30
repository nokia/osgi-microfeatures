package com.nextenso.diameter.agent.impl.h2;

import com.nextenso.diameter.agent.impl.DiameterMessageFacade;
import com.nextenso.diameter.agent.impl.DiameterResponseFacade;
import com.nextenso.diameter.agent.impl.DiameterRequestFacade;
import com.nextenso.diameter.agent.impl.DiameterSessionFacade;
import com.nextenso.diameter.agent.DiameterProperties;
import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterResponse;
import com.nextenso.proxylet.diameter.DiameterSession;
import com.nextenso.proxylet.diameter.client.DiameterClient;
import com.nextenso.proxylet.diameter.client.DiameterClientListener;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;
import com.nextenso.proxylet.ProxyletData;
import com.nextenso.diameter.agent.Utils;
import com.nextenso.diameter.agent.peer.PeerSocket;
import java.util.concurrent.Executor;
import alcatel.tess.hometop.gateways.utils.ByteOutputStream;
import com.alcatel.as.http2.client.api.*;
import java.util.concurrent.CompletableFuture;

import java.io.IOException;
import java.net.NoRouteToHostException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.nextenso.diameter.agent.impl.h2.H2DiameterClient.LOGGER;

public class H2DiameterClientRequest extends DiameterMessageFacade implements DiameterClientRequest {

    private H2DiameterClient _h2dclient;
    
    public static final int NO_FLAG = 0x00;
    public static final int REQUEST_FLAG = 0x80;
    public static final int PROXIABLE_FLAG = 0x40;
    public static final int RETRANSMITTED_FLAG = 0x10;
    public static final int RP_FLAGS = REQUEST_FLAG | PROXIABLE_FLAG;
    public static final int RT_FLAGS = REQUEST_FLAG | RETRANSMITTED_FLAG;
    public static final int RPT_FLAGS = RP_FLAGS | RETRANSMITTED_FLAG;
	
    private H2DiameterClientResponse _response;
    private DiameterSessionFacade _session;
    private int _clientHopIdentifier, _serverHopIdentifier, _endIdentifier;
    private H2DiameterClient _client;
    private Object _attachment;
    private Executor _callerExecutor;
    private ClassLoader _callerCL;
    private Integer _retryTimeoutInMs;
    
    private DiameterClientListener _listener;
    private volatile String _failure = null; // volatile because not modified in a synchronized block
    private int _retransmissions = 0;
    private String _handlerName = null;

    private String _toString;
    
    public H2DiameterClientRequest (H2DiameterClient client, long application, int command, boolean proxiable) {
	super(application, command, (proxiable) ? RP_FLAGS : REQUEST_FLAG);
	_clientHopIdentifier = getNextHopByHopIdentifier();
	setServerHopIdentifier(_clientHopIdentifier);
	_endIdentifier = getNextEndToEndIdentifier();
	_client = client;
	_session = (DiameterSessionFacade) client.getDiameterSession();
	_handlerName = client.getHandlerName();
	_h2dclient = client;
	_toString = new StringBuilder ()
	    .append ("H2DiameterClientRequest[id=")
	    .append (_clientHopIdentifier)
	    .append ("]")
	    .toString ();
    }

    public void setRetryTimeout (Integer seconds){
	_retryTimeoutInMs = seconds * 1000;
    }
    public void setRetryTimeoutInMs (Integer milliseconds){
	_retryTimeoutInMs = milliseconds;
    }
    public Integer getRetryTimeout() {
	return _retryTimeoutInMs != null ? _retryTimeoutInMs / 1000 : null;
    }
    public Integer getRetryTimeoutInMs() {
	return _retryTimeoutInMs != null ? _retryTimeoutInMs : null;
    }
    public boolean isRequest() {
	return true;
    }
    public boolean isLocalOrigin() {
	return true;
    }
    public String getHandlerName() {
	return null;
    }
    public void resumeProxylet(ProxyletData message, int status) {
	throw new IllegalStateException ();
    }
    @Override
    public DiameterRequestFacade getRequestFacade() {
	throw new IllegalStateException ();
    }

    @Override
    public DiameterResponseFacade getResponseFacade() {
	throw new IllegalStateException ();
    }
    
    /** identifiers mgmt from the API / DiameterRequest / methods added later, but old methods kept to avoid breakage ***/
    public int getEndToEndIdentifier () { return _endIdentifier;}
    public int getIncomingHopByHopIdentifier (){ return _clientHopIdentifier;}
    public int getOutgoingHopByHopIdentifier (){ return _serverHopIdentifier;}
    /** identifiers mgmt from the API ***/

    /**
     * @see com.nextenso.diameter.agent.impl.DiameterMessageFacade#getClientHopIdentifier()
     */
    @Override
    public int getClientHopIdentifier() {
	return _clientHopIdentifier;
    }

    private void setServerHopIdentifier(int serverHopIdentifier) {
	_serverHopIdentifier = serverHopIdentifier;
    }

    /**
     * @see com.nextenso.diameter.agent.impl.DiameterMessageFacade#getServerHopIdentifier()
     */
    @Override
    public int getServerHopIdentifier() {
	return _serverHopIdentifier;
    }

    /**
     * @see com.nextenso.diameter.agent.impl.DiameterMessageFacade#getOutgoingClientHopIdentifier()
     */
    @Override
    public int getOutgoingClientHopIdentifier() {
	return _serverHopIdentifier;
    }

    /**
     * @see com.nextenso.diameter.agent.impl.DiameterMessageFacade#getEndIdentifier()
     */
    @Override
    public int getEndIdentifier() {
	return _endIdentifier;
    }

    /**
     * @see com.nextenso.proxylet.diameter.DiameterRequest#hasProxyFlag()
     */
    public boolean hasProxyFlag() {
	return hasFlag(PROXIABLE_FLAG);
    }

    /**
     * @see com.nextenso.proxylet.diameter.DiameterRequest#hasRetransmissionFlag()
     */
    public boolean hasRetransmissionFlag() {
	return hasFlag(RETRANSMITTED_FLAG);
    }

    /**
     * @see com.nextenso.proxylet.diameter.DiameterRequest#setProxyFlag(boolean)
     */
    public void setProxyFlag(boolean flag) {
	setFlag(PROXIABLE_FLAG, flag);
    }

    /**
     * @see com.nextenso.proxylet.diameter.DiameterRequest#setRetransmissionFlag(boolean)
     */
    public void setRetransmissionFlag(boolean flag) {
	setFlag(RETRANSMITTED_FLAG, flag);
    }

    /**
     * @see com.nextenso.proxylet.diameter.DiameterRequest#getResponse()
     */
    public DiameterResponse getResponse() {
	if (_response == null)
	    _response = new H2DiameterClientResponse (this);
	return _response;
    }

    /**
     * @see com.nextenso.proxylet.diameter.DiameterMessage#getDiameterSession()
     */
    public DiameterSession getDiameterSession() {
	return _session;
    }

    /**
     * @see com.nextenso.proxylet.diameter.DiameterMessage#getClientPeer()
     */
    public DiameterPeer getClientPeer() {
	return null;
    }

    /**
     * @see com.nextenso.proxylet.diameter.DiameterMessage#getServerPeer()
     */
    public DiameterPeer getServerPeer() {
	return null;
    }

    /**
     * @see com.nextenso.proxylet.diameter.client.DiameterClientRequest#getDiameterClient()
     */
    public DiameterClient getDiameterClient() {
	return _client;
    }

    // in this case, we use a def behavior : normally it is not called
    protected String getLocalOriginHost(){
	return Utils.getServerOriginHost(getHandlerName());
    }
    // in this case, we use a def behavior : normally it is not called
    protected String getLocalOriginRealm(){
	return DiameterProperties.getOriginRealm();
    }

    /**
     * @see com.nextenso.proxylet.diameter.client.DiameterClientRequest#attach(java.lang.Object)
     */
    public void attach(Object attachment) {
	_attachment = attachment;
    }

    /**
     * @see com.nextenso.proxylet.diameter.client.DiameterClientRequest#attachment()
     */
    public Object attachment() {
	return _attachment;
    }
    
    @Override
    public void send(PeerSocket socket) {
	throw new IllegalStateException ("Method not expected");
    }

    /**
     * @see com.nextenso.proxylet.diameter.client.DiameterClientRequest#execute()
     */
    public DiameterClientResponse execute()
	throws IOException {
	throw new RuntimeException ("Method not implemented");
    }

    /**
     * @see com.nextenso.proxylet.diameter.client.DiameterClientRequest#execute(com.nextenso.proxylet.diameter.client.DiameterClientListener)
     */
    public void execute(DiameterClientListener listener) {
	// Store the current thread executor, which will be used to callback the listener
	_callerExecutor = Utils.getCallbackExecutor();
	_callerCL = Thread.currentThread().getContextClassLoader();

	_listener = listener;
	if (getDiameterSession () != null) {
	    if (((DiameterSessionFacade) getDiameterSession ()).updateLastAccessedTime() == false) {
		localResponse("Session expired");
		return;
	    }
	}	
	doClientRequest ();
    }

    public void localResponse(String failure) {
	_failure = failure;
	if (_listener != null) {
	    Runnable runnable = new Runnable() {

		    public void run() {
			Thread.currentThread().setContextClassLoader(_callerCL);
			if (_failure == null) {
			    _listener.handleResponse(H2DiameterClientRequest.this, _response);
			} else {
			    _listener.handleException(H2DiameterClientRequest.this, new IOException(_failure));
			}
		    }
		};
	    _callerExecutor.execute(runnable);
	}
    }

    private final static AtomicInteger END_SEED = new AtomicInteger((int) (System.currentTimeMillis() & 0xFFF) << 20); // follow rfc suggestion

    public int getNextEndToEndIdentifier() {
	return END_SEED.incrementAndGet();
    }

    private static final AtomicInteger HOP_SEED = new AtomicInteger(END_SEED.get());
    public static int HOP_ID_MASK = 0xFFFFFFFF; // may be sent by java ioh to limit the hop id range

    public int getNextHopByHopIdentifier() {
	return HOP_SEED.getAndIncrement() & HOP_ID_MASK;
    }

    @Override
    public DiameterClientResponse getDiameterClientResponse() {
	return getResponseFacade();
    }
    
    public void doClientRequest (){
	if (LOGGER.isDebugEnabled ())
	    LOGGER.debug (_toString+" : doClientRequest");
	H2DiameterClientFactory h2dclientF = _h2dclient.getH2DiameterClientFactory ();

	HttpClientFactory h2ClientF = h2dclientF.getH2ClientFactory ();
	HttpClient h2Client = h2dclientF.getH2Client ();
	
	ByteOutputStream bos = new ByteOutputStream ();
	getBytes (bos);
	
	HttpRequest.Builder builder = h2ClientF.newHttpRequestBuilder()
	    .uri(h2dclientF.getH2URI ())
	    .header("content-type", "application/octet-stream")
	    .header("destOriginHost", _h2dclient.getDestinationHost())
	    .header("destRealm", _h2dclient.getDestinationRealm());

	_client.getH2DiameterClientFactory ().getHeaders ().decorate (this, builder);
	
	HttpRequest post = builder
	    .POST(h2ClientF.bodyPublishers().ofByteArray(bos.toByteArray ()))
	    .build();

	CompletableFuture<HttpResponse<byte[]>> cf =
	    h2Client.sendAsync(post, h2ClientF.bodyHandlers().ofByteArray())
	    .whenCompleteAsync((response, exception) -> {
                    if (exception != null) {
			failed (-1, exception);
                    } if (response != null) {
			//LOGGER.warn ("x-test-1 = "+response.headers().firstValue("x-test-1").orElse ("----"));
			if (response.statusCode () == 200)
			    respond (response.body ());
			else
			    failed (response.statusCode (), new Exception ("Server responded "+response.statusCode ()+" - Warning = ["+response.headers().firstValue("Warning").orElse ("")+"]"));
		    } else {
			// response is null this should not happen with this API
                    }
		    // It is better to specify your executor where the lambda will be executed
		    // to prevent concurrent access to your internal state.
                }, _callerExecutor);
    }

    private void respond (byte[] respData){
	if (LOGGER.isDebugEnabled ())
	    LOGGER.debug (_toString+" : response received");
	getResponse (); // create the object
	_response.setData (respData, 20, respData.length-20);
	try{
	    _response.parseData ();
	}catch(Exception e){
	    failed (200, e);
	    return;
	}
	if (LOGGER.isDebugEnabled ())
	    LOGGER.debug (_toString+" : parsed response received : "+_response);
	if (_listener != null) {
	    Runnable runnable = new Runnable() {

		    public void run() {
			Thread.currentThread().setContextClassLoader(_callerCL);
			_listener.handleResponse(H2DiameterClientRequest.this, _response);
		    }
		};
	    _callerExecutor.execute(runnable);
	}
    }

    private void failed (int status, Throwable t){
	String failure = "h2 status : "+status;
	if (LOGGER.isInfoEnabled ())
	    LOGGER.info (_toString+" : failed to execute : "+failure, t);
	localResponse (failure);
    }
    
}
