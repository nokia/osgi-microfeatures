// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.impl.sh;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.common.AbstractImsRequest;
import com.alcatel_lucent.as.ims.diameter.sh.ShRequest;
import com.alcatel_lucent.as.ims.diameter.sh.ShUtils;
import com.alcatel_lucent.as.ims.diameter.sh.UserIdentity;
import com.alcatel_lucent.as.ims.diameter.sh.ShConstants.DataReference;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;

/**
 * The ShRequest Implementation.
 */
public class ShRequestImpl
		extends AbstractImsRequest
		implements ShRequest {

	/**
	 * Constructor for this class.
	 * 
	 * @param request The request.
	 * @param version The version.
	 */
	public ShRequestImpl(DiameterClientRequest request, Version version) {
		super(request, version);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.ShRequest#setUserIdentity(java.lang.String)
	 */
	public void setUserIdentity(String userIdentity) {
		UserIdentity avp = new UserIdentity(userIdentity);
		setUserIdentity(avp);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.ShRequest#setUserIdentity(byte[])
	 */
	public void setUserIdentity(byte[] msisdn) {
		UserIdentity avp = new UserIdentity(msisdn);
		setUserIdentity(avp);
	}

	/**
	 * Sets the IMS Public Identity or MSISDN of the user for whom the data is
	 * required. Mapped to the User-Identity diameter AVP.
	 */
	private void setUserIdentity(UserIdentity userIdentity) {
		synchronized (getRequest()) {
			if (userIdentity == null) {
				getRequest().removeDiameterAVP(ShUtils.getUserIdentityAvpDefinition(getVersion()));
				return;
			}
			DiameterAVP avp = getRequest().getDiameterAVP(ShUtils.getUserIdentityAvpDefinition(getVersion()));
			DiameterAVP newIdentity = userIdentity.toAvp(getVersion());
			if (avp != null) {
				avp.setValue(newIdentity.getValue(), false);
			} else {
				getRequest().addDiameterAVP(newIdentity);
			}
		}
	}

	/**
	 * Sets the Data reference.
	 * 
	 * @param dataReference The data reference.
	 */
	public void setDataReference(DataReference dataReference) {
		DiameterAVPDefinition def = ShUtils.getDataReferenceAvpDefinition(getVersion());
		if (def == null) {
			return;
		}
		if (dataReference == null) {
			getRequest().removeDiameterAVP(def);
		} else {
			setEnumeratedAVP(dataReference.getValue(), def);
		}
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.common.AbstractImsRequest#setPublicUserID(java.lang.String)
	 */
	@Override
	public void setPublicUserID(String id) {
		DiameterAVPDefinition def = ShUtils.getPublicIdentityAvpDefinition(getVersion());
		if (def == null) {
			return;
		}
		if (id == null) {
			getRequest().removeDiameterAVP(def);
		} else {
			setUTF8StringAVP(id, def);
		}
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.common.AbstractImsRequest#setServerName(java.lang.String)
	 */
	@Override
	public void setServerName(String scscfName) {
		setUTF8StringAVP(scscfName, ShUtils.getServerNameAvpDefinition(getVersion()));
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.ShRequest#setWildcardedPSI(java.lang.String)
	 */
	public void setWildcardedPSI(String psi) {
		DiameterAVPDefinition def = ShUtils.getWildcardedPSIAVP(getVersion());
		if (def == null) {
			return;
		}
		if (psi == null) {
			getRequest().removeDiameterAVP(def);
		} else {
			setUTF8StringAVP(psi, def);
		}
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.sh.ShRequest#setWildcardedIMPU(java.lang.String)
	 */
	public void setWildcardedIMPU(String impu) {
		setUTF8StringAVP(impu, ShUtils.getWildcardedIMPUAVP(getVersion()));
	}
}
