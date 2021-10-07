package com.nextenso.diameter.agent.peer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.utils.ConfigException;
import alcatel.tess.hometop.gateways.utils.IPAddr;

import com.nextenso.diameter.agent.DiameterProperties;
import com.nextenso.diameter.agent.Utils;
import com.nextenso.diameter.agent.impl.DiameterMessageFacade;
import com.nextenso.diameter.agent.impl.DiameterRequestFacade;
import com.nextenso.mux.MuxConnection;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterApplication;
import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.util.AddressFormat;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;

/**
 * The local peer.
 */
public class LocalPeer
		extends Peer {

	private static final Logger LOGGER = Logger.getLogger("agent.diameter.localpeer");

	private boolean _isHostIPDefined;
	private DiameterAVP _hostIPAddressAVP;

	private class ResultListener
			implements ProcessMessageResultListener {

		private DiameterMessageFacade _message;

		public ResultListener(DiameterMessageFacade message) {
			_message = message;
		}

		public void handleResult(DiameterMessageFacade result) {
			processMessageAfterResult(result, _message);
		}

	}

	public LocalPeer(String originHost, String handlerName, Protocol protocol) {
		super(handlerName, originHost, protocol);
		setId (SEED_LOCAL.incrementAndGet ());
		init();
		if (getLogger().isDebugEnabled()) {
			getLogger().debug("constr: new Local Peer=" + this);
		}
	}

	/**
	 * Loads the LocalPeer configuration. It does it once.
	 * 
	 * @param config
	 * @param instanceName
	 * @throws ConfigException
	 */
	private void init() {
		if (getLogger().isInfoEnabled()) {
			getLogger().info("init: Local originHost is " + getOriginHost());
		}
	}

	/**
	 * Updates the HostIPAddress when a stack is connected.
	 * 
	 * @param connection The MUX connection.
	 */
	public void muxOpened(MuxConnection connection) {
		//setHostIPAddresses(connection.getStackAddress());
		LOGGER.info (this+" : muxOpened : "+connection);
		java.util.Set<String> addrs = DiameterProperties.getHostIPAddresses (getHandlerName ());
		for (String addr : addrs)
			addHostIPAddress (addr);
	}

	public boolean addHostIPAddress (String... addresses) {
		if (_hostIPAddressAVP == null) _hostIPAddressAVP = new DiameterAVP(DiameterBaseConstants.AVP_HOST_IP_ADDRESS);
		List<String> acceptedAddresses = new ArrayList<String>();

		for (String address : addresses) {
			if (LOGGER.isDebugEnabled ())
				LOGGER.debug (this+" : addHostIPAddress : "+address);
			IPAddr addr = null;
			try {
				addr = new IPAddr(address);
			}
			catch (Exception e) {
				LOGGER.warn("setHostIPAddresses: Cannot add this address because it cannot be parsed: address= " + address);
				return false;
			}

			byte[] valueB = addr.toByteArray();
			byte[] data = null;
			if (addr.isIPv4()) {
				data = AddressFormat.toAddress(AddressFormat.IPV4, valueB, 0, 4);
			} else if (addr.isIPv6()) {
				data = AddressFormat.toAddress(AddressFormat.IPV6, valueB, 0, valueB.length);
			} else {
				LOGGER.warn("Cannot add this address because it is not a IPv4 or IPv6 address: address= " + address);
			}
			if (data != null) {
				boolean exist = false;
				for (int i=0; i<_hostIPAddressAVP.getValueSize (); i++){
					byte[] value = _hostIPAddressAVP.getValue (i);
					if (exist = Arrays.equals (data, value)) break;
				}
				if (exist){
					if (LOGGER.isDebugEnabled ())
						LOGGER.debug (this+" : addHostIPAddress : already added : "+address);
				} else {
					LOGGER.info(this+" : addHostIPAddress : added "+address);
					_hostIPAddressAVP.addValue(data, false);
					acceptedAddresses.add(address);
				}
			}
		}
		return acceptedAddresses.size () > 0;
	}

	
	public DiameterAVP getHostIPAddressesAvp() {
		return _hostIPAddressAVP;
	}

    public List<String> getHosts (){ return new ArrayList<> (1);} // not implemented for local peer

	/**
	 * @see com.nextenso.diameter.agent.peer.Peer#processMessage(com.nextenso.diameter.agent.impl.DiameterMessageFacade,
	 *      boolean)
	 */
	@Override
	public void processMessage(DiameterMessageFacade message, boolean mainThread) {
		// only used in DiameterClientFacade

		// TODO check if the application was advertised
		ProcessMessageResultListener listener = new ResultListener(message);
		processRequest(message.getRequestFacade(), mainThread, this, listener);
	}

	private void processMessageAfterResult(DiameterMessageFacade result, DiameterMessageFacade message) {
		if (result == null) {
			return;
		}

		if (result.isRequest()) {
			DiameterRequestFacade request = (DiameterRequestFacade) result;
			request.executeRemote(false);
		} else {
			((DiameterRequestFacade) message).localResponse(null);
		}
	}

	/**
	 * @see com.nextenso.diameter.agent.peer.Peer#sendMessage(com.nextenso.diameter.agent.impl.DiameterMessageFacade)
	 */
	@Override
	public void sendMessage(DiameterMessageFacade message) {
		// called when a response to a client arrives from a remote Peer
		if (message.getResponseFacade ().ignorable ())
			message.getRequestFacade().remoteResponse("Timeout"); // only Timeout for now can set ignorable
		else
			message.getRequestFacade().remoteResponse(null);
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#isLocalDiameterPeer()
	 */
	@Override
	public boolean isLocalDiameterPeer() {
		return true;
	}

	public String getLocalDiameterPeerName (){
		return getHandlerName ();
	}


	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#getOriginRealm()
	 */
	public String getOriginRealm() {
		return DiameterProperties.getOriginRealm();
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#getHostIPAddresses()
	 */
	public byte[][] getHostIPAddresses() {
		byte[][] res = null;

		DiameterAVP avp = getHostIPAddressesAvp();
		if (avp != null) {
			res = new byte[avp.getValueSize()][];
			for (int i = avp.getValueSize() - 1; i >= 0; i--) {
				res[i] = avp.getValue(i);
			}
		}

		return res;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#getVendorId()
	 */
	public long getVendorId() {
		return DiameterProperties.getVendorId();
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#getProductName()
	 */
	public String getProductName() {
		return DiameterProperties.getProductName();
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#getOriginStateId()
	 */
	public long getOriginStateId() {
		return DiameterProperties.getOriginStateId();
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#getAuthApplications()
	 */
	public long[] getAuthApplications() {
		return getLongArray(Utils.getCapabilities().getAuthApplications());
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#getAcctApplications()
	 */
	public long[] getAcctApplications() {
		return getLongArray(Utils.getCapabilities().getAcctApplications());
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#getSupportedVendorIds()
	 */
	public long[] getSupportedVendorIds() {
		return getLongArray(Utils.getCapabilities().getSupportedVendorIds());
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#getSpecificApplications()
	 * @deprecated
	 */
	@SuppressWarnings("deprecation")
	@Deprecated
	public List<DiameterApplication> getSpecificApplications() {
		return Utils.getCapabilities().getVendorSpecificApplications();
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#getInbandSecurityId()
	 */
	public long[] getInbandSecurityId() {
		return new long[0];
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#getFirmwareRevision()
	 */
	public long getFirmwareRevision() {
		long res = DiameterProperties.getFirmwareRevision();
		return res;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#isEncrypted()
	 */
	public boolean isEncrypted() {
		return true;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#disconnect(int)
	 */
	public void disconnect(int diconnectCause) {
		throw new IllegalStateException("Cannot disconnect LocalPeer");
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#isConnected()
	 */
	public boolean isConnected() {
		return true;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append("Local Peer [");
		buff.append("id=").append(getId());
		buff.append(", originHost=").append(getOriginHost());
		buff.append(", handler name=").append(getHandlerName());
		buff.append(']');
		return buff.toString();
	}

	/**
	 * @see com.nextenso.diameter.agent.peer.Peer#getLogger()
	 */
	@Override
	protected Logger getLogger() {
		return LOGGER;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#connect()
	 */
	public void connect() {
		throw new UnsupportedOperationException("Cannot connect a local peer");
	}
	public void connect(int port, String... localIP){
		throw new UnsupportedOperationException("Cannot connect a local peer");
	}
	public void setSctpSocketOptions (java.util.Map options){
		throw new UnsupportedOperationException("Cannot set sctp socket options for a local peer");
	}
	public void setParameters (java.util.Map<String, String> params){
		// no param applicable
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#getLocalDiameterPeer()
	 */
	public DiameterPeer getLocalDiameterPeer() {
		return this;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#getSupportedApplications()
	 */
	public List<DiameterApplication> getSupportedApplications() {
		List<DiameterApplication> res = new ArrayList<DiameterApplication>();
		res.addAll(Utils.getCapabilities().getSupportedApplications());
		return res;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#setSupportedApplications(java.util.List)
	 */
	@Override
	public void setSupportedApplications(List<DiameterApplication> applications)
		throws UnsupportedOperationException {
		if (applications == null) {
			return;
		}
		Utils.getCapabilities().setSupportedApplications(applications);
		Utils.getCapabilities().update();
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#setRelay()
	 */
	@Override
	public void setRelay()
		throws UnsupportedOperationException {
		Utils.getCapabilities().setRelay(true);
		Utils.getCapabilities().update();
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#isRelay()
	 */
	public boolean isRelay() {
		return Utils.getCapabilities().isRelay();
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#getQuarantineDelay()
	 */
	@Override
	public Long getQuarantineDelay() {
		return Long.valueOf(0L);
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#setQuarantineDelay(java.lang.Long)
	 */
	@Override
	public void setQuarantineDelay(Long delayInMs) {}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#isQuarantined()
	 */
	@Override
	public boolean isQuarantined() {
		return false;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#quarantine()
	 */
	@Override
	public void quarantine() {}

	// should not be called
	public DiameterAVP[] getCapabilitiesExchangeAVPs (){
		return new DiameterAVP[0];
	}

}
