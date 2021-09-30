package com.alcatel_lucent.as.ims.impl.charging.ro;

import java.net.NoRouteToHostException;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.CcRecordType;
import com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlRequest;
import com.alcatel_lucent.as.ims.diameter.charging.ro.EcurClient;

/**
 * The ECUR Client Implementation.
 */
public class EcurClientImpl
		extends AbstractRoClient
		implements EcurClient {

	public EcurClientImpl() {}
	
	public EcurClientImpl(Iterable<String> servers, String realm, String serviceContextId, Version version)
			throws NoRouteToHostException {
		super(servers, realm, serviceContextId, version);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.EcurClient#createInitialRequest()
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
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.EcurClient#createTerminationRequest()
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
