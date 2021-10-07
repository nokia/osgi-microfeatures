package com.alcatel_lucent.as.ims.impl.cx;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.common.AbstractImsRequest;
import com.alcatel_lucent.as.ims.diameter.cx.CxUtils;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;

/**
 * The Cx Request Implementation.
 */
public class CxRequestImpl
		extends AbstractImsRequest {

	/**
	 * 
	 * Constructor for this class.
	 * 
	 * @param request The diameter request.
	 * @param version
	 */
	public CxRequestImpl(DiameterClientRequest request, Version version) {
		super(request, version);
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.common.ImsRequest#setPublicUserID(java.lang.String)
	 */
	@Override
	public void setPublicUserID(String id) {
		DiameterAVPDefinition def = CxUtils.getPublicIdentityAVP(getVersion());
		setUTF8StringAVP(id, def);
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.common.ImsRequest#setServerName(java.lang.String)
	 */
	@Override
	public void setServerName(String scscfName) {
		DiameterAVPDefinition def = CxUtils.getServerNameAVP(getVersion());
		setUTF8StringAVP(scscfName, def);
	}

}
