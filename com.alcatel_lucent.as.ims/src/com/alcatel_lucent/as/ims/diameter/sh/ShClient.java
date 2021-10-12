// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.sh;

import com.alcatel_lucent.as.ims.diameter.ImsClient;
import com.alcatel_lucent.as.ims.diameter.sh.ShConstants.DataReference;
import com.alcatel_lucent.as.ims.diameter.sh.ShConstants.SubsReqType;

/**
 * The Sh Client.
 */
public interface ShClient
		extends ImsClient {

	/**
	 * Creates a new User-Data-Request with a public identity.
	 * 
	 * @param publicIdentity The public identity.
	 * @param dataReference The data reference.
	 * @return The new UDR.
	 */
	public UserDataRequest createUDR(String publicIdentity, DataReference... dataReference);

	/**
	 * Creates a new User-Data-Request with a public MSISDN.
	 * 
	 * @param msisdn The MSISDN.
	 * @param dataReference The data reference.
	 * @return The new UDR.
	 */
	public UserDataRequest createUDR(byte[] msisdn, DataReference... dataReference);

	/**
	 * Creates a new Profile Update Request with a public identity..
	 * 
	 * @param publicIdentity The public identity.
	 * @param userData The user data.
	 * @param dataReference The data reference.
	 * @return The new PUR.
	 */
	public ProfileUpdateRequest createPUR(String publicIdentity, byte[] userData, DataReference dataReference);

	/**
	 * Creates a new Profile Update Request with a msisdn.
	 * 
	 * @param msisdn The msisdn.
	 * @param userData The user data.
	 * @param dataReference The data reference.
	 * @return The new PUR.
	 */
	public ProfileUpdateRequest createPUR(byte[] msisdn, byte[] userData, DataReference dataReference);

	/**
	 * Creates a new Subscribe-Notifications-Request with a public identity.
	 * 
	 * @param publicIdentity The public identity.
	 * @param subsReqType The type of the request.
	 * @param dataReferences The data references.
	 * @return The new SNR.
	 */
	public SubscribeNotificationsRequest createSNR(String publicIdentity, SubsReqType subsReqType, DataReference... dataReferences);

	/**
	 * Creates a new Subscribe-Notifications-Request with a msisdn.
	 * 
	 * @param msisdn The msisdn.
	 * @param subsReqType The type of the request.
	 * @param dataReferences The data references.
	 * @return The new SNR.
	 */
	public SubscribeNotificationsRequest createSNR(byte[] msisdn, SubsReqType subsReqType, DataReference... dataReferences);

}
