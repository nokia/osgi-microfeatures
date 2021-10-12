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
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.ReportingReason;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.TimeFormat;

/**
 * The Used-Service-Unit AVP wrapper.
 */
public class UsedServiceUnit
		extends AbstractServiceUnit {

	private ReportingReason _reportingReason = null;
	private List<Date> _eventChargingTimeStamps = new ArrayList<Date>();

	/**
	 * 
	 * Constructor for this class.
	 * 
	 * @param data The avp data to be parsed.
	 * @param version The version of the 3GPP 32.299 document.
	 * @exception DiameterMissingAVPException if a mandatory AVP is missing.
	 */
	public UsedServiceUnit(byte[] data, Version version)
			throws DiameterMissingAVPException {
		super(data, version);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.AbstractServiceUnit#getAVPdefinition(com.alcatel_lucent.as.ims.Version)
	 */
	@Override
	protected DiameterAVPDefinition getAVPdefinition(Version version) {
		return ChargingUtils.getUsedServiceUnitAVP();
	}

	/**
	 * Adds the own Avps.
	 * 
	 * @see com.alcatel_lucent.as.ims.diameter.charging.AbstractServiceUnit#fillAvp(java.util.List,
	 *      com.alcatel_lucent.as.ims.Version)
	 */
	@Override
	protected void fillAvp(List<DiameterAVP> l, Version version) {
		DiameterAVPDefinition def;
		if (getReportingReason() != null) {
			def = ChargingUtils.getReportingReasonAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getReportingReason().getValue()), false);
				l.add(avp);
			}
		}

		super.fillAvp(l, version);

		Iterable<Date> timestamps = getEventChargingTimeStamps();
		if (timestamps.iterator().hasNext()) {
			def = ChargingUtils.getEventChargingTimeStampAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (Date timestamp : timestamps) {
					avp.addValue(TimeFormat.toTime(timestamp.getTime()), false);
				}
				l.add(avp);
			}
		}

	}

	/**
	 * Does nothing, 3GPP does not support this AVP.
	 * 
	 * @see com.alcatel_lucent.as.ims.diameter.charging.AbstractServiceUnit#addAvp(com.nextenso.proxylet.diameter.DiameterAVP)
	 */
	@Override
	public void addAvp(DiameterAVP avp) {}

	/**
	 * Does nothing, 3GPP does not support this AVP.
	 * 
	 * @see com.alcatel_lucent.as.ims.diameter.charging.AbstractServiceUnit#setCcMoney(com.alcatel_lucent.as.ims.diameter.charging.CcMoney)
	 */
	@Override
	public void setCcMoney(CcMoney money) {}

	/**
	 * Sets the Reporting-Reason.
	 * 
	 * @param reason The reason.
	 */
	public void setReportingReason(ReportingReason reason) {
		_reportingReason = reason;
	}

	/**
	 * Gets the Reporting-Reason.
	 * 
	 * @return The reason.
	 */
	public ReportingReason getReportingReason() {
		return _reportingReason;
	}

	/**
	 * Adds an Event-Charging-TimeStamp.
	 * 
	 * @param timeStamp The TimeStamp to be added.
	 */
	public void addEventChargingTimeStamp(Date timeStamp) {
		if (timeStamp != null) {
			_eventChargingTimeStamps.add(timeStamp);
		}
	}

	/**
	 * Gets the Event-Charging-TimeStamp list.
	 * 
	 * @return The TimeStamp list .
	 */
	public Iterable<Date> getEventChargingTimeStamps() {
		return _eventChargingTimeStamps;
	}

}
