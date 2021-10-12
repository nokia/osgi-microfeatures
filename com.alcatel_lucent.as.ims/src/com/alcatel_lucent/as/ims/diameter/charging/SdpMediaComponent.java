// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.MediaInitiatorFlag;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.SdpType;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * The SDP-Media-Component AVP wrapper.
 */
public class SdpMediaComponent {

	private String _sdpMediaName = null;
	private List<String> _sdpMediaDescriptions = new ArrayList<String>();
	private MediaInitiatorFlag _mediaInitiatorFlag = null;
	private String _mediaInitiatorParty = null;
	private String _authorizedQoS = null;
	private byte[] _3gppChargingId = null;
	private byte[] _accessNetworkChargingIdentifierValue = null;
	private SdpType _sdpType = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public SdpMediaComponent(DiameterAVP avp, Version version) {
		this(avp.getValue(), version);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param data The avp data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public SdpMediaComponent(byte[] data, Version version) {
		if (data == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		DiameterAVPDefinition def = ChargingUtils.getSdpMediaNameAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setSdpMediaName(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getSdpMediaDescriptionAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addSdpMediaDescription(UTF8StringFormat.getUtf8String(searchedAvp.getValue(i)));
				}
			}
		}

		def = ChargingUtils.getMediaInitiatorFlagAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setMediaInitiatorFlag(MediaInitiatorFlag.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getMediaInitiatorPartyAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setMediaInitiatorParty(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getAuthorizedQosAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setAuthorizedQoS(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.get3gppChargingIdAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				set3gppChargingId(searchedAvp.getValue());
			}
		}

		def = ChargingUtils.getAccessNetworkChargingIdentifierValueAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setAccessNetworkChargingIdentifierValue(searchedAvp.getValue());
			}
		}

		def = ChargingUtils.getSdpTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setSdpType(SdpType.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
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
		DiameterAVPDefinition def = ChargingUtils.getSdpMediaComponentAVP(version);
		if (def == null) {
			return null;
		}

		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		if (getSdpMediaName() != null) {
			def = ChargingUtils.getSdpMediaNameAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getSdpMediaName()), false);
				l.add(avp);
			}
		}

		Iterable<String> descriptions = getSdpMediaDescriptions();
		if (descriptions.iterator().hasNext()) {
			def = ChargingUtils.getSdpMediaDescriptionAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (String desc : descriptions) {
					avp.addValue(UTF8StringFormat.toUtf8String(desc), false);
				}
				l.add(avp);
			}
		}

		if (getMediaInitiatorFlag() != null) {
			def = ChargingUtils.getMediaInitiatorFlagAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getMediaInitiatorFlag().getValue()), false);
				l.add(avp);
			}
		}

		if (getMediaInitiatorParty() != null) {
			def = ChargingUtils.getMediaInitiatorPartyAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getMediaInitiatorParty()), false);
				l.add(avp);
			}
		}

		if (getAuthorizedQoS() != null) {
			def = ChargingUtils.getAuthorizedQosAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getAuthorizedQoS()), false);
				l.add(avp);
			}
		}

		byte[] value = get3gppChargingId();
		if (value != null) {
			def = ChargingUtils.get3gppChargingIdAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(value, false);
				l.add(avp);
			}
		}

		value = getAccessNetworkChargingIdentifierValue();
		if (value != null) {
			def = ChargingUtils.getAccessNetworkChargingIdentifierValueAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(value, false);
				l.add(avp);
			}
		}

		if (getSdpType() != null) {
			def = ChargingUtils.getSdpTypeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getSdpType().getValue()), false);
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the SDP media name.
	 * 
	 * @param name The name.
	 */
	public void setSdpMediaName(String name) {
		_sdpMediaName = name;
	}

	/**
	 * Gets the SDP media name (mapped to the SDP-Media-Name AVP).
	 * 
	 * @return The name.
	 */
	public String getSdpMediaName() {
		return _sdpMediaName;
	}

	/**
	 * Adds a SDP media description.
	 * 
	 * @param description The description.
	 */
	public void addSdpMediaDescription(String description) {
		_sdpMediaDescriptions.add(description);
	}

	/**
	 * Gets the SDP media descriptions (mapped to the SDP-Media-Description AVP).
	 * 
	 * @return The descriptions.
	 */
	public Iterable<String> getSdpMediaDescriptions() {
		return _sdpMediaDescriptions;
	}

	/**
	 * Sets the media initiator flag.
	 * 
	 * @param flag The flag.
	 */
	public void setMediaInitiatorFlag(MediaInitiatorFlag flag) {
		_mediaInitiatorFlag = flag;
	}

	/**
	 * Gets the media initiator flag (mapped to the Media-Initiator-Flag AVP).
	 * 
	 * @return The flag.
	 */
	public MediaInitiatorFlag getMediaInitiatorFlag() {
		return _mediaInitiatorFlag;
	}

	/**
	 * Sets the media initiator party.
	 * 
	 * @param party The media initiator party.
	 */
	public void setMediaInitiatorParty(String party) {
		_mediaInitiatorParty = party;
	}

	/**
	 * Gets the media initiator party (mapped to the Media-Initiator-Party AVP).
	 * 
	 * @return The media initiator party.
	 */
	public String getMediaInitiatorParty() {
		return _mediaInitiatorParty;
	}

	/**
	 * Sets the authorized QoS.
	 * 
	 * @param qos The authorized QoS.
	 */
	public void setAuthorizedQoS(String qos) {
		_authorizedQoS = qos;
	}

	/**
	 * Gets the authorized QoS (mapped to the Authorized-QoS AVP).
	 * 
	 * @return The QoS.
	 */
	public String getAuthorizedQoS() {
		return _authorizedQoS;
	}

	/**
	 * Sets the 3GPP charging identifier.
	 * 
	 * @param id The identifier.
	 */
	public void set3gppChargingId(byte[] id) {
		_3gppChargingId = copyArray(id);
	}

	/**
	 * Gets the 3GPP charging identifier (mapped to the 3GPP-ChargingId AVP).
	 * 
	 * @return The identifier.
	 */
	public byte[] get3gppChargingId() {
		return copyArray(_3gppChargingId);
	}

	/**
	 * Sets the access network charging identifier value..
	 * 
	 * @param value The value.
	 */
	public void setAccessNetworkChargingIdentifierValue(byte[] value) {
		_accessNetworkChargingIdentifierValue = copyArray(value);
	}

	/**
	 * Gets the access network charging identifier value (mapped to the
	 * Access-Network-Charging-Identifier-Value AVP).
	 * 
	 * @return The value.
	 */
	public byte[] getAccessNetworkChargingIdentifierValue() {
		return copyArray(_accessNetworkChargingIdentifierValue);
	}

	/**
	 * Sets the SDP type.
	 * 
	 * @param type The type.
	 */
	public void setSdpType(SdpType type) {
		_sdpType = type;
	}

	/**
	 * Gets the SDP type (mapped to the SDP-Type AVP).
	 * 
	 * @return The type.
	 */
	public SdpType getSdpType() {
		return _sdpType;
	}

	private byte[] copyArray(byte[] src) {
		if (src == null) {
			return null;
		}

		int len = src.length;
		byte[] res = new byte[len];
		System.arraycopy(src, 0, res, 0, len);
		return res;
	}

}
