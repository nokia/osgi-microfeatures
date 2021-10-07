package com.alcatel_lucent.as.ims.impl.charging.ro;

import java.net.NoRouteToHostException;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.CcRecordType;
import com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlRequest;
import com.alcatel_lucent.as.ims.diameter.charging.ro.IecClient;

/**
 * The IEC Client Implementation.
 */
public class IecClientImpl
		extends AbstractRoClient
		implements IecClient {

	public IecClientImpl() {}

	/**
	 * 
	 * Constructor for this class.
	 * 
	 * @param servers The servers.
	 * @param realm The realm.
	 * @param serviceContextId The Service-Context-Id.
	 * @param version The 32.299 document version.
	 * @throws NoRouteToHostException
	 */
	public IecClientImpl(Iterable<String> servers, String realm, String serviceContextId, Version version)
			throws NoRouteToHostException {
		super(servers, realm, serviceContextId, version);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.IecClient#createEventRequest()
	 */
	public CreditControlRequest createEventRequest()
		throws IllegalStateException {
		Ccr res = createCcr(CcRecordType.EVENT_REQUEST);
		return res;
	}

}
