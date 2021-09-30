package com.alcatel_lucent.as.ims.diameter.common;

import java.net.NoRouteToHostException;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.ImsClient;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterSession;
import com.nextenso.proxylet.diameter.client.DiameterClient;
import com.nextenso.proxylet.diameter.client.DiameterClientFactory;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;

/**
 * The IMS Client.
 */
public abstract class AbstractClient
		implements ImsClient {

	private static final int NO_STATE_MAINTAINED = 1;
	private static final DiameterAVP NO_STATE_MAINTAINED_AVP = new DiameterAVP(DiameterBaseConstants.AVP_AUTH_SESSION_STATE);
	static {
		NO_STATE_MAINTAINED_AVP.setValue(EnumeratedFormat.toEnumerated(NO_STATE_MAINTAINED), false);
	}

	private Version _version;
	private DiameterClient _client;

	/**
	 * Constructor for this class.
	 * 
	 * @param destinationHost The destination host.
	 * @param destinationRealm
	 * @param version
	 * @throws NoRouteToHostException
	 */
	public AbstractClient(String destinationHost, String destinationRealm, Version version)
			throws NoRouteToHostException {
		setVersion(version);
		DiameterClientFactory factory = DiameterClientFactory.getDiameterClientFactory();
		setDiameterClient(factory.newDiameterClient(destinationHost, destinationRealm, getVendorId(), getApplicationId(), DiameterClient.TYPE_AUTH, true, 0));
	}

	public abstract long getVendorId();

	protected abstract long getApplicationId();

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.ImsClient#close()
	 */
	public void close() {
		getDiameterClient().close();
	}

	/**
	 * Creates a new request.
	 */
	protected DiameterClientRequest newRequest(int commandCode, boolean proxiable) {
		DiameterClientRequest request = _client.newAuthRequest(commandCode, proxiable);
		request.addDiameterAVP((DiameterAVP) NO_STATE_MAINTAINED_AVP.clone());
		return request;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.ImsClient#getDiameterSession()
	 */
	public DiameterSession getDiameterSession() {
		if (_client != null) {
			return _client.getDiameterSession();
		}
		return null;
	}

	/**
	 * Sets the version.
	 * 
	 * @param version The version.
	 */
	public void setVersion(Version version) {
		_version = version;
	}

	/**
	 * Gets the version.
	 * 
	 * @return The version.
	 */
	public Version getVersion() {
		return _version;
	}

	/**
	 * Sets the client.
	 * 
	 * @param client The client.
	 */
	public void setDiameterClient(DiameterClient client) {
		_client = client;
	}

	/**
	 * Gets the client.
	 * 
	 * @return The client.
	 */
	public DiameterClient getDiameterClient() {
		return _client;
	}

}
