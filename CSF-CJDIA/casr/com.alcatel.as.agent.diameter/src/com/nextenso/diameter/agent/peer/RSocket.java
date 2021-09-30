package com.nextenso.diameter.agent.peer;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.nextenso.diameter.agent.Utils;
import com.nextenso.diameter.agent.DiameterProperties;
import com.nextenso.diameter.agent.impl.DiameterMessageFacade;
import com.nextenso.diameter.agent.impl.DiameterResponseFacade;
import com.nextenso.mux.MuxConnection;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterPeer.Protocol;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.IdentityFormat;

public class RSocket
		extends PeerSocket
		implements Runnable {

	private static final Logger LOGGER = Logger.getLogger("agent.diameter.RSocket");


	public RSocket(MuxConnection connection, int socketId, long connectionId, String remoteIP, int remotePort, String localIP, int localPort, String virtualIP, int virtualPort,
		       boolean secure, Protocol protocol, String[] remoteIPs, String[] localIPs) {
		super(connection, socketId, connectionId, remoteIP, remotePort, localIP, localPort, virtualIP, virtualPort, secure, protocol, remoteIPs, localIPs);
	}

	public void run (){} // not used anymore / kept to avoid a major version change

	/**
	 * Processes the received CER.
	 * 
	 * @param cer The CER.
	 */
	private void processCER(DiameterMessageFacade cer) {
		if (cer.isRequest() && cer.getDiameterApplication() == DiameterBaseConstants.APPLICATION_COMMON_MESSAGES
				&& cer.getDiameterCommand() == DiameterBaseConstants.COMMAND_CER) {

			try{
				DiameterAVP originHostAVP = cer.getDiameterAVP(DiameterBaseConstants.AVP_ORIGIN_HOST);
				if (originHostAVP == null) throw new DiameterMessageFacade.ParsingException (DiameterBaseConstants.RESULT_CODE_DIAMETER_MISSING_AVP, "Missing Origin-Host AVP", DiameterBaseConstants.AVP_ORIGIN_HOST);
				String originHost = IdentityFormat.getIdentity(originHostAVP.getValue());
				DiameterAVP originRealmAVP = cer.getDiameterAVP(DiameterBaseConstants.AVP_ORIGIN_REALM);
				if (originRealmAVP == null) throw new DiameterMessageFacade.ParsingException (DiameterBaseConstants.RESULT_CODE_DIAMETER_MISSING_AVP, "Missing Origin-Realm AVP", DiameterBaseConstants.AVP_ORIGIN_REALM);
				String originRealm = IdentityFormat.getIdentity(originRealmAVP.getValue());
				RemotePeer peer = Utils.getTableManager().newRemotePeer(getHandlerName(), originHost, originRealm, getRemoteAddr(), getRemotePort(), isSecure(), getProtocol());
				if (peer != null) {
					super.processCER(peer.getStateMachine());
					getStateMachine().connectedWithCER(this, cer);
				} else {
					//#IMSAS0FAG264909
					// the peer has been stopped (because the agent is stopping)
					// but an incoming connection happens with a CER
					// #CSFAR-1590 : if the remote peer has the same orginHost, then PeerTable can return null
					disconnect(false);
				}
			}catch(DiameterMessageFacade.ParsingException pe){
				rejectCER (cer, pe);
			}catch(Exception e){
				if (e.getCause () instanceof DiameterMessageFacade.ParsingException){
					rejectCER (cer, (DiameterMessageFacade.ParsingException) e.getCause ());
				} else {
					disconnect (true);
				}
			}
		} else {
			// the first message is not a CER -> disconnect
			disconnect(true);
		}
	}

	/**
	 * Processes the message.
	 * 
	 * @param message The message.
	 */
	@Override
	public void processMessage(DiameterMessageFacade message) {
		if (getStateMachine() == null) {
			processCER(message);
			return;
		}
		super.processMessage(message);
	}

	/**
	 * @see com.nextenso.diameter.agent.peer.PeerSocket#isInitiator()
	 */
	@Override
	public boolean isInitiator() {
		return false;
	}

	/**
	 * @see com.nextenso.diameter.agent.peer.PeerSocket#toString()
	 */
	@Override
	public String toString() {
		return "R-Socket - " + super.toString();
	}

	@Override
	protected Logger getLogger() {
		return LOGGER;
	}

}
