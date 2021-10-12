// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.LcsFormatIndicator;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * The LCS-Client-Name AVP wrapper.
 */
public class LcsClientName {

	private String _lcsDataCodingScheme = null;
	private String _lcsNameString = null;
	private LcsFormatIndicator _lcsFormatIndicator = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public LcsClientName(DiameterAVP avp, Version version) {
		if (avp == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}
		byte[] data = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.getLcsDataCodingSchemeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setLcsDataCodingScheme(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getLcsNameStringAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setLcsDataCodingScheme(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getLcsFormatIndicatorAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setLcsFormatIndicator(LcsFormatIndicator.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
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
		DiameterAVPDefinition def = ChargingUtils.getLcsClientNameAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> avps = new ArrayList<DiameterAVP>();

		if (getLcsDataCodingScheme() != null) {
			def = ChargingUtils.getLcsDataCodingSchemeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getLcsDataCodingScheme()), false);
				avps.add(avp);
			}
		}

		if (getLcsNameString() != null) {
			def = ChargingUtils.getLcsNameStringAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getLcsNameString()), false);
				avps.add(avp);
			}
		}

		if (getLcsFormatIndicator() != null) {
			def = ChargingUtils.getLcsFormatIndicatorAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getLcsFormatIndicator().getValue()), false);
				avps.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(avps), false);
		return res;
	}

	/**
	 * Sets the LCS-Data-Coding-Scheme.
	 * 
	 * @param scheme The scheme.
	 */
	public void setLcsDataCodingScheme(String scheme) {
		_lcsDataCodingScheme = scheme;
	}

	/**
	 * Gets the LCS-Data-Coding-Scheme.
	 * 
	 * @return The scheme.
	 */
	public String getLcsDataCodingScheme() {
		return _lcsDataCodingScheme;
	}

	/**
	 * Sets the LCS-Name-String.
	 * 
	 * @param name The name.
	 */
	public void setLcsNameString(String name) {
		_lcsNameString = name;
	}

	/**
	 * Gets the LCS-Name-String.
	 * 
	 * @return The name.
	 */
	public String getLcsNameString() {
		return _lcsNameString;
	}

	/**
	 * Sets the LCS-Format-Indicator.
	 * 
	 * @param indicator The indicator.
	 */
	public void setLcsFormatIndicator(LcsFormatIndicator indicator) {
		_lcsFormatIndicator = indicator;
	}

	/**
	 * Gets the LCS-Format-Indicator.
	 * 
	 * @return The indicator.
	 */
	public LcsFormatIndicator getLcsFormatIndicator() {
		return _lcsFormatIndicator;
	}

}
