// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.impl.cx;

import java.net.NoRouteToHostException;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.common.AbstractClient;
import com.alcatel_lucent.as.ims.diameter.cx.CxClient;
import com.alcatel_lucent.as.ims.diameter.cx.CxConstants;
import com.alcatel_lucent.as.ims.diameter.cx.CxUtils;
import com.alcatel_lucent.as.ims.diameter.cx.LocationInfoRequest;
import com.alcatel_lucent.as.ims.diameter.cx.MultimediaAuthRequest;
import com.alcatel_lucent.as.ims.diameter.cx.ServerAssignmentRequest;
import com.alcatel_lucent.as.ims.diameter.cx.UserAuthorizationRequest;
import com.alcatel_lucent.as.ims.diameter.cx.CxConstants.ServerAssignmentType;
import com.alcatel_lucent.as.ims.diameter.cx.CxConstants.UserDataAlreadyAvailable;
import com.alcatel_lucent.as.ims.diameter.cx.CxConstants.UserDataRequestType;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;

/**
 * The Cx Client Implementation.
 */
public class CxClientImpl
		extends AbstractClient
		implements CxClient {

	private final static Version VERSION_6_2 = new Version(6, 2);

	// before the v6.2
	private final static long CX_APPLICATION_ID_V5 = 167772151; // 0x09fffff7
	// after the v6.2
	private final static long CX_APPLICATION_ID_V6 = 16777216; // 0x01000000

	public static long getCxApplicationId(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return CX_APPLICATION_ID_V6;
		}
		return CX_APPLICATION_ID_V5;
	}

	public CxClientImpl(String destinationHost, String destinationRealm, Version version)
			throws NoRouteToHostException {
		super(destinationHost, destinationRealm, version);
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.CxClient#createUAR(java.lang.String,
	 *      java.lang.String, byte[])
	 */
	public UserAuthorizationRequest createUAR(String privateUserID, String publicUserID, byte[] visitedNetworkId) {
		DiameterClientRequest request = newRequest(CxConstants.USER_AUTHORIZATION_COMMAND_CODE, true);

		UarImpl uar = new UarImpl(request, getVersion());
		uar.setUserName(privateUserID);
		uar.setPublicUserID(publicUserID);
		uar.setVisitedNetworkId(visitedNetworkId);

		return uar;
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.CxClient#createLIR(java.lang.String)
	 */
	public LocationInfoRequest createLIR(String publicUserID) {
		DiameterClientRequest request = newRequest(CxConstants.LOCATION_INFO_COMMAND_CODE, true);

		LirImpl lir = new LirImpl(request, getVersion());
		lir.setPublicUserID(publicUserID);

		return lir;
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.CxClient#createMAR(java.lang.String,
	 *      java.lang.String, java.lang.String)
	 */
	public MultimediaAuthRequest createMAR(String privateIdentity, String userName, String serverName) {
		DiameterClientRequest request = newRequest(CxConstants.MULTIMEDIA_AUTH_COMMAND_CODE, true);

		MarImpl mar = new MarImpl(request, getVersion());
		mar.setUserName(privateIdentity);
		mar.setPublicUserID(userName);
		mar.setServerName(serverName);
		return mar;
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.CxClient#createSAR(java.lang.String,
	 *      com.alcatel_lucent.as.interfaces.gpp.diameter.cx.CxConstants.ServerAssignmentType,
	 *      com.alcatel_lucent.as.interfaces.gpp.diameter.cx.CxConstants.UserDataRequestType,
	 *      com.alcatel_lucent.as.interfaces.gpp.diameter.cx.CxConstants.UserDataAlreadyAvailable)
	 */
	public ServerAssignmentRequest createSAR(String serverName, ServerAssignmentType serverAssignmentType, UserDataRequestType requestType,
			UserDataAlreadyAvailable userProfileAlreadyAvailable) {
		DiameterClientRequest request = newRequest(CxConstants.SERVER_ASSIGNMENT_COMMAND_CODE, true);

		SarImpl sar = new SarImpl(request, getVersion());
		sar.setServerName(serverName);
		sar.setServerAssignmentType(serverAssignmentType);
		sar.setUserProfileAlreadyAvailable(userProfileAlreadyAvailable);

		return sar;
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.common.ImsClient#getApplicationId()
	 */
	@Override
	protected long getApplicationId() {
		return getCxApplicationId(getVersion());
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.common.ImsClient#getVendorId()
	 */
	@Override
	public long getVendorId() {
		return CxUtils.THREEGPP_VENDOR_ID;
	}

}
