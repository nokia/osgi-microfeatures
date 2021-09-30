package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.Date;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;

/**
 * The Requested-Service-Unit AVP wrapper.
 */
public class RequestedServiceUnit
		extends AbstractServiceUnit {

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp .
	 * @param version The version of the 3GPP 32.299 document.
	 * @exception DiameterMissingAVPException if a mandatory AVP is missing.
	 */
	public RequestedServiceUnit(DiameterAVP avp, Version version)
			throws DiameterMissingAVPException {
		super(avp, version);
	}

	/**
	 * Does nothing.
	 * 
	 * @see com.alcatel_lucent.as.ims.diameter.charging.AbstractServiceUnit#setTariffTimeChange(java.util.Date)
	 */
	@Override
	public void setTariffTimeChange(Date time) {}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.AbstractServiceUnit#getAVPdefinition(com.alcatel_lucent.as.ims.Version)
	 */
	@Override
	protected DiameterAVPDefinition getAVPdefinition(Version version) {
		return ChargingUtils.getRequestedServiceUnitAVP();
	}

}
