package com.alcatel_lucent.as.ims.impl.charging.rf;

import java.net.NoRouteToHostException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.ImsClient;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.AccountingRecordType;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterSession;
import com.nextenso.proxylet.diameter.client.DiameterClient;
import com.nextenso.proxylet.diameter.client.DiameterClientFactory;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The abstract Rf client.
 */
public abstract class AbstractRfClient
		implements ImsClient {

	private final static Logger LOGGER = Logger.getLogger("3gpp.interfaces.rf");
	private final static long VENDOR_ID = 0;
	private final static long APPLICATION_ID = 3;

	private Version _version;
	private DiameterClient _client;
	private AtomicLong _nextRecordNumber = new AtomicLong(0L);

	/**
	 * Constructor for this class.
	 * 
	 * @param servers The servers.
	 * @param realm The realm.
	 * 
	 * @param version The 32.299 version.
	 * @throws NoRouteToHostException
	 */
	public AbstractRfClient(Iterable<String> servers, String realm, Version version)
			throws NoRouteToHostException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("new AbstractRfClient: servers=" + servers + ", realm=" + realm + ", version=" + version);
		}

		setVersion(version);
		DiameterClientFactory factory = DiameterClientFactory.getDiameterClientFactory();
		if (servers == null || (!servers.iterator().hasNext())) {
			try {
				setDiameterClient(factory.newDiameterClient(null, realm, VENDOR_ID, APPLICATION_ID, DiameterClient.TYPE_AUTH, true, 0));
			}
			catch (Exception e) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("cannot access to the realm=" + realm, e);
				}
			}
			if (getDiameterClient() == null) {
				throw new NoRouteToHostException("Cannot access any Rf server");
			}
			return;
		}

		Iterator<String> iter = servers.iterator();
		while (getDiameterClient() == null && iter.hasNext()) {
			String server = iter.next();
			try {
				setDiameterClient(factory.newDiameterClient(server, realm, VENDOR_ID, APPLICATION_ID, DiameterClient.TYPE_AUTH, true, 0));
			}
			catch (Exception e) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("cannot access to the realm=" + realm, e);
				}
			}
		}
		if (getDiameterClient() == null) {
			throw new NoRouteToHostException("Cannot access any Rf server");
		}
	}

	/**
	 * Creates a new request.
	 * 
	 * @param accountingRecordType
	 * @return
	 */
	protected DiameterClientRequest newRequest(AccountingRecordType accountingRecordType) {
		long accountingRecordNumber = getRecordNumber();
		DiameterClientRequest request = _client.newAcctRequest(DiameterBaseConstants.COMMAND_ACR, true);

		DiameterAVP avp = new DiameterAVP(DiameterBaseConstants.AVP_ACCOUNTING_RECORD_TYPE);
		avp.setValue(EnumeratedFormat.toEnumerated(accountingRecordType.getValue()), false);
		request.addDiameterAVP(avp);

		avp = new DiameterAVP(DiameterBaseConstants.AVP_ACCOUNTING_RECORD_NUMBER);
		avp.setValue(Unsigned32Format.toUnsigned32(accountingRecordNumber), false);
		request.addDiameterAVP(avp);

		return request;
	}

	protected long getRecordNumber() {
		return _nextRecordNumber.getAndIncrement();
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

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.ImsClient#close()
	 */
	public void close() {
		getDiameterClient().close();
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

}
