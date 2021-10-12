// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.impl.charging.rf;

import java.net.NoRouteToHostException;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.AccountingRecordType;
import com.alcatel_lucent.as.ims.diameter.charging.rf.AccountingRequest;
import com.alcatel_lucent.as.ims.diameter.charging.rf.RfEventClient;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;

/**
 * 
 * The Rf event client.
 */
public class EventClient
		extends AbstractRfClient
		implements RfEventClient {

	/**
	 * Constructor for this class. 
	 *
	 * @param servers The server list.
	 * @param realm 
	 * @param version The version.
	 * @throws NoRouteToHostException if no server can be connected.
	 */
	public EventClient(Iterable<String> servers, String realm, Version version)
			throws NoRouteToHostException {
		super(servers, realm, version);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.rf.RfEventClient#createEventRequest()
	 */
	public AccountingRequest createEventRequest() {
		DiameterClientRequest request = newRequest(AccountingRecordType.EVENT_RECORD);
		AccountingRequest res = new Acr(request, getVersion());
		return res;
	}

}
