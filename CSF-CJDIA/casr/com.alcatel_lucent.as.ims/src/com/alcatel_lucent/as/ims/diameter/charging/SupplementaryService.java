package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.ParticipantActionType;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.TimeFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The Supplementary-Service AVP wrapper.
 */
public class SupplementaryService {

	private Long _serviceType = null;
	private Long _serviceMode = null;
	private Long _numberOfDiversions = null;
	private String _associatedPartyAddress = null;
	private String _serviceId = null;
	private Date _changeTime = null;
	private Long _numberOfParticipants = null;
	private ParticipantActionType _participantActionType = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public SupplementaryService(DiameterAVP avp, Version version) {
		this(avp.getValue(), version);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param data The avp data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public SupplementaryService(byte[] data, Version version) {
		if (data == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		DiameterAVPDefinition def = ChargingUtils.getServiceTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setServiceType(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getServiceModeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setServiceMode(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getNumberOfDiversionsAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setNumberOfDiversions(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getAssociatedPartyAddressAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setAssociatedPartyAddress(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getServiceIdAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setServiceId(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getChangeTimeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setChangeTime(new Date(TimeFormat.getTime(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getNumberOfParticipantsAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setNumberOfParticipants(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getParticipantActionTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setParticipantActionType(ParticipantActionType.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
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
		DiameterAVPDefinition def = ChargingUtils.getSupplementaryServiceAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		if (getServiceType() != null) {
			def = ChargingUtils.getServiceTypeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(getServiceType()), false);
				l.add(avp);
			}
		}

		if (getServiceMode() != null) {
			def = ChargingUtils.getServiceModeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(getServiceMode()), false);
				l.add(avp);
			}
		}

		if (getNumberOfDiversions() != null) {
			def = ChargingUtils.getNumberOfDiversionsAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(getNumberOfDiversions()), false);
				l.add(avp);
			}
		}

		if (getAssociatedPartyAddress() != null) {
			def = ChargingUtils.getAssociatedPartyAddressAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getAssociatedPartyAddress()), false);
				l.add(avp);
			}
		}

		if (getServiceId() != null) {
			def = ChargingUtils.getServiceIdAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getServiceId()), false);
				l.add(avp);
			}
		}

		if (getChangeTime() != null) {
			def = ChargingUtils.getChangeTimeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(TimeFormat.toTime(getChangeTime().getTime()), false);
				l.add(avp);
			}
		}

		if (getNumberOfParticipants() != null) {
			def = ChargingUtils.getNumberOfParticipantsAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(getNumberOfParticipants()), false);
				l.add(avp);
			}
		}

		if (getParticipantActionType() != null) {
			def = ChargingUtils.getParticipantActionTypeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getParticipantActionType().getValue()), false);
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the Service-Type.
	 * 
	 * @param type The type.
	 */
	public void setServiceType(Long type) {
		_serviceType = type;
	}

	/**
	 * Gets the Service-Type.
	 * 
	 * @return The type.
	 */
	public Long getServiceType() {
		return _serviceType;
	}

	/**
	 * Sets the Service-Mode.
	 * 
	 * @param mode The mode.
	 */
	public void setServiceMode(Long mode) {
		_serviceMode = mode;
	}

	/**
	 * Gets the Service-Mode.
	 * 
	 * @return The mode.
	 */
	public Long getServiceMode() {
		return _serviceMode;
	}

	/**
	 * Sets the Number-Of-Diversions.
	 * 
	 * @param number The number.
	 */
	public void setNumberOfDiversions(Long number) {
		_numberOfDiversions = number;
	}

	/**
	 * Gets the Number-Of-Diversions.
	 * 
	 * @return The number.
	 */
	public Long getNumberOfDiversions() {
		return _numberOfDiversions;
	}

	/**
	 * Sets the Associated-Party-Address.
	 * 
	 * @param address The address.
	 */
	public void setAssociatedPartyAddress(String address) {
		_associatedPartyAddress = address;
	}

	/**
	 * Gets the Associated-Party-Address.
	 * 
	 * @return The address.
	 */
	public String getAssociatedPartyAddress() {
		return _associatedPartyAddress;
	}

	/**
	 * Sets the Change-Time.
	 * 
	 * @param time The time.
	 */
	public void setChangeTime(Date time) {
		if (time == null) {
			_changeTime = null;
		} else {
			_changeTime = (Date) time.clone();
		}
	}

	/**
	 * Gets the Change-Time.
	 * 
	 * @return The time.
	 */
	public Date getChangeTime() {
		if (_changeTime == null) {
			return null;
		}
		return (Date) _changeTime.clone();
	}

	/**
	 * Sets the Service-Id.
	 * 
	 * @param id The id.
	 */
	public void setServiceId(String id) {
		_serviceId = id;
	}

	/**
	 * Gets the Service-Id.
	 * 
	 * @return The id.
	 */
	public String getServiceId() {
		return _serviceId;
	}

	/**
	 * Sets the Number-Of-Participants.
	 * 
	 * @param number The number.
	 */
	public void setNumberOfParticipants(Long number) {
		_numberOfParticipants = number;
	}

	/**
	 * Gets the Number-Of-Participants.
	 * 
	 * @return The number.
	 */
	public Long getNumberOfParticipants() {
		return _numberOfParticipants;
	}

	/**
	 * Sets the Participant-Action-Type.
	 * 
	 * @param type The type.
	 */
	public void setParticipantActionType(ParticipantActionType type) {
		_participantActionType = type;
	}

	/**
	 * Gets the Participant-Action-Type.
	 * 
	 * @return The type.
	 */
	public ParticipantActionType getParticipantActionType() {
		return _participantActionType;
	}

}
