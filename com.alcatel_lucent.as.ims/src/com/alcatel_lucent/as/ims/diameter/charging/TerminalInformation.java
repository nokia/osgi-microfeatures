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
 * The Terminal-Information AVP wrapper.
 */
public class TerminalInformation {

	private String _imei = null;
	private byte[] _3gpp2Meid = null;
	private String _softwareVersion = null;
	private List<DiameterAVP> _avps = new ArrayList<DiameterAVP>();

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public TerminalInformation(DiameterAVP avp, Version version) {
		byte[] data = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.getImeiAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setImei(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.get3gpp2MeidAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				set3gpp2Meid(searchedAvp.getValue());
			}
		}

		def = ChargingUtils.getSoftwareVersionAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setSoftwareVersion(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		List<DiameterAVP> avps = GroupedFormat.getGroupedAVPs(data, false);
		for (DiameterAVP a : avps) {
			def = a.getDiameterAVPDefinition();
			if (def == ChargingUtils.getImeiAVP(version) || def == ChargingUtils.get3gpp2MeidAVP(version)
					|| def == ChargingUtils.getSoftwareVersionAVP(version)) {
				continue;
			}
			addAvp(a);
		}
	}

	/**
	 * Creates a grouped AVP.
	 * 
	 * @param version The version of the 3GPP 32.299 document.
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp(Version version) {
		DiameterAVPDefinition def = ChargingUtils.getTerminalInformationAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		if (getImei() != null) {
			def = ChargingUtils.getImeiAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getImei()), false);
				l.add(avp);
			}
		}

		if (get3gpp2Meid() != null) {
			def = ChargingUtils.get3gpp2MeidAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(get3gpp2Meid(), false);
				l.add(avp);
			}
		}

		if (getSoftwareVersion() != null) {
			def = ChargingUtils.getSoftwareVersionAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getSoftwareVersion()), false);
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Adds an AVP.
	 * 
	 * @param avp The avp to be added.
	 */
	public void addAvp(DiameterAVP avp) {
		if (avp != null) {
			_avps.add(avp);
		}

	}

	/**
	 * Gets the AVP list.
	 * 
	 * @return The avps.
	 */
	public Iterable<DiameterAVP> getAvps() {
		return _avps;
	}

	/**
	 * Sets the IMEI.
	 * 
	 * @param identity The identity.
	 */
	public void setImei(String identity) {
		_imei = identity;
	}

	/**
	 * Gets the IMEI.
	 * 
	 * @return The identity.
	 */
	public String getImei() {
		return _imei;
	}

	/**
	 * Sets the 3GPP2-MEID.
	 * 
	 * @param identifier The identifier.
	 */
	public void set3gpp2Meid(byte[] identifier) {
		_3gpp2Meid = ChargingUtils.copyArray(identifier);
	}

	/**
	 * Gets the 3GPP2-MEID.
	 * 
	 * @return The identifier.
	 */
	public byte[] get3gpp2Meid() {
		return ChargingUtils.copyArray(_3gpp2Meid);
	}

	/**
	 * Sets the Software-Version.
	 * 
	 * @param version The version.
	 */
	public void setSoftwareVersion(String version) {
		this._softwareVersion = version;
	}

	/**
	 * Gets the Software-Version.
	 * 
	 * @return The version.
	 */
	public String getSoftwareVersion() {
		return _softwareVersion;
	}

}
