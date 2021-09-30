package com.nextenso.diameter.agent.impl.h2;

import org.apache.log4j.Logger;

import com.nextenso.diameter.agent.impl.DiameterMessageFacade;
import com.nextenso.diameter.agent.impl.DiameterRequestFacade;
import com.nextenso.diameter.agent.impl.DiameterResponseFacade;
import com.nextenso.diameter.agent.DiameterProperties;
import com.nextenso.diameter.agent.Utils;
import com.nextenso.diameter.agent.ha.HaManager;
import com.nextenso.diameter.agent.peer.Peer;
import com.nextenso.diameter.agent.peer.PeerSocket;
import com.nextenso.proxylet.ProxyletData;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterRequest;
import com.nextenso.proxylet.diameter.DiameterSession;
import com.nextenso.proxylet.diameter.client.DiameterClient;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.diameter.agent.DiameterProperties;

import static com.nextenso.diameter.agent.impl.h2.H2DiameterClient.LOGGER;

public class H2DiameterClientResponse
    extends DiameterMessageFacade
    implements DiameterClientResponse {

    private static final Integer INT_1 = Integer.valueOf (1);
    private static final String REDIRECT_ATTR = "redirect";

    public static final int E_FLAG = 0x20;

    private H2DiameterClientRequest _request;
    
    public H2DiameterClientResponse(H2DiameterClientRequest request) {
	super(request.getDiameterApplication(), request.getDiameterCommand(),
	      request.hasProxyFlag() ? DiameterRequestFacade.PROXIABLE_FLAG : DiameterRequestFacade.NO_FLAG);
	_request = request;
    }

    public void setDefaultAVPs() {
	// add session-id AVP
	DiameterAVP avp = _request.getDiameterAVP(DiameterBaseConstants.AVP_SESSION_ID);
	if (avp != null) {
	    addDiameterAVP((DiameterAVP) avp.clone());
	}

	setDefaultOriginAVPs();

	// add application-id AVP
	if (getDiameterApplication() != DiameterBaseConstants.APPLICATION_BASE_ACCOUNTING
	    && getDiameterApplication() != DiameterBaseConstants.APPLICATION_COMMON_MESSAGES) {
	    avp = _request.getDiameterAVP(DiameterBaseConstants.AVP_VENDOR_SPECIFIC_APPLICATION_ID);
	    if (avp != null) {
		addDiameterAVP((DiameterAVP) avp.clone());
		return;
	    }
	}
	avp = _request.getDiameterAVP(DiameterBaseConstants.AVP_AUTH_APPLICATION_ID);
	if (avp != null) {
	    addDiameterAVP((DiameterAVP) avp.clone());
	    return;
	}
	avp = _request.getDiameterAVP(DiameterBaseConstants.AVP_ACCT_APPLICATION_ID);
	if (avp != null) {
	    addDiameterAVP((DiameterAVP) avp.clone());
	    return;
	}
    }

    /******************* abstract methods ****************/

    @Override
    public boolean isLocalOrigin() {
	return false;
    }

    @Override
    public boolean isRequest() {
	return false;
    }

    @Override
    public DiameterRequestFacade getRequestFacade() {
	throw new IllegalStateException ();
    }

    @Override
    public DiameterResponseFacade getResponseFacade() {
	throw new IllegalStateException ();
    }

    @Override
    public int getClientHopIdentifier() {
	return _request.getClientHopIdentifier();
    }

    @Override
    public int getServerHopIdentifier() {
	return _request.getServerHopIdentifier();
    }

    @Override
    public int getOutgoingClientHopIdentifier() {
	return _request.getClientHopIdentifier();
    }

    @Override
    public int getEndIdentifier() {
	return _request.getEndIdentifier();
    }

    // in this case, we use a def behavior : normally it is not called
    protected String getLocalOriginHost(){
	return Utils.getServerOriginHost(getHandlerName());
    }
    // in this case, we use a def behavior : normally it is not called
    protected String getLocalOriginRealm(){
	return DiameterProperties.getOriginRealm();
    }

    /**********************************
     * Implementation of DiameterResponse
     *********************************/

    public long getResultCode() {
	DiameterAVP avp = getDiameterAVP(DiameterBaseConstants.AVP_RESULT_CODE);
	if (avp == null) {
	    return -1L;
	}
	return Unsigned32Format.getUnsigned32(avp.getValue(), 0);
    }

    public void setResultCode(long code) {
	DiameterAVP avp = getDiameterAVP(DiameterBaseConstants.AVP_RESULT_CODE);
	if (avp == null) {
	    avp = new DiameterAVP(DiameterBaseConstants.AVP_RESULT_CODE);
	    addDiameterAVP(avp);
	}
	avp.setValue(Unsigned32Format.toUnsigned32(code), false);
	setErrorFlag(code > 3000L && code < 4000L);
    }

    public boolean hasErrorFlag() {
	return hasFlag(E_FLAG);
    }

    public void setErrorFlag(boolean flag) {
	setFlag(E_FLAG, flag);
    }

    public DiameterRequest getRequest() {
	return _request;
    }

    /**
     * @see com.nextenso.proxylet.diameter.DiameterMessage#getDiameterSession()
     */
    public DiameterSession getDiameterSession() {
	return _request.getDiameterSession();
    }

    public DiameterPeer getClientPeer() {
	return _request.getClientPeer();
    }

    public DiameterPeer getServerPeer() {
	return _request.getServerPeer();
    }

    public void send (){
	throw new IllegalStateException ();
    }

    /**********************************
     * Implementation of DiameterClientResponse
     *********************************/

    public DiameterClient getDiameterClient() {
	return _request.getDiameterClient();
    }

    public DiameterClientRequest getDiameterClientRequest() {
	return _request;
    }

    @Override
    public String getHandlerName() {
	return _request.getHandlerName();
    }

    /**
     * @see com.nextenso.proxylet.engine.AsyncProxyletManager.ProxyletResumer#resumeProxylet(com.nextenso.proxylet.ProxyletData,
     *      int)
     */
    @Override
    public void resumeProxylet(ProxyletData msg, int status) {
	throw new IllegalStateException ();
    }

    @Override
    public void send(PeerSocket socket) {
	throw new IllegalStateException ();
    }

}
