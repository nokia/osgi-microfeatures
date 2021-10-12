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
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The Wlan-Radio-Container AVP wrapper.
 * 
 * The AVPs Operator-Name and Location-Information are not present yet. The
 * definitions of these AVPs are unknown.
 */
public class WlanRadioContainer {

	// private Object _operatorName = null;
	private LocationType _locationType = null;
	// private Object _locationInformation = null;
	private Long _wlanTechnology = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public WlanRadioContainer(DiameterAVP avp, Version version) {
		this(avp.getValue(), version);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param data The avp data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public WlanRadioContainer(byte[] data, Version version) {
		if (data == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		DiameterAVPDefinition def = ChargingUtils.getLocationTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setLocationType(new LocationType(searchedAvp, version));
			}
		}

		def = ChargingUtils.getWlanTechnologyAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setWlanTechnology(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
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
		DiameterAVPDefinition def = ChargingUtils.getWlanRadioContainerAVP(version);
		if (def == null) {
			return null;
		}

		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		if (getLocationType() != null) {
			DiameterAVP avp = getLocationType().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getWlanTechnology() != null) {
			def = ChargingUtils.getWlanTechnologyAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(getWlanTechnology()), false);
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the Location-Type.
	 * 
	 * @param type The type.
	 */
	public void setLocationType(LocationType type) {
		_locationType = type;
	}

	/**
	 * Gets the Location-Type.
	 * 
	 * @return The type.
	 */
	public LocationType getLocationType() {
		return _locationType;
	}

	/**
	 * Sets the WLAN-Technology.
	 * 
	 * @param technology The technology.
	 */
	public void setWlanTechnology(Long technology) {
		_wlanTechnology = technology;
	}

	/**
	 * Gets the WLAN-Technology.
	 * 
	 * @return The technology.
	 */
	public Long getWlanTechnology() {
		return _wlanTechnology;
	}

}
