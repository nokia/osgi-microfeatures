// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.TimeQuotaType;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The Time-Quota-Mechanism AVP wrapper.
 */
public class TimeQuotaMechanism {

	private TimeQuotaType _timeQuotaType = null;
	private Long _baseTimeInterval = null;

	private TimeQuotaMechanism() {}

	/**
	 * Constructor for this class.
	 * 
	 * @param type The Time-Quota-Type.
	 * @param interval The Base-Time-Interval.
	 */
	public TimeQuotaMechanism(TimeQuotaType type, Long interval) {
		this();

		if (type == null || interval < 0) {
			throw new IllegalArgumentException();
		}
		setTimeQuotaType(type);
		setBaseTimeInterval(interval);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public TimeQuotaMechanism(DiameterAVP avp, Version version) {
		this();
		if (avp == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		byte[] data = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.getTimeQuotaTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			setTimeQuotaType(TimeQuotaType.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
		}

		def = ChargingUtils.getBaseTimeIntervalAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			setBaseTimeInterval(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
		}
	}

	/**
	 * Creates a grouped AVP.
	 * 
	 * @param version The version of the 3GPP 32.299 document.
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp(Version version) {
		DiameterAVPDefinition def = ChargingUtils.getTimeQuotaMechanismAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();
		
		def = ChargingUtils.getTimeQuotaTypeAVP(version);
		if (def != null) {
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(EnumeratedFormat.toEnumerated(getTimeQuotaType().getValue()), false);
			l.add(avp);
		}

		def = ChargingUtils.getBaseTimeIntervalAVP(version);
		if (def != null) {
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(Unsigned32Format.toUnsigned32(getBaseTimeInterval()), false);
			l.add(avp);
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the Time-Quota-Type.
	 * 
	 * @param type The type.
	 */
	public void setTimeQuotaType(TimeQuotaType type) {
		_timeQuotaType = type;
	}

	/**
	 * Gets the Time-Quota-Type.
	 * 
	 * @return The type.
	 */
	public TimeQuotaType getTimeQuotaType() {
		return _timeQuotaType;
	}

	/**
	 * Sets the Base-Time-Interval.
	 * 
	 * @param interval The interval.
	 */
	public void setBaseTimeInterval(Long interval) {
		_baseTimeInterval = interval;
	}

	/**
	 * Gets the Base-Time-Interval.
	 * 
	 * @return The interval.
	 */
	public Long getBaseTimeInterval() {
		return _baseTimeInterval;
	}

}
