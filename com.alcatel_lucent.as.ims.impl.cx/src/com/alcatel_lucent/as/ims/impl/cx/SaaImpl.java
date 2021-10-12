// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.impl.cx;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.cx.ChargingInformation;
import com.alcatel_lucent.as.ims.diameter.cx.CxUtils;
import com.alcatel_lucent.as.ims.diameter.cx.SCSCFRestorationInfo;
import com.alcatel_lucent.as.ims.diameter.cx.ServerAssignmentAnswer;
import com.alcatel_lucent.as.ims.diameter.cx.CxConstants.LooseRouteIndication;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * The SAA implementation.
 */
public class SaaImpl
		extends CxResponseImpl
		implements ServerAssignmentAnswer {

	/**
	 * Constructor for this class.
	 * 
	 * @param response The response.
	 * @param version The Cx version.
	 */
	public SaaImpl(DiameterClientResponse response, Version version) {
		super(response, version);
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.ServerAssignmentAnswer#getUserProfile()
	 */
	public byte[] getUserProfile() {
		DiameterAVPDefinition def = CxUtils.getUserDataAVP(getVersion());
		if (def == null) {
			return null;
		}

		DiameterAVP avp = getResponse().getDiameterAVP(def);
		if (avp == null) {
			return null;
		}

		return avp.getValue();
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.ServerAssignmentAnswer#getChargingInformation()
	 */
	public ChargingInformation getChargingInformation() {
		DiameterAVPDefinition def = CxUtils.getChargingInformationAVP(getVersion());
		if (def == null) {
			return null;
		}
		DiameterAVP avp = getResponse().getDiameterAVP(def);
		if (avp == null) {
			return null;
		}

		return new ChargingInformation(avp, getVersion());
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.ServerAssignmentAnswer#getAssociatedIdentities()
	 */
	public List<String> getAssociatedIdentities() {
		DiameterAVPDefinition def = CxUtils.getAssociatedIdentitiesAVP(getVersion());
		if (def == null) {
			return null;
		}

		DiameterAVP avp = getResponse().getDiameterAVP(def);
		if (avp == null) {
			return null;
		}

		avp = GroupedFormat.getDiameterAVP(DiameterBaseConstants.AVP_USER_NAME, avp.getValue(), false);
		if (avp == null) {
			return null;
		}
		List<String> res = new ArrayList<String>();
		for (int i = 0; i < avp.getValueSize(); i++) {
			String s = UTF8StringFormat.getUtf8String(avp.getValue(i));
			res.add(s);
		}
		return res;
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.ServerAssignmentAnswer#getAssociatedRegisteredIdentities()
	 */
	public List<String> getAssociatedRegisteredIdentities() {
		DiameterAVPDefinition def = CxUtils.getAssociatedRegisteredIdentitiesAVP(getVersion());
		if (def == null) {
			return null;
		}

		DiameterAVP avp = getResponse().getDiameterAVP(def);
		if (avp == null) {
			return null;
		}

		avp = GroupedFormat.getDiameterAVP(DiameterBaseConstants.AVP_USER_NAME, avp.getValue(), false);
		if (avp == null) {
			return null;
		}
		List<String> res = new ArrayList<String>();
		for (int i = 0; i < avp.getValueSize(); i++) {
			String s = UTF8StringFormat.getUtf8String(avp.getValue(i));
			res.add(s);
		}
		return res;
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.ServerAssignmentAnswer#getLooseRouteIndication()
	 */
	public LooseRouteIndication getLooseRouteIndication() {
		DiameterAVPDefinition def = CxUtils.getLooseRouteIndicationAVP(getVersion());
		if (def == null) {
			return null;
		}

		DiameterAVP avp = getResponse().getDiameterAVP(def);
		if (avp == null) {
			return null;
		}

		int indication = EnumeratedFormat.getEnumerated(avp.getValue(), 0);
		for (LooseRouteIndication loose : LooseRouteIndication.values()) {
			if (loose.getValue() == indication) {
				return loose;
			}
		}
		return null;
	}

	/**
	 * @see com.alcatel_lucent.as.interfaces.gpp.diameter.cx.ServerAssignmentAnswer#getSCSCFRestorationInformation()
	 */
	public List<SCSCFRestorationInfo> getSCSCFRestorationInformation() {
		DiameterAVPDefinition def = CxUtils.getSCSCFRestaurationInfoAVP(getVersion());
		if (def == null) {
			return null;
		}

		DiameterAVP avp = getResponse().getDiameterAVP(def);
		if (avp == null) {
			return null;
		}

		List<SCSCFRestorationInfo> res = new ArrayList<SCSCFRestorationInfo>();
		for (int i = 0; i < avp.getValueSize(); i++) {
			SCSCFRestorationInfo info = new SCSCFRestorationInfo(avp.getValue(i), getVersion());
			res.add(info);
		}
		return res;
	}

}
