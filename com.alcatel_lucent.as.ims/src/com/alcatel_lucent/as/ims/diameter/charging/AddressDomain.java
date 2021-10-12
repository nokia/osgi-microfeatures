// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * The Address-Domain AVP wrapper.
 */
public class AddressDomain {

	private String _domainName = null;
	private String _3gppImsiMccMnc = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public AddressDomain(DiameterAVP avp, Version version) {
		if (avp == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}
		byte[] data = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.getDomainNameAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setDomainName(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.get3gppImsiMccMncAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				set3gppImsiMccMnc(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}
	}

	/**
	 * Creates a grouped AVP.
	 * 
	 * @param version The version of the 3GPP 32.299 document.
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp(Version version) {
		DiameterAVPDefinition def = ChargingUtils.getOriginatorAddressAVP(version);
		if (def == null) {
			return null;
		}
		
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();
		if (getDomainName() != null) {
			def = ChargingUtils.getDomainNameAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getDomainName()), false);
				l.add(avp);
			}
		}

		if (get3gppImsiMccMnc() != null) {
			def = ChargingUtils.get3gppImsiMccMncAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(get3gppImsiMccMnc()), false);
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the Domain-Name.
	 * 
	 * @param name The name.
	 */
	public void setDomainName(String name) {
		_domainName = name;
	}

	/**
	 * Gets the Domain-Name.
	 * 
	 * @return The name.
	 */
	public String getDomainName() {
		return _domainName;
	}

	/**
	 * Sets the 3GPP-IMSI-MCC-MNC.
	 * 
	 * @param data The data.
	 */
	public void set3gppImsiMccMnc(String data) {
		_3gppImsiMccMnc = data;
	}

	/**
	 * Gets the 3GPP-IMSI-MCC-MNC.
	 * 
	 * @return The data.
	 */
	public String get3gppImsiMccMnc() {
		return _3gppImsiMccMnc;
	}

}
