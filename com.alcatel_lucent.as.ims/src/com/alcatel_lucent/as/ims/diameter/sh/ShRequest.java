// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.sh;

import com.alcatel_lucent.as.ims.diameter.ImsRequest;
import com.alcatel_lucent.as.ims.diameter.SupportedFeatures;

/**
 * The ShRequest.
 */
public interface ShRequest
		extends ImsRequest {

	/**
	 * Adds a feature to the list of supported features.
	 * 
	 * @param feature The feature to be added
	 */
	public void addSupportedFeature(SupportedFeatures feature);

	/**
	 * Sets the user identity with the public identity.
	 * 
	 * @param publicIdentity The public identity.
	 */
	public void setUserIdentity(String publicIdentity);

	/**
	 * Sets the user identity with the msisdn.
	 * 
	 * @param msisdn The msisdn.
	 */
	public void setUserIdentity(byte[] msisdn);

	/**
	 * Sets the Wildcarded PSI.
	 * 
	 * @param psi The Wildcarded PSI.
	 */
	public void setWildcardedPSI(String psi);

	/**
	 * Sets the Wilcarded public user identity.
	 * 
	 * @param impu The identity.
	 */
	public void setWildcardedIMPU(String impu);

}
