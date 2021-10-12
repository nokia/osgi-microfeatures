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
import com.nextenso.proxylet.diameter.util.Unsigned32Format;
import com.nextenso.proxylet.diameter.util.Unsigned64Format;

/**
 * 
 * The Abstract class for the Service-Unit AVP wrappers.
 */
public abstract class AbstractServiceUnit {

	private Date _tariffTimeChange = null;
	private Long _ccTime = null;
	private CcMoney _ccMoney = null;
	private Long _ccTotalOctets = null;
	private Long _ccInputOctets = null;
	private Long _ccOutputOctets = null;
	private Long _ccServiceSpecificUnits = null;
	private List<DiameterAVP> _avps = new ArrayList<DiameterAVP>();

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp .
	 * @param version The version of the 3GPP 32.299 document.
	 * @exception DiameterMissingAVPException if a mandatory AVP is missing.
	 */
	public AbstractServiceUnit(DiameterAVP avp, Version version)
			throws DiameterMissingAVPException {
		this(avp.getValue(), version);
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
	public AbstractServiceUnit(byte[] data, Version version)
			throws DiameterMissingAVPException {
		if (data == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		DiameterAVPDefinition def = ChargingUtils.getTariffTimeChangeAVP();
		DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			setTariffTimeChange(new Date(TimeFormat.getTime(searchedAvp.getValue(), 0)));
		}

		def = ChargingUtils.getCcTimeAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			setCcTime(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
		}

		def = ChargingUtils.getCcMoneyAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			setCcMoney(new CcMoney(searchedAvp));
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
			if (def == ChargingUtils.getTariffTimeChangeAVP() || def == ChargingUtils.getCcTimeAVP() || def == ChargingUtils.getCcMoneyAVP()
					|| def == ChargingUtils.getCcTotalOctetsAVP() || def == ChargingUtils.getCcInputOctetsAVP() || def == ChargingUtils.getCcOutputOctetsAVP()
					|| def == ChargingUtils.getCcServiceSpecificUnitsAVP()) {
				continue;
			}
			addAvp(a);
		}
	}

	protected abstract DiameterAVPDefinition getAVPdefinition(Version version);

	/**
	 * Creates a grouped AVP.
	 * 
	 * @param version The version of the 3GPP 32.299 document.
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp(Version version) {
		DiameterAVPDefinition def = getAVPdefinition(version);
		if (def == null) {
			return null;
		}

		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		fillAvp(l, version);

		for (DiameterAVP avp : getAvps()) {
			l.add(avp);
		}
		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;

	}

	/**
	 * Fills the AVP list for the toAvp method.
	 * 
	 * @param l The list where the AVP must be added
	 * @param version The version of the 3GPP 32.299 document.
	 */
	protected void fillAvp(List<DiameterAVP> l, Version version) {
		DiameterAVPDefinition def;
		if (getTariffTimeChange() != null) {
			def = ChargingUtils.getTariffTimeChangeAVP();
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(TimeFormat.toTime(getTariffTimeChange().getTime()), false);
			l.add(avp);
		}

		if (getCcTime() != null) {
			def = ChargingUtils.getCcTimeAVP();
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(Unsigned32Format.toUnsigned32(getCcTime()), false);
			l.add(avp);
		}

		if (getCcMoney() != null) {
			DiameterAVP avp = getCcMoney().toAvp();
			if (avp != null) {
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
	}

	/**
	 * Sets the Tariff-Time-Change.
	 * 
	 * @param time The time.
	 */
	public void setTariffTimeChange(Date time) {
		if (time == null) {
			_tariffTimeChange = null;
		} else {
			_tariffTimeChange = (Date) time.clone();
		}
	}

	/**
	 * Gets the Tariff-Time-Change.
	 * 
	 * @return The time.
	 */
	public Date getTariffTimeChange() {
		if (_tariffTimeChange == null) {
			return null;
		}
		return (Date) _tariffTimeChange.clone();
	}

	/**
	 * Sets the CC-Time.
	 * 
	 * @param time The time.
	 */
	public void setCcTime(Long time) {
		_ccTime = time;
	}

	/**
	 * Gets the CC-Time.
	 * 
	 * @return The time.
	 */
	public Long getCcTime() {
		return _ccTime;
	}

	/**
	 * Sets the CC-Money.
	 * 
	 * @param money The money.
	 */
	public void setCcMoney(CcMoney money) {
		_ccMoney = money;
	}

	/**
	 * Gets the CC-Money.
	 * 
	 * @return The money.
	 */
	public CcMoney getCcMoney() {
		return _ccMoney;
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
