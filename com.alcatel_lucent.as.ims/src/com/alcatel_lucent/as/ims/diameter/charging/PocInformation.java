package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.PocEventType;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.PocServerRole;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.PocSessionInitiationType;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.PocSessionType;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The PoC-Information AVP wrapper.
 */
public class PocInformation {

	private PocServerRole _pocServerRole = null;
	private PocSessionType _pocSessionType = null;
	private PocUserRole _pocUserRole = null;
	private PocSessionInitiationType _pocSessionInitiationType = null;
	private PocEventType _pocEventType = null;
	private Long _numberOfParticipants = null;
	private List<String> _participantsInvolveds = new ArrayList<String>();
	private List<ParticipantGroup> _participantGroups = new ArrayList<ParticipantGroup>();
	private List<TalkBurstExchange> _talkBurstExchanges = new ArrayList<TalkBurstExchange>();
	private String _pocControllingAddress = null;
	private String _pocGroupName = null;
	private String _pocSessionId = null;
	private String _chargedParty = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public PocInformation(DiameterAVP avp, Version version) {
		if (avp == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}
		byte[] data = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.getPocServerRoleAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setPocServerRole(PocServerRole.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getPocSessionTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setPocSessionType(PocSessionType.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getPocUserRoleAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setPocUserRole(new PocUserRole(searchedAvp, version));
			}
		}

		def = ChargingUtils.getPocSessionInitiationTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setPocSessionInitiationType(PocSessionInitiationType.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getPocEventTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setPocEventType(PocEventType.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getNumberOfParticipantsAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setNumberOfParticipants(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getParticipantsInvolvedAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addParticipantsInvolved(UTF8StringFormat.getUtf8String(searchedAvp.getValue(i)));
				}
			}
		}

		def = ChargingUtils.getParticipantGroupAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addParticipantGroup(new ParticipantGroup(searchedAvp.getValue(i), version));
				}
			}
		}

		def = ChargingUtils.getTalkBurstExchangeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addTalkBurstExchange(new TalkBurstExchange(searchedAvp.getValue(i), version));
				}
			}
		}

		def = ChargingUtils.getPocControllingAddressAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setPocControllingAddress(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getPocGroupNameAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setPocGroupName(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getPocSessionIdAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setPocSessionId(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getChargedPartyAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setChargedParty(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
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
		DiameterAVPDefinition def = ChargingUtils.getPocInformationAVP(version);
		if (def == null) {
			return null;
		}

		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		if (getPocServerRole() != null) {
			def = ChargingUtils.getPocServerRoleAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getPocServerRole().getValue()), false);
				l.add(avp);
			}
		}

		if (getPocSessionType() != null) {
			def = ChargingUtils.getPocSessionTypeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getPocSessionType().getValue()), false);
				l.add(avp);
			}
		}

		if (getPocUserRole() != null) {
			DiameterAVP avp = getPocUserRole().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getPocSessionInitiationType() != null) {
			def = ChargingUtils.getPocSessionInitiationTypeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getPocSessionInitiationType().getValue()), false);
				l.add(avp);
			}
		}

		if (getPocEventType() != null) {
			def = ChargingUtils.getPocEventTypeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getPocEventType().getValue()), false);
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

		Iterable<String> participants = getParticipantsInvolveds();
		if (participants.iterator().hasNext()) {
			def = ChargingUtils.getParticipantsInvolvedAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (String participant : participants) {
					avp.addValue(UTF8StringFormat.toUtf8String(participant), false);
				}
				l.add(avp);
			}
		}

		Iterable<ParticipantGroup> groups = getParticipantGroups();
		if (groups.iterator().hasNext()) {
			def = ChargingUtils.getParticipantsInvolvedAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (ParticipantGroup group : groups) {
					avp.addValue(group.toAvp(version).getValue(), false);
				}
				l.add(avp);
			}
		}

		Iterable<TalkBurstExchange> exchanges = getTalkBurstExchanges();
		if (exchanges.iterator().hasNext()) {
			def = ChargingUtils.getParticipantsInvolvedAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (TalkBurstExchange exchange : exchanges) {
					avp.addValue(exchange.toAvp(version).getValue(), false);
				}
				l.add(avp);
			}
		}

		if (getPocControllingAddress() != null) {
			def = ChargingUtils.getPocControllingAddressAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getPocControllingAddress()), false);
				l.add(avp);
			}
		}

		if (getPocGroupName() != null) {
			def = ChargingUtils.getPocGroupNameAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getPocGroupName()), false);
				l.add(avp);
			}
		}

		if (getPocSessionId() != null) {
			def = ChargingUtils.getPocSessionIdAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getPocSessionId()), false);
				l.add(avp);
			}
		}

		if (getChargedParty() != null) {
			def = ChargingUtils.getChargedPartyAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getChargedParty()), false);
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the PoC-Server-Role.
	 * 
	 * @param role The role.
	 */
	public void setPocServerRole(PocServerRole role) {
		_pocServerRole = role;
	}

	/**
	 * Gets the PoC-Server-Role.
	 * 
	 * @return The role.
	 */
	public PocServerRole getPocServerRole() {
		return _pocServerRole;
	}

	/**
	 * Sets the PoC-Session-Type.
	 * 
	 * @param type The type.
	 */
	public void setPocSessionType(PocSessionType type) {
		_pocSessionType = type;
	}

	/**
	 * Gets the PoC-Session-Type.
	 * 
	 * @return The type.
	 */
	public PocSessionType getPocSessionType() {
		return _pocSessionType;
	}

	/**
	 * Sets the PoC-User-Role.
	 * 
	 * @param role The role.
	 */
	public void setPocUserRole(PocUserRole role) {
		_pocUserRole = role;
	}

	/**
	 * Gets the PoC-User-Role.
	 * 
	 * @return The role.
	 */
	public PocUserRole getPocUserRole() {
		return _pocUserRole;
	}

	/**
	 * Sets the PoC-Session-Initiation-Type.
	 * 
	 * @param type The type.
	 */
	public void setPocSessionInitiationType(PocSessionInitiationType type) {
		_pocSessionInitiationType = type;
	}

	/**
	 * Gets the PoC-Session-Initiation-Type.
	 * 
	 * @return The type.
	 */
	public PocSessionInitiationType getPocSessionInitiationType() {
		return _pocSessionInitiationType;
	}

	/**
	 * Sets the PoC-Event-Type.
	 * 
	 * @param type The type.
	 */
	public void setPocEventType(PocEventType type) {
		_pocEventType = type;
	}

	/**
	 * Gets the PoC-Event-Type.
	 * 
	 * @return The type.
	 */
	public PocEventType getPocEventType() {
		return _pocEventType;
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
	 * Adds a Participants-Involved.
	 * 
	 * @param participant The participant.
	 */
	public void addParticipantsInvolved(String participant) {
		if (participant != null) {
			_participantsInvolveds.add(participant);
		}
	}

	/**
	 * Gets the Participants-Involved list.
	 * 
	 * @return The participants.
	 */
	public Iterable<String> getParticipantsInvolveds() {
		return _participantsInvolveds;
	}

	/**
	 * Adds a Participant_Group.
	 * 
	 * @param group The group.
	 */
	public void addParticipantGroup(ParticipantGroup group) {
		if (group != null) {
			_participantGroups.add(group);
		}
	}

	/**
	 * Gets the Participant_Group list.
	 * 
	 * @return The groups.
	 */
	public Iterable<ParticipantGroup> getParticipantGroups() {
		return _participantGroups;
	}

	/**
	 * Sets the Talk-Burst-Exchange.
	 * 
	 * @param exchange The exchange.
	 */
	public void addTalkBurstExchange(TalkBurstExchange exchange) {
		if (exchange != null) {
			_talkBurstExchanges.add(exchange);
		}
	}

	/**
	 * Gets the Talk-Burst-Exchange list.
	 * 
	 * @return The exchanges.
	 */
	public Iterable<TalkBurstExchange> getTalkBurstExchanges() {
		return _talkBurstExchanges;
	}

	/**
	 * Sets the PoC-Controlling-Address.
	 * 
	 * @param address The address.
	 */
	public void setPocControllingAddress(String address) {
		_pocControllingAddress = address;
	}

	/**
	 * Gets the PoC-Controlling-Address.
	 * 
	 * @return The address.
	 */
	public String getPocControllingAddress() {
		return _pocControllingAddress;
	}

	/**
	 * Sets the PoC-Group-Name.
	 * 
	 * @param name The name.
	 */
	public void setPocGroupName(String name) {
		_pocGroupName = name;
	}

	/**
	 * Gets the PoC-Group-Name.
	 * 
	 * @return The name.
	 */
	public String getPocGroupName() {
		return _pocGroupName;
	}

	/**
	 * Sets the PoC-Session-Id.
	 * 
	 * @param id The id.
	 */
	public void setPocSessionId(String id) {
		_pocSessionId = id;
	}

	/**
	 * Gets the PoC-Session-Id.
	 * 
	 * @return The id.
	 */
	public String getPocSessionId() {
		return _pocSessionId;
	}

	/**
	 * Sets the Charged-Party.
	 * 
	 * @param party The party.
	 */
	public void setChargedParty(String party) {
		_chargedParty = party;
	}

	/**
	 * Gets the Charged-Party.
	 * 
	 * @return The party.
	 */
	public String getChargedParty() {
		return _chargedParty;
	}

}
