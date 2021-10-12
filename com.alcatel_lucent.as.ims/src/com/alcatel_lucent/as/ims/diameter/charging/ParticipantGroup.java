// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.ParticipantAccessPriority;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.UserParticipatingType;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * The Participant-Group AVP wrapper.
 */
public class ParticipantGroup {

	private String _calledPartyAddress = null;
	private ParticipantAccessPriority _participantAccessPriority = null;
	private UserParticipatingType _userParticipatingType = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param data The avp data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public ParticipantGroup(byte[] data, Version version) {
		if (data == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		DiameterAVPDefinition def = ChargingUtils.getCalledPartyAddressAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setCalledPartyAddress(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getParticipantAccessPriorityAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setParticipantAccessPriority(ParticipantAccessPriority.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getUserParticipatingTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setUserParticipatingType(UserParticipatingType.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
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
		DiameterAVPDefinition def = ChargingUtils.getParticipantGroupAVP(version);
		if (def == null) {
			return null;
		}

		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		if (getCalledPartyAddress() != null) {
			def = ChargingUtils.getCalledPartyAddressAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getCalledPartyAddress()), false);
				l.add(avp);
			}
		}

		if (getParticipantAccessPriority() != null) {
			def = ChargingUtils.getParticipantAccessPriorityAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getParticipantAccessPriority().getValue()), false);
				l.add(avp);
			}
		}

		if (getUserParticipatingType() != null) {
			def = ChargingUtils.getUserParticipatingTypeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getUserParticipatingType().getValue()), false);
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the Called-Party-Address.
	 * 
	 * @param address The address.
	 */
	public void setCalledPartyAddress(String address) {
		_calledPartyAddress = address;
	}

	/**
	 * Gets the Called-Party-Address.
	 * 
	 * @return The address.
	 */
	public String getCalledPartyAddress() {
		return _calledPartyAddress;
	}

	/**
	 * Sets the Participant-Access-Priority.
	 * 
	 * @param priority The priority.
	 */
	public void setParticipantAccessPriority(ParticipantAccessPriority priority) {
		_participantAccessPriority = priority;
	}

	/**
	 * Gets the Participant-Access-Priority.
	 * 
	 * @return The priority.
	 */
	public ParticipantAccessPriority getParticipantAccessPriority() {
		return _participantAccessPriority;
	}

	/**
	 * Sets the User-Participating-Type.
	 * 
	 * @param type The type.
	 */
	public void setUserParticipatingType(UserParticipatingType type) {
		_userParticipatingType = type;
	}

	/**
	 * Gets the User-Participating-Type.
	 * 
	 * @return The type.
	 */
	public UserParticipatingType getUserParticipatingType() {
		return _userParticipatingType;
	}

}
