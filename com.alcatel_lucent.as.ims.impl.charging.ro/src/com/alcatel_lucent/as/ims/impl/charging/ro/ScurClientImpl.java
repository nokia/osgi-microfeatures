package com.alcatel_lucent.as.ims.impl.charging.ro;

import java.net.NoRouteToHostException;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.CcRecordType;
import com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlRequest;
import com.alcatel_lucent.as.ims.diameter.charging.ro.ScurClient;

/**
 * The SCUR Client implementation.
 */
public class ScurClientImpl
		extends AbstractRoClient
		implements ScurClient {

	public ScurClientImpl() {}

	/**
	 * Constructor for this class.
	 * 
	 * @param servers The CCF servers.
	 * @param realm The realm.
	 * @param serviceContextId The Service-Context-Id.
	 * @param version The version.
	 * @throws NoRouteToHostException if no server can be connected.
	 */
	public ScurClientImpl(Iterable<String> servers, String realm, String serviceContextId, Version version)
			throws NoRouteToHostException {
		super(servers, realm, serviceContextId, version);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.ScurClient#createInitialRequest()
	 */
	public CreditControlRequest createInitialRequest()
		throws IllegalStateException {
		if (getState() != State.INIT) {
			throw new IllegalStateException("cannot create an START record with current state=" + getState());
		}
		Ccr res = createCcr(CcRecordType.INITIAL_REQUEST);
		setState(State.START);
		return res;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.ScurClient#createUpdateRequest()
	 */
	public CreditControlRequest createUpdateRequest()
		throws IllegalStateException {
		if (getState() != State.START) {
			throw new IllegalStateException("cannot create an INTERIM record with current state=" + getState());
		}
		Ccr res = createCcr(CcRecordType.UPDATE_REQUEST);
		return res;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.ScurClient#createTerminationRequest()
	 */
	public CreditControlRequest createTerminationRequest()
		throws IllegalStateException {
		if (getState() != State.START) {
			throw new IllegalStateException("cannot create an STOP record with current state=" + getState());
		}
		Ccr res = createCcr(CcRecordType.TERMINATION_REQUEST);
		setState(State.STOP);

		return res;
	}

}
