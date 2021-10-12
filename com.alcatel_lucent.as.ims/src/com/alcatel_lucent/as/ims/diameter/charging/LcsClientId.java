// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.LcsClientType;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * The LCS-Client-Id AVP wrapper.
 */
public class LcsClientId {

	private LcsClientType _lcsClientType = null;
	private String _lcsClientExternalId = null;
	private String _lcsClientDialedByMs = null;
	private LcsClientName _lcsClientName = null;
	private String _lcsApn = null;
	private LcsRequestorId _lcsRequestorId = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public LcsClientId(DiameterAVP avp, Version version) {
		byte[] data = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.getLcsClientTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setLcsClientType(LcsClientType.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getLcsClientExternalIdAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setLcsClientExternalId(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getLcsClientDialedByMsAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setLcsClientDialedByMs(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getLcsClientNameAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setLcsClientName(new LcsClientName(searchedAvp, version));
			}
		}

		def = ChargingUtils.getLcsApnAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setLcsApn(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getLcsRequestorIdAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setLcsRequestorId(new LcsRequestorId(searchedAvp, version));
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
		DiameterAVPDefinition def = ChargingUtils.getLcsClientIdAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> avps = new ArrayList<DiameterAVP>();

		if (getLcsClientType() != null) {
			def = ChargingUtils.getLcsClientTypeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getLcsClientType().getValue()), false);
				avps.add(avp);
			}
		}

		if (getLcsClientExternalId() != null) {
			def = ChargingUtils.getLcsClientExternalIdAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getLcsClientExternalId()), false);
				avps.add(avp);
			}
		}

		if (getLcsClientDialedByMs() != null) {
			def = ChargingUtils.getLcsClientDialedByMsAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getLcsClientDialedByMs()), false);
				avps.add(avp);
			}
		}

		if (getLcsClientName() != null) {
			DiameterAVP avp = getLcsClientName().toAvp(version);
			if (avp != null) {
				avps.add(avp);
			}
		}

		if (getLcsApn() != null) {
			def = ChargingUtils.getLcsApnAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getLcsApn()), false);
				avps.add(avp);
			}
		}

		if (getLcsRequestorId() != null) {
			DiameterAVP avp = getLcsRequestorId().toAvp(version);
			if (avp != null) {
				avps.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(avps), false);
		return res;
	}

	/**
	 * Sets the LCS-Client-Type.
	 * 
	 * @param type The type.
	 */
	public void setLcsClientType(LcsClientType type) {
		_lcsClientType = type;
	}

	/**
	 * Gets the LCS-Client-Type.
	 * 
	 * @return The type.
	 */
	public LcsClientType getLcsClientType() {
		return _lcsClientType;
	}

	/**
	 * Sets the LCS-Client-ExternalId.
	 * 
	 * @param id The id.
	 */
	public void setLcsClientExternalId(String id) {
		_lcsClientExternalId = id;
	}

	/**
	 * Gets the LCS-Client-ExternalId.
	 * 
	 * @return The id.
	 */
	public String getLcsClientExternalId() {
		return _lcsClientExternalId;
	}

	/**
	 * Sets the ICS-Client-Dialed-By-MS.
	 * 
	 * @param client The client.
	 */
	public void setLcsClientDialedByMs(String client) {
		_lcsClientDialedByMs = client;
	}

	/**
	 * Gets the ICS-Client-Dialed-By-MS.
	 * 
	 * @return The client.
	 */
	public String getLcsClientDialedByMs() {
		return _lcsClientDialedByMs;
	}

	/**
	 * Sets the LCS-Client-Name.
	 * 
	 * @param name The name.
	 */
	public void setLcsClientName(LcsClientName name) {
		_lcsClientName = name;
	}

	/**
	 * Gets the LCS-Client-Name.
	 * 
	 * @return The name.
	 */
	public LcsClientName getLcsClientName() {
		return _lcsClientName;
	}

	/**
	 * Sets the LCS-APN.
	 * 
	 * @param apn The apn.
	 */
	public void setLcsApn(String apn) {
		_lcsApn = apn;
	}

	/**
	 * Gets the LCS-APN.
	 * 
	 * @return The apn.
	 */
	public String getLcsApn() {
		return _lcsApn;
	}

	/**
	 * Sets the LCS-Requestor-Id.
	 * 
	 * @param id The id.
	 */
	public void setLcsRequestorId(LcsRequestorId id) {
		_lcsRequestorId = id;
	}

	/**
	 * Gets the LCS-Requestor-Id.
	 * 
	 * @return The id.
	 */
	public LcsRequestorId getLcsRequestorId() {
		return _lcsRequestorId;
	}

}
