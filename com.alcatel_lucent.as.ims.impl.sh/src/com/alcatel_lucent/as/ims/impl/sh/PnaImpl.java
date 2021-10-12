// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.impl.sh;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.diameter.sh.PushNotificationAnswer;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterResponse;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.Integer32Format;

/**
 * The PnaImpl class.
 */
public class PnaImpl
		implements PushNotificationAnswer {

	DiameterResponse _response = null;

	public PnaImpl(DiameterResponse response) {
		if (response == null) {
			throw new NullPointerException("PnaImpl: Response is null");
		}
		_response = response;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.PushNotificationAnswer#addAvp(com.nextenso.proxylet.diameter.DiameterAVP)
	 */
	public void addAvp(DiameterAVP avp) {
		_response.addDiameterAVP(avp);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.PushNotificationAnswer#setExperimentalResult(int,
	 *      int)
	 */
	public void setExperimentalResult(int vendorId, int experimentalResultCode) {
		if (vendorId < 0 || experimentalResultCode < 0) {
			return;
		}
		
		DiameterAVP vendor = new DiameterAVP(DiameterBaseConstants.AVP_VENDOR_ID);
		vendor.setValue(Integer32Format.toInteger32(vendorId), false);
		
		DiameterAVP experimentalCode = new DiameterAVP(DiameterBaseConstants.AVP_EXPERIMENTAL_RESULT_CODE);
		experimentalCode.setValue(Integer32Format.toInteger32(experimentalResultCode), false);
		List<DiameterAVP> avps = new ArrayList<DiameterAVP>();
		avps.add(vendor);
		avps.add(experimentalCode);
		
		DiameterAVP res = new DiameterAVP(DiameterBaseConstants.AVP_EXPERIMENTAL_RESULT);
		res.setValue(GroupedFormat.toGroupedAVP(avps), false);
		addAvp(res);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.PushNotificationAnswer#setResultCode(long)
	 */
	public void setResultCode(long code) {
		_response.setResultCode(code);
	}
}
