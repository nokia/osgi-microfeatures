// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.TimeFormat;
import com.nextenso.proxylet.diameter.util.Unsigned64Format;

/**
 * The Envelope AVP wrapper.
 */
public class Envelope {

	private Date _envelopeStartTime = null;
	private Date _envelopeEndTime = null;
	private Long _ccTotalOctets = null;
	private Long _ccInputOctets = null;
	private Long _ccOutputOctets = null;
	private Long _ccServiceSpecificUnits = null;
	private List<DiameterAVP> _avps = new ArrayList<DiameterAVP>();

	private Envelope() {}

	/**
	 * 
	 * Constructor for this class.
	 * 
	 * @param startTime The Envelope-Start-Time.
	 */
	public Envelope(Date startTime) {
		this();
		setEnvelopeStartTime(startTime);
	}

	/**
	 * 
	 * Constructor for this class.
	 * 
	 * @param data The avp data to be parsed.
	 * @param version The version of the 3GPP 32.299 document.
	 * @exception DiameterMissingAVPException if a mandatory AVP is missing.
	 * 
	 */

	public Envelope(byte[] data, Version version)
			throws DiameterMissingAVPException {
		if (data == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		DiameterAVPDefinition def = ChargingUtils.getEnvelopeStartTimeAVP(version);
		DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp == null) {
			throw new DiameterMissingAVPException(def);
		}
		setEnvelopeStartTime(new Date(TimeFormat.getTime(searchedAvp.getValue(), 0)));

		def = ChargingUtils.getEnvelopeEndTimeAVP(version);
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			setEnvelopeEndTime(new Date(TimeFormat.getTime(searchedAvp.getValue(), 0)));
		}

		def = ChargingUtils.getCcTotalOctetsAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			setCcTotalOctets(Long.valueOf(Unsigned64Format.getUnsigned64Long(searchedAvp.getValue(), 0)));
		}

		def = ChargingUtils.getCcInputOctetsAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			setCcInputOctets(Long.valueOf(Unsigned64Format.getUnsigned64Long(searchedAvp.getValue(), 0)));
		}

		def = ChargingUtils.getCcOutputOctetsAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			setCcOutputOctets(Long.valueOf(Unsigned64Format.getUnsigned64Long(searchedAvp.getValue(), 0)));
		}

		def = ChargingUtils.getCcServiceSpecificUnitsAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			setCcServiceSpecificUnits(Long.valueOf(Unsigned64Format.getUnsigned64Long(searchedAvp.getValue(), 0)));
		}

		List<DiameterAVP> avps = GroupedFormat.getGroupedAVPs(data, false);
		for (DiameterAVP a : avps) {
			def = a.getDiameterAVPDefinition();
			if (def == ChargingUtils.getEnvelopeStartTimeAVP(version) || def == ChargingUtils.getEnvelopeEndTimeAVP(version)
					|| def == ChargingUtils.getCcTotalOctetsAVP() || def == ChargingUtils.getCcInputOctetsAVP() || def == ChargingUtils.getCcOutputOctetsAVP()
					|| def == ChargingUtils.getCcServiceSpecificUnitsAVP()) {
				continue;
			}
			addAvp(a);
		}
	}

	public DiameterAVP toAvp(Version version) {
		DiameterAVPDefinition def = ChargingUtils.getEnvelopeAVP(version);
		if (def == null) {
			return null;
		}

		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		def = ChargingUtils.getEnvelopeStartTimeAVP(version);
		if (def != null) {
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(TimeFormat.toTime(getEnvelopeStartTime().getTime()), false);
			l.add(avp);
		}

		if (getEnvelopeEndTime() != null) {
			def = ChargingUtils.getEnvelopeEndTimeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(TimeFormat.toTime(getEnvelopeEndTime().getTime()), false);
				l.add(avp);
			}
		}

		if (getCcTotalOctets() != null) {
			def = ChargingUtils.getCcTotalOctetsAVP();
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(Unsigned64Format.toUnsigned64(getCcTotalOctets()), false);
			l.add(avp);
		}

		if (getCcInputOctets() != null) {
			def = ChargingUtils.getCcInputOctetsAVP();
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(Unsigned64Format.toUnsigned64(getCcInputOctets()), false);
			l.add(avp);
		}

		if (getCcOutputOctets() != null) {
			def = ChargingUtils.getCcOutputOctetsAVP();
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(Unsigned64Format.toUnsigned64(getCcOutputOctets()), false);
			l.add(avp);
		}

		if (getCcServiceSpecificUnits() != null) {
			def = ChargingUtils.getCcServiceSpecificUnitsAVP();
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(Unsigned64Format.toUnsigned64(getCcServiceSpecificUnits()), false);
			l.add(avp);
		}

		for (DiameterAVP avp : getAvps()) {
			l.add(avp);
		}
		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the Envelope-Start-Time.
	 * 
	 * @param time The time.
	 */
	public void setEnvelopeStartTime(Date time) {
		if (time == null) {
			throw new NullPointerException("time is null");
		}
		_envelopeStartTime =  (Date) time.clone();
	}

	/**
	 * Gets the Envelope-Start-Time.
	 * 
	 * @return The time.
	 */
	public Date getEnvelopeStartTime() {
		if (_envelopeStartTime == null) {
			return null;
		}
		return (Date) _envelopeStartTime.clone();
	}

	/**
	 * Sets the Envelope-End-Time.
	 * 
	 * @param time The time.
	 */
	public void setEnvelopeEndTime(Date time) {
		if (time == null) {
			_envelopeEndTime = null;
		} else {
			_envelopeEndTime = (Date) time.clone();
		}
	}

	/**
	 * Gets the Envelope-End-Time.
	 * 
	 * @return The time.
	 */
	public Date getEnvelopeEndTime() {
		if (_envelopeEndTime == null) {
			return null;
		}
		return (Date) _envelopeEndTime.clone();
	}

	/**
	 * Sets the CC-Total-Octets.
	 * 
	 * @param number The number.
	 */
	public void setCcTotalOctets(Long number) {
		_ccTotalOctets = number;
	}

	/**
	 * Gets the CC-Total-Octets.
	 * 
	 * @return The number.
	 */
	public Long getCcTotalOctets() {
		return _ccTotalOctets;
	}

	/**
	 * Sets the CC-Input-Octets.
	 * 
	 * @param number The number.
	 */
	public void setCcInputOctets(Long number) {
		_ccInputOctets = number;
	}

	/**
	 * Gets the CC-Input-Octets.
	 * 
	 * @return The number.
	 */
	public Long getCcInputOctets() {
		return _ccInputOctets;
	}

	/**
	 * Sets the CC-Output-Octets.
	 * 
	 * @param number The number.
	 */
	public void setCcOutputOctets(Long number) {
		_ccOutputOctets = number;
	}

	/**
	 * Gets the CC-Output-Octets.
	 * 
	 * @return The number.
	 */
	public Long getCcOutputOctets() {
		return _ccOutputOctets;
	}

	/**
	 * Sets the CC-Services-Specific-Units.
	 * 
	 * @param units The units.
	 */
	public void setCcServiceSpecificUnits(Long units) {
		_ccServiceSpecificUnits = units;
	}

	/**
	 * Gets the CC-Services-Specific-Units.
	 * 
	 * @return The units.
	 */
	public Long getCcServiceSpecificUnits() {
		return _ccServiceSpecificUnits;
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

}
