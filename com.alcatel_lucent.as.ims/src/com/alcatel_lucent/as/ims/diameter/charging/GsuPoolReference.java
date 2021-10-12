// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.CcUnitType;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The G-S-U-Pool-Reference AVP wrapper.
 */
public class GsuPoolReference {

	private Long _gsuPoolIdentifier = null;
	private CcUnitType _ccUnitType = null;
	private UnitValue _unitValue = null;

	private GsuPoolReference() {}

	/**
	 * Constructor for this class.
	 * 
	 * @param id The G-S-U-Pool-Identifier.
	 * @param type The CC-Unit-Type.
	 * @param value The Unit-Value.
	 */
	public GsuPoolReference(Long id, CcUnitType type, UnitValue value) {
		this();
		setGsuPoolIdentifier(id);
		setCcUnitType(type);
		setUnitValue(value);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param data The avp data.
	 * @exception DiameterMissingAVPException if a mandatory AVP is missing.
	 */
	public GsuPoolReference(byte[] data)
			throws DiameterMissingAVPException {
		this();
		if (data == null) {
			throw new IllegalArgumentException("null parameter");
		}

		DiameterAVPDefinition def = ChargingUtils.getGsuPoolIdentifierAVP();
		DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp == null) {
			throw new DiameterMissingAVPException(def);
		}
		setGsuPoolIdentifier(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));

		def = ChargingUtils.getCcUnitTypeAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp == null) {
			throw new DiameterMissingAVPException(def);
		}
		setCcUnitType(CcUnitType.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));

		def = ChargingUtils.getCcUnitTypeAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp == null) {
			throw new DiameterMissingAVPException(def);
		}
		setUnitValue(new UnitValue(searchedAvp));
	}

	/**
	 * Creates a grouped AVP.
	 * 
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp() {
		DiameterAVPDefinition def = ChargingUtils.getGsuPoolReferenceAVP();
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		def = ChargingUtils.getGsuPoolIdentifierAVP();
		DiameterAVP avp = new DiameterAVP(def);
		avp.setValue(Unsigned32Format.toUnsigned32(getGsuPoolIdentifier()), false);
		l.add(avp);

		def = ChargingUtils.getCcUnitTypeAVP();
		avp = new DiameterAVP(def);
		avp.setValue(EnumeratedFormat.toEnumerated(getCcUnitType().getValue()), false);
		l.add(avp);
		
		avp = getUnitValue().toAvp();
		l.add(avp);
		
		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the G-S-U-Pool-Identifier.
	 * 
	 * @param id The id.
	 */
	protected void setGsuPoolIdentifier(Long id) {
		if (id == null) {
			throw new NullPointerException("id is null");
		}
		_gsuPoolIdentifier = id;
	}

	/**
	 * Gets the G-S-U-Pool-Identifier.
	 * 
	 * @return The id.
	 */
	public Long getGsuPoolIdentifier() {
		return _gsuPoolIdentifier;
	}

	/**
	 * Sets the CC-Unit-Type.
	 * 
	 * @param type The type.
	 */
	protected void setCcUnitType(CcUnitType type) {
		if (type == null) {
			throw new NullPointerException("type is null");
		}
		_ccUnitType = type;
	}

	/**
	 * Gets the CC-Unit-Type.
	 * 
	 * @return The type.
	 */
	public CcUnitType getCcUnitType() {
		return _ccUnitType;
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

}
