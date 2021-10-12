// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.impl.sh;

import java.net.NoRouteToHostException;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.common.AbstractClient;
import com.alcatel_lucent.as.ims.diameter.sh.ProfileUpdateRequest;
import com.alcatel_lucent.as.ims.diameter.sh.ShClient;
import com.alcatel_lucent.as.ims.diameter.sh.ShConstants;
import com.alcatel_lucent.as.ims.diameter.sh.ShUtils;
import com.alcatel_lucent.as.ims.diameter.sh.SubscribeNotificationsRequest;
import com.alcatel_lucent.as.ims.diameter.sh.UserDataRequest;
import com.alcatel_lucent.as.ims.diameter.sh.ShConstants.DataReference;
import com.alcatel_lucent.as.ims.diameter.sh.ShConstants.SubsReqType;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;

/**
 * The ShClientImpl class.
 */
public class ShClientImpl
		extends AbstractClient
		implements ShClient {

	public ShClientImpl(String destinationHost, String destinationRealm, Version version)
			throws NoRouteToHostException {
		super(destinationHost, destinationRealm, version);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.ShClient#createUDR(java.lang.String,
	 *      com.alcatel_lucent.as.ims.diameter.sh.ShConstants.DataReference[])
	 */
	public UserDataRequest createUDR(String publicIdentity, DataReference... dataReferences) {
		DiameterClientRequest request = newRequest(ShConstants.USER_DATA_COMMAND, true);

		UserDataRequest res = new UdrImpl(request, getVersion());
		res.setUserIdentity(publicIdentity);
		for (DataReference dataReference : dataReferences) {
			res.addDataReference(dataReference);
		}

		return res;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.ShClient#createUDR(byte[],
	 *      com.alcatel_lucent.as.ims.diameter.sh.ShConstants.DataReference[])
	 */
	public UserDataRequest createUDR(byte[] msisdn, DataReference... dataReferences) {
		DiameterClientRequest request = newRequest(ShConstants.USER_DATA_COMMAND, true);

		UserDataRequest res = new UdrImpl(request, getVersion());
		res.setUserIdentity(msisdn);
		for (DataReference dataReference : dataReferences) {
			res.addDataReference(dataReference);
		}

		return res;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.ShClient#createPUR(java.lang.String,
	 *      byte[],
	 *      com.alcatel_lucent.as.ims.diameter.sh.ShConstants.DataReference)
	 */
	public ProfileUpdateRequest createPUR(String publicIdentity, byte[] userData, DataReference dataReference) {
		DiameterClientRequest request = newRequest(ShConstants.PROFILE_UPDATE_COMMAND, true);

		ProfileUpdateRequest pur = new PurImpl(request, getVersion());
		pur.setUserIdentity(publicIdentity);
		pur.setUserData(userData);
		pur.setDataReference(dataReference);

		return pur;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.ShClient#createPUR(byte[],
	 *      byte[],
	 *      com.alcatel_lucent.as.ims.diameter.sh.ShConstants.DataReference)
	 */
	public ProfileUpdateRequest createPUR(byte[] msisdn, byte[] userData, DataReference dataReference) {
		DiameterClientRequest request = newRequest(ShConstants.PROFILE_UPDATE_COMMAND, true);

		ProfileUpdateRequest pur = new PurImpl(request, getVersion());
		pur.setUserIdentity(msisdn);
		pur.setUserData(userData);
		pur.setDataReference(dataReference);

		return pur;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.ShClient#createSNR(java.lang.String,
	 *      com.alcatel_lucent.as.ims.diameter.sh.ShConstants.SubsReqType,
	 *      com.alcatel_lucent.as.ims.diameter.sh.ShConstants.DataReference[])
	 */
	public SubscribeNotificationsRequest createSNR(String publicIdentity, SubsReqType subsReqType, DataReference... dataReferences) {
		DiameterClientRequest request = newRequest(ShConstants.SUBSCRIBE_NOTIFICATION_COMMAND, true);

		SubscribeNotificationsRequest snr = new SnrImpl(request, getVersion());
		snr.setUserIdentity(publicIdentity);
		snr.setSubsReqType(subsReqType);
		for (DataReference dataReference : dataReferences) {
			snr.addDataReference(dataReference);
		}

		return snr;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.ShClient#createSNR(byte[],
	 *      com.alcatel_lucent.as.ims.diameter.sh.ShConstants.SubsReqType,
	 *      com.alcatel_lucent.as.ims.diameter.sh.ShConstants.DataReference[])
	 */
	public SubscribeNotificationsRequest createSNR(byte[] msisdn, SubsReqType subsReqType, DataReference... dataReferences) {
		DiameterClientRequest request = newRequest(ShConstants.SUBSCRIBE_NOTIFICATION_COMMAND, true);

		SubscribeNotificationsRequest snr = new SnrImpl(request, getVersion());
		snr.setUserIdentity(msisdn);
		snr.setSubsReqType(subsReqType);
		for (DataReference dataReference : dataReferences) {
			snr.addDataReference(dataReference);
		}

		return snr;
	}

	private final static Long SH_APPLICATION_ID_V5 = Long.valueOf(167772152); // 0x09fffff8
	private final static Long SH_APPLICATION_ID_V6 = Long.valueOf(16777217); // 0x01000001
	private final static Version VERSION_5_7 = new Version(5, 7);
	private final static Version VERSION_6_2 = new Version(6, 2);

	/**
	 * Gets the Sh application id.
	 * 
	 * @return The Sh application id or null if version is null.
	 */
	public Long getShApplicationId() {
		if (getVersion().getMajor() >= 6) {
			if (VERSION_6_2.compareTo(getVersion()) <= 0) {
				return SH_APPLICATION_ID_V6;
			}
			return SH_APPLICATION_ID_V5;
		}
		if (VERSION_5_7.compareTo(getVersion()) <= 0) {
			return SH_APPLICATION_ID_V5;
		}
		return SH_APPLICATION_ID_V6;
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.common.ImsClient#getApplicationId()
	 */
	@Override
	protected long getApplicationId() {
		return getShApplicationId();
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.common.AbstractClient#getVendorId()
	 */
	@Override
	public long getVendorId() {
		return ShUtils.THREEGPP_VENDOR_ID;
	}

}
