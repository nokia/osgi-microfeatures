// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The CC-Money AVP wrapper.
 */
public class CcMoney {

	private UnitValue _unitValue = null;
	private Long _currencyCode = null;

	private CcMoney() {}

	/**
	 * Constructor for this class.
	 * 
	 * @param value The Unit-Value.
	 */
	public CcMoney(UnitValue value) {
		this();
		setUnitValue(value);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @exception DiameterMissingAVPException if the Unit-Value avp is not present in
	 *              the avp.
	 */
	public CcMoney(DiameterAVP avp)
			throws DiameterMissingAVPException {
		this();
		if (avp == null) {
			throw new IllegalArgumentException("null parameter");
		}
		byte[] data = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.getUnitValueAVP();
		DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp == null) {
			throw new DiameterMissingAVPException(def);
		}
		setUnitValue(new UnitValue(searchedAvp));

		def = ChargingUtils.getCurrencyCodeAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			setCurrencyCode(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
		}
	}

	/**
	 * Creates a grouped AVP.
	 * 
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp() {
		DiameterAVPDefinition def = ChargingUtils.getCcMoneyAVP();
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> avps = new ArrayList<DiameterAVP>();

		DiameterAVP avp = getUnitValue().toAvp();
		avps.add(avp);

		if (getCurrencyCode() != null) {
			def = ChargingUtils.getCurrencyCodeAVP();
			avp = new DiameterAVP(def);
			avp.setValue(Unsigned32Format.toUnsigned32(getCurrencyCode()), false);
			avps.add(avp);
		}

		res.setValue(GroupedFormat.toGroupedAVP(avps), false);
		return res;
	}

	/**
	 * Sets the Unit-Value.
	 * 
	 * @param value The value.
	 */
	protected void setUnitValue(UnitValue value) {
		if (value == null) {
			throw new NullPointerException("value is null");
		}
		_unitValue = value;
	}

	/**
	 * Gets the Unit-Value.
	 * 
	 * @return The value.
	 */
	public UnitValue getUnitValue() {
		return _unitValue;
	}

	/**
	 * Sets the Currency-Code.
	 * 
	 * @param code The code.
	 */
	public void setCurrencyCode(Long code) {
		_currencyCode = code;
	}

	/**
	 * Gets the Currency-Code.
	 * 
	 * @return The code.
	 */
	public Long getCurrencyCode() {
		return _currencyCode;
	}

}
