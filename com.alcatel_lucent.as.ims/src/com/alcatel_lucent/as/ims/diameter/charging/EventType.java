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
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The Event-Type AVP wrapper.
 */
public class EventType {

	private String _sipMethod = null;
	private String _event = null;
	private Long _expires = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public EventType(DiameterAVP avp, Version version) {
		if (avp == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		byte[] data = avp.getValue();
		DiameterAVPDefinition def = ChargingUtils.getSipMethodAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setSipMethod(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getEventAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setEvent(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getExpiresAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setExpires(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
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
		DiameterAVPDefinition def = ChargingUtils.getEventTypeAVP(version);
		if (def == null) {
			return null;
		}
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();
		if (getSipMethod() != null) {
			def = ChargingUtils.getSipMethodAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getSipMethod()), false);
				l.add(avp);
			}
		}

		if (getEvent() != null) {
			def = ChargingUtils.getEventAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getEvent()), false);
				l.add(avp);
			}
		}

		if (getExpires() != null) {
			def = ChargingUtils.getExpiresAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(getExpires()), false);
				l.add(avp);
			}
		}

		DiameterAVP res = new DiameterAVP(def);
		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Gets the SIP method (mapped to the SIP-Method AVP).
	 * 
	 * @return The method.
	 */
	public String getSipMethod() {
		return _sipMethod;
	}

	/**
	 * Sets the SIP method.
	 * 
	 * @param method The method.
	 */
	public void setSipMethod(String method) {
		_sipMethod = method;
	}

	/**
	 * Gets the event (mapped to the Event AVP).
	 * 
	 * @return The event.
	 */
	public String getEvent() {
		return _event;
	}

	/**
	 * Sets the event.
	 * 
	 * @param event The event.
	 */
	public void setEvent(String event) {
		_event = event;
	}

	/**
	 * Gets the expires value (mapped to the Expires AVP).
	 * 
	 * @return The expires value.
	 */
	public Long getExpires() {
		return _expires;
	}

	/**
	 * Sets the expires value.
	 * 
	 * @param expires The expires value.
	 */
	public void setExpires(Long expires) {
		_expires = expires;
	}

}
